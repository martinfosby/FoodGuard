import android.service.autofill.OnClickAction
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.foodguard.room.AppDatabase
import com.example.foodguard.room.ScannedItem
import com.example.foodguard.room.ScannedItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var storedInDb by remember { mutableStateOf(false) }

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

                        // Set up barcode scanning
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
                    scannedItemDao.insertScannedItem(newItem)
                }
                storedInDb = true
            }

            Column {
                Text("Scanned barcode: $barcode", color = Color.Green)

                if (storedInDb) {
                    Text("Barcode saved to database", color = Color.Yellow)
                }
            }
        } ?: run {
            Text("Scan a barcode", color = Color.Red)
        }

    } else {
        Text("Camera permission is required", color = Color.Red)
    }
}


@Composable
fun FoodScreen(navController: NavController) {
    val darkBackground = Color(0xFF121822)
    val orangeAccent = Color(0xFFFF9500)
    val darkGrayBackground = Color(0xFF222A36)
    val database = AppDatabase.getDatabase(LocalContext.current)
    val scannedItemDao = database.scannedItemDao()
    val scope = rememberCoroutineScope()
    var scannedItems by remember { mutableStateOf<List<ScannedItem>>(emptyList()) }

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

        // Breakfast Foods Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            scope.launch(Dispatchers.IO) {
                scannedItems = scannedItemDao.getAllScannedItem()
            }
            for (scannedItem in scannedItems) {
                Text(
                    text = "Barcode: " + scannedItem.barcode,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

//            // Show More Button
//            Row(
//                modifier = Modifier.padding(vertical = 8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Show More",
//                    color = orangeAccent,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(
//                    text = "9 additional items found",
//                    color = Color.Gray,
//                    fontSize = 14.sp,
//                    modifier = Modifier.padding(start = 8.dp)
//                )
//            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Meals Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "RECENT MEALS",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            FoodItem(
                icon = Icons.Default.AccountBox,
                name = "Breakfast",
                description = "517 calories, 3 items",
                date = "Today"
            )
        }

        // Floating Action Button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { /* Handle click */ },
                modifier = Modifier.padding(16.dp),
                containerColor = orangeAccent,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = "Scan Barcode"
                )
            }
        }
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
fun FoodItem(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, description: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Food Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2E3949)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Food Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // Date
        Text(
            text = date,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}