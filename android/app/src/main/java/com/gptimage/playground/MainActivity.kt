package com.gptimage.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.ui.AppRootViewModel
import com.gptimage.playground.ui.AppRootViewModelFactory
import com.gptimage.playground.ui.i18n.ChineseStrings
import com.gptimage.playground.ui.i18n.EnglishStrings
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.i18n.Strings
import com.gptimage.playground.ui.i18n.toStrings
import com.gptimage.playground.ui.navigation.AppRoot
import com.gptimage.playground.ui.theme.GptImagePlaygroundTheme

class MainActivity : ComponentActivity() {

    private val rootViewModel: AppRootViewModel by viewModels {
        AppRootViewModelFactory(application.locator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val config by rootViewModel.config.collectAsState()
            val strings = resolveStrings(config.language)

            CompositionLocalProvider(LocalStrings provides strings) {
                GptImagePlaygroundTheme(themeMode = config.themeMode) {
                    AppRoot()
                }
            }
        }
    }

    /**
     * Resolves the active [Strings]. When the user selects [AppLanguage.SYSTEM] we look at the
     * device locale and pick Chinese or English accordingly, falling back to Chinese for any other
     * locale (the app's primary audience).
     */
    private fun resolveStrings(language: AppLanguage): Strings {
        if (language != AppLanguage.SYSTEM) return language.toStrings()
        val locale = resources.configuration.locales[0]
        val tag = locale.language.lowercase()
        return when {
            tag.startsWith("en") -> EnglishStrings
            tag.startsWith("zh") -> ChineseStrings
            else -> ChineseStrings
        }
    }
}
