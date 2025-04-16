package com.example.foodguard.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScannedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedItem(scannedItem: ScannedItem)

    @Query("SELECT * FROM scannedItem")
    suspend fun getAllScannedItem(): List<ScannedItem>

    @Query("SELECT * FROM scannedItem WHERE id = :scannedItemId")
    suspend fun getScannedItem(scannedItemId: Int): ScannedItem?

    @Query("DELETE FROM scannedItem WHERE id = :scannedItemId")
    suspend fun deleteScannedItem(scannedItemId: Int)

    @Query("SELECT * FROM scannedItem WHERE barcode = :scannedItemBarcode")
    suspend fun getScannedItemByBarcode(scannedItemBarcode: String): ScannedItem?

    @Query("SELECT * FROM scannedItem WHERE barcode = :code OR qrcode = :code LIMIT 1")
    suspend fun findByCode(code: String): ScannedItem?
}