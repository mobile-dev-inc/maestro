package maestro

enum class Platform {
    ANDROID, IOS, WEB;

    companion object {
        fun fromString(p: String): Platform? {
            return values().find { it.name.lowercase() == p.lowercase() }
        }
    }
}
