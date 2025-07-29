package com.forget.profit.digital.synthesis

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.forget.profit.digital.synthesis.databinding.ActivityMainBinding
import androidx.core.net.toUri
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREF_GAME_SIZE = "game_size"
        const val PREF_HIGH_SCORE = "high_score"
        const val DEFAULT_SIZE = 4
        const val MIN_SIZE = 3
        const val MAX_SIZE = 8
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var soundManager: SoundManager
    private var currentSize = DEFAULT_SIZE
    private lateinit var previewGameView: PreviewGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dl_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()

        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this)

        initViews()
        clickFun()
    }

    private fun initViews() {
        sharedPrefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        currentSize = sharedPrefs.getInt(PREF_GAME_SIZE, DEFAULT_SIZE)

        previewGameView = PreviewGameView(this)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        previewGameView.layoutParams = layoutParams

        binding.flGame.removeAllViews()
        binding.flGame.addView(previewGameView)

        updateSizeDisplay()
        updateNavigationButtons()
        updateGamePreview()
    }

    private fun clickFun() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.dlMain.isOpen) {
                binding.dlMain.close()
            } else {
                finish()
            }
        }

        with(binding) {
            imgMenu.setOnClickListener {
                if (dlMain.isOpen) {
                    dlMain.close()
                } else {
                    dlMain.open()
                }
            }

            privacyPolicyButton.setOnClickListener {
                openPrivacyPolicy()
            }

            imgPrevious.setOnClickListener {
                if (currentSize > MIN_SIZE) {
                    currentSize--
                    updateGameSettings()
                    // 可以在这里添加按钮点击音效
                    // soundManager.playButtonClickSound() // 如果你有按钮点击音效的话
                }
            }

            imgNext.setOnClickListener {
                if (currentSize < MAX_SIZE) {
                    currentSize++
                    updateGameSettings()
                    // 可以在这里添加按钮点击音效
                    // soundManager.playButtonClickSound() // 如果你有按钮点击音效的话
                }
            }

            tvPlay.setOnClickListener {
                startGame()
            }
        }
    }

    private fun updateGameSettings() {
        sharedPrefs.edit { putInt(PREF_GAME_SIZE, currentSize) }
        updateSizeDisplay()
        updateNavigationButtons()
        updateGamePreview()
    }

    private fun updateSizeDisplay() {
        binding.tvNum.text = "${currentSize}×${currentSize}"
    }

    private fun updateNavigationButtons() {
        with(binding) {
            if (currentSize <= MIN_SIZE) {
                imgPrevious.setBackgroundResource(R.drawable.bg_not_selete)
                imgPrevious.setImageResource(R.drawable.ic_previous_1)
                imgPrevious.isEnabled = false
                imgPrevious.alpha = 0.5f
            } else {
                imgPrevious.setBackgroundResource(R.drawable.bg_selete)
                imgPrevious.setImageResource(R.drawable.ic_previous_2)
                imgPrevious.isEnabled = true
                imgPrevious.alpha = 1.0f
            }

            if (currentSize >= MAX_SIZE) {
                imgNext.setBackgroundResource(R.drawable.bg_not_selete)
                imgNext.setImageResource(R.drawable.ic_next_1)
                imgNext.isEnabled = false
                imgNext.alpha = 0.5f
            } else {
                imgNext.setBackgroundResource(R.drawable.bg_selete)
                imgNext.setImageResource(R.drawable.ic_next_2)
                imgNext.isEnabled = true
                imgNext.alpha = 1.0f
            }
        }
    }

    private fun updateGamePreview() {
        previewGameView.setPreviewSize(currentSize)
        previewGameView.requestLayout()
        previewGameView.invalidate()
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("game_size", currentSize)
        }
        startActivity(intent)
    }

    private fun openPrivacyPolicy() {
        val url = "https://google.com"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateGamePreview()
         soundManager.onActivityResume()
    }

    override fun onPause() {
        super.onPause()
    }
}