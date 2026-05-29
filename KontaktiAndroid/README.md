# Kontakti — Android

Native Kotlin + Jetpack Compose app for [Kontakti](https://kontakti.app) — personal relationship intelligence.

---

## Requirements

- Android API 26+ (Android 8.0)
- JDK 21 (use Android Studio's bundled JBR — the system Temurin 8 is too old for AGP)
- Android Studio Hedgehog or newer (or just Gradle + SDK from the command line)
- A running Kontakti backend (kontakti.app or self-hosted)

---

## Stack

| Layer | Tech |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Local persistence | Room 2.6 with KSP |
| Networking | Retrofit 2 + OkHttp + Gson |
| Auth token | DataStore Preferences (`TokenStore`) |
| Bearer injection | Hilt-provided `OkHttpClient` interceptor reads `tokenStore.tokenFlow.first()` |
| Offline sync | WorkManager + `SyncWorker` (Hilt-injected) |
| DI | Hilt |
| Device contacts | `ContactsContract` |
| Google sign-in (login) | GoogleSignIn + `requestIdToken` → backend `/auth/google` |
| Google APIs (Gmail / People) | GoogleSignIn + `GoogleAuthUtil.getToken` for a real OAuth access token |
| Push | Firebase Cloud Messaging |
| Image loading | Coil |
| Home-screen widget | Glance (AppWidget + Material 3) |

---

## Architecture

**Offline-first.** Repositories serve from a Room `Flow` immediately and refresh from the API when connected. Mutations made while offline are stored as `PendingSyncEntity` rows and flushed by `SyncWorker` on reconnect.

```
app/src/main/kotlin/com/kontakti/
├── KontaktiApp.kt                          @HiltAndroidApp + WorkManager + FCM bootstrap
├── MainActivity.kt                         Compose root, AuthState gate, NavHost
├── data/
│   ├── local/                              Room: PersonEntity, CompanyEntity, DiscussionEntity, PendingSyncEntity, KontaktiDao, KontaktiDatabase
│   ├── model/Models.kt                     Gson-annotated DTOs (snake_case @SerializedName)
│   ├── model/EnrichmentModels.kt
│   ├── network/ApiService.kt               Retrofit interface (all endpoints)
│   ├── network/ApiClient.kt                Standalone Retrofit client (legacy; Hilt provider in AppModule is the real one)
│   ├── repository/                         AuthRepository, PeopleRepository, CompanyRepository, DiscussionsRepository, ActivityRepository, NotesRepository, TasksRepository, TodayRepository, QuizRepository, DuplicatesRepository, SearchRepository, SocialGroupRepository, GoogleAccountsRepository, PushRepository, PhotoRepository, VoiceRepository
│   ├── datastore/TokenStore.kt             auth_token + kontakti_onboarded prefs
│   ├── contacts/DeviceContactsImporter.kt  ContactsContract + email dedup → ImportCandidate
│   ├── contacts/ImportCandidate.kt         first_name/last_name/company_name with @SerializedName for the backend wire shape + BulkImportRequest wrapper
│   ├── google/GoogleAuthManager.kt         Two flows: signInForIdToken (login) and handleSignInResult (Gmail/People access token via GoogleAuthUtil)
│   ├── google/GoogleContactsService.kt     People API + Gmail From: header discovery
│   ├── sync/SyncQueue.kt
│   ├── sync/SyncWorker.kt                  @HiltWorker, queue flush on reconnect
│   ├── sync/TodayWidgetWorker.kt           refreshes glance widget
│   └── voice/VoiceRecorder.kt
├── di/AppModule.kt                         Hilt providers (Gson, OkHttp+token interceptor, Retrofit, Room, DAO, WorkManager, TokenStore)
├── push/KontaktiMessagingService.kt        FCM listener
├── ui/
│   ├── auth/AuthViewModel.kt               state combine(tokenFlow, onboardedFlow, _user, _bootstrapping) → Loading | SignedOut | NeedsOnboarding | SignedIn
│   ├── auth/LoginScreen.kt                 sign-in + sign-up + Google button
│   ├── auth/OnboardingScreen.kt            Welcome → Phone → Google → Done
│   ├── components/                         shared composables (PhotoGallery, SectionHeader, ExtraComponents)
│   ├── screens/
│   │   ├── people/                         PeopleListScreen, PersonDetailScreen, PersonEditScreen, AddPersonScreen
│   │   ├── companies/                      CompaniesListScreen, CompanyDetailScreen, AddCompanyScreen
│   │   ├── discussions/                    DiscussionsListScreen, DiscussionDetailScreen, LogDiscussionScreen
│   │   ├── feed/FeedScreen.kt
│   │   ├── review/ReviewContactsScreen.kt
│   │   ├── today/                          TodayScreen, DraftMessageSheet, QuizSection
│   │   ├── voice/                          VoiceRecorderScreen, VoiceResultReview
│   │   ├── duplicates/DuplicatesScreen.kt
│   │   ├── groups/                         SocialGroupsScreen, GroupImportWizard
│   │   ├── quiz/QuizSessionScreen.kt
│   │   ├── search/NaturalSearchScreen.kt
│   │   ├── settings/SettingsScreen.kt
│   │   ├── GmailImportScreen.kt
│   │   ├── ImportContactsScreen.kt
│   │   └── LinkedInImportScreen.kt
│   ├── theme/
│   └── navigation/                         (currently inline in MainActivity.kt)
├── util/NetworkMonitor.kt
└── widget/                                 TodayWidget (Glance), TodayWidgetReceiver, TodayWidgetState
```

Bottom-nav tabs: **Today / People / Companies / Discussions / Settings** (5; Material 3 NavigationBar fits these labels at one line each with the ellipsis on Discussions). Activity feed lives under Settings → Activity feed.

Voice recorder is a global floating action button visible on all tabs.

---

## Setup

### 1. `local.properties`

Gitignored. Recreate locally:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
kontakti.google.web_client_id=<the GOOGLE_WEB_CLIENT_ID from Website/.env>
```

The Google web client ID is read at build time via `Properties().load(...)` in `app/build.gradle.kts` and exposed as `BuildConfig.GOOGLE_WEB_CLIENT_ID`. `GoogleAuthManager.signInForIdToken` uses it to request an id_token the backend `/auth/google` route can verify.

If you don't set the web client ID, the rest of the app works — only the Google sign-in path fails.

### 2. Backend URL

`di/AppModule.kt`:

```kotlin
.baseUrl("https://kontakti.app/api/v1/")
```

Change this if you're running a self-hosted backend.

### 3. `google-services.json`

Place your Firebase config at `app/google-services.json` (gitignored). The bundle's `package_name` must be `app.kontakti` (the `applicationId` in `build.gradle.kts`). FCM push notifications need this; the rest of the app builds without it but `:app:processDebugGoogleServices` will fail.

### 4. Required Google OAuth clients

- **Web client** — id_token issuer for login. Set its ID in `local.properties` (above) and in the backend `.env` as `GOOGLE_WEB_CLIENT_ID`.
- **Android client** — needed so the Sign-In sheet actually opens on-device. Add your SHA-1 fingerprint:
  ```bash
  ./gradlew signingReport
  ```

### 5. Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET` — API calls
- `ACCESS_NETWORK_STATE` — connectivity monitoring
- `READ_CONTACTS` — device contacts import (requested at runtime by `OnboardingScreen` / `ImportContactsScreen`)
- `RECORD_AUDIO` — voice memo recorder
- `POST_NOTIFICATIONS` — FCM (Android 13+, requested at runtime)

---

## Build

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:assembleDebug --no-daemon --console=plain
```

The `JAVA_HOME` override is required because the system Java (Temurin 8) is too old for the Android Gradle Plugin. Android Studio's bundled JBR ships Java 21.

To install the resulting APK on a running emulator:

```bash
"$HOME/Library/Android/sdk/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Auth flow

1. App launches → `AuthViewModel.bootstrap()` calls `GET /auth/me` (Bearer token from `TokenStore` injected by the OkHttp interceptor in `AppModule.provideOkHttp`).
2. `state` is a `combine(tokenFlow, onboardedFlow, _user, _bootstrapping)` that emits one of:
   - `Loading` — first run, no decision yet
   - `SignedOut` — no token → `LoginScreen`
   - `NeedsOnboarding` — token but neither local `onboarded` flag nor `user.has_completed_onboarding` → `OnboardingScreen`
   - `SignedIn` — both true → `MainNavigation` (the bottom-nav app)
3. Logout clears the token AND the local `onboarded` flag so a fresh account on the same device re-runs the wizard.

---

## Project-level docs

- Architecture overview + cross-platform context: [`HANDOFF.md`](../HANDOFF.md) (at the Android repo root)
- What's next: [`NEXT_STEPS.md`](../NEXT_STEPS.md)
- Per-commit history: [`CHANGELOG.md`](../CHANGELOG.md)

These are mirrors of the canonical project-level docs. The cross-repo source of truth lives in the workspace Dropbox folder; copies live in each repo so anyone cloning a single repo has the full context.

---

## License

MIT.
