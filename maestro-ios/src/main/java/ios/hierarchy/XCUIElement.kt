package ios.hierarchy

import com.google.gson.annotations.SerializedName

data class XCUIElement(
    @SerializedName("label") val label: String,
    @SerializedName("elementType") val elementType: Int,
    @SerializedName("identifier") val identifier: String,
    @SerializedName("frame") val frame: Frame,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("horizontalSizeClass") val horizontalSizeClass: Int,
    @SerializedName("title") val title: String,
    @SerializedName("windowContextID") val windowContextID: Long,
    @SerializedName("verticalSizeClass") val verticalSizeClass: Int,
    @SerializedName("selected") val selected: Boolean,
    @SerializedName("displayID") val displayID: Int,
    @SerializedName("children") val children: ArrayList<XCUIElement>?,
    @SerializedName("hasFocus") val hasFocus: Boolean
)