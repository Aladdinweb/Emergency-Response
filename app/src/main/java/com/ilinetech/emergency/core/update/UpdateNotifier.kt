package com.ilinetech.emergency.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ilinetech.emergency.R
import com.ilinetech.emergency.ui.main.MainActivity

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
object UpdateNotifier {

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 2001

    fun notify(context: Context, info: UpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, context.getString(R.string.update_channel_name), NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        // Tapping the notification opens the app; the actual download is
        // triggered from Settings rather than directly from the notification
        // action, since REQUEST_INSTALL_PACKAGES-driven installs read more
        // clearly as something the user chose inside the app.
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText(context.getString(R.string.update_available_message, info.versionName, com.ilinetech.emergency.BuildConfig.VERSION_NAME))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
