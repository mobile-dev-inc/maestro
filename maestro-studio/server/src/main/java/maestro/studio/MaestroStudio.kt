package maestro.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.files
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticRootFolder
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import maestro.Filters
import maestro.Maestro
import maestro.Platform
import maestro.TreeNode
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

data class DeviceScreen(
    val screenshot: String,
    val width: Int,
    val height: Int,
    val elements: List<UIElement>,
)

data class UIElementBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class UIElement(
    val id: UUID, // Autogenerated uuid to make this easier to work with on the frontend
    val bounds: UIElementBounds?,
    val resourceId: String?,
    val resourceIdIndex: Int?,
    val text: String?,
    val textIndex: Int?
)

object MaestroStudio {

    private val screenshotDir = getScreenshotDir()

    fun start(port: Int, maestro: Maestro) {
        embeddedServer(Netty, port = port) {
            routing {
                get("/api/device-screen") {
                    val tree: TreeNode
                    val screenshotFile: File
                    synchronized(this@MaestroStudio) {
                        tree = maestro.viewHierarchy().root
                        screenshotFile = takeScreenshot(maestro)
                    }

                    val deviceInfo = maestro.deviceInfo()

                    val deviceWidth: Int
                    val deviceHeight: Int
                    if (deviceInfo.platform == Platform.ANDROID) {
                        // Using screenshot dimensions instead of Maestro.deviceInfo() since
                        // deviceInfo() is currently inaccurate on Android
                        val image = ImageIO.read(screenshotFile)
                        if (image == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve screenshot")
                            return@get
                        }

                        deviceWidth = image.width
                        deviceHeight = image.height
                    } else {
                        deviceWidth = deviceInfo.widthGrid
                        deviceHeight = deviceInfo.heightGrid
                    }

                    val elements = treeToElements(tree)
                    val deviceScreen = DeviceScreen("/screenshot/${screenshotFile.name}", deviceWidth, deviceHeight, elements)
                    val response = jacksonObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(deviceScreen)
                    call.respondText(response)
                }
                static("/screenshot") {
                    staticRootFolder = screenshotDir.toFile()
                    files(".")
                }
                singlePageApplication {
                    useResources = true
                    filesPath = "web"
                    defaultPage = "index.html"
                }
            }
        }.start()
    }

    private fun treeToElements(tree: TreeNode): List<UIElement> {
        fun gatherElements(tree: TreeNode, list: MutableList<TreeNode>): List<TreeNode> {
            tree.children.forEach { child ->
                gatherElements(child, list)
            }
            list.add(tree)
            return list
        }

        val elements = gatherElements(tree, mutableListOf())
            .sortedWith(Filters.INDEX_COMPARATOR)

        return elements.map { element ->
            val id = UUID.randomUUID()
            val bounds = element.bounds()
            val text = element.attributes["text"]
            val resourceId = element.attributes["resource-id"]
            val textIndex = if (text == null) null else elements.filter { text == it.attributes["text"] }.indexOf(element)
            val resourceIndex = if (resourceId == null) null else elements.filter { resourceId == it.attributes["resource-id"] }.indexOf(element)
            UIElement(id, bounds, resourceId, textIndex, text, resourceIndex)
        }
    }

    private fun TreeNode.bounds(): UIElementBounds? {
        val boundsString = attributes["bounds"] ?: return null
        val pattern = Pattern.compile("\\[([0-9]+),([0-9]+)]\\[([0-9]+),([0-9]+)]")
        val m = pattern.matcher(boundsString)
        if (!m.matches()) {
            System.err.println("Warning: Bounds text does not match expected pattern: $boundsString")
            return null
        }

        val l = m.group(1).toIntOrNull() ?: return null
        val t = m.group(2).toIntOrNull() ?: return null
        val r = m.group(3).toIntOrNull() ?: return null
        val b = m.group(4).toIntOrNull() ?: return null

        return UIElementBounds(
            x = l,
            y = t,
            width = r - l,
            height = b - t,
        )
    }

    private fun takeScreenshot(maestro: Maestro): File {
        val name = "${UUID.randomUUID()}.png"
        val screenshotFile = screenshotDir.resolve(name).toFile()
        screenshotFile.deleteOnExit()
        maestro.takeScreenshot(screenshotFile)
        return screenshotFile
    }

    private fun getScreenshotDir(): Path {
        val home = System.getProperty("user.home")
        val parent = if (home.isNullOrBlank()) createTempDirectory() else Path(home)
        val screenshotDir = parent.resolve(".maestro/studio/screenshots")
        screenshotDir.createDirectories()
        return screenshotDir
    }
}
