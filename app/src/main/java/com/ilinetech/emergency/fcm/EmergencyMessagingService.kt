package com.ilinetech.emergency.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ilinetech.emergency.alert.AlertIngestion
import com.ilinetech.emergency.core.data.AppDatabase
import com.ilinetech.emergency.core.data.Prefs
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.model.TriageLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Handles both lifecycle events FCM cares about:
 *  - onNewToken: persist the refreshed token against the active profile so
 *    the next alert this device *sends* carries a resolvable token if a
 *    future feature needs direct (non-topic) messaging.
 *  - onMessageReceived: parse the data payload and hand off to
 *    AlertIngestion, which de-dupes against the SMS fallback path (same
 *    alert may arrive via both transports), logs it, posts the notification,
 *    and triggers the DND-bypass alarm for CRITICAL alerts. SmsDataPayloadReceiver
 *    goes through the exact same ingestion path so the two transports never
 *    diverge in behavior.
 *
 * Data payload keys expected (mirrors AlertPayload's fields):
 *   facilitySerial, deptSerial, groupId, priority (1/2/3), reason (IncidentReason.name), message, senderLabel
 */
class EmergencyMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            val prefs = Prefs(applicationContext)
            val activeId = prefs.activeStaffMemberId ?: return@launch
            AppDatabase.getInstance(applicationContext).staffMemberDao()
                .updateFcmToken(activeId, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val facilitySerial = data["facilitySerial"] ?: return
        val deptSerial = data["deptSerial"] ?: return
        val groupId = data["groupId"].orEmpty()
        val priorityCode = data["priority"]?.toIntOrNull() ?: TriageLevel.MODERATE.smsCode
        val reason = runCatching {
            com.ilinetech.emergency.core.model.IncidentReason.valueOf(data["reason"].orEmpty())
        }.getOrDefault(com.ilinetech.emergency.core.model.IncidentReason.AUTRE)
        val body = data["message"].orEmpty()
        val senderLabel = data["senderLabel"] ?: "Alerte"

        scope.launch {
            AlertIngestion.ingest(
                context = applicationContext,
                transport = AlertTransport.FCM,
                facilitySerial = facilitySerial,
                deptSerial = deptSerial,
                groupId = groupId,
                priorityCode = priorityCode,
                reason = reason,
                message = body,
                senderLabel = senderLabel
            )
        }
    }
}
