package maestro.js

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.graalvm.polyglot.HostAccess.Export
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.File

class GraalJsHttp(
    private val httpClient: OkHttpClient
) {

    @JvmOverloads
    @Export
    fun get(
        url: String,
        params: Map<String, Any>? = null,
    ): Any {
        return executeRequest(url, "GET", params)
    }

    @JvmOverloads
    @Export
    fun post(
        url: String,
        params: Map<String, Any>? = null,
    ): Any {
        return executeRequest(url, "POST", params)
    }

    @JvmOverloads
    @Export
    fun put(
        url: String,
        params: Map<String, Any>? = null,
    ): Any {
        return executeRequest(url, "PUT", params)
    }

    @JvmOverloads
    @Export
    fun delete(
        url: String,
        params: Map<String, Any>? = null,
    ): Any {
        return executeRequest(url, "DELETE", params)
    }

    @JvmOverloads
    @Export
    fun request(
        url: String,
        params: Map<String, Any>? = null,
    ): Any {
        val method = params?.get("method") as? String ?: "GET"
        return executeRequest(
            url,
            method,
            params,
        )
    }

    private fun executeRequest(
        url: String,
        method: String,
        params: Map<String, Any>?,
    ): Any {
        val requestBuilder = Request.Builder()
            .url(url)

        val multipartBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        val body = params?.get("body") as? String

        val multipartForm = params?.get("multipartForm") as? Map<*, *> ?: emptyMap<Any, Any>()
        multipartForm.forEach { (key, value) ->
            val filePath = (value as? Map<*, *> ?: emptyMap<Any, Any>())["filePath"]
            if (filePath != null) {
                val file = File(filePath.toString())
                val mediaType = (value as? Map<*, *> ?: emptyMap<Any, Any>())["mediaType"].toString()
                multipartBodyBuilder.addFormDataPart(key.toString(), file.name, file.asRequestBody(mediaType.toMediaTypeOrNull()))
            } else {
                multipartBodyBuilder.addFormDataPart(key.toString(), value.toString())
            }
        }

        if (multipartForm.isEmpty()) {
            requestBuilder.method(method, body?.toRequestBody())
        } else {
            requestBuilder.method(method, multipartBodyBuilder.build())
        }

        val headers: Map<*, *> = params?.get("headers") as? Map<*, *> ?: emptyMap<Any, Any>()

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key.toString(), value.toString())
        }

        val request = requestBuilder.build()

        val response = httpClient
            .newCall(request)
            .execute()

        return ProxyObject.fromMap(mapOf(
            "ok" to response.isSuccessful,
            "status" to response.code,
            "body" to response.body?.string(),
        ))
    }
}