package com.example.nback.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class ScoreSortType {
    NAME_ASC,
    SCORE_DESC,
    DATE_DESC
}

/**
 * Room Data Access Object (DAO) for managing player scores.
 */
@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scores ORDER BY playerName ASC")
    fun getScoresSortedByName(): Flow<List<Score>>

    @Query("SELECT * FROM scores ORDER BY score DESC")
    fun getScoresSortedByScore(): Flow<List<Score>>

    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    fun getScoresSortedByDate(): Flow<List<Score>>

    @Query("SELECT * FROM scores")
    fun getAllScores(): Flow<List<Score>>
}
