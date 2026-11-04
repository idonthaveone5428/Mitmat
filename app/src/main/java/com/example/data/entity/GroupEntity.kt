package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultCurrency: String = "৳",
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null
)
