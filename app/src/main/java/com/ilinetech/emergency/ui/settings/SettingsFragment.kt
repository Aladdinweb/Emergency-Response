package com.ilinetech.emergency.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.databinding.FragmentSettingsBinding
import com.ilinetech.emergency.service.ConnectionForegroundService
import com.ilinetech.emergency.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)
        val prefs = repository.prefs

        binding.switchDarkTheme.isChecked = prefs.isDarkTheme
        binding.switchShowStatusBar.isChecked = prefs.showStatusBarIndicator
        binding.switchAlertSound.isChecked = prefs.alertSoundEnabled
        binding.switchAlertVibration.isChecked = prefs.alertVibrationEnabled

        binding.switchDarkTheme.setOnCheckedChangeListener { _, checked ->
            prefs.isDarkTheme = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchShowStatusBar.setOnCheckedChangeListener { _, checked ->
            prefs.showStatusBarIndicator = checked
            // BUG FIX: this used to unconditionally stop() then start() the
            // service, which restarts it (and its notification) regardless
            // of what was just chosen — that's why "off" never actually hid
            // anything. Only restart when turning ON; turning OFF must stop
            // and NOT restart, since a foreground service cannot exist
            // without showing a notification (Android 8+ platform rule) —
            // "hide the indicator" can only mean "the service isn't running."
            ConnectionForegroundService.stop(requireContext())
            if (checked && prefs.appEnabled) {
                ConnectionForegroundService.start(requireContext())
            }
        }

        binding.switchAlertSound.setOnCheckedChangeListener { _, checked ->
            prefs.alertSoundEnabled = checked
        }

        binding.switchAlertVibration.setOnCheckedChangeListener { _, checked ->
            prefs.alertVibrationEnabled = checked
        }

        binding.buttonDeregister.setOnClickListener { confirmDeregister() }
        binding.buttonCheckUpdates.setOnClickListener { onCheckUpdatesClicked() }
        binding.textFooter.text = getString(
            R.string.footer_combined,
            getString(R.string.footer_copyright),
            getString(R.string.footer_version, com.ilinetech.emergency.BuildConfig.VERSION_NAME)
        )

        loadActiveProfileLabel()
    }

    private fun onCheckUpdatesClicked() {
        binding.textUpdateStatus.text = getString(R.string.update_checking)
        binding.buttonCheckUpdates.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val info = com.ilinetech.emergency.core.update.UpdateChecker.checkForUpdate(requireContext().applicationContext)
            binding.buttonCheckUpdates.isEnabled = true
            if (info == null) {
                binding.textUpdateStatus.text = getString(R.string.update_up_to_date)
                return@launch
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.update_available_title)
                .setMessage(
                    getString(
                        R.string.update_available_message,
                        info.versionName,
                        com.ilinetech.emergency.BuildConfig.VERSION_NAME
                    ) + if (info.notes.isNotBlank()) "\n\n${info.notes}" else ""
                )
                .setNegativeButton(R.string.action_later, null)
                .setPositiveButton(R.string.action_download_update) { _, _ ->
                    binding.textUpdateStatus.text = getString(R.string.update_downloading)
                    com.ilinetech.emergency.core.update.UpdateDownloader.startDownload(requireContext().applicationContext, info)
                }
                .show()
        }
    }

    private fun loadActiveProfileLabel() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = repository.getActiveProfile()
            binding.textActiveProfile.text = profile?.let { "${it.fullName} · ${it.role}" }
                ?: getString(R.string.settings_no_profile)
        }
    }

    private fun confirmDeregister() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deregister_confirm_title)
            .setMessage(R.string.deregister_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ -> deregister() }
            .show()
    }

    private fun deregister() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.deregisterActiveProfile()
            ConnectionForegroundService.stop(requireContext())
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
