<img src="/images/kairos-banner.png" width="1024" height="500">

[![Android CI - Kairos Multi-Module](https://github.com/ipirangad3v/kairos-android-app/actions/workflows/android-ci.yaml/badge.svg)](https://github.com/ipirangad3v/kairos-android-app/actions/workflows/android-ci.yaml) [![codecov](https://codecov.io/gh/ipirangad3v/kairos-android-app/graph/badge.svg?token=TKC92HM5VY)](https://codecov.io/gh/ipirangad3v/kairos-android-app)

# Kairós - Calendar Alarms for Android & Wear OS

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
- **Total Control**: Enable or disable alarms globally or for specific events.

<p align="center">
  <img src="/images/watch1.png" width="200">
  <img src="/images/watch2.png" width="200">
  <img src="/images/tile.gif" width="300">
</p>

---

## 🛠 Tech Stack

This project follows modern Android development principles and MVVM architecture.

- **Language**: 100% Kotlin
- **UI**: Jetpack Compose (Phone & Wear OS)
- **Architecture**: MVVM, Clean Architecture, Multi-module
- **Persistence & Background**: DataStore, WorkManager, AlarmManager
- **DI**: Hilt
- **Sync**: Wearable Data Layer (Play Services)
- **Testing**: JUnit4, Robolectric, Turbine, MockK

---

## 🏁 Getting Started

### Prerequisites
- Android Studio Jellyfish+
- JDK 21
- Android SDK 36 (Compile/Target)

### Installation and Execution
1. Clone the repository:
   ```bash
   git clone https://github.com/ipirangad3v/kairos-android-app.git
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

