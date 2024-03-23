package maestro.utils

import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * "Proxy" which just closes a connection when received
 */
class StubProxy(private val serverPort: UInt) {

    private var serverThread: Thread? = null
    private var actualPort: UInt? = null

    val port: UInt?
        get() = actualPort

    val isRunning: Boolean
        get() = serverThread != null

    fun start() {
        if (serverThread != null) {
            return
        }

        this.serverThread = thread(start = true, name = "StubProxy thread") {
            val socket = ServerSocket(serverPort.toInt())
            actualPort = socket.localPort.toUInt()
            socket.use {
                try {
                    while (true) {
                        val client = socket.accept()
                        client.close()
                        Thread.sleep(100)
                    }
                } catch (ignored: InterruptedException) { }
            }
        }
    }

    fun stop() {
        this.serverThread?.interrupt()
    }

}