# openwebui-bridge

Micronaut-Anwendung, die eine OpenAI-kompatible `/v1/chat/completions`-Schnittstelle
bereitstellt und intern über LangChain4j ein LLM (Standard: Anthropic) anspricht.
Gedacht als Backend für Open WebUI (oder jeden anderen OpenAI-kompatiblen Client),
mit voller Kontrolle über Prompt-Aufbau und Antwortverarbeitung in Java.

## Struktur

```
src/main/java/com/example/
├── Application.java
├── ai/
│   ├── ChatAssistant.java          # LangChain4j AI-Service-Interface
│   ├── SystemPromptProvider.java   # liest System-Prompt aus config/system-prompt.txt
│   ├── PromptBuilder.java / FullHistoryPromptBuilder.java
│   ├── Retriever.java / NoopRetriever.java
│   ├── KnowledgeStore.java / InMemoryKnowledgeStore.java
│   └── StatementExtractor.java / NoopStatementExtractor.java
└── openai/
    ├── ChatCompletionsController.java  # POST /v1/chat/completions, GET /v1/models
    └── dto/                             # OpenAI-kompatible Request-/Response-Typen

config/system-prompt.txt   # System-Prompt als editierbare Textdatei
```

## Voraussetzungen

- JDK 17+
- Ein Anthropic API-Key (oder Umstellung auf ein anderes Provider-Modul, siehe unten)

## Start

```bash
export ANTHROPIC_API_KEY=dein-key
./gradlew run
```

Server läuft danach auf `http://localhost:8080`.

## Mit Open WebUI verbinden

In Open WebUI unter **Admin Settings → Connections → OpenAI**:

- Base URL: `http://localhost:8080/v1`
- API-Key: beliebiger Platzhalter (wird nicht geprüft)

## Anderen Provider nutzen

In `build.gradle.kts` die Zeile

```kotlin
implementation("io.micronaut.langchain4j:micronaut-langchain4j-anthropic")
```

gegen z. B. `micronaut-langchain4j-openai` oder `micronaut-langchain4j-ollama` austauschen
und die passende Konfiguration in `application.yml` anpassen (siehe Micronaut-LangChain4j-Doku).

## Bekannte Einschränkungen (bewusst offen gelassen)

- **Kein Streaming**: Antworten kommen komplett auf einmal statt Token für Token.
  Open WebUI erwartet standardmäßig SSE (`stream: true`) – funktioniert erstmal
  auch ohne, wirkt aber weniger "flüssig".
- **Retriever, StatementExtractor**: aktuell No-op-Implementierungen (liefern nichts /
  extrahieren nichts). Bewusst als Platzhalter angelegt, damit die Architektur steht,
  bevor echte Logik (z. B. Vektorsuche, LLM-basierte Extraktion) eingebaut wird.
- **KnowledgeStore**: rein In-Memory, geht beim Neustart verloren.

## Erste Schritte danach

1. Gradle Wrapper erzeugen, falls nicht vorhanden: `gradle wrapper` (benötigt lokal installiertes Gradle einmalig)
2. `git init && git add . && git commit -m "Initial commit"`
3. Auf GitHub ein leeres Repo anlegen, dann:
   ```bash
   git remote add origin git@github.com:DEIN-USER/DEIN-REPO.git
   git branch -M main
   git push -u origin main
   ```
