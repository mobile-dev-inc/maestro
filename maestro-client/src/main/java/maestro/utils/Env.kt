package maestro.utils

object Env {
    fun getSystemEnv(name: String): String? = System.getenv(name)
    fun getSystemEnv(): Map<String, String?> = System.getenv()
}
