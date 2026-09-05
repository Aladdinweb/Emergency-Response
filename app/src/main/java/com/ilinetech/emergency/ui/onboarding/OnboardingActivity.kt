package com.ilinetech.emergency.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.core.data.entities.InstitutionEntity
import com.ilinetech.emergency.core.data.entities.SubBranchEntity
import com.ilinetech.emergency.core.model.AlgerianWilayas
import com.ilinetech.emergency.core.model.EstablishmentType
import com.ilinetech.emergency.core.model.FacilitySelection
import com.ilinetech.emergency.core.model.StaffRole
import com.ilinetech.emergency.core.model.Wilaya
import com.ilinetech.emergency.databinding.ActivityOnboardingBinding
import com.ilinetech.emergency.ui.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * First-run registration flow implementing the spec's cascade:
 *   Wilaya -> EstablishmentType -> Institution -> SubBranch -> Department/Role
 *
 * Institution and SubBranch spinners are populated from Room and are only
 * as complete as SeedData / whatever's been synced — a wilaya/type with no
 * registered institutions yet will show an empty institution list, which is
 * expected for anything beyond the seeded EPSP ES SENIA example until a
 * directory sync or admin-entry flow exists (out of scope for this scaffold).
 *
 * Uses view binding (see build.gradle.kts notes) rather than findViewById.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var repository: AppRepository

    private var currentInstitutions: List<InstitutionEntity> = emptyList()
    private var currentSubBranches: List<SubBranchEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = AppRepository(applicationContext)

        setupWilayaSpinner()
        setupEstablishmentTypeSpinner()
        setupDepartmentAndRoleSpinners()

        binding.buttonRegister.setOnClickListener { onRegisterClicked() }

        // Initial population of the dependent spinners for the default selection.
        loadInstitutionsForCurrentSelection()
    }

    private fun setupWilayaSpinner() {
        val labels = AlgerianWilayas.all.map { "${it.code} — ${it.name}" }
        binding.spinnerWilaya.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        binding.spinnerWilaya.onItemSelectedListener = onSelectionChanged { loadInstitutionsForCurrentSelection() }
    }

    private fun setupEstablishmentTypeSpinner() {
        val labels = EstablishmentType.entries.map { it.label }
        binding.spinnerEstablishmentType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        binding.spinnerEstablishmentType.onItemSelectedListener = onSelectionChanged { loadInstitutionsForCurrentSelection() }
    }

    private fun setupDepartmentAndRoleSpinners() {
        val departmentLabels = com.ilinetech.emergency.core.model.StaffDepartment.entries.map { it.label }
        binding.spinnerDepartment.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, departmentLabels)
        binding.spinnerDepartment.onItemSelectedListener = onSelectionChanged { updateRoleSpinnerForSelectedDepartment() }
        updateRoleSpinnerForSelectedDepartment()
    }

    private fun updateRoleSpinnerForSelectedDepartment() {
        val department = com.ilinetech.emergency.core.model.StaffDepartment.entries[binding.spinnerDepartment.selectedItemPosition]
        val roles = StaffRole.entries.filter { it.department == department }
        binding.spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles.map { it.label })
    }

    private fun selectedWilaya(): Wilaya = AlgerianWilayas.all[binding.spinnerWilaya.selectedItemPosition]
    private fun selectedType(): EstablishmentType = EstablishmentType.entries[binding.spinnerEstablishmentType.selectedItemPosition]

    private fun loadInstitutionsForCurrentSelection() {
        lifecycleScope.launch {
            val wilaya = selectedWilaya()
            val type = selectedType()
            currentInstitutions = repository.observeInstitutions(wilaya.code, type).first()

            val labels = currentInstitutions.map { it.name }
            binding.spinnerInstitution.adapter = ArrayAdapter(
                this@OnboardingActivity, android.R.layout.simple_spinner_dropdown_item, labels
            )

            if (currentInstitutions.isEmpty()) {
                binding.spinnerSubBranch.adapter = ArrayAdapter(
                    this@OnboardingActivity, android.R.layout.simple_spinner_dropdown_item, emptyList<String>()
                )
                currentSubBranches = emptyList()
            } else {
                binding.spinnerInstitution.onItemSelectedListener = onSelectionChanged { loadSubBranchesForSelectedInstitution() }
                loadSubBranchesForSelectedInstitution()
            }
        }
    }

    private fun loadSubBranchesForSelectedInstitution() {
        val institution = currentInstitutions.getOrNull(binding.spinnerInstitution.selectedItemPosition) ?: return
        lifecycleScope.launch {
            currentSubBranches = repository.observeSubBranches(institution.id).first()
            val labels = currentSubBranches.map { it.name }
            binding.spinnerSubBranch.adapter = ArrayAdapter(
                this@OnboardingActivity, android.R.layout.simple_spinner_dropdown_item, labels
            )
        }
    }

    private fun onRegisterClicked() {
        val institution = currentInstitutions.getOrNull(binding.spinnerInstitution.selectedItemPosition)
        val subBranch = currentSubBranches.getOrNull(binding.spinnerSubBranch.selectedItemPosition)
        val fullName = binding.editFullName.text.toString().trim()
        val groupId = binding.editGroupId.text.toString().trim()
        val phoneNumber = binding.editPhoneNumber.text.toString().trim()
        val department = com.ilinetech.emergency.core.model.StaffDepartment.entries[binding.spinnerDepartment.selectedItemPosition]
        val role = StaffRole.entries.filter { it.department == department }
            .getOrNull(binding.spinnerRole.selectedItemPosition)

        if (institution == null || subBranch == null || fullName.isEmpty() || groupId.isEmpty() || role == null) {
            Toast.makeText(this, R.string.error_select_all_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val selection = FacilitySelection(
            wilaya = selectedWilaya(),
            type = selectedType(),
            institution = com.ilinetech.emergency.core.model.Institution(
                id = institution.id, name = institution.name, type = selectedType(), wilaya = selectedWilaya()
            ),
            subBranch = com.ilinetech.emergency.core.model.SubBranch(
                id = subBranch.id, name = subBranch.name, institutionId = institution.id
            )
        )

        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                repository.registerStaffMember(
                    selection = selection,
                    institutionIndex = currentInstitutions.indexOf(institution),
                    branchIndex = currentSubBranches.indexOf(subBranch),
                    role = role,
                    fullName = fullName,
                    groupId = groupId,
                    ownPhoneNumber = phoneNumber.ifEmpty { null }
                )
            }.onSuccess {
                Toast.makeText(this@OnboardingActivity, R.string.registration_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
            }.onFailure {
                setLoading(false)
                Toast.makeText(this@OnboardingActivity, R.string.registration_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !loading
    }

    /** Small helper so every spinner doesn't need a full anonymous listener block. */
    private fun onSelectionChanged(action: () -> Unit) =
        object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = action()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
}
