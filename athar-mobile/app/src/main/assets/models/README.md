# On-device models

Place offline speech models here:

| File | Role |
|---|---|
| `whisper-small-ar.onnx` | Arabic ASR (Whisper tiny/small ONNX) |
| `whisper-small-ar.ggml` | Alternative whisper.cpp binary |

`OnDeviceWhisperAsr` copies the asset into app files and runs inference on-device.

Matching / grading never uses these models — only transcription.
Without a file, the app still runs: paste text and match offline against the bundled corpus.
