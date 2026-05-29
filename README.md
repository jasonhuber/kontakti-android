# Kontakti Android

This is a thin pointer. The actual Gradle project lives one level down at [`KontaktiAndroid/`](./KontaktiAndroid/), and its README is the canonical setup guide.

## Quick links

- 🤖 [Android app README](./KontaktiAndroid/README.md) — stack, setup, build commands
- 📋 [Project HANDOFF](./HANDOFF.md) — architecture, deploy, cross-platform context
- 🛣️ [NEXT_STEPS](./NEXT_STEPS.md) — active work
- 📜 [CHANGELOG](./CHANGELOG.md) — per-commit history

## Build (one-liner)

```bash
cd KontaktiAndroid
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:assembleDebug --no-daemon
```

## Note

The earlier React Native plan (a shared codebase with iOS) was abandoned. Android and iOS are independent native apps that consume the same backend API. The earlier wrapper file's `npm run android` instructions were obsolete.
