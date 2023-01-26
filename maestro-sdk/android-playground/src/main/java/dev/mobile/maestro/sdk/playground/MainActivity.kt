package dev.mobile.maestro.sdk.playground

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.mobile.maestro.sdk.MaestroSdk
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient.Builder()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Playground started")

        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            Log.d(TAG, "Generating URL")
            val baseUrl = MaestroSdk.mockServer().url("https://www.rijksmuseum.nl")
            Log.d(TAG, "URL: $baseUrl")

            Log.d(TAG, "Making request")
            val request = Request.Builder()
                .get()
                .url("$baseUrl/api/en/collection?key=mhaVinBw")
                .build()

            httpClient.newCall(request)
                .execute()
                .use {
                    val body = it.body?.string()
                    Log.d(TAG, "Response: ${it.code} $body")

                    runOnUiThread {
                        Log.d(TAG, "Updating UI")
                        findViewById<TextView>(R.id.result).text = body
                    }
                }
        }
    }

    companion object {

        private val TAG = "MaestroPlayground"

    }

}