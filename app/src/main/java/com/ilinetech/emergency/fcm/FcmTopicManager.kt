package com.ilinetech.emergency.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import kotlinx.coroutines.tasks.await

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Every device subscribes to exactly one FCM topic per registered, active
 * profile: the combination of facility serial + department serial. This is
 * what makes dispatch "targeted" per the spec — an alert published to
 * `topic/<facilitySerial>_<deptSerial>` only reaches staff of that role at
 * that exact sub-branch, nowhere else.
 *
 * Topic names must match `[a-zA-Z0-9-_.~%]{1,900}`; SerialEncoder's Crockford
 * Base32 output (0-9, A-Z only) is already safe, so no extra sanitizing.
 */
object FcmTopicManager {

    private const val TOPIC_PREFIX = "fac"

    fun topicNameFor(facilitySerial: String, deptSerial: String): String =
        "${TOPIC_PREFIX}_${facilitySerial}_$deptSerial"

    suspend fun subscribe(profile: StaffMemberEntity) {
        val topic = topicNameFor(profile.facilitySerial, profile.deptSerial)
        FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
    }

    suspend fun unsubscribe(profile: StaffMemberEntity) {
        val topic = topicNameFor(profile.facilitySerial, profile.deptSerial)
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
    }

    /**
     * Call when a profile's branch/role/group changes (not just deactivation)
     * so the device doesn't keep receiving alerts for a role it no longer holds.
     */
    suspend fun resubscribe(oldProfile: StaffMemberEntity?, newProfile: StaffMemberEntity) {
        if (oldProfile != null &&
            (oldProfile.facilitySerial != newProfile.facilitySerial || oldProfile.deptSerial != newProfile.deptSerial)
        ) {
            unsubscribe(oldProfile)
        }
        subscribe(newProfile)
    }
}
