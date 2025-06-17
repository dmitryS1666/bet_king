    package com.kett.bing.ui

    import android.os.CountDownTimer
    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.ViewModel

    class GameViewModel : ViewModel() {
        val board = MutableLiveData(generateBoard())
        val score = MutableLiveData(0)
        val timeLeft = MutableLiveData(30)
        private var timer: CountDownTimer? = null
        val nextTiles = MutableLiveData<List<TileType>>(generateNextTiles())
        val movesLeft = MutableLiveData(15)
        val gameEnded = MutableLiveData(false)
        val collectedTriplets = MutableLiveData<Map<TileType, Int>>(emptyMap())
        val matchCounters = MutableLiveData<Map<TileType, Int>>(emptyMap())

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
            checkMatches()

            movesLeft.value = 15
            timer = object : CountDownTimer(30_000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeft.value = (millisUntilFinished / 1000).toInt()
                }
                override fun onFinish() {
                    endGame()
                }
            }.start()
        }

        fun endGame(silent: Boolean = false) {
            stopTimer()
            timeLeft.value = 0
            movesLeft.value = 0

            if (!silent) {
                gameEnded.value = true // триггерим событие окончания
            }
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

        private fun generateDistinctTileType(exclude: Set<TileType>): TileType {
            val types = TileType.values().filterNot { it in exclude }
            return if (types.isNotEmpty()) types.random() else TileType.random()
        }

        private fun removeTiles(matched: List<Tile>) {
            val currentBoard = board.value?.map { it.toMutableList() }?.toMutableList() ?: return

            for (tile in matched) {
                val newType = consumeNextTile()
                currentBoard[tile.row][tile.col] = Tile(newType, tile.row, tile.col)
            }

            board.value = currentBoard // обновляем LiveData

            score.value = (score.value ?: 0) + matched.size * 10
        }

        fun swapAndCheck(from: Tile, to: Tile): List<Tile> {
            val currentBoard = board.value?.map { row ->
                row.map { it.copy() }.toMutableList()
            }?.toMutableList() ?: return emptyList()

            val temp = currentBoard[from.row][from.col]
            currentBoard[from.row][from.col] = currentBoard[to.row][to.col]
            currentBoard[to.row][to.col] = temp

            currentBoard[from.row][from.col].row = from.row
            currentBoard[from.row][from.col].col = from.col
            currentBoard[to.row][to.col].row = to.row
            currentBoard[to.row][to.col].col = to.col

            val tempRow = from.row
            from.row = to.row
            to.row = tempRow

            val tempCol = from.col
            from.col = to.col
            to.col = tempCol

            board.value = currentBoard

            // Проверяем совпадения и подсвечиваем
            val matches = findMatchesAndHighlight()

            // Если есть совпадения, запускаем удаление и замену
            if (matches.isNotEmpty()) {
                checkMatches()  // <--- вызов проверки и обработки совпадений
            }

            movesLeft.value = (movesLeft.value ?: 1) - 1
            if ((movesLeft.value ?: 0) <= 0) {
                endGame()
            }

            return matches
        }

        fun findMatchesAndHighlight(): List<Tile> {
            val grid = board.value ?: return emptyList()
            val matched = mutableSetOf<Tile>()

            // По строкам
            for (row in grid) {
                for (i in 0..row.size - 3) {
                    if (row[i].type == row[i+1].type && row[i].type == row[i+2].type) {
                        matched.add(row[i])
                        matched.add(row[i+1])
                        matched.add(row[i+2])
                    }
                }
            }

            // По столбцам
            for (col in 0 until grid[0].size) {
                for (row in 0..grid.size - 3) {
                    val t1 = grid[row][col]
                    val t2 = grid[row+1][col]
                    val t3 = grid[row+2][col]
                    if (t1.type == t2.type && t1.type == t3.type) {
                        matched.add(t1)
                        matched.add(t2)
                        matched.add(t3)
                    }
                }
            }

            board.value = grid.map { row -> row.map { it.copy() }.toMutableList() }

            return matched.toList()
        }

        private fun checkMatches() {
            val grid = board.value!!
            val matched = mutableSetOf<Pair<Int, Int>>()

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

                // Получаем текущие счетчики и копируем их
                val currentCounters = matchCounters.value?.toMutableMap() ?: mutableMapOf()

                for ((r, c) in matched) {
                    val tileType = grid[r][c].type
                    // Увеличиваем счетчик для этого типа
                    currentCounters[tileType] = (currentCounters[tileType] ?: 0) + 1

                    val neighbors = getNeighborTypes(newBoard, r, c)
                    val newType = generateDistinctTileType(neighbors)
                    newBoard[r][c] = Tile(newType, r, c)
                }

                matchCounters.value = currentCounters
                board.value = newBoard

                // Очки
                score.value = (score.value ?: 0) + matched.size * 10

                // Обновляем очередь
                refreshNextTiles()

                // Рекурсивно проверяем
                checkMatches()
            }
        }

        private fun getNeighborTypes(board: List<List<Tile>>, row: Int, col: Int): Set<TileType> {
            val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
            val neighbors = mutableSetOf<TileType>()

            for ((dr, dc) in directions) {
                val r = row + dr
                val c = col + dc
                if (r in board.indices && c in board[0].indices) {
                    neighbors.add(board[r][c].type)
                }
            }
            return neighbors
        }

        fun stopTimer() {
            timer?.cancel()
        }
    }
