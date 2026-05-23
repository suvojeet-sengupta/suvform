package com.suvojeetsengupta.suvform.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.remote.AdminAdminDto
import com.suvojeetsengupta.suvform.data.remote.AdminFormDto
import com.suvojeetsengupta.suvform.data.remote.AdminStatsDto
import com.suvojeetsengupta.suvform.data.remote.AdminUserDto
import com.suvojeetsengupta.suvform.data.repository.AdminRepository
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A paginated, incrementally-loaded list. */
data class PagedList<T>(
    val items: List<T> = emptyList(),
    val offset: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
)

private const val PAGE_SIZE = 25

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow<AdminStatsDto?>(null)
    val stats: StateFlow<AdminStatsDto?> = _stats.asStateFlow()

    private val _users = MutableStateFlow(PagedList<AdminUserDto>())
    val users: StateFlow<PagedList<AdminUserDto>> = _users.asStateFlow()

    private val _forms = MutableStateFlow(PagedList<AdminFormDto>())
    val forms: StateFlow<PagedList<AdminFormDto>> = _forms.asStateFlow()

    private val _admins = MutableStateFlow<List<AdminAdminDto>>(emptyList())
    val admins: StateFlow<List<AdminAdminDto>> = _admins.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _revoked = MutableStateFlow(false)
    val revoked: StateFlow<Boolean> = _revoked.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // Verify the user still has admin access (matters after revocation).
            val accessCheck = adminRepo.verifyAdminAccess()
            if (accessCheck.isFailure || accessCheck.getOrNull() == false) {
                authRepository.markAdminAccessRevoked()
                _revoked.value = true
                _error.value = "Your admin access has been revoked by the owner."
                _loading.value = false
                return@launch
            }

            adminRepo.getStats()
                .onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }

            // Reset and load first page of each list independently.
            _users.value = PagedList()
            _forms.value = PagedList()
            launch { loadUserPage(reset = true) }
            launch { loadFormPage(reset = true) }
            launch { loadAdmins() }

            _loading.value = false
        }
    }

    fun loadMoreUsers() {
        val s = _users.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch { loadUserPage(reset = false) }
    }

    fun loadMoreForms() {
        val s = _forms.value
        if (s.loadingMore || !s.hasMore) return
        viewModelScope.launch { loadFormPage(reset = false) }
    }

    private suspend fun loadUserPage(reset: Boolean) {
        val current = _users.value
        val offset = if (reset) 0 else current.offset
        if (!reset) _users.update { it.copy(loadingMore = true) }
        adminRepo.listUsers(limit = PAGE_SIZE, offset = offset)
            .onSuccess { page ->
                val merged = if (reset) page.users else current.items + page.users
                _users.value = PagedList(
                    items = merged,
                    offset = offset + page.users.size,
                    total = page.total,
                    hasMore = page.hasMore,
                    loadingMore = false,
                )
            }
            .onFailure {
                _users.update { it.copy(loadingMore = false) }
                _error.value = it.message
            }
    }

    private suspend fun loadFormPage(reset: Boolean) {
        val current = _forms.value
        val offset = if (reset) 0 else current.offset
        if (!reset) _forms.update { it.copy(loadingMore = true) }
        adminRepo.listForms(limit = PAGE_SIZE, offset = offset)
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

    fun loadAdmins() = viewModelScope.launch {
        adminRepo.listAdmins()
            .onSuccess { _admins.value = it.admins }
            .onFailure { _error.value = it.message }
    }

    fun addAdminByEmail(email: String) = viewModelScope.launch {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return@launch
        _loading.value = true
        adminRepo.addAdminByEmail(trimmed)
            .onSuccess {
                _message.value = "Admin added: $trimmed"
                loadAdmins()
            }
            .onFailure { _error.value = friendlyError(it.message) }
        _loading.value = false
    }

    fun removeAdmin(uid: String) = viewModelScope.launch {
        _loading.value = true
        adminRepo.removeAdmin(uid)
            .onSuccess {
                _message.value = "Admin removed"
                loadAdmins()
            }
            .onFailure { _error.value = friendlyError(it.message) }
        _loading.value = false
    }

    private fun friendlyError(raw: String?): String {
        val m = raw ?: return "Something went wrong"
        return when {
            m.contains("user_not_found", true) ->
                "No registered user with that email. They must sign in to the app at least once first."
            m.contains("cannot_modify_owner", true) -> "The owner account cannot be changed."
            m.contains("cannot_remove_owner", true) -> "The owner cannot be removed."
            m.contains("cannot_remove_last_admin", true) -> "You can't remove the last admin."
            else -> m
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearError() { _error.value = null }
}
