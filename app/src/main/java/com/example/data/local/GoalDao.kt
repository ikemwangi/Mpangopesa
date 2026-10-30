package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM monthly_goals WHERE monthKey = :monthKey LIMIT 1")
    fun getGoalForMonth(monthKey: String): Flow<MonthlyGoalEntity?>

    @Query("SELECT * FROM monthly_goals ORDER BY monthKey DESC LIMIT 1")
    fun getLatestGoal(): Flow<MonthlyGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGoal(goal: MonthlyGoalEntity)

    @Query("SELECT targetAmount FROM monthly_goals WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getTargetAmountDirect(monthKey: String): Double?
}
