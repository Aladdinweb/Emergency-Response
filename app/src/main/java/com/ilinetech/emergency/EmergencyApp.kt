package com.ilinetech.emergency

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.ilinetech.emergency.core.data.Prefs
import com.ilinetech.emergency.service.ConnectionForegroundService

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
class EmergencyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = Prefs(this)

        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Only start the persistent status service once a profile is
        // registered — before that there's no facility/department context
        // for the service or anything else to report on.
        if (prefs.onboardingComplete) {
            ConnectionForegroundService.start(this)
        }
    }
}
