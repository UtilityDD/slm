package com.blackgrapes.smartlineman.util

import android.content.Context
import java.io.File
import java.io.IOException

object JsonHelper {

    fun loadJSON(context: Context, fileName: String): String? {
        // 1. Check if the file exists in internal storage (synced version)
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                return file.readText()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // 2. Fallback to assets folder (bundled version)
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
