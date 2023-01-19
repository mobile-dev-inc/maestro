package maestro.orchestra.yaml

import com.fasterxml.jackson.annotation.JsonFormat
import maestro.ScrollDirection

data class YamlScrollUntilVisible(
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
    val direction: ScrollDirection ?= null,
    val element: YamlElementSelectorUnion,
    val timeout: Long? = null,
)