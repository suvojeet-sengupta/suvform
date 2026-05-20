package com.suvojeetsengupta.suvform.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("suvform_prefs", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _apiKey = MutableStateFlow(prefs.getString("gemini_api_key", "").orEmpty())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _apiKey.value = key
    }

    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onDone()
        }
    }
}
