package com.example.entrepreneurapp.ui.planner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemHarvestCardBinding
import com.example.entrepreneurapp.models.HarvestSchedule

class MyHarvestsAdapter(
    private val onEditClick: (HarvestSchedule) -> Unit,
    private val onDeleteClick: (HarvestSchedule) -> Unit
) : ListAdapter<HarvestSchedule, MyHarvestsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHarvestCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHarvestCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(harvest: HarvestSchedule) {
            binding.apply {
                tvCropType.text = harvest.cropType
                tvVariety.text = "Variety: ${harvest.variety}"
                tvPlantedDate.text = "Planted: ${formatDate(harvest.plantedDate.toDate())}"
                tvEstimatedYield.text = "Est. Yield: ${harvest.estimatedYield.toInt()} tonnes"
                tvRegion.text = "Region: ${harvest.region}"
                tvHarvestPeriod.text = "📅 ${harvest.getHarvestPeriodString()}"

                // Check for conflicts (simplified - you can enhance this)
                val daysUntil = harvest.getDaysUntilHarvest()
                if (daysUntil < 0) {
                    tvConflict.text = "⚠️ Overdue"
                    tvConflict.visibility = android.view.View.VISIBLE
                    tvConflict.setTextColor(Color.parseColor("#D32F2F"))
                    btnEdit.isEnabled = false
                    btnDelete.isEnabled = false
                    btnEdit.alpha = 0.5f // Optional: visually indicate disabled
                    btnDelete.alpha = 0.5f
                } else if (daysUntil <= 7) {
                    tvConflict.text = "⏰ Harvest in $daysUntil day(s)"
                    tvConflict.visibility = android.view.View.VISIBLE
                    tvConflict.setTextColor(Color.parseColor("#FFA000"))
                    btnEdit.isEnabled = true
                    btnDelete.isEnabled = true
                    btnEdit.alpha = 1.0f
                    btnDelete.alpha = 1.0f
                } else {
                    tvConflict.visibility = android.view.View.GONE
                    btnEdit.isEnabled = true
                    btnDelete.isEnabled = true
                    btnEdit.alpha = 1.0f
                    btnDelete.alpha = 1.0f
                }

                btnEdit.setOnClickListener { onEditClick(harvest) }
                btnDelete.setOnClickListener { onDeleteClick(harvest) }
            }
        }

        private fun formatDate(date: java.util.Date): String {
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return formatter.format(date)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HarvestSchedule>() {
        override fun areItemsTheSame(oldItem: HarvestSchedule, newItem: HarvestSchedule) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HarvestSchedule, newItem: HarvestSchedule) =
            oldItem == newItem
    }
}