package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ItemLanguageBinding
import com.syedsaifhossain.g_chatapplication.models.LanguageItem

class LanguageAdapter(
    private val languages: List<LanguageItem>,
    private var selectedCode: String,
    private val onItemClick: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageItem) {
            binding.languageName.text = item.name
            binding.radioButton.isChecked = item.code == selectedCode

            binding.root.setOnClickListener {
                if (selectedCode != item.code) {
                    selectedCode = item.code
                    notifyDataSetChanged()
                    onItemClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size
}