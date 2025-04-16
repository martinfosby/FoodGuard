package com.example.foodguard

import FoodScreen
import BarcodeScannerScreen
import HomeScreen
import ProductInfoScreen
import RecipeGeneratorScreen
import ScannedItemsScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodguard.ui.theme.FoodGuardTheme

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

        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController)
            }
            composable("food") {
                FoodScreen(navController)
            }
            composable("scanned_items") {
                ScannedItemsScreen(navController)
            }
            composable("barcode_scanner") {
                BarcodeScannerScreen(navController = navController)
            }
            composable("product/{barcode}") { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                ProductInfoScreen(navController, barcode)
            }
            composable("recipe_generator") {
                RecipeGeneratorScreen() }

        }
    }
}

