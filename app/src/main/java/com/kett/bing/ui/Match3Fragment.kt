package com.kett.bing.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kett.bing.MainActivity
import com.kett.bing.R
import com.kett.bing.databinding.FragmentMatch3Binding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Match3Fragment : Fragment() {
    private lateinit var binding: FragmentMatch3Binding
    private lateinit var viewModel: GameViewModel
    private var selected: Tile? = null
    private var selectedTile: Tile? = null
    private val tileViewMap = mutableMapOf<Pair<Int, Int>, View>()
    private var gameEnded = false
    private var swapSoundPlayer: MediaPlayer? = null
    private var winSoundPlayer: MediaPlayer? = null
    private var loseSoundPlayer: MediaPlayer? = null
    private var highlightedTiles = emptySet<Pair<Int, Int>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMatch3Binding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        viewModel.startGame()
        observeGame()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swapSoundPlayer = MediaPlayer.create(requireContext(), R.raw.migrate_sound)
        winSoundPlayer = MediaPlayer.create(requireContext(), R.raw.win_sound)
        loseSoundPlayer = MediaPlayer.create(requireContext(), R.raw.fail_sound)

        // Кнопка "Домой"
        binding.homeButton.setOnClickListener {
            goToMainMenu()
        }

        // Кнопка "Настройки"
        binding.settingsButton.setOnClickListener {
            viewModel.endGame(true)
            (activity as? MainActivity)?.openFragment(SettingsFragment())
        }

        // Обработка кнопки "Назад" — сразу уходим на главный экран
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToMainMenu()
            }
        })
    }

    private fun goToMainMenu() {
        viewModel.endGame(silent = true) // не показываем экран окончания
        (activity as? MainActivity)?.openMainFragment()
    }

    private fun observeGame() {
        viewModel.board.observe(viewLifecycleOwner) { updateGrid(it) }
        viewModel.score.observe(viewLifecycleOwner) { binding.pointsText.text = "POINTS: $it" }

        viewModel.timeLeft.observe(viewLifecycleOwner) {
            binding.timeText.text = "TIME:\n$it"
        }

        viewModel.movesLeft.observe(viewLifecycleOwner) {
            binding.movesText.text = "$it"
        }

        viewModel.matchCounters.observe(viewLifecycleOwner) { counters ->
            updateMatchCounters(counters)
        }

        viewModel.gameEnded.observe(viewLifecycleOwner) { ended ->
            if (ended && !gameEnded) {
                gameEnded = true
                showEndScreen()
            }
        }
    }

    private fun updateMatchCounters(counters: Map<TileType, Int>) {
        Log.d("Match3Fragment", "updateMatchCounters called with: $counters")
        binding.nextTilesContainer.removeAllViews()

        TileType.values().forEach { tileType ->
            val container = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, // ширина 0, чтобы вес работал
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    weight = 1f  // равный вес для равномерного распределения
                    marginEnd = 18
                }
            }

            val imageView = ImageView(requireContext()).apply {
                setImageResource(tileType.resId)
                layoutParams = FrameLayout.LayoutParams(250, 250, Gravity.CENTER)
            }

            val rawCount = counters[tileType] ?: 0
            val tripletsCount = rawCount / 3
            val sizeInDp = 35
            val scale = resources.displayMetrics.density
            val sizeInPx = (sizeInDp * scale + 0.5f).toInt()

            val counterText = TextView(requireContext()).apply {
                text = tripletsCount.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#FA8800"))
                textSize = 22f

                layoutParams = FrameLayout.LayoutParams(
                    sizeInPx,
                    sizeInPx,
                    Gravity.END or Gravity.BOTTOM
                )

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL  // Круглая форма
                    setColor(Color.parseColor("#0E2E49"))
                    setStroke(3, Color.parseColor("#FA8800"))
                }

                // Смещаем визуально за границы на 20 пикселей вправо и вниз
                translationX = 0f
                translationY = 0f
            }

            container.addView(imageView)
            container.addView(counterText)
            binding.nextTilesContainer.addView(container)
        }
    }

    private fun isNeighbor(tile1: Tile, tile2: Tile?): Boolean {
        if (tile2 == null) return false
        val dr = kotlin.math.abs(tile1.row - tile2.row)
        val dc = kotlin.math.abs(tile1.col - tile2.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    private fun updateGrid(board: List<List<Tile>>) {
        val numRows = board.size
        val numCols = board[0].size
        val spacingPx = 30

        binding.gridContainer.rowCount = numRows
        binding.gridContainer.columnCount = numCols

        binding.gridContainer.post {
            val totalWidth = binding.gridContainer.width
            val totalHeight = binding.gridContainer.height

            val availableWidth = totalWidth - spacingPx * (numCols + 1)
            val availableHeight = totalHeight - spacingPx * (numRows + 1)
            val tileSize = minOf(availableWidth / numCols, availableHeight / numRows)

            // Пройдем по всем позициям
            for (rowIndex in 0 until numRows) {
                for (colIndex in 0 until numCols) {
                    val tile = board[rowIndex][colIndex]
                    val pos = Pair(rowIndex, colIndex)
                    val existingView = tileViewMap[pos]

                    if (existingView != null) {
                        // Обновим изображение, если тайл изменился
                        val imageView = existingView.findViewById<ImageView>(R.id.tile_image)
                        if (imageView.tag != tile.type) {
                            imageView.setImageResource(tile.type.resId)
                            imageView.tag = tile.type
                            // Можно добавить анимацию плавного изменения, если нужно
                        }

                        // Обновим выделение (фон)
                        val background = (existingView.background as? GradientDrawable)
                        val isSelected = tile == selectedTile
                        background?.setColor(Color.parseColor(if (isSelected) "#FA8800" else "#C06A02"))
                    } else {
                        // Создаем новый tileContainer
                        val tileContainer = FrameLayout(requireContext()).apply {
                            layoutParams = GridLayout.LayoutParams().apply {
                                width = tileSize
                                height = tileSize
                                rowSpec = GridLayout.spec(rowIndex)
                                columnSpec = GridLayout.spec(colIndex)
                                setMargins(spacingPx / 3)
                            }
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = tileSize / 5f
                                setColor(Color.parseColor("#C06A02"))
                                setStroke(4, Color.BLACK)
                            }
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                handleTileClick(tile)
                            }
                            id = View.generateViewId()
                        }

                        val imageView = ImageView(requireContext()).apply {
                            id = R.id.tile_image // обязательно, чтобы найти потом
                            setImageResource(tile.type.resId)
                            tag = tile.type
                            layoutParams = FrameLayout.LayoutParams(
                                (tileSize * 0.7).toInt(),
                                (tileSize * 0.7).toInt(),
                                Gravity.CENTER
                            )
                        }

                        tileContainer.addView(imageView)
                        binding.gridContainer.addView(tileContainer)
                        tileViewMap[pos] = tileContainer

                        animateTileAppear(tileContainer)
                    }
                }
            }

            // Удалим views, которых больше нет в board (например, если размер изменился)
            val currentPositions = (0 until numRows).flatMap { r -> (0 until numCols).map { c -> Pair(r, c) } }.toSet()
            val toRemove = tileViewMap.keys.filter { it !in currentPositions }
            toRemove.forEach { pos ->
                val view = tileViewMap.remove(pos)
                if (view != null) {
                    binding.gridContainer.removeView(view)
                }
            }
        }
    }

    private fun animateSwap(view1: View, view2: View, onEnd: (() -> Unit)? = null) {
        val dx = view2.x - view1.x
        val dy = view2.y - view1.y

        val anim1 = view1.animate().translationXBy(dx).translationYBy(dy).setDuration(200)
        val anim2 = view2.animate().translationXBy(-dx).translationYBy(-dy).setDuration(200)

        swapSoundPlayer?.start()

        if (onEnd != null) {
            anim2.withEndAction {
                onEnd()
            }
        }

        anim1.start()
        anim2.start()
    }

    private fun animateTileAppear(view: View) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun handleTileClick(clickedTile: Tile) {
        if (selectedTile == null) {
            selectedTile = clickedTile
        } else if (selectedTile == clickedTile) {
            selectedTile = null // отмена выделения
        } else if (isNeighbor(selectedTile!!, clickedTile)) {
            // Получаем позиции выбранных тайлов
            val fromPos = Pair(selectedTile!!.row, selectedTile!!.col)
            val toPos = Pair(clickedTile.row, clickedTile.col)

            // Получаем view по позициям
            val view1 = tileViewMap[fromPos]
            val view2 = tileViewMap[toPos]

            val fromTile = selectedTile
            val toTile = clickedTile

            selectedTile = null // сброс до анимации

            if (view1 != null && view2 != null && fromTile != null && toTile != null) {
                animateSwap(view1, view2) {
                    view1.postDelayed({
                        val matchedTiles = viewModel.swapAndCheck(fromTile, toTile)

                        if (matchedTiles.isNotEmpty()) {
                            highlightedTiles = matchedTiles.map { Pair(it.row, it.col) }.toSet()
                        } else {
                            highlightedTiles = emptySet()
                        }

                        updateTileHighlighting()
                        updateTileViewMapAfterSwap(fromPos, toPos)
                    }, 500)
                }
            } else {
                selectedTile = clickedTile
            }
        } else {
            selectedTile = clickedTile
        }

        updateTileHighlighting()
    }

    private fun updateTileViewMapAfterSwap(fromPos: Pair<Int, Int>, toPos: Pair<Int, Int>) {
        val viewFrom = tileViewMap.remove(fromPos)
        val viewTo = tileViewMap.remove(toPos)

        if (viewFrom != null && viewTo != null) {
            tileViewMap[fromPos] = viewTo
            tileViewMap[toPos] = viewFrom
        }

        // ⚠️ Обновляем selectedTile, если она есть
        selectedTile?.let {
            if (it.row == fromPos.first && it.col == fromPos.second) {
                it.row = toPos.first
                it.col = toPos.second
            } else if (it.row == toPos.first && it.col == toPos.second) {
                it.row = fromPos.first
                it.col = fromPos.second
            }
        }
    }

    private fun updateTileHighlighting() {
        val selectedPos = selectedTile?.let { Pair(it.row, it.col) }

        tileViewMap.forEach { (pos, view) ->
            val background = (view.background as? GradientDrawable) ?: return@forEach
            val isSelected = pos == selectedPos
            val isMatched = highlightedTiles.contains(pos)

            when {
                isSelected -> background.setColor(Color.parseColor("#FA8800")) // оранжевая для выделения
                else -> background.setColor(Color.parseColor("#C06A02"))
            }
        }
    }

    private fun showEndScreen() {
        val score = viewModel.score.value ?: 0
        val intent: Intent

        if (score > 0) {
            intent = Intent(requireContext(), WinDialogActivity::class.java)
            winSoundPlayer?.start()

            // Сохраняем последнее время победы
            addGameResultToList("winTimes")

        } else {
            intent = Intent(requireContext(), LoseDialogActivity::class.java)
            loseSoundPlayer?.start()

            // Сохраняем последнее время поражения
            addGameResultToList("loseTimes")
        }

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val totalScore = prefs.getInt("totalScore", 0) + score

        prefs.edit()
            .putBoolean("isMainGameWon", true)
            .putInt("lastGameScore", score)
            .putInt("totalScore", totalScore)
            .apply()

        intent.putExtra("score", score)
        startActivity(intent)
    }

    private fun addGameResultToList(key: String) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        existing.add(timestamp)

        // Можно ограничить размер (например, только 50 последних)
        val trimmed = existing.sortedDescending().take(50).toMutableSet()

        prefs.edit()
            .putStringSet(key, trimmed)
            .apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swapSoundPlayer?.release()
        swapSoundPlayer = null
    }
}
