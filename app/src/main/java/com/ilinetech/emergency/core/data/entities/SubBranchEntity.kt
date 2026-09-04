package com.ilinetech.emergency.core.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Sub-branch / polyclinique under an institution (e.g. the 7 branches of
 * EPSP ES SENIA). For single-branch establishments (typical CHU/EHU/EPH),
 * a single SubBranchEntity mirroring the parent institution is created.
 */
@Entity(
    tableName = "sub_branches",
    foreignKeys = [
        ForeignKey(
            entity = InstitutionEntity::class,
            parentColumns = ["id"],
            childColumns = ["institutionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("institutionId")]
)
data class SubBranchEntity(
    @PrimaryKey val id: String,          // e.g. "polyclinique-kerma"
    val name: String,                    // e.g. "Polyclinique Kerma"
    val institutionId: String
)
