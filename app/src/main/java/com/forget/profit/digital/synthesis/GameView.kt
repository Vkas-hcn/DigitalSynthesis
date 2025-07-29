package com.forget.profit.digital.synthesis

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

// 动画信息数据类
data class AnimationInfo(
    val value: Int,
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val isMerging: Boolean = false,
    val isNewTile: Boolean = false
)

// 合并动画信息
data class MergeAnimationInfo(
    val row: Int,
    val col: Int,
    val newValue: Int,
    val mergingTiles: List<AnimationInfo>
)

open class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var grid: Array<IntArray>? = null
    private var size = 4
    private var cellSize = 0f
    private var padding = 20f
    private var cornerRadius = 8f
    private var isInteractive = true

    // 动画相关属性
    private var isAnimating = false
    private var animationProgress = 0f
    private var moveAnimations = mutableListOf<AnimationInfo>()
    private var mergeAnimations = mutableListOf<MergeAnimationInfo>()
    private var pendingGrid: Array<IntArray>? = null
    private var originalGrid: Array<IntArray>? = null
    private val animationDuration = 180L // 移动动画时长
    private val mergeAnimationDuration = 100L // 合并动画时长
    private var mergeAnimationProgress = 0f
    private var currentAnimator: ValueAnimator? = null

    // 成就动画渲染器
    private val achievementRenderer = NumberAchievementRenderer()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
    }

    private val emptyCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCC0B3")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val cellColors = mapOf(
        0 to Color.parseColor("#EAEAEA"),
        2 to Color.parseColor("#F9E8D7"),
        4 to Color.parseColor("#FEDBB7"),
        8 to Color.parseColor("#FABF82"),
        16 to Color.parseColor("#FF9F3D"),
        32 to Color.parseColor("#FD8913"),
        64 to Color.parseColor("#F36212"),
        128 to Color.parseColor("#FFC93A"),
        256 to Color.parseColor("#ECAB00"),
        512 to Color.parseColor("#CA9300"),
        1024 to Color.parseColor("#FC6E2C"),
        2048 to Color.parseColor("#333333")
    )

    private val textColors = mapOf(
        0 to Color.TRANSPARENT,
        2 to Color.parseColor("#222222"),
        4 to Color.parseColor("#222222"),
        8 to Color.WHITE,
        16 to Color.WHITE,
        32 to Color.WHITE,
        64 to Color.WHITE,
        128 to Color.WHITE,
        256 to Color.WHITE,
        512 to Color.WHITE,
        1024 to Color.WHITE,
        2048 to Color.WHITE
    )

    // 手势检测
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (!isInteractive || isAnimating) return false

            val deltaX = e2.x - (e1?.x ?: 0f)
            val deltaY = e2.y - (e1?.y ?: 0f)

            if (abs(deltaX) > abs(deltaY)) {
                // 水平滑动
                if (abs(deltaX) > 50) {
                    val direction = if (deltaX > 0) Direction.RIGHT else Direction.LEFT
                    startMoveAnimation(direction)
                    return true
                }
            } else {
                // 垂直滑动
                if (abs(deltaY) > 50) {
                    val direction = if (deltaY > 0) Direction.DOWN else Direction.UP
                    startMoveAnimation(direction)
                    return true
                }
            }
            return false
        }
    })

    interface OnSwipeListener {
        fun onSwipe(direction: Direction)
    }

    interface OnAnimationCompleteListener {
        fun onAnimationComplete(direction: Direction)
    }

    private var onSwipeListener: OnSwipeListener? = null
    private var onAnimationCompleteListener: OnAnimationCompleteListener? = null

    fun setOnSwipeListener(listener: OnSwipeListener) {
        this.onSwipeListener = listener
    }

    fun setOnAnimationCompleteListener(listener: OnAnimationCompleteListener) {
        this.onAnimationCompleteListener = listener
    }

    fun setInteractive(interactive: Boolean) {
        this.isInteractive = interactive
    }

    fun updateGrid(newGrid: Array<IntArray>, newSize: Int) {
        this.grid = newGrid
        this.size = newSize
        calculateSizes()
        invalidate()
    }

    fun triggerAchievement(number: Int) {
        achievementRenderer.triggerAchievement(number, size) {
            invalidate()
        }
    }

    fun startMoveAnimation(direction: Direction, newGrid: Array<IntArray>) {
        val currentGrid = grid ?: return

        originalGrid = Array(size) { i -> IntArray(size) { j -> currentGrid[i][j] } }

        val animationData = calculateAnimations(currentGrid, direction)
        moveAnimations = animationData.first as MutableList<AnimationInfo>
        mergeAnimations = animationData.second as MutableList<MergeAnimationInfo>
        pendingGrid = newGrid

        if (moveAnimations.isEmpty() && mergeAnimations.isEmpty()) {
            return
        }

        isAnimating = true
        animationProgress = 0f
        mergeAnimationProgress = 0f

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (mergeAnimations.isNotEmpty()) {
                        startMergeAnimation(direction)
                    } else {
                        finishAnimation(direction)
                    }
                }
            })
            start()
        }
    }

    private fun startMoveAnimation(direction: Direction) {
        onSwipeListener?.onSwipe(direction)
    }

    private fun startMergeAnimation(direction: Direction) {
        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = mergeAnimationDuration
            addUpdateListener { animator ->
                mergeAnimationProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishAnimation(direction)
                }
            })
            start()
        }
    }

    private fun finishAnimation(direction: Direction) {
        isAnimating = false
        animationProgress = 0f
        mergeAnimationProgress = 0f
        moveAnimations.clear()
        mergeAnimations.clear()

        pendingGrid?.let { newGrid ->
            grid = newGrid
            invalidate()
        }

        onAnimationCompleteListener?.onAnimationComplete(direction)
    }

    private fun calculateAnimations(grid: Array<IntArray>, direction: Direction): Pair<List<AnimationInfo>, List<MergeAnimationInfo>> {
        val moveAnims = mutableListOf<AnimationInfo>()
        val mergeAnims = mutableListOf<MergeAnimationInfo>()

        when (direction) {
            Direction.LEFT -> calculateLeftAnimations(grid, moveAnims, mergeAnims)
            Direction.RIGHT -> calculateRightAnimations(grid, moveAnims, mergeAnims)
            Direction.UP -> calculateUpAnimations(grid, moveAnims, mergeAnims)
            Direction.DOWN -> calculateDownAnimations(grid, moveAnims, mergeAnims)
        }

        return Pair(moveAnims, mergeAnims)
    }

    private fun calculateLeftAnimations(grid: Array<IntArray>, moveAnims: MutableList<AnimationInfo>, mergeAnims: MutableList<MergeAnimationInfo>) {
        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()
            val mergedPositions = mutableSetOf<Int>()

            var targetCol = 0
            for (j in 0 until size) {
                if (grid[i][j] != 0) {
                    if (j != targetCol) {
                        moveAnims.add(AnimationInfo(grid[i][j], i, j, i, targetCol))
                    }
                    targetCol++
                }
            }

            var col = 0
            var sourceCol = 0
            while (sourceCol < row.size) {
                if (sourceCol < row.size - 1 && row[sourceCol] == row[sourceCol + 1] && row[sourceCol] != 0) {
                    val mergingTiles = mutableListOf<AnimationInfo>()
                    var found = 0
                    for (k in 0 until size) {
                        if (grid[i][k] == row[sourceCol]) {
                            mergingTiles.add(AnimationInfo(grid[i][k], i, k, i, col, true))
                            found++
                            if (found == 2) break
                        }
                    }
                    mergeAnims.add(MergeAnimationInfo(i, col, row[sourceCol] * 2, mergingTiles))
                    sourceCol += 2
                } else {
                    sourceCol++
                }
                col++
            }
        }
    }

    private fun calculateRightAnimations(grid: Array<IntArray>, moveAnims: MutableList<AnimationInfo>, mergeAnims: MutableList<MergeAnimationInfo>) {
        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()

            var targetCol = size - 1
            for (j in size - 1 downTo 0) {
                if (grid[i][j] != 0) {
                    if (j != targetCol) {
                        moveAnims.add(AnimationInfo(grid[i][j], i, j, i, targetCol))
                    }
                    targetCol--
                }
            }

            var col = size - 1
            var sourceCol = row.size - 1
            while (sourceCol > 0) {
                if (row[sourceCol] == row[sourceCol - 1] && row[sourceCol] != 0) {
                    val mergingTiles = mutableListOf<AnimationInfo>()
                    var found = 0
                    for (k in size - 1 downTo 0) {
                        if (grid[i][k] == row[sourceCol]) {
                            mergingTiles.add(AnimationInfo(grid[i][k], i, k, i, col, true))
                            found++
                            if (found == 2) break
                        }
                    }
                    mergeAnims.add(MergeAnimationInfo(i, col, row[sourceCol] * 2, mergingTiles))
                    sourceCol -= 2
                } else {
                    sourceCol--
                }
                col--
            }
        }
    }

    private fun calculateUpAnimations(grid: Array<IntArray>, moveAnims: MutableList<AnimationInfo>, mergeAnims: MutableList<MergeAnimationInfo>) {
        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            var targetRow = 0
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    if (i != targetRow) {
                        moveAnims.add(AnimationInfo(grid[i][j], i, j, targetRow, j))
                    }
                    targetRow++
                }
            }

            var row = 0
            var sourceRow = 0
            while (sourceRow < column.size) {
                if (sourceRow < column.size - 1 && column[sourceRow] == column[sourceRow + 1] && column[sourceRow] != 0) {
                    val mergingTiles = mutableListOf<AnimationInfo>()
                    var found = 0
                    for (k in 0 until size) {
                        if (grid[k][j] == column[sourceRow]) {
                            mergingTiles.add(AnimationInfo(grid[k][j], k, j, row, j, true))
                            found++
                            if (found == 2) break
                        }
                    }
                    mergeAnims.add(MergeAnimationInfo(row, j, column[sourceRow] * 2, mergingTiles))
                    sourceRow += 2
                } else {
                    sourceRow++
                }
                row++
            }
        }
    }

    private fun calculateDownAnimations(grid: Array<IntArray>, moveAnims: MutableList<AnimationInfo>, mergeAnims: MutableList<MergeAnimationInfo>) {
        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            // 记录移动动画
            var targetRow = size - 1
            for (i in size - 1 downTo 0) {
                if (grid[i][j] != 0) {
                    if (i != targetRow) {
                        moveAnims.add(AnimationInfo(grid[i][j], i, j, targetRow, j))
                    }
                    targetRow--
                }
            }

            var row = size - 1
            var sourceRow = column.size - 1
            while (sourceRow > 0) {
                if (column[sourceRow] == column[sourceRow - 1] && column[sourceRow] != 0) {
                    val mergingTiles = mutableListOf<AnimationInfo>()
                    var found = 0
                    for (k in size - 1 downTo 0) {
                        if (grid[k][j] == column[sourceRow]) {
                            mergingTiles.add(AnimationInfo(grid[k][j], k, j, row, j, true))
                            found++
                            if (found == 2) break
                        }
                    }
                    mergeAnims.add(MergeAnimationInfo(row, j, column[sourceRow] * 2, mergingTiles))
                    sourceRow -= 2
                } else {
                    sourceRow--
                }
                row--
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = when {
            widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY -> {
                min(widthSize, heightSize)
            }
            widthMode == MeasureSpec.EXACTLY -> {
                widthSize
            }
            heightMode == MeasureSpec.EXACTLY -> {
                heightSize
            }
            else -> {
                min(widthSize, heightSize)
            }
        }

        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateSizes()
    }

    private fun calculateSizes() {
        if (width == 0 || height == 0) return

        val viewSize = min(width, height)

        val basePadding = viewSize * 0.02f
        padding = basePadding

        val totalPadding = padding * (size + 1)
        cellSize = (viewSize - totalPadding) / size

        if (cellSize < 20f) {
            cellSize = 20f
            padding = (viewSize - cellSize * size) / (size + 1)
        }

        textPaint.textSize = cellSize * 0.4f
        cornerRadius = cellSize * 0.08f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentGrid = grid ?: return

        val backgroundRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(backgroundRect, cornerRadius * 2, cornerRadius * 2, backgroundPaint)

        val totalGridSize = size * cellSize + (size - 1) * padding
        val startX = (width - totalGridSize) / 2
        val startY = (height - totalGridSize) / 2

        // 绘制空白网格背景
        for (i in 0 until size) {
            for (j in 0 until size) {
                val left = startX + j * (cellSize + padding)
                val top = startY + i * (cellSize + padding)
                val right = left + cellSize
                val bottom = top + cellSize

                val cellRect = RectF(left, top, right, bottom)
                canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, emptyCellPaint)
            }
        }

        if (isAnimating) {
            drawAnimatingTiles(canvas, startX, startY)
        } else {
            drawStaticTiles(canvas, startX, startY, currentGrid)
        }

        if (achievementRenderer.hasActiveAnimations()) {
            val centerX = width / 2f
            val centerY = height / 2f
            val baseTextSize = cellSize * 0.4f
            achievementRenderer.drawAchievements(canvas, centerX, centerY, baseTextSize, size)
        }
    }

    private fun drawStaticTiles(canvas: Canvas, startX: Float, startY: Float, grid: Array<IntArray>) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = grid[i][j]
                if (value > 0) {
                    val left = startX + j * (cellSize + padding)
                    val top = startY + i * (cellSize + padding)
                    drawTile(canvas, left, top, value, 1f)
                }
            }
        }
    }

    private fun drawAnimatingTiles(canvas: Canvas, startX: Float, startY: Float) {
        val originalGrid = originalGrid ?: return

        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = originalGrid[i][j]
                if (value > 0) {
                    val isMoving = moveAnimations.any { it.fromRow == i && it.fromCol == j }
                    val isMerging = mergeAnimations.any { merge ->
                        merge.mergingTiles.any { it.fromRow == i && it.fromCol == j }
                    }

                    if (!isMoving && !isMerging) {
                        val left = startX + j * (cellSize + padding)
                        val top = startY + i * (cellSize + padding)
                        drawTile(canvas, left, top, value, 1f)
                    }
                }
            }
        }

        for (moveAnim in moveAnimations) {
            if (!moveAnim.isMerging) {
                val fromLeft = startX + moveAnim.fromCol * (cellSize + padding)
                val fromTop = startY + moveAnim.fromRow * (cellSize + padding)
                val toLeft = startX + moveAnim.toCol * (cellSize + padding)
                val toTop = startY + moveAnim.toRow * (cellSize + padding)

                val currentLeft = fromLeft + (toLeft - fromLeft) * animationProgress
                val currentTop = fromTop + (toTop - fromTop) * animationProgress

                drawTile(canvas, currentLeft, currentTop, moveAnim.value, 1f)
            }
        }

        for (mergeAnim in mergeAnimations) {
            val targetLeft = startX + mergeAnim.col * (cellSize + padding)
            val targetTop = startY + mergeAnim.row * (cellSize + padding)

            if (animationProgress >= 1f) {
                for (mergingTile in mergeAnim.mergingTiles) {
                    val alpha = 1f - mergeAnimationProgress * 0.5f
                    val scale = 1f - mergeAnimationProgress * 0.2f
                    drawTile(canvas, targetLeft, targetTop, mergingTile.value, alpha, scale)
                }

                val scale = 0.8f + mergeAnimationProgress * 0.4f
                drawTile(canvas, targetLeft, targetTop, mergeAnim.newValue, 1f, scale)
            } else {
                for (mergingTile in mergeAnim.mergingTiles) {
                    val fromLeft = startX + mergingTile.fromCol * (cellSize + padding)
                    val fromTop = startY + mergingTile.fromRow * (cellSize + padding)

                    val currentLeft = fromLeft + (targetLeft - fromLeft) * animationProgress
                    val currentTop = fromTop + (targetTop - fromTop) * animationProgress

                    drawTile(canvas, currentLeft, currentTop, mergingTile.value, 1f)
                }
            }
        }
    }

    private fun drawTile(canvas: Canvas, left: Float, top: Float, value: Int, alpha: Float = 1f, scale: Float = 1f) {
        val scaledCellSize = cellSize * scale
        val offset = (cellSize - scaledCellSize) / 2

        val adjustedLeft = left + offset
        val adjustedTop = top + offset
        val right = adjustedLeft + scaledCellSize
        val bottom = adjustedTop + scaledCellSize

        val cellRect = RectF(adjustedLeft, adjustedTop, right, bottom)

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cellColors[value] ?: cellColors[0]!!
            this.alpha = (255 * alpha).toInt()
        }
        canvas.drawRoundRect(cellRect, cornerRadius * scale, cornerRadius * scale, cellPaint)

        textPaint.color = textColors[value] ?: Color.WHITE
        textPaint.alpha = (255 * alpha).toInt()

        val textSize = when {
            size <= 4 -> when {
                value < 100 -> scaledCellSize * 0.5f
                value < 1000 -> scaledCellSize * 0.4f
                else -> scaledCellSize * 0.35f
            }
            size <= 6 -> when {
                value < 100 -> scaledCellSize * 0.5f
                value < 1000 -> scaledCellSize * 0.4f
                else -> scaledCellSize * 0.4f
            }
            else -> when {
                value < 100 -> scaledCellSize * 0.5f
                value < 1000 -> scaledCellSize * 0.4f
                else -> scaledCellSize * 0.5f
            }
        }
        textPaint.textSize = textSize

        val centerX = adjustedLeft + scaledCellSize / 2
        val centerY = adjustedTop + scaledCellSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(value.toString(), centerX, centerY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isInteractive && !isAnimating) {
            val handled = gestureDetector.onTouchEvent(event)
            handled || super.onTouchEvent(event)
        } else {
            false
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimator?.cancel()
        achievementRenderer.clear()
    }
}

class PreviewGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    init {
        setInteractive(false)
    }

    fun setPreviewSize(size: Int) {
        val game = Game2048(size)
        val previewGrid = game.generatePreviewGrid()
        updateGrid(previewGrid, size)
    }
}