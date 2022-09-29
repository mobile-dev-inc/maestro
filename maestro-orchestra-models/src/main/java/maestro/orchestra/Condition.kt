package maestro.orchestra

data class Condition(
    val visible: ElementSelector? = null,
) {

    fun description(): String {
        return if (visible != null) {
            return "${visible.description()} is visible"
        } else {
            "true"
        }
    }

}
