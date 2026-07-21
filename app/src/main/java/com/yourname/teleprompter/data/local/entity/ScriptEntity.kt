package com.yourname.teleprompter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long,
    val tags: String = "",
    val fontSizeSp: Float = 22f,
    val colorHex: String = "#FFFFFF",
    val bgColorHex: String = "#CC000000",
    val speedPxPerSec: Float = 30f,
    val readPosition: Int = 0
)