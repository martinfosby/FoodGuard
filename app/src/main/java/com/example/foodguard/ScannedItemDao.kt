package com.example.foodguard

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface ScannedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedItem(food: Food)

    @Query("SELECT * FROM scannedItem")
    suspend fun getAllScannedItem(): List<Food>

    @Query("SELECT * FROM scannedItem WHERE id = :scannedItemId")
    suspend fun getScannedItem(scannedItemId: Int)

    @Query("DELETE FROM scannedItem WHERE id = :scannedItemId")
    suspend fun deleteFood(scannedItemId: Int)

    @Query("SELECT * FROM scannedItem WHERE barcode = :scannedItemBarcode")
    suspend fun getScannedItemByBarcode(scannedItemBarcode: String)
}