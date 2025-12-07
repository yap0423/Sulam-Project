package com.example.entrepreneurapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemSearchFarmBinding
import com.example.entrepreneurapp.models.SearchResult

class SearchFarmsAdapter(
    private val onFarmClick: (SearchResult.FarmResult) -> Unit
) : RecyclerView.Adapter<SearchFarmsAdapter.ViewHolder>() {

    private var farms = listOf<SearchResult.FarmResult>()

    fun submitList(newFarms: List<SearchResult.FarmResult>) {
        farms = newFarms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchFarmBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(farms[position])
    }

    override fun getItemCount() = farms.size

    inner class ViewHolder(
        private val binding: ItemSearchFarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(farm: SearchResult.FarmResult) {
            binding.tvFarmName.text = farm.farmName
            binding.tvLocation.text = "📍 ${farm.location}"
            binding.tvSize.text =  "${farm.totalSize} acres"

            binding.root.setOnClickListener {
                onFarmClick(farm)
            }
        }
    }
}