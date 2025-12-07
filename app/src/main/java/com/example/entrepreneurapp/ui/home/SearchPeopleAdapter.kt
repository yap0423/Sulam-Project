package com.example.entrepreneurapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemSearchPersonBinding
import com.example.entrepreneurapp.models.SearchResult

class SearchPeopleAdapter(
    private val onPersonClick: (SearchResult.PersonResult) -> Unit
) : RecyclerView.Adapter<SearchPeopleAdapter.ViewHolder>() {

    private var people = listOf<SearchResult.PersonResult>()

    fun submitList(newPeople: List<SearchResult.PersonResult>) {
        people = newPeople
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchPersonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(people[position])
    }

    override fun getItemCount() = people.size

    inner class ViewHolder(
        private val binding: ItemSearchPersonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: SearchResult.PersonResult) {
            binding.tvAvatar.text = person.avatar
            binding.tvName.text = person.name
            binding.tvBusinessName.text = person.businessName
            binding.tvRegion.text = person.region
            binding.tvContact.text = "📧 ${person.email} • 📞 ${person.phone}"

            binding.root.setOnClickListener {
                onPersonClick(person)
            }
        }
    }
}