package com.suvojeetsengupta.suvform.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "form_responses")
data class ResponseEntity(
    @PrimaryKey val id: String,
    val formId: String,
    val submittedAt: Long,
    val answersJson: String,
    val calculatedJson: String,
) {
    fun toDto(): ResponseItemDto {
        val json = Json { ignoreUnknownKeys = true }
        return ResponseItemDto(
            id = id,
            submittedAt = submittedAt,
            answers = json.decodeFromString(answersJson),
            calculated = json.decodeFromString(calculatedJson)
        )
    }

    companion object {
        fun fromDto(formId: String, dto: ResponseItemDto): ResponseEntity {
            val json = Json { ignoreUnknownKeys = true }
            return ResponseEntity(
                id = dto.id,
                formId = formId,
                submittedAt = dto.submittedAt,
                answersJson = json.encodeToString(dto.answers),
                calculatedJson = json.encodeToString(dto.calculated)
            )
        }
    }
}
