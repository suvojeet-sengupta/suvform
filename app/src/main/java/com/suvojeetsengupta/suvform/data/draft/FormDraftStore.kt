package com.suvojeetsengupta.suvform.data.draft

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for the form currently being edited.
 * CreateScreen writes into this, then navigates → EditorScreen reads from it.
 * Keeps the nav graph simple (no JSON-serialised route arguments).
 */
@Singleton
class FormDraftStore @Inject constructor() {
    private val _draft = MutableStateFlow(FormDraft.blank())
    val draft: StateFlow<FormDraft> = _draft.asStateFlow()

    fun set(value: FormDraft) {
        _draft.value = value
    }

    fun update(transform: (FormDraft) -> FormDraft) {
        _draft.update(transform)
    }
}
