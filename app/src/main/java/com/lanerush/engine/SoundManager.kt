package com.lanerush.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.lanerush.R

/**
 * SoundManager — Handles game audio effects (SoundPool) and background loops (MediaPlayer).
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundIds   = mutableMapOf<String, Int>()    // key → soundId
    private val loaded     = mutableSetOf<Int>()            // soundIds that are ready
    private val pending    = mutableMapOf<String, Boolean>() // key → pendingPlay
    
    private var musicPlayer: MediaPlayer? = null
    private var enginePlayer: MediaPlayer? = null
    
    private var isSoundEnabled = true
    private var volume = 0.7f

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loaded.add(sampleId)
                val key = soundIds.entries.firstOrNull { it.value == sampleId }?.key
                if (key != null && pending.remove(key) == true) {
                    if (isSoundEnabled) {
                        soundPool.play(sampleId, volume, volume, 0, 0, 1f)
                    }
                }
            } else {
                Log.e("SoundManager", "Failed to load sound $sampleId, status $status")
            }
        }

        // ── Load sound effects ──────────────────────────────────────────
        try {
            soundIds["click"]   = soundPool.load(context, R.raw.click, 1)
            soundIds["crash"]   = soundPool.load(context, R.raw.crash, 1)
            soundIds["victory"] = soundPool.load(context, R.raw.victory, 1)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error loading sounds", e)
        }

        // ── Initialize Music & Engine Loops (MediaPlayer) ──────────────
        try {
            musicPlayer = MediaPlayer.create(context, R.raw.lane_rush_theme_song).apply {
                isLooping = true
                setVolume(volume * 0.5f, volume * 0.5f)
            }
            enginePlayer = MediaPlayer.create(context, R.raw.engine_loop).apply {
                isLooping = true
                setVolume(volume * 0.4f, volume * 0.4f)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error creating media players", e)
        }
    }

    fun updateSettings(enabled: Boolean, newVolume: Float) {
        this.isSoundEnabled = enabled
        this.volume = newVolume
        
        try {
            val musicVol = volume * 0.5f
            val engineVol = volume * 0.4f
            musicPlayer?.setVolume(musicVol, musicVol)
            enginePlayer?.setVolume(engineVol, engineVol)

            if (!enabled) {
                if (musicPlayer?.isPlaying == true) musicPlayer?.pause()
                if (enginePlayer?.isPlaying == true) enginePlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error updating volumes", e)
        }
    }

    fun playClick()   = play("click")
    fun playCrash()   = play("crash")
    fun playVictory() = play("victory")
    
    fun playMusic() {
        if (!isSoundEnabled) return
        try {
            if (musicPlayer?.isPlaying == false) {
                musicPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing music", e)
        }
    }

    fun stopMusic() {
        try {
            if (musicPlayer?.isPlaying == true) {
                musicPlayer?.pause()
                musicPlayer?.seekTo(0)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error stopping music", e)
        }
    }

    fun playEngine() {
        if (!isSoundEnabled) return
        try {
            if (enginePlayer?.isPlaying == false) {
                enginePlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing engine", e)
        }
    }

    fun stopEngine() {
        try {
            if (enginePlayer?.isPlaying == true) {
                enginePlayer?.pause()
                enginePlayer?.seekTo(0)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error stopping engine", e)
        }
    }

    private fun play(key: String) {
        if (!isSoundEnabled) return
        val id = soundIds[key] ?: return
        if (id in loaded) {
            soundPool.play(id, volume, volume, 0, 0, 1f)
        } else {
            pending[key] = true
        }
    }

    fun release() {
        try {
            musicPlayer?.stop()
            musicPlayer?.release()
            musicPlayer = null
            
            enginePlayer?.stop()
            enginePlayer?.release()
            enginePlayer = null
            
            soundPool.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing sound resources", e)
        }
    }
}
