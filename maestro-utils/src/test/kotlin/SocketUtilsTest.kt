import maestro.utils.SocketUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket

class SocketUtilsTest {

    @Test
    fun `nextFreePort should return a free port within the specified range`() {
        val from = 5000
        val to = 5100
        val port = SocketUtils.nextFreePort(from, to)

        assertTrue(port in from..to)
    }

    @Test
    fun `nextFreePort should throw IllegalStateException if no ports are available`() {
        val from = 5000
        val to = 5001

        val serverSocket1 = ServerSocket(from)
        val serverSocket2 = ServerSocket(to)

        try {
            assertThrows(IllegalArgumentException::class.java) {
                SocketUtils.nextFreePort(from, to)
            }
        } finally {
            serverSocket1.close()
            serverSocket2.close()
        }
    }

    @Test
    fun `localIp should return a non-loopback IPv4 address`() {
        val ip = SocketUtils.localIp()

        assertNotNull(ip)
        assertTrue(ip.startsWith("192") || ip.startsWith("10") || ip.startsWith("172"))

        val inetAddress = InetAddress.getByName(ip)

        assertTrue(inetAddress is Inet4Address)
        assertFalse(inetAddress.isLoopbackAddress)
    }

    @Test
    fun `localIp should return localhost address if no network interfaces are available`() {
        val originalNetworkInterfaces = NetworkInterface::class.java.getDeclaredMethod("getNetworkInterfaces")
        originalNetworkInterfaces.isAccessible = true

        try {
            NetworkInterface::class.java.getDeclaredMethod("getNetworkInterfaces").apply {
                isAccessible = true
            }

            val ip = SocketUtils.localIp()

            assertNotNull(ip)
            assertEquals(InetAddress.getLocalHost().hostAddress, ip)
        } finally {
            NetworkInterface::class.java.getDeclaredMethod("getNetworkInterfaces").apply {
                isAccessible = true
            }
        }
    }
}