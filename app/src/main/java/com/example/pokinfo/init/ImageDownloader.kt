package com.example.pokinfo.init

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import java.io.File

class ImageDownloader(private val context: Context) {

    suspend fun downloadImagesAndGetList(nameUrlList: List<Pair<String, String>>): List<Pair<String, String>> {
        val nameAndImagePathList: MutableList<Pair<String, String>> = mutableListOf()
        nameUrlList.forEachIndexed { index, pair ->
            // pair.second = imageURL / pair.first = name
            val bitmap = loadImage(context, pair.second)
            val path = saveImageAndGetPath(index+1, bitmap)
            // adds a pair of name of pokemon and path of saved image corresponding to that pokemon
            nameAndImagePathList.add(Pair(pair.first, path))
        }
        return nameAndImagePathList
    }

    private suspend fun loadImage(context: Context, url: String): Bitmap? {
        // Load Image with Coil
        return Coil.imageLoader(context).execute(
            ImageRequest.Builder(context = context)
                .data(url)
                .build()
        ).drawable?.toBitmap()
    }

    private fun saveImageAndGetPath(index: Int, bitmap: Bitmap?): String {
        // Save Image internally
        val imageFileName = "pokemon_$index.png"
        val imageFile = File(context.filesDir, imageFileName)
        // Compress image into File
        imageFile.outputStream().use { outputstream ->
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputstream)
        }
        return imageFile.absolutePath
    }
}