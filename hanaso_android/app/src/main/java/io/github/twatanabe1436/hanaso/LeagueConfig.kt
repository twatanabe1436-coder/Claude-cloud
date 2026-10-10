package io.github.twatanabe1436.hanaso

/**
 * オンラインのリーグ (Firebase) の接続先。firebase/README.md の手順で作ったプロジェクトの値を入れる。
 * どちらも秘密ではない (アプリに入れて配布する値で、書き込める範囲は Firestore のルールで制限している)。
 * 空のあいだは、XP はこの端末にだけ記録する。
 */
object LeagueConfig {
    const val PROJECT_ID = ""
    const val WEB_API_KEY = ""

    val configured: Boolean get() = PROJECT_ID.isNotBlank() && WEB_API_KEY.isNotBlank()
}
