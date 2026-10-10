package io.github.twatanabe1436.hanaso.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import io.github.twatanabe1436.hanaso.core.Transcript

/** 英語の音声認識。テストでは偽物に差し替える。コールバックはメインスレッドで呼ばれる。 */
interface SpeechInput {
    /** この端末で音声認識が使えるか (Google アプリなどの音声認識サービスが必要) */
    val available: Boolean

    interface Callback {
        /** 認識途中のテキスト */
        fun onPartial(text: String) {}

        /** 入力音量 (dB)。マイクボタンの動きに使う */
        fun onLevel(rmsDb: Float) {}

        fun onError(messageJa: String) {}

        /** 認識が終わった。聞き取れなければ空文字 */
        fun onEnd(text: String)

        /** cancel() や、別の場所で認識が始まったために中断された */
        fun onCancel() {}
    }

    /**
     * 認識を始める。すでに動いているものは中断される (onCancel)。
     *
     * @param untilStopped true なら、途中で黙っても終わらずに聞き続け、[stop] が呼ばれたときに
     *   それまでの発話をまとめて返す。false なら、話し終わり (無音) で自動的に終わる。
     * @param hints そのお題で言いそうな英文。認識エンジンに伝え (Android 13 以降)、認識の候補が
     *   いくつかあるときは、これに合うものを選ぶ ([Transcript.pick])
     */
    fun start(silenceMs: Long, untilStopped: Boolean, callback: Callback, hints: List<String> = emptyList())

    /** 話し終わったことにして、ここまでの結果で終える */
    fun stop()

    /** 結果を出さずに止める */
    fun cancel()
}

class AndroidSpeechInput(context: Context) : SpeechInput {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var active: Session? = null

    override val available: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(silenceMs: Long, untilStopped: Boolean, callback: SpeechInput.Callback, hints: List<String>) {
        cancel()
        val session = Session(callback, silenceMs, untilStopped, hints)
        active = session
        session.begin()
    }

    override fun stop() {
        active?.stop()
    }

    override fun cancel() {
        active?.cancel()
    }

    private fun now() = SystemClock.elapsedRealtime()

    /**
     * 1回の音声入力。Android の音声認識は無音で自動的に区切られるので、untilStopped のときは
     * 区切られるたびに認識をやり直して、発話をつなげていく。
     * 区切りごとに新しい SpeechRecognizer を使い、前の区切りの遅れたコールバックは無視する。
     */
    private inner class Session(
        val callback: SpeechInput.Callback,
        val silenceMs: Long,
        val untilStopped: Boolean,
        val hints: List<String>,
    ) {
        private var recognizer: SpeechRecognizer? = null

        /** 確定した区切りの文 */
        private val segments = mutableListOf<String>()

        /** 認識中の区切りの途中経過 */
        private var partial = ""
        private var finished = false
        private var stopRequested = false
        private var lastHeardAt = now()
        private var busyRetries = 0

        /** ここまでに聞き取った文 (区切りをつなげたもの) */
        private fun text() = (segments + partial).filter { it.isNotBlank() }.joinToString(" ").trim()

        fun begin() {
            if (finished) return
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = r
            partial = ""
            r.setRecognitionListener(Listener(r))
            try {
                r.startListening(intent())
            } catch (e: RuntimeException) {
                callback.onError("音声認識を開始できませんでした。もう一度お試しください。")
                finish(if (untilStopped) text().ifEmpty { null } else null)
            }
        }

        fun stop() {
            if (finished) return
            stopRequested = true
            val r = recognizer
            if (r == null) {
                // 区切りと区切りの間 (やり直す直前) なら、ここまでの文で終える
                finish(text())
                return
            }
            r.stopListening()
            // 結果を返さない認識エンジンもあるので、少し待っても来なければ、ここまでの文で終える
            main.postDelayed({ finish(text()) }, STOP_TIMEOUT_MS)
        }

        fun cancel() {
            if (finished) return
            recognizer?.cancel()
            finish(null)
        }

        private inner class Listener(val r: SpeechRecognizer) : RecognitionListener {
            /** いま動いている区切りのコールバックか (古い区切りや終了後のものは無視する) */
            private val current: Boolean get() = !finished && recognizer === r

            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                if (current) lastHeardAt = now()
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (current) callback.onLevel(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank() && current) {
                    partial = text
                    lastHeardAt = now()
                    callback.onPartial(text())
                }
            }

            override fun onResults(results: Bundle?) {
                if (!current) return
                val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }
                segmentEnded(if (alternatives.isEmpty()) partial else Transcript.pick(alternatives, hints))
            }

            override fun onError(error: Int) {
                if (!current) return
                when (error) {
                    // 何も聞こえなかった / 聞き取れなかった / stop の後に来ることがある
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT ->
                        segmentEnded(partial)
                    // やり直しが早すぎると「使用中」になることがあるので、少し待ってもう一度
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (untilStopped && !stopRequested && busyRetries < 3) {
                        busyRetries++
                        release()
                        restartLater(BUSY_RETRY_MS)
                    } else {
                        fail(error)
                    }
                    else -> fail(error)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        /** 1つの区切りが終わった: 聞き続けるなら次の区切りへ、そうでなければ結果を返す */
        private fun segmentEnded(said: String) {
            if (said.isNotBlank()) {
                segments += said.trim()
                lastHeardAt = now()
                busyRetries = 0
            }
            partial = ""
            release()
            when {
                !untilStopped || stopRequested -> finish(text())
                // 長いあいだ何も話さなければ、聞き続けるのをやめる (区切りのたびにやり直し続けないように)
                now() - lastHeardAt > MAX_IDLE_MS -> finish(text())
                else -> restartLater(RESTART_DELAY_MS)
            }
        }

        private fun restartLater(delayMs: Long) {
            main.postDelayed({ if (!finished && !stopRequested) begin() }, delayMs)
        }

        private fun fail(error: Int) {
            if (finished) return
            callback.onError(errorMessage(error))
            // 聞き続けていた場合は、それまでに聞き取れた分を残す
            finish(if (untilStopped) text().ifEmpty { null } else null)
        }

        private fun release() {
            val r = recognizer ?: return
            recognizer = null
            // コールバックの中で destroy しないよう、少し後で片付ける
            main.post { r.destroy() }
        }

        private fun finish(result: String?) {
            if (finished) return
            finished = true
            release()
            if (active === this) active = null
            if (result == null) callback.onCancel() else callback.onEnd(result.trim())
        }

        private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // お題があれば候補を複数もらって、お題に合うものを選ぶ
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, if (hints.isEmpty()) 1 else MAX_ALTERNATIVES)
            if (hints.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(hints.take(MAX_BIASING_STRINGS)))
            }
            // 話し終わりと判断するまでの無音時間。聞き続けるときは区切りが少なくなるよう長めにする
            // (対応していない認識エンジンもある。その場合も区切りのたびにやり直すので聞き続けられる)
            val silence = if (untilStopped) maxOf(silenceMs, CONTINUOUS_SILENCE_MS) else silenceMs
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silence)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silence)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private companion object {
        const val CONTINUOUS_SILENCE_MS = 6_000L
        const val RESTART_DELAY_MS = 150L
        const val BUSY_RETRY_MS = 500L
        const val STOP_TIMEOUT_MS = 2_500L
        const val MAX_ALTERNATIVES = 5
        const val MAX_BIASING_STRINGS = 40

        /** 聞き続けるモードで、何も話さないまま待つ最大時間 */
        const val MAX_IDLE_MS = 60_000L
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイクの使用が許可されていません。端末の設定でマイクを許可してください。"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "音声認識サービスに接続できませんでした。通信状況を確認してください。"
        SpeechRecognizer.ERROR_AUDIO -> "マイクの音声を取得できませんでした。"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識が使用中です。少し待ってからもう一度お試しください。"
        SpeechRecognizer.ERROR_SERVER -> "音声認識サービスでエラーが発生しました。"
        // ERROR_LANGUAGE_NOT_SUPPORTED (12) / ERROR_LANGUAGE_UNAVAILABLE (13) は Android 12 から
        12, 13 -> "英語の音声認識が使えません。Google アプリの設定で英語 (米国) の音声認識を追加してください。"
        else -> "音声認識でエラーが発生しました (コード $error)。"
    }
}
