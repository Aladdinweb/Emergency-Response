package com.ilinetech.emergency

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.ilinetech.emergency.core.data.Prefs
import com.ilinetech.emergency.core.update.UpdateChecker
import com.ilinetech.emergency.core.update.UpdateNotifier
import com.ilinetech.emergency.service.ConnectionForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
class EmergencyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val prefs = Prefs(this)

        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Only start the persistent status service once a profile is
        // registered AND the master switch (Settings) is on — see
        // Prefs.appEnabled for why this exists.
        if (prefs.onboardingComplete && prefs.appEnabled) {
            ConnectionForegroundService.start(this)
        }

        checkForUpdateIfDue(prefs)
    }

    /** Throttled to once per 24h so app launches don't hammer the manifest URL. */
    private fun checkForUpdateIfDue(prefs: Prefs) {
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        if (System.currentTimeMillis() - prefs.lastUpdateCheckEpochMillis < dayMillis) return

        appScope.launch {
            prefs.lastUpdateCheckEpochMillis = System.currentTimeMillis()
            UpdateChecker.checkForUpdate(applicationContext)?.let { info ->
                UpdateNotifier.notify(applicationContext, info)
            }
        }
    }
}
