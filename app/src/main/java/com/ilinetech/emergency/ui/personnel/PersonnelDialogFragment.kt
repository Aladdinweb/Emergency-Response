package com.ilinetech.emergency.ui.personnel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.databinding.DialogPersonnelBinding
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Lists active staff at the caller's own facility (sub-branch). Honest
 * scope note (also in StaffMemberDao/AppRepository doc comments): this
 * reads THIS DEVICE's local Room table — accurate for a shared station
 * with multiple registered profiles, not a live facility-wide roster
 * across separate phones (no shared directory exists yet).
 */
class PersonnelDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogPersonnelBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var adapter: PersonnelAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogPersonnelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)

        adapter = PersonnelAdapter()
        binding.recyclerPersonnel.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPersonnel.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = repository.getActiveProfile()
            if (profile == null) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.textEmpty.text = getString(R.string.dashboard_no_profile)
                return@launch
            }

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeActiveStaffAtBranch(profile.subBranchId).collect { staff ->
                    adapter.submitList(staff)
                    binding.textBadge.text = getString(R.string.active_staff_badge, staff.size)
                    binding.textEmpty.visibility = if (staff.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerPersonnel.visibility = if (staff.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "personnel"
    }
}
