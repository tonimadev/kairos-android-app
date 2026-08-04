<img src="/images/kairos-banner.png" width="1024" height="500">

[![Android CI - Kairos Multi-Module](https://github.com/tonimadev/kairos-android-app/actions/workflows/android-ci.yaml/badge.svg)](https://github.com/tonimadev/kairos-android-app/actions/workflows/android-ci.yaml) [![codecov](https://codecov.io/gh/tonimadev/kairos-android-app/graph/badge.svg?token=TKC92HM5VY)](https://codecov.io/gh/tonimadev/kairos-android-app)

# Kairós - Calendar Alarms for Android & Wear OS powered by Gemini AI

Kairós is a modern application that transforms your calendar appointments into unmissable full-screen alarms, both on your smartphone and on your wrist with **Wear OS**. It intelligently synchronizes with the device's calendar and ensures you never miss an important event.

## 🚀 Download

<a href='https://play.google.com/store/apps/details?id=digital.tonima.kairos' target="_blank" rel="noopener noreferrer"><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='200'/></a>

---

## ✨ Key Features

- **Full-Screen Alarms**: Wakes the device even when locked with visual and sound alerts (Smartphone and Wear OS).
- **Full Wear OS Integration**:
  - **Complications**: View the next event directly on the watch face.
  - **Tiles**: Instant access to event information with a side swipe.
- **Smart Synchronization**: Uses WorkManager for efficient background scheduling without draining the battery.
- **Native Integration**: Reads events from any calendar account configured on the device (e.g., Google Calendar).
- **AI-Powered Daily Summaries**: Intelligent briefing of your day, processed on-device (Gemini Nano) for privacy and speed.
- **AppFunctions Support**: Exposes core features to the Android system, allowing execution via voice commands or system shortcuts.
- **Total Control**: Enable or disable alarms globally or for specific events.

<p align="center">
  <img src="/images/watch1.png" width="200">
  <img src="/images/watch2.png" width="200">
  <img src="/images/tile.gif" width="300">
</p>

---

## 🛠 Tech Stack

This project follows modern Android development principles with MVVM + MVI architecture.

- **Language**: 100% Kotlin
- **UI**: Jetpack Compose (Phone & Wear OS)
- **Architecture**: MVVM + MVI, Clean Architecture, Multi-module
- **Async**: Kotlin Coroutines + Flow
- **Persistence & Background**: DataStore, Room, WorkManager, AlarmManager
- **DI**: Hilt
- **AI**: Firebase AI (Gemini Pro/Flash) & Gemini Nano (On-Device inference)
- **System**: Android AppFunctions (API 36+)
- **Sync**: Wearable Data Layer (Play Services)
- **Testing**: JUnit4, Robolectric, Turbine, MockK

---

## 🏗 Architecture

### Multi-Module Structure

```
kairos-android-app/
├── app/          → Phone UI (Compose), Activity, Receivers
├── core/         → Shared business logic, ViewModels, UseCases, Repositories
├── wear/         → Wear OS UI, Tiles, Complications
├── build-logic/  → Convention plugins (Jacoco, etc.)
└── gradle/       → Version catalog (libs.versions.toml)
```

### MVVM + MVI Pattern

The app uses **unidirectional data flow** across all features:

```
View (Compose) ──EventIntent──▶ ViewModel ──▶ UseCases / Repositories
       ▲                            │
       │                            ▼
       └──── UiState (StateFlow) ───┘
             SideEffect (Channel)
```

- **`EventIntent`** — sealed class representing every user action.
- **`EventScreenUiState`** — single immutable state driving the UI.
- **`EventSideEffect`** — one-shot events (snackbar, navigation, confirmation dialogs).
- **`EventViewModel`** — processes intents, delegates to UseCases, emits state & effects.

### Core Module Packages

| Package | Responsibility |
|---|---|
| `viewmodel` | ViewModels, Intents, UiState, SideEffects, UiText |
| `usecases` | Business logic (one class per action) |
| `repository` | Data access (Calendar, Weather, Preferences, etc.) |
| `model` | Domain entities (Event, AlarmOffset, Weather, etc.) |
| `service` | AlarmScheduler, Workers |
| `ai` | AI Agent architecture (see below) |
| `analytics` | Firebase Analytics abstraction |

---

## 🤖 AI Agent Architecture

The app integrates an **AI Agent** using a **hybrid approach**:
- **Cloud-hosted (Gemini Pro/Flash)**: Used for complex tasks like "Function Calling" (executing actions).
- **On-Device (Gemini Nano)**: Used for privacy-sensitive tasks like the **Daily Briefing**, utilizing the device's hardware for inference.

### Hybrid Inference Mode

Kairos uses `InferenceMode.PREFER_ON_DEVICE` for text generation tasks. This ensures:
1. **Privacy**: Sensitive data (like your schedule) stays on the device when summarized.
2. **Speed**: No network latency for local tasks.
3. **Fallback**: If the device doesn't support Gemini Nano, it automatically fails back to the Cloud model.

### How It Works (Tool Calling)

```text
User question ──▶ DB (ChatHistoryDao) ──▶ AskAiAgentUseCase (Gemini + Tool declarations)
                                                │
                              ┌─────────────────┴─────────────────┐
                              ▼                                   ▼
                        Text response                   FunctionCall response
                     (save to DB & show)                          │
                                                                  ▼
                                                        onAIFunctionCalled()
                                                                  │
                                                                  ▼
                                                    ActionRegistry.processAIToolCall()
                                                                  │
                                            ┌─────────────────────┼─────────────────────┐
                                            ▼                     ▼                     ▼
                                          SAFE                MODERATE               CRITICAL
                                       (execute)         (execute + snackbar)  (pause, ask user)
                                            │                     │
                                            └─────────────────────┘
                                                      │
                                           DB (Save FunctionResponse)
                                                      │
                                        (Loop back to AskAiAgentUseCase)
```

### Risk Levels

| Level | Behavior | Example |
|---|---|---|
| `SAFE` | Executes immediately, no feedback | `SearchTool` → search events |
| `MODERATE` | Executes immediately + snackbar notification | `ToggleGlobalAlarmsTool` → toggle alarms |
| `CRITICAL` | Pauses, saves intent in `pendingAIAction`, requires explicit user confirmation | `CreateEventTool` → create calendar event |

### Key Components

| Component | Role |
|---|---|
| `ChatHistoryDao` | Room database interface managing the offline chat history persistence. |
| `AITool` | Interface — each tool maps an LLM function call to an `EventIntent` |
| `ActionRegistry` | Singleton — discovers tools, dispatches function calls |
| `AskAiAgentUseCase` | Sends the prompt + tool declarations to Gemini (`model.startChat`), returns `Text` or `FunctionCall` |
| `RiskLevel` | Enum controlling execution policy (`SAFE`, `MODERATE`, `CRITICAL`) |
| `AIToolResult` | Sealed class wrapping dispatch results (`Success`, `ToolNotFound`, `InvalidArguments`) |

### How to Create a New AI Tool

**Step 1 —** Create a class implementing `AITool` in `core/.../ai/tools/`:

```kotlin
class MyNewTool @Inject constructor() : AITool {

    override val name = "my_new_action"

    override val description = "Describe when the LLM should call this tool."

    override val riskLevel = RiskLevel.SAFE // or MODERATE / CRITICAL

    override val parametersSchema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "param1" to mapOf("type" to "string", "description" to "…"),
        ),
        "required" to listOf("param1"),
    )

    override fun parseArguments(args: Map<String, Any?>): EventIntent? {
        val value = args["param1"]?.toString() ?: return null
        return EventIntent.SomeExistingIntent(value)   // reuse an MVI intent
    }
}
```

**Step 2 —** Register it in `AIToolsModule` (`core/.../ai/di/`):

```kotlin
@Provides @IntoSet @Singleton
fun provideMyNewTool(): AITool = MyNewTool()
```

**Step 3 —** Done. `ActionRegistry` discovers it automatically via Hilt multibinding, includes it in the Gemini tool declarations, and routes function calls through `onAIFunctionCalled()`.

> **Tip:** Choose `RiskLevel` carefully — `SAFE` executes silently, `MODERATE` shows a snackbar, and `CRITICAL` pauses for user confirmation.

---

## 📲 AppFunctions & System Integration

Kairos leverages the **Android AppFunctions** framework (available from Android 16+) to expose its capabilities as agentic tools directly to the OS. This allows system-level assistants (like Gemini on Android) to discover and execute app features without opening the UI.

### Exposed Functions

| Function | Description |
|---|---|
| `createEvent` | Allows the system to schedule new appointments on the user's calendar. |
| `getDailyBriefing` | Provides the AI-generated summary of the user's day to the system assistant. |

### Technical Setup
- **Metadata**: Capabilities are declared in `res/xml/app_metadata.xml`.
- **Hilt Integration**: `KairosApplication` implements `AppFunctionConfiguration.Provider` to allow the framework to inject dependencies into function implementations.

---

## 🏁 Getting Started

### Prerequisites
- Android Studio Jellyfish+
- JDK 21
- Android SDK 36 (Compile/Target)

### Installation and Execution
1. Clone the repository:
   ```bash
   git clone https://github.com/tonimadev/kairos-android-app.git
   ```
2. Open in Android Studio and wait for Gradle synchronization.
3. To run via CLI:
   - **Phone**: `./gradlew :app:installDebug`
   - **Wear OS**: `./gradlew :wear:installDebug`

### Testing and Quality
- Run all unit tests: `./gradlew testDebugUnitTest`
- Generate coverage report (Jacoco): `./gradlew createJacocoMergedCoverageReport`
- Apply code style: `./gradlew spotlessApply`

---

## ⚙️ Additional Configuration

For **Release** builds or full **Firebase/AdMob** integration, it is necessary:
1. Add `google-services.json` in `app/` and `wear/` folders.
2. Configure signing keys and ad IDs in `local.properties` or environment variables. See `build.gradle.kts` for details on expected properties.

---

## 🤝 Contributing
Contributions are welcome! Make sure to run `./gradlew spotlessApply` before opening a Pull Request.

---
Developed by **tonimadev**

