# オンラインのリーグ (Firebase)

週ごとの XP リーグは Firebase の **匿名ログイン** と **Firestore** を使います (無料の Spark プランで足ります)。
アプリは SDK を使わず REST API を呼びます (`core/.../League.kt` の `FirebaseLeague`)。

いまの接続先はプロジェクト `hanaso-league` です (`app/src/main/java/.../hanaso/LeagueConfig.kt`)。

## データ

`leagues/{週}/players/{プレイヤー ID}` = `{ name: ニックネーム, xp: その週の XP, level: "B1" など }`
(週は日本時間の月曜はじまりの ISO 週。例: `2026-W41`)

## 料金

Spark プランは支払い方法を登録しないので、請求は発生しません。無料枠 (Firestore は 1 日に読み取り 5 万回・書き込み 2 万回など) を超えると、その日は止まるだけです。
コンソール左下の「アップグレード」(従量課金の Blaze プラン) は押さないでください。

## 準備 (Firebase コンソール)

画面の文言は 2026 年 10 月のものです。メニューが見つからないときは、左上の「商品を検索」で探せます。

1. **プロジェクトを作る**
   - https://console.firebase.google.com/ で「Firebase プロジェクトを作成」
   - プロジェクト名を入れる (名前の下に出るのが**プロジェクト ID**)
   - Gemini と Google アナリティクスはどちらでもよい (オフで足りる。オンでも無料)
2. **匿名ログインを有効にする**
   - 左メニュー「セキュリティ」→「Authentication」→「始める」
   - 「ログイン方法」タブ →「匿名」を有効にして保存
   - 「Google でログイン」や「SMS 多要素認証 (アップグレードして有効にする)」の案内は使わない
3. **Firestore を作る**
   - 左メニュー「Database と Storage」→「Firestore」→「データベースの作成」
   - エディションは Standard、データベース ID は `(default)` のまま (変えるとアプリから見つからない)
   - ロケーションは `asia-northeast1 (Tokyo)` (あとから変えられない)、「本番環境モード」で作成
4. **ルールを公開する**
   - Firestore の「ルール」タブの中身を [`firestore.rules`](firestore.rules) に置き換えて「公開」
5. **ウェブ API キーを作る**
   - 左上の「設定」→「全般」の「マイアプリ」で `</>` (ウェブ) を押す
   - ニックネーム (例: `hanaso`) を入れ、Firebase Hosting はチェックしないで「アプリを登録」
   - 表示される `firebaseConfig` の `apiKey` (`AIza` で始まる) がウェブ API キー、`projectId` がプロジェクト ID
   - (「全般」にはプロジェクト番号の下に「ウェブ API キー」が出ることもあるが、出ないプロジェクトもある)
6. プロジェクト ID とウェブ API キーを `LeagueConfig.kt` の `PROJECT_ID` と `WEB_API_KEY` に書く

プロジェクト ID とウェブ API キーは秘密ではありません (アプリに入れて配布するもの)。
誰が何を書けるかはルールで制限しています: 読むのはログインした人、書くのは自分の分だけで、項目と値の範囲も決めています。

## 動くか確かめる

アプリを使わずに、REST API で確かめられます (`<KEY>` はウェブ API キー)。

```sh
# 匿名ログイン: idToken と localId が返る
curl -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=<KEY>" \
  -H 'Content-Type: application/json' -d '{"returnSecureToken":true}'

# 自分の分を書く: 200 が返る (他人の localId や決められていない項目なら 403)
curl -X POST "https://firestore.googleapis.com/v1/projects/hanaso-league/databases/(default)/documents:commit" \
  -H "Authorization: Bearer <idToken>" -H 'Content-Type: application/json' \
  -d '{"writes":[{"update":{"name":"projects/hanaso-league/databases/(default)/documents/leagues/test/players/<localId>","fields":{"name":{"stringValue":"test"},"xp":{"integerValue":"0"},"level":{"stringValue":"B1"}}}}]}'
```

確かめるときは、週の代わりに `test` のような名前を使うと、本物の順位表に混ざりません。
ルールで削除を禁止しているので、試しに書いたデータは Firebase コンソールの Firestore 画面から消します。
