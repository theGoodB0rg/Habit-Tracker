package com.habittracker.data.database.converter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.Duration

/**
 * Centralized JSON helpers for timing-related collections to keep mappings concise.
 */
object JsonAdapters {
    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    fun toJson(value: Any?): String = gson.toJson(value)

    fun <T> fromJson(json: String?, type: Type): T? =
        if (json.isNullOrBlank()) null else gson.fromJson(json, type)

    // Helper adapters for common types used in timing models
    fun durationListToJson(list: List<Duration>): String =
        toJson(list.map { it.toMillis() })

    fun jsonToDurationList(json: String?): List<Duration> =
        fromJson<List<Long>>(json, object : TypeToken<List<Long>>() {}.type)?.map { Duration.ofMillis(it) } ?: emptyList()
}
