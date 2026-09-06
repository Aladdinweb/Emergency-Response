package com.ilinetech.emergency.core.update

import android.content.Context
import com.ilinetech.emergency.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Reads a small JSON manifest hosted wherever the repo already lives — by
 * default the raw GitHub URL for `update.json` at the repo root, so there's
 * no separate server to run, matching the GitHub-centric workflow already
 * used for builds. Swap MANIFEST_URL if you'd rather host it elsewhere
 * (your own site, a GitHub Release asset, etc.).
 *
 * Expected JSON shape:
 * {
 *   "versionCode": 2,
 *   "versionName": "0.2.0",
 *   "apkUrl": "https://github.com/Aladdinweb/Emergency-Response/releases/download/v0.2.0/app-debug.apk",
 *   "notes": "Bug fixes and SMS formatting improvements"
 * }
 */
object UpdateChecker {

    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Aladdinweb/Emergency-Response/main/update.json"

    /** Returns UpdateInfo if a newer versionCode is available, null if up to date or the check failed. */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val info = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                notes = json.optString("notes", "")
            )

            if (info.versionCode > BuildConfig.VERSION_CODE) info else null
        }.getOrNull()
    }
}
