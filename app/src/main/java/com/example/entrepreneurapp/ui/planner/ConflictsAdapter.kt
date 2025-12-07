package com.example.entrepreneurapp.ui.planner

import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemConflictBinding
import com.example.entrepreneurapp.models.HarvestConflict


class ConflictsAdapter(
    private val onConflictClick: (HarvestConflict) -> Unit
) : ListAdapter<HarvestConflict, ConflictsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConflictBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemConflictBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conflict: HarvestConflict) {
            binding.apply {
                // Use the earliest start date and latest end date from all schedules in this conflict
                val startDate = conflict.schedules.minOfOrNull { it.harvestStartDate.toDate() }
                val endDate = conflict.schedules.maxOfOrNull { it.harvestEndDate.toDate() }

                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateRange = if (startDate != null && endDate != null) {
                    "${formatter.format(startDate)} - ${formatter.format(endDate)}"
                } else {
                    "N/A"
                }

                tvDate.text = "${conflict.getRiskEmoji()} $dateRange"
                tvFarmerCount.text = "${conflict.farmersAffected.size} farmers | Total: ${conflict.totalYield.toInt()} tonnes"
                tvRiskLevel.text = "[${conflict.riskLevel.uppercase()}]"
                tvRiskLevel.setTextColor(Color.parseColor(conflict.getRiskColor()))

                root.setOnClickListener {
                    onConflictClick(conflict)
                }
            }
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<HarvestConflict>() {
        override fun areItemsTheSame(oldItem: HarvestConflict, newItem: HarvestConflict) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: HarvestConflict, newItem: HarvestConflict) =
            oldItem == newItem
    }
}