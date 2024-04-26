package com.example.pokinfo.data.util

import android.widget.ImageView
import coil.load
import com.example.pokinfo.R

object ImageAltLoader {
    /**
     * Loads images into the imageView, and if there are any errors, loads alternative ones
     */
    fun loadAnyImage(
        imageView: ImageView,
        url: String,
        altUrl: String,
        officialUrl: String
    ) {
        // load image (or alternative ones if error)
        imageView.load(url) {
            listener(onError = { _, _ ->
                imageView.load(altUrl) {
                    listener(onError = { _, _ ->
                        imageView.load(officialUrl) {
                            error(R.drawable.pokemon_type_icon_unknown)
                        }
                    })
                }
            })
        }
    }
}