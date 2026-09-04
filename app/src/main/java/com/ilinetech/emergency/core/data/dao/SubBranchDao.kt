package com.ilinetech.emergency.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilinetech.emergency.core.data.entities.SubBranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubBranchDao {

    @Query("SELECT * FROM sub_branches WHERE institutionId = :institutionId ORDER BY name")
    fun observeByInstitution(institutionId: String): Flow<List<SubBranchEntity>>

    @Query("SELECT * FROM sub_branches WHERE id = :id")
    suspend fun getById(id: String): SubBranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(branches: List<SubBranchEntity>)
}
