# DND-bypass alarm + foreground service — wiring notes

## AndroidManifest.xml additions

```xml
<receiver
    android:name=".alert.AlertBroadcastReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.ilinetech.emergency.action.TRIGGER_ALERT" />
    </intent-filter>
</receiver>

<service
    android:name=".service.ConnectionForegroundService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

`foregroundServiceType="dataSync"` is the closest fit for a connectivity
monitor on Android 14+, which requires every foreground service to declare
a type. If Play Store review pushes back, `remoteMessaging` is the other
plausible fit given this app's whole purpose is messaging-adjacent — pick
whichever your Play Console submission ends up categorizing more cleanly.

## Permissions (add to <manifest>, above <application>)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ runtime permission -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" /> <!-- setStreamVolume on STREAM_ALARM -->
```

`POST_NOTIFICATIONS` must also be requested at runtime on Android 13+
(`ActivityCompat.requestPermissions`) before `startForeground()` or
`NotificationManager.notify()` will actually show anything — not wired to
an Activity yet since onboarding UI doesn't exist in this scaffold.

## Where things get started
- `ConnectionForegroundService.start(context)` should be called once, from
  wherever the app first launches after onboarding is complete (Application
  class `onCreate()` or the Dashboard screen) — not wired to a call site yet.
- Nothing currently calls `AlertBroadcastReceiver.broadcastTrigger()` except
  `EmergencyMessagingService` for CRITICAL FCM alerts. The SMS receiver
  (next slice) will call the same method for CRITICAL alerts arriving via
  the GSM fallback path.

## Known simplification, flagged for later hardening
`AlertRingtonePlayer` auto-stops after 30s if never acknowledged. Whether
that's the right timeout — or whether it should re-trigger/escalate (e.g.
notify a supervisor role) if still unacknowledged — is a policy decision,
not a technical one; worth deciding explicitly rather than leaving the
30s constant as the de facto answer.
