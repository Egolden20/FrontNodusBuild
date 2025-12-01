package com.example.frontnodus.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GraphQLClient(private val baseUrl: String, private val tokenProvider: suspend () -> String?) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun executeMutation(query: String, variables: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val bodyJson = JSONObject()
            bodyJson.put("query", query)
            if (variables != null) bodyJson.put("variables", variables)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = bodyJson.toString().toRequestBody(mediaType)

            val builder = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .addHeader("Accept", "application/json")

            // add Authorization header if token available
            val token = tokenProvider.invoke()
            if (!token.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $token")
            }

            val request = builder.build()
            client.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: throw Exception("Empty response")
                return@withContext JSONObject(bodyStr)
            }
        }
}
