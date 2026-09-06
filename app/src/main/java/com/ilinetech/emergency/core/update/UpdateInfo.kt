package com.ilinetech.emergency.core.update

/**
 * © ILINE TECH BY FERAK ALADDIN
 * Parsed from the remote update manifest JSON — see UpdateChecker.
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)
