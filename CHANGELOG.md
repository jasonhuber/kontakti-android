# Changelog — kontakti-android

Notable changes to the Kotlin + Compose Android app. Most recent at top.

---

## 2026-05-29

### Contact Review queue ported

`ReviewContactsScreen` + `ReviewContactsViewModel` consuming `/people/health` and `/people/{id}/review`. Mirrors the iOS bucket order (needs_review → imported_unreviewed). Drill-in shows the sampled rows with inline "Reviewed" buttons that flip to a green check on success. Single Scaffold with state-switched title and back arrow.

`ApiService.listPeople` gained an optional `needs_review` query param (default null so existing callers untouched).

New `PeopleHealth` / `HealthBucket` / `HealthSample` data classes with snake_case `@SerializedName`.

Routed at `Routes.REVIEW_CONTACTS = "review/contacts"`, entered from Settings → Review contacts.

Commit: [`5c416af`](https://github.com/jasonhuber/kontakti-android/commit/5c416af)

### Smoke-test fixes

Found while booting the debug APK in an emulator and walking through registration → onboarding → tabs:

- **AuthViewModel state was stuck on Loading.** The `combine` didn't include `_bootstrapping` as a source, so when the cold-start `me()` call failed (no token), `_user` stayed null, no source flow emitted, and the combine never re-fired. Added `_bootstrapping` to the combine inputs.
- **Google access token broken.** `GoogleAuthManager.handleSignInResult` was returning `account.serverAuthCode` (intended for backend exchange, with a literal `YOUR_WEB_CLIENT_ID` placeholder that would have rejected anyway). Dropped `requestServerAuthCode` from the options builder and now exchange the signed-in `Account` via `GoogleAuthUtil.getToken` for a real OAuth access token, scoped to contacts.readonly + gmail.readonly.
- **Add FABs hidden under voice FAB.** Per-screen add/log FABs were rendering at the same bottom-right slot as the global voice FAB. Moved each screen's primary action to the `TopAppBar` actions slot (matches iOS toolbar pattern).
- **"Discussions" label wrap.** The bottom-nav label was wrapping to two lines at 5 tabs. Added `maxLines=1` + ellipsis.

Commit: [`3339950`](https://github.com/jasonhuber/kontakti-android/commit/3339950)

### Post-auth onboarding wizard + contact-import payload shape fix

4-step wizard (Welcome → Phone → Google → Done) mirroring iOS `OnboardingView`:

- Phone step: runtime `READ_CONTACTS` permission, reads device contacts via `DeviceContactsImporter`, previews up to 4 candidates, imports the batch.
- Google step: launches GoogleSignIn (Gmail + Contacts scopes), fetches via `GoogleContactsService`, previews + imports.
- Done step: shows total imported, single "Open Kontakti" CTA.

`AuthState` gained `NeedsOnboarding`. `KontaktiAppRoot` routes to `OnboardingScreen` for it. `AuthViewModel.completeOnboarding()` persists locally (`TokenStore.markOnboarded`) and best-effort hits `POST /auth/onboarding/complete`. Logout clears the local flag so a new account on the same device re-runs the wizard.

Along the way: discovered the Android client had been POSTing a raw `List<ImportCandidate>` with single-field `name`. Backend expects `{contacts: [{first_name, last_name, ...}]}`. Every Android import call had been silently malformed since the offline branch landed. `ImportCandidate` rebuilt around `first_name`/`last_name`/`company_name`/`source` with Gson `SerializedName`; new `BulkImportRequest` wrapper; existing callers updated.

Commit: [`8711a32`](https://github.com/jasonhuber/kontakti-android/commit/8711a32)

### AddCompany + LogDiscussion + Google sign-in button + tab polish

- **AddCompanyScreen** — form for name/domain/industry/website/linkedin/notes; POST /companies via new `ApiService.createCompany` + `CreateCompanyRequest`. FAB on Companies tab.
- **LogDiscussionScreen** — title/type (emoji-tagged dropdown)/summary + participant search via `GET /people?q=`. Selection chip set. POST /discussions.
- **Google sign-in on LoginScreen** — `GoogleAuthManager.signInForIdToken` mints an id_token, wired to `AuthRepository.loginWithGoogle`. Web client ID flows from `local.properties` → `BuildConfig.GOOGLE_WEB_CLIENT_ID` (kept out of source control, matching iOS convention).
- **Tab-root polish** — back arrow dropped from Companies/Discussions list screens (they're tab roots; back made no sense).

Commit: [`27cdaf5`](https://github.com/jasonhuber/kontakti-android/commit/27cdaf5)

---

## 2026-05-28

### iOS parity catch-up

Massive PR to bring Android into rough parity with iOS:

**Build infra (the app didn't compile before):**
- JVM heap bumped to 4g / metaspace 1g (Gradle was OOM-ing).
- `applicationId = app.kontakti` (matches `google-services.json` + iOS bundle ID).
- `packagingOptions` excludes for META-INF/INDEX.LIST etc (google-auth libs ship duplicates).
- Removed deprecated `package="com.kontakti"` attribute from AndroidManifest.
- `.gitignore`: ignore per-machine `local.properties`.
- `ImportContactsScreen.CandidateRow` visibility relaxed from `private` to `internal` so `GmailImportScreen` can reuse it.
- Root composable renamed from `KontaktiApp()` to `KontaktiAppRoot()` to stop colliding with the `KontaktiApp` Application class.

**Auth gate (the app had no login UI at all and the OkHttp client never attached an Authorization header — every authenticated call had been silently 401-ing):**
- `AppModule.provideOkHttp` interceptor now pulls the persisted token from `TokenStore` and adds `Bearer <token>` to every request.
- `ApiService` adds `auth/register`, `auth/google`, `auth/onboarding/complete`.
- `AuthRepository` / `AuthViewModel` cover login/register/google + bootstrap + logout.
- `LoginScreen` with sign-in + sign-up forms + shared error banner.
- `KontaktiAppRoot` wraps the nav host in an `AuthState` gate.

**Parity screens (Companies, Discussions, Feed — none existed before):**
- `CompaniesListScreen` + `CompanyDetailScreen` with Room cache + search.
- `DiscussionsListScreen` + `DiscussionDetailScreen` with type-filter chips.
- `FeedScreen` for the activity feed.
- Bottom bar reshuffled to Today / People / Companies / Discussions / Settings (5 tabs). Feed under Settings → Activity feed.

**Photo gallery (pre-existing in-flight work, shipped here so it wasn't stranded):**
- `PhotoRepository` (list / multipart upload / data-URL / external URL / delete / set primary).
- `PhotoGallery` composable in `PersonDetailScreen` and `PersonEditScreen`.

Commit: [`dbee0b3`](https://github.com/jasonhuber/kontakti-android/commit/dbee0b3)

---

## 2026-05-22 and earlier

- **Today inbox, voice, push, duplicates, DNC, multi-phone/email** ([`ab9535e`](https://github.com/jasonhuber/kontakti-android/commit/ab9535e))
- **Contact-reach launcher icons** ([`57c25d4`](https://github.com/jasonhuber/kontakti-android/commit/57c25d4))
- **Offline-first Room DB, WorkManager sync, device contacts import, Gmail discovery** ([`2f046b0`](https://github.com/jasonhuber/kontakti-android/commit/2f046b0))
- **Initial placeholder** (React Native, later abandoned in favor of native Compose) ([`3ccf200`](https://github.com/jasonhuber/kontakti-android/commit/3ccf200))
