package com.glowup.ai.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowup.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "",
    userAge: Int = 0,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // USER INFO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Age: $userAge • Style Expert", color = Gray400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // MENU
            SettingsSection("Preferences") {
                MenuLink("Ai sensitivity", Icons.Outlined.Tune)
                MenuLink("Theme", Icons.Outlined.Palette)
                MenuLink("Notifications", Icons.Outlined.Notifications)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection("Support") {
                MenuLink("Help center", Icons.Outlined.HelpOutline)
                MenuLink("Privacy policy", Icons.Outlined.Security)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // LOGOUT
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("Are you sure you want to end your style session?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Log out", color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Gray400)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Gray50,
            border = BorderStroke(1.dp, Gray100)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MenuLink(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Gray600, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Gray300)
    }
}
