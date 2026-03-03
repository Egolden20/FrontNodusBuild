package com.example.frontnodus.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontnodus.BuildConfig
import com.example.frontnodus.R
import com.example.frontnodus.data.repository.ProjectRepository
import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.data.storage.TokenStore
import com.example.frontnodus.models.ChatParticipant
import com.example.frontnodus.models.ConnectionStatus
import com.example.frontnodus.utils.SocketManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject

class ChatListActivity : AppCompatActivity() {

    private val TAG = "ChatListActivity"
    private val projectRepository: ProjectRepository by inject()
    private val tokenStore: TokenStore by inject()
    
    private lateinit var etSearch: android.widget.EditText
    private lateinit var spProjects: Spinner
    private lateinit var rvProjectUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var graphQLClient: GraphQLClient
    
    private var currentChatId: String? = null
    private var currentProjectId: String? = null
    private var allParticipants: List<ChatParticipant> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        etSearch = findViewById(R.id.etSearch)
        spProjects = findViewById(R.id.spProjects)
        rvProjectUsers = findViewById(R.id.rvProjectUsers)

        graphQLClient = GraphQLClient(BuildConfig.BACKEND_BASE_URL) { tokenStore.getToken() }

        rvProjectUsers.layoutManager = LinearLayoutManager(this)
        usersAdapter = UsersAdapter(emptyList()) { 
            // Open project chat when clicking on any participant
            openProjectChat()
        }
        rvProjectUsers.adapter = usersAdapter

        // Search functionality
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterParticipants(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Socket is already initialized globally in App.kt
        // Just listen for status changes
        lifecycleScope.launch {
            try {
                // Ensure socket is connected
                if (!SocketManager.isConnected()) {
                    SocketManager.connect()
                }
                
                // Listen for participants status response
                SocketManager.onParticipantsStatus { chatId, statuses ->
                    runOnUiThread {
                        Log.d(TAG, "Received statuses for chat $chatId: ${statuses.size} users")
                        statuses.forEach { (userId, status) ->
                            usersAdapter.updateUserStatus(userId, status)
                            // Also update in allParticipants list
                            val index = allParticipants.indexOfFirst { it.userId == userId }
                            if (index != -1) {
                                val connectionStatus = when(status.lowercase()) {
                                    "online" -> ConnectionStatus.ONLINE
                                    "away" -> ConnectionStatus.AWAY
                                    else -> ConnectionStatus.OFFLINE
                                }
                                val updatedList = allParticipants.toMutableList()
                                updatedList[index] = allParticipants[index].copy(
                                    status = connectionStatus,
                                    lastSeen = if (connectionStatus == ConnectionStatus.OFFLINE) 
                                        System.currentTimeMillis().toString() else null
                                )
                                allParticipants = updatedList
                            }
                        }
                    }
                }
                
                // Listen for user status changes
                SocketManager.onUserStatusChanged { userId, status ->
                    runOnUiThread {
                        Log.d(TAG, "Status update received: userId=$userId, status=$status")
                        usersAdapter.updateUserStatus(userId, status)
                        // Also update in allParticipants list
                        val index = allParticipants.indexOfFirst { it.userId == userId }
                        if (index != -1) {
                            val connectionStatus = when(status.lowercase()) {
                                "online" -> ConnectionStatus.ONLINE
                                "away" -> ConnectionStatus.AWAY
                                else -> ConnectionStatus.OFFLINE
                            }
                            val updatedList = allParticipants.toMutableList()
                            updatedList[index] = allParticipants[index].copy(
                                status = connectionStatus,
                                lastSeen = if (connectionStatus == ConnectionStatus.OFFLINE) 
                                    System.currentTimeMillis().toString() else null
                            )
                            allParticipants = updatedList
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed init socket", e)
            }
        }

        // Load projects into spinner
        lifecycleScope.launch {
            try {
                val projects = try { projectRepository.fetchAndCacheProjects() } catch (e: Exception) { emptyList() }
                val titles = projects.map { it.title ?: "Proyecto" }
                val ids = projects.map { it.id }
                val adapter = ArrayAdapter(this@ChatListActivity, android.R.layout.simple_spinner_item, titles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spProjects.adapter = adapter

                spProjects.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val projectId = ids.getOrNull(position)
                        if (projectId != null) {
                            currentProjectId = projectId
                            loadProjectChat(projectId)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatListActivity, "Error cargando proyectos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProjectChat(projectId: String) {
        lifecycleScope.launch {
            try {
                // Create or get project chat via GraphQL
                val mutation = """
                    mutation CreateProjectChat(${"$"}input: CreateProjectChatInput!) {
                        createProjectChat(input: ${"$"}input) {
                            id
                            type
                            projectId
                            title
                            participants {
                                userId {
                                    id
                                    email
                                    profile { firstName lastName }
                                }
                                role
                            }
                        }
                    }
                """.trimIndent()

                val variables = JSONObject()
                val input = JSONObject()
                input.put("projectId", projectId)
                variables.put("input", input)

                val response = graphQLClient.executeMutation(mutation, variables)
                
                if (response.has("errors")) {
                    val error = response.getJSONArray("errors").getJSONObject(0).getString("message")
                    Toast.makeText(this@ChatListActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val chatData = response.getJSONObject("data").getJSONObject("createProjectChat")
                currentChatId = chatData.getString("id")
                
                val participantsArray = chatData.getJSONArray("participants")
                val participants = mutableListOf<ChatParticipant>()
                
                for (i in 0 until participantsArray.length()) {
                    val p = participantsArray.getJSONObject(i)
                    val user = p.getJSONObject("userId")
                    val userId = user.getString("id")
                    val email = user.optString("email", "")
                    val profile = user.optJSONObject("profile")
                    val firstName = profile?.optString("firstName", "") ?: ""
                    val lastName = profile?.optString("lastName", "") ?: ""
                    val name = if (firstName.isNotBlank()) "$firstName $lastName".trim() else email
                    
                    participants.add(ChatParticipant(
                        userId = userId,
                        name = name,
                        email = email,
                        status = ConnectionStatus.OFFLINE // Will be updated by socket events
                    ))
                }
                
                allParticipants = participants
                usersAdapter.update(participants)
                
                // Connect to socket and join chat room
                if (!SocketManager.isConnected()) {
                    SocketManager.connect()
                    kotlinx.coroutines.delay(800) // Wait for connection and presence emit
                }
                currentChatId?.let { 
                    SocketManager.joinRoom(it)
                    Log.d(TAG, "Joined chat room: $it")
                    
                    // Request current status of all participants
                    kotlinx.coroutines.delay(200) // Small delay to ensure join is processed
                    SocketManager.requestParticipantsStatus(it)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading project chat", e)
                Toast.makeText(this@ChatListActivity, "Error cargando chat del proyecto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterParticipants(query: String) {
        if (query.isBlank()) {
            usersAdapter.update(allParticipants)
        } else {
            val filtered = allParticipants.filter {
                val name = it.name ?: it.email ?: ""
                name.contains(query, ignoreCase = true)
            }
            usersAdapter.update(filtered)
        }
    }

    private fun openProjectChat() {
        if (currentChatId != null) {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chatId", currentChatId)
            intent.putExtra("projectId", currentProjectId)
            intent.putExtra("chatTitle", spProjects.selectedItem.toString())
            startActivity(intent)
        } else {
            Toast.makeText(this, "Chat no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val json = JSONObject(payload)
                json.optString("sub") ?: json.optString("id") ?: json.optString("userId")
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract userId from token", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentChatId?.let { SocketManager.leaveRoom(it) }
    }

    private class UsersAdapter(
        private var items: List<ChatParticipant>,
        private val onClick: (ChatParticipant) -> Unit
    ) : RecyclerView.Adapter<UsersAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
            val tvName: TextView = view.findViewById(R.id.tvParticipantName)
            val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
            val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
            val tvUnreadBadge: TextView = view.findViewById(R.id.tvUnreadBadge)
            val vStatus: View = view.findViewById(R.id.vStatusIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_conversation, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val displayName = item.name ?: item.email ?: "Usuario"
            
            // Set avatar initial
            holder.tvAvatar.text = displayName.firstOrNull()?.uppercase() ?: "U"
            
            // Set participant name
            holder.tvName.text = displayName
            
            // Set last message preview based on status
            val statusText = when(item.status) {
                ConnectionStatus.ONLINE -> "En línea"
                ConnectionStatus.AWAY -> "Ausente"
                ConnectionStatus.OFFLINE -> {
                    if (!item.lastSeen.isNullOrEmpty() && item.lastSeen != "null") {
                        "Visto hace ${formatLastSeen(item.lastSeen)}"
                    } else {
                        "Desconectado"
                    }
                }
            }
            holder.tvLastMessage.text = statusText
            
            // Show timestamp for offline users
            if (item.status == ConnectionStatus.OFFLINE && !item.lastSeen.isNullOrEmpty() && item.lastSeen != "null") {
                holder.tvTimestamp.visibility = View.VISIBLE
                holder.tvTimestamp.text = formatTimestamp(item.lastSeen)
            } else {
                holder.tvTimestamp.visibility = View.GONE
            }
            
            // Hide unread badge for now
            holder.tvUnreadBadge.visibility = View.GONE
            
            // Set status indicator drawable with animation
            val statusDrawable = when(item.status) {
                ConnectionStatus.ONLINE -> R.drawable.status_indicator_online
                ConnectionStatus.AWAY -> R.drawable.status_indicator_away
                ConnectionStatus.OFFLINE -> R.drawable.status_indicator_offline
            }
            holder.vStatus.setBackgroundResource(statusDrawable)
            
            holder.itemView.setOnClickListener { onClick(item) }
        }
        
        private fun formatLastSeen(timestamp: String): String {
            return try {
                val time = timestamp.toLongOrNull() ?: return "un momento"
                val now = System.currentTimeMillis()
                val diff = now - time
                when {
                    diff < 60000 -> "un momento"
                    diff < 3600000 -> "${diff / 60000}m"
                    diff < 86400000 -> "${diff / 3600000}h"
                    else -> "${diff / 86400000}d"
                }
            } catch (e: Exception) {
                "un momento"
            }
        }
        
        private fun formatTimestamp(timestamp: String): String {
            return try {
                val time = timestamp.toLongOrNull() ?: return ""
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(time))
            } catch (e: Exception) {
                ""
            }
        }

        override fun getItemCount(): Int = items.size

        fun update(newItems: List<ChatParticipant>) {
            this.items = newItems
            notifyDataSetChanged()
        }
        
        fun updateUserStatus(userId: String, status: String) {
            val index = items.indexOfFirst { it.userId == userId }
            if (index != -1) {
                val connectionStatus = when(status.lowercase()) {
                    "online" -> ConnectionStatus.ONLINE
                    "away" -> ConnectionStatus.AWAY
                    else -> ConnectionStatus.OFFLINE
                }
                val updatedItems = items.toMutableList()
                updatedItems[index] = items[index].copy(
                    status = connectionStatus,
                    lastSeen = if (connectionStatus == ConnectionStatus.OFFLINE) 
                        System.currentTimeMillis().toString() else null
                )
                items = updatedItems
                notifyItemChanged(index)
            }
        }
        
        fun getItems(): List<ChatParticipant> = items
    }
}
