package com.matherror.dynamicsounds.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val installedApps = remember { getInstalledApps(context) }
    val selectedApps = remember { mutableStateOf(getSelectedApps(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Apps for Popup") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Save selection and navigate back
                        saveSelectedApps(context, selectedApps.value)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${selectedApps.value.size}/${installedApps.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header with select all/none buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedApps.value = installedApps.map { it.packageName }.toSet()
                        saveSelectedApps(context, selectedApps.value)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }
                OutlinedButton(
                    onClick = {
                        selectedApps.value = emptySet()
                        saveSelectedApps(context, selectedApps.value)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select None")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose which apps can trigger the Smart Notch popup:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // App grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(installedApps) { app ->
                    AppGridItem(
                        app = app,
                        isSelected = selectedApps.value.contains(app.packageName),
                        onSelectionChanged = { selected ->
                            val newSelection = selectedApps.value.toMutableSet()
                            if (selected) {
                                newSelection.add(app.packageName)
                            } else {
                                newSelection.remove(app.packageName)
                            }
                            selectedApps.value = newSelection
                            saveSelectedApps(context, newSelection)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) }
            .padding(8.dp)
    ) {
        // App icon with selection overlay
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                contentDescription = app.appName,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // App name
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}