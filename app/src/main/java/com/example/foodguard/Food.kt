package com.example.foodguard

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Int?,
    val expiry: String?
)

@Entity(tableName = "scannedItem")
data class ScannedItem(
    @PrimaryKey val id: Int = 0,
    val barcode: String
)
