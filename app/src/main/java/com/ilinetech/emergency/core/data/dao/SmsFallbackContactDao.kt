package com.ilinetech.emergency.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilinetech.emergency.core.data.entities.SmsFallbackContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsFallbackContactDao {

    @Query("SELECT * FROM sms_fallback_contacts WHERE facilitySerial = :facilitySerial AND deptSerial = :deptSerial")
    suspend fun getForTarget(facilitySerial: String, deptSerial: String): List<SmsFallbackContactEntity>

    @Query("SELECT * FROM sms_fallback_contacts ORDER BY label")
    fun observeAll(): Flow<List<SmsFallbackContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: SmsFallbackContactEntity): Long

    @Delete
    suspend fun delete(contact: SmsFallbackContactEntity)
}
