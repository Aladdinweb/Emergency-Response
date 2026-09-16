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
import com.ilinetech.emergency.core.model.StaffDepartment
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
 * Uses Material exposed-dropdown-menu fields (AutoCompleteTextView in
 * non-editable mode) instead of plain Spinners, and TextInputLayout/
 * TextInputEditText for free-text fields. Since AutoCompleteTextView has no
 * built-in "selected index" concept the way Spinner does, each field's
 * current selection is tracked in a local var, updated from
 * setOnItemClickListener.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var repository: AppRepository

    private var currentInstitutions: List<InstitutionEntity> = emptyList()
    private var currentSubBranches: List<SubBranchEntity> = emptyList()

    private var wilayaIndex = 0
    private var typeIndex = 0
    private var institutionIndex = 0
    private var subBranchIndex = 0
    private var departmentIndex = 0
    private var roleIndex = 0
    private var groupIdIndex = 0
    private val groupIdLabels = listOf("A", "B", "C", "D", "E", "F")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = AppRepository(applicationContext)

        setupWilayaField()
        setupEstablishmentTypeField()
        setupDepartmentAndRoleFields()

        binding.buttonRegister.setOnClickListener { onRegisterClicked() }
        binding.textFooter.text = getString(
            R.string.footer_combined,
            getString(R.string.footer_copyright),
            getString(R.string.footer_version, com.ilinetech.emergency.BuildConfig.VERSION_NAME)
        )

        loadInstitutionsForCurrentSelection()
    }

    private fun <T> dropdownAdapter(items: List<T>, label: (T) -> String) =
        ArrayAdapter(this, android.R.layout.simple_list_item_1, items.map(label))

    private fun setupWilayaField() {
        val adapter = dropdownAdapter(AlgerianWilayas.all) { "${it.code} — ${it.name}" }
        binding.spinnerWilaya.setAdapter(adapter)
        binding.spinnerWilaya.setText(adapter.getItem(0), false)
        binding.spinnerWilaya.setOnItemClickListener { _, _, position, _ ->
            wilayaIndex = position
            loadInstitutionsForCurrentSelection()
        }
    }

    private fun setupEstablishmentTypeField() {
        val adapter = dropdownAdapter(EstablishmentType.entries) { it.label }
        binding.spinnerEstablishmentType.setAdapter(adapter)
        binding.spinnerEstablishmentType.setText(adapter.getItem(0), false)
        binding.spinnerEstablishmentType.setOnItemClickListener { _, _, position, _ ->
            typeIndex = position
            loadInstitutionsForCurrentSelection()
        }
    }

    private fun setupDepartmentAndRoleFields() {
        val adapter = dropdownAdapter(StaffDepartment.entries) { it.label }
        binding.spinnerDepartment.setAdapter(adapter)
        binding.spinnerDepartment.setText(adapter.getItem(0), false)
        binding.spinnerDepartment.setOnItemClickListener { _, _, position, _ ->
            departmentIndex = position
            roleIndex = 0
            updateRoleFieldForSelectedDepartment()
        }
        updateRoleFieldForSelectedDepartment()

        val groupAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, groupIdLabels)
        binding.spinnerGroupId.setAdapter(groupAdapter)
        binding.spinnerGroupId.setText(groupIdLabels[0], false)
        binding.spinnerGroupId.setOnItemClickListener { _, _, position, _ -> groupIdIndex = position }
    }

    private fun updateRoleFieldForSelectedDepartment() {
        val department = StaffDepartment.entries[departmentIndex]
        val roles = StaffRole.entries.filter { it.department == department }
        val adapter = dropdownAdapter(roles) { it.label }
        binding.spinnerRole.setAdapter(adapter)
        binding.spinnerRole.setText(adapter.getItem(0), false)
        binding.spinnerRole.setOnItemClickListener { _, _, position, _ -> roleIndex = position }
    }

    private fun selectedWilaya(): Wilaya = AlgerianWilayas.all[wilayaIndex]
    private fun selectedType(): EstablishmentType = EstablishmentType.entries[typeIndex]

    private fun loadInstitutionsForCurrentSelection() {
        lifecycleScope.launch {
            val wilaya = selectedWilaya()
            val type = selectedType()
            currentInstitutions = repository.observeInstitutions(wilaya.code, type).first()
            institutionIndex = 0

            val adapter = dropdownAdapter(currentInstitutions) { it.name }
            binding.spinnerInstitution.setAdapter(adapter)
            binding.spinnerInstitution.setText(if (currentInstitutions.isNotEmpty()) adapter.getItem(0) else "", false)

            if (currentInstitutions.isEmpty()) {
                currentSubBranches = emptyList()
                binding.spinnerSubBranch.setAdapter(dropdownAdapter(emptyList<String>()) { it })
                binding.spinnerSubBranch.setText("", false)
                Toast.makeText(this@OnboardingActivity, R.string.error_no_institution_data, Toast.LENGTH_SHORT).show()
            } else {
                binding.spinnerInstitution.setOnItemClickListener { _, _, position, _ ->
                    institutionIndex = position
                    loadSubBranchesForSelectedInstitution()
                }
                loadSubBranchesForSelectedInstitution()
            }
        }
    }

    private fun loadSubBranchesForSelectedInstitution() {
        val institution = currentInstitutions.getOrNull(institutionIndex) ?: return
        lifecycleScope.launch {
            currentSubBranches = repository.observeSubBranches(institution.id).first()
            subBranchIndex = 0
            val adapter = dropdownAdapter(currentSubBranches) { it.name }
            binding.spinnerSubBranch.setAdapter(adapter)
            binding.spinnerSubBranch.setText(if (currentSubBranches.isNotEmpty()) adapter.getItem(0) else "", false)
            binding.spinnerSubBranch.setOnItemClickListener { _, _, position, _ -> subBranchIndex = position }
        }
    }

    private fun onRegisterClicked() {
        val institution = currentInstitutions.getOrNull(institutionIndex)
        val subBranch = currentSubBranches.getOrNull(subBranchIndex)
        val fullName = binding.editFullName.text.toString().trim()
        val groupId = groupIdLabels[groupIdIndex]
        val phoneNumber = binding.editPhoneNumber.text.toString().trim()
        val department = StaffDepartment.entries[departmentIndex]
        val role = StaffRole.entries.filter { it.department == department }.getOrNull(roleIndex)

        if (institution == null || subBranch == null || fullName.isEmpty() || role == null) {
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
}
