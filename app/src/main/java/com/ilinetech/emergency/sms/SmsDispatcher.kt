package com.ilinetech.emergency.sms

import android.content.Context
import android.telephony.SmsManager
import com.ilinetech.emergency.core.data.AppDatabase
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.model.TriageLevel
import com.ilinetech.emergency.core.sms.AlertPayload
import com.ilinetech.emergency.core.sms.SmsPayloadBuilder

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Sends an alert over the GSM/SMS fallback path to every contact registered
 * for the target (facilitySerial, deptSerial) — see SmsFallbackContactEntity
 * for why this needs a local phone-number directory rather than a topic.
 *
 * This is the sending device's counterpart to SmsIncomingReceiver on the
 * other end. Requires android.permission.SEND_SMS (runtime-requested,
 * dangerous permission group) — caller must have already obtained it.
 */
object SmsDispatcher {

    /**
     * Sends [payload] to every registered fallback contact for its
     * (facilitySerial, deptSerial) target. Returns the number of recipients
     * the SMS was handed off to SmsManager for — does not confirm carrier
     * delivery (that requires a separate PendingIntent-based delivery
     * report, worth adding once the Logs UI needs delivery status).
     *
     * Also logs a single OUTGOING entry (not one per recipient) since from
     * the sender's perspective this is one alert, not N separate ones.
     */
    suspend fun sendAlert(context: Context, payload: AlertPayload): Int {
        val contactDao = AppDatabase.getInstance(context).smsFallbackContactDao()
        val recipients = contactDao.getForTarget(payload.facilitySerial, payload.deptSerial)
        if (recipients.isEmpty()) return 0

        val body = SmsPayloadBuilder.build(payload)
        val smsManager = context.getSystemService(SmsManager::class.java)
            ?: SmsManager.getDefault()

        var sentCount = 0
        for (contact in recipients) {
            val parts = smsManager.divideMessage(body)
            runCatching {
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phoneNumber, null, body, null, null)
                }
            }.onSuccess { sentCount++ }
            // A failed send to one contact shouldn't abort sending to the rest —
            // log at the call site if per-recipient failure visibility is needed.
        }

        AppDatabase.getInstance(context).alertLogDao().insert(
            AlertLogEntity(
                direction = AlertDirection.OUTGOING,
                transport = AlertTransport.SMS,
                facilitySerial = payload.facilitySerial,
                deptSerial = payload.deptSerial,
                groupId = payload.groupId,
                priorityLevel = payload.priority.smsCode,
                message = payload.message,
                counterpartLabel = "$sentCount destinataire(s)",
                sentAtEpochMillis = System.currentTimeMillis()
            )
        )

        return sentCount
    }
}
