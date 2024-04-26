package com.example.pokinfo.adapter.home.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.pokinfo.databinding.ItemListPictureBinding

class ImagesAdapter() :
    RecyclerView.Adapter<ImagesAdapter.ItemViewHolder>() {
    private var dataset: List<Pair<String, String>> = emptyList()
    private var displayName: String = ""

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<Pair<String, String>>, name: String) {
        dataset = list
        displayName = name
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(val binding: ItemListPictureBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListPictureBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.imageView2.load(item.second)
        holder.binding.tvVersion.text = item.first
        holder.binding.tvType.text = displayName

    }

}


