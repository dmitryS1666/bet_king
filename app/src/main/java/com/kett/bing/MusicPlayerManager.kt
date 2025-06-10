package com.kett.bing

import android.content.Context
import android.media.MediaPlayer

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("sound_enabled", true)

        if (!enabled) return

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.fon_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
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
