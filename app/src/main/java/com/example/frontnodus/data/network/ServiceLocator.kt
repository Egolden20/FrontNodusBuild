package com.example.frontnodus.data.network

import android.content.Context
import com.example.frontnodus.data.repository.AuthRepository
import com.example.frontnodus.data.storage.TokenStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers


object ServiceLocator {
    // Default base URL read from BuildConfig so it matches KoinModule
    private val DEFAULT_BASE_URL: String = com.example.frontnodus.BuildConfig.BACKEND_BASE_URL

    fun provideTokenStore(context: Context): TokenStore = TokenStore(context)

    fun provideGraphQLClient(context: Context, baseUrl: String = DEFAULT_BASE_URL): GraphQLClient {
        val tokenStore = provideTokenStore(context)
        // tokenProvider reads latest token synchronously via tokenFlow; here we'll read first emitted value
        return GraphQLClient(baseUrl) {
            // Read latest token from DataStore (suspending)
            tokenStore.tokenFlow.firstOrNull()
        }
    }

    fun provideAuthRepository(context: Context, baseUrl: String = DEFAULT_BASE_URL): AuthRepository {
        val tokenStore = provideTokenStore(context)
        val client = provideGraphQLClient(context, baseUrl)
        return AuthRepository(client, tokenStore, Dispatchers.IO)
    }
}
