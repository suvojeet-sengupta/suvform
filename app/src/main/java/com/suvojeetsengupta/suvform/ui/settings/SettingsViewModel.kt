package com.suvojeetsengupta.suvform.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.suvojeetsengupta.suvform.data.prefs.GeminiKeyStore
import com.suvojeetsengupta.suvform.data.prefs.SecurityPreferenceStore
import com.suvojeetsengupta.suvform.data.prefs.ThemePreferenceStore
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.ui.theme.ThemeMode
import com.suvojeetsengupta.suvform.util.BiometricAuthManager
import com.suvojeetsengupta.suvform.util.ErrorMapper
import com.suvojeetsengupta.suvform.util.ResponseExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val themeStore: ThemePreferenceStore,
    private val geminiKeyStore: GeminiKeyStore,
    private val securityStore: SecurityPreferenceStore,
    val biometricAuthManager: BiometricAuthManager,
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    val apiKey: StateFlow<String> = geminiKeyStore.key

    val themeMode: StateFlow<ThemeMode> = themeStore.mode

    val isBiometricEnabled: StateFlow<Boolean> = securityStore.isBiometricEnabled

    fun setThemeMode(mode: ThemeMode) = themeStore.setMode(mode)

    fun saveApiKey(key: String) = geminiKeyStore.setKey(key)

    fun setBiometricEnabled(enabled: Boolean) = securityStore.setBiometricEnabled(enabled)

    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onDone()
        }
    }

    // ---- Account actions (sign out everywhere / export / delete) ----

    private val _account = MutableStateFlow(AccountUiState())
    val account: StateFlow<AccountUiState> = _account.asStateFlow()

    fun clearAccountError() = _account.update { it.copy(error = null) }

    fun signOutEverywhere(context: Context, onSignedOut: () -> Unit) {
        if (_account.value.working) return
        _account.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            runCatching { authRepository.revokeAllSessions(context) }
                .onSuccess { _account.update { it.copy(working = false) }; onSignedOut() }
                .onFailure { e -> _account.update { it.copy(working = false, error = ErrorMapper.message(e)) } }
        }
    }

    fun deleteAccount(context: Context, onSignedOut: () -> Unit) {
        if (_account.value.working) return
        _account.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            runCatching { authRepository.deleteAccount(context) }
                .onSuccess { _account.update { it.copy(working = false) }; onSignedOut() }
                .onFailure { e -> _account.update { it.copy(working = false, error = ErrorMapper.message(e)) } }
        }
    }

    fun exportData(context: Context) {
        if (_account.value.working) return
        _account.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val json = authRepository.exportData()
                withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "suvform-export-${System.currentTimeMillis()}.json")
                    file.writeText(json)
                    file
                }
            }
                .onSuccess { file ->
                    _account.update { it.copy(working = false) }
                    ResponseExport.share(context, file, "application/json", "SuvForm data export")
                }
                .onFailure { e -> _account.update { it.copy(working = false, error = ErrorMapper.message(e)) } }
        }
    }

    data class AccountUiState(
        val working: Boolean = false,
        val error: String? = null,
    )
}
