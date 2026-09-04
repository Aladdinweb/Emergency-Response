package com.ilinetech.emergency.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilinetech.emergency.core.data.dao.AlertLogDao
import com.ilinetech.emergency.core.data.dao.InstitutionDao
import com.ilinetech.emergency.core.data.dao.SmsFallbackContactDao
import com.ilinetech.emergency.core.data.dao.StaffMemberDao
import com.ilinetech.emergency.core.data.dao.SubBranchDao
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.InstitutionEntity
import com.ilinetech.emergency.core.data.entities.SmsFallbackContactEntity
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import com.ilinetech.emergency.core.data.entities.SubBranchEntity
import com.ilinetech.emergency.core.model.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
@Database(
    entities = [
        InstitutionEntity::class,
        SubBranchEntity::class,
        StaffMemberEntity::class,
        AlertLogEntity::class,
        SmsFallbackContactEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun institutionDao(): InstitutionDao
    abstract fun subBranchDao(): SubBranchDao
    abstract fun staffMemberDao(): StaffMemberDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun smsFallbackContactDao(): SmsFallbackContactDao

    companion object {
        private const val DB_NAME = "emergency_response.db"

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addCallback(seedCallback)
                // TODO before any real release with installed users: replace this
                // with a proper Migration(1, 2) that preserves existing staff/alert
                // data. Destructive fallback is only acceptable while this schema
                // has never shipped to a device outside development.
                .fallbackToDestructiveMigration()
                .build()

        /**
         * Seeds the EPSP ES SENIA reference data on first creation so the
         * app is usable offline immediately after install, before any
         * remote config sync exists. Real deployments should replace/extend
         * this via a bundled JSON asset covering all wilayas.
         */
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val database = instance ?: return@launch
                    database.institutionDao().insertAll(listOf(toEntity(SeedData.epspEsSenia)))
                    database.subBranchDao().insertAll(
                        SeedData.epspEsSeniaBranches.map {
                            SubBranchEntity(id = it.id, name = it.name, institutionId = it.institutionId)
                        }
                    )
                }
            }
        }

        private fun toEntity(institution: com.ilinetech.emergency.core.model.Institution) =
            InstitutionEntity(
                id = institution.id,
                name = institution.name,
                establishmentType = institution.type.name,
                wilayaCode = institution.wilaya.code,
                wilayaName = institution.wilaya.name
            )
    }
}
