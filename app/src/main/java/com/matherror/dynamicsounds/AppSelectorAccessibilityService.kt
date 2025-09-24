package com.matherror.dynamicsounds

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppSelectorAccessibilityService : AccessibilityService() {

    private val TAG = "AppSelectorAccessibility"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppSelectorAccessibilityService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppSelectorAccessibilityService destroyed")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppSelectorAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We don't need to handle events for app selection
        // This service is just for permission purposes
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppSelectorAccessibilityService interrupted")
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val expectedComponentName = android.content.ComponentName(context, AppSelectorAccessibilityService::class.java)
            val enabledServicesSetting = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
                if (enabledService != null && enabledService == expectedComponentName) {
                    return true
                }
            }
            return false
        }
    }
}