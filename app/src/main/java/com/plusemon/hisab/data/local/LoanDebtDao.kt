package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.DebtType
import com.plusemon.hisab.data.model.LoanDebt
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDebtDao {
    @Query("SELECT * FROM loans_debts WHERE userId = :userId ORDER BY isSettled ASC, createdAt DESC")
    fun getAllDebts(userId: String): Flow<List<LoanDebt>>

    @Query("SELECT * FROM loans_debts WHERE userId = :userId AND type = :type ORDER BY isSettled ASC, createdAt DESC")
    fun getDebtsByType(userId: String, type: DebtType): Flow<List<LoanDebt>>

    @Query("SELECT * FROM loans_debts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getDebtById(id: Long, userId: String): LoanDebt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: LoanDebt): Long

    @Update
    suspend fun updateDebt(debt: LoanDebt)

    @Query("UPDATE loans_debts SET paidAmount = :paidAmount, isSettled = :isSettled WHERE id = :id AND userId = :userId")
    suspend fun recordRepayment(id: Long, userId: String, paidAmount: Double, isSettled: Boolean)

    @Query("SELECT * FROM loans_debts WHERE userId = :userId")
    suspend fun getAllDebtsList(userId: String): List<LoanDebt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<LoanDebt>)

    @Query("DELETE FROM loans_debts WHERE id = :id AND userId = :userId")
    suspend fun deleteDebt(id: Long, userId: String)

    @Query("DELETE FROM loans_debts WHERE userId = :userId")
    suspend fun deleteAllDebtsForUser(userId: String)
}
