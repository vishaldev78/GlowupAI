package com.glowup.ai.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.glowup.ai.data.api.YouCamApi
import com.glowup.ai.data.model.*
import com.glowup.ai.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnScreen(
    authToken: String = "",
    onResultReady: (personUri: String, resultUrl: String) -> Unit = { _, _ -> }
) {
    var selectedCategory by remember { mutableStateOf(GarmentCategory.TOPS) }
    var selectedGarment by remember { mutableStateOf<Garment?>(null) }
    val customGarments = remember { mutableStateListOf<Garment>() }
    
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var personUri by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Result State
    var resultUrl by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Permissions
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { 
            capturedBitmap = loadBitmapFromUri(context, it)
            personUri = it.toString()
        }
    }

    val customGarmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { 
            val bitmap = loadBitmapFromUri(context, it)
            if (bitmap != null) {
                val newGarment = Garment(
                    id = "custom_${System.currentTimeMillis()}",
                    name = "Custom Piece",
                    imageUrl = it.toString(),
                    category = selectedCategory,
                    gender = Gender.WOMEN // Default gender for custom items
                )
                customGarments.add(newGarment)
                selectedGarment = newGarment
            }
        }
    }

    fun takePicture() {
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees.toFloat())
                    capturedBitmap = bitmap
                    
                    // Save to gallery to get a URI for "Before" image
                    scope.launch(Dispatchers.IO) {
                        val uri = saveBitmapToGallery(context, bitmap)
                        personUri = uri.toString()
                    }
                    
                    image.close()
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("TryOn", "Capture failed", exception)
                }
            }
        )
    }

    fun processTryOn() {
        val garment = selectedGarment ?: return
        val person = capturedBitmap ?: return
        
        errorMessage = null
        isProcessing = true
        
        scope.launch {
            try {
                // HD 4K QUALITY PROCESSING
                val optimizedPerson = withContext(Dispatchers.IO) {
                    val maxDim = 3840f // 4K resolution limit
                    val scale = maxDim / Math.max(person.width, person.height).toFloat()
                    if (scale < 1f) {
                        Bitmap.createScaledBitmap(person, (person.width * scale).toInt(), (person.height * scale).toInt(), true)
                    } else person
                }

                // FIREBASE STORAGE & API INTEGRATION
                val storage = FirebaseStorage.getInstance().reference
                val firestore = FirebaseFirestore.getInstance()

                val personBase64 = bitmapToBase64(optimizedPerson)
                val youCamApi = YouCamApi()
                
                val garmentBase64 = if (garment.id.startsWith("custom")) {
                    val rawBitmap = loadBitmapFromUri(context, Uri.parse(garment.imageUrl))
                    if (rawBitmap != null) {
                        // Optimize custom garment too
                        val maxDim = 2048f
                        val scale = maxDim / Math.max(rawBitmap.width, rawBitmap.height).toFloat()
                        val optimizedGarment = if (scale < 1f) {
                            Bitmap.createScaledBitmap(rawBitmap, (rawBitmap.width * scale).toInt(), (rawBitmap.height * scale).toInt(), true)
                        } else rawBitmap
                        
                        // Try to upload to Firebase if it's a custom piece (Optional, but user asked)
                        try {
                            val bytes = ByteArrayOutputStream().let {
                                optimizedGarment.compress(Bitmap.CompressFormat.JPEG, 90, it)
                                it.toByteArray()
                            }
                            storage.child("custom_garments/${garment.id}.jpg").putBytes(bytes)
                        } catch (e: Exception) {
                            Log.e("Firebase", "Optional upload failed: ${e.message}")
                        }
                        
                        bitmapToBase64(optimizedGarment)
                    } else null
                } else {
                    youCamApi.downloadImageAsBase64(garment.imageUrl)
                }

                if (garmentBase64 == null) {
                    errorMessage = "Garment error. Try another."
                    isProcessing = false
                    return@launch
                }

                val response = youCamApi.tryOn(personBase64, garmentBase64, garment.category.apiValue)
                
                if (response.success && response.resultImageUrl != null) {
                    resultUrl = response.resultImageUrl
                    showResult = true
                } else {
                    errorMessage = response.error ?: "AI Studio is busy"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isProcessing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(White)) {
        if (capturedBitmap == null) {
            // CAMERA VIEW
            if (hasCameraPermission) {
                CameraPreview(
                    imageCapture = imageCapture,
                    lensFacing = lensFacing,
                    flashEnabled = flashEnabled,
                    onCameraReady = { camera = it }
                )
            }

            // CAMERA OVERLAYS
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Back */ }, modifier = Modifier.background(White.copy(0.3f), CircleShape)) {
                        Icon(Icons.Default.ArrowBack, null, tint = Gray900)
                    }
                    
                    Text("Ai studio pro", color = Gray900, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { flashEnabled = !flashEnabled },
                            modifier = Modifier.background(White.copy(0.3f), CircleShape)
                        ) {
                            Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, null, tint = if (flashEnabled) Warning else Gray900)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Capture Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            ) 
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, null, tint = Gray900, modifier = Modifier.size(28.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Gray900)
                            .border(4.dp, Gray900.copy(alpha = 0.2f), CircleShape)
                            .clickable { takePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(54.dp).clip(CircleShape).border(2.dp, White, CircleShape))
                    }

                    IconButton(
                        onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, null, tint = Gray900, modifier = Modifier.size(28.dp))
                    }
                }
            }
        } else {
            // STUDIO PREVIEW
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

                // STUDIO OVERLAYS
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { capturedBitmap = null; personUri = null }, modifier = Modifier.background(White.copy(0.8f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Gray900)
                    }
                    
                    Button(
                        onClick = { processTryOn() },
                        enabled = selectedGarment != null && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                        } else {
                            Text("Generate", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // STUDIO UI - Minimalist Carousel (No Bottom Sheet Surface)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Category Tabs - Scrollable
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(GarmentCategory.entries) { cat ->
                            LensTab(cat.displayName, selectedCategory == cat) { 
                                selectedCategory = cat
                                selectedGarment = null 
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Garment Carousel
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item { 
                            AddLensBtn { 
                                customGarmentPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                ) 
                            } 
                        }
                        
                        val combinedList = (customGarments.filter { it.category == selectedCategory } + 
                                           GarmentCatalog.garments.filter { it.category == selectedCategory })
                                           .filter { it.imageUrl.isNotEmpty() && it.name.isNotEmpty() }
                                           
                        items(combinedList) { garment ->
                            LensGarmentItem(
                                garment = garment,
                                selected = selectedGarment?.id == garment.id,
                                onDelete = if (garment.id.startsWith("custom")) {
                                    { 
                                        customGarments.remove(garment)
                                        if (selectedGarment?.id == garment.id) selectedGarment = null
                                    }
                                } else null,
                                onClick = { selectedGarment = garment }
                            )
                        }
                    }
                }
            }
        }

        // FULL SCREEN RESULT OVERLAY
        if (showResult && resultUrl != null && personUri != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showResult = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                ResultOverlay(
                    personImageUrl = personUri!!,
                    resultImageUrl = resultUrl!!,
                    onClose = { showResult = false },
                    onTryAnother = {
                        showResult = false
                        capturedBitmap = null
                        personUri = null
                    }
                )
            }
        }

        // ERROR TOAST
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                action = { TextButton(onClick = { errorMessage = null }) { Text("OK", color = Accent) } }
            ) { Text(errorMessage!!) }
        }
    }
}

@Composable
fun ResultOverlay(
    personImageUrl: String,
    resultImageUrl: String,
    onClose: () -> Unit,
    onTryAnother: () -> Unit
) {
    var rotationY by remember { mutableStateOf(0f) }
    var showBefore by remember { mutableStateOf(false) }
    var personBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val animRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    LaunchedEffect(personImageUrl) {
        if (personImageUrl.startsWith("content://") || personImageUrl.startsWith("file://")) {
            try {
                personBitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(Uri.parse(personImageUrl))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun saveImage() {
        isSaving = true
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val url = URL(resultImageUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    BitmapFactory.decodeStream(connection.getInputStream())
                }
                
                if (bitmap != null) {
                    val savedUri = saveBitmapToGallery(context, bitmap)
                    if (savedUri != Uri.EMPTY) {
                        Toast.makeText(context, "Saved to gallery! ✨", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // FULL HEIGHT IMAGE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        rotationY += dragAmount.x * 0.5f
                    }
                }
                .graphicsLayer {
                    rotationY = animRotationY
                    cameraDistance = 8f * density
                }
        ) {
            AnimatedContent(
                targetState = showBefore,
                label = "image_swap",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize()
            ) { isBefore ->
                if (isBefore) {
                    personBitmap?.let {
                        Image(bitmap = it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } ?: AsyncImage(model = personImageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    SubcomposeAsyncImage(
                        model = resultImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                    )
                }
            }

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // FLOATING HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Small Transparent Buttons on Top
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTryAnother,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { saveImage() },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.8f)),
                    enabled = !isSaving,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            IconButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out my new look from GlowUp AI! $resultImageUrl")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Outlined.Share, null, tint = Color.White)
            }
        }

        // FLOATING CONTROLS AT BOTTOM
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
        ) {
            // Toggle
            Row(
                modifier = Modifier
                    .width(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ResultToggleBtn("Original", showBefore) { showBefore = true }
                ResultToggleBtn("Ai result", !showBefore) { showBefore = false }
            }
        }
    }
}

@Composable
private fun RowScope.ResultToggleBtn(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        color = if (active) White else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (active) 4.dp else 0.dp
    ) {
        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (active) Gray900 else Gray500)
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    lensFacing: Int,
    flashEnabled: Boolean,
    onCameraReady: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(lensFacing, flashEnabled) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
            )
            camera.cameraControl.enableTorch(flashEnabled)
            onCameraReady(camera)
        } catch (e: Exception) {
            Log.e("Camera", "Binding failed", e)
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun LensTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Gray200 else White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) Gray300 else Gray900.copy(alpha = 0.1f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Gray900 else Gray900.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LensGarmentItem(
    garment: Garment,
    selected: Boolean,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(White) // Pure white background
                .border(2.dp, if (selected) Gray400 else Gray100, RoundedCornerShape(16.dp))
                .clickable { onClick() }
        ) {
            SubcomposeAsyncImage(
                model = garment.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Gray900)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Gray100), Alignment.Center) {
                        Icon(Icons.Default.BrokenImage, null, tint = Gray400, modifier = Modifier.size(24.dp))
                    }
                }
            )
            
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(White.copy(0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Gray900, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            garment.name, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Medium, 
            color = if (selected) Gray900 else Gray600, 
            maxLines = 1, 
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddLensBtn(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(White) // Pure white background
                .border(1.5.dp, Gray100, RoundedCornerShape(16.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Gray900, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Upload", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Gray600)
    }
}

// Helpers
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
    return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
}

private fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri {
    val filename = "GlowUp_${System.currentTimeMillis()}.jpg"
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlowUpAI")
    }
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
    }
    return uri ?: Uri.EMPTY
}
