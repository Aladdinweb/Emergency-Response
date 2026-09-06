package com.ilinetech.emergency.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ilinetech.emergency.core.data.AppDatabase
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.model.TriageLevel
import com.ilinetech.emergency.fcm.AlertAckReceiver
import java.util.concurrent.TimeUnit

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Single entry point for "an alert arrived, regardless of transport."
 * EmergencyMessagingService (FCM) and SmsIncomingReceiver (GSM fallback)
 * both call [ingest] instead of duplicating de-dupe/log/notify logic — this
 * is also what makes the de-dupe window actually work: whichever transport
 * arrives second sees the first one's log row and backs off.
 */
object AlertIngestion {

    private const val CHANNEL_ID = "emergency_alerts"
    private val DEDUPE_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(2)

    /** Returns the inserted log row id, or null if this was a duplicate of an already-logged alert
     *  OR if the app is currently disabled via the Settings master switch — see Prefs.appEnabled. */
    suspend fun ingest(
        context: Context,
        transport: AlertTransport,
        facilitySerial: String,
        deptSerial: String,
        groupId: String,
        priorityCode: Int,
        message: String,
        senderLabel: String
    ): Long? {
        if (!com.ilinetech.emergency.core.data.Prefs(context).appEnabled) return null

        val dao = AppDatabase.getInstance(context).alertLogDao()
        val now = System.currentTimeMillis()

        val duplicates = dao.countRecentDuplicates(
            facilitySerial, deptSerial, groupId, message,
            windowStart = now - DEDUPE_WINDOW_MILLIS, windowEnd = now
        )
        if (duplicates > 0) return null

        val logId = dao.insert(
            AlertLogEntity(
                direction = AlertDirection.INCOMING,
                transport = transport,
                facilitySerial = facilitySerial,
                deptSerial = deptSerial,
                groupId = groupId,
                priorityLevel = priorityCode,
                message = message,
                counterpartLabel = senderLabel,
                sentAtEpochMillis = now
            )
        )

        val priority = runCatching { TriageLevel.fromSmsCode(priorityCode) }
            .getOrDefault(TriageLevel.MODERATE)

        postNotification(context, logId, senderLabel, message, priority)

        if (priority == TriageLevel.CRITICAL) {
            AlertBroadcastReceiver.broadcastTrigger(context)
        }

        return logId
    }

    private fun postNotification(
        context: Context,
        logId: Long,
        senderLabel: String,
        body: String,
        priority: TriageLevel
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val ackIntent = Intent(context, AlertAckReceiver::class.java).apply {
            putExtra(AlertAckReceiver.EXTRA_LOG_ID, logId)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context, logId.toInt(), ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (emoji, colorInt) = when (priority) {
            TriageLevel.CRITICAL -> "\uD83D\uDD34" to android.graphics.Color.parseColor(priority.colorHex)
            TriageLevel.MODERATE -> "\uD83D\uDFE0" to android.graphics.Color.parseColor(priority.colorHex)
            TriageLevel.LOW -> "\uD83D\uDFE2" to android.graphics.Color.parseColor(priority.colorHex)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // TODO: replace with app icon asset
            .setColor(colorInt)
            .setColorized(priority == TriageLevel.CRITICAL)
            .setContentTitle("$emoji $senderLabel — ${priority.label}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(0, "J'arrive", ackPendingIntent)
            .build()

        nm.notify(logId.toInt(), notification)
    }
}
