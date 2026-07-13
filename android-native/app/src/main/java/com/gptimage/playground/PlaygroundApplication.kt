package com.gptimage.playground

import android.app.Application
import com.gptimage.playground.di.AppContainer

class PlaygroundApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
