package maestro.cli.insights

import maestro.cli.view.box

/**
 * The Insights helper for Maestro CLI.
 *  - Uses MAESTRO_CLI_INSIGHTS_NOTIFICATION_DISABLED env var to disable insights notifications
 */
object Insights {

    private const val DISABLE_INSIGHTS_ENV_VAR = "MAESTRO_CLI_INSIGHTS_NOTIFICATION_DISABLED"
    private val disabled: Boolean
        get() = System.getenv(DISABLE_INSIGHTS_ENV_VAR) == "true"

    fun maybeNotifyInsights() {
        if (disabled) return

        println()
        println(
            listOf(
                "Tryout our new Analyze with Ai feature.\n",
                "See what's new:",
                // TODO: Add final link to analyze with Ai Docs
                "https://github.com/mobile-dev-inc/maestro/blob/main/CHANGELOG.md#blaaa",
                "Analyze command:",
                "maestro analyze android-flow.yaml | bash\n",
                "To disable this notification, set $DISABLE_INSIGHTS_ENV_VAR environment variable to \"true\" before running Maestro."
            ).joinToString("\n").box()
        )
    }
}
