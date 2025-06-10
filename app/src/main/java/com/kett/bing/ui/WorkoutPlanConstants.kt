package com.kett.bing.ui

import android.content.Context
import com.google.gson.Gson

object WorkoutPlanConstants {
    val setsMap = mutableMapOf(
        "Beginner" to listOf(4, 3, 3, 2, 2),
        "Medium" to listOf(8, 6, 6, 4, 4),
        "Advanced" to listOf(12, 10, 8, 8, 6)
    )

    val restTimeMap = mutableMapOf(
        "Beginner" to "01:00",
        "Medium" to "00:45",
        "Advanced" to "00:30"
    )

    var individualPlanMap: MutableMap<String, PlanData> = mutableMapOf()

    fun saveIndividualPlans(context: Context) {
        val prefs = context.getSharedPreferences("PlanPrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(individualPlanMap)
        prefs.edit().putString("individualPlanMap", json).apply()
    }

    fun loadIndividualPlans(context: Context) {
        val prefs = context.getSharedPreferences("PlanPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("individualPlanMap", null)
        if (!json.isNullOrEmpty()) {
            val type = object : com.google.common.reflect.TypeToken<MutableMap<String, PlanData>>() {}.type
            individualPlanMap = Gson().fromJson(json, type)
        }
    }
}
