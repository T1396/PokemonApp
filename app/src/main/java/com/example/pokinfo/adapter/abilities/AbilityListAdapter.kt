package com.example.pokinfo.adapter.abilities

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.databinding.ItemListAbilitiesBinding


class AbilityListAdapter(private val onItemClicked: (Int) -> Unit) :
    RecyclerView.Adapter<AbilityListAdapter.ItemViewHolder>() {
    private var dataset: List<AbilityInfo> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<AbilityInfo>, callback: (() -> Unit)? = null) {
        dataset = list
        notifyDataSetChanged()
        callback?.invoke()
    }

    inner class ItemViewHolder(val binding: ItemListAbilitiesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListAbilitiesBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.tvAbilityName.text = item.name
        holder.binding.tvGen.text = item.generationNr.toString()
        val nr = position+1
        holder.binding.tvNr.text = nr.toString()

        holder.binding.cvAbility.setOnClickListener {
            val id = item.abilityId
            // callback
            onItemClicked(id)
        }

    }

}

data class AbilityInfo(
    val abilityId: Int,
    val name: String,
    val generationNr: Int
)


