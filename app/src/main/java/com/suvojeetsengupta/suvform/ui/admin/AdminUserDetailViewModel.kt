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

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    /** Flips to true once the user is deleted so the screen can navigate back. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

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

    /** Permanently delete this user and all their forms + responses. */
    fun deleteUser() {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _deleting.value = true
            _error.value = null
            adminRepo.deleteUser(uid)
                .onSuccess { _deleted.value = true }
                .onFailure { _error.value = friendlyError(it.message) }
            _deleting.value = false
        }
    }

    private fun friendlyError(raw: String?): String {
        val m = raw ?: return "Failed to delete user"
        return when {
            m.contains("cannot_delete_owner", true) -> "The owner account cannot be deleted."
            m.contains("cannot_delete_self", true) -> "You can't delete your own account here."
            else -> m
        }
    }

    fun clearError() { _error.value = null }
}
