package com.gptimage.playground

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.ThemeModeConfig
import com.gptimage.playground.ui.navigation.PlaygroundApp
import com.gptimage.playground.ui.theme.PlaygroundTheme
import com.gptimage.playground.ui.theme.ThemeMode
import com.gptimage.playground.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PlaygroundApplication).container

        // Mirror the persisted language into the synchronous locale cache so
        // attachBaseContext can apply it on the next recreation.
        lifecycleScope.launch {
            val config = container.configRepository.config.first()
            LocaleHelper.saveLanguageCode(this@MainActivity, config.appLanguage)
        }

        setContent {
            val config by container.configRepository.config.collectAsState(initial = AppConfig())

            val themeMode = when (config.resolvedThemeMode) {
                ThemeModeConfig.Light -> ThemeMode.Light
                ThemeModeConfig.Dark -> ThemeMode.Dark
                ThemeModeConfig.System -> ThemeMode.System
            }

            PlaygroundTheme(themeMode = themeMode) {
                PlaygroundApp(container = container)
            }
        }
    }
}
