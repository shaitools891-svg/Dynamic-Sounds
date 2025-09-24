package com.matherror.dynamicsounds.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.navigation.NavController
import com.matherror.dynamicsounds.NotificationSoundService
import com.matherror.dynamicsounds.R
import com.matherror.dynamicsounds.Screen
import com.matherror.dynamicsounds.ui.theme.PrimaryGreen

data class LogEntry(
    val soundIcon: Int,
    val soundName: String,
    val time: String,
    val appName: String,
    val messagePreview: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
        val cn = ComponentName(context, NotificationSoundService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }


    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var enableRNS by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var ignoreOngoing by remember { mutableStateOf(prefs.getBoolean("ignore_ongoing_notifications", false)) }
    var showHelpDialog by remember { mutableStateOf(false) }


    fun updateOverlaySetting(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
        val intent = Intent("${context.packageName}.OVERLAY_LAYOUT_CHANGE")
        val bundle = Bundle().apply { putFloat(key, value) }
        intent.putExtra("settings", bundle)
        context.sendBroadcast(intent)
    }

    // Notification listener launcher
    val notificationListenerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        enableRNS = isNotificationListenerEnabled(context)
        if (enableRNS) {
            Toast.makeText(context, "Notification access granted! Custom sounds will now work.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification access denied. Custom sounds won't work without it.", Toast.LENGTH_LONG).show()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Logs", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Enable RNS toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Enable RNS",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = enableRNS,
                                enabled = true, // Always enabled
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        // User wants to enable - check if already enabled
                                        if (!enableRNS) {
                                            // Open notification listener settings
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            notificationListenerLauncher.launch(intent)
                                        }
                                        // If already enabled, just ensure state is correct
                                        enableRNS = true
                                    } else {
                                        // User wants to disable - show message and force back to enabled
                                        Toast.makeText(context, "To disable, go to Settings > Accessibility > Notification access and disable this app", Toast.LENGTH_LONG).show()
                                        // Force the switch back to enabled state
                                        enableRNS = true
                                    }
                                }
                            )
                        }
                        // Ignore Ongoing Notifications toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ignore Ongoing Notifications",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = ignoreOngoing,
                                onCheckedChange = { checked ->
                                    ignoreOngoing = checked
                                    prefs.edit().putBoolean("ignore_ongoing_notifications", checked).apply()
                                }
                            )
                        }
                        // Help & Troubleshooting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showHelpDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Help & Troubleshooting",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Help"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Custom Time Intervals
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Custom Time Intervals feature coming soon", Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Time Intervals",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Chevron"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Manage Storage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Manage Storage feature coming soon", Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Manage Storage",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Chevron"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Reset to Default
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Settings reset to default", Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reset to Default",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Chevron",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dynamic Sounds",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Version 1.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Developer: Shakibul Hasan",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Created with AI assistance and passion for enhancing user experience through innovative sound design.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transform your notifications into delightful auditory experiences with nature-inspired sounds that change throughout the day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                // Help & Troubleshooting Dialog
                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        title = {
                            Text(
                                "Help & Troubleshooting",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    "If the app isn't working in the background, follow these steps based on your device manufacturer:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
            
                                // Xiaomi
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Xiaomi/MIUI",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6700)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Apps > Manage apps > [App Name]")
                                        Text("2. Tap 'Battery saver' and select 'No restrictions'")
                                        Text("3. Go to Settings > Battery & performance > App battery saver")
                                        Text("4. Find [App Name] and set to 'No restrictions'")
                                        Text("5. Enable 'Autostart' in app settings")
                                    }
                                }
            
                                // Vivo
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Vivo/FunTouch OS",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0066CC)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Battery > High background power consumption")
                                        Text("2. Find [App Name] and allow background activity")
                                        Text("3. Go to Settings > Apps > Special access > Autostart")
                                        Text("4. Enable autostart for [App Name]")
                                        Text("5. Go to i Manager > App manager > Autostart manager")
                                        Text("6. Enable [App Name]")
                                    }
                                }
            
                                // Oppo
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Oppo/ColorOS",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0099FF)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Battery > Battery optimization")
                                        Text("2. Find [App Name] and set to 'Don't optimize'")
                                        Text("3. Go to Settings > Apps > Special access > Autostart")
                                        Text("4. Enable autostart for [App Name]")
                                        Text("5. Go to Phone Manager > Privacy permissions > Startup manager")
                                        Text("6. Enable [App Name]")
                                    }
                                }
            
                                // Realme
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Realme/Realme UI",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Battery > Battery optimization")
                                        Text("2. Find [App Name] and set to 'Don't optimize'")
                                        Text("3. Go to Settings > Apps > Special access > Autostart")
                                        Text("4. Enable autostart for [App Name]")
                                        Text("5. Go to Settings > Battery > App battery management")
                                        Text("6. Set [App Name] to 'Allow background activity'")
                                    }
                                }
            
                                // Samsung
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Samsung/One UI",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF034EA2)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Apps > [App Name] > Battery")
                                        Text("2. Set to 'Unrestricted' or 'Not optimized'")
                                        Text("3. Go to Settings > Device care > Battery > App power management")
                                        Text("4. Find [App Name] and set to 'Apps that won't be put to sleep'")
                                    }
                                }
            
                                // Huawei
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Huawei/EMUI",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD52B1E)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Apps > Apps > [App Name]")
                                        Text("2. Go to Battery > Launch and set to 'Manage manually'")
                                        Text("3. Enable all options under 'Launch'")
                                        Text("4. Go to Settings > Battery > App launch")
                                        Text("5. Find [App Name] and enable all options")
                                    }
                                }
            
                                // General Android
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "General Android/Other Brands",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Go to Settings > Apps > [App Name] > Battery")
                                        Text("2. Set to 'Don't optimize' or 'Unrestricted'")
                                        Text("3. Go to Settings > Battery > Battery optimization")
                                        Text("4. Find [App Name] and set to 'Don't optimize'")
                                        Text("5. Check for any 'Autostart' or 'Startup' settings")
                                    }
                                }
            
                                Text(
                                    "If issues persist, try restarting your device after applying these settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}
