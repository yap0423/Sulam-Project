package com.example.entrepreneurapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemSearchBusinessBinding
import com.example.entrepreneurapp.models.SearchResult

class SearchBusinessesAdapter(
    private val onBusinessClick: (SearchResult.BusinessResult) -> Unit
) : RecyclerView.Adapter<SearchBusinessesAdapter.ViewHolder>() {

    private var businesses = listOf<SearchResult.BusinessResult>()

    fun submitList(newBusinesses: List<SearchResult.BusinessResult>) {
        businesses = newBusinesses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchBusinessBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(businesses[position])
    }

    override fun getItemCount() = businesses.size

    inner class ViewHolder(
        private val binding: ItemSearchBusinessBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(business: SearchResult.BusinessResult) {
            binding.tvBusinessName.text = business.businessName
            binding.tvType.text = "[${business.type}]"
            binding.tvLocation.text = "📍 ${business.location}"

            binding.root.setOnClickListener {
                onBusinessClick(business)
            }
        }
    }
}