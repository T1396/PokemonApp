package com.example.pokinfo.adapter.abilities

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pokinfo.R
import com.example.pokinfo.adapter.home.detail.AbilityEffectText
import com.example.pokinfo.databinding.ItemListAbilityBinding

class AbilityDetailAdapter() :
    RecyclerView.Adapter<AbilityDetailAdapter.ItemViewHolder>() {
    private var dataset: List<AbilityEffectText> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<AbilityEffectText>) {
        dataset = list
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemListAbilityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemListAbilityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.tvAbilit.text = item.name
        holder.binding.tvEffectLong.text = item.textLong
        holder.binding.tvIndicatorNr.text =
            holder.itemView.context.getString(
                R.string.common_nr_indicator,
                (position + 1).toString(),
                itemCount.toString()
            )
    }
}

