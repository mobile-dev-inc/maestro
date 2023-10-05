package dev.mobile.maestro

import android.os.Bundle
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log
import dev.mobile.maestro.receivers.HasAction
import dev.mobile.maestro.receivers.LocaleSettingReceiver

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        Log.d(TAG, "Entering the app")

        registerSettingsReceivers(listOf(
            LocaleSettingReceiver::class.java
        ))
    }

    private fun registerSettingsReceivers(receiverClasses: List<Class<out BroadcastReceiver>>) {
        for (receiverClass in receiverClasses) {
            try {
                val receiver = receiverClass.newInstance()
                val filter = IntentFilter((receiver as HasAction).action())
                applicationContext.registerReceiver(receiver, filter)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "Maestro"
    }
}