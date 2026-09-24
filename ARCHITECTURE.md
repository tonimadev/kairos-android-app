# Kairos Android App - Architecture Guidelines

Welcome to the Kairos Android App project! To ensure consistency, testability, and a clean codebase, all developers and AI coding assistants MUST adhere to the following architectural rules when modifying or creating features.

## 1. Clean Architecture & Dependency Injection

- **Use Cases Must Be Flat**: DO NOT group UseCases into dependency wrapper classes (e.g., `CalendarDeps`, `PreferencesDeps`, `AlarmDeps`). Every UseCase must be injected individually and directly into the ViewModel or target class.
- **No Direct Repositories in ViewModels**: ViewModels must NEVER interact with or depend on Repositories, DAOs, or Data Sources directly. All data access or business logic must go through a single-responsibility `UseCase`.
- **Naming Convention**: UseCases should be named with a clear action verb and end with the `UseCase` suffix (e.g., `GetEventsForMonthUseCase`, `ToggleEventAlarmUseCase`).
- **Single Responsibility**: Each UseCase should ideally have only one public function (e.g., `operator fun invoke(...)`).

## 2. ViewModels

- **State Management**: ViewModels should expose a single `StateFlow` for the UI state (e.g., `EventScreenUiState`).
- **Intent Handling**: User actions should be passed to the ViewModel via an intent mechanism (e.g., `handleIntent(intent: EventIntent)`).
- **Separation of Concerns**: Keep ViewModels focused on mapping UI intents to UseCase calls and reducing the results back into the UI state. Do not put heavy business logic inside the ViewModel.

## 3. UI Layer (Jetpack Compose)

- Use Jetpack Compose for all UI components.
- Composables should observe the ViewModel's `StateFlow` and pass UI events back via intents.
- Keep Composables stateless where possible, relying on state hoisting.

## 4. Testing

- Always verify that new UseCases are properly provided in the corresponding Dagger/Hilt Module.
- If a class requires a large number of UseCases, inject them individually regardless of the number. The "flat" structure is prioritized over constructor brevity.
- Keep tests aligned with the architecture: update the `mockk` objects in test classes strictly using the flat UseCase pattern.

These rules ensure that our architecture remains scalable, testable, and completely decoupled from Android framework dependencies where possible.

## 5. Code Quality & Automated Checks (Mandatory)

Before considering any task, refactoring, or feature "complete", ALL developers and AI agents must execute the following automated checks suite in the terminal and ensure a `BUILD SUCCESSFUL` outcome:
```bash
./gradlew spotlessApply sortDependencies detekt testDebugUnitTest
```

- **Spotless (`spotlessApply`)**: All Kotlin files must adhere strictly to the project's formatting rules (`ktlint`). If there are formatting errors, executing `spotlessApply` automatically fixes them.
- **Detekt (`detekt`)**: Enforces static code analysis rules. Do not bypass code smells, excessive complexity (Cyclomatic/Cognitive), or long lines (`MaxLineLength`). If `detekt` fails, you must refactor the code to comply.
- **Sort Dependencies (`sortDependencies`)**: A custom Gradle task to maintain organized and alphabetically sorted dependencies in all `.gradle.kts` files.
- **Unit Tests (`testDebugUnitTest` ou `test`)**: Existing unit tests must pass without regressions. If an architectural change (like flattening a ViewModel's dependencies) breaks tests, update the `mockk` definitions and injection setup accordingly.

Never push or consider a task complete without validating against this entire check chain!

## 6. Error Handling (Mandatory)

Every exception the app can raise must be handled deliberately — never allowed to crash the app, and never silently ignored.

- **Catch at the boundaries**: `BroadcastReceiver`s, `Service`s, `Worker`s, Activity/Application entry points and coroutines launched in custom scopes (e.g. `receiverScope`) must not let an exception escape. An uncaught exception there crashes the process.
- **No silent swallowing**: an empty `catch`, or one that only returns a default value, is not allowed. Every `catch` must log with `logcat` (appropriate priority) **and** do one of:
  1. recover with a fallback that preserves the user-facing outcome (e.g. the alarm must still ring — see `AlarmFallbackNotification`);
  2. surface an error state to the UI through the ViewModel's `StateFlow`;
  3. report it as a non-fatal to Crashlytics when it is unexpected and cannot be recovered.
- **Catch narrowly**: catch the most specific exception type you can recover from and rethrow anything else. Never catch `CancellationException` inside coroutines without rethrowing it.
- **Platform restrictions are expected failures**: background start limits (`ForegroundServiceStartNotAllowedException`), revoked permissions (`SecurityException`), missing system components (e.g. no WebView on Wear) and flaky Play/Google APIs must be anticipated and handled explicitly.
- **Crash fixes need regression tests**: every crash fixed from Crashlytics gets a test that reproduces the failing condition.
