package com.glowup.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.glowup.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudio: () -> Unit
) {
    var showAllTrends by remember { mutableStateOf(false) }
    
    val trends = listOf(
        "Casual Elegance" to "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600&q=80",
        "Modern Nomad" to "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&q=80",
        "Urban Luxe" to "https://images.unsplash.com/photo-1492707892479-7bc8d5a4ee93?w=600&q=80",
        "Bohemian Chic" to "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=600&q=80",
        "Retro Revival" to "https://images.unsplash.com/photo-1550614000-4895a10e1bfd?w=600&q=80"
    )

    Scaffold(
        containerColor = Color(0xFFF8F9FA), // Professional Off-White
        topBar = {
            TopAppBar(
                title = { 
                    Text("Glowup Ai", color = Gray900, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 20.sp)
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Notifications, null, tint = Gray900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // HERO SECTION - Professional margin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Primary.copy(alpha = 0.9f))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Text("Transform Your\nStyle Instantly", color = White, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNavigateToStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("Open Ai studio", color = Primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Icon(
                    Icons.Default.AutoAwesome, 
                    null, 
                    tint = White.copy(alpha = 0.15f),
                    modifier = Modifier.size(140.dp).align(Alignment.CenterEnd).offset(x = 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QUICK TOOLS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Quick tools", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Gray400, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard("Ai Camera", Icons.Outlined.PhotoCamera, Modifier.weight(1f), onNavigateToStudio)
                    ActionCard("Gallery", Icons.Outlined.Collections, Modifier.weight(1f), onNavigateToStudio)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // AI TRENDS
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ai trends", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Gray400, letterSpacing = 0.5.sp)
                    TextButton(onClick = { showAllTrends = !showAllTrends }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (showAllTrends) "Show less" else "View all", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (showAllTrends) {
                    // Show all in a flow-like grid (vertical list for simplicity in a scrollable column)
                    trends.forEach { trend ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TrendCard(trend.first, trend.second, Modifier.fillMaxWidth().height(200.dp))
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    ) {
                        items(trends) { trend ->
                            TrendCard(trend.first, trend.second, Modifier.width(180.dp).height(260.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gray900)
        }
    }
}

@Composable
private fun TrendCard(name: String, url: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Gray50)
            .clickable { }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
                }
            }
        )
        
        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                name, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 12.sp
            )
            Text(
                "Trending",
                color = Color.White.copy(alpha = 0.6f), 
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
