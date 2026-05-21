package com.suvojeetsengupta.suvform.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.suvojeetsengupta.suvform.BuildConfig
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.data.remote.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val api: SuvFormApi,
) {
    /** Emits the currently signed-in Firebase user (or null) — survives sign-in/out. */
    val authState: Flow<FirebaseAuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val state = fa.currentUser?.let { FirebaseAuthState.SignedIn(it.uid) }
                ?: FirebaseAuthState.SignedOut
            trySend(state)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Triggers the Credential Manager Google Sign-In flow, exchanges the Google ID token
     * for a Firebase credential, then upserts the user on our backend.
     */
    suspend fun signInWithGoogle(context: Context): Result<UserDto> = runCatching {
        require(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID is not configured. Set it in gradle.properties or as a -P/env var."
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(context = context, request = request)
        val credential = response.credential
        check(credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential::class.java.name}"
        }
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        auth.signInWithCredential(firebaseCred).await()

        syncUserWithBackend().getOrThrow()
    }

    /**
     * Syncs the current Firebase user profile with our backend.
     * Should be called after sign-in or when profile details change.
     */
    suspend fun syncUserWithBackend(): Result<UserDto> = runCatching {
        api.upsertMe()
    }

    suspend fun signOut(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        auth.signOut()
    }

    /** "Sign out everywhere": revoke all server sessions, then sign out locally. */
    suspend fun revokeAllSessions(context: Context) {
        api.revokeSessions()
        signOut(context)
    }

    /** Delete the account + all data server-side, then sign out locally. */
    suspend fun deleteAccount(context: Context) {
        api.deleteAccount()
        signOut(context)
    }

    /** Fetch the full data export as a JSON string. */
    suspend fun exportData(): String =
        withContext(Dispatchers.IO) { api.exportData().string() }
}

sealed interface FirebaseAuthState {
    data object SignedOut : FirebaseAuthState
    data class SignedIn(val uid: String) : FirebaseAuthState
}
