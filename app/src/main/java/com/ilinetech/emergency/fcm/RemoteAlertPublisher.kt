package com.ilinetech.emergency.fcm

import com.ilinetech.emergency.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Calls a Cloud Function that publishes to the FCM topic on our behalf —
 * see /functions/index.js for the deployable server code and
 * CLOUD_FUNCTION_NOTES.md for deployment steps. The client SDK still
 * cannot publish to FCM topics directly (a genuine platform limitation,
 * not something this app chooses); this is the trusted-server side of
 * that requirement.
 *
 * Reads the endpoint URL and shared secret from BuildConfig, populated
 * from Gradle properties the same way SERIAL_KEY_HEX is — see build.gradle.kts.
 * Until CLOUD_FUNCTION_URL is configured, [publish] returns NotConfigured
 * rather than silently failing or throwing, so the Dashboard's send flow
 * degrades cleanly to SMS-only.
 */
object RemoteAlertPublisher {

    sealed class PublishResult {
        object NotConfigured : PublishResult()
        data class Success(val messageId: String) : PublishResult()
        data class Failure(val reason: String) : PublishResult()
    }

    suspend fun publish(
        facilitySerial: String,
        deptSerial: String,
        groupId: String,
        priorityCode: Int,
        reason: String,
        message: String,
        senderLabel: String
    ): PublishResult = withContext(Dispatchers.IO) {
        val url = BuildConfig.CLOUD_FUNCTION_URL
        if (url.isBlank()) return@withContext PublishResult.NotConfigured

        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (BuildConfig.CLOUD_FUNCTION_SHARED_SECRET.isNotBlank()) {
                connection.setRequestProperty("X-Shared-Secret", BuildConfig.CLOUD_FUNCTION_SHARED_SECRET)
            }

            val body = JSONObject().apply {
                put("facilitySerial", facilitySerial)
                put("deptSerial", deptSerial)
                put("groupId", groupId)
                put("priority", priorityCode)
                put("reason", reason)
                put("message", message)
                put("senderLabel", senderLabel)
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            connection.disconnect()

            if (responseCode in 200..299) {
                val json = runCatching { JSONObject(responseText) }.getOrNull()
                PublishResult.Success(json?.optString("messageId", "") ?: "")
            } else {
                PublishResult.Failure("HTTP $responseCode: $responseText")
            }
        }.getOrElse { PublishResult.Failure(it.message ?: "unknown error") }
    }
}
