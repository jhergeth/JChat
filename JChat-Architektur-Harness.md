# JChat – Architekturprinzipien für Harness, LLM-Protokolle und Tools

## Zielbild

JChat soll **selbst der steuernde LLM-Harness** bleiben. Externe Frameworks und Bibliotheken sollen nur Basismechanik und Protokolle liefern, aber möglichst keine Architekturentscheidungen übernehmen.

Die zentrale Leitregel lautet:

> **LangChain4j darf JChat Mechanik abnehmen, aber möglichst keine Entscheidungen.**

Damit bleiben die für unsere Experimente interessanten Teile sichtbar und kontrollierbar: Kontextaufbau, Memory, Statement-Extraktion, Retrieval, Tool-Policies und Agent-Loops.

---

## Schichtenmodell

```text
┌───────────────────────────────────────┐
│                JChat                  │
│          Micronaut Application        │
│                                       │
│  Conversation State                   │
│  ContextManager                       │
│  Statement Extraction                 │
│  Memory / Knowledge Store             │
│  Retrieval / Relevance                │
│  Tool Registry / Policies             │
│  Agent Loop                           │
│  Logging / Observability              │
└───────────────────┬───────────────────┘
                    │
┌───────────────────▼───────────────────┐
│             LangChain4j               │
│                                       │
│  LLM API Adapter                      │
│  Messages / Streaming                 │
│  Tool-Calling-Datentypen              │
│  Structured Output                    │
│  OpenAI-kompatible Protokolle         │
│  ggf. MCP-Clientmechanik              │
└───────────────────┬───────────────────┘
                    │
┌───────────────────▼───────────────────┐
│      Gateway / Inference Server       │
│                                       │
│  Bifrost / OmniRoute                  │
│  llama.cpp / vLLM / Provider API      │
└───────────────────┬───────────────────┘
                    │
                    ▼
                   LLM
```

Die Zuständigkeiten lassen sich einfach formulieren:

- **JChat:** Was soll passieren?
- **LangChain4j:** Wie kommuniziere ich mit dem Modell?
- **llama.cpp / vLLM / Provider:** Wie wird mit dem konkreten Modell gesprochen?

---

## Was LangChain4j übernehmen soll

LangChain4j ist für JChat primär eine **Protokoll- und Adapterbibliothek**.

Sinnvolle Aufgaben:

- OpenAI-kompatible Modellaufrufe
- Message-Serialisierung
- Streaming
- Tool-/Function-Calling-Datenstrukturen
- Rückgabe von Tool-Ergebnissen
- Structured Output / JSON-Schemas
- Provider-Abstraktion
- ggf. MCP-Protokoll und Transport

Dadurch muss JChat keine HTTP- und Protokolldetails selbst implementieren.

---

## Was bewusst in JChat bleiben soll

Folgende Bereiche sollen zunächst **nicht** an fertige LangChain4j-Agent-, Memory- oder RAG-Komponenten delegiert werden:

- Conversation State
- Auswahl und Aufbau des Modellkontexts
- Kontextreduktion / Pruning
- Statement Extraction
- Persistenz von Statements
- Knowledge Store
- Retrieval
- Relevanzbewertung
- Vergessen / Alterung von Wissen
- Entscheidung, wann Tools angeboten werden
- Entscheidung, ob ein Tool Call ausgeführt werden darf
- Verarbeitung und Persistenz von Tool-Ergebnissen
- Agent Loop und Abbruchbedingungen

Insbesondere sollten wir vorerst vermeiden, zentrale Funktionen direkt durch fertige Konstrukte wie `ChatMemory`, automatische RAG-Pipelines oder vollständige Agent-Frameworks zu ersetzen.

Sonst verlieren wir genau die Kontrolle über die Mechanismen, die JChat untersuchen soll.

---

## Tool Calling und MCP

Tool Calling und MCP liegen auf unterschiedlichen Ebenen:

```text
LLM
 │
 │ Tool / Function Calling
 ▼
JChat
 │
 │ MCP
 ▼
MCP Server
```

Das Modell spricht normalerweise **kein MCP**.

JChat bzw. LangChain4j präsentiert dem Modell Tools im vom LLM-Endpoint erwarteten Tool-Calling-Format. Wird ein Tool ausgewählt, führt JChat den Aufruf aus. Ein externes Tool kann dabei über MCP angesprochen werden.

Damit können lokale Java-Tools und MCP-Tools im JChat-Kern vereinheitlicht werden.

Eine mögliche interne Abstraktion:

```java
interface JChatTool {
    ToolDefinition definition();

    ToolResult execute(ToolArguments arguments);
}
```

Adapter darunter:

```text
JChatTool
   ├── JavaToolAdapter
   ├── McpToolAdapter
   └── später OpenApiToolAdapter
```

Nach oben wird daraus eine Tooldefinition für LangChain4j bzw. das Modell:

```text
JChat Tool Registry
        │
        ▼
LangChain4j Tool Specification
        │
        ▼
OpenAI Tool Calling
        │
        ▼
LLM
```

Der JChat-Kern hängt damit weder direkt von MCP noch von einem bestimmten Tool-Calling-Protokoll ab.

---

## Bedeutung für lokale Modelle

Bei lokalen Modellen entsteht eine zusätzliche Übersetzungsschicht:

```text
JChat
  │
  │ OpenAI-kompatible API
  ▼
LangChain4j / Gateway
  │
  ▼
llama.cpp / vLLM
  │
  │ Chat Template + Tool Parser
  ▼
Qwen / Llama / Gemma / ...
```

JChat muss die modellspezifische Syntax nicht kennen.

Der Inference Server übersetzt zwischen dem OpenAI-kompatiblen API-Vertrag und der vom jeweiligen Modell trainierten Chat-/Tool-Syntax.

Dadurch kann JChat weitgehend unabhängig vom eingesetzten Modell bleiben.

---

## Architekturprinzip

Die aktuelle Rollenverteilung lässt sich auf einen Satz reduzieren:

> **Micronaut ist das Application Framework, LangChain4j ist der LLM-/Protokolladapter, JChat selbst ist der Harness.**

Diese Trennung erlaubt später, einzelne Komponenten auszutauschen, ohne die Kernlogik neu zu bauen.

Vor allem bleiben die Forschungs- und Entwicklungsfragen dort, wo sie hingehören: in JChat selbst – insbesondere Context Management, Memory, Knowledge Store, Retrieval und kontrollierte Agent-Loops.
