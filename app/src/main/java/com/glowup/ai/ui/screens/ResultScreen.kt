package com.glowup.ai.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.glowup.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    personImageUrl: String,
    resultImageUrl: String,
    onBack: () -> Unit,
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
                        Toast.makeText(context, "Look saved to gallery! ✨", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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

            // Scrim for readability
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AI TRANSFORMATION", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 14.sp, 
                    letterSpacing = 2.sp, 
                    color = Color.White
                )
                Text(
                    "FULL SCREEN PREVIEW", 
                    fontSize = 10.sp, 
                    color = Color.White.copy(alpha = 0.7f), 
                    fontWeight = FontWeight.Bold
                )
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

        // BEFORE/AFTER LABEL
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .graphicsLayer { rotationZ = 90f }
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                if (showBefore) "ORIGINAL" else "AI RESULT",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // FLOATING CONTROLS AT BOTTOM
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ResultToggleBtn("BEFORE", showBefore) { showBefore = true }
                ResultToggleBtn("AFTER", !showBefore) { showBefore = false }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onTryAnother,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Text("RETRY", color = Color.White, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = { saveImage() },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE TO GALLERY", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ResultToggleBtn(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        color = if (active) Color.White else Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text, 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp, 
                color = if (active) Color.Black else Color.White
            )
        }
    }
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
    }
    return uri ?: Uri.EMPTY
}
