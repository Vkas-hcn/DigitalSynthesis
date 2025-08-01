package com.forget.profit.digital.synthesis

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.forget.profit.digital.synthesis.databinding.ActivityGameBinding
import androidx.core.content.edit

class GameActivity : AppCompatActivity(), Game2048.GameListener, GameView.OnSwipeListener, GameView.OnAnimationCompleteListener {

    private val binding by lazy { ActivityGameBinding.inflate(layoutInflater) }
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var game: Game2048
    private lateinit var gameView: GameView
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager
    private var gameSize = 4
    private var hasWon = false
    private var hasStarted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gamedetail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        soundManager = SoundManager.getInstance(this)

        vibrationManager = VibrationManager.getInstance(this)

        initGame()
        setupClickListeners()
    }

    private fun initGame() {
        sharedPrefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

        gameSize = intent.getIntExtra("game_size", MainActivity.DEFAULT_SIZE)

        game = Game2048(gameSize)
        game.setGameListener(this)

        gameView = GameView(this)
        gameView.setOnSwipeListener(this)
        gameView.setOnAnimationCompleteListener(this)
        gameView.updateGrid(game.getGrid(), gameSize)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        gameView.layoutParams = layoutParams

        gameView.isClickable = true
        gameView.isFocusable = true
        gameView.isFocusableInTouchMode = true

        binding.gameContainer.removeAllViews()
        binding.gameContainer.addView(gameView)

        updateScoreDisplay()
        updateHighScoreDisplay()
        binding.tvGridSize.text = "${gameSize}×${gameSize}"
    }

    private fun setupClickListeners() {
        onBackPressedDispatcher.addCallback(this) {
            if (hasStarted) {
                showExitDialog()
            } else {
                finish()
            }
        }

        with(binding) {
            btnBack.setOnClickListener {
                if (hasStarted) {
                    showExitDialog()
                } else {
                    finish()
                }
            }

            btnUndo.setOnClickListener {
                val success = game.undo()
                if (success) {
                    gameView.updateGrid(game.getGrid(), gameSize)
                } else {
                    showToast("Cannot be revoked")
                }
            }

            btnRestart.setOnClickListener {
                showRestartDialog()
            }
        }
    }

    private fun updateScoreDisplay() {
        binding.tvCurrentScore.text = game.getScore().toString()
    }

    private fun updateHighScoreDisplay() {
        val highScoreKey = "high_score_${gameSize}x${gameSize}"
        val highScore = sharedPrefs.getInt(highScoreKey, 0)
        binding.tvHighScore.text = highScore.toString()
    }

    private fun updateHighScore() {
        val highScoreKey = "high_score_${gameSize}x${gameSize}"
        val currentHighScore = sharedPrefs.getInt(highScoreKey, 0)
        val currentScore = game.getScore()

        if (currentScore > currentHighScore) {
            sharedPrefs.edit { putInt(highScoreKey, currentScore) }
            updateHighScoreDisplay()
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle("restart")
            .setMessage("Are you sure you want to start the game again? The current progress will be lost.")
            .setPositiveButton("Sure") { _, _ ->
                restartGame()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Game")
            .setMessage("Are you sure you want to exit the game? Current progress will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restartGame() {
        game.initGame()
        hasWon = false
        hasStarted = true
        updateScoreDisplay()
        gameView.updateGrid(game.getGrid(), gameSize)
        gameView.requestLayout()
        gameView.invalidate()
    }

    @SuppressLint("SetTextI18n")
    private fun showGameOverDialog() {
        binding.tvFailTip.text = "Great job on scoring ${game.getScore()} in 2048! Unfortunately, the game is over,and 2048 wasn't reached. "
        binding.llDialogFail.isVisible = true

        binding.tvBye.setOnClickListener {
            finish()
        }
        binding.tvTry.setOnClickListener {
            binding.llDialogFail.isVisible = false
            restartGame()
        }
    }

    private fun showWinDialog() {
        binding.llDialogSuccess.isVisible = true
        binding.tvConfirm.setOnClickListener {
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onScoreChanged(score: Int) {
        runOnUiThread {
            hasStarted = true
            updateScoreDisplay()
            updateHighScore()
        }
    }

    override fun onGridChanged() {
        runOnUiThread {
            hasStarted = true
            gameView.updateGrid(game.getGrid(), gameSize)
        }
    }

    override fun onGameWon() {
        runOnUiThread {
            if (!hasWon) {
                hasWon = true
                vibrationManager.vibrateSuccess()
                showWinDialog()
            }
        }
    }

    override fun onGameOver() {
        runOnUiThread {
            showGameOverDialog()
        }
    }

    override fun onSwipe(direction: Direction) {
        val preview = game.previewMove(direction)

        if (preview.hasChanged) {
            soundManager.playMoveSound()

            gameView.startMoveAnimation(direction, preview.newGrid)
        }
    }

    override fun onAnimationComplete(direction: Direction) {
        runOnUiThread {
            val moved = game.move(direction)
        }
    }

    override fun onFirstTimeAchievement(number: Int) {
        runOnUiThread {
            gameView.triggerAchievement(number)
        }
    }

    override fun onNumberMerged(number: Int) {
        runOnUiThread {
            vibrationManager.vibrateForNumber(number)
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager.onActivityResume()
    }

    override fun onPause() {
        super.onPause()
        soundManager.onActivityPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}