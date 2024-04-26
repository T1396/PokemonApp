package com.example.pokinfo.adapter.attacks.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.databinding.ItemListAttackdescriptionBinding

class DescriptionAdapter(private val onLangButtonClicked: () -> Unit) :
    RecyclerView.Adapter<DescriptionAdapter.ItemViewHolder>() {
    private var dataset: List<AttackDescription> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<AttackDescription>) {
        dataset = list
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemListAttackdescriptionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListAttackdescriptionBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.tvAttackDescription.text = item.text
        holder.binding.btnGameVersion.text = item.gameVersion
        holder.binding.btnLanguage.text = item.languageString

        holder.binding.btnLanguage.setOnClickListener {
            onLangButtonClicked()
        }
    }

}


data class AttackDescription(
    val languageId: Int,
    val text: String = "No data found",
    val gameVersion: String = "",
    val languageString: String
)
