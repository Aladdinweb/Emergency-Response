package com.ilinetech.emergency.core.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * FCM has topics to fan alerts out to every subscribed device automatically.
 * GSM/SMS has no equivalent directory service — the sending device must
 * know the actual phone numbers to dial. This table is that local address
 * book: every phone opted in to receive SMS-fallback alerts for a given
 * (facilitySerial, deptSerial) pair.
 *
 * Populated during onboarding when a staff member registers (their own
 * number is added for their own facility+department), and kept in sync via
 * a periodic directory sync (not yet built) so devices don't need to have
 * personally met to fall back to SMS for each other.
 */
@Entity(
    tableName = "sms_fallback_contacts",
    indices = [Index(value = ["facilitySerial", "deptSerial"])]
)
data class SmsFallbackContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,       // E.164 format, e.g. "+2135XXXXXXXX"
    val facilitySerial: String,
    val deptSerial: String,
    val groupId: String,
    val label: String              // display name for logs/UI, e.g. "Poste sécurité - Nuit"
)
