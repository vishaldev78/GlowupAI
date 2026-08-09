package com.glowup.ai.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Ai style studio",
            "Try on thousands of outfits instantly using our advanced artificial intelligence.",
            Icons.Default.AutoAwesome
        ),
        OnboardingPage(
            "Your digital closet",
            "Upload your own clothes and see how they look on you before wearing them.",
            Icons.Default.Checkroom
        ),
        OnboardingPage(
            "Instant results",
            "Generate your new look in seconds and share your style with the world.",
            Icons.Default.IosShare
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = White,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                // Indicator
                Row(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Gray900 else Gray200
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 24.dp else 8.dp, 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gray900)
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Get started" else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    if (pagerState.currentPage < pages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = White)
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { index ->
            OnboardingPageView(pages[index])
        }
    }
}

@Composable
fun OnboardingPageView(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Gray50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                null,
                tint = Gray900,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Gray900,
            lineHeight = 40.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            page.description,
            fontSize = 16.sp,
            color = Gray500,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
