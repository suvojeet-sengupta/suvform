package com.suvojeetsengupta.suvform.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto

@Entity(tableName = "form_summaries")
data class FormSummaryEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val title: String,
    val description: String?,
    val published: Int,
    val publicSlug: String?,
    val shareUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDto() = FormSummaryDto(
        id = id,
        title = title,
        description = description,
        published = published,
        publicSlug = publicSlug,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDto(ownerUid: String, dto: FormSummaryDto) = FormSummaryEntity(
            id = dto.id,
            ownerUid = ownerUid,
            title = dto.title,
            description = dto.description,
            published = dto.published,
            publicSlug = dto.publicSlug,
            shareUrl = null, // Primes on first fetch/open
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
        )
    }
}
