package com.forget.profit.digital.synthesis

import kotlin.random.Random

data class GameState(
    val grid: Array<IntArray>,
    val score: Int,
    val size: Int,
    val achievedNumbers: Set<Int> = emptySet()
) {
    fun deepCopy(): GameState {
        val newGrid = Array(size) { i ->
            IntArray(size) { j ->
                grid[i][j]
            }
        }
        return GameState(newGrid, score, size, achievedNumbers.toSet())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (score != other.score) return false
        if (size != other.size) return false
        if (achievedNumbers != other.achievedNumbers) return false
        if (!grid.contentDeepEquals(other.grid)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = score
        result = 31 * result + size
        result = 31 * result + achievedNumbers.hashCode()
        result = 31 * result + grid.contentDeepHashCode()
        return result
    }
}

data class MovePreview(
    val newGrid: Array<IntArray>,
    val newScore: Int,
    val hasChanged: Boolean,
    val scoreIncrease: Int,
    val newAchievements: Set<Int> = emptySet(),
    val mergedNumbers: List<Int> = emptyList()
)

class Game2048(private val size: Int) {
    private var grid: Array<IntArray> = Array(size) { IntArray(size) }
    private var score = 0
    private val gameHistory = mutableListOf<GameState>()

    private val achievedNumbers = mutableSetOf<Int>()

    interface GameListener {
        fun onScoreChanged(score: Int)
        fun onGridChanged()
        fun onGameWon()
        fun onGameOver()
        fun onFirstTimeAchievement(number: Int)
        fun onNumberMerged(number: Int)
    }

    private var gameListener: GameListener? = null

    fun setGameListener(listener: GameListener) {
        this.gameListener = listener
    }

    init {
        initGame()
    }

    fun initGame() {
        for (i in 0 until size) {
            for (j in 0 until size) {
                grid[i][j] = 0
            }
        }
        score = 0
        achievedNumbers.clear()
        gameHistory.clear()

        addRandomNumber()
        addRandomNumber()

        saveState()
        gameListener?.onScoreChanged(score)
        gameListener?.onGridChanged()
    }

    private fun saveState() {
        val state = GameState(grid, score, size, achievedNumbers.toSet())
        gameHistory.add(state.deepCopy())

        if (gameHistory.size > 20) {
            gameHistory.removeAt(0)
        }
    }

    fun undo(): Boolean {
        if (gameHistory.size <= 1) return false

        gameHistory.removeAt(gameHistory.size - 1)

        val previousState = gameHistory.last()
        grid = previousState.deepCopy().grid
        score = previousState.score
        achievedNumbers.clear()
        achievedNumbers.addAll(previousState.achievedNumbers)

        gameListener?.onScoreChanged(score)
        gameListener?.onGridChanged()

        return true
    }

    private fun addRandomNumber() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()

        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells[Random.nextInt(emptyCells.size)]
            val value = if (Random.nextFloat() < 0.9f) 2 else 4
            grid[randomCell.first][randomCell.second] = value
        }
    }

    fun previewMove(direction: Direction): MovePreview {
        val newGrid = Array(size) { i -> IntArray(size) { j -> grid[i][j] } }
        var newScore = score
        var scoreIncrease = 0
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        when (direction) {
            Direction.LEFT -> {
                val result = moveLeftPreview(newGrid)
                scoreIncrease = result.first
                newAchievements.addAll(result.second)
                mergedNumbers.addAll(result.third)
            }
            Direction.RIGHT -> {
                val result = moveRightPreview(newGrid)
                scoreIncrease = result.first
                newAchievements.addAll(result.second)
                mergedNumbers.addAll(result.third)
            }
            Direction.UP -> {
                val result = moveUpPreview(newGrid)
                scoreIncrease = result.first
                newAchievements.addAll(result.second)
                mergedNumbers.addAll(result.third)
            }
            Direction.DOWN -> {
                val result = moveDownPreview(newGrid)
                scoreIncrease = result.first
                newAchievements.addAll(result.second)
                mergedNumbers.addAll(result.third)
            }
        }

        newScore += scoreIncrease

        var hasChanged = false
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] != newGrid[i][j]) {
                    hasChanged = true
                    break
                }
            }
            if (hasChanged) break
        }

        return MovePreview(newGrid, newScore, hasChanged, scoreIncrease, newAchievements, mergedNumbers)
    }

    fun move(direction: Direction): Boolean {
        val oldGrid = Array(size) { i -> IntArray(size) { j -> grid[i][j] } }
        val oldScore = score

        val mergeResult = when (direction) {
            Direction.LEFT -> moveLeft()
            Direction.RIGHT -> moveRight()
            Direction.UP -> moveUp()
            Direction.DOWN -> moveDown()
        }

        val newAchievements = mergeResult.first
        val mergedNumbers = mergeResult.second

        var hasChanged = false
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] != oldGrid[i][j]) {
                    hasChanged = true
                    break
                }
            }
            if (hasChanged) break
        }

        if (hasChanged) {
            mergedNumbers.forEach { number ->
                gameListener?.onNumberMerged(number)
            }

            newAchievements.forEach { number ->
                if (!achievedNumbers.contains(number)) {
                    achievedNumbers.add(number)
                    gameListener?.onFirstTimeAchievement(number)
                }
            }

            addRandomNumber()
            saveState()
            gameListener?.onScoreChanged(score)
            gameListener?.onGridChanged()

            if (hasWon()) {
                gameListener?.onGameWon()
            } else if (isGameOver()) {
                gameListener?.onGameOver()
            }
        }

        return hasChanged
    }


    private fun moveLeftPreview(grid: Array<IntArray>): Triple<Int, Set<Int>, List<Int>> {
        var scoreIncrease = 0
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()

            var j = 0
            while (j < row.size - 1) {
                if (row[j] == row[j + 1]) {
                    val mergedValue = row[j] * 2
                    row[j] = mergedValue
                    scoreIncrease += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    row.removeAt(j + 1)
                }
                j++
            }

            while (row.size < size) {
                row.add(0)
            }

            grid[i] = row.toIntArray()
        }
        return Triple(scoreIncrease, newAchievements, mergedNumbers)
    }

    private fun moveRightPreview(grid: Array<IntArray>): Triple<Int, Set<Int>, List<Int>> {
        var scoreIncrease = 0
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()

            var j = row.size - 1
            while (j > 0) {
                if (row[j] == row[j - 1]) {
                    val mergedValue = row[j] * 2
                    row[j] = mergedValue
                    scoreIncrease += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    row.removeAt(j - 1)
                    j--
                }
                j--
            }

            val newRow = IntArray(size)
            val startIndex = size - row.size
            for (k in row.indices) {
                newRow[startIndex + k] = row[k]
            }

            grid[i] = newRow
        }
        return Triple(scoreIncrease, newAchievements, mergedNumbers)
    }

    private fun moveUpPreview(grid: Array<IntArray>): Triple<Int, Set<Int>, List<Int>> {
        var scoreIncrease = 0
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            var i = 0
            while (i < column.size - 1) {
                if (column[i] == column[i + 1]) {
                    val mergedValue = column[i] * 2
                    column[i] = mergedValue
                    scoreIncrease += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    column.removeAt(i + 1)
                }
                i++
            }

            for (k in 0 until size) {
                grid[k][j] = if (k < column.size) column[k] else 0
            }
        }
        return Triple(scoreIncrease, newAchievements, mergedNumbers)
    }

    private fun moveDownPreview(grid: Array<IntArray>): Triple<Int, Set<Int>, List<Int>> {
        var scoreIncrease = 0
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            var i = column.size - 1
            while (i > 0) {
                if (column[i] == column[i - 1]) {
                    val mergedValue = column[i] * 2
                    column[i] = mergedValue
                    scoreIncrease += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    column.removeAt(i - 1)
                    i--
                }
                i--
            }

            for (k in 0 until size) {
                val index = size - 1 - k
                grid[index][j] = if (k < column.size) column[column.size - 1 - k] else 0
            }
        }
        return Triple(scoreIncrease, newAchievements, mergedNumbers)
    }

    private fun moveLeft(): Pair<Set<Int>, List<Int>> {
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()

            var j = 0
            while (j < row.size - 1) {
                if (row[j] == row[j + 1]) {
                    val mergedValue = row[j] * 2
                    row[j] = mergedValue
                    score += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    row.removeAt(j + 1)
                }
                j++
            }

            while (row.size < size) {
                row.add(0)
            }

            grid[i] = row.toIntArray()
        }
        return Pair(newAchievements, mergedNumbers)
    }

    private fun moveRight(): Pair<Set<Int>, List<Int>> {
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (i in 0 until size) {
            val row = grid[i].filter { it != 0 }.toMutableList()

            var j = row.size - 1
            while (j > 0) {
                if (row[j] == row[j - 1]) {
                    val mergedValue = row[j] * 2
                    row[j] = mergedValue
                    score += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    row.removeAt(j - 1)
                    j--
                }
                j--
            }

            val newRow = IntArray(size)
            val startIndex = size - row.size
            for (k in row.indices) {
                newRow[startIndex + k] = row[k]
            }

            grid[i] = newRow
        }
        return Pair(newAchievements, mergedNumbers)
    }

    private fun moveUp(): Pair<Set<Int>, List<Int>> {
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            var i = 0
            while (i < column.size - 1) {
                if (column[i] == column[i + 1]) {
                    val mergedValue = column[i] * 2
                    column[i] = mergedValue
                    score += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    column.removeAt(i + 1)
                }
                i++
            }

            for (k in 0 until size) {
                grid[k][j] = if (k < column.size) column[k] else 0
            }
        }
        return Pair(newAchievements, mergedNumbers)
    }

    private fun moveDown(): Pair<Set<Int>, List<Int>> {
        val newAchievements = mutableSetOf<Int>()
        val mergedNumbers = mutableListOf<Int>()

        for (j in 0 until size) {
            val column = mutableListOf<Int>()
            for (i in 0 until size) {
                if (grid[i][j] != 0) {
                    column.add(grid[i][j])
                }
            }

            var i = column.size - 1
            while (i > 0) {
                if (column[i] == column[i - 1]) {
                    val mergedValue = column[i] * 2
                    column[i] = mergedValue
                    score += mergedValue
                    newAchievements.add(mergedValue)
                    mergedNumbers.add(mergedValue)
                    column.removeAt(i - 1)
                    i--
                }
                i--
            }

            for (k in 0 until size) {
                val index = size - 1 - k
                grid[index][j] = if (k < column.size) column[column.size - 1 - k] else 0
            }
        }
        return Pair(newAchievements, mergedNumbers)
    }

    private fun hasWon(): Boolean {
        for (i in 0 until size) {
            for (j in 0 until size) {
                if(grid[i][j] == 256 && size==3){
                    return true
                }
                if (grid[i][j] == 2048) {
                    return true
                }
            }
        }
        return false
    }

    private fun isGameOver(): Boolean {
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] == 0) return false
            }
        }

        for (i in 0 until size) {
            for (j in 0 until size) {
                val current = grid[i][j]
                if ((i > 0 && grid[i-1][j] == current) ||
                    (i < size-1 && grid[i+1][j] == current) ||
                    (j > 0 && grid[i][j-1] == current) ||
                    (j < size-1 && grid[i][j+1] == current)) {
                    return false
                }
            }
        }

        return true
    }

    fun getGrid(): Array<IntArray> = grid
    fun getScore(): Int = score
    fun getSize(): Int = size
    fun getAchievedNumbers(): Set<Int> = achievedNumbers.toSet()

    fun generatePreviewGrid(): Array<IntArray> {
        val previewGrid = Array(size) { IntArray(size) }
        val numbers = listOf(2, 4, 8, 16, 32, 64, 128, 256)

        val cellCount = (size * size * 0.3).toInt().coerceAtLeast(2)
        val positions = mutableListOf<Pair<Int, Int>>()

        for (i in 0 until size) {
            for (j in 0 until size) {
                positions.add(Pair(i, j))
            }
        }
        positions.shuffle()

        for (i in 0 until cellCount.coerceAtMost(positions.size)) {
            val pos = positions[i]
            val numberIndex = Random.nextInt(numbers.size.coerceAtMost(size))
            previewGrid[pos.first][pos.second] = numbers[numberIndex]
        }

        return previewGrid
    }
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}