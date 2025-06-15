package com.kett.bing.ui

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    val board = MutableLiveData(generateBoard())
    val score = MutableLiveData(0)
    val timeLeft = MutableLiveData(120)
    private var timer: CountDownTimer? = null
    val nextTiles = MutableLiveData<List<TileType>>(generateNextTiles())

    private fun generateBoard(): List<List<Tile>> {
        val size = 3
        val board = MutableList(size) { MutableList(size) { Tile(TileType.random(), 0, 0) } }

        for (row in 0 until size) {
            for (col in 0 until size) {
                var newTile: TileType
                do {
                    newTile = TileType.random()
                } while (
                    (col >= 2 && board[row][col - 1].type == newTile && board[row][col - 2].type == newTile) ||
                    (row >= 2 && board[row - 1][col].type == newTile && board[row - 2][col].type == newTile)
                )
                board[row][col] = Tile(newTile, row, col)
            }
        }
        return board
    }

    fun startGame() {
        timer = object : CountDownTimer(120_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                timeLeft.value = 0
            }
        }.start()
    }

    private fun generateNextTiles(count: Int = 3): List<TileType> =
        List(count) { TileType.random() }

    // Метод, который достает следующий тайл из очереди и обновляет список
    fun consumeNextTile(): TileType {
        val current = nextTiles.value!!.toMutableList()
        val next = TileType.random()
        val tileType = current.removeAt(0)
        current.add(next)
        nextTiles.value = current  // вот тут LiveData обновляется
        return tileType
    }

    fun swapAndCheck(first: Tile, second: Tile) {
        val currentBoard = board.value!!.map { it.toMutableList() }.toMutableList()

        // Меняем местами типы
        val tempType = first.type
        currentBoard[first.row][first.col].type = second.type
        currentBoard[second.row][second.col].type = tempType

        board.value = currentBoard

        checkMatches()
    }

    private fun checkMatches() {
        val grid = board.value!!
        val rowsToRemove = mutableSetOf<Int>()
        val colsToRemove = mutableSetOf<Int>()
        val matched = mutableSetOf<Pair<Int, Int>>()

        // Проверяем строки
        for (row in grid.indices) {
            if (grid[row][0].type == grid[row][1].type && grid[row][1].type == grid[row][2].type) {
                rowsToRemove.add(row)
            }
        }

        // Проверяем столбцы
        for (col in 0 until grid[0].size) {
            if (grid[0][col].type == grid[1][col].type && grid[1][col].type == grid[2][col].type) {
                colsToRemove.add(col)
            }
        }

        if (rowsToRemove.isNotEmpty() || colsToRemove.isNotEmpty()) {
            val newBoard = grid.map { it.toMutableList() }.toMutableList()
            // Обновляем строки
            for (row in rowsToRemove) {
                for (col in newBoard[row].indices) {
                    newBoard[row][col] = Tile(TileType.random(), row, col)
                }
            }

            // Обновляем столбцы
            for (col in colsToRemove) {
                for (row in newBoard.indices) {
                    newBoard[row][col] = Tile(TileType.random(), row, col)
                }
            }

            board.value = newBoard

            // Начисляем очки: 100 за каждую удалённую строку и столбец
            score.value = (score.value ?: 0) + (rowsToRemove.size + colsToRemove.size) * 100

            // Проверяем заново после обновления доски
            // Можно вызвать с небольшой задержкой, если нужен визуальный эффект
            if (matched.isNotEmpty()) {
                score.value = score.value?.plus(matched.size * 10)
                val gridCopy = grid.map { it.toMutableList() }.toMutableList()
                for ((r, c) in matched) {
                    gridCopy[r][c] = Tile(consumeNextTile(), r, c)  // <-- вот здесь!
                }
                board.value = gridCopy

                // Рекурсивный вызов, если нужно
                checkMatches()
            }
        }
    }

    fun stopTimer() {
        timer?.cancel()
    }
}
