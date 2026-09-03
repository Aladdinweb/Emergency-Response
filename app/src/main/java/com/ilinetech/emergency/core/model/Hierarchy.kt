package com.ilinetech.emergency.core.model

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Pure data model for the cascading facility hierarchy:
 *   Wilaya -> EstablishmentType -> Institution -> SubBranch
 *
 * No Android dependencies here on purpose — keeps this layer unit-testable
 * and reusable outside the app module.
 */

/** One of Algeria's 58 wilayas. Code is the official 2-digit wilaya code (e.g. "31" = Oran). */
data class Wilaya(
    val code: String,
    val name: String
)

/** The four supported healthcare establishment categories. */
enum class EstablishmentType(val label: String) {
    CHU("Centre Hospitalier Universitaire"),
    EHU("Établissement Hospitalier Universitaire"),
    EPH("Établissement Public Hospitalier"),
    EPSP("Établissement Public de Santé de Proximité")
}

/** Top-level registered institution, e.g. "EPSP ES SENIA". */
data class Institution(
    val id: String,               // stable internal id, e.g. "epsp-es-senia"
    val name: String,
    val type: EstablishmentType,
    val wilaya: Wilaya
)

/**
 * A sub-branch / polyclinique under an institution. For CHU/EHU/EPH this may be
 * the institution itself (single-branch); EPSP typically has multiple.
 */
data class SubBranch(
    val id: String,                // stable internal id, e.g. "polyclinique-kerma"
    val name: String,
    val institutionId: String
)

/** Fully-resolved selection produced by the onboarding cascade UI. */
data class FacilitySelection(
    val wilaya: Wilaya,
    val type: EstablishmentType,
    val institution: Institution,
    val subBranch: SubBranch
)

/**
 * Example seed data for EPSP ES SENIA, matching the spec.
 * Real data should live in a Room-seeded table or a bundled JSON asset —
 * this object is a reference / fallback only.
 */
object SeedData {
    val oran = Wilaya(code = "31", name = "Oran")

    val epspEsSenia = Institution(
        id = "epsp-es-senia",
        name = "EPSP ES SENIA",
        type = EstablishmentType.EPSP,
        wilaya = oran
    )

    val epspEsSeniaBranches = listOf(
        SubBranch("polyclinique-es-senia", "Polyclinique Es Senia", epspEsSenia.id),
        SubBranch("polyclinique-ain-beida-1", "Polyclinique Ain Beida 1", epspEsSenia.id),
        SubBranch("polyclinique-ain-beida-2", "Polyclinique Ain Beida 2", epspEsSenia.id),
        SubBranch("polyclinique-aadl-ain-beida-mabrouk-loucif", "Polyclinique Aadl Ain Beida Mabrouk Loucif", epspEsSenia.id),
        SubBranch("polyclinique-kerma", "Polyclinique Kerma", epspEsSenia.id),
        SubBranch("polyclinique-sidi-maarouf", "Polyclinique Sidi Maarouf", epspEsSenia.id),
        SubBranch("polyclinique-saniat-es-sahm", "Polyclinique Saniat Es Sahm", epspEsSenia.id)
    )
}
