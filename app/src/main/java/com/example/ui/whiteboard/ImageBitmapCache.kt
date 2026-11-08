package com.example.ui.whiteboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object ImageBitmapCache {
    private const val TAG = "ImageBitmapCache"
    private val memoryCache = LruCache<String, ImageBitmap>(60)

    fun getImageBitmap(context: Context, uriString: String): ImageBitmap? {
        memoryCache.get(uriString)?.let { return it }

        try {
            val uri = Uri.parse(uriString)
            val bmp: Bitmap? = when (uri.scheme) {
                "content", "file" -> {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                "http", "https" -> {
                    // Check if already cached as file
                    val hash = uriString.hashCode().toString()
                    val cachedFile = File(context.cacheDir, "web_$hash.jpg")
                    if (cachedFile.exists()) {
                        BitmapFactory.decodeFile(cachedFile.absolutePath)
                    } else {
                        null
                    }
                }
                else -> {
                    val file = File(uriString)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }
            }

            if (bmp != null) {
                val imageBitmap = bmp.asImageBitmap()
                memoryCache.put(uriString, imageBitmap)
                return imageBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap for $uriString", e)
        }
        return null
    }

    fun cacheBitmap(uriString: String, bmp: Bitmap) {
        try {
            memoryCache.put(uriString, bmp.asImageBitmap())
        } catch (e: Exception) {
            Log.e(TAG, "Error caching bitmap", e)
        }
    }
}
