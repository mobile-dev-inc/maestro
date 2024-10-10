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

class SocketUtilsTest {

    @Test
    fun `nextFreePort should return a free port within the specified range`() {
        val from = 5000
        val to = 5100
        val port = SocketUtils.nextFreePort(from, to)

        assertTrue(port in from..to)
    }

    @Test
    fun `nextFreePort should throw IllegalStateException when no ports are available in the range`() {
        val from = 100000
        val to = 100010

        assertThrows(IllegalStateException::class.java) {
            SocketUtils.nextFreePort(from, to)
        }
    }

    @Test
    fun `localIp should return a non-loopback IPv4 address`() {
        val ip = SocketUtils.localIp()

        assertNotNull(ip)
        assertTrue(ip.startsWith("192") || ip.startsWith("10") || ip.startsWith("172") || ip.startsWith("127"))

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