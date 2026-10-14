# Hanaso 英会話 (Android) — 作業メモ

Claude Code で続きの作業をするときに読むメモです。アプリの説明は [README.md](README.md) を見てください。

## ユーザーについて

- 返事は**日本語**で書く。
- 英語を勉強中で、Epop (単語・クイズ) と併用している。Epop の対話モードの画面を参考にしてほしいと言われている。
- UI の文字は必要最低限にする (説明文を増やさない)。
- ふだんは**台本モード** (AI なし・無料) で使っている。
- 新しい版は APK で渡す (GitHub の Release に置き、ダウンロードリンクを伝える)。

## ブランチ

- 作業ブランチは `ccr-bc673e97-p715mz`。main にはマージしていない。続きもこのブランチで行う。
- PR はユーザーに頼まれたときだけ作る。

## 構成

| 場所 | 内容 |
| --- | --- |
| `core/` | Android に依存しない部分 (Kotlin/JVM の composite build)。台本・判定・AI 呼び出し・XP・リーグ・聞き間違いの修正など。`./gradlew -p core test` で手元でテストできる (JDK 17 以上)。 |
| `app/` | Android アプリ (Jetpack Compose)。ビルドには Android SDK が必要。SDK がなければ CI に任せる。 |
| `app/src/androidTest/.../SmokeTest.kt` | エミュレータで画面を操作するテスト。音声認識・読み上げ・リーグは `Fakes.kt` の偽物。**画面の文言を変えたら、ここの文字列も直す。** |
| `firebase/` | オンラインのリーグ用の Firestore ルールと準備手順。 |
| `signing/` | 公開用の署名鍵 (`hanaso-release.p12`) と証明書の SHA-256。 |

主なファイル:

- `core/.../ScriptEngine.kt`, `Scripts.kt`: 台本モード。状態を持たず、会話の履歴から毎回いまのお題を求め直す。言い直しは 2 回まで。
- `core/.../Catalog.kt`: シナリオ 16 本 + フリートーク 6 本と CEFR レベル (A1〜C2)。
- `core/.../Transcript.kt`: 音声認識の聞き間違いの修正。直すのは**いまのお題の語にとても近い語だけ**。文法の違い (語形変化・冠詞・よく使う語) は直さない。自由回答のお題では直さない。
- `core/.../Xp.kt`, `League.kt`: XP と週ごとのリーグ (Firebase の REST API。SDK は使わない)。
- `app/.../ui/talk/TalkSession.kt`, `TalkScreen.kt`: 会話画面。ふだんは「いまのやりとり」だけ表示し、ヘッダーの履歴ボタンで全体を表示する。

## CI (`.github/workflows/hanaso-android.yml`)

- push のたびに、ビルド・lint・ユニットテストと、エミュレータ (API 26 と 35) での SmokeTest を行う。
- API 35 の画面のスクショは、ジョブのログに base64 で出る (`=== SHOT screenshots/<名前>.png` 〜 `=== END`)。artifact の `emulator-api35` にも入る。
- 一時的な失敗 (エミュレータの起動や依存のダウンロードのタイムアウト) は、失敗したジョブだけ再実行すればよい。

## リリースの手順

1. `app/build.gradle.kts` の `versionCode` (+1) と `versionName`、`SettingsScreen.kt` のバージョン表記 (`Hanaso v0.x.y`)、README のインストールリンク (`hanaso-v0.x.y`) を更新する。
2. push して CI が通るのを確かめる。
3. CI が通ったコミットにタグ `hanaso-v0.x.y` を付けて push する (`git tag -a hanaso-v0.x.y -m "Hanaso v0.x.y" && git push origin hanaso-v0.x.y`)。
   `gh` があればワークフローの手動実行でもよい (`gh workflow run hanaso-android.yml --ref ccr-bc673e97-p715mz -f release_version=0.x.y`)。
   ワークフローがリポジトリの既定のブランチにないので、GitHub の画面には「Run workflow」が出ないことがある。
   CI が公開用の鍵で署名し、証明書が `signing/release-cert.sha256` と一致するか確かめてから、Release `hanaso-v0.x.y` を作る。
   公開するとだれでもダウンロードできるので、タグを push する前にユーザーに確かめる。
4. リンクは `https://github.com/twatanabe1436-coder/Claude-cloud/releases/download/hanaso-v0.x.y/hanaso.apk`。
   同じリポジトリに別のアプリ (実技タイマー) があるので、Release は「最新版 (latest)」にしない (CI が `--latest=false` で作る)。

署名鍵のパスワードは GitHub の secret `HANASO_KEYSTORE_PASSWORD` にだけある。**リポジトリには絶対に書かない。** 手元で公開用の APK を作る必要はない (CI が作る)。
v0.3.1 から同じ鍵なので、ユーザーは上書きインストールできる。

## 作業する場所

- ユーザーの Windows PC では `E:\Hanaso` がこのリポジトリのルート。ここには JDK・Android SDK・`gh` がないので、ビルドとテストは CI に任せ、リリースはタグの push で行う。
  CI の結果は公開 API (`https://api.github.com/repos/twatanabe1436-coder/Claude-cloud/actions/runs?branch=<ブランチかタグ>`) で認証なしに読める。
- ユーザーは**無料のまま**使いたい。有料のサービスやプランを勧めない。

## いまの状況 (2026-10-14)

- **v0.4.1 をリリース済み (リーグを Firebase に接続)**: Firebase のプロジェクト `hanaso-league` (Spark プラン、ユーザーのアカウント) を `LeagueConfig.kt` に設定した。
  実サーバーで匿名ログイン・トークン更新・自分の分の書き込み・順位の読み込みが通り、ルールで他人の分・余計な項目・上限超えの XP・未ログインの読み込みが拒否されることを確かめた。
  確認に使ったデータは `leagues/test/players/...` にある (本物の週には書いていない)。
- それまでの版: v0.2.0 台本モード、v0.3.0 CEFR レベルとタップで確定する音声入力、v0.3.1 署名鍵の固定、v0.3.3 場面の説明とキーボードを閉じる、v0.3.5 お題をマイクの上に・訳を自動表示、v0.4.0 いまのやりとりだけの表示・聞き間違いの修正・XP とリーグ (サーバー未接続)。

## 残っている作業

1. ユーザーから新しい要望があれば、それに対応する。
2. 実機でリーグに参加したあと、順位が出るかをユーザーに確かめてもらう (エミュレータのテストはリーグを偽物に差し替えている)。
