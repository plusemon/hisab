package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.RecurringRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules WHERE userId = :userId ORDER BY id DESC")
    fun getAllRules(userId: String): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules WHERE userId = :userId AND isPaused = 0")
    suspend fun getActiveRulesList(userId: String): List<RecurringRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringRule): Long

    @Update
    suspend fun updateRule(rule: RecurringRule)

    @Query("UPDATE recurring_rules SET isPaused = :isPaused WHERE id = :id AND userId = :userId")
    suspend fun setPaused(id: Long, userId: String, isPaused: Boolean)

    @Query("UPDATE recurring_rules SET lastGeneratedDate = :lastGenerated WHERE id = :id AND userId = :userId")
    suspend fun updateLastGenerated(id: Long, userId: String, lastGenerated: Long)

    @Query("SELECT * FROM recurring_rules WHERE userId = :userId")
    suspend fun getAllRulesList(userId: String): List<RecurringRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RecurringRule>)

    @Query("DELETE FROM recurring_rules WHERE id = :id AND userId = :userId")
    suspend fun deleteRule(id: Long, userId: String)

    @Query("DELETE FROM recurring_rules WHERE userId = :userId")
    suspend fun deleteAllRulesForUser(userId: String)
}
