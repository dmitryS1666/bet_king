package com.kett.bing

import android.content.Context
import android.media.MediaPlayer

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("sound_enabled", true)
        val volume = prefs.getInt("music_volume", 50) / 100f

        if (!enabled) return

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.fon_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(volume, volume)
            mediaPlayer?.start()
        }
    }

    fun setVolume(level: Int) {
        val volume = level / 100f
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}
