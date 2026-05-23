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

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow<AdminStatsDto?>(null)
    val stats: StateFlow<AdminStatsDto?> = _stats.asStateFlow()

    private val _users = MutableStateFlow<List<AdminUserDto>>(emptyList())
    val users: StateFlow<List<AdminUserDto>> = _users.asStateFlow()

    private val _forms = MutableStateFlow<List<AdminFormDto>>(emptyList())
    val forms: StateFlow<List<AdminFormDto>> = _forms.asStateFlow()

    private val _admins = MutableStateFlow<List<AdminAdminDto>>(emptyList())
    val admins: StateFlow<List<AdminAdminDto>> = _admins.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // First verify if user still has admin access (important after revocation)
            val accessCheck = adminRepo.verifyAdminAccess()
            if (accessCheck.isFailure || accessCheck.getOrNull() == false) {
                authRepository.markAdminAccessRevoked()
                _error.value = "Your admin access has been revoked by the owner."
                _loading.value = false
                return@launch
            }

            val statsRes = adminRepo.getStats()
            statsRes.onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }

            // Load first page of users and forms + current admins
            launch { loadUsers(0) }
            launch { loadForms(0) }
            launch { loadAdmins() }

            _loading.value = false
        }
    }

    fun loadUsers(offset: Int = 0) = viewModelScope.launch {
        adminRepo.listUsers(limit = 50, offset = offset)
            .onSuccess { _users.value = it.users }
            .onFailure { _error.value = it.message }
    }

    fun loadForms(offset: Int = 0) = viewModelScope.launch {
        adminRepo.listForms(limit = 50, offset = offset)
            .onSuccess { _forms.value = it.forms }
            .onFailure { _error.value = it.message }
    }

    fun loadAdmins() = viewModelScope.launch {
        adminRepo.listAdmins()
            .onSuccess { _admins.value = it.admins }
            .onFailure { _error.value = it.message }
    }

    fun addAdmin(uid: String) = viewModelScope.launch {
        if (uid.isBlank()) return@launch
        _loading.value = true
        adminRepo.addAdmin(uid.trim())
            .onSuccess {
                _message.value = "Admin added successfully"
                loadAdmins()
            }
            .onFailure { _error.value = it.message ?: "Failed to add admin" }
        _loading.value = false
    }

    fun removeAdmin(uid: String) = viewModelScope.launch {
        _loading.value = true
        adminRepo.removeAdmin(uid)
            .onSuccess {
                _message.value = "Admin removed"
                loadAdmins()
            }
            .onFailure { _error.value = it.message ?: "Failed to remove admin" }
        _loading.value = false
    }

    fun clearMessage() { _message.value = null }
    fun clearError() { _error.value = null }
}
