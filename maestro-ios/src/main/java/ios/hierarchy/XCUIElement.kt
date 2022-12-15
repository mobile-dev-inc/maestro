package ios.hierarchy

import com.fasterxml.jackson.annotation.JsonProperty

data class XCUIElement(
    @JsonProperty("label") val label: String,
    @JsonProperty("elementType") val elementType: Int,
    @JsonProperty("identifier") val identifier: String,
    @JsonProperty("frame") val frame: Frame,
    @JsonProperty("enabled") val enabled: Boolean,
    @JsonProperty("horizontalSizeClass") val horizontalSizeClass: Int,
    @JsonProperty("title") val title: String,
    @JsonProperty("windowContextID") val windowContextID: Long,
    @JsonProperty("verticalSizeClass") val verticalSizeClass: Int,
    @JsonProperty("selected") val selected: Boolean,
    @JsonProperty("displayID") val displayID: Int,
    @JsonProperty("children") val children: ArrayList<XCUIElement>?,
    @JsonProperty("hasFocus") val hasFocus: Boolean,
    @JsonProperty("placeholderValue") val placeholderValue: String?,
    @JsonProperty("value") val value: String?
)