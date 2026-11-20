package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundEffects {
    @Volatile
    var enabled: Boolean = true

    private val toneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    } catch (e: Exception) {
        Log.w("SoundEffects", "ToneGenerator unavailable", e)
        null
    }

    fun playClick() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (_: Exception) {}
    }

    fun playCashSuccess() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 140)
        } catch (_: Exception) {}
    }

    fun playFraudAlert() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        } catch (_: Exception) {}
    }

    fun playUpgradeFanfare() {
        if (!enabled) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (_: Exception) {}
    }
}
