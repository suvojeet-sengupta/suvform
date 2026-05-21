package com.suvojeetsengupta.suvform.ui.responses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.suvojeetsengupta.suvform.data.draft.SelectedFormStore
import com.suvojeetsengupta.suvform.data.remote.CalculationDto
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.data.repository.FormRepository
import com.suvojeetsengupta.suvform.data.repository.ResponseRepository
import com.suvojeetsengupta.suvform.util.ErrorMapper
import com.suvojeetsengupta.suvform.util.ResponseExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val calculations: List<CalculationDto> = emptyList(),
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
    private val selectedForm: SelectedFormStore,
    private val formRepository: FormRepository,
    private val responseRepository: ResponseRepository,
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
                responseRepository.pagingFlow(formId, viewModelScope) { total ->
                    _state.update { it.copy(totalCount = total) }
                }
            }

    // No init refresh: the screen drives loading via LaunchedEffect, and the
    // paging flow above auto-loads from the restored selectedFormId.

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
            // The response count is reported by the paging source's first page.
            runCatching { formRepository.getForm(id) }
                .onSuccess { form ->
                    _state.update {
                        it.copy(
                            loading = false,
                            fields = form.fields,
                            calculations = form.calculations,
                            formTitle = form.title,
                        )
                    }
                }
                .onFailure { e ->
                    val msg = ErrorMapper.message(e)
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load") }
                }
        }
    }

    private fun loadFormList() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // Offline-first: prefer the Room-cached list (shared with Home).
            val cached = runCatching { formRepository.observeForms().first() }.getOrNull()
            if (!cached.isNullOrEmpty()) {
                _state.update { it.copy(loading = false, formsToSelect = cached.map { e -> e.toDto() }) }
                return@launch
            }
            // Nothing cached — sync from the network.
            runCatching {
                formRepository.syncForms(force = true)
                formRepository.observeForms().first()
            }
                .onSuccess { forms ->
                    _state.update { it.copy(loading = false, formsToSelect = forms.map { e -> e.toDto() }) }
                }
                .onFailure { e ->
                    val msg = ErrorMapper.message(e)
                    _state.update { it.copy(loading = false, error = msg ?: "Failed to load forms") }
                }
        }
    }

    // ---- Export ----

    fun exportCsv(context: Context) {
        val id = selectedForm.formId ?: return
        viewModelScope.launch {
            runCatching {
                val all = responseRepository.fetchAllResponses(id)
                withContext(Dispatchers.IO) {
                    ResponseExport.writeCsv(
                        context,
                        _state.value.formTitle,
                        _state.value.fields,
                        _state.value.calculations,
                        all,
                    )
                }
            }
                .onSuccess { file ->
                    ResponseExport.share(context, file, "text/csv", "${_state.value.formTitle} — responses")
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "CSV export failed: ${ErrorMapper.message(e)}") }
                }
        }
    }

    fun exportPdf(context: Context) {
        val id = selectedForm.formId ?: return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            runCatching {
                val all = responseRepository.fetchAllResponses(id)
                withContext(Dispatchers.IO) {
                    ResponseExport.writePdf(
                        context,
                        _state.value.formTitle,
                        _state.value.fields,
                        _state.value.calculations,
                        all,
                    )
                }
            }
                .onSuccess { file ->
                    _state.update { it.copy(exporting = false) }
                    ResponseExport.share(context, file, "application/pdf", "${_state.value.formTitle} — responses")
                }
                .onFailure { e ->
                    _state.update { it.copy(exporting = false, error = "PDF export failed: ${ErrorMapper.message(e)}") }
                }
        }
    }

    fun loadInsights() {
        val id = selectedForm.formId ?: return
        if (_state.value.loadingInsights) return
        _state.update { it.copy(loadingInsights = true, insightsError = null) }
        viewModelScope.launch {
            runCatching { responseRepository.getInsights(id) }
                .onSuccess { resp ->
                    _state.update {
                        it.copy(
                            loadingInsights = false,
                            insightsSummary = resp.summary,
                        )
                    }
                }
                .onFailure { e ->
                    val msg = ErrorMapper.message(e)
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
