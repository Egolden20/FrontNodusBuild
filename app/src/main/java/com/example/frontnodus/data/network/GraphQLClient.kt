package com.example.frontnodus.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FilePart(val fieldName: String, val filename: String, val bytes: ByteArray, val contentType: String)

class GraphQLClient(private val baseUrl: String, private val tokenProvider: suspend () -> String?) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    // Execute a multipart/form-data GraphQL request following the GraphQL multipart request spec
    suspend fun executeMultipart(operations: String, map: String, fileParts: List<FilePart>, operationName: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

            // Debug logging
            try {
                android.util.Log.d("GraphQLClient", "executeMultipart operations=${operations}")
                android.util.Log.d("GraphQLClient", "executeMultipart map=${map}")
                android.util.Log.d("GraphQLClient", "executeMultipart filePartsCount=${fileParts.size}")
                for (fp in fileParts) {
                    android.util.Log.d("GraphQLClient", "filePart: field=${fp.fieldName} name=${fp.filename} size=${fp.bytes.size} type=${fp.contentType}")
                }
            } catch (ignored: Exception) {
            }

            // Send operations and map as application/json parts (not text/plain)
            val jsonMedia = "application/json; charset=utf-8".toMediaType()
            multipartBuilder.addFormDataPart("operations", null, operations.toRequestBody(jsonMedia))
            multipartBuilder.addFormDataPart("map", null, map.toRequestBody(jsonMedia))

            // add files
            for (fp in fileParts) {
                val mediaType = fp.contentType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
                val rb: RequestBody = RequestBody.create(mediaType, fp.bytes)
                multipartBuilder.addFormDataPart(fp.fieldName, fp.filename, rb)
            }

            val requestBody = multipartBuilder.build()

            val builder = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .addHeader("Accept", "application/json")

            // To satisfy Apollo Server CSRF protection when sending multipart/form-data,
            // include both the operation name header and the preflight header.
            if (!operationName.isNullOrBlank()) {
                builder.addHeader("x-apollo-operation-name", operationName)
            }
            // Some Apollo setups expect this header instead
            builder.addHeader("apollo-require-preflight", "true")

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
