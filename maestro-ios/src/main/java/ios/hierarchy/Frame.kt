package ios.hierarchy

import com.google.gson.annotations.SerializedName

data class Frame(
    @SerializedName("Width") val width: Float,
    @SerializedName("Height") val height: Float,
    @SerializedName("Y") val y: Float,
    @SerializedName("X") val x: Float
)