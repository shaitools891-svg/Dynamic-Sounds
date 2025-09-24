package com.matherror.dynamicsounds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting NotificationSoundService")

            // Start the notification listener service after boot
            val serviceIntent = Intent(context, NotificationSoundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}