package maestro.utils

import com.github.romankh3.image.comparison.ImageComparison
import maestro.Driver
import maestro.Point
import maestro.ViewHierarchy
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class TapUtils {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TapUtils::class.java)
        private const val SCREENSHOT_DIFF_THRESHOLD = 0.005 // 0.5%
        fun performTap(
            driver: Driver,
            x: Int,
            y: Int,
            retryIfNoChange: Boolean = true,
            longPress: Boolean = false,
            initialHierarchy: ViewHierarchy? = null,
        ) {
            LOGGER.info("Tapping at ($x, $y)")

            val hierarchyBeforeTap = initialHierarchy ?: viewHierarchy(driver)
            val screenshotBeforeTap: BufferedImage? = ScreenshotUtils.tryTakingScreenshot(driver)

            val retries = getNumberOfRetries(retryIfNoChange)
            repeat(retries) {
                if (longPress) {
                    driver.longPress(Point(x, y))
                } else {
                    driver.tap(Point(x, y))
                }
                val hierarchyAfterTap = driver.waitForAppToSettle(initialHierarchy)

                if (hierarchyBeforeTap != hierarchyAfterTap) {
                    LOGGER.info("Something have changed in the UI judging by view hierarchy. Proceed.")
                    return
                }

                val screenshotAfterTap: BufferedImage? = ScreenshotUtils.tryTakingScreenshot(driver)
                if (screenshotBeforeTap != null &&
                    screenshotAfterTap != null &&
                    screenshotBeforeTap.width == screenshotAfterTap.width &&
                    screenshotBeforeTap.height == screenshotAfterTap.height
                ) {
                    val imageDiff = ImageComparison(
                        screenshotBeforeTap,
                        screenshotAfterTap
                    ).compareImages().differencePercent

                    if (imageDiff > SCREENSHOT_DIFF_THRESHOLD) {
                        LOGGER.info("Something have changed in the UI judging by screenshot (d=$imageDiff). Proceed.")
                        return
                    } else {
                        LOGGER.info("Screenshots are not different enough (d=$imageDiff)")
                    }
                } else {
                    LOGGER.info("Skipping screenshot comparison")
                }

                LOGGER.info("Nothing changed in the UI.")
            }
        }

        fun getNumberOfRetries(retryIfNoChange: Boolean): Int {
            return if (retryIfNoChange) 2 else 1
        }
        fun viewHierarchy(driver: Driver): ViewHierarchy {
            return ViewHierarchy.from(driver)
        }
    }
}