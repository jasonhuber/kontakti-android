# Kontakti Android

Android is delivered via the shared React Native project — the same codebase as iOS.

**React Native project location:** `../iOS/KontaktiMobile`

## Build Commands

```bash
# Navigate to the React Native project root
cd ../iOS/KontaktiMobile

# Install dependencies (first time only)
npm install

# Start the Metro bundler
npm start

# Run on Android device/emulator
npm run android
# or directly:
npx react-native run-android

# Run on iOS simulator
npm run ios
# or directly:
npx react-native run-ios
```

## Android-Specific Notes

- React Native 0.73.6, targets Android 7.0+ (API 24+)
- Requires Android Studio with SDK Platform 33+ and an emulator or connected device
- The Android build files live under `../iOS/KontaktiMobile/android/`
- Same API client, auth store, navigation, and screens as iOS — zero platform-specific code

## Backend

Connect to the Laravel 11 API at `http://10.0.2.2:8000/api/v1` when running in an Android emulator
(Android emulator routes `10.0.2.2` to the host machine's `localhost`).
Update `BASE_URL` in `../iOS/KontaktiMobile/src/api/api.ts` accordingly when targeting Android.
