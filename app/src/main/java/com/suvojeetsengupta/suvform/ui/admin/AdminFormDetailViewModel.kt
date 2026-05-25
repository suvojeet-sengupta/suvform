package com.suvojeetsengupta.suvform.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.remote.AdminFormDetailDto
import com.suvojeetsengupta.suvform.data.remote.SaveFormRequest
import com.suvojeetsengupta.suvform.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminFormDetailViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val formId: String = savedStateHandle.get<String>("formId").orEmpty()

    private val _form = MutableStateFlow<AdminFormDetailDto?>(null)
    val form: StateFlow<AdminFormDetailDto?> = _form.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    // Editable draft (title, description, and responseLimit are admin-editable; fields/calcs preserved).
    private val _draftTitle = MutableStateFlow("")
    val draftTitle: StateFlow<String> = _draftTitle.asStateFlow()

    private val _draftDescription = MutableStateFlow("")
    val draftDescription: StateFlow<String> = _draftDescription.asStateFlow()

    private val _draftResponseLimit = MutableStateFlow<Int?>(null)
    val draftResponseLimit: StateFlow<Int?> = _draftResponseLimit.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    /** Flips to true once the form is deleted so the screen can navigate back. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

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
            adminRepo.getForm(formId)
                .onSuccess {
                    _form.value = it
                    _draftTitle.value = it.title
                    _draftDescription.value = it.description.orEmpty()
                    _draftResponseLimit.value = it.responseLimit
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun enterEditMode() {
        _form.value?.let {
            _draftTitle.value = it.title
            _draftDescription.value = it.description.orEmpty()
            _draftResponseLimit.value = it.responseLimit
        }
        _editMode.value = true
    }

    fun cancelEdit() {
        _editMode.value = false
        _form.value?.let {
            _draftTitle.value = it.title
            _draftDescription.value = it.description.orEmpty()
            _draftResponseLimit.value = it.responseLimit
        }
    }

    fun setTitle(v: String) { _draftTitle.value = v }
    fun setDescription(v: String) { _draftDescription.value = v }
    fun setResponseLimit(v: Int?) { _draftResponseLimit.value = if (v != null && v > 0) v else null }

    /** Persist the edit. Caller is responsible for the confirm dialog. */
    fun saveEdit() {
        val current = _form.value ?: return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            val body = SaveFormRequest(
                title = _draftTitle.value.trim().ifBlank { "Untitled form" },
                description = _draftDescription.value,
                fields = current.fields,
                calculations = current.calculations,
                responseLimit = _draftResponseLimit.value,
            )
            adminRepo.updateForm(formId, body)
                .onSuccess {
                    _message.value = "Changes saved"
                    _editMode.value = false
                    load()
                }
                .onFailure { _error.value = it.message ?: "Failed to save" }
            _saving.value = false
        }
    }

    /** Permanently delete this form and its responses. Caller handles confirmation. */
    fun deleteForm() {
        if (formId.isBlank()) return
        viewModelScope.launch {
            _deleting.value = true
            _error.value = null
            adminRepo.deleteForm(formId)
                .onSuccess { _deleted.value = true }
                .onFailure { _error.value = it.message ?: "Failed to delete form" }
            _deleting.value = false
        }
    }

    fun clearError() { _error.value = null }
    fun clearMessage() { _message.value = null }
}
