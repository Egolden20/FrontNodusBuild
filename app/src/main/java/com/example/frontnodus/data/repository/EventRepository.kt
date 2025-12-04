package com.example.frontnodus.data.repository

import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.domain.models.Event
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class EventRepository(
    private val client: GraphQLClient,
    private val ioDispatcher: CoroutineDispatcher
) {

        private val eventsByProjectQuery = """
                query EventsByProject(${ '$' }projectId: ID!) {
                    eventsByProject(projectId: ${ '$' }projectId) {
                        id
                        title
                        description
                        date
                        status
                        project { id name }
                    }
                }
        """.trimIndent()

        private val eventsForUserQuery = """
                query EventsForUser {
                    eventsForUser {
                        id
                        title
                        description
                        date
                        status
                        project { id name }
                    }
                }
        """.trimIndent()

    suspend fun getEventsByProject(projectId: String): List<Event> = withContext(ioDispatcher) {
        val vars = JSONObject().put("projectId", projectId)
        val resp = client.executeMutation(eventsByProjectQuery, vars)
        val data = resp.optJSONObject("data") ?: return@withContext emptyList()
        val arr: JSONArray = data.optJSONArray("eventsByProject") ?: return@withContext emptyList()
        val result = mutableListOf<Event>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val idRaw = item.optString("id", "${i + 1}")
            val id = try { idRaw.toInt() } catch (_: Exception) { i + 1 }
            val title = item.optString("title", "Sin título")
            val description = item.optString("description", "")
            val dateRaw = item.optString("date", "")

            // derive display fields
            val dateOnly = formatDateOnly(dateRaw)
            val day = if (dateOnly.length >= 10) dateOnly.substring(8, 10) else ""
            val monthYear = formatMonthShort(dateOnly)
            val time = formatTime(dateRaw)

            val projectObj = item.optJSONObject("project")
            val projectName = projectObj?.optString("name", "") ?: ""
            val location = if (projectName.isNotBlank()) projectName else description

            val status = item.optString("status", "")

            result.add(
                Event(
                    id = id,
                    title = title,
                    description = description,
                    location = location,
                    date = dateOnly,
                    time = time,
                    day = day,
                    monthYear = monthYear,
                    status = status
                )
            )
        }

        return@withContext result
    }

    suspend fun getEventsForUser(): List<Event> = withContext(ioDispatcher) {
        val resp = client.executeMutation(eventsForUserQuery, null)
        val data = resp.optJSONObject("data") ?: return@withContext emptyList()
        val arr: JSONArray = data.optJSONArray("eventsForUser") ?: return@withContext emptyList()
        val result = mutableListOf<Event>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val idRaw = item.optString("id", "${i + 1}")
            val id = try { idRaw.toInt() } catch (_: Exception) { i + 1 }
            val title = item.optString("title", "Sin título")
            val description = item.optString("description", "")
            val dateRaw = item.optString("date", "")

            val dateOnly = formatDateOnly(dateRaw)
            val day = if (dateOnly.length >= 10) dateOnly.substring(8, 10) else ""
            val monthYear = formatMonthShort(dateOnly)
            val time = formatTime(dateRaw)

            val creator = item.optJSONObject("createdBy")?.optJSONObject("profile")
            val createdByName = listOfNotNull(creator?.optString("firstName"), creator?.optString("lastName")).joinToString(" ").ifBlank { "" }
            val location = if (createdByName.isNotBlank()) createdByName else description

            val status = item.optString("status", "")

            result.add(
                Event(
                    id = id,
                    title = title,
                    description = description,
                    location = location,
                    date = dateOnly,
                    time = time,
                    day = day,
                    monthYear = monthYear,
                    status = status
                )
            )
        }

        return@withContext result
    }

    private fun formatDateOnly(raw: String): String {
        if (raw.isBlank()) return ""
        // attempt ISO parse
        val tIndex = raw.indexOf('T')
        return if (tIndex > 0) raw.substring(0, tIndex) else raw
    }

    private fun formatTime(raw: String): String {
        if (raw.isBlank()) return ""
        val tIndex = raw.indexOf('T')
        if (tIndex > 0 && raw.length >= tIndex + 6) {
            // HH:mm
            return try {
                raw.substring(tIndex + 1, tIndex + 6)
            } catch (_: Exception) { "" }
        }
        return ""
    }

    private fun formatMonthShort(dateOnly: String): String {
        if (dateOnly.isBlank()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(dateOnly)
            val out = SimpleDateFormat("MMM", Locale.getDefault())
            out.format(d ?: Date())
        } catch (_: Exception) { "" }
    }
}
