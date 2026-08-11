# JChat – Gap-Backlog (gegen Architektur-Harness)

Abgeleitet aus [`JChat-Architektur-Harness.md`](../JChat-Architektur-Harness.md) und dem Ist-Stand im Code (Stand: Aug 2026).

**Legende:** P0 = kritisch für Harness-Kern, P1 = wichtig für Architektur-Vollständigkeit, P2 = Qualität/Infrastruktur, P3 = später / Forschung

---

## Priorität 0 – Harness-Kern schließen

### JCHAT-001: ContextManager als explizite Schicht einführen

**Gap:** Kontextlogik ist über `ContextWindowPromptBuilder`, `ConversationTurns` und `SystemPromptProvider` verteilt; im Harness als eigene Komponente vorgesehen.

**Ziel:** Eine Klasse `ContextManager` (Package `name.hergeth.jchat.ai`) kapselt:
- System-Prompt laden
- Retrieved Facts einbinden
- Turn-Fenster wählen (`app.context.recent-turns`)
- Ausgabe als `List<ChatMessage>` für `AiServiceFactory`

**Betroffene Dateien:**
- Neu: `src/main/java/name/hergeth/jchat/ai/ContextManager.java`
- Refactor: `ContextWindowPromptBuilder.java` → delegiert an `ContextManager` oder wird ersetzt
- Anpassen: `ChatCompletionsController.java`

**Akzeptanzkriterien:**
- [ ] Controller ruft nur noch `contextManager.build(...)` auf
- [ ] Bestehende Szenarien (`./gradlew runScenarios --args='--validate'`) grün
- [ ] Debug-UI zeigt unverändert Prompt-Snapshot

---

### JCHAT-002: Conversation State serverseitig (optional pro Request)

**Gap:** Chat-Verlauf kommt vollständig vom Client; JChat speichert nur Knowledge, nicht Messages.

**Ziel:** Pro `conversation_id` Turn-Historie im Harness halten, damit Context-Experimente unabhängig vom Client laufen.

**Vorschlag:**
- Interface `ConversationStore` mit `append(conversationId, messages)`, `history(conversationId)`
- Implementierung `InMemoryConversationStore` (analog `InMemoryKnowledgeStore`)
- Request-Flag oder Config: `app.conversation.client-owned: true|false` (Default: true für Open-WebUI-Kompatibilität)

**Betroffene Dateien:**
- Neu: `ConversationStore.java`, `InMemoryConversationStore.java`
- Anpassen: `ChatCompletionsController.java`, `application.yml`

**Akzeptanzkriterien:**
- [ ] Bei `client-owned: false` reicht eine einzelne User-Message; Server baut Historie
- [ ] Open-WebUI-Pfad (`client-owned: true`) unverändert
- [ ] Szenario mit `conversationId` nutzt serverseitige Historie wenn konfiguriert

---

### JCHAT-003: Wissens-Alterung / Vergessen

**Gap:** Harness nennt „Vergessen / Alterung“; aktuell nur Merge + hartes Limit (`MAX_STATEMENTS = 12` in `TurnProcessor`).

**Ziel:** Konfigurierbare Strategie zum Entfernen oder Abschwächen alter Facts.

**Vorschlag:**
- `Statement` um `createdAt` erweitern (falls nicht überall gesetzt) / `lastSeenAt`
- Interface `ForgettingPolicy` mit Implementierungen:
  - `CapForgettingPolicy` (bestehendes Limit)
  - `AgeForgettingPolicy` (älter als N Turns / N Minuten)
- Config: `app.knowledge.max-statements`, `app.knowledge.max-age-turns`

**Betroffene Dateien:**
- Neu: `ForgettingPolicy.java`, `CapForgettingPolicy.java`, `AgeForgettingPolicy.java`
- Anpassen: `TurnProcessor.java`, `application.yml`

**Akzeptanzkriterien:**
- [ ] Korrektur-Szenario (`corrections-profile`) validiert weiterhin
- [ ] Unit-Test: alte Facts werden nach Schwellwert entfernt
- [ ] Debug-UI zeigt Timestamps / Entfernungen

---

## Priorität 1 – LangChain4j-Adapter vervollständigen

### JCHAT-101: Streaming für `/v1/chat/completions`

**Gap:** Harness erwartet Streaming von LangChain4j; `stream: true` wird ignoriert.

**Ziel:** SSE-Response im OpenAI-Format bei `stream: true`.

**Vorschlag:**
- `AiServiceFactory.stream(provider, messages)` → `StreamingChatLanguageModel`
- Neuer Endpoint-Handler oder Branch in `ChatCompletionsController` mit `text/event-stream`
- Chunk-DTO analog OpenAI (`data: {"choices":[{"delta":{"content":"..."}}]}`)

**Betroffene Dateien:**
- Anpassen: `AiServiceFactory.java`, `ChatModelRegistry.java`, `ChatCompletionsController.java`
- Neu: `SseChatCompletionWriter.java` (optional)

**Akzeptanzkriterien:**
- [ ] `curl -N` mit `stream: true` liefert inkrementelle Tokens
- [ ] Open WebUI zeigt „flüssige“ Antwort
- [ ] Non-Streaming-Pfad unverändert

---

### JCHAT-102: Token-Usage aus LangChain4j durchreichen

**Gap:** `Usage(0, 0, 0)` ist hardcodiert.

**Ziel:** Prompt-/Completion-Tokens aus `ChatResponse` in `ChatCompletionResponse` mappen.

**Betroffene Dateien:**
- Anpassen: `AiServiceFactory.java`, `ChatCompletionsController.java`, ggf. `Usage.java`

**Akzeptanzkriterien:**
- [ ] Response enthält sinnvolle `usage`-Werte wenn Provider sie liefert
- [ ] Fallback auf 0 wenn Provider keine Metriken hat

---

### JCHAT-103: Structured Output für Triple-Extraktion (optional)

**Gap:** Harness nennt JSON-Schema; aktuell Zeilenformat `subject | predicate | object` + `StatementParser`.

**Ziel:** Parallel oder ersetzend JSON-Schema über LangChain4j Structured Output.

**Vorschlag:**
- Config-Schalter `app.extraction.format: lines|json`
- Bei `json`: LC4j `responseFormat` / Schema für `{ "statements": [...] }`
- `StatementParser` bleibt Fallback

**Betroffene Dateien:**
- Anpassen: `LlmStatementExtractor.java`, `StatementParser.java`, `extraction-prompt.txt`

**Akzeptanzkriterien:**
- [ ] Beide Formate bestehen Szenario-Validierung
- [ ] Kein LC4j-Agent-Framework, nur Structured Output API

---

## Priorität 1 – Tool Calling & MCP (Harness-Kapitel)

### JCHAT-201: JChatTool-Abstraktion und Registry

**Gap:** Harness skizziert `JChatTool`, Registry, Policies — im Code nur `ToolResult`-Record.

**Ziel:** Minimale Tool-Schicht in JChat (ohne MCP).

```java
interface JChatTool {
    ToolDefinition definition();
    ToolResult execute(ToolArguments arguments);
}
```

**Vorschlag:**
- `ToolRegistry` — registriert Tools, liefert LC4j-`ToolSpecification`-Liste
- `ToolPolicy` — entscheidet ob Tool angeboten/ausgeführt wird (z. B. allowlist)
- `JavaToolAdapter` — erste konkrete Implementierung (Echo- oder Knowledge-Lookup-Tool)

**Betroffene Dateien:**
- Neu: `src/main/java/name/hergeth/jchat/tools/` (Package)
- Anpassen: `ChatCompletionsController.java`, `AiServiceFactory.java`

**Akzeptanzkriterien:**
- [ ] Mindestens ein lokales Tool per Tool-Calling aufrufbar
- [ ] Tool-Ergebnis landet in `Turn.toolResults()` und im Debug-Trace
- [ ] Keine direkte MCP-Abhängigkeit im Kern

---

### JCHAT-202: Agent Loop (minimal)

**Gap:** Harness: „Agent Loop und Abbruchbedingungen“ — aktuell ein LLM-Call.

**Ziel:** Schleife: LLM → optional Tool Call → Ergebnis zurück ans Modell → repeat bis `stop` oder `maxSteps`.

**Vorschlag:**
- `AgentLoop` mit Config `app.agent.max-steps: 5`
- Integration in `ChatCompletionsController` hinter Feature-Flag `app.agent.enabled`

**Abhängigkeit:** JCHAT-201

**Akzeptanzkriterien:**
- [ ] Tool-Call → Execute → Follow-up-Message funktioniert end-to-end
- [ ] Abbruch bei max Steps ohne Endlosschleife
- [ ] Debug-UI zeigt alle Loop-Iterationen

---

### JCHAT-203: MCP-Tool-Adapter (Spike)

**Gap:** Harness: LLM → JChat → MCP Server.

**Ziel:** `McpToolAdapter implements JChatTool` — ein MCP-Server, ein Tool.

**Vorschlag:**
- LangChain4j MCP-Client (wenn verfügbar) oder schlanker HTTP-Client
- Config: `app.mcp.servers[].name`, `url`, `tools[]`
- Spike-Umfang: ein readonly-Tool (z. B. Datei lesen / API abfragen)

**Abhängigkeit:** JCHAT-201

**Akzeptanzkriterien:**
- [ ] MCP-Tool erscheint in Tool-Registry
- [ ] Ausführung im Agent Loop (JCHAT-202)
- [ ] Dokumentation in README: MCP vs. lokale Tools

---

## Priorität 2 – Persistenz & Retrieval

### JCHAT-301: Knowledge Store persistieren

**Gap:** `InMemoryKnowledgeStore` — Neustart verliert alles.

**Ziel:** Interface beibehalten, Implementierung `FileKnowledgeStore` oder SQLite.

**Vorschlag:**
- JSON-Datei pro `conversation_id` unter `data/knowledge/`
- Config: `app.knowledge.store: memory|file`, `app.knowledge.path`

**Betroffene Dateien:**
- Neu: `FileKnowledgeStore.java` oder `SqliteKnowledgeStore.java`
- Anpassen: Micronaut `@Requires` für Store-Auswahl

**Akzeptanzkriterien:**
- [ ] Neustart behält Knowledge für bekannte `conversation_id`
- [ ] Szenario-Runner Ergebnisse reproduzierbar nach Restart

---

### JCHAT-302: Retrieval verbessern (Embedding optional)

**Gap:** Harness will Retrieval in JChat; aktuell nur Keyword (`StatementRelevanceScorer`).

**Ziel:** Zweite Implementierung `EmbeddingRetriever` — Embeddings berechnen/speichern, Cosine-Similarity.

**Vorschlag:**
- Interface `Retriever` existiert bereits
- `@Replaces(RelevanceRetriever.class)` nur per Config aktivierbar
- Embedding-Modell über Ollama oder lokaler Service — **kein** LC4j-RAG-Pipeline-Framework

**Akzeptanzkriterien:**
- [ ] Config-Umschaltung `app.retriever.type: keyword|embedding`
- [ ] Paraphrase-Query findet relevante Facts besser als Keyword (manueller Test dokumentiert)

---

## Priorität 2 – Qualität & Infrastruktur

### JCHAT-401: CI-Workflow reparieren

**Gap:** `.github/workflows/llm-eval_001.yml` verweist auf fehlende `tests/` und `requirements.txt`; Runner-Label Platzhalter.

**Ziel:** Lauffähiger CI-Job auf self-hosted Runner.

**Vorschlag:**
- Phase 1: Gradle-Tests (`./gradlew test`) + Szenario-Validierung gegen gestarteten JChat
- Phase 2: DeepEval/Python in `eval/` verschieben mit eigenem `requirements.txt`

**Akzeptanzkriterien:**
- [ ] Workflow läuft auf echtem Runner-Label (z. B. `am5`)
- [ ] `./gradlew test` grün
- [ ] Kein toter Pfad `tests/` ohne Dateien

---

### JCHAT-402: Unit- und Integrationstests ausbauen

**Gap:** Nur `Step3ContextTest.java` (2 Tests); kein Controller-Test.

**Ziel:** Testpyramide für Harness-Kern.

| Test | Fokus |
|------|-------|
| `TurnProcessorTest` | Merge, Cap, Forgetting |
| `StatementParserTest` | Zeilenformat, Edge Cases |
| `ContextManagerTest` | Prompt-Zusammenbau |
| `RelevanceRetrieverTest` | Scoring, Fallback |
| `ChatCompletionsControllerTest` | Micronaut `@MicronautTest`, Mock LLM |

**Akzeptanzkriterien:**
- [ ] ≥ 15 Unit-Tests
- [ ] `./gradlew test` < 30s lokal
- [ ] Szenario-Expectations (`*.expected.yaml`) zusätzlich in CI

---

### JCHAT-403: Szenario-Ergebnisse als JUnit-Golden-Tests

**Gap:** `scenarios/README.md` erwähnt JUnit-Generierung — noch nicht umgesetzt.

**Ziel:** `./gradlew test` spielt Szenarien gegen `@MicronautTest`-Instanz oder Testcontainers.

**Vorschlag:**
- `ScenarioIntegrationTest` liest `scenarios/*.yaml` + `*.expected.yaml`
- Mock oder Test-Ollama-Provider für deterministische CI

**Abhängigkeit:** JCHAT-401

---

## Priorität 3 – Später / Forschung

### JCHAT-501: OpenAPI-Tool-Adapter

Harness erwähnt `OpenApiToolAdapter` — REST-APIs als Tools registrieren.

---

### JCHAT-502: Gateway-Abstraktion (Bifrost / OmniRoute)

Harness-Diagramm: Gateway-Schicht unter LangChain4j. Aktuell direkte Provider in `ChatModelRegistry`.

Ziel: Einheitliche `base-url` pro Gateway, Provider-Auswahl im Gateway statt in JChat.

---

### JCHAT-503: Multi-Agent / delegierte Sub-Tasks

Mehrere `TaskRouter`-Tasks existieren (`chat`, `extraction`, `meta`) — Erweiterung um parallele Spezial-Agenten mit eigenem Context.

---

## Empfohlene Reihenfolge (Roadmap)

```text
Sprint 1 (Harness-Kern festigen)
  JCHAT-001 → JCHAT-003 → JCHAT-402

Sprint 2 (Protokoll & Beobachtbarkeit)
  JCHAT-101 → JCHAT-102 → JCHAT-401

Sprint 3 (Tools beginnen)
  JCHAT-201 → JCHAT-202

Sprint 4 (Persistenz & Retrieval)
  JCHAT-301 → JCHAT-002 → JCHAT-302

Sprint 5 (MCP & Erweiterung)
  JCHAT-203 → JCHAT-103 → JCHAT-403
```

---

## Abdeckungsmatrix (Harness → Ticket)

| Harness-Bereich | Ticket(s) | Status |
|-----------------|-----------|--------|
| Conversation State | JCHAT-002 | offen |
| ContextManager | JCHAT-001 | offen |
| Statement Extraction | — | **erledigt** (`LlmStatementExtractor`) |
| Knowledge Store | JCHAT-301, JCHAT-003 | teilweise |
| Retrieval / Relevance | JCHAT-302 | teilweise (keyword) |
| Vergessen / Alterung | JCHAT-003 | offen |
| Tool Registry / Policies | JCHAT-201 | offen |
| Agent Loop | JCHAT-202 | offen |
| MCP | JCHAT-203 | offen |
| Logging / Observability | — | **erledigt** (Debug-UI) |
| LC4j Streaming | JCHAT-101 | offen |
| LC4j Tool Calling | JCHAT-201, JCHAT-202 | offen |
| LC4j Structured Output | JCHAT-103 | offen |
| Kein LC4j ChatMemory/RAG | — | **eingehalten** |

---

## Nicht-Ziele (bewusst)

- LangChain4j `ChatMemory`, `EmbeddingStore`-RAG-Pipeline oder Agent-Frameworks einführen
- Open WebUI ersetzen
- Vektordatenbank als Pflicht-Dependency

Diese Punkte widersprechen dem Harness-Prinzip: **Entscheidungen in JChat, Mechanik in LangChain4j.**
