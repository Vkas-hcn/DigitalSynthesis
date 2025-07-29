package com.forget.profit.digital.synthesis

import android.animation.ValueAnimator
import android.graphics.*
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

data class AchievementAnimation(
    val number: Int,
    var progress: Float = 0f,
    var isActive: Boolean = true,
    val startTime: Long = System.currentTimeMillis()
)

class NumberAchievementRenderer {

    companion object {
        private const val ANIMATION_DURATION = 1200L // 动画总时长
        private const val SCALE_PEAK = 1.8f // 最大缩放比例
        private const val INITIAL_ALPHA = 0.95f // 初始透明度
    }

    private val achievements = mutableListOf<AchievementAnimation>()
    private var animator: ValueAnimator? = null

    // 画笔初始化
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    // 数字颜色映射
    private val numberColors = mapOf(
        4 to Color.parseColor("#FF6B6B"),
        8 to Color.parseColor("#4ECDC4"),
        16 to Color.parseColor("#45B7D1"),
        32 to Color.parseColor("#96CEB4"),
        64 to Color.parseColor("#FECA57"),
        128 to Color.parseColor("#FF9FF3"),
        256 to Color.parseColor("#54A0FF"),
        512 to Color.parseColor("#5F27CD"),
        1024 to Color.parseColor("#FF3838"),
        2048 to Color.parseColor("#FFD700"),
        4096 to Color.parseColor("#FF1493")
    )

    fun triggerAchievement(number: Int, gridSize: Int, onUpdate: () -> Unit) {
        if (achievements.isNotEmpty()) {
            val maxCurrentNumber = achievements.maxOfOrNull { it.number } ?: 0
            if (number <= maxCurrentNumber) {
                return
            } else {
                achievements.clear()
                animator?.cancel()
            }
        }

        achievements.add(AchievementAnimation(number))

        startAnimation(onUpdate)
    }

    private fun startAnimation(onUpdate: () -> Unit) {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                achievements.forEach { achievement ->
                    if (achievement.isActive) {
                        achievement.progress = progress
                    }
                }

                achievements.removeAll {
                    it.progress >= 1f || System.currentTimeMillis() - it.startTime > ANIMATION_DURATION
                }

                onUpdate()

                if (achievements.isEmpty()) {
                    cancel()
                }
            }

            start()
        }
    }

    fun drawAchievements(canvas: Canvas, centerX: Float, centerY: Float, baseTextSize: Float, gridSize: Int) {
        achievements.forEach { achievement ->
            if (achievement.isActive) {
                drawSingleAchievement(canvas, achievement, centerX, centerY, baseTextSize, gridSize)
            }
        }
    }

    private fun drawSingleAchievement(
        canvas: Canvas,
        achievement: AchievementAnimation,
        centerX: Float,
        centerY: Float,
        baseTextSize: Float,
        gridSize: Int
    ) {
        val progress = achievement.progress
        val number = achievement.number

        val scale = calculateScale(progress)
        val alpha = calculateAlpha(progress)
        val yOffset = calculateYOffset(progress)
        val rotation = calculateRotation(progress)
        val glowIntensity = calculateGlowIntensity(progress)

        val dynamicTextSize = calculateDynamicTextSize(baseTextSize, gridSize, scale)

        val color = numberColors[number] ?: Color.parseColor("#333333")

        canvas.save()

        canvas.translate(centerX, centerY + yOffset)
        canvas.rotate(rotation, 0f, 0f)

        if (glowIntensity > 0f) {
            glowPaint.apply {
                this.textSize = dynamicTextSize * 1.2f
                this.color = color
                this.alpha = (glowIntensity * 100).toInt().coerceIn(0, 255)
            }
            canvas.drawText(number.toString(), 0f, 0f, glowPaint)
        }

        shadowPaint.apply {
            this.textSize = dynamicTextSize
            this.color = Color.BLACK
            this.alpha = (alpha * 0.3f * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText(number.toString(), 4f, 4f, shadowPaint)

        textPaint.apply {
            this.textSize = dynamicTextSize
            this.color = color
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText(number.toString(), 0f, 0f, textPaint)

        if (progress < 0.6f) {
            drawSparkles(canvas, dynamicTextSize, alpha, color)
        }

        canvas.restore()
    }

    private fun calculateScale(progress: Float): Float {
        return when {
            progress < 0.3f -> {
                1f + (SCALE_PEAK - 1f) * (progress / 0.3f)
            }
            progress < 0.7f -> {
                SCALE_PEAK
            }
            else -> {
                SCALE_PEAK * (1f - (progress - 0.7f) / 0.3f * 0.5f)
            }
        }
    }

    private fun calculateAlpha(progress: Float): Float {
        return when {
            progress < 0.1f -> {
                INITIAL_ALPHA * (progress / 0.1f)
            }
            progress < 0.7f -> {
                INITIAL_ALPHA
            }
            else -> {
                INITIAL_ALPHA * (1f - (progress - 0.7f) / 0.3f)
            }
        }
    }

    private fun calculateYOffset(progress: Float): Float {
        return if (progress < 0.5f) {
            -20f * sin(progress * Math.PI).toFloat()
        } else {
            -10f * (1f - progress)
        }
    }

    private fun calculateRotation(progress: Float): Float {
        return if (progress < 0.4f) {
            sin(progress * Math.PI * 4).toFloat() * 3f
        } else {
            0f
        }
    }

    private fun calculateGlowIntensity(progress: Float): Float {
        return when {
            progress < 0.2f -> progress / 0.2f
            progress < 0.5f -> 1f
            else -> 1f - (progress - 0.5f) / 0.5f
        }
    }

    private fun calculateDynamicTextSize(baseTextSize: Float, gridSize: Int, scale: Float): Float {
        val baseSizeMultiplier = when (gridSize) {
            3 -> 2f
            4 -> 2.5f
            5 -> 3.0f
            6 -> 3.5f
            7 -> 8.0f
            8 -> 10.0f
            else -> {
                2.5f + (gridSize - 4) * 0.3f
            }
        }

        val dynamicSize = baseTextSize * scale * baseSizeMultiplier

        val minSize = baseTextSize * 1.5f
        val maxSize = baseTextSize * 10.0f

        return dynamicSize.coerceIn(minSize, maxSize)
    }

    private fun drawSparkles(canvas: Canvas, dynamicTextSize: Float, alpha: Float, baseColor: Int) {
        val sparkleCount = 8
        val radius = dynamicTextSize * 0.8f

        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            this.alpha = (alpha * 180).toInt().coerceIn(0, 255)
        }

        for (i in 0 until sparkleCount) {
            val angle = (i * 360f / sparkleCount) + (System.currentTimeMillis() / 10f) % 360f
            val x = radius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val y = radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()

            val sparkleSize = dynamicTextSize * 0.05f * (0.5f + 0.5f * sin(System.currentTimeMillis() / 100f + i).toFloat())
            canvas.drawCircle(x, y, sparkleSize, sparklePaint)
        }
    }

    fun hasActiveAnimations(): Boolean = achievements.isNotEmpty()

    fun clear() {
        achievements.clear()
        animator?.cancel()
        animator = null
    }
}