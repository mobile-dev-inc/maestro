package maestro.ios

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import maestro.api.XCTestDriverClient
import maestro.debuglog.DebugLogStore
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sun.security.krb5.Confounder.bytes
import java.util.concurrent.TimeUnit

object GetRunningAppIdResolver {
    private val logger = DebugLogStore.loggerFor(GetRunningAppIdResolver::class.java)

    fun getRunningAppId(): String? {
        val appIds = Simctl.listApps()
        logger.info("installed apps: $appIds")

        return XCTestDriverClient.runningAppId(appIds).use { response ->
            val runningAppBundleId = if (response.isSuccessful) {
                val gson = Gson()
                response.body?.let { body ->
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val responseBody: Map<String, Any> = gson.fromJson(String(body.bytes()), type)

                    responseBody["runningAppBundleId"] as? String
                }
            } else {
                logger.info("request to resolve running app id failed with exception - ${response.body?.toString()}")

                return null
            }

            logger.info("found running app id $runningAppBundleId")

            runningAppBundleId
        }
    }
}