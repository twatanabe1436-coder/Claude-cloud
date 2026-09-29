package io.github.twatanabe1436.biyotimer.runtime

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class Beep {
    /** カウントダウンの「ピッ」 */
    TICK,

    /** 開始の「ピー」(開始の読み上げがないとき) */
    START,

    /** 残り時間の「ピピッ」(日本語音声が使えないとき) */
    ANNOUNCE,

    /** 終了の「ピッピッピー」(終了の読み上げがないとき) */
    FINISH,
}

/** 合図のビープ音を正弦波から作って鳴らす。音量はメディア音量に従う。 */
class BeepPlayer {

    private val tracks = mutableMapOf<Beep, AudioTrack>()

    fun play(beep: Beep) {
        try {
            val track = tracks.getOrPut(beep) { createTrack(pattern(beep)) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        } catch (e: Exception) {
            // 音声出力が無い環境 (エミュレータ等) でもタイマー自体は止めない
            Log.w(TAG, "failed to play $beep", e)
        }
    }

    fun stop() {
        tracks.values.forEach { track ->
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "failed to stop beep", e)
            }
        }
    }

    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }

    /** (周波数 Hz, 長さ ms) の並び。周波数 0 は無音。 */
    private fun pattern(beep: Beep): List<Pair<Int, Int>> = when (beep) {
        Beep.TICK -> listOf(880 to 120)
        Beep.START -> listOf(1320 to 700)
        Beep.ANNOUNCE -> listOf(880 to 150, 0 to 120, 880 to 150)
        Beep.FINISH -> listOf(1320 to 250, 0 to 150, 1320 to 250, 0 to 150, 1320 to 700)
    }

    private fun createTrack(segments: List<Pair<Int, Int>>): AudioTrack {
        val pcm = synthesize(segments)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun synthesize(segments: List<Pair<Int, Int>>): ShortArray {
        val total = segments.sumOf { it.second } * SAMPLE_RATE / 1000
        val out = ShortArray(total)
        var offset = 0
        for ((freq, ms) in segments) {
            val n = ms * SAMPLE_RATE / 1000
            if (freq > 0) {
                val fade = min(FADE_SAMPLES, n / 2)
                for (i in 0 until n) {
                    // 端を短くフェードさせてプチッというノイズを防ぐ
                    val envelope = when {
                        i < fade -> i.toDouble() / fade
                        i >= n - fade -> (n - 1 - i).toDouble() / fade
                        else -> 1.0
                    }
                    val v = sin(2 * PI * freq * i / SAMPLE_RATE) * envelope * AMPLITUDE
                    out[offset + i] = (v * Short.MAX_VALUE).toInt().toShort()
                }
            }
            offset += n
        }
        return out
    }

    private companion object {
        const val TAG = "BeepPlayer"
        const val SAMPLE_RATE = 44_100
        const val FADE_SAMPLES = SAMPLE_RATE / 200 // 5ms
        const val AMPLITUDE = 0.7
    }
}
