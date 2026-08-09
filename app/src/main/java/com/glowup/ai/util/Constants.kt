package com.glowup.ai.util

object Constants {
    // ══════════════════════════════════════════════════════
    // YOUCAM API CONFIGURATION
    // ══════════════════════════════════════════════════════
    
    // Direct Regional Endpoint (Usually faster and more reliable)
    const val YOUCAM_BASE_URL = "https://yce-api-01.makeupar.com/s2s/v2.0"
    
    const val YOUCAM_API_KEY = "YOUR_YOUCAM_API_KEY_HERE"
    const val YOUCAM_SECRET_KEY = "YOUR_YOUCAM_SECRET_KEY_HERE"

    // Legacy Config
    const val BASE_URL = "http://localhost:3000" 
    const val SIGNIN_ENDPOINT = "/api/auth/signin"
    const val SIGNUP_ENDPOINT = "/api/auth/signup"
    const val USER_ENDPOINT = "/api/auth/user"

    const val DATASTORE_NAME = "glowup_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_AGE = "user_age"

    const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    const val GALLERY_PERMISSION_REQUEST_CODE = 1002
}
