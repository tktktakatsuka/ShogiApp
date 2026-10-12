package com.tktkcompany.shogi.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tktkcompany.shogi.R

/** 駒を置く「パチッ」という効果音。 */
class SoundPlayer(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val komaSound = pool.load(context, R.raw.koma, 1)

    fun playKoma() {
        pool.play(komaSound, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
