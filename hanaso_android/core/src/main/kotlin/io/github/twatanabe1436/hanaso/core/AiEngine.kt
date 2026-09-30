package io.github.twatanabe1436.hanaso.core

import kotlinx.coroutines.flow.Flow

/** 会話の AI。Claude 版 (ClaudeEngine) と、API キーなしで試せるデモ版 (MockEngine) がある。 */
interface AiEngine {
    /** デモモードなら true (画面に案内を出す) */
    val isDemo: Boolean

    /** 会話相手の返事を少しずつ流す。最後まで流れたら完了。失敗時は [AiException]。 */
    fun reply(conversation: Conversation): Flow<String>

    /** history の最後 (学習者の発話) を添削し、達成済みミッションも判定する。 */
    suspend fun feedback(conversation: Conversation): Feedback

    /** 次に言えることを3つ提案する。wantJa があれば「日本語で言いたいこと」の英訳を提案する。 */
    suspend fun hint(conversation: Conversation, wantJa: String? = null): List<HintSuggestion>

    suspend fun translate(text: String): Translation

    suspend fun summary(conversation: Conversation, completedMissions: Set<String>): Summary
}

/** AI 呼び出しの失敗の種類と、画面に出す日本語メッセージ。 */
enum class AiErrorKind(val messageJa: String) {
    AUTH("API キーが正しくないか、使えない状態です。設定画面で API キーを確認してください。"),
    BUSY("AI が混み合っています。少し待ってからもう一度お試しください。"),
    NETWORK("AI に接続できませんでした。インターネット接続を確認してください。"),
    REFUSAL("この内容には AI が応答できませんでした。別の言い方で話してみてください。"),
    BAD_OUTPUT("AI の応答を読み取れませんでした。もう一度お試しください。"),
    BAD_REQUEST("AI へのリクエストが受け付けられませんでした（クレジット残高不足などの可能性があります）。"),
    UNKNOWN("予期しないエラーが発生しました。もう一度お試しください。"),
}

class AiException(
    val kind: AiErrorKind,
    cause: Throwable? = null,
    /** API から返ってきた詳細 (英語)。原因の切り分け用に画面にも小さく出す。 */
    val detail: String? = null,
) : Exception(kind.messageJa, cause)
