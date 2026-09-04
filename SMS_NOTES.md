# SMS fallback slice — wiring notes

## AndroidManifest.xml additions

```xml
<receiver
    android:name=".sms.SmsIncomingReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

`exported="true"` is required here — the system, not another app component,
delivers this broadcast, but Android still requires exported="true" for any
receiver with an external <intent-filter> action like this one.
`android:permission="android.permission.BROADCAST_SMS"` restricts who can
send this broadcast to the receiver to the system itself, which is what
actually keeps this safe despite exported="true".

## Permissions (add to <manifest>, above <application>)

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
```

Both are **dangerous** permissions requiring a runtime request
(`ActivityCompat.requestPermissions`) before either sending or receiving
will actually work — not wired to an Activity yet since onboarding UI
doesn't exist in this scaffold. Google Play also requires SMS permissions
to go through the "core app functionality" declaration form if this app
isn't the user's default SMS handler (it isn't, and shouldn't be) — budget
time for that review step before a Play Store submission.

## What's NOT handled yet, on purpose
- **Delivery confirmation**: `SmsDispatcher.sendAlert()` reports how many
  sends were handed to `SmsManager`, not confirmed-delivered. Real delivery
  tracking needs a `PendingIntent` + a second receiver for
  `SMS_DELIVERED_ACTION`. Worth adding once the Logs UI needs a
  sent/delivered/failed distinction rather than just sent/not-sent.
- **Populating `SmsFallbackContactEntity`**: nothing writes to this table
  yet. The natural point is onboarding (a staff member registering adds
  their own number as a fallback contact for their own facility+dept) plus
  a directory sync so devices know about contacts they haven't personally
  exchanged numbers with — neither exists yet since onboarding UI is a
  later slice.
- **Wrong-number / spoofed-sender defense**: see the doc comment in
  `SmsIncomingReceiver` — `StaffMemberDao.findMatchingProfiles()` already
  exists if this needs to become "ignore this alert unless MY registered
  profile matches the serials," it's just not wired in yet.
