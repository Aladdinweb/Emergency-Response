package com.ilinetech.emergency.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffMemberDao {

    @Query("SELECT * FROM staff_members WHERE isActive = 1 ORDER BY registeredAtEpochMillis DESC")
    fun observeActiveProfiles(): Flow<List<StaffMemberEntity>>

    @Query("SELECT * FROM staff_members WHERE id = :id")
    suspend fun getById(id: String): StaffMemberEntity?

    /**
     * Used by SmsDataPayloadReceiver / EmergencyMessagingService to resolve an
     * inbound alert's dept/facility serials back to "is this alert addressed
     * to a profile registered on this device" without decrypting on the
     * receiving side — matching happens on the encrypted serial itself.
     */
    @Query(
        "SELECT * FROM staff_members WHERE isActive = 1 " +
        "AND facilitySerial = :facilitySerial AND deptSerial = :deptSerial"
    )
    suspend fun findMatchingProfiles(facilitySerial: String, deptSerial: String): List<StaffMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: StaffMemberEntity)

    @Update
    suspend fun update(profile: StaffMemberEntity)

    @Query("UPDATE staff_members SET fcmToken = :token WHERE id = :id")
    suspend fun updateFcmToken(id: String, token: String)

    @Query("UPDATE staff_members SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    /**
     * Counts active profiles registered ON THIS DEVICE's local database for
     * a given sub-branch. Honest limitation: StaffMemberEntity is a
     * per-device table with no cross-device sync — this is accurate for a
     * shared station where several staff register on the same physical
     * device (explicitly supported, see this entity's class doc), but does
     * NOT aggregate registrations made on separate phones. A true
     * facility-wide live count needs a shared directory (e.g. Firestore),
     * which doesn't exist yet — see STATE.md.
     */
    @Query("SELECT COUNT(*) FROM staff_members WHERE isActive = 1 AND subBranchId = :subBranchId")
    suspend fun countActiveAtBranch(subBranchId: String): Int

    /** Backs the interactive "Personnel en service" modal — same per-device scope caveat as countActiveAtBranch above. */
    @Query("SELECT * FROM staff_members WHERE isActive = 1 AND subBranchId = :subBranchId ORDER BY fullName")
    fun observeActiveAtBranch(subBranchId: String): Flow<List<StaffMemberEntity>>
}
