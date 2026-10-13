package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plusemon.hisab.data.model.LoanRepayment
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanRepaymentDao {
    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY dateTimestamp DESC")
    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepayment>>

    @Query("SELECT * FROM loan_repayments WHERE userId = :userId ORDER BY dateTimestamp DESC")
    fun getAllRepaymentsForUser(userId: String): Flow<List<LoanRepayment>>

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId")
    suspend fun getRepaymentsForLoanSync(loanId: Long): List<LoanRepayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepayment): Long

    @Query("DELETE FROM loan_repayments WHERE id = :id")
    suspend fun deleteRepayment(id: Long)

    @Query("DELETE FROM loan_repayments WHERE userId = :userId")
    suspend fun deleteAllRepaymentsForUser(userId: String)
}
