package com.kett.bing.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kett.bing.MainActivity
import com.kett.bing.MusicPlayerManager
import com.kett.bing.R
import com.kett.bing.databinding.FragmentMatch3Binding

class Match3Fragment : Fragment() {
    private lateinit var binding: FragmentMatch3Binding
    private lateinit var viewModel: GameViewModel
    private var selected: Tile? = null
    private var selectedTile: Tile? = null
    private val tileViewMap = mutableMapOf<Tile, View>()
    private var gameEnded = false
    private var swapSoundPlayer: MediaPlayer? = null
    private var winSoundPlayer: MediaPlayer? = null

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

        viewModel.nextTiles.observe(viewLifecycleOwner) { nextTiles ->
            binding.nextTilesContainer.removeAllViews()
            nextTiles.forEach { tileType ->
                val imageView = ImageView(requireContext()).apply {
                    setImageResource(tileType.resId)
                    layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        marginEnd = 18
                    }
                }
                binding.nextTilesContainer.addView(imageView)
            }
        }

        viewModel.gameEnded.observe(viewLifecycleOwner) { ended ->
            if (ended && !gameEnded) {
                gameEnded = true
                showEndScreen()
            }
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
        val spacingPx = 25

        binding.gridContainer.rowCount = numRows
        binding.gridContainer.columnCount = numCols

        val existingTiles = tileViewMap.keys.toMutableSet()

        board.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, tile ->
                existingTiles.remove(tile) // Эта ячейка ещё активна

                val isSelected = tile == selectedTile
                val existingView = tileViewMap[tile]

                if (existingView != null) {
                    // Обновим выделение
                    val background = (existingView.background as GradientDrawable)
                    background.setColor(Color.parseColor(if (isSelected) "#FA8800" else "#C06A02"))
                    return@forEachIndexed
                }

                // Новая плитка
                val tileContainer = FrameLayout(requireContext()).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 270
                        height = 270
                        rowSpec = GridLayout.spec(rowIndex)
                        columnSpec = GridLayout.spec(colIndex)
                        setMargins(spacingPx, spacingPx, spacingPx, spacingPx)
                    }

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 80f
                        setColor(Color.parseColor(if (isSelected) "#FA8800" else "#C06A02"))
                        setStroke(4, Color.BLACK)
                    }

                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        handleTileClick(tile)
                    }
                }

                val imageView = ImageView(requireContext()).apply {
                    setImageResource(tile.type.resId)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                }

                tileContainer.addView(imageView)
                binding.gridContainer.addView(tileContainer)
                tileViewMap[tile] = tileContainer

                animateTileAppear(tileContainer)
            }
        }

        // Анимировать исчезновение старых тайлов
        for (oldTile in existingTiles) {
            val viewToRemove = tileViewMap[oldTile]
            if (viewToRemove != null) {
                animateTileDisappear(viewToRemove) {
                    binding.gridContainer.removeView(viewToRemove)
                    tileViewMap.remove(oldTile)
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
                view1.translationX = 0f
                view1.translationY = 0f
                view2.translationX = 0f
                view2.translationY = 0f
                onEnd()
            }
        }

        anim1.start()
        anim2.start()
    }

    private fun animateTileDisappear(view: View, onEnd: () -> Unit) {
        view.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction { onEnd() }
            .start()
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
            val view1 = tileViewMap[selectedTile]
            val view2 = tileViewMap[clickedTile]
            val fromTile = selectedTile
            val toTile = clickedTile

            selectedTile = null // сброс до анимации

            if (view1 != null && view2 != null && fromTile != null && toTile != null) {
                animateSwap(view1, view2) {
                    viewModel.swapAndCheck(fromTile, toTile)
                }
            } else {
                viewModel.swapAndCheck(fromTile!!, toTile!!)
            }
        } else {
            selectedTile = clickedTile
        }

        // Обновить UI с подсветкой
        viewModel.board.value = viewModel.board.value
    }

    private fun showEndScreen() {
        val score = viewModel.score.value ?: 0
        val intent = Intent(requireContext(), WinDialogActivity::class.java)
        winSoundPlayer?.start()

        intent.putExtra("score", score)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swapSoundPlayer?.release()
        swapSoundPlayer = null
    }
}
