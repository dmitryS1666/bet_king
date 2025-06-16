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
    val movesLeft = MutableLiveData(15)

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
        movesLeft.value = 15

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

    private fun refreshNextTiles() {
        nextTiles.value = generateNextTiles()
    }

    fun swapAndCheck(first: Tile, second: Tile) {
        val currentBoard = board.value!!.map { it.toMutableList() }.toMutableList()

        // Меняем местами типы
        val tempType = first.type
        currentBoard[first.row][first.col].type = second.type
        currentBoard[second.row][second.col].type = tempType

        board.value = currentBoard

        // Уменьшаем кол-во ходов
        movesLeft.value = (movesLeft.value ?: 1) - 1

        // Проверка на конец игры по ходам
        if (movesLeft.value == 0) {
            stopTimer()
            timeLeft.value = 0 // Завершаем игру
            return
        }

        checkMatches()
    }

    private fun checkMatches() {
        val grid = board.value!!
        val matched = mutableSetOf<Pair<Int, Int>>() // Все совпавшие позиции

        // Горизонтальные совпадения
        for (row in grid.indices) {
            for (col in 0..grid[row].size - 3) {
                val t1 = grid[row][col].type
                val t2 = grid[row][col + 1].type
                val t3 = grid[row][col + 2].type
                if (t1 == t2 && t2 == t3) {
                    matched.add(Pair(row, col))
                    matched.add(Pair(row, col + 1))
                    matched.add(Pair(row, col + 2))
                }
            }
        }

        // Вертикальные совпадения
        for (col in grid[0].indices) {
            for (row in 0..grid.size - 3) {
                val t1 = grid[row][col].type
                val t2 = grid[row + 1][col].type
                val t3 = grid[row + 2][col].type
                if (t1 == t2 && t2 == t3) {
                    matched.add(Pair(row, col))
                    matched.add(Pair(row + 1, col))
                    matched.add(Pair(row + 2, col))
                }
            }
        }

        if (matched.isNotEmpty()) {
            val newBoard = grid.map { it.toMutableList() }.toMutableList()

            for ((r, c) in matched) {
                // Вместо старого тайла — новый из nextTiles
                newBoard[r][c] = Tile(consumeNextTile(), r, c)
            }

            board.value = newBoard

            // Очки: +10 за каждый тайл
            score.value = (score.value ?: 0) + matched.size * 10

            // Обновляем очередь следующих
            refreshNextTiles()

            // Проверяем рекурсивно: новые тайлы тоже могли создать матч
            checkMatches()
        }
    }

    fun stopTimer() {
        timer?.cancel()
    }
}
