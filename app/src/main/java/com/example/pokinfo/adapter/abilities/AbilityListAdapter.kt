package com.example.pokinfo.adapter.abilities

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.data.models.database.pokemon.PkAbilityInfo
import com.example.pokinfo.data.util.AbilityDiffCallback
import com.example.pokinfo.databinding.ItemListAbilitiesBinding


class AbilityListAdapter(private val onItemClicked: (Int) -> Unit) :
    ListAdapter<AbilityListAdapter.AbilityInfo, AbilityListAdapter.ItemViewHolder>(AbilityDiffCallback()) {
    data class AbilityInfo(
        val abilityId: Int,
        val name: String,
        val generationNr: Int
    )
    inner class ItemViewHolder(val binding: ItemListAbilitiesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListAbilitiesBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = currentList[position]
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




