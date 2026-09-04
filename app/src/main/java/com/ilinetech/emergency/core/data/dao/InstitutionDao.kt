package com.ilinetech.emergency.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilinetech.emergency.core.data.entities.InstitutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstitutionDao {

    @Query("SELECT * FROM institutions WHERE wilayaCode = :wilayaCode AND establishmentType = :type ORDER BY name")
    fun observeByWilayaAndType(wilayaCode: String, type: String): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions WHERE id = :id")
    suspend fun getById(id: String): InstitutionEntity?

    @Query("SELECT DISTINCT wilayaCode, wilayaName FROM institutions ORDER BY wilayaName")
    fun observeWilayasWithData(): Flow<List<WilayaRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(institutions: List<InstitutionEntity>)
}

/** Lightweight projection for populating the wilaya picker without loading full rows. */
data class WilayaRow(val wilayaCode: String, val wilayaName: String)
