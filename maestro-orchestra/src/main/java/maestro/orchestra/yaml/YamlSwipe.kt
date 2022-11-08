package maestro.orchestra.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import maestro.SwipeDirection

@JsonDeserialize(using = YamlSwipeDeserializer::class)
interface YamlSwipe {
    val duration: Long
}

data class YamlSwipeDirection(val direction: SwipeDirection, override val duration: Long = DEFAULT_DURATION_IN_MILLIS) : YamlSwipe
data class YamlCoordinateSwipe(val start: String, val end: String, override val duration: Long = DEFAULT_DURATION_IN_MILLIS) : YamlSwipe

private const val DEFAULT_DURATION_IN_MILLIS = 2000L

class YamlSwipeDeserializer: JsonDeserializer<YamlSwipe>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): YamlSwipe {
        val mapper = parser.codec as ObjectMapper
        val root: TreeNode = mapper.readTree(parser)
        val isDirectionalSwipe = root.fieldNames().asSequence().toList() == listOf("direction", "duration") || root.fieldNames().asSequence().toList() == listOf("direction")
        val isCoordinateSwipe = root.fieldNames().asSequence().toList() == listOf("start", "end", "duration") || root.fieldNames().asSequence().toList() == listOf("start", "end")
        return when {
            isDirectionalSwipe ->  {
                val duration = getDuration(root)
                val direction = SwipeDirection.valueOf(root.get("direction").toString().replace("\"", ""))
                YamlSwipeDirection(direction, duration)
            }
            isCoordinateSwipe -> {
                YamlCoordinateSwipe(
                    root.path("start").toString().replace("\"", ""),
                    root.path("end").toString().replace("\"", ""),
                    getDuration(root)
                )
            }
            else -> throw IllegalStateException(
                "Provide swipe direction UP, DOWN, RIGHT OR LEFT or by giving explicit " +
                    "start and end coordinates."
            )
        }
     }

    private fun getDuration(root: TreeNode): Long {
        return if (root.path("duration").toString().isEmpty()) {
            DEFAULT_DURATION_IN_MILLIS
        } else {
            root.path("duration").toString().replace("\"", "").toLong()
        }
    }

}