package com.matherror.dynamicsounds.ui

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.matherror.dynamicsounds.AppSelectorAccessibilityService
import com.matherror.dynamicsounds.NotificationSoundService
import com.matherror.dynamicsounds.R
import com.matherror.dynamicsounds.ui.theme.PrimaryGreen

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Permission states
    val notificationListenerEnabled = remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val accessibilityEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val postNotificationGranted = remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }

    // UI states
    val selectedApps = remember { mutableStateOf(getSelectedApps(context)) }
    val allPermissionsGranted = remember {
        derivedStateOf {
            notificationListenerEnabled.value &&
            accessibilityEnabled.value &&
            postNotificationGranted.value
        }
    }

    // Permission launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        postNotificationGranted.value = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
        }
    }


    val notificationListenerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        notificationListenerEnabled.value = isNotificationListenerEnabled(context)
        if (notificationListenerEnabled.value) {
            Toast.makeText(context, "Notification access granted! Custom sounds will now work.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification access denied. Custom sounds won't work without it.", Toast.LENGTH_LONG).show()
        }
    }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        accessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        if (accessibilityEnabled.value) {
            Toast.makeText(context, "Accessibility permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Accessibility permission denied. App selection won't work without it.", Toast.LENGTH_LONG).show()
        }
    }

    // Main content
    if (allPermissionsGranted.value) {
        // All permissions granted - show main app interface
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "All Set! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All permissions have been granted. You can now use all features of RNS.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                navController.navigate("app_selection")
            }) {
                Icon(Icons.Filled.Settings, contentDescription = "Apps")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Apps (${selectedApps.value.size} selected)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show current selection info
            if (selectedApps.value.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Selected Apps:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val context = LocalContext.current
                        val selectedAppNames = remember(selectedApps.value) {
                            getInstalledApps(context)
                                .filter { selectedApps.value.contains(it.packageName) }
                                .take(5) // Show first 5
                                .joinToString(", ") { it.appName }
                        }
                        Text(
                            text = if (selectedApps.value.size <= 5) selectedAppNames else "$selectedAppNames...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        // Permissions not fully granted - show permission setup
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Setup Required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant the following permissions to enable all features:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Notification Access
                item {
                    PermissionCard(
                        title = "Notification Access",
                        description = "Required to intercept notifications and play custom sounds",
                        isGranted = notificationListenerEnabled.value,
                        onGrantClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            notificationListenerLauncher.launch(intent)
                        }
                    )
                }


                // Accessibility Service
                item {
                    PermissionCard(
                        title = "Accessibility Service",
                        description = "Required to select specific apps for popup activation. You can configure apps after granting this permission.",
                        isGranted = accessibilityEnabled.value,
                        onGrantClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            accessibilityLauncher.launch(intent)
                        }
                    )
                }

                // Post Notifications (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        PermissionCard(
                            title = "Post Notifications",
                            description = "Required to show notification badges and alerts",
                            isGranted = postNotificationGranted.value,
                            onGrantClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isGranted) Icons.Filled.Notifications else Icons.AutoMirrored.Filled.List,
                        contentDescription = if (isGranted) "Granted" else "Not granted",
                        tint = if (isGranted) PrimaryGreen else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isGranted) {
                Button(onClick = onGrantClick) {
                    Text("Grant")
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val cn = ComponentName(context, NotificationSoundService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    return AppSelectorAccessibilityService.isEnabled(context)
}

internal fun getSelectedApps(context: android.content.Context): Set<String> {
    val prefs = context.getSharedPreferences("app_selection", Context.MODE_PRIVATE)
    val appsJson = prefs.getString("selected_apps", "[]")
    return try {
        Gson().fromJson(appsJson, Array<String>::class.java).toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

internal fun saveSelectedApps(context: android.content.Context, selectedApps: Set<String>) {
    val prefs = context.getSharedPreferences("app_selection", Context.MODE_PRIVATE)
    val appsJson = Gson().toJson(selectedApps.toTypedArray())
    prefs.edit().putString("selected_apps", appsJson).apply()
}

internal fun getInstalledApps(context: android.content.Context): List<AppInfo> {
    val pm = context.packageManager
    android.util.Log.d("AppSelection", "Starting getInstalledApps")

    // Get apps that appear in the launcher (should be user-installed apps)
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos = pm.queryIntentActivities(intent, 0)
    android.util.Log.d("AppSelection", "queryIntentActivities returned ${resolveInfos.size} resolveInfos")

    val packageNames = resolveInfos
        .map { resolveInfo -> resolveInfo.activityInfo.packageName }
        .distinct()
    android.util.Log.d("AppSelection", "Distinct packageNames: ${packageNames.size}")

    val filteredPackages = packageNames.filter { packageName ->
        // Filter out our own app
        packageName != context.packageName
    }
    android.util.Log.d("AppSelection", "After filtering own app: ${filteredPackages.size}")

    val launcherApps = filteredPackages
        .map { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                AppInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (e: Exception) {
                android.util.Log.e("AppSelection", "Error getting app info for $packageName", e)
                null
            }
        }
        .filterNotNull()
        .sortedBy { it.appName }

    // Debug logging
    android.util.Log.d("AppSelection", "Final list: ${launcherApps.size} user apps from launcher")
    launcherApps.take(5).forEach { app ->
        android.util.Log.d("AppSelection", "App: ${app.appName} (${app.packageName})")
    }

    return launcherApps
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)

