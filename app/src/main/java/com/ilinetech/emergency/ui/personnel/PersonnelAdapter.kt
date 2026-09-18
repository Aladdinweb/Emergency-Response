package com.ilinetech.emergency.ui.personnel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.entities.StaffMemberEntity
import com.ilinetech.emergency.core.model.StaffRole

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
class PersonnelAdapter : ListAdapter<StaffMemberEntity, PersonnelAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_personnel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.textName)
        private val role: TextView = itemView.findViewById(R.id.textRole)
        private val group: TextView = itemView.findViewById(R.id.textGroup)

        fun bind(staff: StaffMemberEntity) {
            name.text = staff.fullName
            val roleLabel = runCatching { StaffRole.valueOf(staff.role).label }.getOrDefault(staff.role)
            role.text = roleLabel
            group.text = staff.groupId
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<StaffMemberEntity>() {
        override fun areItemsTheSame(oldItem: StaffMemberEntity, newItem: StaffMemberEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StaffMemberEntity, newItem: StaffMemberEntity) = oldItem == newItem
    }
}
