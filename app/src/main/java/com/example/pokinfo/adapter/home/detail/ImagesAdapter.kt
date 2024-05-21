package com.example.pokinfo.adapter.home.detail

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.os.Build.VERSION.SDK_INT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import com.caverock.androidsvg.SVG
import com.example.pokinfo.data.util.ImagesDiffCallback
import com.example.pokinfo.databinding.ItemListPictureBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ImagesAdapter(private val lifecycleScope: CoroutineScope) :
    ListAdapter<Pair<String, String>, ImagesAdapter.ItemViewHolder>(ImagesDiffCallback()) {
    private var categoryName: String = ""
    private lateinit var imageLoader: ImageLoader


    fun setCategoryName(name: String) {
        categoryName = name
    }
    inner class ItemViewHolder(val binding: ItemListPictureBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        if (!::imageLoader.isInitialized) {
            createImageLoader(parent.context)
        }

        val binding = ItemListPictureBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    private fun createImageLoader(context: Context) {
        imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    override fun getItemCount(): Int {
        // Return half the size because we are displaying two items per row
        return (currentList.size + 1) / 2
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val leftItem = currentList[position * 2]
        var rightItem: Pair<String, String>? = null
        if (position * 2 + 1 < currentList.size) {
            rightItem = currentList[position * 2 + 1]
        }
        holder.binding.tvGameVersion.text = categoryName
        holder.binding.tvTypeOfViewLeft.text = leftItem.first
        if (categoryName == "Dream World") {
            loadAndPostSvgDrawables(leftItem, holder, rightItem)
        } else {
            holder.binding.ivLeft.load(leftItem.second, imageLoader)
            showRightSlotIfAvailable(rightItem, holder)
        }
    }

    private fun showRightSlotIfAvailable(
        rightItem: Pair<String, String>?,
        holder: ItemViewHolder
    ) {
        rightItem?.let { item ->
            holder.binding.ivRight.load(item.second, imageLoader)
            showRightSlot(holder, item)
        } ?: kotlin.run {
            adjustLayoutForSingleImage(binding = holder.binding)
        }
    }

    /** Make the right image Invisible and constraint the left one central */
    private fun adjustLayoutForSingleImage(
        binding: ItemListPictureBinding
    ) {
        val leftImage = binding.ivLeft
        val rightImage = binding.ivRight

        val layoutParams = leftImage.layoutParams as ConstraintLayout.LayoutParams
        rightImage.visibility = View.GONE
        binding.tvTypeOfViewRight.visibility = View.GONE
        // center left image is no right one is available
        layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.width = 0
        leftImage.layoutParams = layoutParams
    }

    private fun ImagesAdapter.loadAndPostSvgDrawables(
        leftItem: Pair<String, String>,
        holder: ItemViewHolder,
        rightItem: Pair<String, String>?
    ) {
        lifecycleScope.launch {
            val drawableLeft = loadSvgAsync(leftItem.second)
            holder.binding.ivLeft.post {
                holder.binding.ivLeft.setImageDrawable(drawableLeft)
            }
            rightItem?.let { rightItem ->
                val drawableRight = loadSvgAsync(rightItem.second)
                holder.binding.ivRight.post {
                    holder.binding.ivRight.setImageDrawable(drawableRight)
                }
                showRightSlot(holder, rightItem)
            } ?: kotlin.run {
                adjustLayoutForSingleImage(holder.binding)
            }
        }
    }

    private fun showRightSlot(
        holder: ItemViewHolder,
        rightItem: Pair<String, String>?,
    ) {
        holder.binding.tvTypeOfViewRight.text = rightItem?.first
        holder.binding.ivRight.visibility = View.VISIBLE
        holder.binding.tvTypeOfViewRight.visibility =  View.VISIBLE
    }

    private suspend fun loadSvgAsync(urlString: String): PictureDrawable? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val svg = SVG.getFromInputStream(url.openStream())
                val picture = svg.renderToPicture()
                PictureDrawable(picture)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}


