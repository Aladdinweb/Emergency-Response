package com.ilinetech.emergency.fcm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ilinetech.emergency.core.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Handles the "J'arrive" action tapped directly from the notification
 * (works even if the app process is dead). Marks the alert acknowledged,
 * cancels the notification, and — once the `alert` package's alarm player
 * is wired in — will also be the point that silences any looping DND-bypass
 * alarm sound tied to this alert.
 *
 * The sender is notified of the acknowledgment via a normal outbound FCM/SMS
 * message from a higher layer (ViewModel/repository), not from here — this
 * receiver only owns local device state.
 */
class AlertAckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
        if (logId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(context).alertLogDao()
                    .markAcknowledged(logId, System.currentTimeMillis())

                NotificationManagerCompat.from(context).cancel(logId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_LOG_ID = "extra_log_id"
    }
}
