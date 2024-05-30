package com.example.pokinfo.data.util

import android.util.Log
import android.widget.ImageView
import coil.clear
import coil.dispose
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
            crossfade(true)
            listener(onError = { _, _ ->
                imageView.load(altUrl) {
                    crossfade(true)
                    listener(onError = { _, _ ->
                        imageView.load(officialUrl) {
                            crossfade(true)
                            error(R.drawable.pokemon_type_icon_unknown)
                        }
                    })
                }
            })
        }
    }
}
