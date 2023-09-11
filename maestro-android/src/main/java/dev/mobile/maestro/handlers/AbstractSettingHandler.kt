package dev.mobile.maestro.handlers

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

abstract class AbstractSettingHandler(private val context: Context, private val permissions: List<String>) {
    fun enable(): Boolean {
        Log.d(TAG, "Enabling $settingDescription")
        return if (!hasPermissions()) {
            false
        } else setState(true)
    }

    fun disable(): Boolean {
        Log.d(TAG, "Disabling $settingDescription")
        return if (!hasPermissions()) {
            false
        } else setState(false)
    }

    protected fun hasPermissions(): Boolean {
        for (p in permissions) {
            if (context.checkCallingOrSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                val logMessage = String.format(
                    "The permission %s is not set. Cannot change state of %s.",
                    p, settingDescription
                )
                Log.e(TAG, logMessage)
                return false
            }
        }
        return true
    }

    abstract fun setState(state: Boolean): Boolean
    abstract val settingDescription: String

    companion object {
        private const val TAG = "Maestro"
    }
}