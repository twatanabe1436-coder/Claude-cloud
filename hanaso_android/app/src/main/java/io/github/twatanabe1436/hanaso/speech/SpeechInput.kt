package io.github.twatanabe1436.hanaso.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

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

    /** 認識を始める。すでに動いているものは中断される (onCancel)。 */
    fun start(silenceMs: Long, callback: Callback)

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

    /** 1回の認識。前回の認識からの遅れたコールバックが混ざらないよう、毎回新しい SpeechRecognizer を使う */
    private inner class Session(val callback: SpeechInput.Callback) {
        val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var partial = ""
        var finished = false

        fun finish(text: String?) {
            if (finished) return
            finished = true
            if (active === this) active = null
            // コールバックの中で destroy しないよう、少し後で片付ける
            main.post { recognizer.destroy() }
            if (text == null) callback.onCancel() else callback.onEnd(text.trim())
        }
    }

    override fun start(silenceMs: Long, callback: SpeechInput.Callback) {
        cancel()
        val session = Session(callback)
        active = session
        val r = session.recognizer
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                if (!session.finished) callback.onLevel(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank() && !session.finished) {
                    session.partial = text
                    callback.onPartial(text)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                session.finish(if (text.isNullOrBlank()) session.partial else text)
            }

            override fun onError(error: Int) {
                when (error) {
                    // 何も聞こえなかった / 聞き取れなかった
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> session.finish(session.partial)
                    // stop の後に来ることがある
                    SpeechRecognizer.ERROR_CLIENT -> session.finish(session.partial)
                    else -> {
                        if (!session.finished) callback.onError(errorMessage(error))
                        session.finish(null)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 考えながら話す学習者向けに、話し終わりの判定を少し長めにする (対応していない認識エンジンもある)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            r.startListening(intent)
        } catch (e: RuntimeException) {
            callback.onError("音声認識を開始できませんでした。もう一度お試しください。")
            session.finish(null)
        }
    }

    override fun stop() {
        active?.recognizer?.stopListening()
    }

    override fun cancel() {
        val session = active ?: return
        session.recognizer.cancel()
        session.finish(null)
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
