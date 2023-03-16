package dev.mobile.maestro

import android.app.UiAutomation
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlin.reflect.jvm.jvmName

object ToastAccessibilityListener : UiAutomation.OnAccessibilityEventListener {

    private var toastNode: AccessibilityNodeInfo? = null
    private var isListening = false
    private var recentToastTimeMillis: Long = 0

    private const val TOAST_LENGTH_LONG_DURATION = 3500

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        if (
            accessibilityEvent.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
            accessibilityEvent.className.toString().contains(Toast::class.jvmName)
        ) {
            recentToastTimeMillis = System.currentTimeMillis()
            Log.d("Maestro", "Toast received")
            toastNode = AccessibilityNodeInfo().apply {
                text = if (accessibilityEvent.text.size == 1) {
                    accessibilityEvent.text.first().toString()
                } else {
                    accessibilityEvent.text.joinToString { ", " }
                }
                className = Toast::class.jvmName
                isVisibleToUser = true
                viewIdResourceName = ""
                packageName = ""
                isCheckable = false
                isChecked = accessibilityEvent.isChecked
                isClickable = false
                isEnabled = accessibilityEvent.isEnabled
            }
        }
    }

    fun getToastAccessibilityNode() = toastNode

    fun isTimedOut(): Boolean {
        return System.currentTimeMillis() - recentToastTimeMillis > TOAST_LENGTH_LONG_DURATION
    }

    fun start(uiAutomation: UiAutomation): ToastAccessibilityListener {
        if (isListening) return this
        uiAutomation.setOnAccessibilityEventListener(this)
        isListening = true
        Log.d("Maestro", "Started listening to accessibility events")
        return this
    }

    fun stop() {
        isListening = false
        Log.d("Maestro", "Stopped listening to accessibility events")
    }
}