# Kontakti — Android

Native Kotlin + Compose app for [Kontakti](https://kontakti.app) — personal relationship intelligence.

---

## Requirements

- Android API 24+ (Android 7.0)
- Android Studio Hedgehog or newer
- A running Kontakti backend (kontakti.app or self-hosted)

---

## Stack

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose |
| Local persistence | Room 2.6 |
| Networking | Retrofit 2 + OkHttp + Gson |
| Auth token | DataStore (`TokenStore`) |
| Offline sync | WorkManager + `SyncWorker` |
| DI | Hilt |
| Contacts import | ContactsContract |
| Gmail discovery | Google Sign-In + People API + Gmail API |

---

## Architecture

**Offline-first.** Repositories serve a Room cache immediately (via `Flow`) and refresh from the API when connected. Mutations made while offline are stored in `PendingSyncEntity` and flushed by `SyncWorker` on reconnect.

```
app/src/main/kotlin/com/kontakti/
  data/
    local/
      PersonEntity, CompanyEntity, DiscussionEntity — Room @Entity
      PendingSyncEntity            — offline mutation queue
      KontaktiDao                  — @Dao with @Upsert, Flow queries
      KontaktiDatabase             — @Database
    network/
      ApiClient.kt                 — Retrofit + OkHttp + Gson setup
      ApiService.kt                — Retrofit interface
    repository/
      PeopleRepository.kt          — Room flow + API refresh
      CompanyRepository.kt
    sync/
      SyncQueue.kt                 — writes mutations to PendingSyncEntity
      SyncWorker.kt                — @HiltWorker, flushes queue on reconnect
    contacts/
      DeviceContactsImporter.kt    — ContactsContract + email dedup
    google/
      GoogleAuthManager.kt         — GoogleSignIn (gmail + contacts scopes)
      GoogleContactsService.kt     — People API + Gmail From: header discovery
  di/
    AppModule.kt                   — Hilt providers
  ui/screens/
    ImportContactsScreen.kt
    GmailImportScreen.kt
  util/
    NetworkMonitor.kt              — ConnectivityManager → StateFlow<Boolean>
  KontaktiApp.kt                   — @HiltAndroidApp, WorkManager config
```

---

## Setup

### 1. Backend URL

`data/network/ApiClient.kt` line 59:

```kotlin
.baseUrl("https://kontakti.app/api/v1/")
```

Change this if you're running a self-hosted backend.

### 2. Google Sign-In (required for Gmail import)

1. Create an OAuth 2.0 **web** client ID in [Google Cloud Console](https://console.cloud.google.com/).
2. Open `data/google/GoogleAuthManager.kt` and replace line 33:
   ```kotlin
   val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
       .requestServerAuthCode("YOUR_WEB_CLIENT_ID")   // ← replace this
   ```
3. Also create an **Android** OAuth client ID in Cloud Console (needed for the Sign-In button to work on-device). Add your SHA-1 fingerprint from:
   ```bash
   ./gradlew signingReport
   ```

Gmail import will not function without this — the rest of the app works fine without it.

---

## Build

Open the project in Android Studio and sync Gradle. Run on an emulator or device.

The build requires KSP for Room and Hilt annotation processing — these are already configured in `build.gradle.kts`. No additional local setup needed for core CRM features.

---

## Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET` — API calls
- `ACCESS_NETWORK_STATE` — connectivity monitoring
- `READ_CONTACTS` — device contacts import (requested at runtime)

---

## License

MIT
