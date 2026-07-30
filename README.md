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
│   ├── PromptBuilder.java / FullHistoryPromptBuilder.java
│   ├── ConversationIds.java
│   ├── TurnFactory.java
│   ├── model/                      # Turn, Statement (Triple), ToolResult
│   ├── StatementExtractor.java / NoopStatementExtractor.java
│   ├── StatementNormalizer.java / IdentityStatementNormalizer.java
│   ├── Retriever.java / NoopRetriever.java
│   └── KnowledgeStore.java / InMemoryKnowledgeStore.java
├── ai/llm/
│   ├── ChatModelRegistry.java      # Anthropic, OpenAI, Ollama
│   ├── AiServiceFactory.java       # Multi-Message-Chat über LangChain4j
│   ├── TaskRouter.java             # Aufgaben → Provider (chat, extraction, …)
│   └── LlmProviderConfig.java
└── openai/
    ├── ChatCompletionsController.java  # POST /v1/chat/completions, GET /v1/models
    └── dto/                             # OpenAI-kompatible Request-/Response-Typen

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

## Mit Open WebUI verbinden

In Open WebUI unter **Admin Settings → Connections → OpenAI**:

- Base URL: `http://localhost:8080/v1`
- API-Key: beliebiger Platzhalter (wird nicht geprüft)

## Provider konfigurieren

Alle Provider stehen in [`application.yml`](src/main/resources/application.yml) unter `llm.providers`.
Nicht konfigurierte Provider (fehlender API-Key) werden beim Start übersprungen.

Aufgaben-Zuordnung unter `llm.tasks`:

| Aufgabe     | Standard-Provider | Verwendung                          |
|-------------|-------------------|-------------------------------------|
| `chat`      | anthropic-main    | Haupt-Chat (LLM_GROSS)              |
| `extraction`| anthropic-fast    | Vorbereitet für Schritt 2           |
| `default`   | anthropic-main    | Fallback                            |

Im Open-WebUI-Dropdown erscheinen die Provider-Namen aus `llm.providers` (z. B. `anthropic-main`, `ollama-local`).

## Bekannte Einschränkungen (bewusst offen gelassen)

- **Kein Streaming**: Antworten kommen komplett auf einmal statt Token für Token.
  Open WebUI erwartet standardmäßig SSE (`stream: true`) – funktioniert erstmal
  auch ohne, wirkt aber weniger „flüssig“.
- **Retriever, StatementExtractor**: aktuell No-op-Implementierungen. Interfaces und
  Datenmodelle für Schritt 2 sind vorbereitet (`Turn`, `Statement`, `StatementNormalizer`,
  Conversation-Scope im Store/Retriever). Extraktion (LLM → Normalizer → Store) folgt in Schritt 2.
- **KnowledgeStore**: rein In-Memory, pro `conversation_id` (Fallback: `"default"`).
  Optional im Request: `"conversation_id": "..."`.
