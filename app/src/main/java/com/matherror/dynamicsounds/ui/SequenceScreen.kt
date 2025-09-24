package com.matherror.dynamicsounds.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.matherror.dynamicsounds.R
import com.matherror.dynamicsounds.SoundClip
import com.matherror.dynamicsounds.SoundManager
import com.matherror.dynamicsounds.ui.theme.PrimaryGreen

data class TimePeriod(
    val name: String,
    val timeRange: String,
    val icon: Int,
    val sounds: List<SoundClip> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    var showEditSoundDialog by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<TimePeriod?>(null) }

    val availableSounds by remember { derivedStateOf { soundManager.getAllSounds() } }

    // Group sounds by time period
    val timePeriods by remember(availableSounds) {
        derivedStateOf {
            val timePeriodConfigs = listOf(
                Triple("Morning", "6am-12pm", R.drawable.ic_morning),
                Triple("Noon", "12pm-2pm", R.drawable.ic_sunny),
                Triple("Afternoon", "2pm-6pm", R.drawable.ic_afternoon),
                Triple("Evening", "6pm-10pm", R.drawable.ic_evening),
                Triple("Night", "10pm-6am", R.drawable.ic_night)
            )

            timePeriodConfigs.map { (name, timeRange, icon) ->
                val periodSounds = availableSounds.filter { sound ->
                    sound.assetPath?.startsWith(name.lowercase()) == true ||
                    sound.assetPath?.contains("/${name.lowercase()}/") == true
                }
                TimePeriod(name, timeRange, icon, periodSounds)
            }
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Extract display name from URI
                val displayName = getDisplayNameFromUri(context, uri) ?: "Custom Sound"
                val success = soundManager.addSoundFromUri(uri, displayName)
                Toast.makeText(
                    context,
                    if (success) "Sound added successfully" else "Failed to add sound",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val pickAudioFile = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound Sequences", fontWeight = FontWeight.Bold) }
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
            items(timePeriods) { period ->
                TimePeriodCard(period, soundManager, context, onPreview = {
                    // Play preview of all available sounds randomly
                    if (period.sounds.isNotEmpty()) {
                        playSoundSequencePreview(context, period.sounds, soundManager)
                        Toast.makeText(context, "Playing ${period.name} sound sequence", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No sounds available for ${period.name}", Toast.LENGTH_SHORT).show()
                    }
                }, onEdit = {
                    editingPeriod = period
                    showEditSoundDialog = true
                }, onRemove = {
                    Toast.makeText(context, "Sound removed", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    if (showEditSoundDialog && editingPeriod != null) {
        EditSoundDialog(
            period = editingPeriod!!,
            availableSounds = availableSounds,
            onSoundSelected = { selectedSound ->
                // For now, just show that editing is not fully implemented
                // In a full implementation, this would update the sound sequence
                Toast.makeText(context, "Sound sequence editing coming soon", Toast.LENGTH_SHORT).show()
                showEditSoundDialog = false
                editingPeriod = null
            },
            onAddNewSound = {
                showEditSoundDialog = false
                editingPeriod = null
                pickAudioFile()
            },
            onDismiss = {
                showEditSoundDialog = false
                editingPeriod = null
            }
        )
    }
}

@Composable
fun TimePeriodCard(period: TimePeriod, soundManager: SoundManager, context: android.content.Context, onPreview: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(period.icon),
                        contentDescription = period.name,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = period.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = period.timeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Show preview button if there are sounds available
                if (period.sounds.isNotEmpty()) {
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_play),
                            contentDescription = "Preview",
                            tint = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound count and list
            Text(
                text = "${period.sounds.size} sound${if (period.sounds.size != 1) "s" else ""} available",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (period.sounds.isEmpty()) {
                Text(
                    text = "No sounds configured for this time period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                period.sounds.forEachIndexed { index, sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play button for preview
                        IconButton(
                            onClick = {
                                playSoundPreview(context, sound, soundManager)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_play),
                                contentDescription = "Play ${sound.name}",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sound.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = sound.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (sound.isUserAdded) {
                            Icon(
                                painterResource(R.drawable.ic_custom_time_period),
                                contentDescription = "User added",
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        if (sound.isUserAdded) {
                            IconButton(onClick = onRemove) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = "Remove",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundClipItem(sound: SoundClip, showDrag: Boolean = false, showDelete: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDrag) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Drag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        if (sound.isPrimary) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Primary",
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_play),
                contentDescription = "Sound",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sound.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sound.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!sound.isPrimary) {
            IconButton(onClick = { /* TODO: More options */ }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDelete) {
            IconButton(onClick = { /* TODO: Delete */ }) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

// Helper function to get display name from URI
private fun getDisplayNameFromUri(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                cursor.getString(nameIndex)?.substringBeforeLast('.') ?: "Unknown"
            } else {
                null
            }
        } else {
            null
        }
    }
}

// Function to play sound preview
private fun playSoundPreview(context: android.content.Context, soundClip: SoundClip, soundManager: SoundManager) {
    playSoundClipPreview(context, soundClip, soundManager)
}

// Function to play individual sound clip preview
private fun playSoundClipPreview(context: android.content.Context, soundClip: SoundClip, soundManager: SoundManager) {
    if (soundClip.name == "Silent") {
        return
    }

    try {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager

        // Request audio focus
        val result = audioManager.requestAudioFocus(
            null,
            android.media.AudioManager.STREAM_NOTIFICATION,
            android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )

        if (result != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return
        }

        // Try to play asset sound
        if (soundClip.assetPath != null) {
            playSoundFromAssetsPreview(context, soundClip.assetPath!!, audioManager)
            return
        }

        // Try to play user-added sound
        if (soundClip.isUserAdded && soundClip.userSoundId != null) {
            val soundFile = soundManager.getSoundFile(soundClip.userSoundId!!)
            if (soundFile != null && soundFile.exists()) {
                playSoundFilePreview(context, soundFile, audioManager)
                return
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SequenceScreen", "Error playing sound preview", e)
    }
}

private fun playSoundFromAssetsPreview(context: android.content.Context, assetPath: String, audioManager: android.media.AudioManager) {
    try {
        val afd = context.assets.openFd(assetPath)
        val mediaPlayer = android.media.MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            setAudioStreamType(android.media.AudioManager.STREAM_NOTIFICATION)
            setOnCompletionListener {
                afd.close()
                release()
                audioManager.abandonAudioFocus(null)
            }
            prepare()
            start()
        }
    } catch (e: Exception) {
        android.util.Log.e("SequenceScreen", "Error playing asset sound preview", e)
    }
}

private fun playSoundFilePreview(context: android.content.Context, soundFile: java.io.File, audioManager: android.media.AudioManager) {
    try {
        val mediaPlayer = android.media.MediaPlayer().apply {
            setDataSource(soundFile.absolutePath)
            setAudioStreamType(android.media.AudioManager.STREAM_NOTIFICATION)
            setOnCompletionListener {
                release()
                audioManager.abandonAudioFocus(null)
            }
            prepare()
            start()
        }
    } catch (e: Exception) {
        android.util.Log.e("SequenceScreen", "Error playing file sound preview", e)
    }
}

private fun playSoundSequencePreview(context: android.content.Context, soundSequence: List<SoundClip>, soundManager: SoundManager) {
    if (soundSequence.isEmpty()) return

    // For preview, play all available sounds randomly (up to 3 sounds)
    val previewSequence = soundSequence.shuffled().take(3)

    // Play the preview sequence with delays between sounds
    playPreviewSequenceWithDelay(context, previewSequence, soundManager, 0)
}

private fun playPreviewSequenceWithDelay(context: android.content.Context, sequence: List<SoundClip>, soundManager: SoundManager, index: Int) {
    if (index >= sequence.size) return

    val soundClip = sequence[index]

    // Play current sound
    playSoundClipPreview(context, soundClip, soundManager)

    // Schedule next sound after a delay (1.5 seconds)
    if (index < sequence.size - 1) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playPreviewSequenceWithDelay(context, sequence, soundManager, index + 1)
        }, 1500)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSoundDialog(
    period: TimePeriod,
    availableSounds: List<SoundClip>,
    onSoundSelected: (SoundClip) -> Unit,
    onAddNewSound: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${period.name} Sounds") },
        text = {
            Column {
                Text("${period.name} has ${period.sounds.size} sound(s)")
                Spacer(modifier = Modifier.height(16.dp))

                // Add New Sound button
                Button(
                    onClick = onAddNewSound,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add_circle),
                        contentDescription = "Add",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Sound")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Available sounds:")
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(availableSounds) { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSoundSelected(sound) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sound.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (sound.isUserAdded) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            painterResource(R.drawable.ic_custom_time_period),
                                            contentDescription = "User added",
                                            modifier = Modifier.size(16.dp),
                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                                Text(
                                    text = sound.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}