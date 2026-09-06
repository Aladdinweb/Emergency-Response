# SMS fallback — wiring notes (updated after device testing)

## REDESIGNED: two messages per alert, not one
The original single-SMS design (routing codes glued into the visible body)
looked unprofessional in the actual Messages app during device testing.
Now every alert sends TWO separate SMS to each fallback contact:

1. **Human-readable text SMS** — what a person sees opening the thread
   directly. No routing codes in it. Built by `SmsPayloadBuilder.buildHumanText()`.
2. **Binary data SMS** on port `6474` (`SmsDispatcher.DATA_SMS_PORT`) —
   invisible in any normal messaging app, delivered only to
   `SmsDataPayloadReceiver` in this app. Carries the actual routing
   metadata: `URG#facilitySerial#deptSerial#groupId#priorityCode#reasonOrdinal`.
   Built by `SmsPayloadBuilder.buildRoutingPayload()`.

The free-text message a sender types travels ONLY in the human-readable
SMS — the data payload deliberately excludes it (keeps it well under the
~140-byte single-segment limit for data SMS, which has no public multipart
API unlike text SMS). The receiving app shows the `IncidentReason` label
instead when logging/notifying from an SMS-sourced alert.

## AndroidManifest.xml (current, replaces the old single-receiver version)

```xml
<receiver
    android:name=".sms.SmsDataPayloadReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.intent.action.DATA_SMS_RECEIVED" />
        <data android:scheme="sms" android:port="6474" />
    </intent-filter>
</receiver>
```

Note the different action (`DATA_SMS_RECEIVED`, not `SMS_RECEIVED`) and the
`<data>` port filter — this is what routes binary SMS on that specific port
to this receiver instead of the general SMS inbox.

## Permissions (unchanged)
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
```
Same permissions cover both text and data SMS send/receive — no new
permission was needed for this redesign.

## What's NOT handled yet, on purpose
- **Delivery confirmation**: neither the human text SMS nor the data SMS
  has delivery tracking — `SmsDispatcher` only knows whether `SmsManager`
  accepted the send, not whether it reached the recipient. Needs a
  `PendingIntent` + `SMS_DELIVERED_ACTION` receiver per message, ideally
  tracked per-recipient in the Logs UI.
- **Partial-failure visibility**: if the human text sends but the data SMS
  fails (or vice versa) for a given recipient, that recipient isn't
  counted as successfully alerted, but nothing surfaces WHICH half failed.
  Fine for now; worth a richer per-recipient log if failures become common.
- **Populating `SmsFallbackContactEntity`**: still only self-registers a
  staff member's own number at onboarding. No directory sync across
  devices — a device only knows contacts it added itself.
- **Carrier data-SMS support**: binary/port-addressed SMS is part of the
  GSM spec and should work on any standard carrier, but hasn't been tested
  across multiple real carriers/networks yet. If data SMS silently fails
  on some network while text SMS works, that's the first thing to
  investigate (a small number of carriers restrict binary SMS on some
  routes).
