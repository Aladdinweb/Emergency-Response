package com.ilinetech.emergency.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Decouples "an alert arrived" (FCM service / SMS receiver) from "play the
 * full-volume alarm" (AlertRingtonePlayer), matching the spec's call for a
 * dedicated background BroadcastReceiver for this job. Only CRITICAL/
 * EMERGENCY alerts trigger the DND-bypass alarm — LOW and MODERATE alerts
 * get the standard heads-up notification from EmergencyMessagingService
 * without hijacking the device's audio.
 *
 * Senders: EmergencyMessagingService.postNotification() and (once built)
 * SmsIncomingReceiver both send ACTION_TRIGGER_ALERT for CRITICAL alerts.
 * fcm.AlertAckReceiver calls AlertRingtonePlayer.stop() directly on ack
 * rather than routing back through this receiver — acknowledging is a
 * device-local action with no need for the broadcast indirection.
 */
class AlertBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_ALERT) return
        AlertRingtonePlayer.start(context)
    }

    companion object {
        const val ACTION_TRIGGER_ALERT = "com.ilinetech.emergency.action.TRIGGER_ALERT"

        fun broadcastTrigger(context: Context) {
            val intent = Intent(ACTION_TRIGGER_ALERT).apply {
                setPackage(context.packageName) // explicit target — required for implicit broadcasts on Android 8+
            }
            context.sendBroadcast(intent)
        }
    }
}
