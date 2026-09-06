# Cloud Function deployment — enabling real-time FCM dispatch

This is what turns the "online" path in the spec from a stub into something
that actually sends push notifications over Wi-Fi/4G, with SMS remaining
the offline fallback. Without this, the app already works — SMS is fully
functional — this only adds the FCM path on top.

## 1. Prerequisites (once, in Termux)

```bash
pkg install nodejs -y
npm install -g firebase-tools
```

## 2. Log in to Firebase

Termux has no GUI browser session for OAuth, so use the no-localhost flow:
```bash
firebase login --no-localhost
```
It prints a URL — open it in Chrome on your phone, sign in, copy the code
it gives you back into Termux when prompted.

## 3. Connect this folder to your Firebase project

```bash
cd ~/emergency-response-app
firebase init functions
```
- Select "Use an existing project" → pick the Firebase project you created
  for `google-services.json` earlier.
- When asked about language, choose JavaScript.
- When asked whether to overwrite existing files: say **no** to
  `functions/package.json` and `functions/index.js` — the real ones are
  already in this repo; you only need `firebase init` to create
  `.firebaserc` and `firebase.json` linking this folder to your project.

## 4. Set the shared secret

Generate one the same way as the serial key:
```bash
openssl rand -hex 16
```
Then set it for the deployed function:
```bash
firebase functions:config:set app.shared_secret="<paste the generated value>"
```

Add the **same value** to the Android side — either your local
`local.properties`:
```
CLOUD_FUNCTION_SHARED_SECRET=<same value>
```
or as a GitHub Actions secret (`CLOUD_FUNCTION_SHARED_SECRET`) if building
via CI, following the same pattern as `ILINE_SERIAL_KEY_HEX` in
`BUILD_NOTES.md`.

## 5. Install dependencies and deploy

```bash
cd functions
npm install
cd ..
firebase deploy --only functions:publishAlert
```

The command output includes the deployed function's HTTPS URL, something
like:
```
https://us-central1-<your-project-id>.cloudfunctions.net/publishAlert
```

## 6. Point the Android app at it

Add to `local.properties` (or the equivalent CI secret,
`CLOUD_FUNCTION_URL`):
```
CLOUD_FUNCTION_URL=https://us-central1-<your-project-id>.cloudfunctions.net/publishAlert
```

Rebuild. `RemoteAlertPublisher` will now actually call this endpoint
instead of returning `NotConfigured` — check Firebase Console → Functions
→ Logs after a test send to confirm it's being hit.

## What this does NOT cover
- **Testing the full round trip** requires two real devices (or one device
  plus the Firebase Console's "send test message" tool) — I can't verify
  this myself since I have no Firebase project access.
- **Rate limiting / abuse protection** beyond the shared-secret check —
  fine for a small trusted user base, would need hardening (e.g. Firebase
  App Check, per-caller rate limits) before wider deployment.
- **Cost**: Cloud Functions has a free tier generous enough for this app's
  likely volume, but it isn't literally free at unlimited scale — worth
  keeping an eye on Firebase Console → Usage if this sees heavy use.
