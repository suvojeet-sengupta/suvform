package com.suvojeetsengupta.suvform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FormSummaryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SuvFormDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
}
