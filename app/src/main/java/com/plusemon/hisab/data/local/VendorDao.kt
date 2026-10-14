package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.Vendor
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors WHERE userId = :userId ORDER BY name ASC")
    fun getAllVendors(userId: String): Flow<List<Vendor>>

    @Query("SELECT * FROM vendors WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getVendorById(id: Long, userId: String): Vendor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: Vendor): Long

    @Update
    suspend fun updateVendor(vendor: Vendor)

    @Query("UPDATE vendors SET isArchived = :isArchived WHERE id = :vendorId AND userId = :userId")
    suspend fun archiveVendor(vendorId: Long, userId: String, isArchived: Boolean)

    @Query("SELECT * FROM vendors WHERE userId = :userId")
    suspend fun getAllVendorsList(userId: String): List<Vendor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendors(vendors: List<Vendor>)

    @Query("DELETE FROM vendors WHERE id = :id AND userId = :userId")
    suspend fun deleteVendor(id: Long, userId: String)

    @Query("DELETE FROM vendors WHERE userId = :userId")
    suspend fun deleteAllVendorsForUser(userId: String)
}
