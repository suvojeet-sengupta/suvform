package com.suvojeetsengupta.suvform.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches `Authorization: Bearer <firebaseIdToken>` to every Retrofit call.
 * Uses [FirebaseAuth.currentUser]'s ID token; OkHttp interceptor runs on a background
 * thread so a tight runBlocking is acceptable here.
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

        val request = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original

        return chain.proceed(request)
    }
}
