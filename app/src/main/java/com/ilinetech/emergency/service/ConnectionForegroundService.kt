package com.ilinetech.emergency.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ilinetech.emergency.core.data.Prefs

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Persistent monitor showing whether the device is on the online (FCM) path
 * or has fallen back to offline/GSM-SMS. Per spec:
 *   🟢 Online  - Connected to Emergency Network
 *   🟠 Offline - GSM SMS Fallback Active
 *
 * Platform constraint worth calling out explicitly: on Android 8+ a
 * foreground service MUST show a notification — there is no way to run one
 * invisibly. The Settings "show/hide status bar alert" toggle therefore
 * doesn't stop the notification from existing; it switches its importance
 * between DEFAULT (visible, makes a sound/appears in the status bar icon
 * area) and MIN (still present in the notification shade if pulled down,
 * but no status bar icon and silent) — see updateVisibilityFromPrefs().
 * This is the closest legal approximation of "hide" without dropping the
 * foreground guarantee that keeps connectivity monitoring alive.
 */
class ConnectionForegroundService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var prefs: Prefs
    private var isOnline = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isOnline = true
            updateNotification()
        }

        override fun onLost(network: Network) {
            isOnline = hasAnyValidatedNetwork()
            updateNotification()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        prefs = Prefs(this)
        createChannelIfNeeded()
        isOnline = hasAnyValidatedNetwork()
        startForeground(NOTIFICATION_ID, buildNotification())
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: this is a monitoring service, not tied to one bound
        // component's lifecycle — the OS should restart it if it's killed
        // under memory pressure, since "no connectivity indicator" during
        // an actual emergency is the failure mode we're guarding against.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Call after the user flips the Settings toggle to refresh the notification immediately. */
    fun updateVisibilityFromPrefs() = updateNotification()

    private fun hasAnyValidatedNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val (icon, title) = if (isOnline) {
            "🟢" to "Online - Connecté au réseau d'urgence"
        } else {
            "🟠" to "Offline - Repli SMS/GSM actif"
        }

        val visible = prefs.showStatusBarIndicator
        val importance = if (visible) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MIN

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online) // TODO: replace with app icon asset
            .setContentTitle("$icon $title")
            .setPriority(importance)
            .setOngoing(true)
            .setSilent(true) // this is a status indicator, not an alert — never make sound itself
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "Statut de connexion", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Indicateur permanent du mode Online/Offline"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "connection_status"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionForegroundService::class.java))
        }
    }
}
