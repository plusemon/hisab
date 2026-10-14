package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoals(userId: String): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGoalById(id: Long, userId: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Query("UPDATE savings_goals SET currentSavedAmount = :amount, isCompleted = :isCompleted WHERE id = :id AND userId = :userId")
    suspend fun updateSavedAmount(id: Long, userId: String, amount: Double, isCompleted: Boolean)

    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    suspend fun getAllGoalsList(userId: String): List<SavingsGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SavingsGoal>)

    @Query("DELETE FROM savings_goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoal(id: Long, userId: String)

    @Query("DELETE FROM savings_goals WHERE userId = :userId")
    suspend fun deleteAllGoalsForUser(userId: String)
}
