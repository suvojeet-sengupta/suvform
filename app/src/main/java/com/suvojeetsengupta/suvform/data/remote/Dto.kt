package com.suvojeetsengupta.suvform.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val uid: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
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

// --- Publish / responses / insights ---

@Serializable
data class PublishResponse(
    val slug: String,
    val url: String,
    val published: Int,
)

@Serializable
data class ResponseItemDto(
    val id: String,
    @SerialName("submitted_at") val submittedAt: Long,
    val answers: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val calculated: Map<String, Double> = emptyMap(),
)

@Serializable
data class ResponsesListDto(
    val responses: List<ResponseItemDto> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class InsightsDto(
    val summary: String,
    @SerialName("response_count") val responseCount: Int = 0,
)

// ===================== ADMIN DTOs =====================

@Serializable
data class AdminStatsDto(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_forms") val totalForms: Int = 0,
    @SerialName("published_forms") val publishedForms: Int = 0,
    @SerialName("total_responses") val totalResponses: Int = 0,
    @SerialName("total_admins") val totalAdmins: Int = 0,
)

@Serializable
data class AdminUserDto(
    val uid: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class AdminUsersDto(
    val users: List<AdminUserDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class AdminFormDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val published: Int = 0,
    @SerialName("public_slug") val publicSlug: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("owner_uid") val ownerUid: String = "",
    @SerialName("owner_email") val ownerEmail: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
)

@Serializable
data class AdminFormsDto(
    val forms: List<AdminFormDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class AdminAdminDto(
    val uid: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("added_by") val addedBy: String,
    val role: String = "admin",
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class AdminAdminsDto(
    val admins: List<AdminAdminDto> = emptyList(),
)

@Serializable
data class AdminAddRequest(
    val uid: String? = null,
    val email: String? = null,
)

@Serializable
data class AdminUserDetailDto(
    val uid: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("created_at_str") val createdAtStr: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    val role: String? = null,
    @SerialName("total_forms") val totalForms: Int = 0,
    @SerialName("published_forms") val publishedForms: Int = 0,
    @SerialName("total_responses") val totalResponses: Int = 0,
)

@Serializable
data class AdminFormDetailDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val fields: List<FieldDto> = emptyList(),
    val calculations: List<CalculationDto> = emptyList(),
    val published: Int = 0,
    @SerialName("public_slug") val publicSlug: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("updated_at_str") val updatedAtStr: String? = null,
    @SerialName("owner_uid") val ownerUid: String = "",
    @SerialName("owner_email") val ownerEmail: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("total_responses") val totalResponses: Int = 0,
)
