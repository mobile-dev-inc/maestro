package maestro.orchestra

import maestro.DeviceInfo
import maestro.ElementFilter
import maestro.Filters
import maestro.Filters.asFilter
import maestro.Maestro
import maestro.MaestroException
import maestro.TreeNode
import maestro.UiElement
import maestro.orchestra.filter.FilterWithDescription
import maestro.orchestra.filter.TraitFilters

class OrchestraContext(
    maestro: Maestro,
    private val optionalLookupTimeoutMs: Long,
    override val lookupTimeoutMs: Long,
) : Context(maestro) {
    override fun findElement(
        selector: ElementSelector,
        timeoutMs: Long?
    ): UiElement {
        val timeout = timeoutMs
            ?: if (selector.optional) {
                optionalLookupTimeoutMs
            } else {
                lookupTimeoutMs
            }

        val (description, filterFunc) = buildFilter(
            selector,
            maestro.deviceInfo(),
            maestro.viewHierarchy().aggregate(),
        )

        return maestro.findElementWithTimeout(
            timeoutMs = timeout,
            filter = filterFunc,
        ) ?: throw MaestroException.ElementNotFound(
            "Element not found: $description",
            maestro.viewHierarchy().root,
        )
    }

    private fun buildFilter(
        selector: ElementSelector,
        deviceInfo: DeviceInfo,
        allNodes: List<TreeNode>,
    ): FilterWithDescription {
        val filters = mutableListOf<ElementFilter>()
        val descriptions = mutableListOf<String>()

        selector.textRegex
            ?.let {
                descriptions += "Text matching regex: $it"
                filters += Filters.textMatches(it.toRegex(Orchestra.REGEX_OPTIONS)).asFilter()
            }

        selector.idRegex
            ?.let {
                descriptions += "Id matching regex: $it"
                filters += Filters.idMatches(it.toRegex(Orchestra.REGEX_OPTIONS)).asFilter()
            }

        selector.size
            ?.let {
                descriptions += "Size: $it"
                filters += Filters.sizeMatches(
                    width = it.width,
                    height = it.height,
                    tolerance = it.tolerance,
                ).asFilter()
            }

        selector.below
            ?.let {
                descriptions += "Below: ${it.description()}"
                filters += Filters.below(buildFilter(it, deviceInfo, allNodes).filterFunc)
            }

        selector.above
            ?.let {
                descriptions += "Above: ${it.description()}"
                filters += Filters.above(buildFilter(it, deviceInfo, allNodes).filterFunc)
            }

        selector.leftOf
            ?.let {
                descriptions += "Left of: ${it.description()}"
                filters += Filters.leftOf(buildFilter(it, deviceInfo, allNodes).filterFunc)
            }

        selector.rightOf
            ?.let {
                descriptions += "Right of: ${it.description()}"
                filters += Filters.rightOf(buildFilter(it, deviceInfo, allNodes).filterFunc)
            }

        selector.containsChild
            ?.let {
                descriptions += "Contains child: ${it.description()}"
                filters += Filters.containsChild(findElement(it)).asFilter()
            }

        selector.traits
            ?.map {
                TraitFilters.buildFilter(it)
            }
            ?.forEach { (description, filter) ->
                descriptions += description
                filters += filter
            }

        val finalFilter = selector.index
            ?.let {
                Filters.compose(
                    listOf(
                        Filters.intersect(filters),
                        Filters.index(it),
                    )
                )
            } ?: Filters.intersect(filters)

        return FilterWithDescription(
            descriptions.joinToString(", "),
            finalFilter,
        )
    }
}
