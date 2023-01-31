/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package maestro

data class UiElement(
    val treeNode: TreeNode,
    val bounds: Bounds,
) {

    fun distanceTo(other: UiElement): Float {
        return bounds.center().distance(other.bounds.center())
    }

    fun getVisiblePercentage(screenWidth: Int, screenHeight: Int): Double {
        val visibleX = maxOf(0, minOf(bounds.x + bounds.width, screenWidth) - maxOf(bounds.x, 0))
        val visibleY = maxOf(0, minOf(bounds.y + bounds.height, screenHeight) - maxOf(bounds.y, 0))
        val visibleArea = visibleX * visibleY
        val totalArea = bounds.width * bounds.height
        return visibleArea.toDouble() / totalArea.toDouble()
    }

    fun isWithinViewPortBounds(info: DeviceInfo, paddingHorizontal: Float = 0f, paddingVertical: Float = 0f): Boolean {
        val paddingX = (info.widthGrid * paddingHorizontal).toInt()
        val paddingY = (info.heightGrid * paddingVertical).toInt()
        val xEnd = info.widthGrid - paddingX
        val yEnd = info.heightGrid - paddingY

        val isXWithinBounds = bounds.x in paddingX..xEnd
        val isYWithinBounds = bounds.y in paddingY..yEnd

        return isXWithinBounds && isYWithinBounds
    }

    companion object {

        fun TreeNode.toUiElement(): UiElement {
            return toUiElementOrNull()
                ?: throw IllegalStateException("Node has no bounds")
        }

        fun TreeNode.toUiElementOrNull(): UiElement? {
            // TODO needs different impl for iOS
            val boundsStr = attributes["bounds"]
                ?: return null

            val boundsArr = boundsStr
                .replace("][", ",")
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { it.toInt() }

            return UiElement(
                this,
                Bounds(
                    x = boundsArr[0],
                    y = boundsArr[1],
                    width = boundsArr[2] - boundsArr[0],
                    height = boundsArr[3] - boundsArr[1]
                ),
            )
        }
    }
}
