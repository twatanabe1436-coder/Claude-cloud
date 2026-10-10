# オンラインのリーグ (Firebase)

週ごとの XP リーグは Firebase の **匿名ログイン** と **Firestore** を使います (無料の Spark プランで足ります)。
アプリは SDK を使わず REST API を呼びます (`core/.../League.kt` の `FirebaseLeague`)。

## データ

`leagues/{週}/players/{プレイヤー ID}` = `{ name: ニックネーム, xp: その週の XP, level: "B1" など }`
(週は日本時間の月曜はじまりの ISO 週。例: `2026-W41`)

## 準備 (Firebase コンソール)

1. プロジェクトを作成する
2. Authentication → ログイン方法で「匿名」を有効にする
3. Firestore Database を作成する (asia-northeast1、本番環境モード)
4. Firestore の「ルール」に [`firestore.rules`](firestore.rules) を貼り付けて公開する
5. プロジェクトの設定 → 全般の「プロジェクト ID」と「ウェブ API キー」を
   `app/src/main/java/.../hanaso/LeagueConfig.kt` に書く

プロジェクト ID とウェブ API キーは秘密ではありません (アプリに入れて配布するもの)。
誰が何を書けるかはルールで制限しています: 読むのはログインした人、書くのは自分の分だけで、項目と値の範囲も決めています。
