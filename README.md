# Konataki Android

Android is delivered via React Native — the same codebase as the iOS app.

See [../iOS/README.md](../iOS/README.md) for the full mobile strategy.

## Android-Specific Notes

- React Native targets Android 7.0+ (API 24+)
- Builds live in the `ios/` sibling project under `android/` folder
- Same API client, same hooks, native navigation via React Navigation

## Setup

```bash
# From iOS/KonatakiMobile/
npx react-native run-android
```

Requires Android Studio + SDK Platform 33+ installed.
