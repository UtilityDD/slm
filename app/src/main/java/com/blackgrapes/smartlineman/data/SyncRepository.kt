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

    // Using raw.githubusercontent.com for direct file access without authentication
    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/UtilityDD/slm/refs/heads/main"
    private const val TAG = "SyncRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // List of JSON files to sync from the repository
    private val jsonFiles = listOf(
        "B1.json",
        "B2.json",
        "B3.json",
        "chapter_1_1.json",
        "chapter_1_2.json",
        "chapter_1_3.json",
        "chapter_1_4.json",
        "chapter_1_5.json",
        "chapter_2_1.json",
        "chapter_2_2.json",
        "chapter_2_3.json",
        "chapter_2_4.json",
        "chapter_2_5.json",
        "chapter_3_1.json",
        "chapter_3_2.json",
        "chapter_3_3.json",
        "chapter_3_4.json",
        "chapter_3_5.json",
        "marketplace.json",
        "menu.json"
    )
    
    // User-friendly display names for files
    private val fileDisplayNames = mapOf(
        "B1.json" to "Book 1 - Resources",
        "B2.json" to "Book 2 - Resources",
        "B3.json" to "Book 3 - Resources",
        "chapter_1_1.json" to "Chapter 1.1 - Safety",
        "chapter_1_2.json" to "Chapter 1.2 - Tools",
        "chapter_1_3.json" to "Chapter 1.3 - Materials",
        "chapter_1_4.json" to "Chapter 1.4 - Basics",
        "chapter_1_5.json" to "Chapter 1.5 - Installation",
        "chapter_2_1.json" to "Chapter 2.1 - Distribution",
        "chapter_2_2.json" to "Chapter 2.2 - Transformers",
        "chapter_2_3.json" to "Chapter 2.3 - Protection",
        "chapter_2_4.json" to "Chapter 2.4 - Metering",
        "chapter_2_5.json" to "Chapter 2.5 - Maintenance",
        "chapter_3_1.json" to "Chapter 3.1 - Troubleshooting",
        "chapter_3_2.json" to "Chapter 3.2 - Testing",
        "chapter_3_3.json" to "Chapter 3.3 - Repairs",
        "chapter_3_4.json" to "Chapter 3.4 - Advanced",
        "chapter_3_5.json" to "Chapter 3.5 - Best Practices",
        "marketplace.json" to "Equipment Catalog",
        "menu.json" to "Main Menu"
    )
    
    private fun getDisplayName(fileName: String): String {
        return fileDisplayNames[fileName] ?: fileName.replace("_", " ").replace(".json", "")
    }

    fun syncFiles(context: Context, onProgress: (String) -> Unit, onComplete: (Boolean, String) -> Unit) {
        Thread {
            try {
                onProgress("Starting update from GitHub...")
                
                val executor = java.util.concurrent.Executors.newFixedThreadPool(5) // Download 5 files at a time
                val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
                val successCount = java.util.concurrent.atomic.AtomicInteger(0)
                val totalFiles = jsonFiles.size
                
                jsonFiles.forEach { fileName ->
                    executor.submit {
                        try {
                            val url = "$GITHUB_RAW_URL/$fileName"
                            val displayName = getDisplayName(fileName)
                            val currentCount = completedCount.incrementAndGet()
                            
                            onProgress("Updating $displayName ($currentCount/$totalFiles)...")
                            
                            if (downloadFile(context, url, fileName)) {
                                successCount.incrementAndGet()
                            } else {
                                Log.e(TAG, "Failed to download $fileName")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error downloading $fileName", e)
                        }
                    }
                }
                
                // Shutdown executor and wait for all downloads to complete
                executor.shutdown()
                executor.awaitTermination(5, TimeUnit.MINUTES)
                
                val finalSuccessCount = successCount.get()
                if (finalSuccessCount > 0) {
                    onComplete(true, "Successfully updated $finalSuccessCount of $totalFiles files.")
                } else {
                    onComplete(false, "Failed to download any files.")
                }

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
