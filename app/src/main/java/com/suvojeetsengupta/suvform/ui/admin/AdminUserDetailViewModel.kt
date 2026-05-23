package com.suvojeetsengupta.suvform.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.remote.AdminFormDto
import com.suvojeetsengupta.suvform.data.remote.AdminUserDetailDto
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
class AdminUserDetailViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val uid: String = savedStateHandle.get<String>("uid").orEmpty()

    private val _user = MutableStateFlow<AdminUserDetailDto?>(null)
    val user: StateFlow<AdminUserDetailDto?> = _user.asStateFlow()

    private val _forms = MutableStateFlow(PagedList<AdminFormDto>())
    val forms: StateFlow<PagedList<AdminFormDto>> = _forms.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (uid.isBlank()) {
            _error.value = "Missing user id"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            adminRepo.getUser(uid)
                .onSuccess { _user.value = it }
                .onFailure { _error.value = it.message }
            _forms.value = PagedList()
            loadFormPage(reset = true)
            _loading.value = false
        }
    }

    fun loadMoreForms() {
        val s = _forms.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch { loadFormPage(reset = false) }
    }

    private suspend fun loadFormPage(reset: Boolean) {
        val current = _forms.value
        val offset = if (reset) 0 else current.offset
        if (!reset) _forms.update { it.copy(loadingMore = true) }
        adminRepo.listUserForms(uid, limit = PAGE_SIZE, offset = offset)
            .onSuccess { page ->
                val merged = if (reset) page.forms else current.items + page.forms
                _forms.value = PagedList(
                    items = merged,
                    offset = offset + page.forms.size,
                    total = page.total,
                    hasMore = page.hasMore,
                    loadingMore = false,
                )
            }
            .onFailure {
                _forms.update { it.copy(loadingMore = false) }
                _error.value = it.message
            }
    }

    fun clearError() { _error.value = null }
}
