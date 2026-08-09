package com.glowup.ai

import android.app.Application
import com.glowup.ai.data.store.AuthStore
import com.google.firebase.FirebaseApp

class GlowUpApplication : Application() {

    lateinit var authStore: AuthStore
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        authStore = AuthStore(this)
    }
}