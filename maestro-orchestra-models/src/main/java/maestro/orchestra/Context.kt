package maestro.orchestra

import maestro.Maestro
import maestro.UiElement

abstract class Context(val maestro: Maestro) {
    abstract fun findElement(
        selector: ElementSelector,
        timeoutMs: Long? = null
    ): UiElement
}
