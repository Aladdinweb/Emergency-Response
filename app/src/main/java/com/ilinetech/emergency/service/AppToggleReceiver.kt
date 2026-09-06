package com.ilinetech.emergency.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ilinetech.emergency.core.data.Prefs

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Flips the master enabled/disabled switch (Prefs.appEnabled) from outside
 * the app UI — currently only reachable via the connection-status
 * notification's quick action. See ConnectionForegroundService's doc
 * comment for why this can only go active→inactive from the notification,
 * not the reverse (there's nothing to tap once the notification is gone).
 */
class AppToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_APP_ENABLED) return

        val prefs = Prefs(context)
        prefs.appEnabled = !prefs.appEnabled

        if (prefs.appEnabled) {
            ConnectionForegroundService.start(context)
        } else {
            ConnectionForegroundService.stop(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_APP_ENABLED = "com.ilinetech.emergency.action.TOGGLE_APP_ENABLED"
    }
}
