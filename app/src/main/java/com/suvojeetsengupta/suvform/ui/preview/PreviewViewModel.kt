package com.suvojeetsengupta.suvform.ui.preview

import androidx.lifecycle.ViewModel
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FieldType
import com.suvojeetsengupta.suvform.data.draft.FormDraftStore
import com.suvojeetsengupta.suvform.util.ExpressionEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class PreviewViewModel @Inject constructor(
    store: FormDraftStore,
) : ViewModel() {

    /** Answers keyed by field id. Strings for text/email/phone/date; numbers stored as string too. */
    private val _answers = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val answers: StateFlow<Map<String, Any?>> = _answers.asStateFlow()

    val draft = store.draft

    /** Live calculation values keyed by calculation id. */
    val calculated: StateFlow<Map<String, Double>> = combine(draft, _answers) { d, ans ->
        evaluateAll(d.fields, d.calculations, ans)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setText(fieldId: String, value: String) {
        _answers.update { it + (fieldId to value) }
    }

    fun setNumber(fieldId: String, value: String) {
        _answers.update { it + (fieldId to value) }
    }

    fun setSingleChoice(fieldId: String, value: String) {
        _answers.update { it + (fieldId to value) }
    }

    fun toggleMultiChoice(fieldId: String, option: String) {
        _answers.update { ans ->
            val current = (ans[fieldId] as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            val next = if (option in current) current - option else current + option
            ans + (fieldId to next)
        }
    }

    fun setRating(fieldId: String, value: Int) {
        _answers.update { it + (fieldId to value) }
    }

    fun setDate(fieldId: String, millis: Long?) {
        _answers.update { it + (fieldId to millis) }
    }

    fun clear() = _answers.update { emptyMap() }

    companion object {
        fun evaluateAll(
            fields: List<FieldEdit>,
            calculations: List<CalculationEdit>,
            answers: Map<String, Any?>,
        ): Map<String, Double> {
            // Build variables map: numeric fields + rating fields contribute their numeric value.
            val variables = mutableMapOf<String, Double>()
            for (f in fields) {
                val raw = answers[f.id]
                val numeric = when (f.type) {
                    FieldType.NUMBER -> (raw as? String)?.toDoubleOrNull() ?: 0.0
                    FieldType.RATING -> (raw as? Int)?.toDouble() ?: 0.0
                    else -> 0.0
                }
                variables[f.id] = numeric
            }
            val eval = ExpressionEvaluator(variables)
            return calculations.associate { c -> c.id to (eval.tryEval(c.expression) ?: 0.0) }
        }
    }
}
