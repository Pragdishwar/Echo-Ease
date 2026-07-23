package com.echoease.app

import android.app.Application
import com.google.firebase.FirebaseApp

class EchoEaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase manually to ensure it's ready before any viewmodels are created
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
