package com.suvojeetsengupta.suvform.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponseDao {
    @Query("SELECT * FROM form_responses WHERE formId = :formId ORDER BY submittedAt DESC")
    fun observeForForm(formId: String): Flow<List<ResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ResponseEntity>)

    @Query("DELETE FROM form_responses WHERE formId = :formId")
    suspend fun deleteAllForForm(formId: String)

    @Transaction
    suspend fun replaceForForm(formId: String, items: List<ResponseEntity>) {
        deleteAllForForm(formId)
        upsertAll(items)
    }
}
