package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator

object SoundEffects {
    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        null
    }

    /**
     * Triggers a crisp, short click/beep tone on a separate thread to prevent UI blocking
     */
    fun playClick() {
        try {
            Thread {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            }.start()
        } catch (_: Exception) {}
    }

    /**
     * Triggers a high-pitched victory chime on a separate thread to indicate a win/success
     */
    fun playWin() {
        try {
            Thread {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 350)
            }.start()
        } catch (_: Exception) {}
    }
}
