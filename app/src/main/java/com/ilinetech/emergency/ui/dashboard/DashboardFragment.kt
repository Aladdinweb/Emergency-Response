package com.ilinetech.emergency.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import com.ilinetech.emergency.core.model.StaffRole
import com.ilinetech.emergency.core.model.TriageLevel
import com.ilinetech.emergency.core.sms.AlertPayload
import com.ilinetech.emergency.databinding.FragmentDashboardBinding
import com.ilinetech.emergency.sms.SmsDispatcher
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Shows the active profile and lets the user send an alert to any
 * department at their OWN facility (cross-facility alerting — e.g. one
 * polyclinique alerting another — isn't in this scaffold; it would need a
 * facility picker reusing the onboarding cascade, deliberately left out to
 * keep this slice's scope bounded).
 *
 * Sending currently only has a real transport: SMS, via SmsDispatcher. FCM
 * publishing is a stub (see RemoteAlertPublisher) since it requires a
 * backend this project doesn't have yet — both are attempted so the UI
 * doesn't need to change once a backend exists, but only the SMS result is
 * meaningful right now.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private var activeProfile: StaffMemberEntity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)

        setupSpinners()
        loadProfile()

        binding.buttonSendAlert.setOnClickListener { onSendClicked() }
    }

    private var targetDepartmentIndex = 0
    private var priorityIndex = TriageLevel.MODERATE.ordinal

    private fun setupSpinners() {
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, StaffRole.entries.map { it.label })
        binding.spinnerTargetDepartment.setAdapter(roleAdapter)
        binding.spinnerTargetDepartment.setText(roleAdapter.getItem(0), false)
        binding.spinnerTargetDepartment.setOnItemClickListener { _, _, position, _ -> targetDepartmentIndex = position }

        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, TriageLevel.entries.map { it.label })
        binding.spinnerPriority.setAdapter(priorityAdapter)
        binding.spinnerPriority.setText(priorityAdapter.getItem(TriageLevel.MODERATE.ordinal), false)
        binding.spinnerPriority.setOnItemClickListener { _, _, position, _ -> priorityIndex = position }
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

        binding.buttonSendAlert.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val targetDeptSerial = repository.computeSiblingDeptSerial(profile, targetRole)

            val payload = AlertPayload(
                facilitySerial = profile.facilitySerial,
                deptSerial = targetDeptSerial,
                groupId = profile.groupId,
                priority = priority,
                message = message
            )

            val sentCount = SmsDispatcher.sendAlert(requireContext().applicationContext, payload)

            // Attempted for forward-compatibility; currently always NotImplemented — see RemoteAlertPublisher.
            com.ilinetech.emergency.fcm.RemoteAlertPublisher.publish(
                facilitySerial = payload.facilitySerial,
                deptSerial = payload.deptSerial,
                groupId = payload.groupId,
                priorityCode = payload.priority.smsCode,
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
