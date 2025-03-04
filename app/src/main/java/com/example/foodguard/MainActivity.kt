package com.example.foodguard

import BarcodeScannerScreen
import MainScreen
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
        BarcodeScannerScreen()
    }
}

@Composable
fun FoodScreen() {
    var foodName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Enter food name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                // Call function to insert food (Implement Room integration here)
            }
        }) {
            Text("Save Food")
        }
    }
}
