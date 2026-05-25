package com.suvojeetsengupta.suvform.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 25

@HiltViewModel
class AdminFormResponsesViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val formId: String = savedStateHandle.get<String>("formId").orEmpty()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _fields = MutableStateFlow<List<FieldDto>>(emptyList())
    val fields: StateFlow<List<FieldDto>> = _fields.asStateFlow()

    private val _responses = MutableStateFlow(PagedList<ResponseItemDto>())
    val responses: StateFlow<PagedList<ResponseItemDto>> = _responses.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (formId.isBlank()) {
            _error.value = "Missing form id"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            // Load form once for the title and field labels.
            adminRepo.getForm(formId)
                .onSuccess {
                    _title.value = it.title
                    _fields.value = it.fields
                }
                .onFailure { _error.value = it.message }
            _responses.value = PagedList()
            loadPage(reset = true)
            _loading.value = false
        }
    }

    fun loadMore() {
        val s = _responses.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch { loadPage(reset = false) }
    }

    private suspend fun loadPage(reset: Boolean) {
        val current = _responses.value
        val offset = if (reset) 0 else current.offset
        if (!reset) _responses.update { it.copy(loadingMore = true) }
        adminRepo.listFormResponses(formId, limit = PAGE_SIZE, offset = offset)
            .onSuccess { page ->
                val merged = if (reset) page.responses else current.items + page.responses
                _responses.value = PagedList(
                    items = merged,
                    offset = offset + page.responses.size,
                    total = page.totalCount,
                    hasMore = page.hasMore,
                    loadingMore = false,
                )
            }
            .onFailure {
                _responses.update { it.copy(loadingMore = false) }
                _error.value = it.message
            }
    }

    fun clearError() { _error.value = null }

    // ---- Selection & Deletion ----

    fun toggleSelection(id: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) current - id else current + id
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        if (formId.isBlank() || _selectedIds.value.isEmpty()) return
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            _deleting.value = true
            adminRepo.adminDeleteResponses(formId, ids)
                .onSuccess {
                    _selectedIds.value = emptySet()
                    load() // Full reload for simplicity
                }
                .onFailure { _error.value = it.message ?: "Delete failed" }
            _deleting.value = false
        }
    }

    fun deleteAll() {
        if (formId.isBlank()) return
        viewModelScope.launch {
            _deleting.value = true
            adminRepo.adminDeleteAllResponses(formId)
                .onSuccess {
                    _selectedIds.value = emptySet()
                    load()
                }
                .onFailure { _error.value = it.message ?: "Delete all failed" }
            _deleting.value = false
        }
    }
}
