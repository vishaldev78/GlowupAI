package com.glowup.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.glowup.ai.data.api.AuthApi
import com.glowup.ai.ui.navigation.AppNavigation
import com.glowup.ai.ui.theme.GlowUpAITheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        val app = application as GlowUpApplication

        // Keep splash visible until we check auth state
        var isLoggedIn by mutableStateOf(false)
        var isReady by mutableStateOf(false)
        var userName by mutableStateOf("")
        var userAge by mutableStateOf(0)

        // Check auth on startup
        lifecycleScope.launch {
            val token = app.authStore.authToken.first()
            val name = app.authStore.userName.first()
            val age = app.authStore.userAge.first()
            isLoggedIn = !token.isNullOrBlank()
            userName = name ?: ""
            userAge = age ?: 0
            isReady = true
            splashScreen.setKeepOnScreenCondition { !isReady }
        }

        setContent {
            // Collect auth state changes
            LaunchedEffect(Unit) {
                launch {
                    app.authStore.isLoggedIn.collect { loggedIn ->
                        isLoggedIn = loggedIn
                    }
                }
                launch {
                    app.authStore.userName.collect { userName = it ?: "" }
                }
                launch {
                    app.authStore.userAge.collect { userAge = it ?: 0 }
                }
            }

            GlowUpAITheme {
                AppNavigation(
                    authStore = app.authStore,
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    userAge = userAge,
                    onAuthSuccess = { /* Token saved in AuthScreen, flows update state */ },
                    onLogout = {
                        lifecycleScope.launch {
                            app.authStore.clearAuth()
                        }
                    }
                )
            }
        }
    }
}