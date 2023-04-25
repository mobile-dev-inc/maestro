import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import util.IOSLaunchArguments.toIOSLaunchArguments

class IOSLaunchArgumentsTest {

    @Test
    fun `boolean params with one key are not touched`() {
        // given
        val launchArguments = listOf("isCartScreen")

        // when
        val iOSLaunchArguments = launchArguments.toIOSLaunchArguments()

        // then
        assertThat(iOSLaunchArguments).containsExactly("isCartScreen")
    }

    @Test
    fun `key-value pair without prefixed '-' sign are transformed`() {
        // given
        val launchArguments = listOf("isCartScreen", "cartValue=3")

        // when
        val iOSLaunchArguments = launchArguments.toIOSLaunchArguments()

        // then
        assertThat(iOSLaunchArguments).containsExactly("isCartScreen", "-cartValue", "3")
    }

    @Test
    fun `key-value pair with prefixed '-' sign are not changed`() {
        // given
        val launchArguments = listOf("isCartScreen", "cartValue=4", "-cartColor=Orange")

        // when
        val iOSLaunchArguments = launchArguments.toIOSLaunchArguments()

        // then
        assertThat(iOSLaunchArguments).containsExactly("isCartScreen", "-cartValue", "4", "-cartColor", "Orange")
    }
}