package com.ilinetech.emergency.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.Prefs
import com.ilinetech.emergency.databinding.ActivityMainBinding
import com.ilinetech.emergency.service.ConnectionForegroundService
import com.ilinetech.emergency.ui.dashboard.DashboardFragment
import com.ilinetech.emergency.ui.logs.LogsFragment
import com.ilinetech.emergency.ui.onboarding.OnboardingActivity
import com.ilinetech.emergency.ui.settings.SettingsFragment
import com.ilinetech.emergency.util.PermissionsHelper

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Entry point after onboarding. Redirects back to OnboardingActivity if no
 * profile is registered yet (e.g. app data was cleared, or this is a fresh
 * install reaching MainActivity directly for some reason). Requests all
 * runtime permissions up front — see PermissionsHelper for why.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionsHelper: PermissionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs(applicationContext)
        if (!prefs.onboardingComplete) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionsHelper = PermissionsHelper(this)
        permissionsHelper.register()
        permissionsHelper.requestAll { /* handled per-feature; SMS send/receive and notifications
            will simply no-op or fail silently until granted — each entry point
            (SmsDispatcher, notification posting) is independent of this callback. */ }

        ConnectionForegroundService.start(this)

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { showFragment(DashboardFragment()); true }
                R.id.nav_logs -> { showFragment(LogsFragment()); true }
                R.id.nav_settings -> { showFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
