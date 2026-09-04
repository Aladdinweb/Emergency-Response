package com.ilinetech.emergency.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Local cache of institutions available for the onboarding cascade.
 * Seeded on first launch (see AppDatabase.Callback) and optionally refreshed
 * from a remote config later — the app must work fully offline, so this
 * table is the source of truth, not a cache of a network call made at
 * registration time.
 */
@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: String,          // e.g. "epsp-es-senia"
    val name: String,                    // e.g. "EPSP ES SENIA"
    val establishmentType: String,       // EstablishmentType.name, e.g. "EPSP"
    val wilayaCode: String,              // e.g. "31"
    val wilayaName: String               // e.g. "Oran"
)
