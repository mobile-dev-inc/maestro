package maestro.ios

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import maestro.debuglog.DebugLogStore
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GetRunningAppIdResolver {
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val logger = DebugLogStore.loggerFor(GetRunningAppIdResolver::class.java)

    fun getRunningAppId(): String {
        val gson = Gson()
        val appIds = GetRunningAppRequest(Simctl.listApps())

        logger.info("installed apps: $appIds")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(appIds).toRequestBody(mediaType)

        val httpUrl = HttpUrl.Builder()
            .scheme("http")
            .host("localhost")
            .addPathSegment("runningApp")
            .port(9080)
            .build()
        val request = Request.Builder()
            .addHeader("contentType", "application/json")
            .url(httpUrl)
            .post(body)
            .build()
        val response = okHttpClient.newCall(request).execute()
        val runningAppBundleId = if (response.isSuccessful) {
            response.body?.let {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val responseBody: Map<String, Any> = gson.fromJson(String(it.bytes()), type)

                responseBody["runningAppBundleId"] as? String
            } ?: throw IllegalStateException("View Hierarchy not available, response body is null")
        } else {
            throw IllegalArgumentException("Maestro was not able to capture view hierarchy. Run maestro bugreport command and submit " +
                "new github issue on https://github.com/mobile-dev-inc/maestro/issues/new with the bugreport created.")
        }

        logger.info("found running app id $runningAppBundleId")
        
        return runningAppBundleId
    }
}