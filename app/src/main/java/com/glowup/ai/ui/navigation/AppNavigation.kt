package com.glowup.ai.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.glowup.ai.data.store.AuthStore
import com.glowup.ai.ui.screens.*
import com.glowup.ai.ui.theme.*

// Routes
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STUDIO = "studio"
    const val SETTINGS = "settings"
}

// Bottom nav items
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : BottomNavItem(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Studio : BottomNavItem(Routes.STUDIO, "Studio", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome)
    data object Settings : BottomNavItem(Routes.SETTINGS, "Settings", Icons.Outlined.Person, Icons.Filled.Person)

    companion object {
        val items = listOf(Home, Studio, Settings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    authStore: AuthStore,
    isLoggedIn: Boolean,
    userName: String,
    userAge: Int,
    onAuthSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authToken = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        authStore.authToken.collect { token ->
            authToken.value = token ?: ""
        }
    }

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        BottomNavItem.items.any { it.route == dest.route }
    } == true

    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp) 
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavItem.items.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.icon,
                                            contentDescription = item.label,
                                            tint = if (selected) Primary else Gray600,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Primary else Gray600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.HOME else Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) 60.dp else 0.dp),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        if (isLoggedIn) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        scope.launch {
                            // Save a default profile for onboarding completion
                            authStore.saveAuth("token_${System.currentTimeMillis()}", "Guest User", 25)
                            onAuthSuccess()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToStudio = {
                        navController.navigate(Routes.STUDIO) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.STUDIO) {
                TryOnScreen(
                    authToken = authToken.value
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    userName = userName,
                    userAge = userAge,
                    onLogout = {
                        onLogout()
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
