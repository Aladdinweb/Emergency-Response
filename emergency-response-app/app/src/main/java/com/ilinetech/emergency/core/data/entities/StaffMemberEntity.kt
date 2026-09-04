package com.ilinetech.emergency.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * A single row represents the current device's registration — who's using
 * this phone, in which role, at which sub-branch, and the pre-computed
 * serials used to address them (so SmsSendReceiver/FcmTopicManager never
 * have to recompute them on the hot path).
 *
 * In practice there's normally exactly one row (the device owner), but this
 * is modeled as a table rather than a single-row prefs blob so a shared
 * station (e.g. a nursing post tablet) could support switching between a
 * few registered staff profiles without a schema change.
 */
@Entity(tableName = "staff_members")
data class StaffMemberEntity(
    @PrimaryKey val id: String,              // UUID generated at registration
    val fullName: String,
    val role: String,                        // StaffRole.name
    val department: String,                  // StaffDepartment.name
    val subBranchId: String,
    val institutionId: String,
    val groupId: String,                     // shift/group identifier
    val facilitySerial: String,              // SerialEncoder output (institution+branch)
    val deptSerial: String,                  // SerialEncoder output (department)
    val fcmToken: String?,                   // null until Firebase registration completes
    val registeredAtEpochMillis: Long,
    val isActive: Boolean = true             // false = deactivated without deleting history
)
