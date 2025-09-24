package com.matherror.dynamicsounds

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class UserSoundClip(
    val id: String,
    val name: String,
    val fileName: String, // Internal storage filename
    val originalUri: String? = null, // Original URI for reference
    val description: String = ""
)

class SoundManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sound_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Directory for storing user sound files
    private val soundsDir: File = File(context.filesDir, "user_sounds").apply {
        if (!exists()) mkdirs()
    }

    // In-memory list of user sounds
    val userSounds: SnapshotStateList<UserSoundClip> = mutableStateListOf()

    init {
        loadUserSounds()
    }

    private fun loadUserSounds() {
        val soundsJson = prefs.getString("user_sounds", "[]")
        try {
            val type = object : TypeToken<List<UserSoundClip>>() {}.type
            val sounds = gson.fromJson<List<UserSoundClip>>(soundsJson, type)
            userSounds.clear()
            userSounds.addAll(sounds ?: emptyList())
            Log.d("SoundManager", "Loaded ${sounds?.size ?: 0} user sounds")
        } catch (e: Exception) {
            Log.e("SoundManager", "Error loading user sounds from prefs, clearing corrupted data", e)
            userSounds.clear()
            prefs.edit().remove("user_sounds").apply()
        }
    }

    private fun saveUserSounds() {
        val soundsJson = gson.toJson(userSounds.toList())
        prefs.edit().putString("user_sounds", soundsJson).apply()
    }

    fun addSoundFromUri(uri: Uri, displayName: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val fileName = "${UUID.randomUUID()}.mp3" // Assume mp3 for simplicity
                val outputFile = File(soundsDir, fileName)

                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }

                val userSound = UserSoundClip(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    fileName = fileName,
                    originalUri = uri.toString()
                )

                userSounds.add(userSound)
                saveUserSounds()

                Log.d("SoundManager", "Added sound: $displayName")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("SoundManager", "Error adding sound", e)
            false
        }
    }

    fun removeSound(soundId: String): Boolean {
        val sound = userSounds.find { it.id == soundId }
        return if (sound != null) {
            // Delete the file
            val file = File(soundsDir, sound.fileName)
            file.delete()

            // Remove from list
            userSounds.remove(sound)
            saveUserSounds()

            Log.d("SoundManager", "Removed sound: ${sound.name}")
            true
        } else {
            false
        }
    }

    fun getSoundFile(soundId: String): File? {
        val sound = userSounds.find { it.id == soundId }
        return sound?.let { File(soundsDir, it.fileName) }
    }

    fun getAllSounds(): List<SoundClip> {
        val assetSounds = mutableListOf<SoundClip>()

        // Time period folders in assets
        val timePeriods = listOf("morning", "noon", "afternoon", "evening", "night")

        for (period in timePeriods) {
            try {
                val files = context.assets.list(period)
                files?.forEach { fileName ->
                    val name = fileName.substringBeforeLast(".")
                    val assetPath = "$period/$fileName"
                    assetSounds.add(SoundClip(name, "Asset sound from $period", assetPath = assetPath))
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error loading assets for $period", e)
            }
        }

        val userSoundClips = userSounds.map { userSound ->
            SoundClip(userSound.name, userSound.description, isUserAdded = true, userSoundId = userSound.id)
        }

        return assetSounds + userSoundClips
    }
}

// Extension to SoundClip to include user-added properties
data class SoundClip(
    val name: String,
    val description: String,
    val isPrimary: Boolean = false,
    val isUserAdded: Boolean = false,
    val userSoundId: String? = null,
    val assetPath: String? = null
)