package com.example.pokinfo.adapter.home.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.pokinfo.R

class ImageViewPagerAdapter(
    private val images: List<Pair<String,String>> // name to url
) : RecyclerView.Adapter<ImageViewPagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPokemonPager)
        val textView: TextView = view.findViewById(R.id.tvPokemonnamePager)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pager_home_detail, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.imageView.load(images[position].second) {
            error(R.drawable.image_not_found)
        }
        holder.textView.text = images[position].first
    }

    override fun getItemCount(): Int {
        return images.size
    }
}