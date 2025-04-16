import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodguard.ktor.KtorApi
import com.example.foodguard.ktor.Product
import com.example.foodguard.llminference.MLCBridge
import com.example.foodguard.room.AppDatabase
import com.example.foodguard.room.ScannedItem
import com.example.foodguard.room.ScannedItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(navController: NavController) {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val database = AppDatabase.getDatabase(LocalContext.current)
    val scannedItemDao = database.scannedItemDao()
    var storedInDb by remember { mutableStateOf(false) }
    var existInDb by remember { mutableStateOf(false) }


    // Barcode data state
    var barcodeData by remember { mutableStateOf<String?>(null) }

    // Request camera permission
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        // Set up the CameraX preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )

                        // Set up barcode or qr scanning
                        val analysisUseCase = ImageAnalysis.Builder().build()
                        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val barcodeScanner = BarcodeScanning.getClient()
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        // Update barcode data state if a barcode is detected
                                        if (barcodes.isNotEmpty()) {
                                            barcodeData = barcodes[0].displayValue // Update barcodeData here
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Handle failure (if needed)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysisUseCase
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Display barcode data if found
        barcodeData?.let { barcode ->
            // This effect runs once when barcode changes (or first loaded)
            LaunchedEffect(barcode) {
                withContext(Dispatchers.IO) {
                    val newItem = ScannedItem(barcode = barcode)
                    val found = scannedItemDao.findByCode(barcode)
                    if (found == null) {
                        scannedItemDao.insertScannedItem(newItem)
                        storedInDb = true
                        existInDb = false
                    }
                    else {
                        existInDb = true
                        storedInDb = false
                    }
                }
            }

            Column {
                Text("Scanned barcode: $barcode", color = Color.Green)

                if (storedInDb) {
                    Text("Code saved to database", color = Color.Green)
                }
                else if (existInDb) {
                    Text("Code already exist in database", color = Color.Yellow)
                }
            }
        } ?: run {
            Text("Scan a barcode or QR", color = Color.Red)
        }

    } else {
        Text("Camera permission is required", color = Color.Red)
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kitchen Assistant",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { navController.navigate("scanned_items") },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Scanned Items",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Scanned Items",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { navController.navigate("recipe_generator") },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Recipes",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Recipe Generator",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun FoodScreen(navController: NavController) {
    val darkBackground = Color(0xFF121822)
    val orangeAccent = Color(0xFFFF9500)
    val darkGrayBackground = Color(0xFF222A36)
    val database = AppDatabase.getDatabase(LocalContext.current)
    val scannedItemDao = database.scannedItemDao()

    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(bottom = 80.dp) // Space for bottom nav bar
    ) {
        // Top Bar with Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Orange circle icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(orangeAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "App Logo",
                    tint = Color.White
                )
            }

            // Search Bar
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = darkGrayBackground,
                    focusedContainerColor = darkGrayBackground,
                    cursorColor = orangeAccent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                placeholder = { Text("Search", color = Color.Gray) }
            )

            // Done Button
            Text(
                text = "Done",
                color = orangeAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Tab Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(darkGrayBackground),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CategoryTab("Search", true)
            CategoryTab("My Foods", false)
            CategoryTab("Meals", false)
            CategoryTab("Recipes", false)
        }

        // Input Methods
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InputMethodButton(
                icon = Icons.Default.Add,
                text = "Scan It",
                color = orangeAccent,
                onClick = { navController.navigate("barcode_scanner") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanned items section
        ScannedItemsList(scannedItemDao, navController, Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Action Button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("barcode_scanner")},
                modifier = Modifier.padding(16.dp),
                containerColor = orangeAccent,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Scan Barcode"
                )
            }
        }
    }
}

@Composable
fun ScannedItemsScreen(navController: NavController) {
    val database = AppDatabase.getDatabase(LocalContext.current)
    val scannedItemDao = database.scannedItemDao()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp) // Space for bottom nav bar
    ) {
        // Scanned items section
        ScannedItemsList(scannedItemDao, navController, Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Action Button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("barcode_scanner")},
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Scan Barcode"
                )
            }
        }
    }
}

@Composable
fun ScannedItemsList(
    scannedItemDao: ScannedItemDao,
    navController: NavController,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
) {
    var scannedItems by remember { mutableStateOf<List<ScannedItem>>(emptyList()) }

    // Load scanned items from the database when this Composable enters composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            scannedItems = scannedItemDao.getAllScannedItem()
        }
    }

    LazyColumn(
        modifier = modifier
    ) {
        items(scannedItems) { scannedItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Navigate to the product info screen with barcode
                        navController.navigate("product/${scannedItem.barcode}")
                    }
            ) {
                FoodItem(scannedItem.barcode.toString())
            }
        }
    }
}


@Composable
fun ProductInfoScreen(navController: NavController, barcode: String) {
    var product by remember { mutableStateOf<Product?>(null) }
    val context = LocalContext.current

    LaunchedEffect(barcode) {
        val result = KtorApi.fetchProduct(barcode)
        if (result != null) {
            product = result
        } else {
            Toast.makeText(context, "Fant ikke produktet", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Strekkode: $barcode", fontWeight = FontWeight.Bold, color = Color.Green)
        Spacer(modifier = Modifier.height(12.dp))

        product?.let {
            Text("Navn: ${it.product_name ?: "Ukjent"}")
            Text("Merke: ${it.brands ?: "Ukjent"}")
            Text("Utgår: ${it.expiration_date ?: "Ukjent"}")
            it.nutriments?.let { n ->
                Text("Fett: ${n.fat ?: 0.0}g")
                Text("Karbohydrater: ${n.carbohydrates ?: 0.0}g")
                Text("Proteiner: ${n.proteins ?: 0.0}g")
            }
            AsyncImage(
                model = it.image_url,
                contentDescription = "Product Image",
                modifier = Modifier.size(200.dp)
            )
        } ?: Text("Laster produktinfo...")
    }
}


@Composable
fun CategoryTab(name: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2E3949) else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color(0xFFFF9500) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun InputMethodButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2E3949))
            .padding(vertical = 16.dp, horizontal = 24.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FoodItem(barcode: String) {
    var product by remember { mutableStateOf<Product?>(null) }
    val context = LocalContext.current

    LaunchedEffect(barcode) {
        val result = KtorApi.fetchProduct(barcode)
        if (result != null) {
            product = result
        } else {
            Toast.makeText(context, "Fant ikke produktet", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        product?.let {
            AsyncImage(
                model = it.image_url,
                contentDescription = "Product Image",
                modifier = Modifier.size(50.dp)
            )
            Text(it.product_name ?: "Ukjent")
        } ?: Text("Laster produktinfo...")
    }
}

@Composable
fun RecipeGeneratorScreen() {
    var result by remember { mutableStateOf("Generating...") }

    LaunchedEffect(Unit) {
        val modelLoaded = MLCBridge.initModel("mlc_models/phi-2/")
        if (modelLoaded) {
            result = MLCBridge.runInference("Ingredients: apple, flour, sugar\nRecipe:")
        } else {
            result = "Failed to load model."
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Recipe Suggestion:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(result)
    }
}