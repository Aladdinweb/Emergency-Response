package com.ilinetech.emergency.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ilinetech.emergency.BuildConfig
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import com.ilinetech.emergency.core.model.IncidentReason
import com.ilinetech.emergency.core.model.StaffRole
import com.ilinetech.emergency.core.model.TriageLevel
import com.ilinetech.emergency.core.sms.AlertPayload
import com.ilinetech.emergency.databinding.FragmentDashboardBinding
import com.ilinetech.emergency.service.ConnectionForegroundService
import com.ilinetech.emergency.sms.SmsDispatcher
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Shows the active profile, the master on/off switch (also in Settings —
 * both read/write the same Prefs.appEnabled so they stay consistent), and
 * lets the user send an alert to any department at their OWN facility.
 *
 * Sending currently only has a real transport: SMS, via SmsDispatcher. FCM
 * publishing calls RemoteAlertPublisher, which hits a real Cloud Function
 * once one is deployed and configured — see CLOUD_FUNCTION_NOTES.md.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private var activeProfile: StaffMemberEntity? = null

    private var targetDepartmentIndex = 0
    private var priorityIndex = TriageLevel.MODERATE.ordinal
    private var reasonIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)

        setupSpinners()
        setupHeaderSwitch()
        loadProfile()

        binding.buttonSendAlert.setOnClickListener { onSendClicked() }
        binding.textFooter.text = getString(R.string.footer_combined, getString(R.string.footer_copyright), getString(R.string.footer_version, BuildConfig.VERSION_NAME))
    }

    private fun setupHeaderSwitch() {
        binding.switchAppEnabledHeader.isChecked = repository.prefs.appEnabled
        binding.switchAppEnabledHeader.setOnCheckedChangeListener { _, checked ->
            repository.prefs.appEnabled = checked
            if (checked) {
                ConnectionForegroundService.start(requireContext())
            } else {
                ConnectionForegroundService.stop(requireContext())
            }
        }
    }

    private fun setupSpinners() {
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, StaffRole.entries.map { it.label })
        binding.spinnerTargetDepartment.setAdapter(roleAdapter)
        binding.spinnerTargetDepartment.setText(roleAdapter.getItem(0), false)
        binding.spinnerTargetDepartment.setOnItemClickListener { _, _, position, _ -> targetDepartmentIndex = position }

        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, TriageLevel.entries.map { it.label })
        binding.spinnerPriority.setAdapter(priorityAdapter)
        binding.spinnerPriority.setText(priorityAdapter.getItem(TriageLevel.MODERATE.ordinal), false)
        binding.spinnerPriority.setOnItemClickListener { _, _, position, _ -> priorityIndex = position }

        val reasonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, IncidentReason.entries.map { it.label })
        binding.spinnerReason.setAdapter(reasonAdapter)
        binding.spinnerReason.setText(reasonAdapter.getItem(0), false)
        binding.spinnerReason.setOnItemClickListener { _, _, position, _ -> reasonIndex = position }
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            activeProfile = repository.getActiveProfile()
            val profile = activeProfile
            if (profile == null) {
                binding.textProfileName.text = getString(R.string.dashboard_no_profile)
                binding.textProfileRole.text = ""
                binding.textFacilitySerial.text = ""
                binding.textDeptSerial.text = ""
                binding.buttonSendAlert.isEnabled = false
                return@launch
            }
            binding.textProfileName.text = profile.fullName
            binding.textProfileRole.text = "${profile.role} · ${profile.groupId}"
            binding.textFacilitySerial.text = "${getString(R.string.label_facility_serial)}: ${profile.facilitySerial}"
            binding.textDeptSerial.text = "${getString(R.string.label_dept_serial)}: ${profile.deptSerial}"
        }
    }

    private fun onSendClicked() {
        val profile = activeProfile ?: return
        val message = binding.editMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), R.string.send_alert_missing_message, Toast.LENGTH_SHORT).show()
            return
        }

        val targetRole = StaffRole.entries[targetDepartmentIndex]
        val priority = TriageLevel.entries[priorityIndex]
        val reason = IncidentReason.entries[reasonIndex]

        binding.buttonSendAlert.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val targetDeptSerial = repository.computeSiblingDeptSerial(profile, targetRole)

            val payload = AlertPayload(
                facilitySerial = profile.facilitySerial,
                deptSerial = targetDeptSerial,
                groupId = profile.groupId,
                priority = priority,
                reason = reason,
                message = message
            )

            val sentCount = SmsDispatcher.sendAlert(requireContext().applicationContext, payload)

            com.ilinetech.emergency.fcm.RemoteAlertPublisher.publish(
                facilitySerial = payload.facilitySerial,
                deptSerial = payload.deptSerial,
                groupId = payload.groupId,
                priorityCode = payload.priority.smsCode,
                reason = payload.reason.name,
                message = payload.message,
                senderLabel = profile.fullName
            )

            binding.textSendResult.text = if (sentCount > 0) {
                getString(R.string.send_alert_sms_result, sentCount)
            } else {
                getString(R.string.send_alert_no_contacts)
            }
            binding.buttonSendAlert.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
