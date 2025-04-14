package com.example.foodguard

import FoodScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodguard.ui.theme.FoodGuardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodGuardApp()
        }
    }
}

@Composable
fun FoodGuardApp() {
    FoodGuardTheme {
//        FoodScreen()
//        MainScreen()
//        BarcodeScannerScreen()
        FoodScreen()
    }
}
