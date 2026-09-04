package com.ilinetech.emergency.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ilinetech.emergency.core.data.AppDatabase
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.model.TriageLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Handles both lifecycle events FCM cares about:
 *  - onNewToken: persist the refreshed token against the active profile so
 *    the next alert this device *sends* carries a resolvable token if a
 *    future feature needs direct (non-topic) messaging.
 *  - onMessageReceived: parse the data payload, de-duplicate against the SMS
 *    fallback path (same alert may arrive via both transports), log it, and
 *    post the interactive notification with the "J'arrive" action.
 *
 * Data payload keys expected (mirrors AlertPayload / the SMS field order):
 *   facilitySerial, deptSerial, groupId, priority (1/2/3), message, senderLabel
 *
 * Full DND-bypass alarm behavior (AudioAttributes.USAGE_ALARM playback) lives
 * in the `alert` package (next slice) and is invoked from here once that's
 * wired in — for now this posts a high-priority heads-up notification, which
 * on its own already bypasses most silent-mode configurations on Android 8+
 * when the channel importance is IMPORTANCE_HIGH.
 */
class EmergencyMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            val prefs = com.ilinetech.emergency.core.data.Prefs(applicationContext)
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
        val body = data["message"].orEmpty()
        val senderLabel = data["senderLabel"] ?: "Alerte"

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.alertLogDao()

            val now = System.currentTimeMillis()
            val dedupeWindowMillis = TimeUnit.MINUTES.toMillis(2)
            val duplicates = dao.countRecentDuplicates(
                facilitySerial, deptSerial, groupId, body,
                windowStart = now - dedupeWindowMillis, windowEnd = now
            )
            if (duplicates > 0) return@launch // already logged via SMS fallback or a retry

            val logId = dao.insert(
                AlertLogEntity(
                    direction = AlertDirection.INCOMING,
                    transport = AlertTransport.FCM,
                    facilitySerial = facilitySerial,
                    deptSerial = deptSerial,
                    groupId = groupId,
                    priorityLevel = priorityCode,
                    message = body,
                    counterpartLabel = senderLabel,
                    sentAtEpochMillis = now
                )
            )

            val priority = runCatching { TriageLevel.fromSmsCode(priorityCode) }
                .getOrDefault(TriageLevel.MODERATE)

            postNotification(logId = logId, senderLabel = senderLabel, body = body, priority = priority)

            if (priority == TriageLevel.CRITICAL) {
                com.ilinetech.emergency.alert.AlertBroadcastReceiver.broadcastTrigger(applicationContext)
            }
        }
    }

    private fun postNotification(logId: Long, senderLabel: String, body: String, priority: TriageLevel) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Alertes d'urgence", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de sécurité et d'urgence en temps réel"
                enableVibration(true)
                setBypassDnd(true)
            }
            nm.createNotificationChannel(channel)
        }

        val ackIntent = Intent(this, AlertAckReceiver::class.java).apply {
            putExtra(AlertAckReceiver.EXTRA_LOG_ID, logId)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            this, logId.toInt(), ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // TODO: replace with app icon asset
            .setContentTitle("$senderLabel — ${priority.label}")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(0, "J'arrive", ackPendingIntent)
            .build()

        nm.notify(logId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "emergency_alerts"
    }
}
