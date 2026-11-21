# Doon

Offline-first, day-based to-do app with countdowns, habits and home-screen widgets. Native Android: Kotlin and Jetpack Compose.

## Build

Open the folder in Android Studio, or from a terminal:

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk (R8, signed with the debug key)
./gradlew testDebugUnitTest
```

All data stays on the device in SQLite. The app has no network permission.
