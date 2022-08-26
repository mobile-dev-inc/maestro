package maestro.orchestra.predicate

import maestro.Filters
import maestro.Filters.asFilter
import maestro.orchestra.ElementTrait

object TraitFilters {

    fun buildFilter(
        trait: ElementTrait,
    ): FilterWithDescription {
        return when (trait) {
            ElementTrait.TEXT -> FilterWithDescription(
                "Has text",
                Filters.hasText().asFilter()
            )
            ElementTrait.SQUARE -> FilterWithDescription(
                "Is square",
                Filters.isSquare().asFilter()
            )
            ElementTrait.LONG_TEXT -> FilterWithDescription(
                "Has long text",
                Filters.hasLongText().asFilter()
            )
        }
    }

}