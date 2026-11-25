package io.github.twatanabe1436.hanaso.core

// 台本モードの台本のうち、v0.6.0 で足した B1・B2 のロールプレイ (書き方は Scripts.kt と同じ)。
// Scripts.all にまとめて入る。

private fun p(en: String, ja: String) = Phrase(en, ja)

internal val intermediateScripts: List<Script> = listOf(
    // ---- B1 ----
    Script(
        scenarioId = "airport",
        openerJa = "こんにちは！今日はどちらまで行かれますか？パスポートを拝見できますか？",
        steps = listOf(
            ScriptStep(
                taskJa = "東京へ行くと伝えて、パスポートを渡そう",
                mission = "checkin",
                keywords = listOf("tokyo|japan|narita|haneda"),
                answers = listOf(
                    p("I'm flying to Tokyo. Here's my passport.", "東京へ行きます。パスポートはこちらです。"),
                    p("To Tokyo. Here you go.", "東京です。どうぞ。"),
                    p("I'm going to Tokyo.", "東京に行きます。"),
                ),
                tipJa = "Here's my passport. や Here you go. は、物を手渡すときのひとことです。",
                reply = p("Thank you. Do you have a seat preference, window or aisle?", "ありがとうございます。お座席のご希望はありますか？窓側と通路側、どちらがよろしいですか？"),
            ),
            ScriptStep(
                taskJa = "通路側の席をお願いしよう",
                mission = "seat",
                keywords = listOf("aisle|isle|seat"),
                answers = listOf(
                    p("Could I have an aisle seat, please?", "通路側の席をお願いできますか？"),
                    p("An aisle seat, please.", "通路側でお願いします。"),
                    p("I'd prefer an aisle seat.", "通路側がいいです。"),
                ),
                tipJa = "aisle（通路）の s は読みません。「アイル」と発音します。",
                reply = p(
                    "Sure, you're in 34C, on the aisle. Now, could you put your suitcase on the scale? Hmm, it's 26 kilograms. The limit is 23, so there's a 100-dollar fee.",
                    "かしこまりました。34C の通路側のお席です。では、スーツケースをはかりに載せていただけますか？うーん、26 キロですね。制限は 23 キロなので、100 ドルの超過料金がかかります。",
                ),
            ),
            ScriptStep(
                taskJa = "料金を払わずにすむ方法がないか聞こう",
                contextJa = "3 キロオーバーです。お土産を少し機内持ち込みのバッグに移せそうです。",
                mission = "baggage",
                keywords = listOf("move|take|avoid|way|option*|instead|without|remove"),
                answers = listOf(
                    p("Is it okay if I move some things to my carry-on?", "手荷物にいくつか移してもいいですか？"),
                    p("Is there any way to avoid the fee?", "料金を払わずにすむ方法はありますか？"),
                    p("Can I take some things out?", "中身を少し出してもいいですか？"),
                ),
                tipJa = "Is it okay if I 〜? は「〜してもいいですか？」と許可を求める丁寧な言い方です。",
                reply = p("Of course. Take your time. ... Great, it's 23 kilograms now. You're all set.", "もちろんです。ごゆっくりどうぞ。…はい、23 キロになりました。これで大丈夫です。"),
            ),
            ScriptStep(
                taskJa = "搭乗が何時に始まるか聞こう",
                keywords = listOf("boarding|board|time|when|start*"),
                answers = listOf(
                    p("What time does boarding start?", "搭乗は何時に始まりますか？"),
                    p("When do we start boarding?", "搭乗はいつ始まりますか？"),
                ),
                tipJa = "boarding は「搭乗」。boarding pass（搭乗券）や boarding gate（搭乗口）も一緒に覚えましょう。",
                reply = p("Boarding starts at 12:50 at gate 52. Here's your boarding pass.", "搭乗は 12 時 50 分から、52 番ゲートで始まります。こちらが搭乗券です。"),
            ),
            ScriptStep(
                taskJa = "お礼を言って、ゲートへの行き方を聞こう",
                keywords = listOf("thank*", "gate|where|how|way"),
                answers = listOf(
                    p("Thank you. How do I get to gate 52?", "ありがとうございます。52 番ゲートへはどう行けばいいですか？"),
                    p("Thanks! Which way is the gate?", "ありがとう！ゲートはどちらですか？"),
                ),
                tipJa = "Which way is 〜? で「〜はどちらの方向ですか？」と聞けます。",
                reply = p("Go through security over there, then turn right. Have a nice flight!", "あちらの保安検査を通って、右に曲がってください。よい空の旅を！"),
            ),
        ),
    ),
    Script(
        scenarioId = "lostfound",
        openerJa = "こんにちは、遺失物係です。どうされましたか？",
        steps = listOf(
            ScriptStep(
                taskJa = "電車にリュックを置き忘れたと伝えよう",
                mission = "explain",
                keywords = listOf("backpack|bag|rucksack", "left|lost|forgot|leave"),
                answers = listOf(
                    p("I think I left my backpack on the train.", "電車にリュックを置き忘れたと思います。"),
                    p("I lost my backpack on the subway.", "地下鉄でリュックをなくしました。"),
                    p("I left my bag on the train this morning.", "今朝、電車にかばんを置き忘れました。"),
                ),
                tipJa = "「置き忘れる」は leave（過去形は left）。I left 〜 on the train. の形で使います。",
                reply = p("Oh no, I'm sorry to hear that. Which line were you on, and about what time?", "それは大変でしたね。何線に乗っていましたか？だいたい何時ごろですか？"),
            ),
            ScriptStep(
                taskJa = "グリーンラインに、今朝 9 時ごろ乗っていたと答えよう",
                keywords = listOf("green", "nine|morning|around|about"),
                answers = listOf(
                    p("I was on the green line, around 9 this morning.", "今朝 9 時ごろ、グリーンラインに乗っていました。"),
                    p("The green line, at about nine.", "グリーンラインで、9 時ごろです。"),
                ),
                tipJa = "around や about を付けると「〜ごろ」と時間をぼかして言えます。",
                reply = p("Okay. We did get a few bags from the green line this morning. Can you describe it?", "わかりました。今朝はグリーンラインからかばんがいくつか届いています。どんなかばんか説明してもらえますか？"),
            ),
            ScriptStep(
                taskJa = "黒いリュックで、ノートパソコンが入っていると説明しよう",
                mission = "describe",
                keywords = listOf("black", "laptop|computer|pc"),
                answers = listOf(
                    p("It's a black backpack with a laptop inside.", "ノートパソコンが入った黒いリュックです。"),
                    p("It's black, and there's a laptop in it.", "黒くて、中にノートパソコンが入っています。"),
                ),
                tipJa = "with 〜 inside で「中に〜が入った」と、物の特徴をまとめて言えます。",
                reply = p("Let me check... Is this it? Can you tell me anything else that's inside, just to be sure?", "確認しますね…これですか？念のため、ほかに何が入っているか教えてもらえますか？"),
            ),
            ScriptStep(
                taskJa = "前のポケットに青い財布も入っていると伝えよう",
                keywords = listOf("wallet|purse", "blue"),
                answers = listOf(
                    p("Yes, that's it! There's also a blue wallet in the front pocket.", "それです！前のポケットに青い財布も入っています。"),
                    p("There should be a blue wallet, too.", "青い財布も入っているはずです。"),
                ),
                tipJa = "There should be 〜. で「〜があるはずです」と言えます。",
                reply = p("Yes, here's the blue wallet. It's definitely yours. I just need to see some ID before I give it back.", "はい、青い財布がありますね。あなたのもので間違いありません。お返しする前に、身分証明書を見せていただく必要があります。"),
            ),
            ScriptStep(
                taskJa = "パスポートでいいか確認しよう",
                mission = "pickup",
                keywords = listOf("passport"),
                answers = listOf(
                    p("Will a passport work?", "パスポートでいいですか？"),
                    p("Is my passport okay?", "パスポートで大丈夫ですか？"),
                    p("Here's my passport. Is that okay?", "パスポートです。これで大丈夫ですか？"),
                ),
                tipJa = "Will 〜 work? で「〜で大丈夫ですか（使えますか）？」と聞けます。",
                reply = p("A passport is perfect. Just sign here, and it's all yours. Have a good day!", "パスポートで大丈夫です。ここにサインしていただければ、お返しします。良い一日を！"),
            ),
        ),
    ),
    Script(
        scenarioId = "reservation",
        openerJa = "こんにちは、ハーバー・グリルにお電話ありがとうございます。ルーシーです。ご用件をお伺いします。",
        steps = listOf(
            ScriptStep(
                taskJa = "土曜の夜に予約したいと伝えよう",
                keywords = listOf("reservation|reserve|book|table", "saturday"),
                answers = listOf(
                    p("Hi, I'd like to make a reservation for Saturday evening.", "こんにちは、土曜の夜に予約をしたいのですが。"),
                    p("Can I book a table for Saturday night?", "土曜の夜にテーブルを予約できますか？"),
                ),
                tipJa = "make a reservation（予約する）は電話予約の定番。book a table とも言います。",
                reply = p("Of course! For how many people, and what time?", "かしこまりました！何名様で、何時がよろしいですか？"),
            ),
            ScriptStep(
                taskJa = "4 人で、7 時からと伝えよう",
                mission = "book",
                keywords = listOf("four", "seven"),
                answers = listOf(
                    p("It's for four people at seven.", "7 時に 4 人です。"),
                    p("Four people at seven o'clock, please.", "4 人で、7 時でお願いします。"),
                ),
                tipJa = "for 〜 people で人数、at 〜 で時刻を伝えます。",
                reply = p(
                    "Let me check... I'm sorry, seven o'clock is fully booked. We do have a table at seven thirty. Would that work?",
                    "確認しますね…申し訳ありません、7 時は満席です。7 時半ならお席がございますが、いかがですか？",
                ),
            ),
            ScriptStep(
                taskJa = "7 時半で大丈夫と答えよう",
                mission = "adjust",
                keywords = listOf("thirty|30|half|fine|good|work*|okay|ok|great|perfect|sure"),
                answers = listOf(
                    p("Seven thirty works for us.", "7 時半で大丈夫です。"),
                    p("Sure, seven thirty is fine.", "はい、7 時半でかまいません。"),
                ),
                tipJa = "〜 works for us. で「〜で都合がいいです」と言えます。予定の調整でよく使います。",
                reply = p("Great. Can I have your name and phone number, please?", "ありがとうございます。お名前とお電話番号をいただけますか？"),
            ),
            ScriptStep(
                taskJa = "名前（佐藤）と電話番号を伝えよう",
                contextJa = "名前は佐藤、電話番号は 555-0123 です。",
                keywords = listOf("sato|name|number|it's|this is"),
                answers = listOf(
                    p("My name is Sato, and my number is 555-0123.", "名前は佐藤で、電話番号は 555-0123 です。"),
                    p("It's Sato. My number is 555-0123.", "佐藤です。番号は 555-0123 です。"),
                ),
                tipJa = "電話番号は数字を 1 つずつ区切って読みます（five five five, zero one two three）。",
                reply = p("Thank you. And is it for a special occasion?", "ありがとうございます。何か特別なお祝いですか？"),
            ),
            ScriptStep(
                taskJa = "友達の誕生日なので、何か特別なことができるか聞こう",
                mission = "request",
                keywords = listOf("birthday", "special|something|cake|candle|dessert|surprise|could you|can you"),
                answers = listOf(
                    p("Yes, it's my friend's birthday. Could you do something special?", "はい、友達の誕生日なんです。何か特別なことをしてもらえますか？"),
                    p("It's my friend's birthday. Can you bring a cake with a candle?", "友達の誕生日なんです。ろうそくを立てたケーキを持ってきてもらえますか？"),
                ),
                tipJa = "It's my friend's birthday. のように理由を先に言ってからお願いすると自然です。",
                reply = p(
                    "How nice! We'll put a candle on a dessert for free. We look forward to seeing you on Saturday at seven thirty!",
                    "すてきですね！デザートに無料でろうそくをお付けします。土曜の 7 時半にお待ちしております！",
                ),
            ),
        ),
    ),
    Script(
        scenarioId = "neighbor",
        openerJa = "あら、こんにちは！3B に越してきたのはあなた？隣のヘレンよ。",
        steps = listOf(
            ScriptStep(
                taskJa = "先週引っ越してきたと、名前と一緒に伝えよう",
                mission = "intro",
                keywords = listOf("moved|move|new", "i'm|name|nice to meet"),
                answers = listOf(
                    p("Yes, I just moved in last week. I'm Ken. Nice to meet you.", "はい、先週引っ越してきたばかりです。ケンです。よろしくお願いします。"),
                    p("Yes, that's me. My name is Ken. I moved here last week.", "はい、私です。ケンといいます。先週ここに越してきました。"),
                ),
                tipJa = "move in で「（新居に）入居する」。I just moved in. で「越してきたばかりです」になります。",
                reply = p("Nice to meet you! Welcome to the building. How do you like it so far?", "よろしくね！ようこそ。ここの暮らしはどう？"),
            ),
            ScriptStep(
                taskJa = "部屋は気に入っているけど、まだ荷ほどき中と伝えよう",
                keywords = listOf("like|love|nice|great|good", "unpack*|box*|still|busy"),
                answers = listOf(
                    p("I really like it, but I'm still unpacking.", "とても気に入っていますが、まだ荷ほどき中です。"),
                    p("It's great, but there are still boxes everywhere.", "すてきです。でもまだ段ボールだらけです。"),
                ),
                tipJa = "but I'm still 〜ing. で「でもまだ〜しているところ」と近況を付け足せます。",
                reply = p("Ha ha, that takes a while. If you need anything, just knock on my door.", "ふふ、時間がかかるわよね。何か困ったら、遠慮なくうちのドアをノックしてね。"),
            ),
            ScriptStep(
                taskJa = "ゴミの日がいつか聞こう",
                mission = "rule",
                keywords = listOf("garbage|trash|rubbish|recycling|recycle"),
                answers = listOf(
                    p("Thank you! Actually, which day is garbage day?", "ありがとうございます！ところで、ゴミの日は何曜日ですか？"),
                    p("Can I ask when the garbage goes out?", "ゴミはいつ出せばいいか聞いてもいいですか？"),
                ),
                tipJa = "Actually, 〜? は「ところで」と、聞きたいことを切り出すときに便利です。",
                reply = p(
                    "It's Tuesday morning, for both garbage and recycling. Just don't put the bags out before eight on Monday night.",
                    "火曜の朝よ。ゴミもリサイクルも同じ日。ただ、月曜の夜 8 時より前には出さないでね。",
                ),
            ),
            ScriptStep(
                taskJa = "近くにいいスーパーがあるか聞こう",
                mission = "local",
                keywords = listOf("supermarket|grocery|store|shop|market"),
                answers = listOf(
                    p("Got it. Is there a good supermarket around here?", "わかりました。この辺りにいいスーパーはありますか？"),
                    p("Do you know a good grocery store nearby?", "近くにいい食料品店を知っていますか？"),
                ),
                tipJa = "around here や nearby で「この辺りに」「近くに」と言えます。",
                reply = p(
                    "Yes! There's a cheap one two blocks away, and there's a farmers market every Saturday. The bread there is wonderful.",
                    "あるわよ！2 ブロック先に安いお店があるし、毎週土曜はファーマーズマーケットも開かれるの。そこのパンが最高なのよ。",
                ),
            ),
            ScriptStep(
                taskJa = "お礼を言って、今度の土曜に行ってみると伝えよう",
                keywords = listOf("thank*", "try|go|check|visit"),
                answers = listOf(
                    p("Thank you so much. I'll check it out this Saturday.", "本当にありがとうございます。今度の土曜に行ってみます。"),
                    p("Thanks! I'll go and try the bread on Saturday.", "ありがとう！土曜にパンを買いに行ってみます。"),
                ),
                tipJa = "check it out は「（試しに）行ってみる・見てみる」というカジュアルな言い方です。",
                reply = p("You're welcome. It's so nice to have a friendly new neighbor. See you around!", "どういたしまして。感じのいいお隣さんが来てくれてうれしいわ。またね！"),
            ),
        ),
    ),
    Script(
        scenarioId = "homestay",
        openerJa = "夕ごはんができたわよ！今夜はローストチキンにしたの。おなかすいてるといいんだけど！",
        steps = listOf(
            ScriptStep(
                taskJa = "おなかがすいていると答えて、料理をほめよう",
                mission = "thanks",
                keywords = listOf("delicious|good|great|amazing|tasty|love|smells|looks|yummy|wonderful"),
                answers = listOf(
                    p("I'm starving! This looks delicious.", "おなかぺこぺこです！とてもおいしそうです。"),
                    p("Yes! It smells amazing.", "はい！すごくいいにおいです。"),
                    p("This is delicious! Thank you for cooking.", "とてもおいしいです！作ってくれてありがとうございます。"),
                ),
                tipJa = "looks delicious（おいしそう）、smells amazing（いいにおい）のように、五感の動詞で感想を言えます。",
                reply = p("Aw, thank you! So, tell me about your family back in Japan.", "あら、ありがとう！それで、日本の家族のことを教えて。"),
            ),
            ScriptStep(
                taskJa = "自分の家族のことを話そう",
                contextJa = "家族構成や、家族のちょっとした紹介を自由に話しましょう。",
                mission = "share",
                open = true,
                answers = listOf(
                    p("I live with my parents and my younger sister in Osaka. My sister is a high school student.", "大阪で両親と妹と住んでいます。妹は高校生です。"),
                    p("I have two brothers, and my family loves cooking, too.", "兄弟が 2 人いて、うちの家族も料理が大好きです。"),
                ),
                tipJa = "I live with 〜. や I have 〜. で家族を紹介できます。younger / older で年下・年上を表します。",
                reply = p("That sounds lovely. So, what would you like to do while you're here?", "すてきね。それで、こっちにいる間に何をしてみたい？"),
            ),
            ScriptStep(
                taskJa = "週末にビーチへ行ってみたいと伝えよう",
                keywords = listOf("beach*|ocean|sea"),
                answers = listOf(
                    p("I'd love to go to the beach on the weekend.", "週末にビーチに行ってみたいです。"),
                    p("I want to visit a beach this weekend.", "今週末、ビーチに行きたいです。"),
                ),
                tipJa = "I'd love to 〜. は I want to 〜. より気持ちが伝わる、やわらかい言い方です。",
                reply = p(
                    "Good idea! Mission Bay is lovely. We can take you there on Saturday. Oh, by the way, do you have any questions about the house?",
                    "いいわね！ミッション・ベイがきれいよ。土曜に連れて行ってあげる。あ、そうだ、家のことで何か質問はある？",
                ),
            ),
            ScriptStep(
                taskJa = "シャワーは何時までに浴びればいいか聞こう",
                mission = "rule",
                keywords = listOf("shower*|bath*"),
                answers = listOf(
                    p("Yes. Is there a time I should take a shower by?", "はい。何時までにシャワーを浴びたほうがいいですか？"),
                    p("When is a good time to take a shower?", "シャワーはいつ浴びるのがいいですか？"),
                ),
                tipJa = "Is there a time I should 〜 by? で「何時までに〜すべきですか」と、家のルールを丁寧に確認できます。",
                reply = p(
                    "Good question. Please finish your shower before ten, because the kids go to bed early. And let me know if you'll miss dinner.",
                    "いい質問ね。子どもたちが早く寝るから、シャワーは 10 時までにお願い。あと、夕食がいらないときは教えてね。",
                ),
            ),
            ScriptStep(
                taskJa = "わかったと伝えて、皿洗いを手伝うと申し出よう",
                keywords = listOf("help|wash|dishes|clean"),
                answers = listOf(
                    p("Okay, I will. Can I help you with the dishes?", "わかりました、そうします。お皿洗いを手伝いましょうか？"),
                    p("Sure. Let me wash the dishes tonight.", "はい。今夜は私にお皿を洗わせてください。"),
                ),
                tipJa = "Can I help you with 〜? や Let me 〜. で、手伝いを申し出ることができます。",
                reply = p("Oh, you're so sweet! Thank you. I think we're going to have a great month together.", "まあ、なんてやさしいの！ありがとう。楽しい 1 か月になりそうね。"),
            ),
        ),
    ),
    Script(
        scenarioId = "favor",
        openerJa = "やあ、ちょっと参ってるみたいだね。大丈夫？",
        steps = listOf(
            ScriptStep(
                taskJa = "Excel のグラフで困っていると説明しよう",
                mission = "explain",
                keywords = listOf("chart*|graph*|excel|spreadsheet*", "trouble|problem*|can't|difficult|hard|stuck|help|working"),
                answers = listOf(
                    p("Not really. I'm having trouble with the charts in Excel.", "あまり。Excel のグラフで困っていて。"),
                    p("I can't get the charts right in Excel.", "Excel のグラフがうまく作れなくて。"),
                ),
                tipJa = "I'm having trouble with 〜. は「〜で困っています」という便利な言い方です。",
                reply = p("Oh, I know that feeling. What are they for?", "ああ、その気持ちわかるよ。何に使うグラフなの？"),
            ),
            ScriptStep(
                taskJa = "木曜に部長たちに売上を発表すると説明しよう",
                keywords = listOf("thursday|presentation|present|meeting|sales|managers"),
                answers = listOf(
                    p("I have to present our sales numbers to the managers on Thursday.", "木曜に部長たちに売上の数字を発表しないといけないんです。"),
                    p("They're for my presentation on Thursday.", "木曜のプレゼン用なんです。"),
                ),
                tipJa = "present 〜 to … で「…に〜を発表する」。動詞の present はアクセントが後ろ（プリゼント）です。",
                reply = p("Ah, that's an important one. Charts can be really tricky.", "なるほど、大事な発表だね。グラフって本当に難しいよね。"),
            ),
            ScriptStep(
                taskJa = "見てもらえないか丁寧に頼もう",
                mission = "ask",
                keywords = listOf("mind|could you|can you|would you|help|look"),
                answers = listOf(
                    p("Would you mind taking a look?", "ちょっと見てもらえませんか？"),
                    p("Could you help me with them if you have time?", "時間があったら手伝ってもらえますか？"),
                ),
                tipJa = "Would you mind 〜ing? は「〜していただけませんか」と丁寧に頼む言い方。引き受けるときの返事は Not at all. です。",
                reply = p("Not at all. I'm busy today, but I'm free tomorrow afternoon. Does that work for you?", "もちろんいいよ。今日は忙しいけど、明日の午後なら空いてる。それで大丈夫？"),
            ),
            ScriptStep(
                taskJa = "明日の午後で大丈夫と答えて、2 時はどうか提案しよう",
                keywords = listOf("two|tomorrow|afternoon"),
                answers = listOf(
                    p("Tomorrow afternoon is perfect. How about two o'clock?", "明日の午後でばっちりです。2 時はどうですか？"),
                    p("Yes, that works. Is two okay?", "はい、大丈夫です。2 時でいいですか？"),
                ),
                tipJa = "How about 〜? で時間を提案できます。",
                reply = p("Two is great. Just send me the file before then.", "2 時でいいよ。それまでにファイルを送っておいて。"),
            ),
            ScriptStep(
                taskJa = "お礼を言って、コーヒーをおごると申し出よう",
                mission = "thank",
                keywords = listOf("thank*|appreciate", "coffee*|on me|treat|buy"),
                answers = listOf(
                    p("Thanks a lot, Ben. Coffee's on me!", "ベン、本当にありがとう。コーヒーおごるね！"),
                    p("Thank you so much. Let me buy you a coffee.", "本当にありがとう。コーヒーをごちそうさせて。"),
                ),
                tipJa = "〜 is on me. で「〜は私のおごり」。お礼の気持ちを軽く伝えられます。",
                reply = p("Ha ha, deal! Good luck with the presentation. You'll do great.", "はは、決まりだね！プレゼン頑張って。きっとうまくいくよ。"),
            ),
        ),
    ),

    // ---- B2 ----
    Script(
        scenarioId = "deadline",
        openerJa = "話があるんですって？どうしたの？",
        steps = listOf(
            ScriptStep(
                taskJa = "金曜までにレポートが間に合わないと切り出そう",
                contextJa = "市場調査のレポートの締め切りは今週の金曜日です。",
                keywords = listOf("friday|deadline|on time|in time", "report|ready|finish*|make it|done"),
                answers = listOf(
                    p("I'm afraid the report won't be ready by Friday.", "申し訳ないのですが、レポートが金曜までに間に合いそうにありません。"),
                    p("I don't think I can finish the report by Friday.", "金曜までにレポートを終えられそうにありません。"),
                ),
                tipJa = "I'm afraid 〜. で、言いにくいことをやわらかく切り出せます。",
                reply = p("Oh. That's not great news. What happened?", "そう。あまりいい知らせではないわね。何があったの？"),
            ),
            ScriptStep(
                taskJa = "アンケートのデータが 1 週間遅れて届いたと説明しよう",
                contextJa = "別のチームから届くはずのアンケートデータが、予定より 1 週間遅れて、昨日やっと届きました。",
                mission = "explain",
                keywords = listOf("data|survey|results", "late|delay*|yesterday|week"),
                answers = listOf(
                    p("The survey data from the other team came in a week late. We only got it yesterday.", "別チームからのアンケートデータが 1 週間遅れて届いたんです。受け取ったのは昨日です。"),
                    p("We got the survey data a week late, so the analysis is behind.", "アンケートデータが 1 週間遅れて届いたので、分析が遅れています。"),
                ),
                tipJa = "come in late で「（データなどが）遅れて届く」。be behind は「予定より遅れている」です。",
                reply = p(
                    "I see. That's not your fault, but I've promised the client a summary at our meeting next Monday.",
                    "なるほど。それはあなたのせいではないけれど、来週月曜の打ち合わせでクライアントに概要を見せると約束しているの。",
                ),
            ),
            ScriptStep(
                taskJa = "締め切りを来週の水曜に延ばせないか提案しよう",
                contextJa = "データの分析にあと数日かかります。来週の水曜までなら仕上げられます。",
                mission = "propose",
                keywords = listOf("wednesday", "possible|could|can|would|push|move|extend*|extension|how about|what if"),
                answers = listOf(
                    p("Would it be possible to push the deadline to next Wednesday?", "締め切りを来週水曜に延ばすことは可能でしょうか？"),
                    p("Could I have until next Wednesday to finish it?", "来週水曜まで時間をいただけませんか？"),
                ),
                tipJa = "Would it be possible to 〜? は、無理をお願いするときのとても丁寧な言い方です。push the deadline to 〜 で「締め切りを〜に延ばす」。",
                reply = p("Wednesday is after the client meeting, though. What could I show them on Monday?", "でも、水曜だとクライアントとの打ち合わせの後になるわ。月曜には何を見せられる？"),
            ),
            ScriptStep(
                taskJa = "金曜までに主な結果だけは送れると伝えよう",
                contextJa = "全体は無理でも、主な分析結果だけなら金曜までにまとめられます。",
                mission = "reassure",
                keywords = listOf("friday", "key|main|summary|findings|results|draft|highlights"),
                answers = listOf(
                    p("I can send you the key findings by Friday, so you'll have something for the client on Monday.", "金曜までに主な結果をお送りできるので、月曜にはクライアントに見せるものがあります。"),
                    p("I could put together a summary of the main results by Friday.", "金曜までに主な結果の概要をまとめられます。"),
                ),
                tipJa = "put together で「（資料などを）まとめる」。so 〜 で相手にとってのメリットを付け加えると説得力が増します。",
                reply = p("Okay, that could work. But I need to be sure about Wednesday this time. Can you promise that?", "そうね、それならなんとかなりそう。でも今度こそ水曜は確実にしてほしいの。約束できる？"),
            ),
            ScriptStep(
                taskJa = "水曜は必ず守ると約束し、進み具合も報告すると伝えよう",
                contextJa = "データはもうそろっているので、水曜には確実に仕上げられます。",
                keywords = listOf("promise|definitely|absolutely|sure|guarantee|will|commit*", "wednesday|update*|report|let you know|keep you"),
                answers = listOf(
                    p("Yes, I promise. I'll keep you updated on my progress every day.", "はい、約束します。毎日進み具合を報告します。"),
                    p("Absolutely. You'll have the full report by Wednesday.", "もちろんです。水曜までにレポートを全部お渡しします。"),
                ),
                tipJa = "keep you updated で「随時ご報告します」。相手を安心させるビジネスの決まり文句です。",
                reply = p("Great, thanks for telling me early. That really helps. Let's go with that plan.", "ありがとう、早めに言ってくれて助かったわ。その計画でいきましょう。"),
            ),
        ),
    ),
    Script(
        scenarioId = "feedback",
        openerJa = "それで…どうでしたか？正直に言ってください。すごく緊張しました。",
        steps = listOf(
            ScriptStep(
                taskJa = "全体としてはよかったと伝えて、緊張をほぐそう",
                contextJa = "レオはとても緊張していて、自信をなくしかけています。",
                keywords = listOf("well|good|great|nice|job"),
                answers = listOf(
                    p("Overall, I thought it went really well.", "全体としては、とてもうまくいったと思うよ。"),
                    p("Honestly, you did a good job, especially for your first time.", "正直、よくやったよ。初めてにしては特に。"),
                ),
                tipJa = "Overall, 〜. で「全体としては〜」と、まず大きな印象から伝えられます。",
                reply = p("Really? Thanks. I was worried the client looked bored.", "本当ですか？よかった。お客さんが退屈そうに見えて心配だったんです。"),
            ),
            ScriptStep(
                taskJa = "調査がしっかりしていた点を具体的にほめよう",
                contextJa = "市場のデータをよく調べてあり、お客さんの質問にも実際の数字で答えられていました。",
                mission = "praise",
                keywords = listOf("research|data|numbers|content|prepared|preparation|questions"),
                answers = listOf(
                    p("Not at all. Your research was really solid, and you answered their questions with real numbers.", "そんなことないよ。調査がしっかりしていたし、質問にも実際の数字で答えていたね。"),
                    p("The client was impressed by your data. You were really well prepared.", "お客さんはデータに感心していたよ。とてもよく準備していたね。"),
                ),
                tipJa = "solid は「しっかりした・確かな」。ほめるときは、何がよかったかを具体的に言うと伝わります。",
                reply = p("That's good to hear. Was there anything I could do better?", "それを聞いて安心しました。もっとよくできたところはありますか？"),
            ),
            ScriptStep(
                taskJa = "話すのが速かったことを、やわらかく伝えよう",
                contextJa = "緊張のせいか早口で、お客さんがついていけていない場面がありました。",
                mission = "improve",
                keywords = listOf("fast|quick*|pace|speed|slow*", "one thing|maybe|might|could|work on|try|a bit|a little"),
                answers = listOf(
                    p("One thing you could work on is your pace. You spoke a little fast at times.", "改善できるとしたら話すペースかな。ときどき少し速かったよ。"),
                    p("Maybe try to slow down a bit. It was a little hard to follow in places.", "もう少しゆっくり話してみるといいかも。ところどころついていきにくかったから。"),
                ),
                tipJa = "One thing you could work on is 〜. は、改善点を前向きに伝える定番の言い方です。",
                reply = p("Hmm, I guess I rushed because there was so much to cover. Thirty slides is a lot.", "うーん、伝えることが多すぎて焦ったのかもしれません。スライドが 30 枚もありましたから。"),
            ),
            ScriptStep(
                taskJa = "スライドを減らせば、ゆっくり話せると提案しよう",
                contextJa = "大事なポイントに絞ってスライドを 10 枚くらいに減らせば、時間に余裕ができます。",
                keywords = listOf("slide*", "fewer|less|cut|reduce|ten|focus*|key|main"),
                answers = listOf(
                    p("That's true. If you cut it down to about ten slides, you'd have more time to explain each one.", "そうだね。スライドを 10 枚くらいに減らせば、1 枚ずつ説明する時間ができるよ。"),
                    p("What if you used fewer slides and focused on the key points?", "スライドを減らして、大事なポイントに絞ってみたらどうかな？"),
                ),
                tipJa = "If you 〜（過去形）, you'd 〜. の仮定法を使うと、押しつけずに提案できます。",
                reply = p("That makes sense. I just don't know which parts to cut.", "なるほど、そうですね。ただ、どこを削ればいいのかわからなくて。"),
            ),
            ScriptStep(
                taskJa = "次の発表の前に、一緒にスライドを見直そうと申し出よう",
                contextJa = "次の客先での発表は来週です。その前に時間を取って手伝ってあげたいと思っています。",
                mission = "support",
                keywords = listOf("together|help|go through|look at|review|sit down|with you", "slide*|before|next|week|tomorrow"),
                answers = listOf(
                    p("Why don't we go through your slides together before next week?", "来週までに、一緒にスライドを見直さない？"),
                    p("I'm happy to help. Let's sit down and look at them together tomorrow.", "喜んで手伝うよ。明日、一緒に座って見てみよう。"),
                ),
                tipJa = "Why don't we 〜? で「一緒に〜しない？」と、相手に寄り添った提案ができます。",
                reply = p("That would be amazing. Thank you. I really appreciate the honest feedback.", "それはすごく助かります。ありがとうございます。正直なフィードバック、本当にありがたいです。"),
            ),
        ),
    ),
    Script(
        scenarioId = "flight",
        openerJa = "次の方どうぞ。お待たせして申し訳ありません。どうされましたか？",
        steps = listOf(
            ScriptStep(
                taskJa = "東京行きの便が欠航になったと伝えよう",
                contextJa = "夕方 6 時発の東京行きの便です。",
                keywords = listOf("cancel*", "flight|tokyo"),
                answers = listOf(
                    p("Hi, my flight to Tokyo was just canceled.", "こんにちは、東京行きの便が欠航になってしまって。"),
                    p("My six o'clock flight to Tokyo got canceled.", "6 時の東京行きの便が欠航になりました。"),
                ),
                tipJa = "「欠航になった」は was canceled / got canceled。get を使うと口語的になります。",
                reply = p(
                    "I'm so sorry about that. The storm has canceled a lot of flights. Our next flight to Tokyo is tomorrow at six p.m.",
                    "大変申し訳ありません。嵐でたくさんの便が欠航になっています。当社の次の東京行きは、明日の夕方 6 時です。",
                ),
            ),
            ScriptStep(
                taskJa = "あさって大事な会議があるので、もっと早い便がいいと伝えよう",
                contextJa = "明日の夕方 6 時発だと、東京に着くのはあさっての夜。会議に間に合いません。",
                mission = "situation",
                keywords = listOf("meeting|important|business|work", "earlier|sooner|morning|other|anything|late|make it|miss"),
                answers = listOf(
                    p("That's too late. I have an important meeting in Tokyo the day after tomorrow. Is there anything earlier?", "それでは遅すぎます。あさって東京で大事な会議があるんです。もっと早い便はありませんか？"),
                    p("I'm afraid I'll miss an important meeting. Do you have anything earlier?", "大事な会議に間に合わなくなってしまいます。もっと早い便はありませんか？"),
                ),
                tipJa = "Is there anything earlier? で「もっと早いのはありませんか」と、条件を具体的に伝えられます。",
                reply = p("Let me look... On our airline, I'm afraid that's the earliest one. Everything else is full.", "お調べしますね…当社では、申し訳ありませんがそれが一番早い便です。ほかはすべて満席です。"),
            ),
            ScriptStep(
                taskJa = "ほかの航空会社の便に乗れないか聞こう",
                contextJa = "提携している航空会社の便なら、空いているかもしれません。",
                mission = "alternative",
                keywords = listOf("another|other|partner|different", "airline*|carrier*|company"),
                answers = listOf(
                    p("Is there any chance of getting a seat on another airline?", "ほかの航空会社の便に乗れる可能性はありませんか？"),
                    p("Could you check your partner airlines, too?", "提携している航空会社も確認してもらえますか？"),
                ),
                tipJa = "Is there any chance of 〜ing? は「〜できる見込みはありますか」と、遠慮がちに可能性を探る言い方です。",
                reply = p(
                    "Good idea. Let me check... Yes! There's one seat on a partner airline tomorrow at eleven a.m. Shall I book it for you?",
                    "そうですね。確認します…ありました！提携会社の明日午前 11 時の便に 1 席空きがあります。お取りしましょうか？",
                ),
            ),
            ScriptStep(
                taskJa = "その便をお願いして、今夜のホテルについて相談しよう",
                contextJa = "今夜はシカゴに泊まるしかありません。ホテル代を少しでも抑えたいところです。",
                mission = "compensation",
                keywords = listOf("yes|please|book|take|great", "hotel|stay|room|tonight"),
                answers = listOf(
                    p("Yes, please. Also, could you help me with a hotel for tonight?", "はい、お願いします。それと、今夜のホテルについて助けていただけますか？"),
                    p("That would be great. Will the airline cover a hotel for tonight?", "それは助かります。今夜のホテル代は航空会社で負担してもらえますか？"),
                ),
                tipJa = "cover は「（費用を）負担する」。Will the airline cover 〜? で補償があるか確認できます。",
                reply = p(
                    "Since it's a weather issue, we can't pay for the hotel, I'm afraid. But I can give you a discount rate at the airport hotel and a 20-dollar meal voucher.",
                    "天候が理由のため、申し訳ありませんがホテル代はお支払いできません。ですが、空港ホテルの割引料金と、20 ドルのお食事券をお出しできます。",
                ),
            ),
            ScriptStep(
                taskJa = "それで大丈夫と受け入れ、新しい便の時間を確認しよう",
                contextJa = "提携会社の、明日午前 11 時発の便です。",
                keywords = listOf("eleven|tomorrow|time|confirm|morning"),
                answers = listOf(
                    p("That's fine, thank you. Just to confirm, the new flight leaves at eleven tomorrow morning, right?", "それで大丈夫です、ありがとうございます。確認ですが、新しい便は明日の午前 11 時発ですよね？"),
                    p("Okay, I understand. So my flight is at 11 tomorrow?", "わかりました。では、私の便は明日の 11 時ですね？"),
                ),
                tipJa = "Just to confirm, 〜, right? で、大事な情報を最後に確認しましょう。",
                reply = p(
                    "That's right. Eleven a.m., gate B12. Please be here by nine. Here are your vouchers. Have a safe trip home.",
                    "その通りです。午前 11 時、B12 ゲートです。9 時までにお越しください。こちらがクーポンです。お気をつけてお帰りください。",
                ),
            ),
        ),
    ),
    Script(
        scenarioId = "fraud",
        openerJa = "不正利用窓口にお電話ありがとうございます。担当のケビンです。本日はどうされましたか？",
        steps = listOf(
            ScriptStep(
                taskJa = "身に覚えのない請求があると伝えよう",
                contextJa = "今朝、アプリでカードの明細を見て気づきました。",
                mission = "report",
                keywords = listOf("charge*|purchase*|payment*|transaction*", "didn't|don't|not mine|unknown|recognize|never"),
                answers = listOf(
                    p("Hi, I found some charges on my card that I didn't make.", "もしもし、カードに身に覚えのない請求があったんです。"),
                    p("There are some purchases on my statement that I don't recognize.", "明細に心当たりのない買い物があります。"),
                ),
                tipJa = "charges I didn't make / purchases I don't recognize で「身に覚えのない請求」を表せます。",
                reply = p("I'm sorry to hear that. Let's sort this out. Can you tell me about the charges?", "それはご心配ですね。解決しましょう。どんな請求か教えていただけますか？"),
            ),
            ScriptStep(
                taskJa = "ネットショップで 3 件、合計 450 ドルだと説明しよう",
                contextJa = "どれも聞いたことのないネットショップで、合計 450 ドルです。",
                keywords = listOf("three", "450|hundred|dollars|online|store|shop"),
                answers = listOf(
                    p("There are three online purchases, about 450 dollars in total.", "ネットでの買い物が 3 件で、合計 450 ドルくらいです。"),
                    p("Three charges from an online store I've never heard of. It's 450 dollars altogether.", "聞いたことのないネットショップからの請求が 3 件です。全部で 450 ドルです。"),
                ),
                tipJa = "in total や altogether で「合計で」。450 は four hundred fifty と読みます。",
                reply = p("Thank you. Do you still have the card with you? And when did you last use it?", "ありがとうございます。カードはまだお手元にありますか？最後に使ったのはいつですか？"),
            ),
            ScriptStep(
                taskJa = "カードは手元にあり、最後に使ったのは昨日のスーパーだと答えよう",
                contextJa = "カードは財布の中にあります。最後に使ったのは昨日、近所のスーパーでした。",
                keywords = listOf("have|wallet|with me", "yesterday|supermarket|grocery|last"),
                answers = listOf(
                    p("Yes, I still have it with me. I last used it yesterday at the supermarket.", "はい、手元にあります。最後に使ったのは昨日のスーパーです。"),
                    p("It's in my wallet. The last time I used it was yesterday, at a grocery store.", "財布の中にあります。最後に使ったのは昨日、食料品店でした。"),
                ),
                tipJa = "I last used it 〜. や The last time I used it was 〜. で「最後に使ったのは〜」と言えます。",
                reply = p("Okay. It sounds like your card details were stolen online. I recommend we block the card right away.", "わかりました。カードの情報がネットで盗まれた可能性があります。すぐにカードを止めることをおすすめします。"),
            ),
            ScriptStep(
                taskJa = "カードを止めて、新しいカードを送ってほしいと頼もう",
                contextJa = "毎日の買い物に使っているカードなので、新しいカードはできるだけ早くほしいところです。",
                mission = "block",
                keywords = listOf("block|cancel|stop|freeze", "new|replace*|another"),
                answers = listOf(
                    p("Yes, please. Could you block the card and send me a new one?", "はい、お願いします。カードを止めて、新しいのを送ってもらえますか？"),
                    p("Please cancel it and send me a replacement as soon as possible.", "止めて、できるだけ早く代わりのカードを送ってください。"),
                ),
                tipJa = "block / cancel the card で「カードを止める」。a replacement は「代わりの品」です。",
                reply = p("Done. The card is blocked, and a new one will arrive within five business days.", "完了しました。カードを停止しました。新しいカードは 5 営業日以内に届きます。"),
            ),
            ScriptStep(
                taskJa = "450 ドルはいつ戻ってくるか聞こう",
                contextJa = "来月の家賃の支払いがあるので、返金の時期が気になります。",
                mission = "refund",
                keywords = listOf("money|refund*|back|450|charges|dollars", "how long|when|time|take"),
                answers = listOf(
                    p("Thank you. How long will it take to get my money back?", "ありがとうございます。お金が戻ってくるまでどのくらいかかりますか？"),
                    p("When will the 450 dollars be refunded?", "450 ドルはいつ返金されますか？"),
                ),
                tipJa = "How long will it take to 〜? で、かかる時間をたずねられます。",
                reply = p(
                    "We'll investigate the charges, and you should get a refund within ten business days. You don't need to do anything else.",
                    "請求について調査し、10 営業日以内に返金される見込みです。お客様がほかにすることはありません。",
                ),
            ),
        ),
    ),
    Script(
        scenarioId = "noise",
        openerJa = "あ、どうも！下の階の人だよね？どうしたの？",
        steps = listOf(
            ScriptStep(
                taskJa = "邪魔してごめんと言って、少し話せるか聞こう",
                contextJa = "ジェイクとはあいさつする程度の仲です。いきなり苦情から入らないようにしましょう。",
                keywords = listOf("bother|disturb|sorry|minute|moment|second|talk"),
                answers = listOf(
                    p("Hi, sorry to bother you. Do you have a minute?", "こんにちは、お邪魔してごめんなさい。少しいいですか？"),
                    p("Hey, have you got a second? There's something I wanted to talk about.", "ちょっといい？話したいことがあって。"),
                ),
                tipJa = "Sorry to bother you. は、相手の時間をもらうときの丁寧な前置きです。",
                reply = p("Sure, what's going on?", "いいよ、どうしたの？"),
            ),
            ScriptStep(
                taskJa = "夜中の音楽が下の部屋までよく聞こえると、やわらかく伝えよう",
                contextJa = "夜中の 12 時過ぎに、大きな音楽が何度も聞こえてきました。責める言い方にならないようにしましょう。",
                mission = "raise",
                keywords = listOf("music|noise|sound|guitar|loud", "night|midnight|late|downstairs|hear|through"),
                answers = listOf(
                    p("I don't know if you're aware, but the music comes through quite clearly, especially after midnight.", "気づいていないかもしれないけど、特に夜 12 時過ぎは、音楽がかなりはっきり聞こえるんだ。"),
                    p("This is a bit awkward, but I can hear your music late at night.", "ちょっと言いにくいんだけど、夜遅くに音楽が聞こえるんだ。"),
                ),
                tipJa = "I don't know if you're aware, but 〜. で、相手を責めずに問題を伝えられます。",
                reply = p("Oh, really? I had no idea. But it's only on weekends, and it's not that loud, is it?", "えっ、本当？全然知らなかった。でも週末だけだし、そんなにうるさくないよね？"),
            ),
            ScriptStep(
                taskJa = "相手の事情を認めつつ、朝 5 時に起きることを説明しよう",
                contextJa = "演奏をやめてほしいわけではありません。ただ、週末も仕事で朝 5 時に起きなければなりません。",
                mission = "listen",
                keywords = listOf("understand|get it|know|see|totally|fair", "five|early|work|sleep|wake|get up"),
                answers = listOf(
                    p("I totally understand, and I don't want you to stop playing. It's just that I have to get up at five for work.", "本当にわかるし、演奏をやめてほしいわけじゃないんだ。ただ、仕事で 5 時に起きないといけなくて。"),
                    p("I get it, it's your home too. The problem is I start work very early, even on weekends.", "わかるよ、君の家でもあるしね。問題は、週末も仕事がすごく早く始まることなんだ。"),
                ),
                tipJa = "It's just that 〜. で「ただ〜というだけなんです」と、事情をやわらかく説明できます。",
                reply = p("Oh man, five? That's rough. I'm sorry. What would help?", "うわ、5 時？それはきついね。ごめん。どうしたらいい？"),
            ),
            ScriptStep(
                taskJa = "夜 10 時以降は音を小さくしてもらえないか頼もう",
                contextJa = "夜 10 時までなら、まったく問題ありません。",
                mission = "agree",
                keywords = listOf("ten", "down|quiet*|lower|turn|stop|headphone*"),
                answers = listOf(
                    p("Would you mind keeping it down after ten?", "夜 10 時以降は音を控えめにしてもらえるかな？"),
                    p("Maybe you could turn the music down after ten?", "10 時を過ぎたら、音楽を小さくしてもらえるとうれしいな。"),
                ),
                tipJa = "keep it down で「静かにする・音を抑える」。Would you mind 〜ing? で丁寧に頼めます。",
                reply = p(
                    "Yeah, that's fair. I'll keep it down after ten, and I'll use headphones for my guitar amp. Does that sound okay?",
                    "うん、それはもっともだね。10 時以降は静かにするし、ギターのアンプはヘッドホンにするよ。それでどう？",
                ),
            ),
            ScriptStep(
                taskJa = "お礼を言って、気持ちよく会話を終えよう",
                contextJa = "これからもいいご近所付き合いを続けたいと思っています。",
                keywords = listOf("thank*|appreciate|great|perfect|awesome"),
                answers = listOf(
                    p("That sounds perfect. Thanks for being so understanding.", "完ぺきだよ。わかってくれてありがとう。"),
                    p("Thanks, I really appreciate it. Let me know if I'm ever too noisy, too.", "ありがとう、本当に助かるよ。こっちがうるさいときも言ってね。"),
                ),
                tipJa = "Thanks for being so understanding. は「理解してくれてありがとう」。お願いを聞いてもらったあとの締めにぴったりです。",
                reply = p("No worries at all. Thanks for telling me in a nice way. Come up and listen to us play sometime!", "全然いいよ。感じよく言ってくれてありがとう。今度、演奏を聴きにおいでよ！"),
            ),
        ),
    ),
    Script(
        scenarioId = "movie",
        openerJa = "で、どうだった？あの結末、まだ頭の中で整理しきれてないよ！",
        steps = listOf(
            ScriptStep(
                taskJa = "映像がすばらしかったと、理由を添えて感想を言おう",
                contextJa = "宇宙のシーンがとても美しく、大きなスクリーンで見てよかったと思っています。",
                mission = "opinion",
                keywords = listOf("visual*|effects|picture*|scene*|space|look*|beautiful", "amazing|beautiful|stunning|great|incredible|loved|love|glad"),
                answers = listOf(
                    p("Honestly, the visuals were amazing. I'm glad we saw it on the big screen.", "正直、映像がすごかった。大きなスクリーンで見てよかったよ。"),
                    p("I loved the space scenes. They were so beautiful.", "宇宙のシーンが大好きだった。本当にきれいだったね。"),
                ),
                tipJa = "visuals は「映像」。I'm glad we 〜. で「〜してよかった」と気持ちを添えられます。",
                reply = p("Right? And that ending! I thought it was perfect. Didn't you love it?", "でしょ？それにあの結末！完ぺきだったと思う。すごくよくなかった？"),
            ),
            ScriptStep(
                taskJa = "結末は急ぎすぎに感じたと、感じよく反対しよう",
                contextJa = "主人公が急に惑星に残ると決めたのが、少し唐突に感じました。",
                mission = "disagree",
                keywords = listOf("ending|end", "rushed|sudden*|quick*|fast|not sure|but|felt"),
                answers = listOf(
                    p("I see what you mean, but the ending felt a bit rushed to me.", "言いたいことはわかるけど、私には結末が少し駆け足に感じたな。"),
                    p("Hmm, I'm not sure. The ending was a little sudden for me.", "うーん、どうかな。私には結末が少し唐突だった。"),
                ),
                tipJa = "I see what you mean, but 〜. は、相手の意見を受け止めてから反対する言い方です。to me / for me を付けると「私にとっては」と主観だと伝わります。",
                reply = p("Really? Why do you say that? I thought it made sense.", "そう？どうしてそう思うの？筋は通ってたと思うけど。"),
            ),
            ScriptStep(
                taskJa = "主人公が残ると決めた理由がよくわからなかったと説明しよう",
                contextJa = "地球にいる家族の話がずっと出ていたのに、最後に急に惑星に残ると決めたのが不思議でした。",
                keywords = listOf("why|reason|understand|decid*|choice|chose|family|explain*"),
                answers = listOf(
                    p("Well, the whole movie was about getting back to his family, so I didn't understand why he decided to stay.", "だって、映画全体が家族のもとに帰る話だったのに、なんで残ると決めたのかわからなかったんだ。"),
                    p("His choice to stay wasn't really explained. He kept talking about his family until the end.", "残るという選択がちゃんと説明されてなかった。最後まで家族の話をしていたのに。"),
                ),
                tipJa = "The whole movie was about 〜, so 〜. のように、理由を順に並べると説得力が出ます。",
                reply = p(
                    "Hmm, that's a fair point. Maybe they cut some scenes. I still loved it, though. What was your favorite part?",
                    "うーん、それはもっともだね。いくつかシーンをカットしたのかも。それでも私は大好きだったけどね。一番好きなシーンはどこだった？",
                ),
            ),
            ScriptStep(
                taskJa = "一番好きなシーンを話そう",
                contextJa = "どのシーンでもかまいません（例：ブラックホールのシーン、ロボットが冗談を言うシーンなど）。",
                open = true,
                answers = listOf(
                    p("What I liked most was the scene near the black hole. The music was incredible.", "一番好きだったのはブラックホールの近くのシーン。音楽がすごかった。"),
                    p("My favorite part was when the robot told jokes. It was so funny.", "好きだったのはロボットが冗談を言うところ。すごくおかしかった。"),
                ),
                tipJa = "What I liked most was 〜. で「一番よかったのは〜」と強調できます。",
                reply = p("Oh, me too! That scene gave me chills. We should watch something else together soon.", "あ、私も！あのシーンは鳥肌が立ったよ。また近いうちに何か一緒に見ようよ。"),
            ),
            ScriptStep(
                taskJa = "似た映画をすすめて、次に一緒に見ようと提案しよう",
                contextJa = "宇宙が舞台の別の映画で、ミアが好きそうなものを知っています。",
                mission = "recommend",
                keywords = listOf("should|recommend|watch|see|check out|how about|let's|why don't"),
                answers = listOf(
                    p("I know a great space movie you'd love. Why don't we watch it at my place next time?", "君が気に入りそうな宇宙の映画を知ってるよ。次はうちで見ない？"),
                    p("If you liked this, you should check out another space movie I know. Let's watch it next weekend.", "これが好きなら、私の知ってる別の宇宙の映画を見てみて。来週末に一緒に見ようよ。"),
                ),
                tipJa = "If you liked this, you should 〜. はおすすめの定番の形です。",
                reply = p("That sounds great! It's a plan, then. I'll bring the popcorn!", "いいね！じゃあ決まりだね。ポップコーンは私が持っていくね！"),
            ),
        ),
    ),
)
