package com.gptimage.playground.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.gptimage.playground.data.model.AppLanguage
import java.util.Locale

/**
 * Synchronous language cache backed by the default SharedPreferences.
 *
 * The source of truth for config lives in DataStore (async), but applying a
 * locale requires a value synchronously in [android.app.Activity.attachBaseContext].
 * This cache mirrors the chosen language so the Activity can apply it before
 * Compose or DataStore are ready. Mirrors the web app's `detectRuntimeAppLanguage`.
 */
object LocaleHelper {
    private const val PREFS = "gpt_image_playground_locale"
    private const val KEY_LANGUAGE = "app_language"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLanguageCode(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, AppLanguage.ZhCN.code) ?: AppLanguage.ZhCN.code

    fun saveLanguageCode(context: Context, code: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, code).apply()
    }

    /** Wrap a context with the chosen locale, returning a new configured context. */
    fun wrap(context: Context): Context {
        val code = getLanguageCode(context)
        val locale = localeFor(code)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    private fun localeFor(code: String): Locale = when {
        code.startsWith("zh", ignoreCase = true) -> Locale.SIMPLIFIED_CHINESE
        code.startsWith("en", ignoreCase = true) -> Locale.US
        else -> Locale.getDefault()
    }
}
