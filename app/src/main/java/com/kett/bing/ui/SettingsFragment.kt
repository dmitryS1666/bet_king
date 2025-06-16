package com.kett.bing.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kett.bing.BannerWebActivity
import com.kett.bing.MainActivity
import com.kett.bing.MusicPlayerManager
import com.kett.bing.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val musicSeekBar = view.findViewById<SeekBar>(R.id.musicVolume)
        val soundSeekBar = view.findViewById<SeekBar>(R.id.soundVolume)
        val vibrationSwitch = view.findViewById<Switch>(R.id.vibrationSwitch)
        val webButton = view.findViewById<LinearLayout>(R.id.btnPrivacy)

        // Настройка текущих значений (можно расширить)
        musicSeekBar.progress = prefs.getInt("music_volume", 50)
        soundSeekBar.progress = prefs.getInt("sound_volume", 50)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)

        musicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("music_volume", progress).apply()
                MusicPlayerManager.setVolume(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        soundSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("sound_volume", progress).apply()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        webButton.setOnClickListener {
            val url = "https://bedkingthegame.com/policy"
            val intent = Intent(requireContext(), BannerWebActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.deleteScoreButton).setOnClickListener {
            // Очистка результата
            val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("game_score") // замените на нужный ключ, если другой
                .apply()

            // Показываем Toast
            Toast.makeText(requireContext(), "Game results cleared", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            (activity as? MainActivity)?.openMainFragment()
        }
    }

    override fun onStart() {
        super.onStart()
//        dialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
