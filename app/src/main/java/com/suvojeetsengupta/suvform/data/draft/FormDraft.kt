package com.suvojeetsengupta.suvform.data.draft

import com.suvojeetsengupta.suvform.data.remote.CalculationDto
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.GeneratedFormDto
import java.util.UUID

/** Mutable form draft used by the editor. Maps to / from the DTOs the API uses. */
data class FormDraft(
    val remoteId: String? = null,        // backend form id once saved
    val title: String = "Untitled form",
    val description: String = "",
    val fields: List<FieldEdit> = emptyList(),
    val calculations: List<CalculationEdit> = emptyList(),
    val published: Boolean = false,
    val publicSlug: String? = null,
    val shareUrl: String? = null,
    val responseLimit: Int? = null,      // null or 0 = unlimited
) {
    companion object {
        fun blank() = FormDraft(
            title = "Untitled form",
            fields = listOf(FieldEdit(label = "Your name", type = FieldType.SHORT_TEXT)),
            responseLimit = null,
        )

        fun fromGenerated(g: GeneratedFormDto) = FormDraft(
            title = g.title.ifBlank { "Untitled form" },
            description = g.description.orEmpty(),
            fields = g.fields.map { FieldEdit.fromDto(it) },
            calculations = g.calculations.map { CalculationEdit.fromDto(it) },
            responseLimit = null,
        )
    }
}

data class FieldEdit(
    val id: String = "f_${UUID.randomUUID().toString().take(8)}",
    val type: FieldType = FieldType.SHORT_TEXT,
    val label: String = "Untitled field",
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val placeholder: String? = null,
) {
    fun toDto() = FieldDto(
        id = id,
        type = type.key,
        label = label,
        required = required,
        options = if (type.hasOptions) options else emptyList(),
        placeholder = placeholder,
    )

    companion object {
        fun fromDto(d: FieldDto) = FieldEdit(
            id = d.id,
            type = FieldType.fromKey(d.type),
            label = d.label,
            required = d.required,
            options = d.options,
            placeholder = d.placeholder,
        )
    }
}

data class CalculationEdit(
    val id: String = "c_${UUID.randomUUID().toString().take(8)}",
    val label: String = "",
    val expression: String = "",
    val format: String? = null,
) {
    fun toDto() = CalculationDto(id = id, label = label, expression = expression, format = format)

    companion object {
        fun fromDto(d: CalculationDto) =
            CalculationEdit(id = d.id, label = d.label, expression = d.expression, format = d.format)
    }
}

enum class FieldType(val key: String, val display: String, val emoji: String, val hasOptions: Boolean = false) {
    SHORT_TEXT("short_text", "Short text", "Aa"),
    LONG_TEXT("long_text", "Long text", "¶"),
    NUMBER("number", "Number", "#"),
    EMAIL("email", "Email", "✉"),
    PHONE("phone", "Phone", "☏"),
    SINGLE_CHOICE("single_choice", "Single choice", "◉", hasOptions = true),
    MULTI_CHOICE("multi_choice", "Multi choice", "☑", hasOptions = true),
    DATE("date", "Date", "📅"),
    RATING("rating", "Rating", "★");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SHORT_TEXT
    }
}
