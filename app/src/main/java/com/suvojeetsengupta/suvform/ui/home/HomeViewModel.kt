package com.suvojeetsengupta.suvform.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.FormDraft
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: SuvFormApi,
    private val draftStore: FormDraftStore,
    private val selectedForm: SelectedFormStore,
) : ViewModel() {

    fun selectForResponses(form: FormSummaryDto) {
        selectedForm.formId = form.id
        selectedForm.formTitle = form.title
    }


    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.listForms() }
                .onSuccess { resp ->
                    _state.update { it.copy(loading = false, forms = resp.forms) }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load") }
                }
        }
    }

    /** Load a saved form into the draft store and return success/failure via callback. */
    fun openForm(formId: String, onReady: () -> Unit) {
        _state.update { it.copy(openingFormId = formId) }
        viewModelScope.launch {
            runCatching { api.getForm(formId) }
                .onSuccess { detail ->
                    val shareUrl = detail.publicSlug?.let { slug ->
                        com.suvojeetsengupta.suvform.BuildConfig.API_BASE_URL.trimEnd('/') + "/f/" + slug
                    }
                    draftStore.set(
                        FormDraft(
                            remoteId = detail.id,
                            title = detail.title,
                            description = detail.description.orEmpty(),
                            fields = detail.fields.map { FieldEdit.fromDto(it) },
                            calculations = detail.calculations.map { CalculationEdit.fromDto(it) },
                            published = detail.published == 1,
                            publicSlug = detail.publicSlug,
                            shareUrl = shareUrl,
                        ),
                    )
                    _state.update { it.copy(openingFormId = null) }
                    onReady()
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(openingFormId = null, error = msg ?: "Failed to open") }
                }
        }
    }

    fun delete(formId: String) {
        viewModelScope.launch {
            runCatching { api.deleteForm(formId) }
                .onSuccess {
                    _state.update { s -> s.copy(forms = s.forms.filterNot { it.id == formId }) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Delete failed") }
                }
        }
    }

    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onDone()
        }
    }
}

data class HomeUiState(
    val loading: Boolean = false,
    val forms: List<FormSummaryDto> = emptyList(),
    val error: String? = null,
    val openingFormId: String? = null,
)
