package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveAccounts(userId: String): Flow<List<UserAccount>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllAccounts(userId: String): Flow<List<UserAccount>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getAllAccountsList(userId: String): List<UserAccount>

    @Query("SELECT * FROM accounts WHERE id = :accountId AND userId = :userId LIMIT 1")
    suspend fun getAccountById(accountId: Long, userId: String): UserAccount?

    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId")
    suspend fun getAccountCount(userId: String): Int

    @Query("SELECT * FROM accounts WHERE userId = :userId AND isArchived = 0 LIMIT 1")
    suspend fun getFirstActiveAccount(userId: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<UserAccount>)

    @Update
    suspend fun updateAccount(account: UserAccount)

    @Query("UPDATE accounts SET isArchived = :isArchived WHERE id = :accountId AND userId = :userId")
    suspend fun setArchived(accountId: Long, userId: String, isArchived: Boolean)

    @Query("DELETE FROM accounts WHERE id = :accountId AND userId = :userId")
    suspend fun deleteAccount(accountId: Long, userId: String)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteAllAccountsForUser(userId: String)
}
