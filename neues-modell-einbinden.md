# Rezept: Neues Modell in den Stack einbinden

Stack: `LibreChat -> eigene App -> Bifrost -> llama.cpp (ggf. OpenRouter)`

---

## 1. Recherche (vor dem Download)

- [ ] **HF-Modellkarte lesen** (README/Blogpost der Familie) – auf folgende Punkte achten:
  - Unterstützt das Modell eine System-Rolle, oder wird die in den ersten User-Turn gemerged?
  - Gibt es einen eingebauten Reasoning/Thinking-Modus? Falls ja: wie wird er deaktiviert (`/no_think`, `enable_thinking`, `--reasoning-budget`)?
  - Wird Tool-/Function-Calling unterstützt, und welches Format (Hermes-Style, native JSON, etc.)?
  - Empfohlene Kontextlänge vs. maximale (YaRN/RoPE-Skalierung nötig, um über die Trainingslänge zu gehen?)
- [ ] **`generation_config.json`** aus dem Original-HF-Repo (nicht dem GGUF-Quant!) ziehen – enthält die vom Hersteller empfohlenen Sampling-Defaults:
  ```
  https://huggingface.co/<org>/<model>/blob/main/generation_config.json
  ```
  Notieren: `temperature`, `top_p`, `top_k`, `repetition_penalty`, `min_p` (falls vorhanden).
- [ ] **Lizenz prüfen** (Apache 2.0 / MIT vs. Custom-Lizenz mit Nutzungsbeschränkungen, z. B. Llama Community License, Gemma Terms of Use) – relevant falls das Modell in irgendeiner Form produktiv/extern genutzt wird.

---

## 2. GGUF besorgen & verifizieren

- [ ] GGUF von vertrauenswürdiger Quelle laden (offizielles Repo der Familie bevorzugt, sonst bekannte Quantizer wie Bartowski/unsloth/mradermacher).
- [ ] Metadata gegen Recherche aus Schritt 1 prüfen:
  ```bash
  python gguf-py/scripts/gguf_dump.py model.gguf
  # oder
  llama-gguf model.gguf
  ```
  Checken:
  - [ ] `tokenizer.chat_template` vorhanden und plausibel (richtige Family erkennbar, z. B. ChatML `<|im_start|>` vs. Llama `<|begin_of_text|>` vs. Gemma `<start_of_turn>`)
  - [ ] `tokenizer.ggml.eos_token_id` / EOT-Token korrekt gesetzt (häufige Quelle für "Modell labert nach Antwort weiter")
  - [ ] `*.rope.freq_base` / `*.rope.scaling.*` passt zur beworbenen Kontextlänge
- [ ] Quant-Stufe wählen (Q4_K_M als Standard-Startpunkt, bei kleinen Modellen ggf. Q6_K/Q8_0 wenn VRAM übrig ist).

---

## 3. llama.cpp Preset (`.ini`) anlegen

- [ ] Neue Preset-Datei in `/srv/fast/BIN` nach bestehendem Muster (r9700 / p100 / split) anlegen.
- [ ] Modell in passendes Verzeichnis legen: `/srv/fast/models/r9700/` **oder** `/srv/fast/models/p100/` (nicht in beide – sonst Random-Load-Balancing-Bug wie bei OpenWebUI erlebt).
- [ ] `--jinja` aktivieren, damit das eingebettete Chat-Template greift.
- [ ] Sampling-Defaults aus Schritt 1 übernehmen (nicht blind alte Presets einer anderen Familie recyclen).
- [ ] Falls Reasoning-Modell und Reasoning unerwünscht:
  ```
  --reasoning-budget 0
  --chat-template-kwargs '{"enable_thinking": false}'
  ```
  → **Testen, nicht annehmen** – bei manchen Modellen/Quants wirkungslos (siehe MiniMax-M2.5-Fall).
- [ ] Kontextgröße setzen (`n_ctx_train` als Obergrenze, KV-Cache-Quantisierung `q8_0` falls VRAM knapp), Reserve-MiB analog zu bestehenden Presets.
- [ ] Modell lokal starten und Endpoint `/v1/models` prüfen – korrekte Modell-ID sichtbar?

---

## 4. Smoke-Test direkt gegen llama.cpp

- [ ] Einfache Chat-Completion per `curl` gegen `*.am5.hergeth.cloud`, System-Prompt + kurze User-Frage:
  - [ ] Antwort inhaltlich sinnvoll?
  - [ ] Kein ungewolltes `<think>`-Leaking im Output?
  - [ ] Stop-Token greift (Antwort endet sauber, kein Weiterlabern)?
  - [ ] Falls Tool-Calling benötigt: Tool-Call-Response im erwarteten Format?

---

## 5. Bifrost einbinden

- [ ] Provider/Modell in Bifrost-Config ergänzen (Modell-ID exakt wie in llama.cpp `/v1/models`).
- [ ] Falls Wildcard-Modell-IDs für Custom-Provider genutzt werden: explizit testen, ob Routing greift (bekannter Bifrost-Stolperstein).
- [ ] Virtual Key(s) prüfen – muss der neue Modellzugriff für bestehende Keys (LibreChat, eigene App, OpenCode) freigeschaltet werden, oder eigener Key?
- [ ] SSRF-Check: `allow_private_network: true` falls noch nicht global gesetzt.
- [ ] Test-Request über Bifrost (nicht direkt llama.cpp) wiederholen – Ergebnis muss identisch zu Schritt 4 sein.

---

## 6. Eigene App / Middleware

- [ ] Prüfen, ob System-Prompt-Konventionen der App zum neuen Modell passen (manche Modelle ignorieren/mergen System-Rolle – siehe Schritt 1).
- [ ] Token-Budget-Berechnungen (falls vorhanden) neu kalibrieren – anderer Tokenizer = andere Tokenanzahl für denselben Text.
- [ ] Falls Routing-Logik (z. B. "welches Modell für welche Aufgabe") betroffen ist: neues Modell dort eintragen.

---

## 7. LibreChat

- [ ] Modell erscheint in der Modellliste (ggf. LibreChat-Cache/Config neu laden).
- [ ] Test-Chat inkl. Titel-Generierung (nicht vergessen: `titleModel` sollte weiterhin auf ein sinnvolles Modell zeigen, siehe frühere Fix-Historie).
- [ ] Falls Scraper/Tools (CRW) im Chat genutzt werden: Kompatibilität mit dem neuen Modell kurz gegentesten.

---

## 8. Abschluss-Doku

- [ ] Kurznotiz pro Modell (z. B. in der Preset-`.ini` als Kommentar oder separates Notizfile) mit:
  - Quelle des GGUF + Quant-Stufe
  - Sampling-Defaults (Quelle: `generation_config.json`)
  - Reasoning-Verhalten (an/aus, wie deaktiviert, funktioniert es zuverlässig?)
  - Tool-Calling: ja/nein, Parser-Typ
  - Bekannte Eigenheiten/Bugs

---

## Schnellreferenz: Wo finde ich was?

| Info | Quelle |
|---|---|
| Chat-Template, EOS/EOT, RoPE-Settings | GGUF-Metadata (`gguf_dump.py`) |
| Sampling-Defaults (temperature, top_p, ...) | `generation_config.json` im Original-HF-Repo |
| System-Prompt-Verhalten, Tool-Support, Reasoning-Steuerung | README/Blogpost der Modellfamilie |
| Unterstützte Chat-Templates/Tool-Parser in llama.cpp | `llama-server --help` / `common/chat.cpp` im llama.cpp-Repo |
| Praxis-Hinweise, bekannte Bugs bei Quants | Modellkarte des Quant-Erstellers (Bartowski, unsloth, mradermacher) |
