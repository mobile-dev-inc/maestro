package conductor.utils

import conductor.TreeNode
import conductor.UiElement.Companion.toUiElement

object ViewUtils {

    fun isVisible(root: TreeNode, node: TreeNode): Boolean {
        if (!node.attributes.containsKey("bounds")) {
            return false
        }

        val center = node.toUiElement().bounds.center()

        val elementAtPosition = getElementAt(root, center.x, center.y)

        return node == elementAtPosition
    }

    private fun getElementAt(
        node: TreeNode,
        x: Int,
        y: Int
    ): TreeNode? {
        return node
            .children
            .asReversed()
            .asSequence()
            .mapNotNull {
                val elementWithinChild = if (it.children.isNotEmpty()) {
                    getElementAt(it, x, y)
                } else {
                    null
                }

                elementWithinChild
                    ?: if (it.attributes.containsKey("bounds")) {
                        val bounds = it.toUiElement().bounds

                        if (bounds.contains(x, y)) {
                            it
                        } else {
                            null
                        }
                    } else {
                        null
                    }
            }
            .firstOrNull()
    }

}