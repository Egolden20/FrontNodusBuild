package com.example.frontnodus.models

data class Chat(
    val id: String,
    val type: ChatType,
    val projectId: String? = null,
    val title: String? = null,
    val participants: List<ChatParticipant> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

enum class ChatType {
    DIRECT,
    PROJECT,
    GROUP
}
