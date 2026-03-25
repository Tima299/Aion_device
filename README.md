# AION Device

**AION Device** is an Android app that runs a local LLM directly on your phone using **MediaPipe LLM Inference**.
No cloud API is required once a compatible model file is on the device.

---

## Why this project exists

Most mobile AI demos are either:
- cloud-first (privacy tradeoff), or
- emulator-heavy (unreliable for real model behavior).

AION Device is opinionated around a practical goal:

> **Import a model, verify readiness, and chat privately on-device with streaming responses.**

---

## Core capabilities

- **Private local chat** (on-device inference, no server round-trip).
- **Model import flow** from Android file picker (`.task` recommended, legacy `.bin` also supported).
- **Automatic model discovery** across common internal/external app directories.
- **Streaming token output** with cancel support.
- **Session metrics** visible in-app:
  - estimated prompt tokens
  - response character count
  - last response latency
- **Clean Compose architecture** with separate model/runtime/state/UI layers.

---

## App structure (project-specific)

### 1) UI shell
- `MainActivity` sets Compose content with `AionNav`.
- `AionApp` provides a 3-tab experience:
  - **Chat** — conversation, send/cancel, clear history
  - **Status** — model readiness, import/rescan, runtime snapshot
  - **Profile** — implementation achievements + avatar setup notes

### 2) State + orchestration
- `OnDeviceViewModel` owns all user-driven flows:
  - model scan/refresh
  - import from `Uri`
  - prompt send
  - stream handling (`Chunk`/`Failure` events)
  - cancellation

### 3) Inference runtime
- `LlmInferenceManager` wraps MediaPipe objects:
  - initializes `LlmInference` + `LlmInferenceSession`
  - dispatches streaming callbacks
  - exposes a shared event stream
  - supports cancellation and disposal

### 4) Model strategy
- `ModelFileLocator` ranks likely model files and picks the best available candidate.
- Preference is given to `.task` files and larger valid files.

### 5) Prompt shaping
- `PromptComposer` applies an AION system prompt and last-message windowing for concise context.

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** ViewModel + `StateFlow`
- **Inference engine:** `com.google.mediapipe:tasks-genai`
- **Build:** Gradle (Kotlin DSL)
- **Android target:**
  - `minSdk = 24`
  - `targetSdk = 34`
  - Java/Kotlin target = 17

---

## Getting started

### Prerequisites

- Android Studio (recent stable)
- Android SDK 34
- A physical Android device is strongly recommended for realistic on-device inference behavior
- A compatible MediaPipe LLM model file (`.task` preferred)

### Run the app

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` module on your device.

### Install a model (two options)

#### Option A — in-app import (recommended)
1. Open **Status** tab.
2. Tap **Import model**.
3. Choose a local model file.
4. Tap **Rescan** if needed.

#### Option B — ADB push (fast dev loop)
```bash
adb push model_version.task /sdcard/Android/data/com.example.aion_device/files/models/model_version.task
```
Then open **Status** and tap **Rescan**.

---

## Model compatibility notes

Current discovery logic checks common names such as:
- `model_version.task`
- `gemma-3-1b-it-int4.task`
- `gemma-3n-e2b-it-int4.task`
- `gemma-2b-it-cpu-int4.bin`
- `gemma2-2b-it-cpu-int4.bin`

It also scans the broader app model directories and selects viable files above a minimum size threshold.

---

## Developer map

```text
app/src/main/java/com/example/aion_device/
├── llm/
│   ├── LlmInferenceManager.kt
│   └── ModelFileLocator.kt
├── model/
│   ├── ChatMessage.kt
│   ├── InferenceConfig.kt
│   ├── InferenceStats.kt
│   └── ModelInfo.kt
├── navigation/
│   └── AionNav.kt
├── ui/
│   ├── AionApp.kt
│   ├── components/
│   └── screens/
├── util/
│   └── PromptComposer.kt
├── viewmodel/
│   ├── OnDeviceUiState.kt
│   └── OnDeviceViewModel.kt
└── MainActivity.kt
```

---

## Troubleshooting

### “No compatible model found”
- Confirm the file is fully copied (not partial).
- Prefer `.task` bundles.
- Re-open **Status** and tap **Rescan**.

### “Initialization failed”
- Model may be incompatible with the current runtime.
- Try another known-good `.task` model.

### Generation does not start
- Ensure **Status** reports model as installed.
- Check that the model path is populated and file size is realistic.

---

## Roadmap ideas

- Persist chat history (Room/DataStore)
- Multi-session prompt profiles
- Better token/speed telemetry panels
- Download manager UI with progress for remote model URLs
- Optional quantization/profile presets

---

## License

No license file is currently included in this repository.
Add one if you plan to distribute the project.
