package com.ilinetech.emergency.ui.sos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.core.model.IncidentReason
import com.ilinetech.emergency.core.model.StaffRole
import com.ilinetech.emergency.core.model.TriageLevel
import com.ilinetech.emergency.core.sms.AlertPayload
import com.ilinetech.emergency.databinding.DialogSosBinding
import com.ilinetech.emergency.fcm.RemoteAlertPublisher
import com.ilinetech.emergency.sms.SmsDispatcher
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * National numbers open the phone dialer pre-filled (ACTION_DIAL) rather
 * than calling directly — this needs no CALL_PHONE permission and leaves
 * the actual call as a deliberate action the user confirms, which is both
 * safer and the normal pattern for "quick dial" shortcuts.
 *
 * "SOS SÉCURITÉ" reuses the exact same send path as the Dashboard's alert
 * flow (SmsDispatcher + RemoteAlertPublisher), just pre-filled: CRITICAL
 * priority, AGRESSION reason, targeted at this facility's Agent de
 * sécurité role, no message needed (the reason label carries the context).
 */
class SosDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSosBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)

        loadActiveStaffBadge()

        binding.buttonProtectionCivile14.setOnClickListener { dial("14") }
        binding.buttonProtectionCivile1021.setOnClickListener { dial("1021") }
        binding.buttonPolice.setOnClickListener { dial("1548") }
        binding.buttonGendarmerie.setOnClickListener { dial("1055") }
        binding.buttonSosSecurity.setOnClickListener { onSosSecurityClicked() }
        binding.textActiveStaffBadge.setOnClickListener {
            com.ilinetech.emergency.ui.personnel.PersonnelDialogFragment()
                .show(parentFragmentManager, com.ilinetech.emergency.ui.personnel.PersonnelDialogFragment.TAG)
        }
    }

    private fun loadActiveStaffBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = repository.countActiveStaffAtMyFacility()
            binding.textActiveStaffBadge.text = getString(R.string.active_staff_badge, count)
        }
    }

    private fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    private fun onSosSecurityClicked() {
        binding.buttonSosSecurity.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = repository.getActiveProfile()
            if (profile == null) {
                binding.textSosResult.text = getString(R.string.dashboard_no_profile)
                binding.buttonSosSecurity.isEnabled = true
                return@launch
            }

            val securityDeptSerial = repository.computeSiblingDeptSerial(profile, StaffRole.AGENT_DE_SECURITE)
            val payload = AlertPayload(
                facilitySerial = profile.facilitySerial,
                deptSerial = securityDeptSerial,
                groupId = profile.groupId,
                priority = TriageLevel.CRITICAL,
                reason = IncidentReason.AGRESSION,
                message = ""
            )

            val sentCount = SmsDispatcher.sendAlert(requireContext().applicationContext, payload)

            RemoteAlertPublisher.publish(
                facilitySerial = payload.facilitySerial,
                deptSerial = payload.deptSerial,
                groupId = payload.groupId,
                priorityCode = payload.priority.smsCode,
                reason = payload.reason.name,
                message = payload.message,
                senderLabel = profile.fullName
            )

            binding.textSosResult.text = if (sentCount > 0) {
                getString(R.string.sos_security_sent, sentCount)
            } else {
                getString(R.string.sos_security_no_contacts)
            }
            binding.buttonSosSecurity.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
