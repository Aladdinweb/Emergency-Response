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
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.Prefs

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Persistent monitor showing whether the device is on the online (FCM) path
 * or has fallen back to offline/GSM-SMS. Per spec:
 *   🟢 Online  - Connected to Emergency Network
 *   🟠 Offline - GSM SMS Fallback Active
 *
 * BUG FIX (was reported after device testing): Settings' "show/hide status
 * bar indicator" toggle previously only changed the notification's priority
 * (DEFAULT vs MIN) while the service kept running — MIN-priority
 * notifications still appear in the shade, so "hide" never actually hid
 * anything. Since a foreground service on Android 8+ cannot exist without
 * SOME visible notification (platform rule, not something this app can
 * override), the only correct way to "hide the indicator" is to stop being
 * a foreground service at all. That means: when the toggle is off, this
 * service does not run, and connectivity is simply not monitored — that
 * trade-off is now explicit rather than silently fake-succeeding.
 *
 * Also carries a quick "Désactiver" action (see AppToggleReceiver) so the
 * whole app can be paused without opening it — this action only appears
 * while the notification is visible, i.e. while the app is active; once
 * paused there is deliberately no notification left to re-activate FROM
 * (that would defeat "hide the indicator" above) — re-enabling requires
 * opening the app (Dashboard header switch or Settings).
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
        prefs = Prefs(this)

        // Guard here too, not just at call sites: anything that calls
        // start() (EmergencyApp on launch, Settings, the toggle receiver)
        // should never leave a notification showing when the preference
        // says not to — checking here as well as at call sites means one
        // missed call site can't reintroduce the original bug.
        if (!prefs.showStatusBarIndicator || !prefs.appEnabled) {
            stopSelf()
            return
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createChannelIfNeeded()
        isOnline = hasAnyValidatedNetwork()
        startForeground(NOTIFICATION_ID, buildNotification())
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityManager.isInitialized) {
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            "\uD83D\uDFE2" to "Online - Connecté au réseau d'urgence"
        } else {
            "\uD83D\uDFE0" to "Offline - Repli SMS/GSM actif"
        }

        val toggleIntent = Intent(this, AppToggleReceiver::class.java).apply {
            action = AppToggleReceiver.ACTION_TOGGLE_APP_ENABLED
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_online) // TODO: replace with app icon asset
            .setContentTitle("$icon $title")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setSilent(true) // this is a status indicator, not an alert — never make sound itself
            .addAction(0, getString(R.string.action_deactivate_quick), togglePendingIntent)
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
            val prefs = Prefs(context)
            if (!prefs.showStatusBarIndicator || !prefs.appEnabled) return // see onCreate's guard for why
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
