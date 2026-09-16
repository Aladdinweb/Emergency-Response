package com.ilinetech.emergency.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.AppRepository
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.databinding.FragmentLogsBinding
import kotlinx.coroutines.launch

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Swipe-to-delete on individual entries (with an Undo snackbar, since a
 * swipe is easy to trigger by accident), plus "Effacer tout" in the header
 * for clearing everything at once behind a confirmation dialog.
 */
class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AppRepository
    private lateinit var adapter: AlertLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = AppRepository(requireContext().applicationContext)

        adapter = AlertLogAdapter { log ->
            viewLifecycleOwner.lifecycleScope.launch { repository.acknowledge(log.id) }
        }
        binding.recyclerLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLogs.adapter = adapter

        attachSwipeToDelete()

        binding.buttonClearAll.setOnClickListener { confirmClearAll() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeAlertLogs().collect { logs ->
                    adapter.submitList(logs)
                    binding.textEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerLogs.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val log = adapter.currentList.getOrNull(position) ?: return
                deleteWithUndo(log)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerLogs)
    }

    private fun deleteWithUndo(log: AlertLogEntity) {
        viewLifecycleOwner.lifecycleScope.launch { repository.deleteLog(log) }
        Snackbar.make(binding.root, R.string.log_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewLifecycleOwner.lifecycleScope.launch {
                    // Re-insert as a new row (autoGenerate id) rather than
                    // restoring the exact id — acceptable for an undo window,
                    // and avoids fighting Room's primary key semantics.
                    com.ilinetech.emergency.core.data.AppDatabase.getInstance(requireContext().applicationContext)
                        .alertLogDao().insert(log.copy(id = 0))
                }
            }
            .show()
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_all_confirm_title)
            .setMessage(R.string.clear_all_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch { repository.clearAllLogs() }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
