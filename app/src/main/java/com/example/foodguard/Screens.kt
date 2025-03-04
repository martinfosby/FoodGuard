import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Barcode data state
    var barcodeData by remember { mutableStateOf<String?>(null) }

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
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    // Check if any barcode is detected
                                    if (barcodes.isNotEmpty()) {
                                        val barcode = barcodes[0]
                                        barcodeData = barcode.displayValue
                                    }
                                }
                                .addOnFailureListener {
                                    // Handle failure
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
        modifier = Modifier.fillMaxSize(),
        onRelease = {
            // Clean up camera resources if needed when composable is removed
            cameraProviderFuture.get().unbindAll()
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen() {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

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
        barcodeData?.let {
            Text("Scanned barcode: $it", color = Color.Green)
        } ?: run {
            Text("Scan a barcode", color = Color.Red)
        }

    } else {
        Text("Camera permission is required", color = Color.Red)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    // Launch permission request if necessary
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // If permission granted, show CameraPreviewScreen, else show permission required message
    if (cameraPermissionState.status.isGranted) {
        CameraPreviewScreen()
    } else {
        Text("Camera permission is required", color = Color.Red)
    }
}
