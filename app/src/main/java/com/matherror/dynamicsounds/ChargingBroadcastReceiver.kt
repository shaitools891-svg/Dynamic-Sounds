package com.matherror.dynamicsounds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class ChargingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ChargingBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_POWER_CONNECTED) {
            Log.d(TAG, "Power connected")
        } else if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d(TAG, "Power disconnected")
        }
    }
}