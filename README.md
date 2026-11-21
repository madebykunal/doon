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

## Project layout

```
app/src/main/java/com/madebykunal/doon/
  DoonApp.kt, MainActivity.kt   app start-up and the single activity
  data/      SQLite database, models and repositories (all SQL lives here)
  lib/       pure logic: dates, repeat rules, countdown and habit maths, markdown, colours
  state/     settings, navigation, editor, dialogs, snackbar and to-do actions
  notify/    reminders, alarms, morning digest and the full-screen alert
  widget/    the three home-screen widgets
  ui/        Compose screens: today, todos, lists, countdowns, habits, settings, sheets
    components/  shared pieces (dock, sheets, pickers, icons)
    theme/       colours, type and spacing
app/src/main/res/   fonts, sounds, icons, widget layouts
app/src/test/       unit tests for the logic in lib/
licenses/           font licences
```
