package com.example.frontnodus.models

data class ChatParticipant(
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val status: ConnectionStatus = ConnectionStatus.OFFLINE,
    val lastSeen: String? = null,
    val joinedAt: String? = null
)

enum class ParticipantRole {
    ADMIN,
    MEMBER
}

enum class ConnectionStatus {
    ONLINE,
    AWAY,
    OFFLINE
}
