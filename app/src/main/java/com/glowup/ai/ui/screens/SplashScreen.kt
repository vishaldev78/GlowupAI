package com.glowup.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = Primary,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Glowup Ai",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}
