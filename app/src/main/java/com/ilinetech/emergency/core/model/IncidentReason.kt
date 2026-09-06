package com.ilinetech.emergency.core.model

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Structured incident context, distinct from TriageLevel (severity) —
 * a security alert and a medical alert can both be CRITICAL, but "there's
 * a fight" and "cardiac arrest" call for completely different responses.
 * Sent as a short ordinal-based code in the SMS data payload (see
 * SmsPayloadBuilder) so it stays compact, and as a named field in the FCM
 * data payload.
 */
enum class IncidentReason(val label: String) {
    BAGARRE_ALTERCATION("Bagarre / Altercation"),
    RENFORT_ASSISTANCE("Renfort / Assistance nécessaire"),
    URGENCE_MEDICALE("Urgence médicale"),
    AGRESSION("Agression"),
    AUTRE("Autre")
}
