package dev.mobile.maestro

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityNodeInfoExt {

    /**
     * Retrieves the hint text associated with this [android.view.accessibility.AccessibilityNodeInfo].
     *
     * If the device API level is below 26 (Oreo), this function provides a fallback
     * by returning an empty CharSequence instead. If both hint text and content description
     * are unavailable, it returns an empty string.
     *
     * @return [CharSequence] representing the hint text or its fallback.
     */
    fun AccessibilityNodeInfo.getHintOrFallback(): CharSequence {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.hintText
        } else {
            ""
        }
    }

}
