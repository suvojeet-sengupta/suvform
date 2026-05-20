package com.suvojeetsengupta.suvform.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called by OkHttp ONLY when the server returns 401. Force-refreshes the Firebase ID
 * token and retries the request once. If the user isn't signed in or the refresh
 * fails, we return null which lets the 401 propagate to the caller.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val auth: FirebaseAuth,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid an infinite loop if the refreshed token also returns 401.
        if (responseCount(response) >= 2) {
            Log.w(TAG, "Already retried; giving up on 401")
            return null
        }

        val user = auth.currentUser ?: run {
            Log.w(TAG, "No signed-in user on 401")
            return null
        }

        val newToken = runCatching {
            runBlocking { user.getIdToken(true).await().token }
        }.onFailure { Log.e(TAG, "Token refresh failed", it) }.getOrNull() ?: return null

        Log.i(TAG, "Refreshed token, retrying request")
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}
