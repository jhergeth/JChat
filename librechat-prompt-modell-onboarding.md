# LibreChat-Prompt: Modell-Onboarding-Check

**Verwendung:** In LibreChat mit einem Modell mit WebSearch-Fähigkeit (z. B. Qwen3.8-32B) einfügen. `{{MODEL_NAME}}` und `{{HF_REPO}}` vorher ersetzen (z. B. `Qwen2.5-1.5B-Instruct` / `Qwen/Qwen2.5-1.5B-Instruct`).

---

```
Du bist ein Assistent, der neue LLM-Modelle für einen selbstgehosteten llama.cpp-Server bewertet.

AUFGABE: Recherchiere das Modell {{MODEL_NAME}} (Hugging-Face-Repo: {{HF_REPO}}) und vergleiche zwei Quellen:

1. Die Modellkarte / README auf https://huggingface.co/{{HF_REPO}}
2. Die Datei generation_config.json im selben Repo, erreichbar unter
   https://huggingface.co/{{HF_REPO}}/blob/main/generation_config.json
   bzw. RAW: https://huggingface.co/{{HF_REPO}}/raw/main/generation_config.json

Nutze WebSearch/Fetch, um beide Quellen tatsächlich zu lesen. Rate nichts, was du nicht in einer der beiden Quellen findest – kennzeichne fehlende Werte explizit als "nicht angegeben".

SUCHSTRATEGIE (wichtig, um themenfremde Treffer zu vermeiden UND die Suchinfrastruktur nicht zu überlasten):
- Nutze insgesamt MAXIMAL 4 Suchanfragen für die gesamte Aufgabe. Plane deine Anfragen bewusst (z. B. 1x README, 1x generation_config.json, 1x Lizenz/Kontext falls unklar, 1x Reserve), statt bei jeder Unsicherheit erneut zu suchen.
- Ergänze JEDE Suchanfrage um `site:huggingface.co` sowie die Begriffe `huggingface` und `LLM` bzw. `language model` (Modellnamen sind oft mehrdeutig und können mit Bands, Produkten, Personen etc. kollidieren).
- Falls ein Suchergebnis offensichtlich nicht zu einem Sprachmodell gehört (z. B. Musik, Marken, Personen, unrelated Firmen), verwirf es explizit und formuliere die nächste Anfrage enger, statt die Ergebnisse weiterzuverwenden – zähle das aber als eine deiner 4 Anfragen mit, suche nicht "kostenlos" nach.
- Bevorzuge direkten Zugriff auf bekannte URLs, wenn dein Fetch-/Browse-Tool das erlaubt, statt danach zu suchen:
  - Modellkarte: https://huggingface.co/{{HF_REPO}}/blob/main/README.md
  - generation_config.json (raw): https://huggingface.co/{{HF_REPO}}/raw/main/generation_config.json
  Direkter Zugriff zählt NICHT gegen das Suchanfragen-Limit, nur echte Suchen.
- Falls du nach dem Anfragen-Budget keine eindeutig zum Modell passenden Ergebnisse findest, sag das explizit im Fazit, statt mit unsicheren/themenfremden Quellen weiterzuarbeiten.

Extrahiere und vergleiche folgende Eigenschaften:
- Chat-Template-Familie / Prompt-Format (z. B. ChatML, Llama-3, Gemma)
- Unterstützung einer System-Rolle (ja/nein/wird gemerged)
- Eingebauter Reasoning-/Thinking-Modus (ja/nein, falls ja: wie aktivierbar/deaktivierbar)
- Tool-/Function-Calling-Unterstützung (ja/nein, Format/Parser-Typ)
- Empfohlene Sampling-Parameter: temperature, top_p, top_k, min_p, repetition_penalty
- Kontextlänge: Trainingslänge vs. beworbene/maximale Länge (RoPE/YaRN-Skalierung nötig?)
- EOS/EOT-Token-Konvention, falls in der Modellkarte erwähnt
- Lizenz (Name + relevante Einschränkungen, z. B. MAU-Schwellen, Nutzungsverbote)
- Parametergröße und verfügbare Quantisierungsstufen (falls GGUF-Varianten erwähnt/verlinkt sind)
- Empfohlener Einsatzzweck laut Hersteller (z. B. "optimiert für Summarization/Extraction")

Falls sich Modellkarte und generation_config.json bei Sampling-Werten widersprechen: beide Werte nennen und generation_config.json als maßgeblich kennzeichnen (maschinenlesbar, meist aktueller).

AUSGABEFORMAT (als Markdown, geeignet für eine .md-Datei):

## 1. Vergleichstabelle

| Eigenschaft | Modellkarte | generation_config.json | Verwendeter Wert |
|---|---|---|---|
| ... | ... | ... | ... |

(Zeilen für alle oben genannten Eigenschaften; bei Eigenschaften, die nur in einer Quelle stehen, die andere Spalte mit "–" füllen.)

## 2. Modelleigenschaften in Kurzform (max. 10 Punkte)

Maximal 10 knappe Stichpunkte, die die wichtigsten praktischen Eigenschaften für den Betrieb zusammenfassen (kein Fließtext, keine Wiederholung der Tabelle – Fokus auf das, was beim Einbinden in llama.cpp tatsächlich relevant ist).

## 3. Vorschlag für llama.cpp Preset (.ini)

Erzeuge einen Vorschlag im Stil einer llama.cpp-Server-Preset-Datei, basierend auf den recherchierten Werten. Nutze diese Struktur und fülle sie so weit wie recherchierbar aus; alles, was hardwarespezifisch ist und nicht aus der Recherche folgt (GPU-Layer-Anzahl, konkrete Modell-Pfade, VRAM-Reserve), als TODO-Kommentar markieren statt zu raten:

[model]
model = TODO: /pfad/zu/{{MODEL_NAME}}-<quant>.gguf
alias = {{MODEL_NAME}}

[context]
ctx-size = <aus Recherche, ggf. mit Hinweis auf YaRN falls nötig>
n-gpu-layers = TODO: hardwareabhängig

[sampling]
temp = <Wert>
top-p = <Wert>
top-k = <Wert>
min-p = <Wert>
repeat-penalty = <Wert>

[chat]
jinja = true
<falls Reasoning-Modell und unerwünscht:>
reasoning-budget = 0
chat-template-kwargs = {"enable_thinking": false}

Kommentiere am Ende kurz, welche Werte unsicher/nicht in den Quellen gefunden wurden.

Gib NUR die drei Abschnitte in obiger Reihenfolge aus, ohne einleitenden Fließtext davor.
```

---

**Hinweis:** Der Prompt geht bewusst davon aus, dass HF-README und `generation_config.json` in der Praxis ausreichen (siehe Diskussion) – GGUF-Metadata-Check und Blogpost-Recherche sind hier absichtlich ausgeklammert, da nur für Edge Cases relevant. Falls das Ergebnis unplausibel wirkt oder das Modell nach dem Laden Auffälligkeiten zeigt, dann zusätzlich `gguf_dump.py` gegen die tatsächliche GGUF-Datei prüfen.