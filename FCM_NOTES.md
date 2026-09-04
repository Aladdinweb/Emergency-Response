# FCM slice — wiring notes

## AndroidManifest.xml additions

```xml
<service
    android:name=".fcm.EmergencyMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<receiver
    android:name=".fcm.AlertAckReceiver"
    android:exported="false" />
```

## app/build.gradle dependencies

```gradle
implementation platform('com.google.firebase:firebase-bom:33.1.2')
implementation 'com.google.firebase:firebase-messaging-ktx'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1'
```
(`kotlinx-coroutines-play-services` provides the `.await()` extension used in
`FcmTopicManager` on Firebase's Task objects.)

## Project-level build.gradle
```gradle
plugins {
    id 'com.google.gms.google-services' version '4.4.2' apply false
}
```
and in `app/build.gradle`:
```gradle
plugins {
    id 'com.google.gms.google-services'
}
```

## google-services.json
Firebase Console → Project settings → your Android app → download
`google-services.json` → place it at `app/google-services.json`.
**Do not commit this if the Firebase project is shared/private-sensitive** —
it's not a secret key by itself (it's safe to ship inside a released APK),
but keep it out of a public repo if you'd rather not expose your project ID
and package name publicly. For a public repo, add `app/google-services.json`
to `.gitignore` and document that contributors need to supply their own.

## Where topic subscription gets called
`FcmTopicManager.subscribe(profile)` should be called right after a
`StaffMemberEntity` is inserted during onboarding (next relevant slice: UI),
and `resubscribe(...)` when a profile's branch/role changes. Not wired to a
call site yet since onboarding UI doesn't exist in this scaffold yet.
