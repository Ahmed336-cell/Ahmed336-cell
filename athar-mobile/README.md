# Athar Mobile — on-device ledger

Android port of [athar-ledger](https://github.com/Ahmedanwernouser/athar-ledger): find Qur’an verses and hadith in text **on the phone**, with optional **on-device Whisper** for speech.

Live web app stays at https://athar-ledger.pages.dev — this repo folder is the mobile path.

## What runs where

| Piece | Mobile |
|---|---|
| Detection / matching | Kotlin engine on-device (deterministic, no LLM verdicts) |
| Corpus pack | Full Qur’an + Nawawi 40 + Qudsi 40 + 40 short Bukhari (~1 MB) |
| Speech → text | Whisper ONNX/ggml hook (`assets/models/`) — optional |
| Ask Athar / grades | Lookup + copied grades only; app never fatwas |

Scholarly rules kept from upstream:

- No rulings; grades copied with scholar names when present.
- “Verbatim” = counted word agreement, not a model opinion.
- “Not found” = missing from the loaded pack, not a verdict.

## Open in Android Studio

1. Open the `athar-mobile/` folder.
2. Sync Gradle (AGP 8.7 / Kotlin 2.0 / minSdk 26).
3. Run on a device or emulator.
4. Paste text → **اسأل أثَر** or **حلّل النص**. Tap **جرّب مثالًا** for a demo lecture snippet.

### Optional offline ASR

Put `whisper-small-ar.onnx` (or ggml) under `app/src/main/assets/models/`.  
`OnDeviceWhisperAsr` loads it; wire ONNX Runtime / whisper.cpp in that class. Without the file, typed text still works fully offline.

## Modules

```
athar-mobile/
  engine/     pure Kotlin matcher + ArabicText (JVM-testable)
  app/        Jetpack Compose UI + asset corpus + ASR hook
  scripts/    rebuild mobile corpus from upstream athar-ledger
```

## Rebuild corpus pack

```bash
# clone or point at athar-ledger checkout
python3 scripts/build_mobile_pack.py /path/to/athar-ledger/public/data
```

## Engine tests (no Android SDK)

```bash
cd athar-mobile
./gradlew :engine:test
```

## Limits vs the website

- Mobile pack is smaller than the 9 collections + book packs on the site.
- Lecture scanning uses a lighter window matcher than the full browser engine.
- YouTube / Groq proxy paths are not in this MVP; ASR is local-first.

Upstream detection quality numbers and licences: see athar-ledger `docs/`.
