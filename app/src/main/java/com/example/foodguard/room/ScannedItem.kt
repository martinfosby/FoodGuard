package com.example.foodguard.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scannedItem")
data class ScannedItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val barcode: String,
    val name: String? = null,
    val scanTimestamp: Long = System.currentTimeMillis()
)