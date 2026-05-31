package com.suvojeetsengupta.suvform.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FieldType
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.data.draft.ThemeEdit
import com.suvojeetsengupta.suvform.data.remote.GenerateThemeRequest
import com.suvojeetsengupta.suvform.data.remote.SaveFormRequest
import com.suvojeetsengupta.suvform.data.repository.FormRepository
import com.suvojeetsengupta.suvform.util.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val store: FormDraftStore,
    private val formRepository: FormRepository,
) : ViewModel() {

    val draft = store.draft

    private val _save = MutableStateFlow(SaveUiState())
    val saveState: StateFlow<SaveUiState> = _save.asStateFlow()

    // ---- Form-level ----
    fun setTitle(value: String) = store.update { it.copy(title = value) }
    fun setDescription(value: String) = store.update { it.copy(description = value) }
    fun setResponseLimit(limit: Int?) = store.update { it.copy(responseLimit = if (limit != null && limit > 0) limit else null) }

    // ---- Field-level ----
    fun addField() = store.update {
        it.copy(fields = it.fields + FieldEdit(label = "New field"))
    }

    fun deleteField(index: Int) = store.update {
        it.copy(fields = it.fields.toMutableList().apply { removeAt(index) })
    }

    fun duplicateField(index: Int) = store.update { d ->
        val src = d.fields.getOrNull(index) ?: return@update d
        val copy = src.copy(
            id = "f_" + java.util.UUID.randomUUID().toString().take(8),
            label = "${src.label} (copy)",
        )
        d.copy(fields = d.fields.toMutableList().apply { add(index + 1, copy) })
    }

    fun moveUp(index: Int) = store.update { d ->
        if (index <= 0) d else d.copy(
            fields = d.fields.toMutableList().apply {
                val tmp = this[index - 1]; this[index - 1] = this[index]; this[index] = tmp
            },
        )
    }

    fun moveDown(index: Int) = store.update { d ->
        if (index >= d.fields.lastIndex) d else d.copy(
            fields = d.fields.toMutableList().apply {
                val tmp = this[index + 1]; this[index + 1] = this[index]; this[index] = tmp
            },
        )
    }

    fun setFieldLabel(index: Int, label: String) = mutateField(index) {
        if (it.label == "New field" || it.label.isBlank()) {
            it.copy(label = label, id = label.toId("f"))
        } else {
            it.copy(label = label)
        }
    }
    fun setFieldType(index: Int, type: FieldType) = mutateField(index) {
        if (!type.hasOptions) it.copy(type = type, options = emptyList())
        else it.copy(type = type)
    }
    fun setFieldRequired(index: Int, required: Boolean) = mutateField(index) { it.copy(required = required) }
    fun setPlaceholder(index: Int, placeholder: String) = mutateField(index) {
        it.copy(placeholder = placeholder.takeIf { p -> p.isNotBlank() })
    }

    // ---- Calculations ----
    fun addCalculation() = store.update {
        it.copy(calculations = it.calculations + CalculationEdit(label = "New calculation"))
    }

    fun deleteCalculation(index: Int) = store.update {
        it.copy(calculations = it.calculations.toMutableList().apply { removeAt(index) })
    }

    fun setCalculationLabel(index: Int, label: String) = mutateCalculation(index) {
        if (it.label == "New calculation" || it.label.isBlank()) {
            it.copy(label = label, id = label.toId("c"))
        } else {
            it.copy(label = label)
        }
    }
    fun setCalculationExpression(index: Int, expr: String) = mutateCalculation(index) { it.copy(expression = expr) }
    fun setCalculationFormat(index: Int, format: String?) = mutateCalculation(index) { it.copy(format = format) }

    // ---- Theme ----
    private val _themeLoading = MutableStateFlow(false)
    val themeLoading: StateFlow<Boolean> = _themeLoading.asStateFlow()

    fun generateTheme(prompt: String) {
        if (prompt.isBlank() || _themeLoading.value) return
        _themeLoading.value = true
        viewModelScope.launch {
            runCatching { formRepository.generateTheme(GenerateThemeRequest(prompt)) }
                .onSuccess { themeDto ->
                    store.update { it.copy(theme = ThemeEdit.fromDto(themeDto)) }
                }
                .onFailure { e ->
                    _save.value = SaveUiState(error = ErrorMapper.message(e))
                }
            _themeLoading.value = false
        }
    }

    // ---- Options ----
    fun addOption(fieldIndex: Int) = mutateField(fieldIndex) {
        it.copy(options = it.options + "Option ${it.options.size + 1}")
    }
    fun setOption(fieldIndex: Int, optionIndex: Int, value: String) = mutateField(fieldIndex) {
        it.copy(options = it.options.toMutableList().apply { this[optionIndex] = value })
    }
    fun removeOption(fieldIndex: Int, optionIndex: Int) = mutateField(fieldIndex) {
        it.copy(options = it.options.toMutableList().apply { removeAt(optionIndex) })
    }

    // ---- Save ----
    fun save(onSuccess: (() -> Unit)? = null) {
        if (_save.value.saving) return
        val d = store.draft.value
        if (d.fields.isEmpty()) {
            _save.value = SaveUiState(error = "Add at least one field before saving.")
            return
        }
        _save.value = SaveUiState(saving = true)
        viewModelScope.launch {
            val req = SaveFormRequest(
                title = d.title.ifBlank { "Untitled form" },
                description = d.description,
                fields = d.fields.map { it.toDto() },
                calculations = d.calculations.map { it.toDto() },
                theme = d.theme?.toDto(),
                responseLimit = d.responseLimit,
            )
            runCatching {
                if (d.remoteId == null) formRepository.createForm(req)
                else {
                    formRepository.updateForm(d.remoteId, req)  // invalidates cache internally
                    null  // detail not returned by PUT — we already have it
                }
            }
                .onSuccess { detail ->
                    if (detail != null) store.update { it.copy(remoteId = detail.id) }
                    _save.value = SaveUiState(saved = true)
                    onSuccess?.invoke()
                }
                .onFailure { e ->
                    _save.value = SaveUiState(error = ErrorMapper.message(e))
                }
        }
    }

    fun saveAndPublish() {
        save {
            publish()
        }
    }

    fun consumeSaved() {
        _save.value = SaveUiState()
    }

    // ---- Publish / unpublish ----
    fun publish() {
        if (_save.value.saving) return
        val d = store.draft.value
        val id = d.remoteId
        if (id == null) {
            _save.value = SaveUiState(error = "Save the form first, then publish.")
            return
        }
        _save.value = SaveUiState(saving = true)
        viewModelScope.launch {
            runCatching { formRepository.publish(id) }
                .onSuccess { r ->
                    store.update { it.copy(published = true, publicSlug = r.slug, shareUrl = r.url) }
                    _save.value = SaveUiState(published = true)
                }
                .onFailure { e ->
                    _save.value = SaveUiState(error = ErrorMapper.message(e))
                }
        }
    }

    fun unpublish() {
        if (_save.value.saving) return
        val id = store.draft.value.remoteId ?: return
        _save.value = SaveUiState(saving = true)
        viewModelScope.launch {
            runCatching { formRepository.unpublish(id) }
                .onSuccess {
                    store.update { it.copy(published = false) }
                    _save.value = SaveUiState(unpublished = true)
                }
                .onFailure { e ->
                    _save.value = SaveUiState(error = ErrorMapper.message(e))
                }
        }
    }

    private inline fun mutateField(index: Int, crossinline transform: (FieldEdit) -> FieldEdit) {
        store.update { d ->
            val f = d.fields.getOrNull(index) ?: return@update d
            d.copy(fields = d.fields.toMutableList().apply { this[index] = transform(f) })
        }
    }

    private inline fun mutateCalculation(index: Int, crossinline transform: (CalculationEdit) -> CalculationEdit) {
        store.update { d ->
            val c = d.calculations.getOrNull(index) ?: return@update d
            d.copy(calculations = d.calculations.toMutableList().apply { this[index] = transform(c) })
        }
    }

    private fun String.toId(prefix: String): String {
        val slug = this.lowercase()
            .replace(NON_ALNUM, "_")
            .replace(MULTI_UNDERSCORE, "_")
            .trim('_')
        return if (slug.isEmpty()) "${prefix}_${java.util.UUID.randomUUID().toString().take(8)}"
        else "${prefix}_$slug"
    }

    private companion object {
        private val NON_ALNUM = Regex("[^a-z0-9]")
        private val MULTI_UNDERSCORE = Regex("_+")
    }
}

data class SaveUiState(
    val saving: Boolean = false,
    val saved: Boolean = false,
    val published: Boolean = false,
    val unpublished: Boolean = false,
    val error: String? = null,
)
