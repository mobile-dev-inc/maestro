package xcuitest

interface XcUITestDriver {
    fun listApps(): Set<String>
    fun uninstall()
    fun setup()
    fun cleanup()
}