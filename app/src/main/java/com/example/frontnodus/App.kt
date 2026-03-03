package com.example.frontnodus

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.frontnodus.data.storage.TokenStore
import com.example.frontnodus.di.appModule
import com.example.frontnodus.utils.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application(), LifecycleObserver {

	private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
	private var isAppInForeground = false

	override fun onCreate() {
		super.onCreate()
		
		Log.d("App", "onCreate called")
		
		// Initialize Koin
		startKoin {
			androidContext(this@App)
			modules(appModule)
		}

		// Register lifecycle observer to handle app foreground/background
		ProcessLifecycleOwner.get().lifecycle.addObserver(this)
		
		Log.d("App", "Lifecycle observer registered")

		// Initialize socket connection globally after a short delay to ensure Koin is ready
		applicationScope.launch {
			kotlinx.coroutines.delay(500) // Wait for Koin to be fully initialized
			initializeSocketConnection()
		}
	}

	private suspend fun initializeSocketConnection() {
		try {
			Log.d("App", "initializeSocketConnection started")
			val tokenStore: TokenStore by inject()
			val token = tokenStore.getToken()
			Log.d("App", "Token retrieved: ${if (token.isNullOrEmpty()) "empty" else "present (${token.length} chars)"}")

			if (!token.isNullOrEmpty()) {
				val userId = extractUserIdFromToken(token)
				Log.d("App", "UserId extracted: ${if (userId.isNullOrEmpty()) "empty" else userId}")
				
				if (!userId.isNullOrEmpty()) {
					val wsBase = BuildConfig.BACKEND_BASE_URL.replace("/graphql", "")
					Log.d("App", "WS Base URL: $wsBase")
					
					Log.d("App", "Initializing global socket connection for user: $userId")
					
					SocketManager.init(wsBase, token, userId)
					SocketManager.connect()
					
					// Emit online presence
					SocketManager.emitPresence("online")
					
					Log.d("App", "Socket connected and presence emitted")
				} else {
					Log.d("App", "Could not extract userId from token")
				}
			} else {
				Log.d("App", "No token found, skipping socket initialization")
			}
		} catch (e: Exception) {
			Log.e("App", "Error initializing socket connection", e)
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	fun onAppForegrounded() {
		Log.d("App", "App moved to foreground")
		isAppInForeground = true
		
		// Emit online status when app comes to foreground
		applicationScope.launch {
			try {
				if (SocketManager.isConnected()) {
					SocketManager.emitPresence("online")
					Log.d("App", "Emitted online presence")
				} else {
					// Reconnect if disconnected
					initializeSocketConnection()
				}
			} catch (e: Exception) {
				Log.e("App", "Error emitting online presence", e)
			}
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	fun onAppBackgrounded() {
		Log.d("App", "App moved to background")
		isAppInForeground = false
		
		// Emit away status when app goes to background
		applicationScope.launch {
			try {
				if (SocketManager.isConnected()) {
					SocketManager.emitPresence("away")
					Log.d("App", "Emitted away presence")
				}
			} catch (e: Exception) {
				Log.e("App", "Error emitting away presence", e)
			}
		}
	}

	private fun extractUserIdFromToken(token: String): String? {
		return try {
			val parts = token.split(".")
			if (parts.size >= 2) {
				val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
				Log.d("App", "Token payload decoded: $payload")
				val json = JSONObject(payload)
				val userId = json.optString("sub").takeIf { it.isNotEmpty() }
					?: json.optString("id").takeIf { it.isNotEmpty() }
					?: json.optString("userId").takeIf { it.isNotEmpty() }
				Log.d("App", "Extracted userId from token: $userId")
				userId
			} else {
				Log.e("App", "Token has invalid format (less than 2 parts)")
				null
			}
		} catch (e: Exception) {
			Log.e("App", "Failed to extract userId from token", e)
			null
		}
	}
}
