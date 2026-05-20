package com.suvojeetsengupta.suvform.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val uid: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class FormSummaryDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val published: Int = 0,
    @SerialName("public_slug") val publicSlug: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class FormListDto(val forms: List<FormSummaryDto>)

@Serializable
data class CreateFormRequest(val title: String = "Untitled form", val description: String = "")

// --- AI form generation ---

@Serializable
data class GenerateFormRequest(val prompt: String, val locale: String = "en")

@Serializable
data class FieldDto(
    val id: String,
    val type: String,
    val label: String,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val placeholder: String? = null,
)

@Serializable
data class CalculationDto(
    val id: String,
    val label: String,
    val expression: String,
    val format: String? = null,
)

@Serializable
data class GeneratedFormDto(
    val title: String,
    val description: String? = null,
    val fields: List<FieldDto> = emptyList(),
    val calculations: List<CalculationDto> = emptyList(),
)

// --- Save / load ---

@Serializable
data class SaveFormRequest(
    val title: String,
    val description: String = "",
    val fields: List<FieldDto> = emptyList(),
    val calculations: List<CalculationDto> = emptyList(),
)

@Serializable
data class FormDetailDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val fields: List<FieldDto> = emptyList(),
    val calculations: List<CalculationDto> = emptyList(),
    val published: Int = 0,
    @SerialName("public_slug") val publicSlug: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class UpdateAckDto(
    val id: String,
    @SerialName("updated_at") val updatedAt: Long,
)
