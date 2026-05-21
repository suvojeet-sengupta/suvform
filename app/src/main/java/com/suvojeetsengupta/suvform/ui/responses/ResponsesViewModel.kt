package com.suvojeetsengupta.suvform.ui.responses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.util.ResponseExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suvojeetsengupta.suvform.data.remote.ResponsesPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

data class ResponsesUiState(
    val formTitle: String = "Responses",
    val fields: List<FieldDto> = emptyList(),
    val loading: Boolean = false,
    val exporting: Boolean = false,
    val formsToSelect: List<com.suvojeetsengupta.suvform.data.remote.FormSummaryDto> = emptyList(),
    val error: String? = null,
    val loadingInsights: Boolean = false,
    val insightsSummary: String? = null,
    val insightsError: String? = null,
    val selectedResponse: ResponseItemDto? = null,
    val selectedFormId: String? = null,
    val totalCount: Int = 0,
)

@HiltViewModel
class ResponsesViewModel @Inject constructor(
    private val api: SuvFormApi,
    private val selectedForm: SelectedFormStore,
    private val formRepository: com.suvojeetsengupta.suvform.data.repository.FormRepository,
    private val formDao: com.suvojeetsengupta.suvform.data.local.FormDao,
    private val auth: com.google.firebase.auth.FirebaseAuth,
) : ViewModel() {

    private val _selectedFormId = MutableStateFlow(selectedForm.formId)
    val selectedFormId = _selectedFormId.asStateFlow()

    private val _state = MutableStateFlow(
        ResponsesUiState(
            formTitle = selectedForm.formTitle ?: "Responses",
            selectedFormId = selectedForm.formId
        ),
    )
    val state: StateFlow<ResponsesUiState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val responsesPagingData: Flow<PagingData<ResponseItemDto>> = 
        _selectedFormId
            .filterNotNull()
            .flatMapLatest { formId ->
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        prefetchDistance = 20,
                        enablePlaceholders = false,
                        initialLoadSize = 50
                    ),
                    pagingSourceFactory = {
                        ResponsesPagingSource(api, formId) { total ->
                            _state.update { it.copy(totalCount = total) }
                        }
                    }
                ).flow.cachedIn(viewModelScope)
            }

    // No init refresh: the screen drives loading via LaunchedEffect, and the
    // paging flow above auto-loads from the restored selectedFormId. Avoids the
    // duplicate refresh that used to fire on both VM init and screen entry.

    fun selectForm(formId: String, title: String) {
        selectedForm.formId = formId
        selectedForm.formTitle = title
        _selectedFormId.value = formId
        _state.update {
            it.copy(
                formTitle = title,
                error = null,
                selectedFormId = formId,
                formsToSelect = emptyList(),
                totalCount = 0
            )
        }
        refresh()
    }

    fun clearSelection() {
        selectedForm.formId = null
        selectedForm.formTitle = null
        _selectedFormId.value = null
        _state.update {
            it.copy(
                formTitle = "Responses",
                formsToSelect = emptyList(),
                selectedResponse = null,
                selectedFormId = null,
                totalCount = 0
            )
        }
        refresh()
    }

    fun refresh() {
        val id = selectedForm.formId
        
        if (id != _state.value.selectedFormId) {
            _state.update { 
                it.copy(
                    selectedFormId = id, 
                    formTitle = selectedForm.formTitle ?: "Responses"
                ) 
            }
            _selectedFormId.value = id
        }

        if (id == null) {
            loadFormList()
            return
        }

        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null, formsToSelect = emptyList()) }
        viewModelScope.launch {
            // Only fetch the schema (needed for column headers / export). It comes
            // from the shared cache, so re-entering this screen is usually free.
            // The response count is reported by the paging source's first page —
            // no separate listResponses(limit=1) round-trip anymore.
            runCatching { formRepository.getForm(id) }
                .onSuccess { form ->
                    _state.update {
                        it.copy(loading = false, fields = form.fields, formTitle = form.title)
                    }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load") }
                }
        }
    }

    private fun loadFormList() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // Prefer Home's already-synced Room cache so switching to the Responses
            // tab doesn't fire a duplicate listForms() network call.
            val uid = auth.currentUser?.uid
            val cached = if (uid != null) {
                runCatching {
                    formDao.observeForOwner(uid).first()
                }.getOrNull()
            } else null

            if (!cached.isNullOrEmpty()) {
                _state.update { it.copy(loading = false, formsToSelect = cached.map { e -> e.toDto() }) }
                return@launch
            }

            runCatching { api.listForms() }
                .onSuccess { resp ->
                    _state.update { it.copy(loading = false, formsToSelect = resp.forms) }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load forms") }
                }
        }
    }

    // ---- Export ----

    /**
     * Fetches every response for a form by walking the paginated endpoint until
     * the server reports no more pages, so exports are never silently truncated.
     */
    private suspend fun fetchAllResponses(id: String): List<ResponseItemDto> {
        val pageSize = 200
        val all = mutableListOf<ResponseItemDto>()
        var offset = 0
        while (true) {
            val page = api.listResponses(id, limit = pageSize, offset = offset)
            all += page.responses
            if (!page.hasMore || page.responses.isEmpty()) break
            offset += page.responses.size
        }
        return all
    }

    fun exportCsv(context: Context) {
        val id = selectedForm.formId ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val all = fetchAllResponses(id)
                    ResponseExport.writeCsv(context, _state.value.formTitle, _state.value.fields, all)
                }
            }
                .onSuccess { file ->
                    ResponseExport.share(context, file, "text/csv", "${_state.value.formTitle} — responses")
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "CSV export failed: ${e.message}") }
                }
        }
    }

    fun exportPdf(context: Context) {
        val id = selectedForm.formId ?: return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val all = fetchAllResponses(id)
                    ResponseExport.writePdf(context, _state.value.formTitle, _state.value.fields, all)
                }
            }
                .onSuccess { file ->
                    _state.update { it.copy(exporting = false) }
                    ResponseExport.share(context, file, "application/pdf", "${_state.value.formTitle} — responses")
                }
                .onFailure { e ->
                    _state.update { it.copy(exporting = false, error = "PDF export failed: ${e.message}") }
                }
        }
    }

    fun loadInsights() {
        val id = selectedForm.formId ?: return
        if (_state.value.loadingInsights) return
        _state.update { it.copy(loadingInsights = true, insightsError = null) }
        viewModelScope.launch {
            runCatching { api.getInsights(id) }
                .onSuccess { resp ->
                    _state.update {
                        it.copy(
                            loadingInsights = false,
                            insightsSummary = resp.summary,
                        )
                    }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(loadingInsights = false, insightsError = msg ?: "Failed") }
                }
        }
    }

    fun dismissInsights() {
        _state.update { it.copy(insightsSummary = null) }
    }

    fun selectResponse(response: ResponseItemDto) {
        _state.update { it.copy(selectedResponse = response) }
    }
}
