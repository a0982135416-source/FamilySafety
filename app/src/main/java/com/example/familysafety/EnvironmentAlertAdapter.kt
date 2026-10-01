package com.example.familysafety

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.familysafety.databinding.ItemEnvironmentAlertBinding
import java.text.DateFormat
import java.util.Date

/** STEP 4-2: environment cards; Resolve is a mock-record action only. */
class EnvironmentAlertAdapter(
    private val onResolve: (EnvironmentAlert) -> Unit
) : ListAdapter<EnvironmentAlert, EnvironmentAlertAdapter.AlertViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EnvironmentAlert>() {
            override fun areItemsTheSame(oldItem: EnvironmentAlert, newItem: EnvironmentAlert) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: EnvironmentAlert, newItem: EnvironmentAlert) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemEnvironmentAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(
        private val binding: ItemEnvironmentAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EnvironmentAlert) {
            val context = binding.root.context
            val gas = item.type == EnvironmentAlertType.GAS_LEAK_RISK
            binding.imageViewAlertItemIcon.setImageResource(
                if (gas) R.drawable.ic_gas else R.drawable.ic_flame
            )
            binding.textViewAlertItemTitle.setText(
                if (gas) R.string.alert_type_gas_leak else R.string.alert_type_unattended
            )
            binding.textViewAlertItemMessage.setText(
                if (gas) R.string.alert_message_gas_leak else R.string.alert_message_unattended
            )
            binding.textViewAlertItemLocation.setText(R.string.alert_location_kitchen)
            binding.textViewAlertItemTime.text =
                DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, context.resources.configuration.locales[0]
                )
                    .format(Date(item.createdAt))

            val pending = item.status == EnvironmentAlertStatus.PENDING
            binding.textViewAlertItemStatus.setText(
                if (pending) R.string.alert_status_pending else R.string.alert_status_resolved
            )
            binding.textViewAlertItemStatus.setTextColor(
                ContextCompat.getColor(context,
                    if (pending) R.color.primary_blue else R.color.title_blue)
            )
            binding.buttonAlertItemResolve.visibility = if (pending) View.VISIBLE else View.GONE
            binding.buttonAlertItemResolve.setOnClickListener {
                if (pending) onResolve(item)
            }
        }
    }
}
