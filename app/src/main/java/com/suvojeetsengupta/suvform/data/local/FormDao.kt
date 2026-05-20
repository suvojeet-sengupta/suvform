package com.suvojeetsengupta.suvform.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Query("SELECT * FROM form_summaries WHERE ownerUid = :uid ORDER BY updatedAt DESC")
    fun observeForOwner(uid: String): Flow<List<FormSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FormSummaryEntity>)

    @Query("DELETE FROM form_summaries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM form_summaries WHERE ownerUid = :uid")
    suspend fun deleteAllForOwner(uid: String)

    @Query("UPDATE form_summaries SET shareUrl = :url WHERE id = :id")
    suspend fun updateShareUrl(id: String, url: String)

    @Transaction
    suspend fun replaceForOwner(uid: String, items: List<FormSummaryEntity>) {
        deleteAllForOwner(uid)
        upsertAll(items)
    }
}
