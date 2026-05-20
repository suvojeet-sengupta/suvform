package com.suvojeetsengupta.suvform.ui.editor

import androidx.lifecycle.ViewModel
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FieldType
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val store: FormDraftStore,
) : ViewModel() {

    val draft = store.draft

    // ---- Form-level ----
    fun setTitle(value: String) = store.update { it.copy(title = value) }
    fun setDescription(value: String) = store.update { it.copy(description = value) }

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

    fun setFieldLabel(index: Int, label: String) = mutateField(index) { it.copy(label = label) }
    fun setFieldType(index: Int, type: FieldType) = mutateField(index) {
        // When switching away from a choice type, drop options.
        if (!type.hasOptions) it.copy(type = type, options = emptyList())
        else it.copy(type = type)
    }
    fun setFieldRequired(index: Int, required: Boolean) = mutateField(index) { it.copy(required = required) }
    fun setPlaceholder(index: Int, placeholder: String) = mutateField(index) {
        it.copy(placeholder = placeholder.takeIf { p -> p.isNotBlank() })
    }

    // ---- Options (for choice fields) ----
    fun addOption(fieldIndex: Int) = mutateField(fieldIndex) {
        it.copy(options = it.options + "Option ${it.options.size + 1}")
    }
    fun setOption(fieldIndex: Int, optionIndex: Int, value: String) = mutateField(fieldIndex) {
        it.copy(options = it.options.toMutableList().apply { this[optionIndex] = value })
    }
    fun removeOption(fieldIndex: Int, optionIndex: Int) = mutateField(fieldIndex) {
        it.copy(options = it.options.toMutableList().apply { removeAt(optionIndex) })
    }

    private inline fun mutateField(index: Int, crossinline transform: (FieldEdit) -> FieldEdit) {
        store.update { d ->
            val f = d.fields.getOrNull(index) ?: return@update d
            d.copy(fields = d.fields.toMutableList().apply { this[index] = transform(f) })
        }
    }
}
