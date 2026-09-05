package com.ilinetech.emergency.core.data

import android.content.Context
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.InstitutionEntity
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import com.ilinetech.emergency.core.data.entities.SubBranchEntity
import com.ilinetech.emergency.core.model.EstablishmentType
import com.ilinetech.emergency.core.model.FacilitySelection
import com.ilinetech.emergency.core.model.StaffDepartment
import com.ilinetech.emergency.core.model.StaffRole
import com.ilinetech.emergency.core.security.AppKeyProvider
import com.ilinetech.emergency.core.security.SerialEncoder
import com.ilinetech.emergency.fcm.FcmTopicManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Thin facade over Room + Prefs so Activities/Fragments don't reach into
 * DAOs directly. Not a full clean-architecture repository per entity —
 * deliberately one class for this scaffold's UI surface, given how small
 * it currently is; split it up if/when the UI grows past these four screens.
 */
class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    val prefs = Prefs(context)

    // --- Onboarding: hierarchy lookups ---

    fun observeInstitutions(wilayaCode: String, type: EstablishmentType): Flow<List<InstitutionEntity>> =
        db.institutionDao().observeByWilayaAndType(wilayaCode, type.name)

    fun observeSubBranches(institutionId: String): Flow<List<SubBranchEntity>> =
        db.subBranchDao().observeByInstitution(institutionId)

    // --- Onboarding: registration ---

    /**
     * Registers a staff profile: computes both serials via SerialEncoder,
     * persists the profile, marks onboarding complete, subscribes the FCM
     * topic, and adds this device's own number as an SMS fallback contact
     * for its own facility+department (see SMS_NOTES.md — this is the one
     * piece of the fallback contact directory that's wired up so far).
     */
    suspend fun registerStaffMember(
        selection: FacilitySelection,
        institutionIndex: Int,
        branchIndex: Int,
        role: StaffRole,
        fullName: String,
        groupId: String,
        ownPhoneNumber: String?
    ): StaffMemberEntity {
        val key = AppKeyProvider.getSerialEncryptionKey()
        val facilitySerial = SerialEncoder.encode(
            selection = selection,
            institutionIndex = institutionIndex,
            branchIndex = branchIndex,
            departmentOrdinal = 0, // facility serial doesn't encode department
            keyBytes = key
        )
        val deptSerial = SerialEncoder.encode(
            selection = selection,
            institutionIndex = institutionIndex,
            branchIndex = branchIndex,
            departmentOrdinal = role.ordinal,
            keyBytes = key
        )

        val profile = StaffMemberEntity(
            id = UUID.randomUUID().toString(),
            fullName = fullName,
            role = role.name,
            department = role.department.name,
            subBranchId = selection.subBranch.id,
            institutionId = selection.institution.id,
            institutionIndex = institutionIndex,
            branchIndex = branchIndex,
            groupId = groupId,
            facilitySerial = facilitySerial,
            deptSerial = deptSerial,
            fcmToken = null,
            registeredAtEpochMillis = System.currentTimeMillis()
        )

        db.staffMemberDao().insert(profile)
        prefs.activeStaffMemberId = profile.id
        prefs.onboardingComplete = true

        runCatching { FcmTopicManager.subscribe(profile) }

        if (!ownPhoneNumber.isNullOrBlank()) {
            db.smsFallbackContactDao().insert(
                com.ilinetech.emergency.core.data.entities.SmsFallbackContactEntity(
                    phoneNumber = ownPhoneNumber,
                    facilitySerial = facilitySerial,
                    deptSerial = deptSerial,
                    groupId = groupId,
                    label = fullName
                )
            )
        }

        return profile
    }

    suspend fun getActiveProfile(): StaffMemberEntity? {
        val id = prefs.activeStaffMemberId ?: return null
        return db.staffMemberDao().getById(id)
    }

    /**
     * Recomputes the deptSerial for a different role at the SAME facility
     * this profile belongs to — used by the Dashboard's "send to department X
     * at my own facility" flow. Does not require any additional lookups
     * beyond the profile itself since institutionIndex/branchIndex were
     * captured at registration time specifically to make this possible offline.
     */
    suspend fun computeSiblingDeptSerial(profile: StaffMemberEntity, targetRole: StaffRole): String {
        val institution = db.institutionDao().getById(profile.institutionId)
            ?: error("Institution not found for profile ${profile.id}")
        return SerialEncoder.encode(
            wilayaCode = institution.wilayaCode,
            type = EstablishmentType.valueOf(institution.establishmentType),
            institutionIndex = profile.institutionIndex,
            branchIndex = profile.branchIndex,
            departmentOrdinal = targetRole.ordinal,
            keyBytes = AppKeyProvider.getSerialEncryptionKey()
        )
    }

    // --- Logs ---

    fun observeAlertLogs(): Flow<List<AlertLogEntity>> = db.alertLogDao().observeAll()

    suspend fun acknowledge(logId: Long) =
        db.alertLogDao().markAcknowledged(logId, System.currentTimeMillis())

    // --- Settings ---

    suspend fun deregisterActiveProfile() {
        val profile = getActiveProfile() ?: return
        runCatching { FcmTopicManager.unsubscribe(profile) }
        db.staffMemberDao().deactivate(profile.id)
        prefs.clearSession()
    }
}
