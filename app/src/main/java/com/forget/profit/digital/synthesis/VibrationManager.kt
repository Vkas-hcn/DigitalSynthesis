package com.forget.profit.digital.synthesis

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: VibrationManager? = null

        fun getInstance(context: Context): VibrationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VibrationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var isVibrationEnabled = true


    fun vibrateLight() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return

        try {
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun vibrateMedium() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return

        try {
            val effect = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun vibrateStrong() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return

        try {
            val effect = VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun vibrateSuccess() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return

        try {
            val timings = longArrayOf(0, 100, 50, 150, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun vibrateForNumber(number: Int) {
        when {
            number <= 16 -> vibrateLight()
            number <= 256 -> vibrateMedium()
            number == 2048 -> vibrateSuccess()
            number >= 512 -> vibrateStrong()
        }
    }


    fun setVibrationEnabled(enabled: Boolean) {
        isVibrationEnabled = enabled
    }


    fun isVibrationEnabled(): Boolean {
        return isVibrationEnabled
    }


    fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }
}