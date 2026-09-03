package com.ilinetech.emergency.core.model

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * All staff roles supported for registration and targeted alert routing,
 * grouped by department so the UI can render them under section headers.
 */
enum class StaffDepartment(val label: String) {
    SECURITY("Sécurité"),
    MEDICAL("Médical"),
    NURSING_SUPPORT("Paramédical / Soutien"),
    TRANSPORT("Transport"),
    DIAGNOSTICS("Diagnostics"),
    TECHNICAL_SERVICES("Technique / Services")
}

enum class StaffRole(val label: String, val department: StaffDepartment) {
    // Security
    AGENT_DE_SECURITE("Agent de sécurité", StaffDepartment.SECURITY),
    CHEF_DE_POSTE("Chef de poste", StaffDepartment.SECURITY),

    // Medical
    MEDECIN_URGENTISTE("Médecin Urgentiste", StaffDepartment.MEDICAL),
    MEDECIN_SPECIALISTE("Médecin Spécialiste", StaffDepartment.MEDICAL),
    REANIMATEUR("Réanimateur", StaffDepartment.MEDICAL),

    // Nursing / Support
    INFIRMIER("Infirmier", StaffDepartment.NURSING_SUPPORT),
    AIDE_SOIGNANT("Aide-soignant", StaffDepartment.NURSING_SUPPORT),
    SAGE_FEMME("Sage-femme", StaffDepartment.NURSING_SUPPORT),

    // Transport
    AMBULANCIER("Ambulancier", StaffDepartment.TRANSPORT),
    BRANCARDIER("Brancardier", StaffDepartment.TRANSPORT),

    // Diagnostics
    RADIO("Radio", StaffDepartment.DIAGNOSTICS),
    LABORATOIRE("Laboratoire", StaffDepartment.DIAGNOSTICS),
    PHARMACIEN("Pharmacien", StaffDepartment.DIAGNOSTICS),

    // Technical / Services
    MAINTENANCE("Maintenance", StaffDepartment.TECHNICAL_SERVICES),
    HYGIENE("Hygiène", StaffDepartment.TECHNICAL_SERVICES);

    companion object {
        fun byDepartment(): Map<StaffDepartment, List<StaffRole>> =
            entries.groupBy { it.department }
    }
}
