package com.suvojeetsengupta.suvform.data.draft

import javax.inject.Inject
import javax.inject.Singleton

/** Tracks which form the user clicked from Home to view responses for. */
@Singleton
class SelectedFormStore @Inject constructor() {
    var formId: String? = null
    var formTitle: String? = null
}
