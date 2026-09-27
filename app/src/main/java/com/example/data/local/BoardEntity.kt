package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val boardColorArgb: Int = 0xFFFFFFFF.toInt(),
    val boardTexture: String = "PLAIN",
    val dataJson: String = "",
    val thumbnailBase64: String? = null
)
