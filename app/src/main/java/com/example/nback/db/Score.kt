package com.example.nback.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a player's saved game score in Room database.
 */
@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,
    val totalEvents: Int,
    val nValue: Int,
    val gridSize: Int,
    val timestamp: Long = System.currentTimeMillis()
)
