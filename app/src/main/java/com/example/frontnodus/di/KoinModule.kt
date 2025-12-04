package com.example.frontnodus.di

import com.example.frontnodus.data.network.GraphQLClient
import com.example.frontnodus.data.repository.AuthRepository
import com.example.frontnodus.data.storage.TokenStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.qualifier.named
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { TokenStore(get()) }

    // Dispatchers
    single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("main")) { Dispatchers.Main }

    // Application scope
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // GraphQL client: capture TokenStore now, return a GraphQLClient whose tokenProvider reads from DataStore
    single {
        val tokenStore: TokenStore = get()
        // Use BACKEND_BASE_URL from BuildConfig so you can change it without editing code.
        val baseUrl = com.example.frontnodus.BuildConfig.BACKEND_BASE_URL
        GraphQLClient(baseUrl) { tokenStore.tokenFlow.firstOrNull() }
    }

    // AppDatabase singleton
    single { com.example.frontnodus.data.local.AppDatabase.getInstance(androidContext()) }

    // Project repository
    single { com.example.frontnodus.data.repository.ProjectRepository(get(), get(named("io")), get<com.example.frontnodus.data.local.AppDatabase>().projectDao()) }

    // Auth repository uses the IO dispatcher
    single { AuthRepository(get(), get(), get(named("io"))) }
    // Task repository
        single { com.example.frontnodus.data.repository.TaskRepository(get(), get(named("io")), get<com.example.frontnodus.data.local.AppDatabase>().taskDao()) }
    // Event repository
    single { com.example.frontnodus.data.repository.EventRepository(get(), get(named("io"))) }
}
