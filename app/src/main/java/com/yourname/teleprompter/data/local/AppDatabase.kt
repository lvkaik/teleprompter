package com.yourname.teleprompter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourname.teleprompter.data.local.entity.ScriptEntity

@Database(entities = [ScriptEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
}