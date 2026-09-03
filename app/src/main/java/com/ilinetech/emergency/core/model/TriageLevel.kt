package com.ilinetech.emergency.core.model

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Severity triage used to color-code alerts and decide alarm behavior
 * (e.g. only CRITICAL bypasses DND at full volume).
 */
enum class TriageLevel(
    val label: String,
    val colorHex: String,
    val smsCode: Int
) {
    LOW("Non-urgent / Routine", "#2ECC71", 1),      // Green
    MODERATE("Urgent, non vital", "#E67E22", 2),    // Orange
    CRITICAL("Urgence vitale", "#E74C3C", 3);        // Red

    companion object {
        fun fromSmsCode(code: Int): TriageLevel =
            entries.firstOrNull { it.smsCode == code }
                ?: throw IllegalArgumentException("Unknown triage sms code: $code")
    }
}
