package com.ilinetech.emergency.core.model

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Full list of Algeria's 58 wilayas (post-2019 redistricting, codes 49-58
 * are the newer wilayas). Used to populate the first step of the onboarding
 * cascade regardless of which wilayas already have seeded institution data —
 * a device should be able to start registering a new wilaya's institution
 * even before any SeedData exists for it.
 */
object AlgerianWilayas {
    val all: List<Wilaya> = listOf(
        Wilaya("01", "Adrar"), Wilaya("02", "Chlef"), Wilaya("03", "Laghouat"),
        Wilaya("04", "Oum El Bouaghi"), Wilaya("05", "Batna"), Wilaya("06", "Béjaïa"),
        Wilaya("07", "Biskra"), Wilaya("08", "Béchar"), Wilaya("09", "Blida"),
        Wilaya("10", "Bouira"), Wilaya("11", "Tamanrasset"), Wilaya("12", "Tébessa"),
        Wilaya("13", "Tlemcen"), Wilaya("14", "Tiaret"), Wilaya("15", "Tizi Ouzou"),
        Wilaya("16", "Alger"), Wilaya("17", "Djelfa"), Wilaya("18", "Jijel"),
        Wilaya("19", "Sétif"), Wilaya("20", "Saïda"), Wilaya("21", "Skikda"),
        Wilaya("22", "Sidi Bel Abbès"), Wilaya("23", "Annaba"), Wilaya("24", "Guelma"),
        Wilaya("25", "Constantine"), Wilaya("26", "Médéa"), Wilaya("27", "Mostaganem"),
        Wilaya("28", "M'Sila"), Wilaya("29", "Mascara"), Wilaya("30", "Ouargla"),
        Wilaya("31", "Oran"), Wilaya("32", "El Bayadh"), Wilaya("33", "Illizi"),
        Wilaya("34", "Bordj Bou Arréridj"), Wilaya("35", "Boumerdès"), Wilaya("36", "El Tarf"),
        Wilaya("37", "Tindouf"), Wilaya("38", "Tissemsilt"), Wilaya("39", "El Oued"),
        Wilaya("40", "Khenchela"), Wilaya("41", "Souk Ahras"), Wilaya("42", "Tipaza"),
        Wilaya("43", "Mila"), Wilaya("44", "Aïn Defla"), Wilaya("45", "Naâma"),
        Wilaya("46", "Aïn Témouchent"), Wilaya("47", "Ghardaïa"), Wilaya("48", "Relizane"),
        Wilaya("49", "Timimoun"), Wilaya("50", "Bordj Badji Mokhtar"), Wilaya("51", "Ouled Djellal"),
        Wilaya("52", "Béni Abbès"), Wilaya("53", "In Salah"), Wilaya("54", "In Guezzam"),
        Wilaya("55", "Touggourt"), Wilaya("56", "Djanet"), Wilaya("57", "El M'Ghair"),
        Wilaya("58", "El Meniaa")
    )
}
