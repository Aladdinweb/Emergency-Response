package com.ilinetech.emergency.core.data

import android.content.Context
import android.content.SharedPreferences

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Thin typed wrapper over SharedPreferences for settings that don't belong
 * in Room (device-local UI/behavior toggles, not domain data):
 *  - which registered staff profile is "active" on this device
 *  - dark/light theme
 *  - foreground status-bar visibility toggle
 *  - alert sound/vibration preferences
 *
 * Keep domain data (institutions, staff, alerts) in Room — this class is
 * only for device-local settings, matching the split used in TASHIL-Hub.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var activeStaffMemberId: String?
        get() = sp.getString(KEY_ACTIVE_STAFF_ID, null)
        set(value) = sp.edit().putString(KEY_ACTIVE_STAFF_ID, value).apply()

    var isDarkTheme: Boolean
        get() = sp.getBoolean(KEY_DARK_THEME, true)
        set(value) = sp.edit().putBoolean(KEY_DARK_THEME, value).apply()

    /** Controls whether ConnectionForegroundService shows its persistent status notification. */
    var showStatusBarIndicator: Boolean
        get() = sp.getBoolean(KEY_SHOW_STATUS_BAR, true)
        set(value) = sp.edit().putBoolean(KEY_SHOW_STATUS_BAR, value).apply()

    var alertSoundEnabled: Boolean
        get() = sp.getBoolean(KEY_ALERT_SOUND, true)
        set(value) = sp.edit().putBoolean(KEY_ALERT_SOUND, value).apply()

    var alertVibrationEnabled: Boolean
        get() = sp.getBoolean(KEY_ALERT_VIBRATION, true)
        set(value) = sp.edit().putBoolean(KEY_ALERT_VIBRATION, value).apply()

    /** True once onboarding (facility/role selection) has completed at least once. */
    var onboardingComplete: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    fun clearSession() {
        sp.edit()
            .remove(KEY_ACTIVE_STAFF_ID)
            .remove(KEY_ONBOARDING_COMPLETE)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "ilinetech_emergency_prefs"
        private const val KEY_ACTIVE_STAFF_ID = "active_staff_id"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_SHOW_STATUS_BAR = "show_status_bar"
        private const val KEY_ALERT_SOUND = "alert_sound_enabled"
        private const val KEY_ALERT_VIBRATION = "alert_vibration_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
