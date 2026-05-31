package com.suvojeetsengupta.suvform.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.FormDraft
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.data.remote.GenerateFormRequest
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.util.ErrorMapper
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
    private val draftStore: FormDraftStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun updatePrompt(value: String) {
        _state.update { it.copy(prompt = value, error = null) }
    }

    fun setLocale(locale: String) {
        _state.update { it.copy(locale = locale) }
    }

    /** Generate via AI, push result into the draft store, signal nav to editor. */
    fun generate() {
        val prompt = _state.value.prompt.trim()
        if (prompt.length < 3) {
            _state.update { it.copy(error = "Please describe the form you need.") }
            return
        }
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.generateForm(GenerateFormRequest(prompt, _state.value.locale)) }
                .onSuccess { generated ->
                    draftStore.set(FormDraft.fromGenerated(generated))
                    _state.update { it.copy(loading = false, navigateToEditor = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = ErrorMapper.message(e)) }
                }
        }
    }

    /** Start with a blank form (manual mode). */
    fun startBlank() {
        draftStore.set(FormDraft.blank())
        _state.update { it.copy(navigateToEditor = true) }
    }

    fun onNavigated() {
        _state.update { it.copy(navigateToEditor = false) }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }
}

data class CreateUiState(
    val prompt: String = "",
    val locale: String = "en",
    val loading: Boolean = false,
    val error: String? = null,
    val navigateToEditor: Boolean = false,
)
