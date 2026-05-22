package com.suvojeetsengupta.suvform.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches headers to every Retrofit call:
 * 1. Authorization: Bearer <firebaseIdToken>
 * 2. X-Timezone: <ID> (e.g. Asia/Kolkata)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val auth: FirebaseAuth,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val user = auth.currentUser
        val token = if (user != null) {
            runCatching { runBlocking { user.getIdToken(false).await().token } }.getOrNull()
        } else null

        val builder = original.newBuilder()
        if (token != null) builder.header("Authorization", "Bearer $token")
        
        // Pass the device timezone to the backend so it can localize AI quota and timestamps.
        builder.header("X-Timezone", TimeZone.getDefault().id)

        return chain.proceed(builder.build())
    }
}
