package com.ilinetech.emergency.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ilinetech.emergency.alert.AlertIngestion
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.sms.SmsPayloadBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Receives incoming SMS, checks whether the body matches the alert payload
 * shape (SmsPayloadBuilder.parse returns null for any ordinary SMS — this
 * receiver does nothing and lets normal SMS apps handle those), and if it
 * matches, hands off to the same AlertIngestion path FCM uses.
 *
 * Deliberately does NOT check here whether this device's registered profile
 * actually matches the facility/dept serials in the payload — AlertIngestion
 * logs and notifies unconditionally once parsed. Rationale: the serials are
 * already the addressing mechanism (only the intended facility+department's
 * devices would have a registered SmsFallbackContactEntity that caused the
 * sender to dial this number in the first place — see SmsDispatcher). Filtering
 * again here would be redundant unless a wrong-number/relay scenario needs
 * defending against, which the StaffMemberDao.findMatchingProfiles query
 * (already built) is available for wiring in later.
 *
 * Manifest requirement: apps targeting Android 4.4+ can only receive
 * SMS_RECEIVED_ACTION as a non-default SMS app (this app doesn't want to be
 * the default SMS handler) — that's exactly this receiver's use case and
 * requires only the RECEIVE_SMS permission, not full default-SMS-app status.
 */
class SmsIncomingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Multipart SMS arrive as separate PDUs in the same intent — concatenate
        // before parsing, since SmsPayloadBuilder expects the full body.
        val fullBody = messages.joinToString(separator = "") { it.messageBody }
        val senderAddress = messages.first().originatingAddress ?: "Inconnu"

        val payload = SmsPayloadBuilder.parse(fullBody) ?: return // not one of ours

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AlertIngestion.ingest(
                    context = context.applicationContext,
                    transport = AlertTransport.SMS,
                    facilitySerial = payload.facilitySerial,
                    deptSerial = payload.deptSerial,
                    groupId = payload.groupId,
                    priorityCode = payload.priority.smsCode,
                    message = payload.message,
                    senderLabel = senderAddress
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
