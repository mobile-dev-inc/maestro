package maestro.utils.maestro.insight

data class Insight(
    val level: Level,
    val message: String,
) {

    enum class Level {
        WARNING
    }

}
