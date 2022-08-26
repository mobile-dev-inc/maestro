package maestro.orchestra.predicate

import maestro.ElementFilter

data class FilterWithDescription(
    val description: String,
    val filterFunc: ElementFilter,
)
