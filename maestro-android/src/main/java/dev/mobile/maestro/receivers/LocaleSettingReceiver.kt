package dev.mobile.maestro.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.mobile.maestro.handlers.LocaleSettingHandler
import org.apache.commons.lang3.LocaleUtils
import java.util.*

class LocaleSettingReceiver : BroadcastReceiver(), HasAction {
    override fun onReceive(context: Context?, intent: Intent?) {
        var language = intent?.getStringExtra(LANG)
        var country = intent?.getStringExtra(COUNTRY)
        val skipLocaleCheck = intent?.getStringExtra(SKIP_LOCALE_CHECK)

        if (language == null || country == null) {
            Log.w(TAG, "It is required to provide both language and country, for example: " +
                    "am broadcast -a dev.mobile.maestro --es lang ja --es country JP")
            Log.i(TAG, "Set en-US by default.")
            language = "en"
            country = "US"
        }

        var locale = Locale(language, country)

        Log.i(TAG, "Obtained locale: $locale")

        val script = intent?.getStringExtra(SCRIPT)
        if (script != null) {
            Locale.Builder().setLocale(locale).setScript(script).build().also { locale = it }
        }

        if (skipLocaleCheck != null) {
            Log.i(TAG, "'skip_locale_check' value is provided, will not check locale availability")
        } else if (!LocaleUtils.isAvailableLocale(locale)) {
            val approximateMatchesLc = matchLocales(language, country)

            if (approximateMatchesLc.isNotEmpty() && script.isNullOrBlank()) {
                Log.i(
                    TAG,
                    "The locale $locale is not known. Selecting the closest known one ${approximateMatchesLc[0]} instead"
                )
                locale = approximateMatchesLc[0]
            } else {
                val approximateMatchesL = matchLocales(language)
                if (approximateMatchesL.isEmpty()) {
                    Log.e(
                        TAG,
                        "The locale $locale is not known. Only the following locales are available: ${LocaleUtils.availableLocaleList()}"
                    )
                } else {
                    Log.e(
                        TAG,
                        "The locale $locale is not known. " +
                                "The following locales are available for the $language language: $approximateMatchesL" +
                                "The following locales are available altogether: ${LocaleUtils.availableLocaleList()}"
                    )
                }
                resultCode = Activity.RESULT_CANCELED
                resultData = "The locale $locale is not known"
                return
            }
        }

        context?.let {
            LocaleSettingHandler(it).setLocale(locale)
            Log.i(TAG, "Set locale: $locale")
            resultCode = Activity.RESULT_OK
            resultData = locale.toString()
        } ?: Log.e(TAG, "Failed to set device locale setting (context is null)")
    }

    private fun matchLocales(language: String): List<Locale> {
        val matches = ArrayList<Locale>()
        for (locale in LocaleUtils.availableLocaleList()) {
            if (locale.language.equals(language, ignoreCase = true)) {
                matches.add(locale)
            }
        }
        return matches
    }

    private fun matchLocales(language: String, country: String): List<Locale> {
        val matches = ArrayList<Locale>()
        for (locale in LocaleUtils.availableLocaleList()) {
            if (locale.language.equals(language, ignoreCase = true) &&
                locale.country.equals(country, ignoreCase = true)
            ) {
                matches.add(locale)
            }
        }
        return matches
    }

    override fun action(): String {
        return ACTION
    }

    companion object {
        private const val LANG = "lang"
        private const val COUNTRY = "country"
        private const val SKIP_LOCALE_CHECK = "skip_locale_check"
        private const val SCRIPT = "script"
        private const val ACTION = "dev.mobile.maestro.locale"
        private const val TAG = "Maestro"
    }
}