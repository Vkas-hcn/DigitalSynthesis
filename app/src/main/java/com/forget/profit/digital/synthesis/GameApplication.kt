package com.forget.profit.digital.synthesis

import android.app.Application

class GameApplication : Application() {

    private var soundManager: SoundManager? = null
    private var vibrationManager: VibrationManager? = null

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager.getInstance(this)

        vibrationManager = VibrationManager.getInstance(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        soundManager?.release()
        soundManager = null

        vibrationManager = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        soundManager?.pauseBackgroundMusic()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                soundManager?.pauseBackgroundMusic()
            }
        }
    }
}