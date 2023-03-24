package hierarchy

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(`as` = ElementNode::class)
data class ElementNode(
    val label: String,
    val elementType: Int,
    val identifier: String,
    val horizontalSizeClass: Int,
    val windowContextID: Long,
    val verticalSizeClass: Int,
    val selected: Boolean,
    val displayID: Int,
    val hasFocus: Boolean,
    val placeholderValue: String?,
    val value: String?,
    val frame: Frame,
    val enabled: Boolean,
    val title: String?,
    val children: ArrayList<ElementNode>?,
)
