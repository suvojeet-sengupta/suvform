package com.suvojeetsengupta.suvform.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FormDraft
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.local.FormDao
import com.suvojeetsengupta.suvform.data.local.FormSummaryEntity
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    private val formDao: FormDao,
    private val auth: FirebaseAuth,
) : ViewModel() {

    fun selectForResponses(form: FormSummaryDto) {
        selectedForm.formId = form.id
        selectedForm.formTitle = form.title
    }

    private val _meta = MutableStateFlow(HomeMeta())

    /** Combine local-cached forms with loading/error meta state. */
    val state: StateFlow<HomeUiState> = run {
        val uid = auth.currentUser?.uid
        val cached = if (uid != null) formDao.observeForOwner(uid) else flowOf(emptyList())
        combine(cached, _meta) { entities, meta ->
            HomeUiState(
                loading = meta.loading,
                forms = entities.map { it.toDto() },
                error = meta.error,
                openingFormId = meta.openingFormId,
                offline = meta.offline,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(loading = true))
    }

    init { refresh() }

    fun refresh() {
        if (_meta.value.loading) return
        _meta.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.listForms() }
                .onSuccess { resp ->
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        formDao.replaceForOwner(uid, resp.forms.map { FormSummaryEntity.fromDto(uid, it) })
                    }
                    _meta.update { it.copy(loading = false, offline = false) }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    // Don't blow away cached data — show offline banner.
                    _meta.update { it.copy(loading = false, error = msg, offline = true) }
                }
        }
    }

    fun openForm(formId: String, onReady: () -> Unit) {
        _meta.update { it.copy(openingFormId = formId) }
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
                    _meta.update { it.copy(openingFormId = null) }
                    onReady()
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _meta.update { it.copy(openingFormId = null, error = msg ?: "Failed to open") }
                }
        }
    }

    fun delete(formId: String) {
        viewModelScope.launch {
            // Optimistic: remove from cache immediately.
            formDao.deleteById(formId)
            runCatching { api.deleteForm(formId) }
                .onFailure { e ->
                    _meta.update { it.copy(error = "Delete failed: ${e.message}") }
                    // Re-fetch to restore in case server still has it.
                    refresh()
                }
        }
    }

    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            // Don't wipe the cache — preserves last-seen forms for the next sign-in.
            onDone()
        }
    }

    private data class HomeMeta(
        val loading: Boolean = false,
        val error: String? = null,
        val openingFormId: String? = null,
        val offline: Boolean = false,
    )
}

data class HomeUiState(
    val loading: Boolean = false,
    val forms: List<FormSummaryDto> = emptyList(),
    val error: String? = null,
    val openingFormId: String? = null,
    val offline: Boolean = false,
)
