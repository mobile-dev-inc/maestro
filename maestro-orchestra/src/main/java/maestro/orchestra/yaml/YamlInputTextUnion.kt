package maestro.orchestra.yaml

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.POJONode
import com.fasterxml.jackson.databind.node.ValueNode

@JsonDeserialize(using = YamlInputTextDeserializer::class)
interface YamlInputTextUnion

data class YamlInputTextV1(val text: String) : YamlInputTextUnion
@JsonDeserialize(`as` = YamlInputTextV2::class)
data class YamlInputTextV2(
    val text: String,
    val accessibilityText: String? = null,
    val id: String? = null,
    val point: String? = null,
    val pasteTitle: String? = null,
) : YamlInputTextUnion

class YamlInputTextDeserializer : JsonDeserializer<YamlInputTextUnion>() {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): YamlInputTextUnion {
        val mapper = parser.codec as ObjectMapper
        val root: TreeNode = mapper.readTree(parser)

        return if (root is ValueNode && root !is POJONode) {
            YamlInputTextV1(root.asText())
        } else {
            mapper.convertValue(root, YamlInputTextV2::class.java)
        }
    }
}