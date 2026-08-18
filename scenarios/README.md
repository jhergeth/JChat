# Chat-Szenarien für JChat

YAML-Dateien in diesem Ordner definieren mehrstufige Chat-Verläufe, die automatisch
gegen eine laufende JChat-Instanz abgespielt werden. Nach jedem Turn wird der
Knowledge Store ausgelesen und als JSON gespeichert.

## Szenario-Format

```yaml
name: mein-szenario              # Dateiname ohne .yaml empfohlen
conversationId: test-mein-szenario
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

`object`-Vergleich ist flexibel (Substring, Umlaute, ohne Leerzeichen). Subject und Predicate
tolerieren Kurzformen und typische Extraktions-Varianten (siehe `TripleMatcher`).

## Ausführen

JChat muss laufen (`./gradlew run`):

```bash
./gradlew runScenarios

# Nur ein Szenario (Dateiname ohne .yaml oder YAML-Feld name):
./gradlew runScenarios --args='--scenario web-search'

# Mit Validierung gegen *.expected.yaml:
./gradlew runScenarios --args='--validate'

# Optional: LLM-Fallback für Triples, die regelbasiert nicht matchen:
./gradlew runScenarios --args='--validate --semantic-validate'

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
