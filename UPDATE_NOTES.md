# In-app updates — how it works, and how to publish a release

## The mechanism
- `UpdateChecker` fetches `update.json` from this repo's `main` branch (via
  the `raw.githubusercontent.com` URL) and compares its `versionCode`
  against `BuildConfig.VERSION_CODE`.
- Checked automatically once per 24h on app launch (`EmergencyApp`), and
  on-demand via Settings → "Vérifier les mises à jour".
- If newer, a notification appears; tapping it opens the app. The actual
  download only starts when the user taps "Télécharger" in the in-app
  dialog (Settings screen) — this is a deliberate choice so the
  REQUEST_INSTALL_PACKAGES flow reads as something the user chose, not
  something that happened in the background.
- Download uses the system `DownloadManager` (handles retries/backgrounding
  for free) to `getExternalFilesDir(Downloads)`, then `UpdateDownloadReceiver`
  fires on completion and hands the file to the system installer via
  `FileProvider`.

## First-time install permission
Since this isn't distributed via Play Store, Android requires the user to
explicitly allow "install unknown apps" for this app specifically. The
first time `UpdateDownloader.promptInstall()` runs, Android will either:
- Show the install confirmation directly (if already allowed), or
- Redirect to Settings → "Install unknown apps" → toggle allow for this app,
  then the user has to re-tap install.

Not automated in this scaffold — if it becomes a recurring friction point,
`Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` can be launched proactively
with an explanation dialog first.

## How to publish a new version

1. Bump the version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "0.2.0"
   ```
2. Commit and push — this triggers `.github/workflows/android-build.yml`,
   producing a fresh APK as a workflow artifact.
3. Download that APK artifact (as you've been doing), then create a
   **GitHub Release** instead of just leaving it as a workflow artifact:
   - GitHub → repo → Releases → "Draft a new release"
   - Tag it (e.g. `v0.2.0`), upload the APK as a release asset
   - Publish
4. Copy the release asset's direct download URL (right-click / long-press
   the asset link on the release page).
5. Update `update.json` at the repo root:
   ```json
   {
     "versionCode": 2,
     "versionName": "0.2.0",
     "apkUrl": "<the release asset URL from step 4>",
     "notes": "What changed in this version, shown to users."
   }
   ```
6. Commit and push `update.json` to `main`. Within 24h (or immediately, if
   someone taps "Vérifier les mises à jour"), devices on the old version
   will see the update notification.

## Known limitation
`update.json` lives on `main` unauthenticated/unsigned — anyone who can
push to this repo (or, if it ever goes public, anyone who compromises the
repo) controls what APK URL gets pushed to every installed device. There's
no signature verification on the downloaded APK beyond Android's own APK
signing check (which does verify it's signed by the same key as the
currently-installed app, so a malicious *different* APK would fail to
install — but a malicious release built with your own signing key would
not be caught by anything in this update system). Fine for an internal
tool with a small trusted repo; worth hardening (e.g. checksum pinning) if
this ever needs to resist a compromised GitHub account specifically.
