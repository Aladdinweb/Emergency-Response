package com.ilinetech.emergency.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Centralizes the runtime permissions this app actually needs and why:
 *   - POST_NOTIFICATIONS (Android 13+): without it, neither the alert
 *     notification nor the foreground status service notification shows.
 *   - SEND_SMS / RECEIVE_SMS: the entire offline fallback path is inert
 *     without both — SEND for SmsDispatcher, RECEIVE for SmsDataPayloadReceiver.
 *
 * Call requestAll() once, early (e.g. MainActivity.onCreate), rather than
 * requesting piecemeal when each feature first needs its permission — for
 * an emergency app, "ask everything up front so it's ready when needed"
 * beats "ask mid-emergency when the user is trying to send an alert."
 */
class PermissionsHelper(private val activity: AppCompatActivity) {

    private val requiredPermissions: Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
    }.toTypedArray()

    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    private var onResult: ((allGranted: Boolean) -> Unit)? = null

    /** Must be called before the Activity reaches STARTED (e.g. in onCreate, before super calls resume). */
    fun register() {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            onResult?.invoke(results.values.all { it })
        }
    }

    fun allGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    fun requestAll(onResult: (allGranted: Boolean) -> Unit) {
        this.onResult = onResult
        if (allGranted()) {
            onResult(true)
        } else {
            launcher.launch(requiredPermissions)
        }
    }
}
