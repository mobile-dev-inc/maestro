package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import maestro.SwipeDirection
import maestro.directionValueOfOrNull

@JsonDeserialize(using = YamlSwipeDeserializer::class)
interface YamlSwipe {
    val duration: Long
    val label: String?
    val optional: Boolean
}

data class YamlSwipeDirection(
    val direction: SwipeDirection,
    override val duration: Long = DEFAULT_DURATION_IN_MILLIS,
    override val label: String? = null,
    override val optional: Boolean,
) : YamlSwipe

data class YamlCoordinateSwipe(
    val start: String,
    val end: String,
    override val duration: Long = DEFAULT_DURATION_IN_MILLIS,
    override val label: String? = null,
    override val optional: Boolean,
) : YamlSwipe

data class YamlRelativeCoordinateSwipe(
    val start: String,
    val end: String,
    override val duration: Long = DEFAULT_DURATION_IN_MILLIS,
    override val label: String? = null,
    override val optional: Boolean,
) : YamlSwipe

@JsonDeserialize(`as` = YamlSwipeElement::class)
data class YamlSwipeElement(
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
    val direction: SwipeDirection,
    val from: YamlElementSelectorUnion,
    override val duration: Long = DEFAULT_DURATION_IN_MILLIS,
    override val label: String? = null,
    override val optional: Boolean,
) : YamlSwipe

data class YamlRelativeCoordinateSwipeElement(
    val from: YamlElementSelectorUnion,
    val to: String,
    override val duration: Long = DEFAULT_DURATION_IN_MILLIS,
    override val label: String? = null,
    override val optional: Boolean,
) : YamlSwipe

private const val DEFAULT_DURATION_IN_MILLIS = 400L

class YamlSwipeDeserializer : JsonDeserializer<YamlSwipe>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): YamlSwipe {
        val mapper = (parser.codec as ObjectMapper)
        val root: TreeNode = mapper.readTree(parser)
        val input = root.fieldNames().asSequence().toList()
        val duration = getDuration(root)
        val label = getLabel(root)
        val optional = getOptional(root)
        when {
            input.contains("start") && input.contains("end") -> {
                check(root.get("direction") == null) { "You cannot provide direction with start/end swipe." }
                check(root.get("start") != null && root.get("end") != null) {
                    "You need to provide both start and end coordinates, to swipe with coordinates"
                }
                return resolveCoordinateSwipe(root, duration, label, optional)
            }
            input.contains("direction") -> {
                check(root.get("start") == null && root.get("end") == null) {
                    "You cannot provide start/end coordinates with directional swipe"
                }
                val direction = root.get("direction").toString().replace("\"", "")
                check(directionValueOfOrNull<SwipeDirection>(direction) != null) {
                    "Invalid direction provided to directional swipe: $direction. Direction can be either:\n" +
                        "1. RIGHT or right\n" +
                        "2. LEFT or left\n" +
                        "3. UP or up\n" +
                        "4. DOWN or down"
                }
                val isDirectionalSwipe = isDirectionalSwipe(input)
                return if (isDirectionalSwipe) {
                    YamlSwipeDirection(SwipeDirection.valueOf(direction.uppercase()), duration, label, optional)
                } else {
                    mapper.convertValue(root, YamlSwipeElement::class.java)
                }
            }
            input.contains("from") && input.contains("to") -> {
                return resolveRelativeCoordinateSwipeElement(root, duration, label, optional, mapper)
            }
            else -> {
                throw IllegalArgumentException(
                    "Swipe command takes either: \n" +
                            "\t1. direction: Direction based swipe with: \"RIGHT\", \"LEFT\", \"UP\", or \"DOWN\" or \n" +
                            "\t2. start and end: Coordinates based swipe with: \"start\" and \"end\" coordinates \n" +
                            "\t3. direction and element to swipe directionally on element\n" +
                            "\t4. from and direction/to: more precise swipe from one point to another\n" +

                            "It seems you provided invalid input with: $input"
                )
            }
        }
    }

    private fun resolveRelativeCoordinateSwipeElement(
        root: TreeNode,
        duration: Long,
        label: String?,
        optional: Boolean,
        mapper: ObjectMapper
    ): YamlRelativeCoordinateSwipeElement {
        val from = mapper.convertValue(root.path("from"), YamlElementSelectorUnion::class.java)
        val to = root.path("to").toString().replace("\"", "")

        val isRelative = to.contains("%")

        if (isRelative) {
            val endPoints = to
                .replace("%", "")
                .split(",")
                .map { it.trim().toInt() }
            check(endPoints[0] in 0..100 && endPoints[1] in 0..100) {
                "Invalid end point: $to should be between 0 to 100 when using relative coordinates."
            }
        } else {
            val endPoints = to
                .split(",")
                .map { it.trim().toInt() }
            check(endPoints.size == 2) { "Invalid format for absolute coordinates: $to" }
        }

        return YamlRelativeCoordinateSwipeElement(from, to, duration, label, optional)
    }

    private fun resolveCoordinateSwipe(root: TreeNode, duration: Long, label: String?, optional: Boolean): YamlSwipe {
        when {
            isRelativeSwipe(root) -> {
                val start = root.path("start").toString().replace("\"", "")
                val end = root.path("end").toString().replace("\"", "")
                check(start.contains("%") && end.contains("%")) {
                    "You need to provide start and end coordinates with %, Found: (${start}, ${end})"
                }
                val startPoints = start
                    .replace("%", "")
                    .split(",")
                    .map { it.trim().toInt() }
                val endPoints = end
                    .replace("%", "")
                    .split(",")
                    .map { it.trim().toInt() }
                check(startPoints[0] in 0..100 && startPoints[1] in 0..100) {
                    "Invalid start point: $start should be between 0 to 100"
                }
                check(endPoints[0] in 0..100 && endPoints[1] in 0..100) {
                    "Invalid start point: $end should be between 0 to 100"
                }

                return YamlRelativeCoordinateSwipe(
                    start,
                    end,
                    duration,
                    label,
                    optional,
                )
            }
            else -> return YamlCoordinateSwipe(
                root.path("start").toString().replace("\"", ""),
                root.path("end").toString().replace("\"", ""),
                duration,
                label,
                optional,
            )
        }
    }

    private fun isRelativeSwipe(root: TreeNode): Boolean {
        return root.get("start").toString().contains("%") || root.get("end").toString().contains("%")
    }

    private fun getDuration(root: TreeNode): Long {
        return if (root.path("duration").isMissingNode) {
            DEFAULT_DURATION_IN_MILLIS
        } else {
            root.path("duration").toString().replace("\"", "").toLong()
        }
    }

    private fun getLabel(root: TreeNode): String? {
        return if (root.path("label").isMissingNode) {
            null
        } else {
            root.path("label").toString().replace("\"", "")
        }
    }

    private fun getOptional(root: TreeNode): Boolean {
        return if (root.path("optional").isMissingNode) {
            false
        } else {
            root.path("optional").toString().replace("\"", "").toBoolean()
        }
    }

    private fun isDirectionalSwipe(input: List<String>): Boolean {
        return input == listOf("direction", "duration") || input == listOf("direction") ||
                input == listOf("direction", "label") || input == listOf("direction", "duration", "label")
    }
}
