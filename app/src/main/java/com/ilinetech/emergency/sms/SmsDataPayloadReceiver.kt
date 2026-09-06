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
import java.nio.charset.StandardCharsets

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Receives the binary data-SMS routing payload SmsDispatcher sends on
 * SmsDispatcher.DATA_SMS_PORT — invisible in any normal messaging app,
 * delivered only here. This, not the human-readable text SMS, is what
 * actually triggers AlertIngestion.
 *
 * Manifest requirement: registered for android.intent.action.DATA_SMS_RECEIVED
 * with a <data android:scheme="sms" android:port="6474"/> filter — a
 * different action and matching mechanism from the old text-SMS receiver
 * (which is why this replaces rather than extends it). Still just needs
 * RECEIVE_SMS permission, no new manifest permission required.
 */
class SmsDataPayloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val raw = String(messages[0].userData, StandardCharsets.UTF_8)
        val senderAddress = messages[0].originatingAddress ?: "Inconnu"

        val routing = SmsPayloadBuilder.parseRoutingPayload(raw) ?: return // not one of ours

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AlertIngestion.ingest(
                    context = context.applicationContext,
                    transport = AlertTransport.SMS,
                    facilitySerial = routing.facilitySerial,
                    deptSerial = routing.deptSerial,
                    groupId = routing.groupId,
                    priorityCode = routing.priority.smsCode,
                    reason = routing.reason,
                    message = "", // deliberately not transmitted over the data channel — see SmsPayloadBuilder
                    senderLabel = senderAddress
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
