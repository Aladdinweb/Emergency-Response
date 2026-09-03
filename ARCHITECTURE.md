# Emergency & Security Response System — Architecture
© ILINE TECH BY FERAK ALADDIN

## Package hierarchy

```
com.ilinetech.emergency
├── core
│   ├── model              // Pure data classes / enums — no Android deps
│   │   ├── Hierarchy.kt       (Wilaya, EstablishmentType, Institution, SubBranch)
│   │   ├── StaffRole.kt       (role enum, grouped by department)
│   │   └── TriageLevel.kt     (LOW / MODERATE / CRITICAL)
│   │
│   ├── security
│   │   └── SerialEncoder.kt   // Encrypted facility/department serial generation
│   │
│   ├── sms
│   │   └── SmsPayloadBuilder.kt // Build + parse the offline SMS payload
│   │
│   └── data
│       ├── AppDatabase.kt     // Room database (next slice)
│       ├── entities/          // Room @Entity classes (next slice)
│       ├── dao/                // Room DAOs (next slice)
│       └── Prefs.kt           // SharedPreferences wrapper (next slice)
│
├── fcm                     // Firebase Cloud Messaging (next slice)
│   ├── EmergencyMessagingService.kt
│   └── FcmTopicManager.kt
│
├── sms
│   ├── SmsSendReceiver.kt     // SmsManager dispatch (next slice)
│   └── SmsIncomingReceiver.kt // Parses incoming fallback SMS (next slice)
│
├── alert
│   ├── AlertBroadcastReceiver.kt  // DND-bypass alarm trigger (next slice)
│   └── AlertRingtonePlayer.kt     // USAGE_ALARM playback
│
├── service
│   └── ConnectionForegroundService.kt // Persistent status service (next slice)
│
└── ui
    ├── onboarding/   // Cascading Wilaya -> Type -> Institution -> Branch picker
    ├── dashboard/
    ├── logs/
    └── settings/
```

## Design notes

- **`core.model` has zero Android dependencies** — plain Kotlin data classes/enums,
  so they're trivially unit-testable and reusable in both the app module and any
  future companion/watch module.
- **Encrypted serials** are derived, not stored-then-encrypted: `SerialEncoder`
  takes the hierarchy path and produces a deterministic, reversible-only-with-key
  serial, so two devices registering the same branch always converge on the same
  serial without a server round-trip (important for the offline/GSM path).
- **SMS payload format** (`URGENCY#FACILITY#DEPT#GROUP#PRIORITY#MESSAGE`) is
  encoded/decoded through one shared class so the online (FCM) and offline (SMS)
  paths can't drift out of sync — same `AlertPayload` model feeds both.
- Next slices, in suggested order: (1) Room persistence layer, (2) FCM service +
  topic manager, (3) SMS send/receive receivers, (4) DND-bypass alert +
  foreground service, (5) UI layouts.
