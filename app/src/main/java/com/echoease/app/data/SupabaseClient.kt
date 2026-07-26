package com.echoease.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val SUPABASE_URL = "https://jfdoctzjtdpajsvpjnwj.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpmZG9jdHpqdGRwYWpzdnBqbndqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUwMzIxNDIsImV4cCI6MjEwMDYwODE0Mn0.J7rcBGTsr4rmupMXwLXSWAThxxcMaNuCpqNCbo5Jl48"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        install(Auth)
        install(ComposeAuth) {
            googleNativeLogin(serverClientId = "2848583707-076focc6npr2ataaf8d02fstlukh9ado.apps.googleusercontent.com")
        }
        install(Postgrest)
        install(Realtime)
    }
}
