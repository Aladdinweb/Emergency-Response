package com.ilinetech.emergency.core.sms

import com.ilinetech.emergency.core.model.IncidentReason
import com.ilinetech.emergency.core.model.TriageLevel

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * REDESIGNED after device testing showed the old single-SMS format (a
 * human header glued to a machine payload line in the same message body)
 * still looked messy in practice. The GSM fallback now sends TWO separate
 * SMS per alert, each doing one job:
 *
 *  1. A plain, short, human-readable text message — what shows up in the
 *     recipient's normal Messages app if they open the thread. No routing
 *     codes in it at all.
 *  2. A binary "data SMS" (SmsManager.sendDataMessage, port-addressed) —
 *     invisible in any normal messaging app, delivered only to a receiver
 *     in THIS app listening on the same port (see SmsDataPayloadReceiver).
 *     This carries the actual routing metadata: URG#facilitySerial#
 *     deptSerial#groupId#priorityCode#reasonOrdinal.
 *
 * The free-text message the sender typed travels ONLY in the human-readable
 * SMS — the data payload deliberately does not carry it, both to stay well
 * under the ~140-byte single-segment limit for data SMS (which has no
 * public multipart API, unlike text SMS) and because IncidentReason already
 * gives the receiving app's notification/log something structured and
 * useful to show without needing the free text at all.
 */
data class AlertPayload(
    val facilitySerial: String,
    val deptSerial: String,
    val groupId: String,
    val priority: TriageLevel,
    val reason: IncidentReason,
    val message: String
)

/** Parsed straight from the data-SMS payload — deliberately message-less, see class doc above. */
data class SmsRoutingPayload(
    val facilitySerial: String,
    val deptSerial: String,
    val groupId: String,
    val priority: TriageLevel,
    val reason: IncidentReason
)

object SmsPayloadBuilder {

    private const val TAG = "URG"
    private const val DELIMITER = "#"
    private const val FIELD_COUNT = 6

    private fun priorityEmoji(priority: TriageLevel): String = when (priority) {
        TriageLevel.CRITICAL -> "\uD83D\uDD34"
        TriageLevel.MODERATE -> "\uD83D\uDFE0"
        TriageLevel.LOW -> "\uD83D\uDFE2"
    }

    /** The message a human sees if they open the SMS thread directly — no routing codes. */
    fun buildHumanText(payload: AlertPayload): String {
        val cleanMessage = payload.message.trim()
        val body = if (cleanMessage.isNotEmpty()) cleanMessage else payload.reason.label
        return "${priorityEmoji(payload.priority)} ${payload.priority.label} — ${payload.reason.label}\n$body"
    }

    /**
     * The compact binary payload sent as a data SMS. Encoded as UTF-8 bytes
     * by the caller (SmsDispatcher) — this just builds the delimited string.
     */
    fun buildRoutingPayload(payload: AlertPayload): String {
        val fields = listOf(
            TAG,
            payload.facilitySerial,
            payload.deptSerial,
            payload.groupId,
            payload.priority.smsCode.toString(),
            payload.reason.ordinal.toString()
        )
        return fields.joinToString(DELIMITER)
    }

    /** Parses a data-SMS body (raw bytes decoded as UTF-8 text) back into its fields. */
    fun parseRoutingPayload(raw: String): SmsRoutingPayload? {
        val tagIndex = raw.indexOf("$TAG$DELIMITER")
        if (tagIndex == -1) return null

        val parts = raw.substring(tagIndex).trim().split(DELIMITER, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT || parts[0] != TAG) return null

        val priorityCode = parts[4].toIntOrNull() ?: return null
        val priority = runCatching { TriageLevel.fromSmsCode(priorityCode) }.getOrNull() ?: return null

        val reasonOrdinal = parts[5].toIntOrNull() ?: return null
        val reason = IncidentReason.entries.getOrNull(reasonOrdinal) ?: return null

        return SmsRoutingPayload(
            facilitySerial = parts[1],
            deptSerial = parts[2],
            groupId = parts[3],
            priority = priority,
            reason = reason
        )
    }
}
