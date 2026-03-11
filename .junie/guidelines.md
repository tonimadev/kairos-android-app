# Project Development Guidelines

This document provides essential information for developers working on the **Kairós** project.

## 🛠 Build & Configuration

### Prerequisites
- **JDK 21**: Ensure your environment uses Java 21.
- **Android Studio Jellyfish+**: Recommended for full support of the latest Compose features.
- **Android SDK 36**: Compile and Target SDK version.

### Key Build Commands
- **Assemble Debug**: `./gradlew assembleDebug`
- **Install Phone App**: `./gradlew :app:installDebug`
- **Install Wear OS App**: `./gradlew :wear:installDebug`
- **Apply Code Style**: `./gradlew spotlessApply`
- **Check Style & Lint**: `./gradlew detekt`

### Firebase & AdMob Configuration
For full functionality (Release builds):
1. Place `google-services.json` in `app/` and `wear/` modules.
2. Configure AdMob IDs and signing keys in `local.properties`:
   - `admob.app.id`
   - `admob.banner.ad.unit.home`
   - `admob.banner.ad.unit.alarm_acitivity`

## 🧪 Testing

### Running Tests
- **Unit Tests (All modules)**: `./gradlew testDebugUnitTest`
- **Unit Tests (Specific module)**: `./gradlew :core:testDebugUnitTest`
- **Code Coverage**: `./gradlew createJacocoMergedCoverageReport` (Generates a merged report in `build/reports/jacoco/`).

### Adding New Tests
- Use **JUnit 4** and **MockK** for mocking.
- For components interacting with Android APIs (like `Context`), use **Robolectric**.
- Ensure test functions use backticks for descriptive names: `` `when condition then expected result` ``.
- **Test Strategy**:
  - `core`: Focus on Business Logic, Use Cases, and Repositories.
  - `app`/`wear`: Focus on ViewModels and UI logic.

### Demonstration Test
To verify your environment, you can run the following command for a simple test:
```bash
./gradlew :core:testDebugUnitTest --tests "digital.tonima.core.model.EventTest"
```
*Note: This specific test file is for demonstration purposes and should be used as a reference for creating new tests.*

## 📝 Development Information

### Code Style
- **Kotlin Official Style**: Enforced via `ktlint` and `Spotless`.
- **Indentation**: 4 spaces for Kotlin/Java/Gradle, 2 spaces for other files (YAML, Ruby, etc.).
- **Naming**: Follow standard Kotlin naming conventions.
- **Imports**: Avoid star imports (except for very specific cases configured in `.editorconfig`).

### Architecture
- **Clean Architecture**: Separation of concerns into `data`, `domain` (use cases), and `presentation`.
- **MVVM**: Used throughout both Phone and Wear OS modules.
- **Multi-module**:
  - `:core`: Shared logic, models, and repositories.
  - `:app`: Smartphone-specific UI and features.
  - `:wear`: Wear OS-specific UI, complications, and tiles.

### Best Practices
- **Spotless**: Always run `./gradlew spotlessApply` before committing code to ensure consistency.
- **Detekt**: Use `./gradlew detekt` to find potential code smells.
- **Hilt**: All dependency injection is managed via Hilt. Use `@BindType` for interface binding as seen in `CalendarRepositoryImpl`.

## 🚀 Modern Android Best Practices

### UI & Compose
- **Unidirectional Data Flow (UDF)**: Events flow up (UI to ViewModel) and state flows down (ViewModel to UI).
- **UI State**: ViewModels should expose a single, immutable `StateFlow` representing the entire UI state (e.g., `EventScreenUiState`).
- **Lifecycle-aware collection**: Use `collectAsStateWithLifecycle()` in Composables to safely observe state and avoid leaks or unnecessary updates.
- **Previews**: Create `@Preview` functions for all major UI components with representative mock data.

### Concurrency & Coroutines
- **Structured Concurrency**: Use `viewModelScope` for coroutines in ViewModels and `lifecycleScope` for UI components.
- **Data Layers**: Repositories and Use Cases should expose `suspend` functions for one-shot actions and `Flow` for reactive data streams.
- **Dispatchers**: Offload long-running or blocking tasks to `Dispatchers.IO` or `Dispatchers.Default` in the data layer (Repository/DataSource).

### Architecture & DI
- **Clean Architecture**: Strictly separate `presentation`, `domain` (use cases), and `data` (repositories/data sources) layers.
- **Use Cases**: Encapsulate specific business logic into Use Cases (e.g., `GetEventsForMonthUseCase`) for better reuse and isolation.
- **Hilt**: Always use `@HiltViewModel` for ViewModels and Hilt's dependency injection for all components.

### Dependencies & Configuration
- **Version Catalog**: Centralize all dependency management in `gradle/libs.versions.toml`.
- **Kotlin official style**: Consistently follow the Kotlin official style guide as enforced by Ktlint and Spotless.

### Wear OS (Specific)
- **Compose for Wear OS**: Use specialized components (e.g., `ScalingLazyColumn`, `Chip`, `Button`) designed for circular screens.
- **Tiles & Complications**: Expose key information via Tiles (using Protolayout) and Complications to provide glanceable data without opening the app.
- **Horologist**: Leverage the [Horologist](https://github.com/google/horologist) libraries to implement common Wear OS patterns like media playback or list optimization.
- **Data Layer API**: Use the Wearable Data Layer to synchronize state efficiently between the phone and watch apps.

### localization

- **String Resources**: All user-facing text should be defined in `strings.xml` for both `app` and `wear` modules to support localization.
- **Pluralization**: Use `plurals` for any text that varies based on quantity (e.g., "1 event" vs "2 events").
- **Accessibility**: Ensure all UI components have appropriate content descriptions and support for screen readers, especially on Wear OS where screen space is limited.
- **Translations**: If adding new features, consider providing translations for all supported languages by the app(simplified Chinese, Spanish, English,French,Arabian, hindi,Japanese,Russian, German) to broaden accessibility. the base language is Brazilian Portuguese, so all new strings should be added to `strings.xml` in the `values/` directory, and then translated versions can be added to `values-<language>/strings.xml` as needed.

## 📦 Release & Metadata

- **Fastlane Metadata**: When large new features are developed, ensure to update the changelogs in `fastlane/metadata/android/<lang>/changelogs/default.txt` for all supported languages.
- **Character Limit**: Changelog entries should be concise and must not exceed 500 characters per file to ensure compatibility with Google Play Store limits.
