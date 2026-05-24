package com.suvojeetsengupta.suvform.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FormDraft
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.data.repository.FormRepository
import com.suvojeetsengupta.suvform.util.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val formRepository: FormRepository,
    private val draftStore: FormDraftStore,
    private val selectedForm: SelectedFormStore,
) : ViewModel() {

    fun selectForResponses(form: FormSummaryDto) {
        selectedForm.formId = form.id
        selectedForm.formTitle = form.title
    }

    private val _meta = MutableStateFlow(HomeMeta())

    /** Combine local-cached forms (offline-first) with loading/error meta state. */
    val state: StateFlow<HomeUiState> =
        combine(formRepository.observeForms(), _meta) { entities, meta ->
            HomeUiState(
                loading = meta.loading,
                forms = entities.map { it.toDto() },
                shareUrls = entities.associate { it.id to it.shareUrl },
                stats = meta.stats ?: formRepository.getCachedStats(),
                error = meta.error,
                openingFormId = meta.openingFormId,
                offline = meta.offline,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(loading = true))

    init { refresh(force = false) }

    fun refresh(force: Boolean = true) {
        if (_meta.value.loading) return
        _meta.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { formRepository.syncDashboard(force) }
                .onSuccess { stats -> 
                    _meta.update { it.copy(loading = false, offline = false, stats = stats) } 
                }
                .onFailure { e ->
                    val msg = ErrorMapper.message(e)
                    // Don't blow away cached data — show offline banner.
                    _meta.update { it.copy(loading = false, error = msg, offline = true) }
                }
        }
    }

    fun openForm(formId: String, onReady: () -> Unit) {
        _meta.update { it.copy(openingFormId = formId) }
        viewModelScope.launch {
            runCatching { formRepository.getForm(formId) }
                .onSuccess { detail ->
                    val shareUrl = detail.publicSlug?.let { slug ->
                        com.suvojeetsengupta.suvform.BuildConfig.PUBLIC_FORM_BASE_URL.trimEnd('/') + "/f/" + slug
                    }
                    if (shareUrl != null) {
                        formRepository.cacheShareUrl(detail.id, shareUrl)
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
                    val msg = ErrorMapper.message(e)
                    _meta.update { it.copy(openingFormId = null, error = msg ?: "Failed to open") }
                }
        }
    }

    fun shareForm(context: Context, form: FormSummaryDto) {
        val cachedUrl = state.value.shareUrls[form.id]
        if (cachedUrl != null) {
            doShare(context, form.title, cachedUrl)
            return
        }

        // Fetch URL from server if not cached
        viewModelScope.launch {
            runCatching { formRepository.getForm(form.id) }
                .onSuccess { detail ->
                    val shareUrl = detail.publicSlug?.let { slug ->
                        com.suvojeetsengupta.suvform.BuildConfig.PUBLIC_FORM_BASE_URL.trimEnd('/') + "/f/" + slug
                    }
                    if (shareUrl != null) {
                        formRepository.cacheShareUrl(form.id, shareUrl)
                        doShare(context, form.title, shareUrl)
                    } else {
                        _meta.update { it.copy(error = "Form is not published.") }
                    }
                }
                .onFailure { e ->
                    _meta.update { it.copy(error = "Failed to get share link: ${ErrorMapper.message(e)}") }
                }
        }
    }

    private fun doShare(context: Context, title: String, url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Share Form: $title")
            putExtra(android.content.Intent.EXTRA_TEXT, "Fill out this form: $url")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
    }

    fun delete(formId: String) {
        viewModelScope.launch {
            // Optimistic delete is handled inside the repository (Room first).
            runCatching { formRepository.deleteForm(formId) }
                .onFailure { e ->
                    _meta.update { it.copy(error = "Delete failed: ${ErrorMapper.message(e)}") }
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
        val stats: UserStatsDto? = null,
    )
}

data class HomeUiState(
    val loading: Boolean = false,
    val forms: List<FormSummaryDto> = emptyList(),
    val shareUrls: Map<String, String?> = emptyMap(),
    val stats: UserStatsDto? = null,
    val error: String? = null,
    val openingFormId: String? = null,
    val offline: Boolean = false,
)
