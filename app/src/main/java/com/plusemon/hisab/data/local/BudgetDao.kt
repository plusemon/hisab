package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    fun getBudgetsForMonth(userId: String, monthYear: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND categoryId = :categoryId LIMIT 1")
    suspend fun getCategoryBudget(userId: String, monthYear: String, categoryId: Long): Budget?

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND categoryId IS NULL LIMIT 1")
    suspend fun getOverallBudget(userId: String, monthYear: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: Budget): Long

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getAllBudgetsList(userId: String): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Query("DELETE FROM budgets WHERE id = :id AND userId = :userId")
    suspend fun deleteBudget(id: Long, userId: String)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllBudgetsForUser(userId: String)
}
