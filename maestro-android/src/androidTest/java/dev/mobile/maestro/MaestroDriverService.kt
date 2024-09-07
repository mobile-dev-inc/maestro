package dev.mobile.maestro

import android.app.UiAutomation
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.graphics.Bitmap
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_1
import android.view.KeyEvent.KEYCODE_4
import android.view.KeyEvent.KEYCODE_5
import android.view.KeyEvent.KEYCODE_6
import android.view.KeyEvent.KEYCODE_7
import android.view.KeyEvent.KEYCODE_APOSTROPHE
import android.view.KeyEvent.KEYCODE_AT
import android.view.KeyEvent.KEYCODE_BACKSLASH
import android.view.KeyEvent.KEYCODE_COMMA
import android.view.KeyEvent.KEYCODE_EQUALS
import android.view.KeyEvent.KEYCODE_GRAVE
import android.view.KeyEvent.KEYCODE_LEFT_BRACKET
import android.view.KeyEvent.KEYCODE_MINUS
import android.view.KeyEvent.KEYCODE_NUMPAD_ADD
import android.view.KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN
import android.view.KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN
import android.view.KeyEvent.KEYCODE_PERIOD
import android.view.KeyEvent.KEYCODE_POUND
import android.view.KeyEvent.KEYCODE_RIGHT_BRACKET
import android.view.KeyEvent.KEYCODE_SEMICOLON
import android.view.KeyEvent.KEYCODE_SLASH
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.KeyEvent.KEYCODE_STAR
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_CAPS_LOCK_ON
import android.view.KeyEvent.META_CTRL_ON
import android.view.KeyEvent.META_FUNCTION_ON
import android.view.KeyEvent.META_META_ON
import android.view.KeyEvent.META_NUM_LOCK_ON
import android.view.KeyEvent.META_SCROLL_LOCK_ON
import android.view.KeyEvent.META_SHIFT_LEFT_ON
import android.view.KeyEvent.META_SHIFT_ON
import android.view.KeyEvent.META_SYM_ON
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiDeviceExt.clickExt
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import maestro_android.MaestroAndroid
import maestro_android.MaestroDriverGrpc
import maestro_android.addMediaResponse
import maestro_android.checkWindowUpdatingResponse
import maestro_android.deviceInfo
import maestro_android.eraseAllTextResponse
import maestro_android.inputTextResponse
import maestro_android.launchAppResponse
import maestro_android.screenshotResponse
import maestro_android.setLocationResponse
import maestro_android.tapResponse
import maestro_android.viewHierarchyResponse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.system.measureTimeMillis

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MaestroDriverService {

    @Test
    fun grpcServer() {
        Configurator.getInstance()
            .setActionAcknowledgmentTimeout(0L)
            .setWaitForIdleTimeout(0L)
            .setWaitForSelectorTimeout(0L)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        val uiAutomation = instrumentation.uiAutomation

        val port = InstrumentationRegistry.getArguments().getString("port", "7001").toInt()

        println("Server running on port [ $port ]")

        NettyServerBuilder.forPort(port)
            .addService(Service(uiDevice, uiAutomation))
            .build()
            .start()

        while (!Thread.interrupted()) {
            Thread.sleep(100)
        }
    }
}

class Service(
    private val uiDevice: UiDevice,
    private val uiAutomation: UiAutomation,
) : MaestroDriverGrpc.MaestroDriverImplBase() {

    private val geoHandler = Handler(Looper.getMainLooper())
    private var locationCounter = 0
    private val toastAccessibilityListener = ToastAccessibilityListener.start(uiAutomation)

    override fun launchApp(
        request: MaestroAndroid.LaunchAppRequest,
        responseObserver: StreamObserver<MaestroAndroid.LaunchAppResponse>
    ) {
        try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            val intent = context.packageManager.getLaunchIntentForPackage(request.packageName)

            if (intent == null) {
                Log.e("Maestro", "No launcher intent found for package ${request.packageName}")
                responseObserver.onError(RuntimeException("No launcher intent found for package ${request.packageName}"))
                return
            }

            request.argumentsList
                .forEach {
                    when (it.type) {
                        String::class.java.name -> intent.putExtra(it.key, it.value)
                        Boolean::class.java.name -> intent.putExtra(it.key, it.value.toBoolean())
                        Int::class.java.name -> intent.putExtra(it.key, it.value.toInt())
                        Double::class.java.name -> intent.putExtra(it.key, it.value.toDouble())
                        Long::class.java.name -> intent.putExtra(it.key, it.value.toLong())
                        else -> intent.putExtra(it.key, it.value)
                    }
                }
            context.startActivity(intent)

            responseObserver.onNext(launchAppResponse { })
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    override fun deviceInfo(
        request: MaestroAndroid.DeviceInfoRequest,
        responseObserver: StreamObserver<MaestroAndroid.DeviceInfo>
    ) {
        try {
            val windowManager = InstrumentationRegistry.getInstrumentation()
                .context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            responseObserver.onNext(
                deviceInfo {
                    widthPixels = displayMetrics.widthPixels
                    heightPixels = displayMetrics.heightPixels
                }
            )
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    override fun viewHierarchy(
        request: MaestroAndroid.ViewHierarchyRequest,
        responseObserver: StreamObserver<MaestroAndroid.ViewHierarchyResponse>
    ) {
        try {
            refreshAccessibilityCache()
            val stream = ByteArrayOutputStream()

            val ms = measureTimeMillis {
                if (toastAccessibilityListener.getToastAccessibilityNode() != null && !toastAccessibilityListener.isTimedOut()) {
                    Log.d("Maestro", "Requesting view hierarchy with toast")
                    ViewHierarchy.dump(
                        uiDevice,
                        uiAutomation,
                        stream,
                        toastAccessibilityListener.getToastAccessibilityNode()
                    )
                } else {
                    Log.d("Maestro", "Requesting view hierarchy")
                    ViewHierarchy.dump(
                        uiDevice,
                        uiAutomation,
                        stream
                    )
                }
            }
            Log.d("Maestro", "View hierarchy received in $ms ms")

            responseObserver.onNext(
                viewHierarchyResponse {
                    hierarchy = stream.toString(Charsets.UTF_8.name())
                }
            )
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    /**
     * Clears the in-process Accessibility cache, removing any stale references. Because the
     * AccessibilityInteractionClient singleton stores copies of AccessibilityNodeInfo instances,
     * calls to public APIs such as `recycle` do not guarantee cached references get updated.
     */
    private fun refreshAccessibilityCache() {
        try {
            uiDevice.waitForIdle(500)
            uiAutomation.serviceInfo = null
        } catch (nullExp: NullPointerException) {
            /* no-op */
        }
    }

    override fun tap(
        request: MaestroAndroid.TapRequest,
        responseObserver: StreamObserver<MaestroAndroid.TapResponse>
    ) {
        try {
            uiDevice.clickExt(
                request.x,
                request.y
            )

            responseObserver.onNext(tapResponse {})
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    override fun addMedia(responseObserver: StreamObserver<MaestroAndroid.AddMediaResponse>): StreamObserver<MaestroAndroid.AddMediaRequest> {
        return object : StreamObserver<MaestroAndroid.AddMediaRequest> {

            var outputStream: OutputStream? = null

            override fun onNext(value: MaestroAndroid.AddMediaRequest) {
                if (outputStream == null) {
                    outputStream = MediaStorage.getOutputStream(
                        value.mediaName,
                        value.mediaExt
                    )
                }
                value.payload.data.writeTo(outputStream)
            }

            override fun onError(t: Throwable) {
                responseObserver.onError(t.internalError())
            }

            override fun onCompleted() {
                responseObserver.onNext(addMediaResponse {  })
                responseObserver.onCompleted()
            }

        }
    }

    override fun eraseAllText(
        request: MaestroAndroid.EraseAllTextRequest,
        responseObserver: StreamObserver<MaestroAndroid.EraseAllTextResponse>
    ) {
        try {
            val charactersToErase = request.charactersToErase
            Log.d("Maestro", "Erasing text $charactersToErase")

            for (i in 0..charactersToErase) {
                uiDevice.pressDelete()
            }

            responseObserver.onNext(eraseAllTextResponse { })
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    override fun inputText(
        request: MaestroAndroid.InputTextRequest,
        responseObserver: StreamObserver<MaestroAndroid.InputTextResponse>
    ) {
        try {
            Log.d("Maestro", "Inputting text")
            setText(request.text)
            responseObserver.onNext(inputTextResponse { })
            responseObserver.onCompleted()
        } catch (e: Throwable) {
            responseObserver.onError(e.internalError())
        }
    }

    override fun screenshot(
        request: MaestroAndroid.ScreenshotRequest,
        responseObserver: StreamObserver<MaestroAndroid.ScreenshotResponse>
    ) {
        val outputStream = ByteString.newOutput()
        val bitmap = uiAutomation.takeScreenshot()
        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
            responseObserver.onNext(screenshotResponse {
                bytes = outputStream.toByteString()
            })
            responseObserver.onCompleted()
        } else {
            Log.e("Maestro", "Failed to compress bitmap")
            responseObserver.onError(Throwable("Failed to compress bitmap").internalError())
        }
    }

    override fun isWindowUpdating(
        request: MaestroAndroid.CheckWindowUpdatingRequest,
        responseObserver: StreamObserver<MaestroAndroid.CheckWindowUpdatingResponse>
    ) {
        try {
            responseObserver.onNext(checkWindowUpdatingResponse {
                isWindowUpdating = uiDevice.waitForWindowUpdate(request.appId, 500)
            })
            responseObserver.onCompleted()
        } catch(e: Throwable) {
            responseObserver.onError(e.internalError())
        }
    }

    override fun setLocation(
        request: MaestroAndroid.SetLocationRequest,
        responseObserver: StreamObserver<MaestroAndroid.SetLocationResponse>
    ) {
        try {
            locationCounter++
            val version = locationCounter

            geoHandler.removeCallbacksAndMessages(null)

            val latitude = request.latitude
            val longitude = request.longitude
            val accuracy = 1F

            val locMgr = InstrumentationRegistry.getInstrumentation()
                .context
                .getSystemService(LOCATION_SERVICE) as LocationManager

            locMgr.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE
            )

            val newLocation = Location(LocationManager.GPS_PROVIDER)

            newLocation.latitude = latitude
            newLocation.longitude = longitude
            newLocation.accuracy = accuracy
            newLocation.altitude = 0.0
            locMgr.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

            fun postLocation() {
                geoHandler.post {
                    if (locationCounter != version) {
                        return@post
                    }

                    newLocation.setTime(System.currentTimeMillis())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        newLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    }
                    locMgr.setTestProviderStatus(
                        LocationManager.GPS_PROVIDER,
                        LocationProvider.AVAILABLE,
                        null, System.currentTimeMillis()
                    )

                    locMgr.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation)

                    postLocation()
                }
            }

            postLocation()

            responseObserver.onNext(setLocationResponse { })
            responseObserver.onCompleted()
        } catch (t: Throwable) {
            responseObserver.onError(t.internalError())
        }
    }

    private fun setText(text: String) = text
        .map { it.toKeyCodeAndMeta() }
        .filterNotNull()
        .fold(emptyList<Pair<List<Int>, Int>>()) { acc, elem ->
            if (acc.isEmpty() || acc.last().second != elem.second) acc + listOf(listOf(elem.first) to elem.second)
            else acc.dropLast(1) + listOf(acc.last().let { (it.first + listOf(elem.first)) to it.second })
        }.forEach {
            val codes = it.first.map(KeyEvent::keyCodeToString).joinToString(",") { it.replace("KEYCODE_", "") }
            val meta = it.second.metaStateToString()
            Log.d("Maestro", "pressKeyCodes(codes='$codes', meta='$meta')")
            uiDevice.pressKeyCodes(it.first.toIntArray(), it.second ?: 0)
        }

    private fun Int.metaStateToString() = with (KeyEvent.normalizeMetaState(this)) {
        val metas = listOf(
            META_SHIFT_ON to "SHIFT",
            META_ALT_ON to "ALT",
            META_CTRL_ON to "CTRL",
            META_CAPS_LOCK_ON to "CAPS_LOCK",
            META_NUM_LOCK_ON to "NUM_LOCK",
            META_SCROLL_LOCK_ON to "SCROLL_LOCK",
            META_FUNCTION_ON to "FUNCTION",
            META_SYM_ON to "SYM",
            META_META_ON to "META",
        )
        if (KeyEvent.metaStateHasNoModifiers(this)) "<no modifier>"
        else metas.filter { (this and it.first) != 0 }.joinToString(",") { it.second }.let { "($it)" }
    }

    private fun Char.toKeyCodeAndMeta(): Pair<Int, Int>? = when (code) {
        in 48..57 -> {
            /** 0~9 **/
            (code - 41).keyCode()
        }
        in 65..90 -> {
            /** A~Z **/
            (code - 36).keyCodeShifted()
        }
        in 97..122 -> {
            /** a~z **/
            (code - 68).keyCode()
        }
        ';'.code -> KEYCODE_SEMICOLON.keyCode()
        '='.code -> KEYCODE_EQUALS.keyCode()
        ','.code -> KEYCODE_COMMA.keyCode()
        '-'.code -> KEYCODE_MINUS.keyCode()
        '.'.code -> KEYCODE_PERIOD.keyCode()
        '/'.code -> KEYCODE_SLASH.keyCode()
        '`'.code -> KEYCODE_GRAVE.keyCode()
        '\''.code -> KEYCODE_APOSTROPHE.keyCode()
        '['.code -> KEYCODE_LEFT_BRACKET.keyCode()
        ']'.code -> KEYCODE_RIGHT_BRACKET.keyCode()
        '\\'.code -> KEYCODE_BACKSLASH.keyCode()
        ' '.code -> KEYCODE_SPACE.keyCode()
        '@'.code -> KEYCODE_AT.keyCode()
        '#'.code -> KEYCODE_POUND.keyCode()
        '*'.code -> KEYCODE_STAR.keyCode()
        '('.code -> KEYCODE_NUMPAD_LEFT_PAREN.keyCode()
        ')'.code -> KEYCODE_NUMPAD_RIGHT_PAREN.keyCode()
        '+'.code -> KEYCODE_NUMPAD_ADD.keyCode()
        '!'.code -> KEYCODE_1.keyCodeShifted()
        '$'.code -> KEYCODE_4.keyCodeShifted()
        '%'.code -> KEYCODE_5.keyCodeShifted()
        '^'.code -> KEYCODE_6.keyCodeShifted()
        '&'.code -> KEYCODE_7.keyCodeShifted()
        '"'.code -> KEYCODE_APOSTROPHE.keyCodeShifted()
        '{'.code -> KEYCODE_LEFT_BRACKET.keyCodeShifted()
        '}'.code -> KEYCODE_RIGHT_BRACKET.keyCodeShifted()
        ':'.code -> KEYCODE_SEMICOLON.keyCodeShifted()
        '|'.code -> KEYCODE_BACKSLASH.keyCodeShifted()
        '<'.code -> KEYCODE_COMMA.keyCodeShifted()
        '>'.code -> KEYCODE_PERIOD.keyCodeShifted()
        '?'.code -> KEYCODE_SLASH.keyCodeShifted()
        '~'.code -> KEYCODE_GRAVE.keyCodeShifted()
        '_'.code -> KEYCODE_MINUS.keyCodeShifted()
        else -> null
    }

    private fun Int.keyCodeShifted(): Pair<Int, Int> = this to META_SHIFT_LEFT_ON
    private fun Int.keyCode(): Pair<Int, Int> = this to 0

    internal fun Throwable.internalError() = Status.INTERNAL.withDescription(message).asException()

    enum class FileType(val ext: String, val mimeType: String) {
        JPG("jpg", "image/jpg"),
        JPEG("jpeg", "image/jpeg"),
        PNG("png", "image/png"),
        GIF("gif", "image/gif"),
        MP4("mp4", "video/mp4"),
    }
}
