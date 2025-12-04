package com.example.frontnodus.data.repository

import com.example.frontnodus.data.local.TaskDao
import com.example.frontnodus.data.local.TaskEntity
import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.domain.models.Task
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.frontnodus.utils.DateUtils
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(
    private val client: GraphQLClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val taskDao: TaskDao
) {

    private val tasksByProjectQuery = """
        query Tasks(${'$'}projectId: ID!) {
          tasksByProject(projectId: ${'$'}projectId) {
            id
            title
            plannedDate
            status
            priority
            createdBy { profile { firstName lastName } }
            assignedTo { profile { firstName lastName } }
          }
        }
    """.trimIndent()

    suspend fun getTasksByProject(projectId: String): List<Task> = withContext(ioDispatcher) {
        // return cached tasks quickly if available, and trigger a background refresh
        val cached = try { taskDao.getByProject(projectId) } catch (e: Exception) { emptyList() }
        if (cached.isNotEmpty()) {
            // fire-and-forget refresh to update cache in background
            GlobalScope.launch(ioDispatcher) {
                try { refreshAndCache(projectId) } catch (_: Exception) { }
            }
            return@withContext cached.map { e ->
                Task(id = e.id, title = e.title, subtitle = e.subtitle ?: "", date = e.date ?: "", status = e.status ?: "", actionButton = if (e.status.equals("pendiente", true) || e.status.equals("Nuevo", true)) "Gestionar" else "Ver")
            }
        }

        // no cached data - fetch remote synchronously
        val vars = JSONObject().put("projectId", projectId)
        val resp = client.executeMutation(tasksByProjectQuery, vars)
        val data = resp.optJSONObject("data") ?: return@withContext emptyList()
        val arr: JSONArray = data.optJSONArray("tasksByProject") ?: return@withContext emptyList()
        val result = mutableListOf<Task>()
        val entities = mutableListOf<TaskEntity>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            // keep remote id as string
            val idRaw = item.optString("id", "${i + 1}")
            val id = idRaw
            val title = item.optString("title", "Sin título")
            val plannedDateRaw = item.optString("plannedDate", item.optString("createdAt", ""))
            val plannedDate = DateUtils.formatToDate(plannedDateRaw) ?: ""
            val status = item.optString("status", "")
            // build subtitle from assignedTo or createdBy
            val assigned = item.optJSONObject("assignedTo")?.optJSONObject("profile")
            val created = item.optJSONObject("createdBy")?.optJSONObject("profile")
            val subtitle = listOfNotNull(
                assigned?.optString("firstName")?.takeIf { it.isNotBlank() },
                assigned?.optString("lastName")?.takeIf { it.isNotBlank() }
            ).takeIf { it?.isNotEmpty() == true }?.joinToString(" ")
                ?: listOfNotNull(
                    created?.optString("firstName")?.takeIf { it.isNotBlank() },
                    created?.optString("lastName")?.takeIf { it.isNotBlank() }
                ).joinToString(" ")

            val action = if (status.equals("pendiente", true) || status.equals("Nuevo", true)) "Gestionar" else "Ver"

            result.add(
                Task(
                    id = id,
                    title = title,
                    subtitle = subtitle.ifBlank { "Sin asignar" },
                    date = plannedDate,
                    status = status,
                    actionButton = action
                )
            )

            entities.add(TaskEntity(id = id, projectId = projectId, title = title, subtitle = subtitle.ifBlank { "" }, date = plannedDate, status = status))
        }

        // persist and cleanup for this project
        if (entities.isEmpty()) {
            try { taskDao.clearProject(projectId) } catch (_: Exception) { }
        } else {
            val ids = entities.map { it.id }
            try { taskDao.deleteNotInForProject(projectId, ids) } catch (_: Exception) { }
            taskDao.insertAll(entities)
        }

        return@withContext result
    }

        suspend fun getTaskById(taskId: String): JSONObject? = withContext(ioDispatcher) {
                val query = """
                        query GetTaskFull(${ '$' }id: ID!) {
                            task(id: ${ '$' }id) {
                                id
                                title
                                description
                                status
                                priority
                                plannedDate
                                actualDate
                                createdAt
                                updatedAt
                                project { id name }
                                assignedTo { id email profile { firstName lastName phone avatar } }
                                createdBy { id email }
                                checklist { title completed }
                                dependencies { id title status }
                                ppcWeek
                                attachments
                                comments { id text createdAt commenter { id email profile { firstName lastName } } }
                            }
                        }
                """.trimIndent()

                val vars = JSONObject().put("id", taskId)
                val resp = client.executeMutation(query, vars)
                val data = resp.optJSONObject("data") ?: return@withContext null
                return@withContext data.optJSONObject("task")
        }

        suspend fun updateChecklist(taskId: String, checklist: List<Pair<String, Boolean>>): JSONObject? = withContext(ioDispatcher) {
                val mutation = """
                        mutation UpdateTask(${'$'}id: ID!, ${'$'}input: UpdateTaskInput!) {
                            updateTask(id: ${'$'}id, input: ${'$'}input) {
                                id
                                checklist { title completed }
                            }
                        }
                """.trimIndent()

                val checklistArr = org.json.JSONArray()
                for (c in checklist) {
                        val obj = org.json.JSONObject()
                        obj.put("title", c.first)
                        obj.put("completed", c.second)
                        checklistArr.put(obj)
                }

                val input = JSONObject()
                input.put("checklist", checklistArr)

                val vars = JSONObject().put("id", taskId).put("input", input)
                val resp = client.executeMutation(mutation, vars)
                return@withContext resp.optJSONObject("data")?.optJSONObject("updateTask")
        }

        suspend fun addTaskComment(taskId: String, text: String): JSONObject? = withContext(ioDispatcher) {
                val mutation = """
                        mutation AddTaskComment(${'$'}taskId: ID!, ${'$'}text: String!) {
                            addTaskComment(taskId: ${'$'}taskId, text: ${'$'}text) {
                                        id
                                        comments { id text createdAt commenter { id email profile { firstName lastName } } }
                            }
                        }
                """.trimIndent()

                val vars = JSONObject().put("taskId", taskId).put("text", text)
                val resp = client.executeMutation(mutation, vars)
                // surface GraphQL errors
                val errs = resp.optJSONArray("errors")
                if (errs != null && errs.length() > 0) {
                    val msg = errs.optJSONObject(0)?.optString("message") ?: "GraphQL error"
                    throw Exception(msg)
                }
                return@withContext resp.optJSONObject("data")?.optJSONObject("addTaskComment")
        }

            suspend fun editTaskComment(taskId: String, commentId: String, text: String): JSONObject? = withContext(ioDispatcher) {
                val mutation = """
                    mutation EditTaskComment(${ '$' }taskId: ID!, ${ '$' }commentId: ID!, ${ '$' }text: String!) {
                        editTaskComment(taskId: ${ '$' }taskId, commentId: ${ '$' }commentId, text: ${ '$' }text) {
                        id
                        comments { id text createdAt commenter { id email profile { firstName lastName } } }
                        }
                    }
                """.trimIndent()

                val vars = JSONObject().put("taskId", taskId).put("commentId", commentId).put("text", text)
                val resp = client.executeMutation(mutation, vars)
                val errs = resp.optJSONArray("errors")
                if (errs != null && errs.length() > 0) {
                    val msg = errs.optJSONObject(0)?.optString("message") ?: "GraphQL error"
                    throw Exception(msg)
                }
                return@withContext resp.optJSONObject("data")?.optJSONObject("editTaskComment")
            }

            suspend fun deleteTaskComment(taskId: String, commentId: String): JSONObject? = withContext(ioDispatcher) {
                val mutation = """
                    mutation DeleteTaskComment(${ '$' }taskId: ID!, ${ '$' }commentId: ID!) {
                        deleteTaskComment(taskId: ${ '$' }taskId, commentId: ${ '$' }commentId) {
                        id
                        comments { id text createdAt commenter { id email profile { firstName lastName } } }
                        }
                    }
                """.trimIndent()

                val vars = JSONObject().put("taskId", taskId).put("commentId", commentId)
                val resp = client.executeMutation(mutation, vars)
                val errs = resp.optJSONArray("errors")
                if (errs != null && errs.length() > 0) {
                    val msg = errs.optJSONObject(0)?.optString("message") ?: "GraphQL error"
                    throw Exception(msg)
                }
                return@withContext resp.optJSONObject("data")?.optJSONObject("deleteTaskComment")
            }

            // comment status / resolved handling removed: only add/edit/delete supported by server now

    private suspend fun refreshAndCache(projectId: String) {
        val vars = JSONObject().put("projectId", projectId)
        val resp = client.executeMutation(tasksByProjectQuery, vars)
        val data = resp.optJSONObject("data") ?: return
        val arr: JSONArray = data.optJSONArray("tasksByProject") ?: return
        val entities = mutableListOf<TaskEntity>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val idRaw = item.optString("id", "${i + 1}")
            val id = idRaw
            val title = item.optString("title", "Sin título")
            val plannedDateRaw = item.optString("plannedDate", item.optString("createdAt", ""))
            val plannedDate = DateUtils.formatToDate(plannedDateRaw) ?: ""
            val status = item.optString("status", "")
            val assigned = item.optJSONObject("assignedTo")?.optJSONObject("profile")
            val created = item.optJSONObject("createdBy")?.optJSONObject("profile")
            val subtitle = listOfNotNull(
                assigned?.optString("firstName")?.takeIf { it.isNotBlank() },
                assigned?.optString("lastName")?.takeIf { it.isNotBlank() }
            ).takeIf { it?.isNotEmpty() == true }?.joinToString(" ")
                ?: listOfNotNull(
                    created?.optString("firstName")?.takeIf { it.isNotBlank() },
                    created?.optString("lastName")?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
            entities.add(TaskEntity(id = id, projectId = projectId, title = title, subtitle = subtitle.ifBlank { "" }, date = plannedDate, status = status))
        }

        if (entities.isEmpty()) {
            try { taskDao.clearProject(projectId) } catch (_: Exception) { }
        } else {
            val ids = entities.map { it.id }
            try { taskDao.deleteNotInForProject(projectId, ids) } catch (_: Exception) { }
            taskDao.insertAll(entities)
        }
    }
}
