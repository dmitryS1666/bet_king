package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.R

class MainFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireActivity().getSharedPreferences("UserData", MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_main, container, false)

        val startButton: Button = view.findViewById(R.id.startButton)

        // Обработчик нажатия на кнопку
        startButton.setOnClickListener {
            showGameSelectionDialog()
        }

        return view
    }

    @SuppressLint("MissingInflatedId")
    private fun showGameSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_selection, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.game3InRowButton).setOnClickListener {
            dialog.dismiss()
            println("3Game")
            // (activity as? MainActivity)?.startMatch3Game()
        }

        dialogView.findViewById<Button>(R.id.bonusGameButton).setOnClickListener {
            dialog.dismiss()
            println("bonus game")
            // (activity as? MainActivity)?.openWorkoutFragment()
        }

        dialog.show()
    }

}
