package com.example.frontnodus.data.repository

import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.data.storage.TokenStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AuthResult(val token: String, val user: JSONObject)

class AuthRepository(
    private val client: GraphQLClient,
    private val tokenStore: TokenStore,
    private val ioDispatcher: CoroutineDispatcher
) {

    private val loginMutation = """
        mutation Login(${'$'}email: String!, ${'$'}password: String!) {
          login(email: ${'$'}email, password: ${'$'}password) { token user { id email role isActive profile { firstName lastName phone avatar } } }
        }
    """.trimIndent()

    private val registerMutation = """
        mutation Register(${'$'}input: RegisterInput!) {
          register(input: ${'$'}input) { token user { id email role isActive profile { firstName lastName phone avatar } } }
        }
    """.trimIndent()

    suspend fun login(email: String, password: String): AuthResult = withContext(ioDispatcher) {
        val variables = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val resp = client.executeMutation(loginMutation, variables)
        val data = resp.optJSONObject("data") ?: throw Exception("No data in response")
        val login = data.optJSONObject("login") ?: throw Exception("Login failed")
        val token = login.optString("token")
        val user = login.optJSONObject("user") ?: JSONObject()
        // persist token
        tokenStore.saveToken(token)
        AuthResult(token, user)
    }

    suspend fun register(email: String, password: String, profile: JSONObject? = null): AuthResult = withContext(ioDispatcher) {
        val input = JSONObject().apply {
            put("email", email)
            put("password", password)
            if (profile != null) put("profile", profile)
        }
        val vars = JSONObject().apply { put("input", input) }
        val resp = client.executeMutation(registerMutation, vars)
        val data = resp.optJSONObject("data") ?: throw Exception("No data in response")
        val reg = data.optJSONObject("register") ?: throw Exception("Register failed")
        val token = reg.optString("token")
        val user = reg.optJSONObject("user") ?: JSONObject()
        tokenStore.saveToken(token)
        AuthResult(token, user)
    }
}
