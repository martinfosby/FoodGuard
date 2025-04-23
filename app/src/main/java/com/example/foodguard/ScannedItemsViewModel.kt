package com.example.foodguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodguard.room.AppDatabase
import com.example.foodguard.room.ScannedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScannedItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val scannedItemDao = database.scannedItemDao()

    // Using MutableStateFlow to store scanned items
    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems: StateFlow<List<ScannedItem>> = _scannedItems

    var selectedItem = MutableStateFlow<ScannedItem?>(null)

    init {
        // Fetch scanned items from the database when the ViewModel is created
        loadScannedItems()
    }

    fun loadScannedItems() {
        viewModelScope.launch {
            // Simulate loading data from the database
            _scannedItems.value = scannedItemDao.getAllScannedItems()
        }
    }
}
