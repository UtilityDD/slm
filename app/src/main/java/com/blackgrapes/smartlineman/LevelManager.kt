package com.blackgrapes.smartlineman

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

data class BadgeInfo(val badgeId: String, val levelCount: Int)

object LevelManager {
    private val badgeInfos = mutableListOf<BadgeInfo>()
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            // Dynamically find all badge files (B1.json, B2.json, etc.)
            val badgeFiles = context.assets.list("")?.filter { it.matches(Regex("B\\d+\\.json")) }?.sorted()

            badgeFiles?.forEach { fileName ->
                try {
                    val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(jsonString)
                    val badgeId = jsonObject.getString("badge_id")
                    val levelsArray = jsonObject.getJSONArray("levels")
                    badgeInfos.add(BadgeInfo(badgeId, levelsArray.length()))
                } catch (e: JSONException) {
                    Log.e("LevelManager", "Error parsing $fileName", e)
                }
            }
        } catch (e: IOException) {
            Log.e("LevelManager", "Error reading badge files from assets", e)
        }
        isInitialized = true
    }

    fun getLevelId(level: Int): String {
        var cumulativeLevels = 0
        for ((index, badgeInfo) in badgeInfos.withIndex()) {
            if (level <= cumulativeLevels + badgeInfo.levelCount) {
                val majorLevel = index + 1
                val minorLevel = level - cumulativeLevels
                return "$majorLevel.$minorLevel"
            }
            cumulativeLevels += badgeInfo.levelCount
        }

        // Fallback for levels beyond the defined badges. This should ideally not be reached if all levels are covered by B*.json files.
        Log.w("LevelManager", "Level $level is outside the range of defined badges. Using fallback calculation.")
        return "?.?"
    }
}