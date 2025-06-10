package com.kett.bing.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import com.kett.bing.BannerWebActivity
import com.kett.bing.MusicPlayerManager
import com.kett.bing.R

class SettingsFragment : DialogFragment() {

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
        val webButton = view.findViewById<Button>(R.id.btnPrivacy)

        // Настройка текущих значений (можно расширить)
        musicSeekBar.progress = prefs.getInt("music_volume", 50)
        soundSeekBar.progress = prefs.getInt("sound_volume", 50)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)

        musicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("music_volume", progress).apply()
                // можно управлять громкостью через AudioManager
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
            dismiss()
            val url = "https://bedkingthegame.com/policy"
            val intent = Intent(requireContext(), BannerWebActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
        }

        view.findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
