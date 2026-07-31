# Chat-Szenarien für JChat

YAML-Dateien in diesem Ordner definieren mehrstufige Chat-Verläufe, die automatisch
gegen eine laufende JChat-Instanz abgespielt werden. Nach jedem Turn wird der
Knowledge Store ausgelesen und als JSON gespeichert.

## Szenario-Format

```yaml
name: mein-szenario              # Dateiname ohne .yaml empfohlen
conversationId: test-mein-szenario
model: ollama-main               # optional
description: Kurzbeschreibung    # optional
turns:
  - "Erste User-Nachricht"
  - "Zweite User-Nachricht"
```

## Erwartungen (optional, für Tests)

Datei `mein-szenario.expected.yaml`:

```yaml
mustContain:
  - subject: Anna
    predicate: wohnt_in
    object: Hamburg
minStatements: 5
```

`object`-Vergleich ist flexibel (Substring, case-insensitive).

## Ausführen

JChat muss laufen (`./gradlew run`):

```bash
./gradlew runScenarios

# Mit Validierung gegen *.expected.yaml:
./gradlew runScenarios --args='--validate'

# Andere URL:
./gradlew runScenarios --args='--base-url http://am5:8080 --validate'
```

## Output

Unter `build/scenario-runs/`:

| Datei | Inhalt |
|---|---|
| `summary.json` | Alle Szenario-Ergebnisse |
| `{name}.result.json` | Turns, Antworten, Store pro Turn |
| `{name}.store.json` | Finaler Knowledge Store |

Daraus lassen sich später JUnit-Tests generieren oder Golden Files pflegen.
