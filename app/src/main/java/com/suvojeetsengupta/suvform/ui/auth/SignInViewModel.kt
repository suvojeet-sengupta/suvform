package com.suvojeetsengupta.suvform.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun signIn(context: Context) {
        if (_state.value.loading) return
        _state.value = SignInUiState(loading = true)
        viewModelScope.launch {
            authRepository.signInWithGoogle(context)
                .onSuccess { _state.value = SignInUiState(success = true) }
                .onFailure { e -> _state.value = SignInUiState(error = e.message ?: "Sign-in failed") }
        }
    }
}

data class SignInUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)
