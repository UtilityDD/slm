package com.blackgrapes.smartlineman.data

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object SyncRepository {

    private const val GITHUB_API_URL = "https://api.github.com/repos/UtilityDD/smartlineman/contents/app/src/main/assets"
    private const val TAG = "SyncRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun syncFiles(context: Context, onProgress: (String) -> Unit, onComplete: (Boolean, String) -> Unit) {
        Thread {
            try {
                onProgress("Fetching file list from GitHub...")
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    onComplete(false, "Failed to fetch file list: ${response.code}")
                    return@Thread
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    onComplete(false, "Empty response from GitHub")
                    return@Thread
                }

                val jsonArray = JSONArray(responseBody)
                val filesToDownload = mutableListOf<Pair<String, String>>()

                for (i in 0 until jsonArray.length()) {
                    val fileObject = jsonArray.getJSONObject(i)
                    val name = fileObject.getString("name")
                    val downloadUrl = fileObject.getString("download_url")

                    if (name.endsWith(".json")) {
                        filesToDownload.add(name to downloadUrl)
                    }
                }

                if (filesToDownload.isEmpty()) {
                    onComplete(true, "No JSON files found to update.")
                    return@Thread
                }

                var successCount = 0
                for ((index, fileData) in filesToDownload.withIndex()) {
                    val (fileName, url) = fileData
                    onProgress("Downloading $fileName (${index + 1}/${filesToDownload.size})...")

                    if (downloadFile(context, url, fileName)) {
                        successCount++
                    } else {
                        Log.e(TAG, "Failed to download $fileName")
                    }
                }

                onComplete(true, "Successfully updated $successCount files.")

            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                onComplete(false, "Sync failed: ${e.message}")
            }
        }.start()
    }

    private fun downloadFile(context: Context, url: String, fileName: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return false

            val file = File(context.filesDir, fileName)
            val bytes = response.body?.bytes() ?: return false
            file.writeBytes(bytes)
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading file: $fileName", e)
            false
        }
    }
}
