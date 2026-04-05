# LeetGraph

[![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B?logo=flutter)](https://flutter.dev)
[![Dart](https://img.shields.io/badge/Dart-3.11+-0175C2?logo=dart)](https://dart.dev)
[![Android Widget](https://img.shields.io/badge/Android-Home%20Widget-3DDC84?logo=android)](android/app/src/main/kotlin/com/example/leetgraph/LeetGraphWidgetProvider.kt)
[![Version](https://img.shields.io/badge/version-1.0.0%2B1-informational)](pubspec.yaml)

LeetGraph is a Flutter app that powers a resizable Android home screen widget showing a GitHub-style heatmap of a user's recent LeetCode activity.

The app handles username setup and manual refresh actions. The Android side fetches data from LeetCode's GraphQL endpoint, stores snapshot data locally, and keeps widgets up to date with WorkManager.

## Why This Project Is Useful

- Home screen-first workflow: your LeetCode activity is visible without opening a browser.
- Dynamic heatmap rendering: the widget adapts the number of visible weeks based on available widget size.
- Background sync: periodic refresh is scheduled with Android WorkManager when a username is configured.
- Fast setup: one screen in-app flow to save username, trigger sync, and open widget pinning.
- Resilient behavior: failures keep the last snapshot visible and surface friendly status messages.

## How It Works

1. Flutter UI stores a LeetCode username through a platform channel.
2. Android code fetches yearly calendar data from `https://leetcode.com/graphql/`.
3. Counts are merged and reduced into a rolling activity window.
4. Widget snapshot data is saved in SharedPreferences.
5. The widget is redrawn on demand and by scheduled background jobs.

Relevant implementation files:

- [lib/main.dart](lib/main.dart) - setup UI and user actions
- [lib/widget_bridge.dart](lib/widget_bridge.dart) - Flutter/Android method channel bridge
- [android/app/src/main/kotlin/com/example/leetgraph/LeetCodeService.kt](android/app/src/main/kotlin/com/example/leetgraph/LeetCodeService.kt) - LeetCode GraphQL fetcher
- [android/app/src/main/kotlin/com/example/leetgraph/LeetGraphSyncRunner.kt](android/app/src/main/kotlin/com/example/leetgraph/LeetGraphSyncRunner.kt) - sync orchestration and error handling
- [android/app/src/main/kotlin/com/example/leetgraph/WidgetRenderer.kt](android/app/src/main/kotlin/com/example/leetgraph/WidgetRenderer.kt) - bitmap heatmap renderer
- [android/app/src/main/kotlin/com/example/leetgraph/LeetGraphSyncScheduler.kt](android/app/src/main/kotlin/com/example/leetgraph/LeetGraphSyncScheduler.kt) - periodic and immediate work scheduling

## Getting Started

### Prerequisites

- Flutter SDK installed and on PATH
- Dart SDK compatible with `^3.11.4` (managed by Flutter)
- Android Studio (or Android SDK + emulator/device)
- Java 17 (required by Android Gradle settings)

### Installation

```bash
git clone https://github.com/var-ad/LeetGraph.git
cd LeetGraph/leetgraph
flutter pub get
```

### Quick Install (No Build Required)

If you want to skip local setup and try the app directly, install the bundled APK:

1. Use [app-release.apk](app-release.apk) from the repository root.
2. Sideload it on an Android device.
3. Open LeetGraph and continue with the widget setup flow below.

### Run The App

```bash
flutter run -d android
```

The widget implementation is Android-only. On other platforms, the app displays an informational message instead of widget controls.

## Usage

1. Launch the app on Android.
2. Enter your public LeetCode username.
3. Click Save and sync.
4. Click Add widget (or use your launcher widget picker manually).
5. Resize the widget on your home screen as needed.

### Developer Workflow

Run static analysis:

```bash
flutter analyze
```

Run tests:

```bash
flutter test
```

Build a release APK:

```bash
flutter build apk --release
```

## Project Structure

```text
lib/
	main.dart              Flutter setup UI
	widget_bridge.dart     Platform-channel contract and DTOs

android/app/src/main/kotlin/com/example/leetgraph/
	MainActivity.kt
	LeetCodeService.kt
	LeetGraphSyncRunner.kt
	LeetGraphSyncScheduler.kt
	LeetGraphSyncWorker.kt
	LeetGraphWidgetProvider.kt
	WidgetRenderer.kt
	WidgetStorage.kt
	LeetGraphContract.kt
```

## Contributing

1. Fork the repository.
2. Create a feature branch.
3. Make your changes and run:

```bash
flutter analyze
flutter test
```

4. Open a pull request with a clear summary and screenshots/GIFs for UI changes.

## Notes

- This repository currently has no CI workflow configured.
- A license file is not present yet. Add one before publishing or accepting external contributions.
