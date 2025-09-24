package com.matherror.dynamicsounds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import java.io.File
import java.util.*

class NotificationSoundService : NotificationListenerService() {

    private val TAG = "NotificationSoundService"
    private var mediaPlayer: MediaPlayer? = null
    private var notificationCount = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var soundManager: SoundManager
    private lateinit var prefs: android.content.SharedPreferences
    private var currentTimePeriod = "" // Track current time period for sequence reset
    private var sequenceIndex = 0 // Track current position in sound sequence
    private var isPlayingSound = false // Prevent overlapping sounds
    private var lastNotificationTime = 0L // Debounce rapid notifications

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationSoundService created")

        // Initialize sound manager
        soundManager = SoundManager(this)

        // Initialize preferences
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Load sequence index and current time period
        sequenceIndex = prefs.getInt("sequence_index", 0)
        currentTimePeriod = prefs.getString("current_time_period", "") ?: ""

        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationSoundService::WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes

        // Start as foreground service
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndReleaseMediaPlayer()
        wakeLock?.release()
        wakeLock = null
        Log.d(TAG, "NotificationSoundService destroyed")
    }

    private fun startForegroundService() {
        val channelId = "notification_sound_service"
        val channelName = "Notification Sound Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for intercepting notifications and playing custom sounds"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notification Sound Service")
            .setContentText("Intercepting all notifications to play custom sounds")
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSound(null)
            .setVibrate(null)
            .setLights(0, 0, 0)
            .build()

        startForeground(1, notification)
        Log.d(TAG, "Foreground service started with enhanced configuration")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val currentTime = System.currentTimeMillis()

        // Debounce rapid notifications (ignore notifications within 1 second of each other)
        if (currentTime - lastNotificationTime < 1000) {
            Log.d(TAG, "Debouncing rapid notification")
            return
        }
        lastNotificationTime = currentTime

        // Skip if notification is from our own app
        if (sbn.packageName == packageName) {
            return
        }

        // Skip ongoing notifications if enabled
        val ignoreOngoing = prefs.getBoolean("ignore_ongoing_notifications", false)
        if (ignoreOngoing && sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            Log.d(TAG, "Skipping ongoing notification from: ${sbn.packageName}")
            return
        }

        // Check if app is selected for sound playback
        val selectedApps = getSelectedApps()
        if (selectedApps.isNotEmpty() && !selectedApps.contains(sbn.packageName)) {
            Log.d(TAG, "Skipping notification from unselected app: ${sbn.packageName}")
            return
        }

        // Skip if already playing a sound
        if (isPlayingSound) {
            Log.d(TAG, "Already playing sound, skipping notification from: ${sbn.packageName}")
            return
        }

        Log.d(TAG, "Notification posted from: ${sbn.packageName}")

        // Get current time period
        val timePeriod = getCurrentTimePeriod()
        Log.d(TAG, "Current time period: $timePeriod")

        // Play sound sequence for the time period
        playSoundSequence(timePeriod)
    }

    private fun cancelNotificationByKey(key: String) {
        try {
            cancelNotification(key)
            Log.d(TAG, "Cancelled notification: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification", e)
        }
    }

    private fun repostNotificationWithoutSound(sbn: StatusBarNotification) {
        try {
            // Create a new notification builder from the original
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = sbn.notification

            // Create a new notification without sound
            val newNotification = NotificationCompat.Builder(this, notification.channelId ?: "default")
                .setSmallIcon(R.drawable.ic_notification_bell) // Use our icon since we can't access the original
                .setContentTitle(notification.extras.getString(Notification.EXTRA_TITLE) ?: "Notification")
                .setContentText(notification.extras.getString(Notification.EXTRA_TEXT) ?: "")
                .setWhen(notification.`when`)
                .setShowWhen(notification.`when` != 0L)
                .setAutoCancel(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
                .setOngoing(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
                .setPriority(notification.priority)
                .setCategory(notification.category)
                .setVisibility(notification.visibility)
                .setSound(null) // Remove sound
                .setVibrate(null) // Remove vibration if we want to handle it ourselves
                .build()

            // Repost the notification
            notificationManager.notify(sbn.id, newNotification)
            Log.d(TAG, "Reposted notification without sound: ${sbn.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error reposting notification", e)
        }
    }

    private fun getCurrentTimePeriod(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..11 -> "Morning"
            in 12..13 -> "Noon"
            in 14..17 -> "Afternoon"
            in 18..21 -> "Evening"
            else -> "Night"
        }
    }

    private fun playSoundSequence(timePeriod: String) {
        val soundSequence = getSoundSequenceForPeriod(timePeriod)
        Log.d(TAG, "Found ${soundSequence.size} sounds for $timePeriod")
        if (soundSequence.isEmpty()) {
            Log.d(TAG, "No sounds configured for $timePeriod")
            return
        }

        // Reset sequence if time period changed
        if (currentTimePeriod != timePeriod) {
            currentTimePeriod = timePeriod
            sequenceIndex = 0
            Log.d(TAG, "Time period changed to $timePeriod, resetting sequence index to 0")
        }

        // Play sounds sequentially, then randomly
        val soundClip = if (sequenceIndex < soundSequence.size) {
            // Play next sound in sequence
            val clip = soundSequence[sequenceIndex]
            Log.d(TAG, "Playing sequential sound at index $sequenceIndex: ${clip.name}")
            sequenceIndex++
            clip
        } else {
            // All sounds played, now play random
            val clip = soundSequence.random()
            Log.d(TAG, "Playing random sound: ${clip.name}")
            clip
        }

        // Save updated sequence index and current time period
        prefs.edit()
            .putInt("sequence_index", sequenceIndex)
            .putString("current_time_period", currentTimePeriod)
            .apply()

        playSoundClip(soundClip)
    }

    private fun getSoundSequenceForPeriod(timePeriod: String): List<SoundClip> {
        val allSounds = soundManager.getAllSounds()
        val periodLower = timePeriod.lowercase()
        Log.d(TAG, "All sounds count: ${allSounds.size}")
        allSounds.take(3).forEach { sound ->
            Log.d(TAG, "Sound: ${sound.name}, assetPath: ${sound.assetPath}, isUserAdded: ${sound.isUserAdded}")
        }
        val filteredSounds = allSounds.filter { it.assetPath?.startsWith(periodLower) == true }
        Log.d(TAG, "Filtered sounds for $periodLower: ${filteredSounds.size}")
        filteredSounds.forEach { sound ->
            Log.d(TAG, "Filtered sound: ${sound.name}, assetPath: ${sound.assetPath}")
        }
        return filteredSounds
    }

    private fun playSoundClip(soundClip: SoundClip) {
        Log.d(TAG, "Playing sound clip: ${soundClip.name}")

        if (soundClip.name == "Silent") {
            Log.d(TAG, "Silent mode - no sound played")
            return
        }

        if (soundClip.name == "Vibrate Only") {
            // TODO: Implement vibration
            Log.d(TAG, "Vibrate only mode - vibration not implemented yet")
            return
        }

        // Set playing flag
        isPlayingSound = true

        try {
            // Try to play asset sound
            if (soundClip.assetPath != null) {
                Log.d(TAG, "Playing asset sound: ${soundClip.assetPath}")
                playSoundFromAssets(soundClip.assetPath!!)
                return
            }

            // Try to play user-added sound
            if (soundClip.isUserAdded && soundClip.userSoundId != null) {
                val soundFile = soundManager.getSoundFile(soundClip.userSoundId!!)
                Log.d(TAG, "User sound file: ${soundFile?.absolutePath}, exists: ${soundFile?.exists()}")
                if (soundFile != null && soundFile.exists()) {
                    playSoundFile(soundFile)
                    return
                }
            }

            // Fallback: no playable sound found
            Log.d(TAG, "No playable sound found for: ${soundClip.name}")
        } finally {
            // Reset playing flag after a short delay to allow for sound completion
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isPlayingSound = false
            }, 2000) // 2 seconds should be enough for most notification sounds
        }
    }

    private fun playSound(soundResId: Int) {
        if (soundResId == 0) return

        try {
            // Stop and release any existing MediaPlayer
            stopAndReleaseMediaPlayer()

            mediaPlayer = MediaPlayer.create(this, soundResId).apply {
                setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                setOnCompletionListener {
                    stopAndReleaseMediaPlayer()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAndReleaseMediaPlayer()
                    true
                }
                start()
            }

            Log.d(TAG, "Playing sound alongside system: $soundResId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
            isPlayingSound = false
        }
    }

    private fun stopAndReleaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping/releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun playSoundFile(soundFile: File) {
        try {
            // Stop and release any existing MediaPlayer
            stopAndReleaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(soundFile.absolutePath)
                setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                setOnCompletionListener {
                    stopAndReleaseMediaPlayer()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAndReleaseMediaPlayer()
                    true
                }
                prepare()
                start()
            }

            Log.d(TAG, "Playing sound file alongside system: ${soundFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound file", e)
            isPlayingSound = false
        }
    }

    private fun playSoundFromAssets(assetPath: String) {
        try {
            // Stop and release any existing MediaPlayer
            stopAndReleaseMediaPlayer()

            val afd = assets.openFd(assetPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                setOnCompletionListener {
                    try {
                        afd.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing asset file descriptor", e)
                    }
                    stopAndReleaseMediaPlayer()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAndReleaseMediaPlayer()
                    true
                }
                prepare()
                start()
            }

            Log.d(TAG, "Playing sound from assets alongside system: $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound from assets", e)
            isPlayingSound = false
        }
    }


    private fun getSelectedApps(): Set<String> {
        val appSelectionPrefs = getSharedPreferences("app_selection", Context.MODE_PRIVATE)
        val appsJson = appSelectionPrefs.getString("selected_apps", "[]")
        return try {
            Gson().fromJson(appsJson, Array<String>::class.java).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app name for $packageName", e)
            null
        }
    }

}