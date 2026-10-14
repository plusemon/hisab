package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.ShopCreditPurchase
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopCreditPurchaseDao {
    @Query("SELECT * FROM shop_credit_purchases WHERE userId = :userId ORDER BY isSettled ASC, dateTimestamp DESC")
    fun getAllPurchases(userId: String): Flow<List<ShopCreditPurchase>>

    @Query("SELECT * FROM shop_credit_purchases WHERE vendorId = :vendorId AND userId = :userId ORDER BY isSettled ASC, dateTimestamp DESC")
    fun getPurchasesForVendor(vendorId: Long, userId: String): Flow<List<ShopCreditPurchase>>

    @Query("SELECT * FROM shop_credit_purchases WHERE vendorId = :vendorId AND userId = :userId AND isSettled = 0 ORDER BY dateTimestamp ASC")
    suspend fun getUnsettledPurchasesForVendorSync(vendorId: Long, userId: String): List<ShopCreditPurchase>

    @Query("SELECT * FROM shop_credit_purchases WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getPurchaseById(id: Long, userId: String): ShopCreditPurchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: ShopCreditPurchase): Long

    @Update
    suspend fun updatePurchase(purchase: ShopCreditPurchase)

    @Query("UPDATE shop_credit_purchases SET paidAmount = :paidAmount, isSettled = :isSettled WHERE id = :id")
    suspend fun updatePaidAmount(id: Long, paidAmount: Double, isSettled: Boolean)

    @Query("SELECT * FROM shop_credit_purchases WHERE userId = :userId")
    suspend fun getAllPurchasesList(userId: String): List<ShopCreditPurchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<ShopCreditPurchase>)

    @Query("DELETE FROM shop_credit_purchases WHERE id = :id AND userId = :userId")
    suspend fun deletePurchase(id: Long, userId: String)

    @Query("DELETE FROM shop_credit_purchases WHERE userId = :userId")
    suspend fun deleteAllPurchasesForUser(userId: String)
}
