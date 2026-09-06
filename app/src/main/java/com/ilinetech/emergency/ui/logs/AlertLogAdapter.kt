package com.ilinetech.emergency.ui.logs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ilinetech.emergency.R
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertLogEntity
import com.ilinetech.emergency.core.data.entities.AlertTransport
import com.ilinetech.emergency.core.model.IncidentReason
import com.ilinetech.emergency.core.model.TriageLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * © ILINE TECH BY FERAK ALADDIN
 */
class AlertLogAdapter(
    private val onAcknowledge: (AlertLogEntity) -> Unit
) : ListAdapter<AlertLogEntity, AlertLogAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat, onAcknowledge)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val priorityBar: View = itemView.findViewById(R.id.priorityBar)
        private val counterpart: TextView = itemView.findViewById(R.id.textCounterpart)
        private val direction: TextView = itemView.findViewById(R.id.textDirection)
        private val message: TextView = itemView.findViewById(R.id.textMessage)
        private val timestamp: TextView = itemView.findViewById(R.id.textTimestamp)
        private val transport: TextView = itemView.findViewById(R.id.textTransport)
        private val ackButton: Button = itemView.findViewById(R.id.buttonAcknowledge)

        fun bind(log: AlertLogEntity, dateFormat: SimpleDateFormat, onAcknowledge: (AlertLogEntity) -> Unit) {
            val context = itemView.context

            val colorRes = when (log.priorityLevel) {
                TriageLevel.LOW.smsCode -> R.color.triage_low
                TriageLevel.CRITICAL.smsCode -> R.color.triage_critical
                else -> R.color.triage_moderate
            }
            priorityBar.setBackgroundColor(context.getColor(colorRes))

            counterpart.text = log.counterpartLabel
            direction.text = context.getString(
                if (log.direction == AlertDirection.INCOMING) R.string.direction_incoming else R.string.direction_outgoing
            )
            val reasonLabel = runCatching { IncidentReason.valueOf(log.reason).label }.getOrNull()
            message.text = if (reasonLabel != null && reasonLabel != log.message) {
                "$reasonLabel — ${log.message}"
            } else {
                log.message
            }
            timestamp.text = dateFormat.format(Date(log.sentAtEpochMillis))
            transport.text = if (log.transport == AlertTransport.SMS) "SMS" else "FCM"

            if (log.direction == AlertDirection.INCOMING && !log.acknowledged) {
                ackButton.visibility = View.VISIBLE
                ackButton.isEnabled = true
                ackButton.text = context.getString(R.string.action_acknowledge)
                ackButton.setOnClickListener { onAcknowledge(log) }
            } else if (log.direction == AlertDirection.INCOMING && log.acknowledged) {
                ackButton.visibility = View.VISIBLE
                ackButton.isEnabled = false
                ackButton.text = context.getString(R.string.acknowledged_label)
            } else {
                ackButton.visibility = View.GONE
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AlertLogEntity>() {
        override fun areItemsTheSame(oldItem: AlertLogEntity, newItem: AlertLogEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AlertLogEntity, newItem: AlertLogEntity) = oldItem == newItem
    }
}
