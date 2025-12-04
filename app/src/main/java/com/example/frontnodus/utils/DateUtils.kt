package com.example.frontnodus.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    // Normalize numeric timestamps (seconds or ms) and ISO strings to yyyy-MM-dd
    fun formatToDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
        try {
            val asLong = cleaned.toLong()
            var millis = asLong
            if (cleaned.length <= 10) millis = asLong * 1000L
            val date = Date(millis)
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return fmt.format(date)
        } catch (nfe: NumberFormatException) {
            val tIndex = cleaned.indexOf('T')
            if (tIndex > 0) return cleaned.substring(0, tIndex)
            return cleaned
        }
    }
}
