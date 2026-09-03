package com.ilinetech.emergency.core.sms

import com.ilinetech.emergency.core.model.TriageLevel

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Format (matches the online/FCM path 1:1 so both channels carry the same
 * information and the receiver UI doesn't need to branch on transport):
 *
 *   URGENCY#ENCRYPTED_FACILITY_SERIAL#DEPT_SERIAL#GROUP_ID#PRIORITY_LEVEL#MESSAGE
 *
 * - URGENCY               literal tag, currently always "URG" — reserved so
 *                          future non-emergency SMS types can share the
 *                          receiver's parsing path without ambiguity.
 * - ENCRYPTED_FACILITY_SERIAL  from SerialEncoder — identifies wilaya/type/
 *                          institution/sub-branch.
 * - DEPT_SERIAL            from SerialEncoder — identifies the target
 *                          role/department within that sub-branch.
 * - GROUP_ID               shift/group identifier (plain string, no PII).
 * - PRIORITY_LEVEL         TriageLevel.smsCode (1/2/3).
 * - MESSAGE                free text; '#' is stripped/escaped since it's the
 *                          field delimiter.
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

    fun build(payload: AlertPayload): String {
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

    /** Returns null if the SMS body doesn't match the expected payload shape. */
    fun parse(smsBody: String): AlertPayload? {
        val parts = smsBody.split(DELIMITER, limit = FIELD_COUNT)
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
