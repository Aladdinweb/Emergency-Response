package com.ilinetech.emergency.core.sms

import com.ilinetech.emergency.core.model.TriageLevel

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * An SMS this app sends has TWO parts in the same message body:
 *
 *   1. A human-readable header — what someone sees if they open the raw
 *      SMS thread directly (e.g. on a phone that doesn't have this app,
 *      or before opening the notification). Priority-coded with an emoji,
 *      readable at a glance.
 *   2. A machine-parseable payload line, unchanged in shape from before:
 *        URG#ENCRYPTED_FACILITY_SERIAL#DEPT_SERIAL#GROUP_ID#PRIORITY_LEVEL#MESSAGE
 *      SmsIncomingReceiver finds this line via its "URG#" prefix regardless
 *      of what human-readable text precedes it, so the two can never drift
 *      out of sync — the header is generated FROM the same payload fields,
 *      never entered separately.
 *
 * This fixes what raw parsing-format SMS bodies look like when a person
 * actually opens the thread (which happens in practice — see the very
 * first test send) — without touching the wire format SmsIncomingReceiver
 * already depends on.
 */
data class AlertPayload(
    val facilitySerial: String,
    val deptSerial: String,
    val groupId: String,
    val priority: TriageLevel,
    val message: String
)

object SmsPayloadBuilder {

    private const val TAG = "URG"
    private const val DELIMITER = "#"
    private const val FIELD_COUNT = 6

    /** '#' can't appear inside the message field since it's the delimiter. */
    private fun sanitizeMessage(message: String): String =
        message.replace(DELIMITER, "-").trim()

    private fun priorityEmoji(priority: TriageLevel): String = when (priority) {
        TriageLevel.CRITICAL -> "\uD83D\uDD34" // 🔴
        TriageLevel.MODERATE -> "\uD83D\uDFE0" // 🟠
        TriageLevel.LOW -> "\uD83D\uDFE2"      // 🟢
    }

    private fun machinePayload(payload: AlertPayload): String {
        val fields = listOf(
            TAG,
            payload.facilitySerial,
            payload.deptSerial,
            payload.groupId,
            payload.priority.smsCode.toString(),
            sanitizeMessage(payload.message)
        )
        return fields.joinToString(DELIMITER)
    }

    /**
     * Builds the full SMS body: a short human-readable header, then a blank
     * line, then the machine payload. Kept as compact as reasonably possible
     * since GSM-7 messages over ~160 chars split into multipart SMS — see
     * SmsDispatcher, which already handles multipart via divideMessage().
     */
    fun build(payload: AlertPayload): String {
        val header = "${priorityEmoji(payload.priority)} ${payload.priority.label} — ${payload.groupId}\n${sanitizeMessage(payload.message)}"
        return "$header\n\n${machinePayload(payload)}"
    }

    /**
     * Returns null if the SMS body doesn't contain a valid payload line
     * anywhere in it. Locates the payload by its "URG#" prefix rather than
     * assuming it's the whole body, so this still parses correctly whether
     * the body is old-format (payload only) or new-format (header + payload).
     */
    fun parse(smsBody: String): AlertPayload? {
        val tagIndex = smsBody.indexOf("$TAG$DELIMITER")
        if (tagIndex == -1) return null

        val payloadLine = smsBody.substring(tagIndex).trim()
        val parts = payloadLine.split(DELIMITER, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT || parts[0] != TAG) return null

        val priorityCode = parts[4].toIntOrNull() ?: return null
        val priority = runCatching { TriageLevel.fromSmsCode(priorityCode) }
            .getOrNull() ?: return null

        return AlertPayload(
            facilitySerial = parts[1],
            deptSerial = parts[2],
            groupId = parts[3],
            priority = priority,
            message = parts[5]
        )
    }
}
