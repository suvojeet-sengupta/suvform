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
