package com.kett.bing.ui

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kett.bing.databinding.FragmentMatch3Binding

class Match3Fragment : Fragment() {
    private lateinit var binding: FragmentMatch3Binding
    private lateinit var viewModel: GameViewModel
    private var selected: Tile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMatch3Binding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        viewModel.startGame()
        observeGame()
        return binding.root
    }

    private fun observeGame() {
        viewModel.board.observe(viewLifecycleOwner) { updateGrid(it) }
        viewModel.score.observe(viewLifecycleOwner) { binding.pointsText.text = "Points: $it" }
        viewModel.timeLeft.observe(viewLifecycleOwner) {
            binding.timeText.text = "Time: $it"
            if (it == 0) showEndScreen()
        }
        viewModel.nextTiles.observe(viewLifecycleOwner) { nextTiles ->
            binding.nextTilesContainer.removeAllViews()
            nextTiles.forEach { tileType ->
                val imageView = ImageView(requireContext()).apply {
                    setImageResource(tileType.resId)
                    layoutParams = LinearLayout.LayoutParams(150, 150).apply {
                        marginEnd = 8
                    }
                }
                binding.nextTilesContainer.addView(imageView)
            }
        }
    }

    private fun updateGrid(board: List<List<Tile>>) {
        binding.gridContainer.removeAllViews()
        binding.gridContainer.rowCount = board.size
        binding.gridContainer.columnCount = board[0].size

        board.flatten().forEach { tile ->
            val imageView = ImageView(requireContext()).apply {
                setImageResource(tile.type.resId)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 200
                    height = 200
                    bottomMargin = 8
                    rightMargin = 8
                }

                tag = tile

                // Старт drag
                setOnTouchListener { v, event ->
                    val data = ClipData.newPlainText("", "${tile.row},${tile.col}")
                    val shadowBuilder = View.DragShadowBuilder(v)
                    v.startDragAndDrop(data, shadowBuilder, v, 0)
                    true
                }

                // Обработка drop
                setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> true
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            v.alpha = 0.5f
                            true
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            v.alpha = 1f
                            true
                        }
                        DragEvent.ACTION_DROP -> {
                            v.alpha = 1f
                            val srcCoords = event.clipData.getItemAt(0).text.toString()
                            val (srcRow, srcCol) = srcCoords.split(",").map { it.toInt() }
                            val sourceTile = viewModel.board.value!![srcRow][srcCol]
                            val targetTile = v.tag as Tile

                            val rowDiff = kotlin.math.abs(srcRow - targetTile.row)
                            val colDiff = kotlin.math.abs(srcCol - targetTile.col)

                            Log.d("DRAG_DROP", "Drop from $srcRow,$srcCol to ${targetTile.row},${targetTile.col}")

                            // Проверяем, что ячейки соседние по вертикали или горизонтали, но не по диагонали
                            if ((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)) {
                                viewModel.swapAndCheck(sourceTile, targetTile)
                            } else {
                                Log.d("DRAG_DROP", "Перемещение запрещено: ячейки не соседние")
                            }
                            true
                        }

                        DragEvent.ACTION_DRAG_ENDED -> {
                            v.alpha = 1f
                            true
                        }
                        else -> false
                    }
                }
            }

            binding.gridContainer.addView(imageView)
        }
    }

    private fun handleClick(tile: Tile) {
        if (selected == null) {
            selected = tile
        } else {
            viewModel.swapAndCheck(selected!!, tile)
            selected = null
        }
    }

    private fun showEndScreen() {
        val score = viewModel.score.value ?: 0
        val intent = Intent(requireContext(), WinDialogActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
    }
}
