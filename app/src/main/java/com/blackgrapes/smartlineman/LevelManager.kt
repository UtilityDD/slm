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

        // Assuming badge files are named B1.json, B2.json, etc.
        for (i in 1..10) { // Check for up to 10 badge files
            val fileName = "B$i.json"
            try {
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val jsonObject = org.json.JSONObject(jsonString)
                val badgeId = jsonObject.getString("badge_id")
                val levelsArray = jsonObject.getJSONArray("levels")
                badgeInfos.add(BadgeInfo(badgeId, levelsArray.length()))
            } catch (e: IOException) {
                // This is expected when we run out of badge files to find (e.g., B7.json doesn't exist)
                break // Stop looking for more files
            } catch (e: JSONException) {
                Log.e("LevelManager", "Error parsing $fileName", e)
                break
            }
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

        // Fallback for levels beyond the defined badges (or if initialization failed)
        Log.w("LevelManager", "Level $level is outside the range of defined badges. Using fallback calculation.")
        val majorLevel = ((level - 1) / 10) + 1
        val minorLevel = ((level - 1) % 10) + 1
        return "$majorLevel.$minorLevel"
    }
}