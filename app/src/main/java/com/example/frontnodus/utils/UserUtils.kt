package com.example.frontnodus.utils

import org.json.JSONObject

object UserUtils {
    fun extractDisplayName(user: JSONObject?): String {
        if (user == null) return "Usuario"
        try {
            val profile = user.optJSONObject("profile")
            if (profile != null) {
                val first = profile.optString("firstName", "").trim()
                val last = profile.optString("lastName", "").trim()
                val combined = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
                if (combined.isNotEmpty()) return combined
            }
            val name = user.optString("name", "").trim()
            if (name.isNotEmpty()) return name
            val email = user.optString("email", "").trim()
            if (email.isNotEmpty()) return email
        } catch (e: Exception) {
            // ignore and fallback
        }
        return "Usuario"
    }
}
