package com.example.entrepreneurapp.ui.planner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemWeeklyYieldBinding
import com.example.entrepreneurapp.models.WeeklyYield

class WeeklyYieldAdapter(private var currentRegion: String) : ListAdapter<WeeklyYield, WeeklyYieldAdapter.ViewHolder>(DiffCallback()) {

    fun updateRegion(region: String) {
        currentRegion = region
        notifyDataSetChanged() // Rebind views to reflect new region
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeeklyYieldBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemWeeklyYieldBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(weeklyYield: WeeklyYield) {
            binding.apply {
                tvWeek.text = "${weeklyYield.weekStart} - ${weeklyYield.weekEnd}"
                tvYield.text = "${weeklyYield.totalYield.toInt()} tonnes"
                tvRiskLevel.text = "(${weeklyYield.riskLevel.uppercase()})"
                tvRiskLevel.setTextColor(Color.parseColor(weeklyYield.getRiskColor()))

                progressBar.progress = weeklyYield.getYieldPercentage(currentRegion)
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(weeklyYield.getRiskColor())
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WeeklyYield>() {
        override fun areItemsTheSame(oldItem: WeeklyYield, newItem: WeeklyYield) =
            oldItem.weekStart == newItem.weekStart

        override fun areContentsTheSame(oldItem: WeeklyYield, newItem: WeeklyYield) =
            oldItem == newItem
    }


}