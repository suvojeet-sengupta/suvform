package com.suvojeetsengupta.suvform.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.remote.GeneratedFormDto
import com.suvojeetsengupta.suvform.data.remote.GenerateFormRequest
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val api: SuvFormApi,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun updatePrompt(value: String) {
        _state.update { it.copy(prompt = value, error = null) }
    }

    fun toggleLocale() {
        _state.update { it.copy(locale = if (it.locale == "en") "hi" else "en") }
    }

    fun generate() {
        val prompt = _state.value.prompt.trim()
        if (prompt.length < 3) {
            _state.update { it.copy(error = "Likhna padega kya form chahiye") }
            return
        }
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null, result = null) }
        viewModelScope.launch {
            runCatching { api.generateForm(GenerateFormRequest(prompt, _state.value.locale)) }
                .onSuccess { form -> _state.update { it.copy(loading = false, result = form) } }
                .onFailure { e ->
                    val msg = when (e) {
                        is HttpException -> {
                            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                            "HTTP ${e.code()}${if (!body.isNullOrBlank()) ": $body" else ""}"
                        }
                        else -> e.message ?: "Failed"
                    }
                    _state.update { it.copy(loading = false, error = msg) }
                }
        }
    }
}

data class CreateUiState(
    val prompt: String = "",
    val locale: String = "en",
    val loading: Boolean = false,
    val result: GeneratedFormDto? = null,
    val error: String? = null,
)
