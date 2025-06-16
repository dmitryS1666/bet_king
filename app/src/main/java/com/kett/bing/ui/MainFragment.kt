package com.kett.bing.ui

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.MusicPlayerManager
import com.kett.bing.R

class MainFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireActivity().getSharedPreferences("UserData", MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val settingsButton: Button = view.findViewById(R.id.settingsButton)
        val startButton: Button = view.findViewById(R.id.startButton)

        settingsButton.setOnClickListener {
            (activity as? MainActivity)?.openFragment(SettingsFragment())
        }

        // Обработчик нажатия на кнопку
        startButton.setOnClickListener {
            showGameSelectionDialog()
        }

        return view
    }

    @SuppressLint("MissingInflatedId")
    private fun showWinDialog(score: Int) {
        val intent = Intent(requireContext(), WinDialogActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
    }

    @SuppressLint("MissingInflatedId")
    private fun showLoseDialog() {
        val intent = Intent(requireContext(), LoseDialogActivity::class.java)
        startActivity(intent)
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, Match3Fragment())
                .addToBackStack(null)
                .commit()
        }

        dialogView.findViewById<Button>(R.id.bonusGameButton).setOnClickListener {
            dialog.dismiss()
            (activity as? MainActivity)?.openMinerFragment()
        }

        dialog.show()
    }
}
