package com.ilinetech.emergency.fcm

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * DELIBERATE STUB — read before wiring a call site to this.
 *
 * The FCM client SDK (what runs on the phone) can only SUBSCRIBE to and
 * RECEIVE topic messages — see FcmTopicManager. It cannot PUBLISH to a
 * topic. Publishing requires a trusted server holding either:
 *   (a) a Firebase Admin SDK service-account credential, or
 *   (b) an OAuth2 access token for the FCM HTTP v1 API,
 * neither of which may ever be embedded in an Android app — either one
 * would let anyone who decompiles the APK send arbitrary push notifications
 * to every device subscribed to any topic.
 *
 * This app currently has no backend. The SMS fallback path (SmsDispatcher)
 * is fully functional without one; the FCM "online" path described in the
 * spec needs one of:
 *   - A small Cloud Function (Node/Python) exposed as an HTTPS callable
 *     function, invoked from this class via a Retrofit/OkHttp call, which
 *     then uses the Admin SDK server-side to publish to the topic.
 *   - Writing the alert to Firestore/Realtime Database instead, with a
 *     Cloud Function trigger that publishes to FCM on document creation.
 *
 * Until one of those exists, [publish] intentionally does nothing but
 * report failure — this is safer than silently pretending the alert went
 * out over a channel that doesn't actually exist yet.
 */
object RemoteAlertPublisher {

    sealed class PublishResult {
        object NotImplemented : PublishResult()
        data class Success(val messageId: String) : PublishResult()
        data class Failure(val reason: String) : PublishResult()
    }

    suspend fun publish(
        facilitySerial: String,
        deptSerial: String,
        groupId: String,
        priorityCode: Int,
        message: String,
        senderLabel: String
    ): PublishResult {
        // TODO: replace with a call to your backend endpoint once it exists, e.g.:
        //   val response = backendApi.publishAlert(PublishAlertRequest(...))
        //   return if (response.isSuccessful) PublishResult.Success(response.body()!!.messageId)
        //          else PublishResult.Failure(response.errorBody()?.string() ?: "unknown error")
        return PublishResult.NotImplemented
    }
}
