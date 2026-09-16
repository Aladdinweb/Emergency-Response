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
 * Reference seed data for Oran wilaya's EPSP network — the real directory as
 * supplied for this project, not a placeholder. Real data for other wilayas
 * should be added the same way (a new Institution + its SubBranch list) or
 * migrated to a bundled JSON asset / remote sync once the directory grows
 * beyond what's comfortable to hand-maintain in Kotlin.
 */
object SeedData {
    val oran = Wilaya(code = "31", name = "Oran")

    private fun epsp(id: String, name: String) =
        Institution(id = id, name = name, type = EstablishmentType.EPSP, wilaya = oran)

    val epspEsSenia = epsp("epsp-es-senia", "EPSP ES SENIA")
    private val epspHaiBouamama = epsp("epsp-hai-bouamama", "EPSP HAI BOUAMAMA")
    private val epspFrontDeMer = epsp("epsp-front-de-mer", "EPSP FRONT DE MER")
    private val epspGhoualem = epsp("epsp-ghoualem", "EPSP GHOUALEM")
    private val epspEsSeddikia = epsp("epsp-es-seddikia", "EPSP ES SÉDDIKIA")
    private val epspOuedTlelat = epsp("epsp-oued-tlelat", "EPSP OUED TLÉLAT")
    private val epspBoutlelis = epsp("epsp-boutlelis", "EPSP BOUTLELIS")
    private val epspAinElTurck = epsp("epsp-ain-el-turck", "EPSP AIN EL TURCK")
    private val epspArzew = epsp("epsp-arzew", "EPSP ARZEW")

    /** All Oran EPSP institutions — insert these into InstitutionEntity on first launch. */
    val oranInstitutions: List<Institution> = listOf(
        epspEsSenia, epspHaiBouamama, epspFrontDeMer, epspGhoualem, epspEsSeddikia,
        epspOuedTlelat, epspBoutlelis, epspAinElTurck, epspArzew
    )

    private fun branch(id: String, name: String, institution: Institution) =
        SubBranch(id = id, name = name, institutionId = institution.id)

    /** All sub-branches across every Oran EPSP — insert these into SubBranchEntity on first launch. */
    val oranBranches: List<SubBranch> = listOf(
        // EPSP ES SENIA
        branch("branch-es-senia", "Es Senia", epspEsSenia),
        branch("branch-ain-el-beida-1", "Ain El Beida I", epspEsSenia),
        branch("branch-ain-el-beida-2", "Ain El Beida II", epspEsSenia),
        branch("branch-el-kerma", "El Kerma", epspEsSenia),
        branch("branch-sidi-maarouf", "Sidi Maarouf", epspEsSenia),
        branch("branch-sidi-chami", "Sidi Chami", epspEsSenia),

        // EPSP HAI BOUAMAMA
        branch("branch-hai-bouamama", "Hai Bouamama", epspHaiBouamama),
        branch("branch-emir-khaled", "Emir Khaled", epspHaiBouamama),

        // EPSP FRONT DE MER
        branch("branch-front-de-mer", "Front de Mer", epspFrontDeMer),

        // EPSP GHOUALEM
        branch("branch-hai-el-ghoualem", "Hai El Ghoualem", epspGhoualem),
        branch("branch-hai-el-hemri", "Hai El Hemri", epspGhoualem),

        // EPSP ES SÉDDIKIA
        branch("branch-seddikia", "Seddikia", epspEsSeddikia),
        branch("branch-chouhada", "Chouhada", epspEsSeddikia),
        branch("branch-felloucene-2", "Felloucene II", epspEsSeddikia),
        branch("branch-bir-el-djir", "Bir El Djir", epspEsSeddikia),

        // EPSP OUED TLÉLAT
        branch("branch-oued-tlelat", "Oued Tlelat", epspOuedTlelat),

        // EPSP BOUTLELIS
        branch("branch-boutlelis", "Boutlelis", epspBoutlelis),
        branch("branch-misserghine", "Misserghine", epspBoutlelis),
        branch("branch-ain-el-karma", "Ain El Karma", epspBoutlelis),

        // EPSP AIN EL TURCK
        branch("branch-tabek-boucif", "Tabek Boucif", epspAinElTurck),
        branch("branch-mers-el-kebir", "Mers El Kebir", epspAinElTurck),
        branch("branch-el-ancor", "El Ancor", epspAinElTurck),

        // EPSP ARZEW
        branch("branch-arzew", "Arzew", epspArzew),
        branch("branch-bethioua", "Bethioua", epspArzew),
        branch("branch-ain-el-bia", "Ain El Bia", epspArzew),
        branch("branch-mers-el-hadjadj", "Mers El Hadjadj", epspArzew),
        branch("branch-gdyel", "Gdyel", epspArzew),
        branch("branch-hassi-bounif", "Hassi Bounif", epspArzew),
        branch("branch-hassi-benokba", "Hassi Benokba", epspArzew)
    )

    // Kept for backward compatibility with any code still referencing the
    // old single-institution seed shape.
    val epspEsSeniaBranches: List<SubBranch> get() = oranBranches.filter { it.institutionId == epspEsSenia.id }
}
