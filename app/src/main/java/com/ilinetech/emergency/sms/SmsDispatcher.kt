package com.ilinetech.emergency.sms

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.ilinetech.emergency.core.data.AppDatabase
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.sms.AlertPayload
import com.ilinetech.emergency.core.sms.SmsPayloadBuilder
import java.nio.charset.StandardCharsets

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Sends TWO SMS per recipient per alert (see SmsPayloadBuilder's class doc
 * for why): a normal human-readable text message, and a binary data SMS on
 * DATA_SMS_PORT carrying the routing payload that SmsDataPayloadReceiver
 * picks up on the other end. Requires android.permission.SEND_SMS.
 */
object SmsDispatcher {

    /** Arbitrary private port for this app's data SMS — must match SmsDataPayloadReceiver's manifest filter. */
    const val DATA_SMS_PORT: Short = 6474

    /**
     * Returns the number of recipients both messages were successfully
     * handed off to SmsManager for (a recipient only counts if BOTH sends
     * succeeded — a routing payload with no human-readable counterpart, or
     * vice versa, isn't a coherent delivered alert).
     */
    suspend fun sendAlert(context: Context, payload: AlertPayload): Int {
        val contactDao = AppDatabase.getInstance(context).smsFallbackContactDao()
        val recipients = contactDao.getForTarget(payload.facilitySerial, payload.deptSerial)
        if (recipients.isEmpty()) return 0

        val humanText = SmsPayloadBuilder.buildHumanText(payload)
        val routingPayload = SmsPayloadBuilder.buildRoutingPayload(payload)
        val routingBytes = routingPayload.toByteArray(StandardCharsets.UTF_8)

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var sentCount = 0
        for (contact in recipients) {
            val humanSent = runCatching {
                val parts = smsManager.divideMessage(humanText)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phoneNumber, null, humanText, null, null)
                }
            }.isSuccess

            val routingSent = runCatching {
                smsManager.sendDataMessage(contact.phoneNumber, null, DATA_SMS_PORT, routingBytes, null, null)
            }.isSuccess

            if (humanSent && routingSent) sentCount++
            // Partial failure (one send succeeded, one didn't) isn't rolled
            // back — there's no atomic "send both or neither" primitive for
            // SMS. A human-only send without routing means the recipient
            // sees a readable alert but it won't trigger the receiving
            // app's notification/alarm; logged at the call site's summary
            // level only, not per-recipient — see SMS_NOTES.md for the
            // known-gaps note on per-recipient delivery visibility.
        }

        AppDatabase.getInstance(context).alertLogDao().insert(
            AlertLogEntity(
                direction = AlertDirection.OUTGOING,
                transport = AlertTransport.SMS,
                facilitySerial = payload.facilitySerial,
                deptSerial = payload.deptSerial,
                groupId = payload.groupId,
                priorityLevel = payload.priority.smsCode,
                reason = payload.reason.name,
                message = payload.message,
                counterpartLabel = "$sentCount destinataire(s)",
                sentAtEpochMillis = System.currentTimeMillis()
            )
        )

        return sentCount
    }
}
