# STATE.md — Emergency Response App
© ILINE TECH BY FERAK ALADDIN. All Rights Reserved.

Last updated: after the second round of device-testing fixes (9-item punch list).
This file is the map back into the project after time away — read this before
diving into any specific file.

## Project identity
- Display name: **Emergency Response** (package `com.ilinetech.emergency`)
- Purpose: emergency/security response coordination across Algerian
  healthcare institutions (CHU/EHU/EPH/EPSP), dual-mode online (FCM) +
  offline (GSM/SMS) alert dispatch, targeted by facility + department.
- `versionCode` / `versionName`: see `app/build.gradle.kts` `defaultConfig`
  — bump both together per `UPDATE_NOTES.md` when publishing.
- Repo: `github.com/Aladdinweb/Emergency-Response`, built via
  `.github/workflows/android-build.yml` (no local Android Studio required).

## Architecture status: functioning scaffold, device-tested
First APK built and installed successfully; core registration → serial
generation → SMS send/receive → notification round-trip has been confirmed
working on a real device. This is NOT yet a finished, hardened product —
see "Outstanding roadmap" below.

## Database
Room, current **schema version 4**. Migrations are `fallbackToDestructiveMigration()`
— acceptable ONLY because nothing has shipped to end users yet; the moment
real installs exist with data worth keeping, this must become real
`Migration` objects (flagged in `AppDatabase.kt`).

Entities:
- `InstitutionEntity` — seeded facility directory (currently just EPSP ES SENIA + its 7 branches)
- `SubBranchEntity` — polyclinique/sub-branch under an institution
- `StaffMemberEntity` — this device's registered profile(s); carries
  `institutionIndex`/`branchIndex` (added for sibling-department serial
  recomputation) alongside the precomputed `facilitySerial`/`deptSerial`
- `AlertLogEntity` — unified log for sent/received alerts, both transports;
  now carries `reason` (IncidentReason) alongside `priorityLevel`
- `SmsFallbackContactEntity` — phone-number directory for the GSM fallback
  (thin: only populated with a staff member's own number at registration)

## Active feature set
- **Onboarding**: cascading Wilaya → EstablishmentType → Institution →
  SubBranch → Department → Role picker, Material 3 exposed-dropdown fields,
  writes a `StaffMemberEntity` and an `SmsFallbackContactEntity`.
- **Encrypted serials**: `SerialEncoder` (AES/ECB single-block, deterministic
  by design — see its doc comment for why that's intentional, not a bug).
  Shared key via `AppKeyProvider` — currently an all-zero DEV placeholder;
  real key must be injected via `local.properties`/CI secret
  (`ILINE_SERIAL_KEY_HEX`) before any real deployment.
- **SMS fallback — redesigned after device testing**: sends TWO separate
  SMS per alert:
  1. A plain human-readable text SMS (visible in any Messages app)
  2. A binary **data SMS** on port `6474` (`SmsDispatcher.DATA_SMS_PORT`)
     carrying the actual routing payload, invisible outside this app.
  `SmsDataPayloadReceiver` (not the old `SmsIncomingReceiver`, which was
  removed) is what actually triggers alert ingestion.
- **FCM receiver**: fully functional for RECEIVING (subscribe/receive is
  all the client SDK supports). SENDING requires the Cloud Function in
  `/functions` — see below.
- **DND-bypass alarm**: `AlertRingtonePlayer`, `USAGE_ALARM` + max
  `STREAM_ALARM` volume + looping vibration. **No auto-timeout** — loops
  until "J'arrive" is tapped, by explicit product decision (an earlier
  30s auto-stop was found to defeat the entire point and was removed).
- **Master on/off switch** (`Prefs.appEnabled`): reachable from both
  Settings and a header switch on the Dashboard (same underlying pref),
  plus a quick-deactivate action embedded in the connection-status
  notification itself. When off: `AlertIngestion.ingest()` short-circuits
  entirely (no log, no notification, no alarm) and the foreground service
  stops. Re-activating requires opening the app — there's nothing to tap
  once the notification is gone (deliberate, see `ConnectionForegroundService`'s
  doc comment).
- **Connection-status foreground service**: 🟢/🟠 online/offline indicator.
  Its visibility toggle actually works now — previously the toggle
  restarted the service unconditionally regardless of the chosen state,
  which is why "hide" never hid anything. Fixed: the service simply
  doesn't run when the preference (or the master switch) is off, since a
  foreground service cannot exist without SOME visible notification
  (Android 8+ platform rule, not something this app can bypass).
- **Incident reasons** (`IncidentReason`): structured motif selection
  (Bagarre/Altercation, Renfort/Assistance, Urgence médicale, Agression,
  Autre) alongside priority — shown in the Dashboard send flow and in
  Logs, transmitted as a compact ordinal in the SMS data payload and as a
  named field in the FCM data payload.
- **In-app updates**: `UpdateChecker` polls `update.json` on the repo's
  `main` branch once/day (+ on-demand from Settings), `UpdateDownloader`
  uses `DownloadManager` + `FileProvider` to install. Full publish
  workflow in `UPDATE_NOTES.md`.
- **Logs screen**: swipe-to-delete (with Undo snackbar) + "Effacer tout"
  with confirmation, wired to real `@Delete`/`DELETE FROM alert_logs`
  DAO methods.
- **Branding/footer**: copyright + live `BuildConfig.VERSION_NAME` shown
  on Settings and Onboarding.

## Cloud Function (FCM send path)
`/functions/index.js` — deployable Firebase Cloud Function (`publishAlert`)
using the Admin SDK to publish to `fac_<facilitySerial>_<deptSerial>`
topics, gated by a shared-secret header. **Not deployed by me** — I have no
Firebase/GCP access; the code is real and deployable, but someone with
project access needs to run through `CLOUD_FUNCTION_NOTES.md` to actually
stand it up. Until `CLOUD_FUNCTION_URL` is configured (Gradle property /
CI secret), `RemoteAlertPublisher.publish()` returns `NotConfigured` and
the app silently continues on SMS-only — this is intentional graceful
degradation, not a crash risk.

## Resolved issues (this round, post device-testing)
1. SMS body showed raw routing codes → split into human-text SMS + hidden binary data SMS
2. No way to delete log entries → swipe-to-delete + clear-all added
3. "Hide status indicator" toggle didn't hide anything → real fix (service doesn't run when off, not just lower notification priority)
4. No structured incident context beyond priority → `IncidentReason` added
5. Alarm auto-stopped after 30s regardless of acknowledgment → auto-stop removed entirely
6. Master toggle buried in Settings only → added to Dashboard header + notification quick action
7. FCM send had no backend at all → real Cloud Function written + client wired to call it once deployed
8. No visible branding/version → footer added to Settings + Onboarding
9. No persistent project-memory doc → this file

## Known gaps / outstanding roadmap
- **Cloud Function not yet deployed** by anyone — code exists, deployment is a manual step (`CLOUD_FUNCTION_NOTES.md`).
- **Per-recipient SMS delivery confirmation** still absent (sent-to-OS vs. carrier-confirmed-delivered aren't distinguished).
- **`SmsFallbackContactEntity` directory is thin** — no cross-device contact sync; a device only knows numbers it registered itself.
- **Destructive DB migrations** — must become real `Migration` objects before any release with data worth keeping.
- **No automated tests** — JUnit/Espresso dependencies exist, no test files yet.
- **`isMinifyEnabled = false`** — fine for now, needs real ProGuard/R8 rules before a release build.
- **Update system has no integrity check beyond Android's own APK-signature verification** — see `UPDATE_NOTES.md`'s "Known limitation" section.
- **Alarm has no foreground-service backing** — currently a plain `MediaPlayer` triggered from a `BroadcastReceiver`; likely reliable in practice (active audio playback is one of the OS's background-keep-alive signals) but not as bulletproof as promoting it to its own foreground service would be. Worth hardening if missed alarms are ever reported.
- **No multi-step onboarding wizard** — the cascade is still a single scrollable screen (with better Material styling now), not a guided step-by-step flow.
- **Launcher icon is a hand-drawn vector**, not designed in a proper icon tool — functional and reasonably modern-looking, but worth a real design pass eventually.
