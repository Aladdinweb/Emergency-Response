# Build notes — start here

This supersedes the Gradle snippets in FCM_NOTES.md / ALERT_SERVICE_NOTES.md /
SMS_NOTES.md, which were illustrative when written. The actual, complete
`build.gradle.kts` files are now in the repo root and `app/`.

## 1. Generate the shared serial-encryption key

Every device needs the SAME key (see `AppKeyProvider.kt` for why). Generate
a random 128-bit key as a 32-character hex string, from Termux:

```bash
openssl rand -hex 16
```

Copy the output — you'll use it in two places below.

## 2. Local build (once you have Android Studio or a JDK+SDK on some machine)

Create `local.properties` in the project root (already gitignored):
```
sdk.dir=/path/to/your/Android/sdk
ILINE_SERIAL_KEY_HEX=<paste the key from step 1>
CLOUD_FUNCTION_URL=<optional — see CLOUD_FUNCTION_NOTES.md; leave blank to stay SMS-only>
CLOUD_FUNCTION_SHARED_SECRET=<optional — must match the deployed function's secret>
```

Then, in Firebase Console → your project → Android app → download
`google-services.json` → place it at `app/google-services.json`.

Open the project root folder in Android Studio and let it sync — it will
generate the Gradle wrapper (`gradlew`/`gradle-wrapper.jar`) automatically
the first time, since **that binary file isn't included in this scaffold**
(it's a compiled jar I can't hand-write without a real Gradle install to
generate it from). If you ever run `gradle wrapper` from any machine with
Gradle installed, commit the resulting `gradle/wrapper/` folder and
`gradlew`/`gradlew.bat` so `./gradlew` works everywhere after that.

## 3. Building without Android Studio: GitHub Actions

`.github/workflows/android-build.yml` builds a debug APK on every push to
`main` and uploads it as a downloadable workflow artifact — same idea as
your TASHIL-Hub Windows-runner PyInstaller build, just for Android. It uses
`gradle/actions/setup-gradle` directly rather than the wrapper, so it works
even before you've generated one locally.

Set these in GitHub → repo → Settings → Secrets and variables → Actions:
- `ILINE_SERIAL_KEY_HEX` — the key from step 1
- `GOOGLE_SERVICES_JSON_B64` — your `google-services.json`, base64-encoded:
  ```bash
  base64 -w0 google-services.json
  ```
  (paste the single-line output as the secret value)

Without `GOOGLE_SERVICES_JSON_B64` set, the workflow still builds (using a
placeholder file) so you can confirm the code compiles even before Firebase
is configured — just won't produce a build where push notifications
actually work.

Once both secrets exist, push to `main` (or trigger the workflow manually
from the Actions tab) and download the APK from the workflow run's
"Artifacts" section.

GitHub's `ubuntu-latest` runners ship with the Android SDK preinstalled, so
no separate SDK setup step should be needed — if the build fails looking
for a specific platform/build-tools version, that's the first thing to add
back into the workflow (`sdkmanager "platforms;android-34"`, etc.).

## 4. Known gaps, all flagged in-code where they live

- **No backend for FCM publishing** — `RemoteAlertPublisher` is a
  documented stub. SMS is the only transport that actually sends right now.
- **`SmsFallbackContactEntity` directory is thin** — only populated with a
  staff member's own number for their own facility+dept at registration.
  Nothing syncs contacts across devices yet.
- **No delivery confirmation for SMS** — sent vs. carrier-confirmed-delivered
  isn't distinguished (see `SMS_NOTES.md`).
- **`isMinifyEnabled = false`** — fine for debug/testing, revisit before
  any release build with real ProGuard/R8 rules.
- **Launcher icon is a placeholder vector** (`drawable/ic_launcher.xml`) —
  swap for real branding via Android Studio's Image Asset tool once available.
- **DB schema has gone through 3 versions with destructive migration** —
  acceptable only because nothing's shipped to a real device yet; the
  moment this is installed anywhere you care about keeping data on,
  destructive migration needs to become a real `Migration` object.
- **No unit/instrumented tests** — dependencies for JUnit/Espresso are in
  `build.gradle.kts` but no test files exist yet.
