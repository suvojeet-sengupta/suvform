package com.suvojeetsengupta.suvform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FormSummaryEntity::class, ResponseEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class SuvFormDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun responseDao(): ResponseDao
}
