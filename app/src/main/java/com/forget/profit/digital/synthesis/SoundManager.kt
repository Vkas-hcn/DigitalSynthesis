package com.forget.profit.digital.synthesis

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import kotlinx.coroutines.*

class SoundManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 背景音乐
    private var backgroundMediaPlayer: MediaPlayer? = null
    private var isBgMusicInitialized = false

    // 音效
    private var soundPool: SoundPool? = null
    private var moveSoundId: Int = 0
    private var isSoundPoolInitialized = false

    private val soundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isBgMusicEnabled = true
    private var isSoundEffectsEnabled = true
    private var lastMoveTime = 0L
    private val moveSoundDelay = 100L

    init {
        initializeAsync()
    }

    private fun initializeAsync() {
        soundScope.launch {
            try {
                initializeSoundPool()
                initializeBackgroundMusic()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun initializeSoundPool() = withContext(Dispatchers.IO) {
        try {
            soundPool?.release()

            soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                SoundPool.Builder()
                    .setMaxStreams(3)
                    .setAudioAttributes(audioAttributes)
                    .build()
            } else {
                SoundPool(3, AudioManager.STREAM_MUSIC, 0)
            }

            moveSoundId = soundPool?.load(context, R.raw.auto_move, 1) ?: 0

            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) {
                    isSoundPoolInitialized = true
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun initializeBackgroundMusic() = withContext(Dispatchers.IO) {
        try {
            backgroundMediaPlayer?.release()

            backgroundMediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes)

                val afd = context.resources.openRawResourceFd(R.raw.auto_bg)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                isLooping = true
                setVolume(0.7f, 0.7f)

                setOnPreparedListener {
                    isBgMusicInitialized = true
                }

                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("SoundManager", "MediaPlayer error: what=$what, extra=$extra")
                    true
                }

                prepareAsync()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startBackgroundMusic() {
        if (!isBgMusicEnabled) return

        soundScope.launch {
            try {
                if (isBgMusicInitialized && backgroundMediaPlayer?.isPlaying != true) {
                    backgroundMediaPlayer?.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseBackgroundMusic() {
        soundScope.launch {
            try {
                if (backgroundMediaPlayer?.isPlaying == true) {
                    backgroundMediaPlayer?.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopBackgroundMusic() {
        soundScope.launch {
            try {
                backgroundMediaPlayer?.stop()
                if (isBgMusicInitialized) {
                    backgroundMediaPlayer?.prepare()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playMoveSound() {
        if (!isSoundEffectsEnabled || !isSoundPoolInitialized) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMoveTime < moveSoundDelay) return
        lastMoveTime = currentTime

        soundScope.launch {
            try {
                soundPool?.play(moveSoundId, 0.8f, 0.8f, 1, 0, 1.0f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBgMusicEnabled(enabled: Boolean) {
        isBgMusicEnabled = enabled
        if (!enabled) {
            pauseBackgroundMusic()
        } else {
            startBackgroundMusic()
        }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        isSoundEffectsEnabled = enabled
    }

    fun setBackgroundMusicVolume(volume: Float) {
        soundScope.launch {
            try {
                val clampedVolume = volume.coerceIn(0f, 1f)
                backgroundMediaPlayer?.setVolume(clampedVolume, clampedVolume)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        soundScope.launch {
            try {
                backgroundMediaPlayer?.release()
                backgroundMediaPlayer = null

                soundPool?.release()
                soundPool = null

                isBgMusicInitialized = false
                isSoundPoolInitialized = false

                soundScope.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onActivityResume() {
        if (isBgMusicEnabled) {
            startBackgroundMusic()
        }
    }

    fun onActivityPause() {
        pauseBackgroundMusic()
    }
}