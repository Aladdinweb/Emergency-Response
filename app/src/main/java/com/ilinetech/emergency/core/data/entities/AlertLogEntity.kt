package com.ilinetech.emergency.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertDirection { OUTGOING, INCOMING }
enum class AlertTransport { FCM, SMS }

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Unified log for every alert this device has sent or received, regardless
 * of which transport carried it. Powers the Logs screen and lets the app
 * detect duplicate delivery (e.g. an SMS fallback arriving after FCM already
 * delivered the same alert) by matching on (facilitySerial, deptSerial,
 * groupId, message, sentAtEpochMillis-within-a-window).
 */
@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: AlertDirection,
    val transport: AlertTransport,
    val facilitySerial: String,
    val deptSerial: String,
    val groupId: String,
    val priorityLevel: Int,              // TriageLevel.smsCode
    val reason: String,                  // IncidentReason.name
    val message: String,
    val counterpartLabel: String,        // human-readable sender/recipient for the UI
    val sentAtEpochMillis: Long,
    val acknowledged: Boolean = false,
    val acknowledgedAtEpochMillis: Long? = null
)
