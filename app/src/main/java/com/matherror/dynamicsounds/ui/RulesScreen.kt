package com.matherror.dynamicsounds.ui

import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matherror.dynamicsounds.R
import com.matherror.dynamicsounds.SoundClip
import com.matherror.dynamicsounds.SoundManager
import com.matherror.dynamicsounds.ui.theme.PrimaryGreen
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TimePeriodRule(
    val name: String,
    val startTime: String, // HH:mm format
    val endTime: String, // HH:mm format
    val icon: Int
) {
    val timeRange: String
        get() = "$startTime-$endTime"
}

// Persistence functions
private val validIcons = setOf(
    R.drawable.ic_morning,
    R.drawable.ic_sunny,
    R.drawable.ic_afternoon,
    R.drawable.ic_evening,
    R.drawable.ic_night
)

private fun loadTimePeriodRules(context: android.content.Context): List<TimePeriodRule> {
    val prefs: SharedPreferences = context.getSharedPreferences("rules_prefs", android.content.Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString("time_period_rules", null)
    val defaults = listOf(
        TimePeriodRule(
            name = "Morning",
            startTime = "06:00",
            endTime = "12:00",
            icon = R.drawable.ic_morning
        ),
        TimePeriodRule(
            name = "Noon",
            startTime = "12:00",
            endTime = "14:00",
            icon = R.drawable.ic_sunny
        ),
        TimePeriodRule(
            name = "Afternoon",
            startTime = "14:00",
            endTime = "18:00",
            icon = R.drawable.ic_afternoon
        ),
        TimePeriodRule(
            name = "Evening",
            startTime = "18:00",
            endTime = "22:00",
            icon = R.drawable.ic_evening
        ),
        TimePeriodRule(
            name = "Night",
            startTime = "22:00",
            endTime = "06:00",
            icon = R.drawable.ic_night
        )
    )

    val result = if (json != null) {
        try {
            val type = object : TypeToken<List<TimePeriodRule>>() {}.type
            val loaded: List<TimePeriodRule>? = gson.fromJson(json, type)
            android.util.Log.d("RulesScreen", "Loaded time period rules: ${loaded?.size ?: 0} items")
            val validLoaded = loaded?.filter { it.icon in validIcons && it.name.isNotBlank() && it.startTime.matches(Regex("\\d{2}:\\d{2}")) && it.endTime.matches(Regex("\\d{2}:\\d{2}")) }
            if (validLoaded != null && validLoaded.size == loaded.size) {
                validLoaded
            } else {
                android.util.Log.w("RulesScreen", "Loaded data contains invalid entries, using defaults")
                defaults
            }
        } catch (e: Exception) {
            android.util.Log.e("RulesScreen", "Error loading time period rules from prefs, clearing corrupted data and using defaults", e)
            prefs.edit().remove("time_period_rules").apply()
            defaults
        }
    } else {
        android.util.Log.d("RulesScreen", "No saved time period rules, using defaults")
        defaults
    }

    return result
}

private fun saveTimePeriodRules(context: android.content.Context, rules: List<TimePeriodRule>) {
    val prefs: SharedPreferences = context.getSharedPreferences("rules_prefs", android.content.Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(rules)
    prefs.edit().putString("time_period_rules", json).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(modifier: Modifier = Modifier) {
    android.util.Log.d("RulesScreen", "RulesScreen composable started")
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    var timePeriods by remember { mutableStateOf(loadTimePeriodRules(context)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerPeriod by remember { mutableStateOf<TimePeriodRule?>(null) }
    var isPickingStartTime by remember { mutableStateOf(true) }

    // Save time periods whenever they change
    LaunchedEffect(timePeriods) {
        saveTimePeriodRules(context, timePeriods)
    }

    fun playSoundPreview(periodName: String) {
        val soundSequence = getSoundSequenceForPeriod(periodName, soundManager)
        if (soundSequence.isNotEmpty()) {
            // Play the full sequence like the notification service does
            playSoundSequencePreview(context, soundSequence, soundManager)
            Toast.makeText(context, "Playing $periodName sound sequence", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No sounds available for $periodName", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePeriodTime(periodName: String, startTime: String, endTime: String) {
        timePeriods = timePeriods.map { period ->
            if (period.name == periodName) {
                period.copy(startTime = startTime, endTime = endTime)
            } else {
                period
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Periods", fontWeight = FontWeight.Bold) }
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
                TimePeriodCard(period, onEditTime = { isStart ->
                    timePickerPeriod = period
                    isPickingStartTime = isStart
                    showTimePicker = true
                }, onPreview = {
                    playSoundPreview(period.name)
                })
            }
        }
    }


    if (showTimePicker && timePickerPeriod != null) {
        val initialTime = if (isPickingStartTime) {
            LocalTime.parse(timePickerPeriod!!.startTime, DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            LocalTime.parse(timePickerPeriod!!.endTime, DateTimeFormatter.ofPattern("HH:mm"))
        }

        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false // Force 12-hour format with AM/PM
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        painterResource(timePickerPeriod!!.icon),
                        contentDescription = timePickerPeriod!!.name,
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Set ${if (isPickingStartTime) "Start" else "End"} Time for ${timePickerPeriod!!.name}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Preview of selected time
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = formatTimeForDisplay(String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        if (isPickingStartTime) {
                            updatePeriodTime(timePickerPeriod!!.name, selectedTime, timePickerPeriod!!.endTime)
                        } else {
                            updatePeriodTime(timePickerPeriod!!.name, timePickerPeriod!!.startTime, selectedTime)
                        }
                        showTimePicker = false
                        timePickerPeriod = null
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimePeriodCard(period: TimePeriodRule, onEditTime: (Boolean) -> Unit, onPreview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(period.icon),
                    contentDescription = period.name,
                    modifier = Modifier.size(24.dp),
                    tint = PrimaryGreen
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = period.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time range display with separate start and end times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start time
                TimeDisplayCard(
                    label = "Start",
                    time = period.startTime,
                    modifier = Modifier.weight(1f),
                    onClick = { onEditTime(true) }
                )

                // End time
                TimeDisplayCard(
                    label = "End",
                    time = period.endTime,
                    modifier = Modifier.weight(1f),
                    onClick = { onEditTime(false) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview button
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
                ),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f))
            ) {
                Icon(
                    painterResource(R.drawable.ic_play),
                    contentDescription = "Preview",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Sound", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Instruction text
            Text(
                text = "Tap time to edit • Tap preview to hear",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun TimeDisplayCard(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTimeForDisplay(time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

private fun formatTimeForDisplay(timeString: String): String {
    return try {
        val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: Exception) {
        timeString
    }
}

private fun getSoundSequenceForPeriod(timePeriod: String, soundManager: SoundManager): List<SoundClip> {
    val allSounds = soundManager.getAllSounds()
    val periodLower = timePeriod.lowercase()
    return allSounds.filter { it.assetPath?.startsWith(periodLower) == true }
}

private fun playSoundClipPreview(context: android.content.Context, soundClip: SoundClip, soundManager: SoundManager) {
    if (soundClip.name == "Silent") {
        return
    }

    try {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

        // Request audio focus
        val result = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
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
        android.util.Log.e("RulesScreen", "Error playing sound preview", e)
    }
}

private fun playSoundFromAssetsPreview(context: android.content.Context, assetPath: String, audioManager: AudioManager) {
    try {
        val afd = context.assets.openFd(assetPath)
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
            setOnCompletionListener {
                afd.close()
                release()
                audioManager.abandonAudioFocus(null)
            }
            prepare()
            start()
        }
    } catch (e: Exception) {
        android.util.Log.e("RulesScreen", "Error playing asset sound preview", e)
    }
}

private fun playSoundFilePreview(context: android.content.Context, soundFile: java.io.File, audioManager: AudioManager) {
    try {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(soundFile.absolutePath)
            setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
            setOnCompletionListener {
                release()
                audioManager.abandonAudioFocus(null)
            }
            prepare()
            start()
        }
    } catch (e: Exception) {
        android.util.Log.e("RulesScreen", "Error playing file sound preview", e)
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
