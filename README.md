# JChat

Micronaut-Anwendung, die eine OpenAI-kompatible `/v1/chat/completions`-Schnittstelle
bereitstellt und intern über LangChain4j mehrere LLM-Provider anspricht.
Gedacht als Backend für Open WebUI (oder jeden anderen OpenAI-kompatiblen Client),
mit voller Kontrolle über Prompt-Aufbau und Antwortverarbeitung in Java.

## Struktur

```
src/main/java/name/hergeth/jchat/
├── Application.java
├── ai/
│   ├── SystemPromptProvider.java
│   ├── PromptBuilder.java / ContextWindowPromptBuilder.java
│   ├── ConversationTurns.java / QueryTerms.java / StatementRelevanceScorer.java
│   ├── ConversationIds.java
│   ├── TurnFactory.java / TurnRenderer.java / TurnProcessor.java
│   ├── model/                      # Turn, Statement (Triple), ToolResult
│   ├── StatementExtractor.java / LlmStatementExtractor.java
│   ├── StatementParser.java
│   ├── StatementNormalizer.java / IdentityStatementNormalizer.java
│   ├── Retriever.java / RelevanceRetriever.java
│   └── KnowledgeStore.java / InMemoryKnowledgeStore.java
├── ai/llm/
│   ├── ChatModelRegistry.java      # Anthropic, OpenAI, Ollama
│   ├── AiServiceFactory.java       # Multi-Message-Chat über LangChain4j
│   ├── TaskRouter.java             # Aufgaben → Provider (chat, extraction, …)
│   └── LlmProviderConfig.java
└── openai/
    ├── ChatCompletionsController.java  # POST /v1/chat/completions, GET /v1/models
    └── dto/                             # OpenAI-kompatible Request-/Response-Typen
├── debug/
│   ├── DebugController.java             # GET /api/debug/*
│   └── DebugTraceService.java           # Snapshots pro Turn
└── ...

frontend/                                # Vue 3 Debug-UI → META-INF/resources/

src/main/resources/system-prompt.txt   # System-Prompt (Classpath)
config/system-prompt.txt               # optional: Override per Dateipfad
```

## Voraussetzungen

- JDK 17+
- API-Key für mindestens einen konfigurierten Provider (siehe `application.yml`)

## Start

```bash
export ANTHROPIC_API_KEY=dein-key
./gradlew run
```

Nur Ollama lokal (ohne Cloud-Keys):

```bash
# In application.yml tasks.chat auf ollama-local setzen
./gradlew run
```

Server läuft danach auf `http://localhost:8080`.

## Debug-UI

Nach `./gradlew run` ist das Debug-UI unter **http://localhost:8080/** erreichbar.
(Vor dem ersten Start oder nach Frontend-Änderungen: `./gradlew processResources` bzw. `./gradlew run` baut das UI automatisch mit.)

Frontend-Abhängigkeiten und Build (Node wird bei Bedarf von Gradle heruntergeladen — kein systemweites npm nötig):

```bash
./gradlew npmInstall      # npm install in frontend/
./gradlew npm_run_build   # Vue-UI bauen
./gradlew run
```

Entwicklung mit Hot-Reload:

```bash
cd frontend && npm install && npm run dev   # Proxy auf :8080
```

Das UI zeigt pro Turn (Auto-Refresh alle 2s): User-Eingabe, Kontext, Prompt, LLM-Antwort, Knowledge Store.

API:

- `GET /api/debug/latest?conversationId=...`
- `GET /api/debug/knowledge-store?conversationId=default`
- `GET /api/debug/traces?limit=20`

## Mit Open WebUI verbinden

In Open WebUI unter **Admin Settings → Connections → OpenAI**:

- Base URL: `http://localhost:8080/v1`
- API-Key: beliebiger Platzhalter (wird nicht geprüft)

## Provider konfigurieren

Alle Provider stehen in [`application.yml`](src/main/resources/application.yml) unter `llm.providers`.
Nicht konfigurierte Provider (fehlender API-Key) werden beim Start übersprungen.

Aufgaben-Zuordnung unter `llm.tasks` (jeweils mit `provider`-Feld):

```yaml
llm:
  tasks:
    chat:
      provider: ollama-main
    extraction:
      provider: ollama-fast
    default:
      provider: ollama-main
```

| Aufgabe     | Standard-Provider | Verwendung                          |
|-------------|-------------------|-------------------------------------|
| `chat`      | ollama-main       | Haupt-Chat (LLM_GROSS)              |
| `extraction`| ollama-fast       | Triple-Extraktion nach jedem Turn   |
| `default`   | ollama-main       | Fallback                            |

Im Open-WebUI-Dropdown erscheinen die Provider-Namen aus `llm.providers` (z. B. `anthropic-main`, `ollama-local`).

## Bekannte Einschränkungen (bewusst offen gelassen)

- **Kein Streaming**: Antworten kommen komplett auf einmal statt Token für Token.
  Open WebUI erwartet standardmäßig SSE (`stream: true`) – funktioniert erstmal
  auch ohne, wirkt aber weniger „flüssig“.
- **Retriever**: keyword-basiert — relevante Triples aus dem Knowledge Store (Schritt 3).
- **Prompt**: letzte `app.context.recent-turns` Turns als Klartext + retrieved Wissen im System-Prompt.
- **Extraktion**: Nach jedem Turn ruft `ollama-fast` Triples ab (`subject | predicate | object`),
  normalisiert sie und speichert sie im In-Memory-Store pro `conversation_id`.
- **KnowledgeStore**: rein In-Memory, geht beim Neustart verloren.
  Optional im Request: `"conversation_id": "..."`.
