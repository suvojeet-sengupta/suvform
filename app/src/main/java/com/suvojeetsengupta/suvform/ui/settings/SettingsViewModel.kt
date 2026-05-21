package com.suvojeetsengupta.suvform.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.suvojeetsengupta.suvform.data.prefs.GeminiKeyStore
import com.suvojeetsengupta.suvform.data.prefs.ThemePreferenceStore
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val themeStore: ThemePreferenceStore,
    private val geminiKeyStore: GeminiKeyStore,
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    val apiKey: StateFlow<String> = geminiKeyStore.key

    val themeMode: StateFlow<ThemeMode> = themeStore.mode

    fun setThemeMode(mode: ThemeMode) = themeStore.setMode(mode)

    fun saveApiKey(key: String) = geminiKeyStore.setKey(key)

    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onDone()
        }
    }
}
