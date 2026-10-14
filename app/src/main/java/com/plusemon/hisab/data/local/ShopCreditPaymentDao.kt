package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plusemon.hisab.data.model.ShopCreditPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopCreditPaymentDao {
    @Query("SELECT * FROM shop_credit_payments WHERE userId = :userId ORDER BY dateTimestamp DESC")
    fun getAllPayments(userId: String): Flow<List<ShopCreditPayment>>

    @Query("SELECT * FROM shop_credit_payments WHERE vendorId = :vendorId AND userId = :userId ORDER BY dateTimestamp DESC")
    fun getPaymentsForVendor(vendorId: Long, userId: String): Flow<List<ShopCreditPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: ShopCreditPayment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<ShopCreditPayment>)

    @Query("SELECT * FROM shop_credit_payments WHERE userId = :userId")
    suspend fun getAllPaymentsList(userId: String): List<ShopCreditPayment>

    @Query("DELETE FROM shop_credit_payments WHERE id = :id AND userId = :userId")
    suspend fun deletePayment(id: Long, userId: String)

    @Query("DELETE FROM shop_credit_payments WHERE userId = :userId")
    suspend fun deleteAllPaymentsForUser(userId: String)
}
