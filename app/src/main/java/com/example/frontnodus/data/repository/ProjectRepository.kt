package com.example.frontnodus.data.repository

import com.example.frontnodus.data.local.ProjectDao
import com.example.frontnodus.data.local.ProjectEntity
import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.ui.adapters.ProjectCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(
    private val client: GraphQLClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val projectDao: ProjectDao
) {

    data class TeamMember(val id: String?, val email: String?, val name: String?, val role: String?)

    data class ProjectDetail(
        val id: String,
        val name: String?,
        val description: String?,
        val status: String?,
        val ownerEmail: String?,
        val team: List<TeamMember>,
        val startDate: String?,
        val endDate: String?,
        val estimatedDuration: Int?,
        val locationAddress: String?
    )


    private val myProjectsQuery = """
        query MyProjects {
          myProjects {
            id
            name
            description
            status
            timeline { startDate endDate }
            owner { profile { firstName lastName } }
            team { role }
          }
        }
    """.trimIndent()

    suspend fun getCachedProjects(): List<ProjectCard> = withContext(ioDispatcher) {
        val list = projectDao.getAll()
        return@withContext list.map { e ->
            ProjectCard(
                id = e.id,
                title = e.name,
                startDate = parseAndFormatDate(e.startDate),
                endDate = parseAndFormatDate(e.endDate),
                role = e.role
            )
        }
    }

    private fun parseAndFormatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
        // If it's a numeric timestamp (seconds or milliseconds), convert to a readable date
        try {
            val asLong = cleaned.toLong()
            var millis = asLong
            if (cleaned.length <= 10) millis = asLong * 1000L
            val date = Date(millis)
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return fmt.format(date)
        } catch (nfe: NumberFormatException) {
            // Not a pure number; might be ISO like 2026-02-01T00:00:00Z — extract date portion if possible
            val tIndex = cleaned.indexOf('T')
            if (tIndex > 0) return cleaned.substring(0, tIndex)
            // Otherwise return the raw string (could already be yyyy-MM-dd or other human-readable form)
            return cleaned
        }
    }

    suspend fun fetchAndCacheProjects(): List<ProjectCard> = withContext(ioDispatcher) {
        val resp = client.executeMutation(myProjectsQuery, JSONObject())
        val data = resp.optJSONObject("data") ?: return@withContext emptyList()
        val arr: JSONArray = data.optJSONArray("myProjects") ?: return@withContext emptyList()
        // Use maps keyed by id to deduplicate entries coming from the server
        val resultMap = linkedMapOf<String, ProjectCard>()
        val entityMap = linkedMapOf<String, ProjectEntity>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val id = item.optString("id", i.toString())
            val name = item.optString("name", "Proyecto")
            val description = item.optString("description", null)
            val status = item.optString("status", null)
            val timeline = item.optJSONObject("timeline")
            val rawStart = timeline?.optString("startDate", null)
            val rawEnd = timeline?.optString("endDate", null)
            val startDate = parseAndFormatDate(rawStart)
            val endDate = parseAndFormatDate(rawEnd)
            // owner profile
            val owner = item.optJSONObject("owner")?.optJSONObject("profile")
            val ownerName = listOfNotNull(owner?.optString("firstName"), owner?.optString("lastName")).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
            // role: try first team member role
            val teamArr = item.optJSONArray("team")
            val role = if (teamArr != null && teamArr.length() > 0) {
                teamArr.optJSONObject(0)?.optString("role", null)
            } else null

            // put into maps (last occurrence wins if duplicates present)
            resultMap[id] = ProjectCard(id = id, title = name, startDate = startDate, endDate = endDate, role = role)

            entityMap[id] = ProjectEntity(
                id = id,
                name = name,
                description = description,
                status = status,
                startDate = startDate,
                endDate = endDate,
                ownerName = ownerName,
                role = role
            )
        }

        val result = resultMap.values.toMutableList()
        val entities = entityMap.values.toMutableList()

        // persist and cleanup: if remote returned no entities, clear local table.
        if (entities.isEmpty()) {
            projectDao.clearAll()
        } else {
            // delete local rows whose id is not present in remote response
            val remoteIds = entities.map { it.id }
            try {
                projectDao.deleteNotIn(remoteIds)
            } catch (_: Exception) {
                // ignore if DB doesn't support deleteNotIn for some reason
            }
            projectDao.insertAll(entities)
        }

        return@withContext result
    }

    suspend fun fetchProjectById(projectId: String): ProjectDetail? = withContext(ioDispatcher) {
        val query = """
            query ProjectById(${'$'}id: ID!) {
              project(id: ${'$'}id) {
                id
                name
                description
                status
                owner { id email profile { firstName lastName } }
                team { role user { id email profile { firstName lastName } } }
                timeline { startDate endDate estimatedDuration }
                location { address coordinates }
                createdAt
                updatedAt
              }
            }
        """.trimIndent()

        val vars = JSONObject()
        vars.put("id", projectId)
        val resp = client.executeMutation(query, vars)
        val data = resp.optJSONObject("data") ?: return@withContext null
        val proj = data.optJSONObject("project") ?: return@withContext null

        val id = proj.optString("id", projectId)
        val name = proj.optString("name", null)
        val description = proj.optString("description", null)
        val status = proj.optString("status", null)

        val ownerObj = proj.optJSONObject("owner")
        val ownerEmail = ownerObj?.optString("email", null)

        val teamList = mutableListOf<TeamMember>()
        val teamArr = proj.optJSONArray("team")
        if (teamArr != null) {
            for (i in 0 until teamArr.length()) {
                val t = teamArr.optJSONObject(i) ?: continue
                val role = t.optString("role", null)
                val user = t.optJSONObject("user")
                val uid = user?.optString("id", null)
                val uemail = user?.optString("email", null)
                val profile = user?.optJSONObject("profile")
                val uname = listOfNotNull(profile?.optString("firstName"), profile?.optString("lastName")).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
                teamList.add(TeamMember(id = uid, email = uemail, name = uname, role = role))
            }
        }

        val timeline = proj.optJSONObject("timeline")
        val rawStart = timeline?.optString("startDate", null)
        val rawEnd = timeline?.optString("endDate", null)
        val startDate = parseAndFormatDate(rawStart)
        val endDate = parseAndFormatDate(rawEnd)
        val estimatedDuration = timeline?.optInt("estimatedDuration")

        val locationObj = proj.optJSONObject("location")
        val locationAddress = locationObj?.optString("address", null)

        return@withContext ProjectDetail(
            id = id,
            name = name,
            description = description,
            status = status,
            ownerEmail = ownerEmail,
            team = teamList,
            startDate = startDate,
            endDate = endDate,
            estimatedDuration = if (estimatedDuration == 0) null else estimatedDuration,
            locationAddress = locationAddress
        )
    }
}
