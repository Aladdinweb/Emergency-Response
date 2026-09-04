package com.ilinetech.emergency.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {

    @Query("SELECT * FROM alert_logs ORDER BY sentAtEpochMillis DESC")
    fun observeAll(): Flow<List<AlertLogEntity>>

    @Query("SELECT * FROM alert_logs WHERE direction = 'INCOMING' AND acknowledged = 0 ORDER BY sentAtEpochMillis DESC")
    fun observeUnacknowledgedIncoming(): Flow<List<AlertLogEntity>>

    @Insert
    suspend fun insert(log: AlertLogEntity): Long

    @Query("UPDATE alert_logs SET acknowledged = 1, acknowledgedAtEpochMillis = :atEpochMillis WHERE id = :id")
    suspend fun markAcknowledged(id: Long, atEpochMillis: Long)

    /**
     * De-duplication check for the SMS fallback: has an equivalent alert
     * already been logged (e.g. delivered via FCM) within the given window?
     */
    @Query(
        "SELECT COUNT(*) FROM alert_logs WHERE facilitySerial = :facilitySerial " +
        "AND deptSerial = :deptSerial AND groupId = :groupId AND message = :message " +
        "AND sentAtEpochMillis BETWEEN :windowStart AND :windowEnd"
    )
    suspend fun countRecentDuplicates(
        facilitySerial: String,
        deptSerial: String,
        groupId: String,
        message: String,
        windowStart: Long,
        windowEnd: Long
    ): Int
}
