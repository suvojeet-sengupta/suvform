package com.suvojeetsengupta.suvform.ui.responses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.local.ResponseDao
import com.suvojeetsengupta.suvform.data.local.ResponseEntity
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.util.ResponseExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

data class ResponsesUiState(
    val formTitle: String = "Responses",
    val fields: List<FieldDto> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val exporting: Boolean = false,
    val responses: List<ResponseItemDto> = emptyList(),
    val formsToSelect: List<com.suvojeetsengupta.suvform.data.remote.FormSummaryDto> = emptyList(),
    val error: String? = null,
    val loadingInsights: Boolean = false,
    val insightsSummary: String? = null,
    val insightsError: String? = null,
    val selectedResponse: ResponseItemDto? = null,
    val selectedFormId: String? = null,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
)

@HiltViewModel
class ResponsesViewModel @Inject constructor(
    private val api: SuvFormApi,
    private val selectedForm: SelectedFormStore,
    private val responseDao: ResponseDao,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ResponsesUiState(
            formTitle = selectedForm.formTitle ?: "Responses",
            selectedFormId = selectedForm.formId
        ),
    )
    val state: StateFlow<ResponsesUiState> = _state.asStateFlow()

    private var observationJob: kotlinx.coroutines.Job? = null

    init {
        refresh()
    }

    private fun startObservation(formId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            responseDao.observeForForm(formId).collectLatest { entities ->
                _state.update { it.copy(responses = entities.map { e -> e.toDto() }) }
            }
        }
    }

    fun selectForm(formId: String, title: String) {
        selectedForm.formId = formId
        selectedForm.formTitle = title
        _state.update {
            it.copy(
                formTitle = title,
                responses = emptyList(),
                error = null,
                selectedFormId = formId,
                formsToSelect = emptyList(),
                hasMore = false,
                totalCount = 0
            )
        }
        startObservation(formId)
        refresh()
    }

    fun clearSelection() {
        selectedForm.formId = null
        selectedForm.formTitle = null
        observationJob?.cancel()
        _state.update {
            it.copy(
                formTitle = "Responses",
                responses = emptyList(),
                formsToSelect = emptyList(),
                selectedResponse = null,
                selectedFormId = null,
                hasMore = false,
                totalCount = 0
            )
        }
        refresh()
    }

    fun refresh() {
        val id = selectedForm.formId
        
        // Synchronize state with store in case it changed elsewhere
        if (id != _state.value.selectedFormId) {
            _state.update { 
                it.copy(
                    selectedFormId = id, 
                    formTitle = selectedForm.formTitle ?: "Responses",
                    responses = emptyList() 
                ) 
            }
            if (id != null) startObservation(id) else observationJob?.cancel()
        }

        if (id == null) {
            loadFormList()
            return
        }

        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null, formsToSelect = emptyList()) }
        viewModelScope.launch {
            val formResult = runCatching { api.getForm(id) }
            val respResult = runCatching { api.listResponses(id, limit = 50, offset = 0) }
            respResult
                .onSuccess { resp ->
                    // Update cache (full refresh of first page replaces local data for now to stay synced)
                    responseDao.replaceForForm(id, resp.responses.map { ResponseEntity.fromDto(id, it) })
                    
                    _state.update {
                        it.copy(
                            loading = false,
                            fields = formResult.getOrNull()?.fields ?: it.fields,
                            formTitle = formResult.getOrNull()?.title ?: it.formTitle,
                            hasMore = resp.hasMore,
                            totalCount = resp.totalCount
                        )
                    }
                }
                .onFailure { e ->
                    val msg = (e as? HttpException)?.let { "HTTP ${it.code()}" } ?: e.message
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load") }
                }
        }
    }

    fun loadMore() {
        val id = selectedForm.formId ?: return
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return

        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val offset = s.responses.size
            runCatching { api.listResponses(id, limit = 50, offset = offset) }
                .onSuccess { resp ->
                    // Add to cache
                    responseDao.upsertAll(resp.responses.map { ResponseEntity.fromDto(id, it) })
                    
                    _state.update {
                        it.copy(
                            loadingMore = false,
                            hasMore = resp.hasMore,
                            totalCount = resp.totalCount
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loadingMore = false) }
                }
        }
    }

    private fun loadFormList() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
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

    fun exportCsv(context: Context) {
        val s = _state.value
        if (s.responses.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ResponseExport.writeCsv(context, s.formTitle, s.fields, s.responses)
                }
            }
                .onSuccess { file ->
                    ResponseExport.share(context, file, "text/csv", "${s.formTitle} — responses")
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "CSV export failed: ${e.message}") }
                }
        }
    }

    fun exportPdf(context: Context) {
        val s = _state.value
        if (s.responses.isEmpty()) return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ResponseExport.writePdf(context, s.formTitle, s.fields, s.responses)
                }
            }
                .onSuccess { file ->
                    _state.update { it.copy(exporting = false) }
                    ResponseExport.share(context, file, "application/pdf", "${s.formTitle} — responses")
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
