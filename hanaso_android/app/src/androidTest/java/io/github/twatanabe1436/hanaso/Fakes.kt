package io.github.twatanabe1436.hanaso

import android.os.Handler
import android.os.Looper
import io.github.twatanabe1436.hanaso.core.LeagueEntry
import io.github.twatanabe1436.hanaso.core.LeagueService
import io.github.twatanabe1436.hanaso.core.Level
import io.github.twatanabe1436.hanaso.speech.SpeechInput
import io.github.twatanabe1436.hanaso.speech.SpeechOutput
import io.github.twatanabe1436.hanaso.speech.VoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList

/**
 * テスト用の音声認識: queue に入れた英文を、少しずつ認識したふりをして返す。
 * untilStopped (マイクをもう一度押すまで聞き続ける) のときは、stop() が呼ばれるまで結果を返さない。
 */
class FakeSpeechInput : SpeechInput {
    val queue = ConcurrentLinkedDeque<String>()
    @Volatile var starts = 0
    @Volatile var lastUntilStopped: Boolean? = null
    @Volatile var lastHints: List<String> = emptyList()
    private val main = Handler(Looper.getMainLooper())
    private var current: SpeechInput.Callback? = null
    private var heard = ""

    override val available: Boolean = true

    override fun start(silenceMs: Long, untilStopped: Boolean, callback: SpeechInput.Callback, hints: List<String>) {
        cancel()
        current = callback
        heard = ""
        starts++
        lastUntilStopped = untilStopped
        lastHints = hints
        val text = queue.pollFirst()
        if (text == null) {
            // 何も話さない → 少し待って「聞き取れず」で終わる (聞き続けるときは stop() を待つ)
            if (!untilStopped) main.postDelayed({ finish(callback, "") }, 500)
            return
        }
        val words = text.split(" ")
        words.indices.forEach { i ->
            main.postDelayed({
                if (current === callback) {
                    heard = words.take(i + 1).joinToString(" ")
                    callback.onPartial(heard)
                }
            }, 80L * (i + 1))
        }
        if (!untilStopped) main.postDelayed({ finish(callback, text) }, 80L * (words.size + 2))
    }

    private fun finish(callback: SpeechInput.Callback, text: String) {
        if (current !== callback) return
        current = null
        callback.onEnd(text)
    }

    override fun stop() {
        val cb = current ?: return
        finish(cb, heard)
    }

    override fun cancel() {
        val cb = current ?: return
        current = null
        cb.onCancel()
    }
}

/** テスト用の読み上げ: 読んだ文を記録し、少し待って読み終わったことにする */
class FakeSpeaker : SpeechOutput {
    val spoken = CopyOnWriteArrayList<String>()
    private val main = Handler(Looper.getMainLooper())
    private var pending = 0
    private var generation = 0
    private var callbacks = mutableListOf<() -> Unit>()

    override val status: StateFlow<VoiceStatus> = MutableStateFlow(VoiceStatus.READY)

    override fun say(text: String, rate: Float?) {
        spoken += text
        pending++
        val gen = generation
        main.postDelayed({
            if (gen == generation) {
                pending--
                if (pending == 0) {
                    val cbs = callbacks
                    callbacks = mutableListOf()
                    cbs.forEach { it() }
                }
            }
        }, 60)
    }

    override fun stop() {
        generation++
        pending = 0
        callbacks = mutableListOf()
    }

    override fun whenIdle(callback: () -> Unit) {
        if (pending == 0) callback() else callbacks += callback
    }
}

/** テスト用のリーグ: ほかのプレイヤーは決まった XP、自分の分は送られた値 */
class FakeLeague(private val others: List<LeagueEntry>) : LeagueService {
    data class Submit(val week: String, val name: String, val xp: Int, val level: Level)

    val submitted = CopyOnWriteArrayList<Submit>()
    override val uid: String = "me"

    override suspend fun submit(week: String, name: String, xp: Int, level: Level) {
        submitted += Submit(week, name, xp, level)
    }

    override suspend fun top(week: String, limit: Int): List<LeagueEntry> {
        val me = submitted.lastOrNull()?.let { LeagueEntry(uid, it.name, it.xp, it.level) }
        return (others + listOfNotNull(me)).sortedByDescending { it.xp }.take(limit)
    }
}
