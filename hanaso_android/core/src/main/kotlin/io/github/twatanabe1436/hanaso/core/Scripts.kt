package io.github.twatanabe1436.hanaso.core

// 台本モードの台本。シナリオ (Catalog) ごとに、日本語のお題・お手本・相手のセリフを決めておく。
// 相手のセリフは、学習者がお手本と少し違うことを言っても不自然にならないように書く。

private fun p(en: String, ja: String) = Phrase(en, ja)

// フリートークの回答例のレベル (A: A1-A2、B: B1-B2、C: C1-C2 の代表)
private fun a(en: String, ja: String) = Phrase(en, ja, Level.A2)

private fun b(en: String, ja: String) = Phrase(en, ja, Level.B1)

private fun c(en: String, ja: String) = Phrase(en, ja, Level.C1)

object Scripts {
    val all: Map<String, Script> = listOf(
        Script(
            scenarioId = "cafe",
            openerJa = "いらっしゃいませ、おはようございます！ご注文は何にしますか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "ラテの M サイズを注文しよう",
                    mission = "drink",
                    keywords = listOf(
                        "latte|coffee|cappuccino|tea|americano|mocha|espresso|drink",
                        "medium|small|large|tall|grande|venti|regular",
                    ),
                    answers = listOf(
                        p("Can I get a medium latte, please?", "ラテの M サイズをください。"),
                        p("I'd like a medium latte, please.", "ラテの M サイズをお願いします。"),
                        p("A medium latte, please.", "ラテの M をひとつください。"),
                    ),
                    tipJa = "Can I get 〜, please? は注文の定番。I want 〜 よりやわらかく聞こえます。",
                    reply = p(
                        "Sure! Would you like regular milk, or would you prefer oat or almond milk?",
                        "かしこまりました！ミルクは普通のものにしますか？それともオーツミルクかアーモンドミルクにしますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "オーツミルクに変えてもらおう",
                    mission = "custom",
                    keywords = listOf("oat|almond|soy|skim|nonfat|whole|regular"),
                    answers = listOf(
                        p("Could you make it with oat milk?", "オーツミルクにしてもらえますか？"),
                        p("Oat milk, please.", "オーツミルクでお願いします。"),
                        p("I'll have oat milk, please.", "オーツミルクにします。"),
                    ),
                    tipJa = "Could you make it with 〜? で「〜で作ってもらえますか？」とカスタマイズを頼めます。",
                    reply = p("No problem! Anything else for you today?", "かしこまりました！ほかにご注文はありますか？"),
                ),
                ScriptStep(
                    taskJa = "ブルーベリーマフィンも頼もう",
                    mission = "food",
                    keywords = listOf("muffin*|croissant*|bagel*|cookie*|sandwich*|scone*|cake*|donut*|doughnut*"),
                    answers = listOf(
                        p("I'll also have a blueberry muffin.", "ブルーベリーマフィンもお願いします。"),
                        p("Can I also get a blueberry muffin?", "ブルーベリーマフィンももらえますか？"),
                        p("A blueberry muffin, too, please.", "ブルーベリーマフィンもひとつください。"),
                    ),
                    tipJa = "also や too を使うと「〜も」と追加の注文になります。",
                    reply = p("Good choice! Is that for here or to go?", "いいですね！店内でお召し上がりですか、それともお持ち帰りですか？"),
                ),
                ScriptStep(
                    taskJa = "店内で食べると伝えよう",
                    keywords = listOf("here|go|takeout|take out|takeaway|eat in"),
                    answers = listOf(
                        p("For here, please.", "店内でお願いします。"),
                        p("I'll eat here.", "ここで食べます。"),
                    ),
                    tipJa = "店内なら For here, please.、持ち帰りなら To go, please. と言います。",
                    reply = p("Got it. That'll be eight fifty. How would you like to pay?", "かしこまりました。8 ドル 50 セントです。お支払いはどうされますか？"),
                ),
                ScriptStep(
                    taskJa = "カードで払うと伝えよう",
                    keywords = listOf("card|credit|cash|apple pay|phone|debit"),
                    answers = listOf(
                        p("By card, please.", "カードでお願いします。"),
                        p("Can I pay by card?", "カードで払えますか？"),
                        p("I'll pay with my credit card.", "クレジットカードで払います。"),
                    ),
                    tipJa = "by card（カードで）、in cash（現金で）で支払い方法を伝えられます。",
                    reply = p(
                        "Perfect. Your drink will be ready at the end of the counter. Have a great day!",
                        "ありがとうございます。お飲み物はカウンターの端でお渡しします。良い一日を！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "directions",
            openerJa = "あら、こんにちは！道に迷っているみたいですね。何かお探しですか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "美術館への行き方をたずねよう",
                    mission = "ask",
                    keywords = listOf("museum"),
                    answers = listOf(
                        p("Yes, please. How do I get to the art museum?", "はい、お願いします。美術館へはどう行けばいいですか？"),
                        p("Excuse me, where is the art museum?", "すみません、美術館はどこですか？"),
                        p("I'm looking for the art museum.", "美術館を探しています。"),
                    ),
                    tipJa = "How do I get to 〜? は道をたずねる定番フレーズです。",
                    reply = p(
                        "Sure! Go straight for two blocks, then turn left at the big bookstore. You'll see it on your right.",
                        "いいですよ！2 ブロックまっすぐ行って、大きな本屋のところで左に曲がってください。右側に見えますよ。",
                    ),
                ),
                ScriptStep(
                    taskJa = "歩いてどのくらいかかるか聞こう",
                    mission = "time",
                    keywords = listOf("how long|how far|far|minutes|long"),
                    answers = listOf(
                        p("How long does it take to walk there?", "歩いてどのくらいかかりますか？"),
                        p("Is it far from here?", "ここから遠いですか？"),
                        p("How far is it?", "どのくらいの距離ですか？"),
                    ),
                    tipJa = "時間は How long、距離は How far でたずねます。",
                    reply = p("It's about a ten-minute walk. Not too far.", "歩いて 10 分くらいです。そんなに遠くないですよ。"),
                ),
                ScriptStep(
                    taskJa = "道順をくり返して確認しよう（まっすぐ行って、本屋を左）",
                    mission = "confirm",
                    keywords = listOf("straight|block*", "left"),
                    answers = listOf(
                        p("So I go straight and turn left at the bookstore, right?", "まっすぐ行って、本屋で左ですね？"),
                        p("Straight for two blocks, then left at the bookstore?", "2 ブロックまっすぐ行って、本屋で左ですか？"),
                    ),
                    tipJa = "So 〜, right? で「つまり〜ですよね？」と聞いた内容を確認できます。",
                    reply = p("Exactly! You can also take bus number six. It's just two stops.", "その通り！6 番のバスに乗ってもいいですよ。2 つ目のバス停です。"),
                ),
                ScriptStep(
                    taskJa = "天気がいいので歩いて行くと伝えよう",
                    keywords = listOf("walk*"),
                    answers = listOf(
                        p("I think I'll walk. It's a nice day.", "歩いて行こうと思います。いい天気なので。"),
                        p("I'll walk there, thanks.", "歩いて行きます、ありがとう。"),
                    ),
                    tipJa = "I think I'll 〜. で「〜しようと思います」と、その場で決めたことを伝えられます。",
                    reply = p(
                        "Good idea! Enjoy the museum. The café on the top floor is really nice, too.",
                        "いいですね！美術館を楽しんでね。最上階のカフェもすごくいいですよ。",
                    ),
                ),
                ScriptStep(
                    taskJa = "お礼を言おう",
                    keywords = listOf("thank*|appreciate"),
                    answers = listOf(
                        p("Thank you so much for your help!", "助けてくれて本当にありがとうございます！"),
                        p("Thanks a lot. Have a nice day!", "ありがとうございます。良い一日を！"),
                    ),
                    tipJa = "Thank you for 〜. の for のあとに、お礼の理由を続けます。",
                    reply = p("You're welcome! Have a great time!", "どういたしまして！楽しんでくださいね！"),
                ),
            ),
        ),
        Script(
            scenarioId = "restaurant",
            openerJa = "こんばんは、いらっしゃいませ！メニューをどうぞ。まずはお飲み物はいかがですか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "飲み物を注文しよう（例：お水、オレンジジュース）",
                    keywords = listOf("water|juice|wine|beer|soda|coke|tea|coffee|lemonade|sparkling|cola|drink"),
                    answers = listOf(
                        p("Can I have an orange juice, please?", "オレンジジュースをいただけますか？"),
                        p("Just water, please.", "お水をお願いします。"),
                        p("I'd like a glass of red wine.", "赤ワインをグラスでお願いします。"),
                    ),
                    tipJa = "Can I have 〜? や I'd like 〜. で丁寧に注文できます。",
                    reply = p(
                        "Of course. I'll bring that right out. Are you ready to order, or do you need a few minutes?",
                        "かしこまりました。すぐにお持ちします。ご注文はお決まりですか？それとも少しお時間が必要ですか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "おすすめを聞こう",
                    mission = "recommend",
                    keywords = listOf("recommend*|special*|popular|best|good"),
                    answers = listOf(
                        p("What do you recommend?", "おすすめは何ですか？"),
                        p("What are today's specials?", "今日のおすすめ料理は何ですか？"),
                        p("What's popular here?", "ここでは何が人気ですか？"),
                    ),
                    tipJa = "What do you recommend? はレストランでとても便利なひとことです。",
                    reply = p(
                        "Today's specials are the seafood risotto and the mushroom pasta. The mushroom pasta is my favorite!",
                        "本日のおすすめはシーフードリゾットときのこのパスタです。きのこのパスタは私のお気に入りなんです！",
                    ),
                ),
                ScriptStep(
                    taskJa = "きのこのパスタを注文しよう",
                    mission = "order",
                    keywords = listOf("pasta|risotto|pizza|salad|steak|fish|chicken"),
                    answers = listOf(
                        p("I'd like the mushroom pasta, please.", "きのこのパスタをお願いします。"),
                        p("I'll have the mushroom pasta.", "きのこのパスタにします。"),
                        p("The mushroom pasta sounds great. I'll take it.", "きのこのパスタ、おいしそう。それにします。"),
                    ),
                    tipJa = "I'll have 〜. は「〜にします」と注文を決めるときの定番です。",
                    reply = p(
                        "Excellent choice. And would you like to try our tiramisu for dessert? It's famous here.",
                        "いい選択ですね。デザートに当店のティラミスはいかがですか？名物なんですよ。",
                    ),
                ),
                ScriptStep(
                    taskJa = "ティラミスもお願いしよう",
                    keywords = listOf("yes|sure|tiramisu|okay|ok|why not|sounds|please"),
                    answers = listOf(
                        p("Yes, I'll try the tiramisu.", "はい、ティラミスもお願いします。"),
                        p("Sure, that sounds good!", "ぜひ、おいしそうですね！"),
                    ),
                    tipJa = "That sounds good! は相手の提案に乗るときの便利なひとことです。",
                    reply = p("Wonderful! I'll be right back with your food.", "かしこまりました！すぐにお料理をお持ちします。"),
                ),
                ScriptStep(
                    taskJa = "（食事のあと）お会計を頼もう",
                    mission = "check",
                    keywords = listOf("check|bill"),
                    answers = listOf(
                        p("Could we get the check, please?", "お会計をお願いできますか？"),
                        p("Can I have the check, please?", "お会計をお願いします。"),
                        p("Check, please.", "お会計お願いします。"),
                    ),
                    tipJa = "アメリカでは check、イギリスでは bill がよく使われます。",
                    reply = p(
                        "Of course. I hope you enjoyed your meal. Have a lovely evening!",
                        "かしこまりました。お食事を楽しんでいただけたならうれしいです。すてきな夜をお過ごしください！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "immigration",
            openerJa = "こんにちは。パスポートをお願いします。渡航の目的は何ですか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "観光で来たと答えよう",
                    mission = "purpose",
                    keywords = listOf("sightseeing|vacation|holiday|business|tour*|travel*|visit*|study|conference|trip"),
                    answers = listOf(
                        p("I'm here for sightseeing.", "観光で来ました。"),
                        p("Sightseeing.", "観光です。"),
                        p("I'm here on vacation.", "休暇で来ました。"),
                    ),
                    tipJa = "I'm here for 〜. で目的を伝えます。仕事なら for business です。",
                    reply = p("How long are you staying?", "どのくらい滞在しますか？"),
                ),
                ScriptStep(
                    taskJa = "1 週間滞在すると答えよう",
                    mission = "stay",
                    keywords = listOf("week*|day*|night*|month*"),
                    answers = listOf(
                        p("I'm staying for one week.", "1 週間滞在します。"),
                        p("For a week.", "1 週間です。"),
                        p("About seven days.", "7 日間くらいです。"),
                    ),
                    tipJa = "期間は for one week / for five days のように for を使います。",
                    reply = p("And where will you be staying?", "どこに滞在しますか？"),
                ),
                ScriptStep(
                    taskJa = "ダウンタウンのホテルに泊まると答えよう",
                    mission = "stay",
                    keywords = listOf("hotel|hilton|friend*|airbnb|hostel|house|downtown|inn|apartment"),
                    answers = listOf(
                        p("I'm staying at the Hilton downtown.", "ダウンタウンのヒルトンに泊まります。"),
                        p("At a hotel downtown.", "ダウンタウンのホテルです。"),
                    ),
                    tipJa = "I'm staying at 〜. で滞在先を伝えます。ホテル名を言えるようにしておくと安心です。",
                    reply = p("What do you do for work?", "お仕事は何をしていますか？"),
                ),
                ScriptStep(
                    taskJa = "自分の職業を答えよう（例：エンジニア、会社員、学生）",
                    contextJa = "職業は自分のことでも、架空でもかまいません。会社員は office worker で通じます。",
                    mission = "job",
                    open = true,
                    answers = listOf(
                        p("I work as an engineer.", "エンジニアとして働いています。"),
                        p("I'm an office worker.", "会社員です。"),
                        p("I'm a student.", "学生です。"),
                    ),
                    tipJa = "I work as 〜. / I'm a(n) 〜. で職業を伝えます。会社員は office worker で通じます。",
                    reply = p("Okay. Do you have a return ticket?", "わかりました。帰りの航空券はありますか？"),
                ),
                ScriptStep(
                    taskJa = "帰りの航空券があると答えよう",
                    keywords = listOf("yes|yeah|here|have|sure"),
                    answers = listOf(
                        p("Yes, here it is.", "はい、こちらです。"),
                        p("Yes, I have a return ticket.", "はい、帰りの航空券があります。"),
                        p("Yes, I'm flying back next Sunday.", "はい、来週の日曜日に帰ります。"),
                    ),
                    tipJa = "書類を渡すときは Here it is.（はい、どうぞ）と言います。",
                    reply = p(
                        "Thank you. Everything looks good. Enjoy your stay in San Francisco!",
                        "ありがとうございます。問題ありません。サンフランシスコでの滞在を楽しんでください！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "hotel",
            openerJa = "こんにちは、ハーバービューホテルへようこそ。本日チェックインですか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "田中の名前で予約していると伝えよう",
                    mission = "checkin",
                    keywords = listOf("reservation|booking|booked|reserved|book|check in|checking in|under"),
                    answers = listOf(
                        p("Yes, I have a reservation under the name Tanaka.", "はい、田中の名前で予約しています。"),
                        p("Yes, I booked a room under Tanaka.", "はい、田中で部屋を予約しました。"),
                    ),
                    tipJa = "under the name 〜 で「〜の名前で」。予約は reservation でも booking でも OK。",
                    reply = p(
                        "Let me check... Yes, I have it here. Three nights in a double room. May I see your passport, please?",
                        "確認しますね…はい、ございました。ダブルルームで 3 泊ですね。パスポートを拝見できますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "パスポートを渡そう",
                    keywords = listOf("here|sure|of course|passport"),
                    answers = listOf(
                        p("Sure, here you go.", "はい、どうぞ。"),
                        p("Of course. Here's my passport.", "もちろんです。パスポートです。"),
                    ),
                    tipJa = "物を渡すときは Here you go. / Here it is. がよく使われます。",
                    reply = p(
                        "Thank you. You're in room 512. Is there anything else I can help you with?",
                        "ありがとうございます。お部屋は 512 号室です。ほかに何かお手伝いできることはありますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "朝食の時間と場所を聞こう",
                    mission = "breakfast",
                    keywords = listOf("breakfast"),
                    answers = listOf(
                        p("What time is breakfast served?", "朝食は何時からですか？"),
                        p("Where can I have breakfast?", "朝食はどこで食べられますか？"),
                        p("What time and where is breakfast?", "朝食は何時に、どこでありますか？"),
                    ),
                    tipJa = "What time is breakfast served? の served は「（食事が）出される」という意味です。",
                    reply = p(
                        "Breakfast is from six thirty to ten on the second floor. Enjoy your stay!",
                        "朝食は 6 時半から 10 時まで、2 階でご用意しています。ごゆっくりお過ごしください！",
                    ),
                ),
                ScriptStep(
                    taskJa = "（部屋からフロントに電話して）エアコンが動かないと伝えよう",
                    contextJa = "部屋（512 号室）に入ったら、エアコンが動きません。フロントに電話しています。",
                    keywords = listOf(
                        "air conditioner|air conditioning|ac|a c|aircon|air con|heater",
                        "work*|broken|not|problem",
                    ),
                    answers = listOf(
                        p("Hi, the air conditioner in my room isn't working.", "もしもし、部屋のエアコンが動かないんです。"),
                        p("Excuse me, the air conditioning in room 512 is broken.", "すみません、512 号室のエアコンが壊れています。"),
                    ),
                    tipJa = "〜 isn't working. は「〜が動かない・調子が悪い」と伝える便利な表現です。",
                    reply = p(
                        "Oh, I'm so sorry about that. I can send someone from maintenance, or I can move you to another room. Which would you prefer?",
                        "大変申し訳ございません。係の者を向かわせることも、別のお部屋にご案内することもできます。どちらがよろしいですか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "部屋を替えてもらえるか頼もう",
                    contextJa = "修理を待つより、別の部屋に移りたいと思っています。",
                    mission = "problem",
                    keywords = listOf("room*|move|change|switch"),
                    answers = listOf(
                        p("Would it be possible to change rooms?", "部屋を替えてもらうことはできますか？"),
                        p("Could I move to another room, please?", "別の部屋に移ってもいいですか？"),
                    ),
                    tipJa = "Would it be possible to 〜? はとても丁寧なお願いの言い方です。",
                    reply = p(
                        "Of course. I'll move you to room 804 right away. It has a great view of the harbor. Sorry again for the trouble!",
                        "かしこまりました。すぐに 804 号室にご案内します。港がよく見えるお部屋ですよ。ご迷惑をおかけして申し訳ありませんでした！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "shopping",
            openerJa = "こんにちは、本日はどうされましたか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "セーターが小さすぎたので交換したいと伝えよう",
                    mission = "reason",
                    keywords = listOf("exchange|return|swap|change|refund", "small|tight|size|fit*"),
                    answers = listOf(
                        p("I'd like to exchange this sweater. It's too small.", "このセーターを交換したいです。小さすぎました。"),
                        p("Hi, I bought this sweater, but it's too small. Can I exchange it?", "このセーターを買ったのですが、小さすぎました。交換できますか？"),
                    ),
                    tipJa = "too small の too は「〜すぎる」。exchange は交換、return は返品です。",
                    reply = p("I'm sorry about that. Do you have your receipt with you?", "申し訳ございません。レシートはお持ちですか？"),
                ),
                ScriptStep(
                    taskJa = "レシートを渡そう",
                    keywords = listOf("here|yes|receipt|sure"),
                    answers = listOf(
                        p("Yes, here's my receipt.", "はい、レシートです。"),
                        p("Sure, here it is.", "はい、こちらです。"),
                    ),
                    tipJa = "Here's 〜. で「はい、〜です」と物を差し出せます。",
                    reply = p("Thank you. Which size would you like?", "ありがとうございます。どのサイズがよろしいですか？"),
                ),
                ScriptStep(
                    taskJa = "M サイズがあるか聞こう",
                    keywords = listOf("medium|large|bigger|larger"),
                    answers = listOf(
                        p("Do you have it in a medium?", "M サイズはありますか？"),
                        p("Can I get a medium instead?", "代わりに M サイズをもらえますか？"),
                    ),
                    tipJa = "Do you have it in 〜? で「〜（サイズ・色）はありますか？」とたずねられます。",
                    reply = p(
                        "Let me check... I'm sorry, we're out of medium in gray. But we have it in navy, or I can give you a full refund.",
                        "確認しますね…申し訳ございません、グレーの M サイズは在庫切れです。ネイビーならございますし、全額返金することもできます。",
                    ),
                ),
                ScriptStep(
                    taskJa = "ネイビーのものを見せてもらおう",
                    contextJa = "グレーの M サイズは品切れ。同じセーターのネイビーならあると言われました。",
                    mission = "option",
                    keywords = listOf("navy|see|look|color|colour|show"),
                    answers = listOf(
                        p("Could I see the navy one?", "ネイビーのものを見せてもらえますか？"),
                        p("Can you show me the navy one?", "ネイビーのを見せてくれますか？"),
                    ),
                    tipJa = "the navy one の one は「もの」。同じ物の名前をくり返さずに言えます。",
                    reply = p("Sure, here it is. It's the same sweater, just in navy.", "はい、こちらです。同じセーターで、色がネイビーなだけです。"),
                ),
                ScriptStep(
                    taskJa = "考えた結果、返金にしてもらうと伝えよう",
                    contextJa = "ネイビーも見てみましたが、やっぱり返金にしたいと思いました。",
                    mission = "decide",
                    keywords = listOf("refund|money back"),
                    answers = listOf(
                        p("I think I'll just get a refund, then.", "それなら返金にしてもらいます。"),
                        p("Hmm, I'd prefer a refund, please.", "うーん、返金でお願いします。"),
                    ),
                    tipJa = "I think I'll 〜, then. で「それなら〜にします」と、決めたことをやわらかく伝えられます。",
                    reply = p(
                        "No problem. The refund will go back to your card in three to five days. Thanks for coming in!",
                        "かしこまりました。返金は 3〜5 日でカードに戻ります。ご来店ありがとうございました！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "party",
            openerJa = "やあ！まだ会ったことないよね。クリスだよ。エミリーとはどういう知り合い？",
            steps = listOf(
                ScriptStep(
                    taskJa = "自己紹介して、エミリーとは同僚だと伝えよう",
                    contextJa = "名前は自分の名前でどうぞ。エミリーとは同じ職場で働いています。",
                    mission = "intro",
                    keywords = listOf(
                        "i am|my name|name is|nice to meet|this is",
                        "work*|colleague*|coworker*|friend*|college|school|together|know",
                    ),
                    answers = listOf(
                        p("Nice to meet you, Chris. I'm Ken. Emily and I work together.", "はじめまして、クリス。ケンです。エミリーとは同僚なんです。"),
                        p("Hi, I'm Ken. I work with Emily.", "やあ、ケンです。エミリーと一緒に働いています。"),
                    ),
                    tipJa = "Emily and I work together. で「エミリーと私は一緒に働いている＝同僚」と言えます。名前は自分の名前でどうぞ。",
                    reply = p("Oh, cool! Nice to meet you. I'm a graphic designer. What do you do for fun?", "へえ、いいね！よろしく。僕はグラフィックデザイナーなんだ。休みの日は何してるの？"),
                ),
                ScriptStep(
                    taskJa = "自分の趣味を話そう（例：ハイキング、料理、映画）",
                    mission = "common",
                    open = true,
                    answers = listOf(
                        p("I like hiking. I go to the mountains almost every month.", "ハイキングが好きです。ほぼ毎月山に行きます。"),
                        p("I love cooking and watching movies.", "料理と映画を見るのが大好きです。"),
                    ),
                    tipJa = "I like 〜. のあとに頻度や理由を 1 文足すと、会話が広がります。",
                    reply = p(
                        "Oh, that's awesome! I'm really into hiking and trying new restaurants. Do you have any favorite places around here?",
                        "へえ、いいね！僕はハイキングと新しいレストラン巡りにはまってるんだ。この辺でお気に入りの場所はある？",
                    ),
                ),
                ScriptStep(
                    taskJa = "お気に入りの場所を 1 つ教えよう",
                    open = true,
                    answers = listOf(
                        p("There's a great ramen place near the station.", "駅の近くにおいしいラーメン屋があります。"),
                        p("I really like the park by the river.", "川沿いの公園がすごく好きです。"),
                    ),
                    tipJa = "There's 〜 near …. で「…の近くに〜がある」と場所を紹介できます。",
                    reply = p("Oh, I've never been there! I'd love to check it out.", "へえ、行ったことないな！ぜひ行ってみたいよ。"),
                ),
                ScriptStep(
                    taskJa = "今度一緒に行こうと誘おう",
                    mission = "contact",
                    keywords = listOf("together|sometime|hang out|join|we should|let's|want to|go there"),
                    answers = listOf(
                        p("We should go together sometime!", "今度一緒に行こうよ！"),
                        p("Do you want to go there together next weekend?", "来週末一緒に行かない？"),
                    ),
                    tipJa = "We should 〜 sometime! は「今度〜しようよ」と気軽に誘う定番フレーズです。",
                    reply = p("Definitely! Let's do it. What's the best way to reach you?", "ぜひ！行こう。連絡はどうするのが一番いい？"),
                ),
                ScriptStep(
                    taskJa = "連絡先（電話番号や Instagram など）を交換しよう",
                    mission = "contact",
                    keywords = listOf("instagram|number*|phone|line|email|whatsapp|text|message|facebook|contact"),
                    answers = listOf(
                        p("Let's exchange numbers.", "電話番号を交換しよう。"),
                        p("Are you on Instagram? I'll follow you.", "インスタやってる？フォローするね。"),
                    ),
                    tipJa = "Let's exchange 〜. で「〜を交換しよう」。Are you on Instagram? で「インスタやってる？」と聞けます。",
                    reply = p("Sure! Here's my phone. Just type yours in. It was really great meeting you!", "もちろん！はい、僕のスマホ。入れておいて。会えて本当によかったよ！"),
                ),
            ),
        ),
        Script(
            scenarioId = "weekend",
            openerJa = "やあ！元気？今週末は何か予定ある？",
            steps = listOf(
                ScriptStep(
                    taskJa = "特にないと答えて、土曜日に映画に行こうと誘おう",
                    mission = "suggest",
                    keywords = listOf("movie*|film*|cinema"),
                    answers = listOf(
                        p("Not really. Do you want to see a movie on Saturday?", "特にないよ。土曜日に映画を見に行かない？"),
                        p("Nothing special. How about a movie on Saturday?", "特に何も。土曜に映画はどう？"),
                    ),
                    tipJa = "Do you want to 〜? は友達を誘うときのカジュアルな言い方です。",
                    reply = p("Ooh, I'd love to! Saturday afternoon works for me. What time is good for you?", "おー、いいね！土曜の午後なら空いてるよ。何時がいい？"),
                ),
                ScriptStep(
                    taskJa = "2 時に会おうと提案しよう",
                    mission = "when",
                    keywords = listOf("one|two|three|four|five|six|o'clock|pm|noon|afternoon|thirty"),
                    answers = listOf(
                        p("How about meeting at two?", "2 時に会うのはどう？"),
                        p("Let's meet at two o'clock.", "2 時に会おう。"),
                    ),
                    tipJa = "How about 〜ing? で「〜するのはどう？」と提案できます。",
                    reply = p("Perfect, that works for me. Where should we meet?", "ばっちり、その時間で大丈夫。どこで待ち合わせる？"),
                ),
                ScriptStep(
                    taskJa = "駅の前で待ち合わせしようと言おう",
                    mission = "where",
                    keywords = listOf("station|front|theater|theatre|entrance|exit|gate"),
                    answers = listOf(
                        p("Let's meet in front of the station.", "駅の前で待ち合わせしよう。"),
                        p("How about the station? The north exit.", "駅はどう？北口で。"),
                    ),
                    tipJa = "in front of 〜 で「〜の前で」。待ち合わせ場所の定番表現です。",
                    reply = p("Sounds good. Do you want to grab something to eat after the movie?", "いいね。映画のあと何か食べに行く？"),
                ),
                ScriptStep(
                    taskJa = "賛成して、ラーメンを提案しよう",
                    contextJa = "テイラーは、映画のあとに何か食べに行こうと誘っています。",
                    keywords = listOf("yes|sure|sounds|good|great|love|ramen|pizza|sushi"),
                    answers = listOf(
                        p("Sure! How about ramen?", "いいね！ラーメンはどう？"),
                        p("Sounds great! I know a good ramen place.", "いいね！おいしいラーメン屋を知ってるよ。"),
                    ),
                    tipJa = "Sounds great! は相手の提案に賛成するときの万能フレーズです。",
                    reply = p("Yes, I love ramen! Okay, see you on Saturday!", "やった、ラーメン大好き！じゃあ土曜日にね！"),
                ),
                ScriptStep(
                    taskJa = "「じゃあそのときに」と言って電話を切ろう",
                    keywords = listOf("see you|bye|then|later|saturday"),
                    answers = listOf(
                        p("Sounds good! See you then.", "いいね！じゃあそのときに。"),
                        p("See you on Saturday. Bye!", "土曜日にね。バイバイ！"),
                    ),
                    tipJa = "See you then. は「じゃあそのときに」。予定を決めたあとの別れのあいさつです。",
                    reply = p("Bye! I can't wait!", "バイバイ！楽しみにしてるね！"),
                ),
            ),
        ),
        Script(
            scenarioId = "doctor",
            openerJa = "こんにちは、医師のリーです。今日はどうされましたか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "喉が痛くて熱があると伝えよう",
                    mission = "symptom",
                    keywords = listOf("throat|fever|temperature|cough*|headache|pain|hurt*|sick|cold"),
                    answers = listOf(
                        p("I have a sore throat and a fever.", "喉が痛くて熱があります。"),
                        p("My throat hurts, and I think I have a fever.", "喉が痛くて、熱があるみたいです。"),
                    ),
                    tipJa = "症状は I have a 〜. で伝えます。sore throat（喉の痛み）、fever（熱）、headache（頭痛）。",
                    reply = p("I'm sorry to hear that. When did it start?", "それはおつらいですね。いつからですか？"),
                ),
                ScriptStep(
                    taskJa = "昨日の朝からだと答えよう",
                    mission = "since",
                    keywords = listOf("yesterday|day*|morning|night|last|ago|since|week"),
                    answers = listOf(
                        p("It started yesterday morning.", "昨日の朝からです。"),
                        p("Since yesterday morning.", "昨日の朝からです。"),
                    ),
                    tipJa = "It started 〜. / Since 〜. で、症状が始まった時を伝えます。",
                    reply = p("I see. Do you have any allergies to medicine?", "わかりました。薬のアレルギーはありますか？"),
                ),
                ScriptStep(
                    taskJa = "ペニシリンにアレルギーがあると伝えよう",
                    contextJa = "医師に薬のアレルギーを聞かれました。あなたはペニシリンにアレルギーがあります。",
                    keywords = listOf("allerg*|penicillin"),
                    answers = listOf(
                        p("I'm allergic to penicillin.", "ペニシリンにアレルギーがあります。"),
                        p("Yes, I have a penicillin allergy.", "はい、ペニシリンアレルギーがあります。"),
                    ),
                    tipJa = "I'm allergic to 〜. で「〜にアレルギーがある」。旅行前に覚えておくと安心です。",
                    reply = p(
                        "Thank you for telling me. It looks like a common cold. I'll give you a mild medicine that's safe for you. Get plenty of rest and drink lots of water.",
                        "教えてくれてありがとうございます。普通の風邪のようですね。あなたに合う弱めの薬を出しておきます。しっかり休んで、水分をたくさんとってください。",
                    ),
                ),
                ScriptStep(
                    taskJa = "薬をどのくらいの頻度で飲めばいいか聞こう",
                    mission = "question",
                    keywords = listOf("how often|how many|times|when should|how much"),
                    answers = listOf(
                        p("How often should I take this medicine?", "この薬はどのくらいの頻度で飲めばいいですか？"),
                        p("How many times a day should I take it?", "1 日に何回飲めばいいですか？"),
                    ),
                    tipJa = "How often 〜? で「どのくらいの頻度で〜？」。薬を「飲む」は drink ではなく take です。",
                    reply = p("Take one tablet three times a day, after meals.", "1 日 3 回、食後に 1 錠ずつ飲んでください。"),
                ),
                ScriptStep(
                    taskJa = "来週、飛行機に乗っても大丈夫か聞こう",
                    contextJa = "来週、飛行機で日本に帰る予定です。",
                    keywords = listOf("fly|flight|plane|airplane|travel*|trip"),
                    answers = listOf(
                        p("Is it okay to fly next week?", "来週飛行機に乗っても大丈夫ですか？"),
                        p("Can I still travel by plane next week?", "来週、飛行機で移動しても大丈夫ですか？"),
                    ),
                    tipJa = "Is it okay to 〜? で「〜しても大丈夫ですか？」と確認できます。",
                    reply = p(
                        "Yes, you should feel much better in a few days. Take care, and have a safe trip!",
                        "はい、数日でかなり良くなるはずですよ。お大事に。気をつけて旅行してくださいね！",
                    ),
                ),
            ),
        ),
        Script(
            scenarioId = "coworker",
            openerJa = "おはよう！うーん、月曜日だね。週末はどうだった？",
            steps = listOf(
                ScriptStep(
                    taskJa = "自分の週末について話そう（何をしたか・どうだったか）",
                    contextJa = "例：家でゆっくりした、買い物に行った、友達と会った。自分のことでも想像でもかまいません。",
                    mission = "weekend",
                    open = true,
                    answers = listOf(
                        p("It was pretty relaxing, actually. I just stayed home and watched movies.", "実はけっこうのんびりできたよ。家で映画を見てたんだ。"),
                        p("It was great! I went shopping with my family.", "最高だったよ！家族と買い物に行ったんだ。"),
                    ),
                    tipJa = "It was 〜. で感想を、went / watched など過去形で何をしたかを言うと、会話が続けやすくなります。",
                    reply = p(
                        "Nice! That sounds good. I went camping with some friends. I'm still a little tired, though.",
                        "いいね！よさそう。僕は友達とキャンプに行ったんだ。まだちょっと疲れてるけどね。",
                    ),
                ),
                ScriptStep(
                    taskJa = "どこにキャンプに行ったか質問しよう",
                    mission = "follow",
                    keywords = listOf("where|which|camp*"),
                    answers = listOf(
                        p("Oh nice, where did you go camping?", "いいね、どこにキャンプに行ったの？"),
                        p("Camping? Where did you go?", "キャンプ？どこに行ったの？"),
                    ),
                    tipJa = "相手の話に質問を返すと会話が広がります。Where did you go 〜ing? で「どこに〜しに行ったの？」",
                    reply = p("We went to a lake about two hours from the city. The stars at night were amazing!", "街から 2 時間くらいの湖に行ったんだ。夜の星がすごかったよ！"),
                ),
                ScriptStep(
                    taskJa = "「楽しそう！」とリアクションしよう",
                    keywords = listOf("fun|amazing|great|awesome|nice|cool|wow|beautiful|wonderful|jealous|sounds"),
                    answers = listOf(
                        p("That sounds like fun!", "楽しそう！"),
                        p("Wow, that sounds amazing! I'm jealous.", "わあ、すごそう！うらやましい。"),
                    ),
                    tipJa = "That sounds like fun! / That sounds amazing! はリアクションの定番です。",
                    reply = p("It really was. You should come with us next time!", "本当に楽しかったよ。今度一緒に来なよ！"),
                ),
                ScriptStep(
                    taskJa = "「ぜひ行きたい！」と答えよう",
                    keywords = listOf("love|like|sure|yes|definitely|great|count me in|want|would|of course|absolutely|why not|sounds"),
                    answers = listOf(
                        p("I'd love to!", "ぜひ行きたい！"),
                        p("Sure, count me in!", "いいね、私も入れて！"),
                    ),
                    tipJa = "I'd love to! は誘いに「ぜひ！」と答える一番自然な言い方です。",
                    reply = p("Awesome! Oh, look at the time. I have a meeting at nine.", "やった！あ、もうこんな時間。9 時から会議なんだ。"),
                ),
                ScriptStep(
                    taskJa = "「仕事に戻らなきゃ、またあとで」と会話を切り上げよう",
                    contextJa = "相手は 9 時から会議。あなたもそろそろ仕事に戻る時間です。",
                    mission = "close",
                    keywords = listOf("later|bye|back|anyway|see you|go"),
                    answers = listOf(
                        p("Anyway, I'd better get back to work. Talk later!", "さて、そろそろ仕事に戻らなきゃ。またあとで！"),
                        p("Me too. See you later!", "私も。またあとでね！"),
                    ),
                    tipJa = "Anyway, I'd better 〜. で「さて、そろそろ〜しなきゃ」と自然に会話を切り上げられます。",
                    reply = p("Yeah, talk later! Have a good one!", "うん、またあとで！いい一日を！"),
                ),
            ),
        ),
        Script(
            scenarioId = "meeting",
            openerJa = "皆さん、参加ありがとうございます。新しい検索機能を来週の月曜日にリリースしたいと思っています。どう思いますか？何か懸念はありますか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "テストが足りないという懸念を、理由と一緒に伝えよう",
                    contextJa = "テストはまだ半分ほどしか終わっていません。このまま来週出すと、バグが残るかもしれません。",
                    mission = "opinion",
                    keywords = listOf(
                        "test*|bug*|quality|risk*|ready",
                        "concern*|worried|worry|not sure|think|afraid|because|so|since",
                    ),
                    answers = listOf(
                        p("Honestly, I'm a bit concerned. We haven't finished testing yet, so there might be some bugs.", "正直、少し心配です。まだテストが終わっていないので、バグがあるかもしれません。"),
                        p("I think it's too early, because we still need more time for testing.", "テストにまだ時間が必要なので、早すぎると思います。"),
                    ),
                    tipJa = "I'm a bit concerned. で懸念をやわらかく伝え、so や because で理由をつなげましょう。",
                    reply = p(
                        "I hear you, but the marketing team has already planned a campaign for next week. Delaying it would be a big problem.",
                        "言いたいことはわかります。でも、マーケティングチームがもう来週のキャンペーンを計画しているんです。延期すると大きな問題になります。",
                    ),
                ),
                ScriptStep(
                    taskJa = "相手の意見を認めつつ、丁寧に反論しよう",
                    contextJa = "来週のキャンペーンの事情はわかります。でも、バグのある機能を出すとユーザーの信頼を失います。",
                    mission = "disagree",
                    keywords = listOf(
                        "but|however|although|though|still|that said",
                        "understand|see|point|true|know|agree|right",
                    ),
                    answers = listOf(
                        p("I see your point, but releasing a buggy feature could hurt our users' trust.", "おっしゃることはわかりますが、バグのある機能を出すとユーザーの信頼を損ないかねません。"),
                        p("I understand, but I think quality should come first.", "わかりますが、品質を優先すべきだと思います。"),
                    ),
                    tipJa = "I see your point, but 〜. は相手を立てながら反対意見を言う定番の形です。",
                    reply = p("Fair enough. So what do you suggest? We can't just wait another month.", "なるほど。では何を提案しますか？さすがにもう 1 か月待つわけにはいきません。"),
                ),
                ScriptStep(
                    taskJa = "まず一部のユーザーに公開する、という代案を出そう",
                    contextJa = "全員に出す前に、一部のユーザー（例：10%）にベータ版として先に出す案です。",
                    mission = "propose",
                    keywords = listOf(
                        "what if|how about|could we|suggest*|propose|why don't we|maybe we|we could|let's",
                        "small|some|group|beta|percent|first|few|limited|part",
                    ),
                    answers = listOf(
                        p("What if we released it to a small group of users first?", "まず一部のユーザーに公開するのはどうでしょう？"),
                        p("How about a beta release for ten percent of users?", "ユーザーの 10 パーセントにベータ版を出すのはどうでしょう？"),
                    ),
                    tipJa = "What if we 〜（過去形）? で「〜してみてはどうでしょう」と控えめに提案できます。",
                    reply = p(
                        "Hmm, interesting. That way, marketing can still start the campaign. How long would the beta need?",
                        "うーん、面白いですね。それならマーケティングもキャンペーンを始められる。ベータ期間はどれくらい必要ですか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "2 週間ほしいと、理由を添えて答えよう",
                    contextJa = "2 週間あれば、大きなバグを直して、ユーザーの声も集められます。",
                    keywords = listOf("week*|days|month*"),
                    answers = listOf(
                        p("I think two weeks would be enough to fix any major issues.", "大きな問題を直すには、2 週間あれば十分だと思います。"),
                        p("About two weeks. That gives us time to collect feedback and fix bugs.", "2 週間くらいです。フィードバックを集めてバグを直す時間ができます。"),
                    ),
                    tipJa = "That gives us time to 〜. で「それで〜する時間ができる」と理由を補足できます。",
                    reply = p(
                        "Okay, I can live with that. Let's do a beta next Monday and a full release in two weeks. Can you share a test plan by Wednesday?",
                        "わかりました、それならいいでしょう。来週月曜にベータ、2 週間後に正式リリースにしましょう。水曜までにテスト計画を共有してもらえますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "水曜までに共有すると引き受けよう",
                    contextJa = "テスト計画を、水曜までにチーム全員に送ればよい状況です。",
                    keywords = listOf("sure|yes|of course|will|absolutely|no problem|can do|definitely|okay|ok"),
                    answers = listOf(
                        p("Sure, I'll send it to everyone by Wednesday.", "はい、水曜までに皆さんに送ります。"),
                        p("Absolutely. I'll have it ready by Wednesday.", "もちろんです。水曜までに用意します。"),
                    ),
                    tipJa = "by Wednesday は「水曜までに（期限）」。until（〜までずっと）との違いに注意しましょう。",
                    reply = p("Perfect. Thanks, everyone. Great discussion!", "完ぺきです。皆さんありがとう。いい議論でした！"),
                ),
            ),
        ),
        Script(
            scenarioId = "interview",
            openerJa = "こんにちは、本日はお時間をいただきありがとうございます。まずは簡単に自己紹介をお願いできますか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "経歴を簡潔に自己紹介しよう（仕事と経験年数）",
                    contextJa = "今の仕事・経験年数・得意な分野など。架空の経歴でもかまいません。",
                    mission = "self",
                    open = true,
                    answers = listOf(
                        p("Sure. I've been working in sales for five years, mainly with clients in the IT industry.", "はい。5 年間営業をしていて、主に IT 業界のお客様を担当しています。"),
                        p("I'm a software engineer with three years of experience in web development.", "Web 開発で 3 年の経験があるソフトウェアエンジニアです。"),
                    ),
                    tipJa = "I've been working in 〜 for … years. で経験の長さを伝えられます。",
                    reply = p("Great, thank you. What would you say is your greatest strength?", "ありがとうございます。あなたの一番の強みは何だと思いますか？"),
                ),
                ScriptStep(
                    taskJa = "自分の強みを話そう",
                    contextJa = "例：覚えが早い、人と協力するのが得意、計画を立てるのが得意。",
                    open = true,
                    answers = listOf(
                        p("One of my strengths is that I'm a quick learner.", "私の強みのひとつは、覚えが早いことです。"),
                        p("I'm good at working with people from different teams.", "ほかのチームの人と協力して働くのが得意です。"),
                    ),
                    tipJa = "One of my strengths is that 〜. は面接での定番の切り出し方です。",
                    reply = p("Could you give me a specific example?", "具体的な例を挙げていただけますか？"),
                ),
                ScriptStep(
                    taskJa = "強みの具体例を話そう（前の仕事でのエピソード）",
                    contextJa = "強みが伝わる仕事のエピソードを 1 つ。何をして、どうなったかを話します。",
                    mission = "strength",
                    open = true,
                    answers = listOf(
                        p("For example, in my last project, I learned a new tool in two weeks and trained my team.", "例えば前のプロジェクトでは、2 週間で新しいツールを覚えて、チームに教えました。"),
                        p("For example, I led a project with the design team, and we finished it a month early.", "例えば、デザインチームとのプロジェクトをリードして、1 か月早く終わらせました。"),
                    ),
                    tipJa = "For example, のあとに「何をして、どうなったか」を話すと説得力が増します。",
                    reply = p("That's impressive. So, why are you interested in working with us?", "すばらしいですね。では、なぜ当社で働くことに興味を持ったのですか？"),
                ),
                ScriptStep(
                    taskJa = "志望動機を伝えよう",
                    contextJa = "この会社で働きたい理由。例：ミッションに共感した、経験を活かしたい、海外とかかわる仕事がしたい。",
                    mission = "why",
                    open = true,
                    answers = listOf(
                        p("I'm really drawn to your company's mission, and I want to work on products used around the world.", "御社のミッションにとても惹かれていて、世界中で使われる製品に携わりたいと思っています。"),
                        p("I'd like to use my experience to help your company grow in Asia.", "自分の経験を活かして、御社のアジアでの成長に貢献したいです。"),
                    ),
                    tipJa = "I'm really drawn to 〜. で「〜に惹かれている」と前向きな気持ちを伝えられます。",
                    reply = p("Thank you. Do you have any questions for me?", "ありがとうございます。何か質問はありますか？"),
                ),
                ScriptStep(
                    taskJa = "チームや仕事内容について質問しよう",
                    contextJa = "面接の最後に「何か質問は？」と聞かれました。例：チームのこと、1 日の仕事の流れ。",
                    open = true,
                    answers = listOf(
                        p("Could you tell me more about the team I'd be working with?", "一緒に働くチームについて、もう少し教えていただけますか？"),
                        p("What does a typical day look like in this position?", "このポジションの典型的な 1 日はどんな感じですか？"),
                    ),
                    tipJa = "最後に質問をすると意欲が伝わります。Could you tell me more about 〜? が便利です。",
                    reply = p(
                        "Of course. You'd be working with a team of eight people in Tokyo and Singapore. Thank you so much for your time today. We'll be in touch soon!",
                        "もちろんです。東京とシンガポールの 8 人のチームで働くことになります。本日はお時間をいただき本当にありがとうございました。近いうちにご連絡します！",
                    ),
                ),
            ),
        ),

        Script(
            scenarioId = "negotiation",
            openerJa = "本日はお時間をいただきありがとうございます。ご存じのとおり来月で契約更新ですが、コスト上昇のため、今年は 10% の値上げをご提案しています。いかがでしょうか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "10% の値上げは受け入れにくいと、理由を添えて伝えよう",
                    contextJa = "今の契約は年 5 万ドル。来年の予算はすでに削られていて、増やせません。",
                    mission = "pushback",
                    keywords = listOf(
                        "difficult|hard|tough|can't|not able|unable|too much|too high|expensive|concern*|justify",
                        "budget|cost*|justify|price|increase|percent",
                    ),
                    answers = listOf(
                        p("I understand where you're coming from, but a ten percent increase would be hard to justify with our current budget.", "おっしゃることはわかりますが、今の予算では 10% の値上げを正当化するのは難しいです。"),
                        p("To be honest, ten percent is too much for us. Our budget for next year has already been cut.", "正直、10% は厳しいです。来年の予算はすでに削られています。"),
                    ),
                    tipJa = "I understand where you're coming from, but 〜. は、相手の立場を認めつつ反対するときの丁寧な言い方です。",
                    reply = p(
                        "I hear you. Our costs have gone up quite a bit, though. Is there any flexibility on your side?",
                        "おっしゃることはわかります。ただ、こちらのコストもかなり上がっているんです。そちらで何か調整の余地はありますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "2 年契約にする代わりに、今の価格を据え置けないか提案しよう",
                    contextJa = "長く契約すれば、相手にも安定した売上というメリットがあります。",
                    mission = "counter",
                    keywords = listOf("two year*|longer|multi year*|multiyear"),
                    answers = listOf(
                        p("Would you be willing to keep the current price if we committed to a two-year contract?", "2 年契約にするなら、今の価格を据え置いていただけますか？"),
                        p("What if we signed for two years? Could you keep the price the same?", "2 年契約にしたらどうでしょう？価格を据え置けますか？"),
                    ),
                    tipJa = "Would you be willing to 〜 if we …? は、条件付きで譲歩を求める交渉の定番表現です。",
                    reply = p(
                        "Hmm, that's an interesting offer. I can't freeze the price completely, but for a two-year deal I could bring the increase down to three percent.",
                        "うーん、興味深いご提案ですね。完全に据え置くことはできませんが、2 年契約なら値上げを 3% に抑えられます。",
                    ),
                ),
                ScriptStep(
                    taskJa = "3% ならまだ高いので、研修を無料で付けてもらえないか聞こう",
                    contextJa = "チームの研修（トレーニング）が無料で付けば、3% でも受け入れられそうです。",
                    mission = "counter",
                    keywords = listOf("training|support|onboarding", "free|include*|no extra|no charge|throw in|add*|at no"),
                    answers = listOf(
                        p("Three percent is closer, but could you include the training sessions at no extra cost?", "3% なら近づきましたが、研修を追加料金なしで付けていただけませんか？"),
                        p("If you could throw in free training for our team, I think we could make three percent work.", "チーム向けの研修を無料で付けてもらえるなら、3% でなんとかなると思います。"),
                    ),
                    tipJa = "throw in は「おまけで付ける」。make 〜 work は「〜で何とかする」という交渉でよく使う表現です。",
                    reply = p(
                        "Okay, I think we can do that. Three percent, a two-year contract, and free training for up to twenty people.",
                        "わかりました、それならできると思います。3% の値上げ、2 年契約、20 名までの無料研修ですね。",
                    ),
                ),
                ScriptStep(
                    taskJa = "合意した内容をくり返して確認しよう",
                    contextJa = "決まったのは「値上げ 3%・2 年契約・20 名分の研修が無料」です。",
                    mission = "agree",
                    keywords = listOf("confirm|so|recap|just to|make sure|correct|right|summar*", "three|two year*|training|twenty"),
                    answers = listOf(
                        p("Just to confirm, that's a three percent increase, a two-year contract, and free training for twenty people, right?", "確認ですが、3% の値上げ、2 年契約、20 名分の無料研修ということでよろしいですね？"),
                        p("So, to recap: three percent, two years, and training included. Is that correct?", "まとめると、3%、2 年、研修込み。合っていますか？"),
                    ),
                    tipJa = "Just to confirm, 〜, right? / So, to recap, 〜 で合意内容を確認すると、行き違いを防げます。",
                    reply = p("That's right. I'll send over the updated contract by Friday.", "その通りです。金曜までに修正した契約書をお送りします。"),
                ),
                ScriptStep(
                    taskJa = "社内で確認してから返事をする、と伝えて締めくくろう",
                    contextJa = "最終決定には上司の承認が必要です。",
                    mission = "agree",
                    keywords = listOf("manager|team|boss|internally|run it by|check|get back|review|confirm|discuss"),
                    answers = listOf(
                        p("Great. I'll need to run it by my manager, but I'll get back to you early next week.", "ありがとうございます。上司に確認する必要がありますが、来週の早いうちにお返事します。"),
                        p("Sounds good. Let me check with my team, and I'll get back to you by Wednesday.", "いいですね。チームに確認して、水曜までにお返事します。"),
                    ),
                    tipJa = "run it by 〜 は「〜に確認を取る」、get back to you は「改めて連絡する」という定番表現です。",
                    reply = p("Perfect. Thanks for a productive conversation. Talk soon!", "完ぺきです。有意義なお話をありがとうございました。ではまた！"),
                ),
            ),
        ),
        Script(
            scenarioId = "complaint",
            openerJa = "もしもし、注文の件で電話しています。買った机が 1 週間遅れて届いて、箱を開けたら天板にひどい傷があったんです。正直、本当に腹が立っています。",
            steps = listOf(
                ScriptStep(
                    taskJa = "お客さんの気持ちに寄り添って、丁寧に謝ろう",
                    contextJa = "まずは言い訳をせず、相手の気持ちを受け止めて謝りましょう。",
                    mission = "empathize",
                    keywords = listOf("sorry|apologi*"),
                    answers = listOf(
                        p("I'm so sorry to hear that. I completely understand your frustration.", "それは大変申し訳ございません。お怒りはごもっともです。"),
                        p("I sincerely apologize for the delay and the damage. That must be really disappointing.", "遅延と破損について心からお詫び申し上げます。さぞがっかりされたことと思います。"),
                    ),
                    tipJa = "I completely understand your frustration. で、相手の気持ちを受け止めていることを伝えられます。",
                    reply = p("Thank you. I just want this fixed. I've been waiting for this desk for weeks.", "ありがとう。とにかく何とかしてほしいんです。何週間もこの机を待っていたんですよ。"),
                ),
                ScriptStep(
                    taskJa = "注文番号を教えてもらおう",
                    contextJa = "状況を調べるために、注文番号が必要です。",
                    mission = "details",
                    keywords = listOf("order*|number"),
                    answers = listOf(
                        p("Of course. May I have your order number, please?", "もちろんです。ご注文番号をいただけますか？"),
                        p("Let me look into it right away. Could you tell me your order number?", "すぐにお調べします。ご注文番号を教えていただけますか？"),
                    ),
                    tipJa = "May I have 〜? / Could you tell me 〜? は、情報を丁寧にたずねる定番の形です。",
                    reply = p("Sure, it's four eight two seven one.", "はい、48271 です。"),
                ),
                ScriptStep(
                    taskJa = "傷の大きさや、机が使える状態かをたずねよう",
                    contextJa = "対応を決めるために、傷の程度と、机がまだ使えるかを確認します。",
                    mission = "details",
                    keywords = listOf("scratch*|damage*|size|big|bad|large|deep|usable|use|condition"),
                    answers = listOf(
                        p("Thank you. Could you tell me how big the scratch is? Is the desk still usable?", "ありがとうございます。傷の大きさを教えていただけますか？机はまだ使える状態ですか？"),
                        p("I see. How bad is the damage? Can you still use the desk?", "承知しました。破損の程度はいかがですか？机はまだお使いになれますか？"),
                    ),
                    tipJa = "How bad is the damage? は「どの程度の破損か」を聞く自然な言い方です。",
                    reply = p(
                        "It's about ten centimeters long and pretty deep. I can use it, but I paid for a brand-new desk, not a damaged one.",
                        "10 センチくらいで、かなり深い傷です。使えなくはないですが、払ったのは新品の机の代金で、傷物のためじゃありません。",
                    ),
                ),
                ScriptStep(
                    taskJa = "新しい机をすぐに無料で送ることを提案しよう",
                    contextJa = "傷のない新品を、無料ですぐに送ることができます。",
                    mission = "solution",
                    keywords = listOf("replace*|new one|new desk|brand new|send|ship|exchange|refund"),
                    answers = listOf(
                        p("You're absolutely right. I'd be happy to send you a replacement right away, free of charge.", "おっしゃる通りです。すぐに無料で代わりの品をお送りいたします。"),
                        p("That's not acceptable, and I'm sorry. We'll ship a brand-new desk to you this week and pick up the damaged one.", "あってはならないことで、申し訳ございません。今週中に新品の机をお送りし、破損品は引き取りに伺います。"),
                    ),
                    tipJa = "I'd be happy to 〜. は「喜んで〜いたします」と前向きに対応を申し出る表現です。",
                    reply = p("Okay, that would be great. But how do I know it won't arrive late again?", "それは助かります。でも、また遅れないってどうしてわかるんですか？"),
                ),
                ScriptStep(
                    taskJa = "速達で送ることと、届く日を約束しよう",
                    contextJa = "速達（express）に切り替えれば、金曜までに届けられます。",
                    mission = "solution",
                    keywords = listOf("express|priority|expedite*|fast*|guarantee|promise|by|within|tomorrow|friday|days|track*"),
                    answers = listOf(
                        p("I'll upgrade it to express shipping, so it'll arrive by Friday. I'll also email you the tracking number.", "速達に切り替えますので、金曜までに届きます。追跡番号もメールでお送りします。"),
                        p("We'll send it by express delivery at no cost, and I'll personally make sure it arrives within three days.", "無料で速達にいたします。3 日以内に届くよう、私が責任を持って確認します。"),
                    ),
                    tipJa = "I'll personally make sure 〜. で「私が責任を持って〜します」と誠意を示せます。",
                    reply = p("Alright, thank you. I appreciate you taking care of this so quickly.", "わかりました、ありがとう。すぐに対応してくれて感謝します。"),
                ),
            ),
        ),
        Script(
            scenarioId = "debate",
            openerJa = "大企業が全員に週 5 日の出社を義務づけるっていう記事、見た？個人的には、そろそろそうなるべきだと思うな。あなたはどう思う？",
            steps = listOf(
                ScriptStep(
                    taskJa = "賛成か反対か、理由とともに自分の立場をはっきり述べよう",
                    contextJa = "あなたは「在宅勤務も認めるべき」という立場です。例：家のほうが集中できる、通勤時間がない、仕事によって向き不向きがある。",
                    mission = "stance",
                    keywords = listOf(
                        "think|argue|believe|disagree|agree|opinion|view|take|personally|honestly|opposite|mistake",
                        "because|since|productiv*|flexib*|commut*|balance|trust|focus|ignore*|treat*|better|home|remote|efficien*",
                    ),
                    answers = listOf(
                        p("Honestly, I'd argue the opposite. Forcing people back full-time ignores how productive many of them have been at home.", "正直、私は逆の意見です。全員をフルタイムで出社させるのは、多くの人が在宅で成果を上げてきた事実を無視しています。"),
                        p("I can see why companies want it, but I think a blanket rule is a mistake, because it treats very different jobs as if they were the same.", "企業がそうしたい理由はわかりますが、一律のルールは間違いだと思います。まったく違う仕事を同じように扱うことになるからです。"),
                    ),
                    tipJa = "I'd argue 〜. は自分の主張を論理的に打ち出す表現。a blanket rule は「一律のルール」です。",
                    reply = p(
                        "Fair enough, but what about younger employees? They learn so much just by sitting next to experienced colleagues.",
                        "なるほど。でも若手社員はどう？経験のある同僚の隣に座っているだけで、すごく多くを学ぶでしょう。",
                    ),
                ),
                ScriptStep(
                    taskJa = "その点は認めつつ、それでも全面出社は必要ないと反論しよう",
                    contextJa = "「若手は先輩の隣で学ぶ」という点は認めつつ、だから週 5 日必要とは限らない、と返しましょう。",
                    mission = "concede",
                    keywords = listOf(
                        "fair|true|point|admit|grant|agree|valid|right",
                        "but|however|although|that said|still|yet|whereas",
                    ),
                    answers = listOf(
                        p("That's a fair point, but it doesn't follow that everyone needs to be in five days a week. Mentoring can happen on two or three set days.", "それはもっともですが、だからといって全員が週 5 日出社する必要があることにはなりません。指導は決まった週 2、3 日でもできます。"),
                        p("I'll grant you that, but the solution is better mentoring, not mandatory attendance for everyone.", "そこは認めますが、解決策はより良い指導であって、全員の出社の義務化ではありません。"),
                    ),
                    tipJa = "it doesn't follow that 〜 は「だからといって〜とは限らない」。I'll grant you that は「そこは認めるよ」という譲歩の表現です。",
                    reply = p(
                        "Hmm, but don't you think companies have a point about culture too? It's hard to build trust over video calls.",
                        "うーん、でも企業文化については会社側の言い分にも一理あると思わない？ビデオ通話で信頼関係を築くのは難しいよ。",
                    ),
                ),
                ScriptStep(
                    taskJa = "「問題は場所ではなく〜だ」という形で論点を整理して反論しよう",
                    contextJa = "相手は「ビデオ通話では信頼関係を築きにくい」と言っています。問題は場所ではなく、チームの意思疎通の仕方だと返します。",
                    mission = "nuance",
                    keywords = listOf("not so much|not about|rather than|less about|more about|real issue|real problem|the point is|it's about|problem is|issue is|not the"),
                    answers = listOf(
                        p("I think it's not so much about where people work as it is about how teams communicate.", "問題はどこで働くかというより、チームがどう意思疎通するかだと思います。"),
                        p("To me, the real issue isn't the office itself, but whether managers know how to lead remote teams.", "私にとって本当の問題はオフィスそのものではなく、管理職がリモートのチームを率いる方法を知っているかどうかです。"),
                    ),
                    tipJa = "It's not so much about A as it is about B. で「A というよりむしろ B の問題だ」と論点を整理できます。",
                    reply = p("Okay, that's an interesting way to put it. So where would you draw the line?", "なるほど、面白い言い方だね。じゃあ、どこで線を引く？"),
                ),
                ScriptStep(
                    taskJa = "条件付きの折衷案を示そう（例：チームごとに決める）",
                    contextJa = "例：チームごとに決める、週に何日かだけ集まる日を決める、など。",
                    mission = "nuance",
                    keywords = listOf("depend*|if|unless|case by case|each team|flexib*|hybrid|balance|compromise|middle|as long as"),
                    answers = listOf(
                        p("I'd let each team decide, as long as they agree on a few core days to meet in person.", "対面で集まるコアの日をいくつか決めることを条件に、チームごとに決めさせると思います。"),
                        p("If anything, a hybrid model with clear expectations would give companies most of the benefits without the resentment.", "むしろ、期待値をはっきりさせたハイブリッド型なら、反発を招かずに利点のほとんどを得られるでしょう。"),
                    ),
                    tipJa = "as long as 〜（〜する限り）や If anything（むしろ）で、条件付きの主張に厚みが出ます。",
                    reply = p("That actually sounds pretty reasonable. I might have to rethink my position a bit.", "それ、実はかなり筋が通ってるね。私も少し考え直さないといけないかも。"),
                ),
                ScriptStep(
                    taskJa = "相手の意見も尊重して、議論を気持ちよく締めくくろう",
                    keywords = listOf("interesting|enjoy*|appreciate|respect|agree to disagree|food for thought|perspective|thank*|fun|great"),
                    answers = listOf(
                        p("I guess we'll have to agree to disagree on some of it, but I really enjoyed this conversation.", "一部は意見が違うままだけど、この会話はとても楽しかったよ。"),
                        p("Thanks for pushing back. It's given me a lot of food for thought, too.", "反論してくれてありがとう。私にとってもいい考える材料になったよ。"),
                    ),
                    tipJa = "agree to disagree は「意見の違いを認め合う」、food for thought は「考える材料」という慣用表現です。",
                    reply = p("Same here. Let's pick this up again over coffee next week!", "こちらこそ。来週コーヒーでも飲みながら続きを話そう！"),
                ),
            ),
        ),
        Script(
            scenarioId = "qanda",
            openerJa = "興味深い発表をありがとうございました。30% という数字について伺いたいのですが、具体的にどう測定したのですか？また、期間はどのくらいですか？",
            steps = listOf(
                ScriptStep(
                    taskJa = "データの測り方と期間を説明しよう（リリース前後の 6 か月を比較）",
                    contextJa = "30% は、アプリ公開前の 6 か月と後の 6 か月の、問い合わせ件数を比べた数字です。",
                    mission = "explain",
                    keywords = listOf(
                        "compar*|measur*|before|after|track*|analy*",
                        "month*|period|year*|week*|data|logs|calls",
                    ),
                    answers = listOf(
                        p("That's a great question. We compared the six months before and after the launch, using our call center logs.", "いい質問ですね。コールセンターの記録を使って、リリース前後の 6 か月を比較しました。"),
                        p("We measured it by comparing support call volumes over six months before and after the app went live.", "アプリ公開前後の 6 か月間の問い合わせ件数を比較して測定しました。"),
                    ),
                    tipJa = "That's a great question. と一呼吸おくと、落ち着いて答えを組み立てられます。",
                    reply = p(
                        "I see. But couldn't other factors explain the drop? For example, seasonal changes or a smaller customer base?",
                        "なるほど。でも、ほかの要因でも説明できませんか？例えば季節の変動や顧客数の減少などで。",
                    ),
                ),
                ScriptStep(
                    taskJa = "ほかの要因の可能性を認めつつ、どう対策したかを説明しよう",
                    contextJa = "前年の同じ月と比べて、季節の影響は取り除きました。ただ、ほかの要因を完全には否定できません。",
                    mission = "limit",
                    keywords = listOf(
                        "fair|point|possible|true|admit|right|valid|rule out|agree",
                        "control*|adjust*|account*|normaliz*|per customer|same months|season*|compar*",
                    ),
                    answers = listOf(
                        p("That's a fair point. We did adjust for seasonality by comparing the same months year over year, but we can't completely rule out other factors.", "ごもっともです。前年同月と比較して季節要因は調整しましたが、ほかの要因を完全には否定できません。"),
                        p("You're right that it's possible. To account for that, we looked at calls per customer rather than total calls.", "その可能性はおっしゃる通りです。それを考慮して、総件数ではなく顧客あたりの件数を見ました。"),
                    ),
                    tipJa = "We can't completely rule out 〜. は「〜の可能性を完全には否定できない」と、限界を率直に認める表現です。",
                    reply = p("That's helpful. And do you know whether customer satisfaction changed over the same period?", "参考になります。同じ期間で顧客満足度が変わったかどうかはご存じですか？"),
                ),
                ScriptStep(
                    taskJa = "手元にデータがないので、あとで共有すると伝えよう",
                    contextJa = "顧客満足度のデータは、いま手元にありません。",
                    mission = "handle",
                    keywords = listOf(
                        "don't have|not sure|not with me|on hand|off the top|don't know",
                        "follow up|get back|share|send|email|after",
                    ),
                    answers = listOf(
                        p("I don't have that figure with me, but I'd be happy to follow up with you after the session.", "その数字は手元にありませんが、セッションのあとで喜んでご連絡します。"),
                        p("Off the top of my head, I'm not sure, so I'd rather not guess. Let me send you the exact numbers by email.", "すぐにはわからないので、推測で答えるのは控えます。正確な数字をメールでお送りします。"),
                    ),
                    tipJa = "I'd rather not guess. で「憶測では答えない」と誠実さを示せます。off the top of my head は「今すぐ思いつく範囲では」。",
                    reply = p(
                        "Of course, that would be great. One last question: do you think this approach would work in other industries?",
                        "ええ、ぜひ。最後にひとつ。この手法はほかの業界でも通用すると思いますか？",
                    ),
                ),
                ScriptStep(
                    taskJa = "条件付きで「通用する」と答え、業界の例を挙げよう",
                    contextJa = "銀行や通信会社のように、同じような質問が多い業界なら効果がありそうです。",
                    mission = "handle",
                    keywords = listOf(
                        "depend*|if|as long as|provided|likely|probably|principle|generally|believe|think",
                        "bank*|insurance|retail|health*|telecom*|industr*|sector|compan*|airline*",
                    ),
                    answers = listOf(
                        p("I believe so, as long as the company handles a lot of repetitive questions, like banks or telecom providers.", "銀行や通信会社のように、繰り返しの多い問い合わせを扱う企業であれば、通用すると思います。"),
                        p("In principle, yes, but it depends on how complex the customers' problems are. Insurance might be a good next test case.", "原則としてはそうですが、顧客の問題の複雑さによります。保険業界は次の検証先として良いかもしれません。"),
                    ),
                    tipJa = "as long as 〜 や provided that 〜 で条件を付けると、断定しすぎない説得力のある答えになります。",
                    reply = p("Very interesting. Thank you, that answers my question.", "とても興味深いです。ありがとうございます、よくわかりました。"),
                ),
                ScriptStep(
                    taskJa = "質問へのお礼を言って、質疑応答を締めくくろう",
                    keywords = listOf("thank*|appreciate|grateful"),
                    answers = listOf(
                        p("Thank you for those thoughtful questions. If anyone would like to discuss this further, please feel free to find me afterward.", "示唆に富むご質問をありがとうございました。さらに議論したい方は、このあと気軽に声をかけてください。"),
                        p("I appreciate the challenging questions. I think we're out of time, so thank you all for listening.", "鋭いご質問に感謝します。時間になりましたので、ご清聴ありがとうございました。"),
                    ),
                    tipJa = "thoughtful questions（示唆に富む質問）と言うと、質問者への敬意が伝わります。",
                    reply = p("Thank you! Let's give our speaker another round of applause.", "ありがとうございました！もう一度、発表者に拍手をお願いします。"),
                ),
            ),
        ),

        // ---- フリートーク (お題はすべて自由回答。回答例は A・B・C の 3 段階で、学習者のレベルに近いものから見せる) ----
        Script(
            scenarioId = "free:free",
            openerJa = "こんにちは！アレックスです。お話しできてうれしいです。今日はここまでどんな一日？",
            steps = listOf(
                ScriptStep(
                    taskJa = "今日の調子を答えよう",
                    open = true,
                    answers = listOf(
                        a("I'm good, thank you. How are you?", "元気です、ありがとう。あなたは？"),
                        b("It's going pretty well, thanks. How about you?", "けっこういい感じだよ、ありがとう。あなたは？"),
                        c("Honestly, it's been a bit hectic, but I managed to squeeze in a walk this morning, so I can't complain.", "正直ちょっとバタバタしてるけど、朝に散歩する時間はなんとか作れたから、文句は言えないかな。"),
                    ),
                    tipJa = "How about you? と聞き返すと、会話のキャッチボールになります。",
                    reply = p("I'm doing great, thanks! I just had a really good cup of coffee. So, where are you from?", "元気だよ、ありがとう！さっきすごくおいしいコーヒーを飲んだんだ。ところで、出身はどこ？"),
                ),
                ScriptStep(
                    taskJa = "出身地や住んでいる場所を答えよう",
                    open = true,
                    answers = listOf(
                        a("I'm from Osaka. I live in Tokyo now.", "大阪出身です。今は東京に住んでいます。"),
                        b("I live in Tokyo. It's a busy but fun city.", "東京に住んでいます。にぎやかだけど楽しい街です。"),
                        c("I grew up in Osaka, but I've been living in Tokyo for about ten years, so it feels like home now.", "大阪で育ちましたが、東京に住んで 10 年ほどになるので、今ではこちらが地元のように感じます。"),
                    ),
                    tipJa = "I'm from 〜. は出身、I live in 〜. は今住んでいる場所です。",
                    reply = p("Oh, nice! I've always wanted to visit Japan. What do you usually do on weekends?", "へえ、いいね！ずっと日本に行ってみたいと思ってるんだ。週末はふだん何をしてるの？"),
                ),
                ScriptStep(
                    taskJa = "週末によくすることを話そう",
                    open = true,
                    answers = listOf(
                        a("I often watch movies at home.", "よく家で映画を見ます。"),
                        b("I usually go to the gym and then watch movies at home.", "たいていジムに行って、そのあと家で映画を見ます。"),
                        c("It depends on my mood, but I usually try to get outside, whether it's hiking or just exploring a new neighborhood.", "気分によりますが、ハイキングでも新しい街の散策でも、できるだけ外に出るようにしています。"),
                    ),
                    tipJa = "usually（たいてい）や often（よく）で、ふだんの習慣を表せます。",
                    reply = p("That sounds nice. By the way, why are you learning English?", "いいね。ところで、どうして英語を勉強してるの？"),
                ),
                ScriptStep(
                    taskJa = "英語を勉強している理由を話そう",
                    open = true,
                    answers = listOf(
                        a("I want to travel and talk to people.", "旅行して人と話したいからです。"),
                        b("I need English for my job, and I also want to travel abroad.", "仕事で英語が必要で、海外旅行もしたいからです。"),
                        c("Mainly for work, since I deal with overseas clients, but I'd also love to be able to express myself without holding back.", "主に海外のお客さんとやりとりする仕事のためですが、遠慮せずに自分を表現できるようになりたいという気持ちもあります。"),
                    ),
                    tipJa = "I want to 〜. / I need 〜 for …. で目的や理由を説明できます。",
                    reply = p(
                        "That's a great reason. Honestly, your English is really good! Is there anything fun you want to do this week?",
                        "すてきな理由だね。正直、英語すごく上手だよ！今週、何かやりたい楽しいことはある？",
                    ),
                ),
                ScriptStep(
                    taskJa = "今週やりたいことを 1 つ話そう",
                    open = true,
                    answers = listOf(
                        a("I want to eat at a new restaurant.", "新しいレストランで食事したいです。"),
                        b("I'm going to see a movie with my friend on Saturday.", "土曜日に友達と映画を見に行く予定です。"),
                        c("I'm hoping to finally finish a novel I started last month. I keep getting distracted, so this week I'm determined.", "先月読み始めた小説をようやく読み終えたいです。つい気が散ってしまうので、今週こそはと決めています。"),
                    ),
                    tipJa = "be going to 〜 は「〜する予定」。決まっている予定を話すときに使います。",
                    reply = p("That sounds fun! Well, it was really nice talking with you. Let's chat again soon!", "楽しそう！お話しできて本当に楽しかったよ。またすぐ話そうね！"),
                ),
            ),
        ),
        Script(
            scenarioId = "free:today",
            openerJa = "やあ、アレックスだよ！ねえ、今日は何をしたの？",
            steps = listOf(
                ScriptStep(
                    taskJa = "今日したことを話そう（過去形で）",
                    open = true,
                    answers = listOf(
                        a("I went to work today.", "今日は仕事に行きました。"),
                        b("I went to work and had a long meeting in the afternoon.", "仕事に行って、午後に長い会議がありました。"),
                        c("I spent most of the day preparing for a presentation, and then I treated myself to a nice dinner.", "一日の大半をプレゼンの準備に費やして、そのあと自分へのごほうびにおいしい夕食を食べました。"),
                    ),
                    tipJa = "今日のことは went / had / spent のように過去形で話します。",
                    reply = p("Oh, nice! Thanks for sharing. What did you have for lunch today?", "へえ、いいね！教えてくれてありがとう。今日のお昼は何を食べた？"),
                ),
                ScriptStep(
                    taskJa = "お昼に食べたものを話そう",
                    open = true,
                    answers = listOf(
                        a("I ate ramen for lunch.", "お昼にラーメンを食べました。"),
                        b("I had ramen at a small shop near my office.", "会社の近くの小さなお店でラーメンを食べました。"),
                        c("I just grabbed a sandwich and ate it at my desk, which I know isn't great, but I was swamped.", "サンドイッチを買ってデスクで食べただけです。よくないのはわかっていますが、とても忙しかったので。"),
                    ),
                    tipJa = "「食べた」は ate でも had でも OK。会話では had がよく使われます。",
                    reply = p("Yum, that sounds good! Did anything interesting happen today?", "おいしそう！今日、何か面白いことはあった？"),
                ),
                ScriptStep(
                    taskJa = "今日あった面白いこと・うれしかったことを話そう",
                    open = true,
                    answers = listOf(
                        a("I saw a cute dog on the street.", "道でかわいい犬を見ました。"),
                        b("My coworker brought some sweets for everyone. That made me happy.", "同僚がみんなにお菓子を持ってきてくれて、うれしかったです。"),
                        c("A client I'd been struggling with actually thanked me today, which was a pleasant surprise.", "手こずっていたお客さんが今日お礼を言ってくれて、うれしい驚きでした。"),
                    ),
                    tipJa = "That made me happy. で「それでうれしくなった」と気持ちを添えられます。",
                    reply = p("Ha, I love that! Little things like that can make your day. How are you feeling right now?", "はは、いいね！そういう小さなことで一日が楽しくなるよね。いまはどんな気分？"),
                ),
                ScriptStep(
                    taskJa = "いまの気分を、理由と一緒に話そう",
                    open = true,
                    answers = listOf(
                        a("I'm tired because I worked a lot.", "たくさん働いたので疲れています。"),
                        b("I feel relaxed because tomorrow is a holiday.", "明日は休みなので、リラックスしています。"),
                        c("I'm a bit drained, to be honest, but in a good way, since I got a lot done.", "正直少し疲れていますが、たくさん片付いたので、いい意味での疲れです。"),
                    ),
                    tipJa = "I'm 〜 because …. で、気分と理由をセットで伝えましょう。",
                    reply = p("That makes sense. What are you going to do tonight?", "なるほどね。今夜は何をする予定？"),
                ),
                ScriptStep(
                    taskJa = "今夜の予定を話そう",
                    open = true,
                    answers = listOf(
                        a("I will take a bath and sleep early.", "お風呂に入って早く寝ます。"),
                        b("I'm going to watch a drama on Netflix.", "ネットフリックスでドラマを見る予定です。"),
                        c("I'm planning to cook something simple and then catch up on a podcast I've fallen behind on.", "簡単なものを作って、聞きそびれているポッドキャストを追いかけるつもりです。"),
                    ),
                    tipJa = "be going to 〜 や I'm planning to 〜 で「〜するつもり」と予定を話せます。",
                    reply = p("Sounds like a perfect evening. Thanks for telling me about your day. Talk to you soon!", "最高の夜になりそうだね。今日のことを話してくれてありがとう。またね！"),
                ),
            ),
        ),
        Script(
            scenarioId = "free:hobbies",
            openerJa = "こんにちは、アレックスです！気になるんだけど、自由な時間には何をするのが好き？",
            steps = listOf(
                ScriptStep(
                    taskJa = "好きなこと・趣味を話そう",
                    open = true,
                    answers = listOf(
                        a("I like playing the guitar.", "ギターを弾くのが好きです。"),
                        b("I love watching anime and reading manga in my free time.", "時間があるときは、アニメを見たりマンガを読んだりするのが大好きです。"),
                        c("I'm really into photography these days, especially taking pictures of old buildings around the city.", "最近は写真にはまっていて、特に街の古い建物を撮るのが好きです。"),
                    ),
                    tipJa = "I like 〜ing. で「〜するのが好き」。love を使うと「大好き」、be into 〜 で「〜にはまっている」です。",
                    reply = p("Oh, cool! How did you get into it?", "へえ、いいね！どうやってそれにはまったの？"),
                ),
                ScriptStep(
                    taskJa = "始めたきっかけを話そう",
                    open = true,
                    answers = listOf(
                        a("My friend taught me in high school.", "高校で友達が教えてくれました。"),
                        b("I started a few years ago because I had a lot of free time.", "時間がたくさんあったので、数年前に始めました。"),
                        c("I got into it almost by accident, when a friend lent me an old camera and I couldn't put it down.", "友達に古いカメラを借りたら手放せなくなって、ほとんど偶然はまりました。"),
                    ),
                    tipJa = "get into 〜 は「〜にはまる」。きっかけは when や because を使って説明しましょう。",
                    reply = p("That's a great story. How often do you do it?", "いい話だね。どのくらいの頻度でやってるの？"),
                ),
                ScriptStep(
                    taskJa = "どのくらいの頻度でやるか話そう",
                    open = true,
                    answers = listOf(
                        a("I do it every day.", "毎日やっています。"),
                        b("About twice a week, usually on weekends.", "週に 2 回くらい、たいてい週末です。"),
                        c("Whenever I can find the time, which realistically means a couple of hours on Sunday mornings.", "時間が取れるときはいつでも。現実的には日曜の朝の 2 時間ほどです。"),
                    ),
                    tipJa = "頻度は every day / twice a week / once a month のように表します。",
                    reply = p("Wow, nice! What do you like most about it?", "わあ、いいね！一番好きなところはどこ？"),
                ),
                ScriptStep(
                    taskJa = "一番好きなところを話そう",
                    open = true,
                    answers = listOf(
                        a("It is fun, and I can relax.", "楽しくて、リラックスできます。"),
                        b("It helps me relax after a long day.", "長い一日のあとにリラックスできるところです。"),
                        c("It forces me to slow down and notice details I'd normally walk right past.", "いつもなら素通りしてしまう細かいことに、立ち止まって気づかせてくれるところです。"),
                    ),
                    tipJa = "It helps me 〜. で「〜するのに役立つ」と良さを説明できます。",
                    reply = p("I totally get that. I play the piano a little myself. Is there anything new you want to try?", "すごくわかる。私も少しピアノを弾くんだ。何か新しく挑戦したいことはある？"),
                ),
                ScriptStep(
                    taskJa = "新しく挑戦したいことを話そう",
                    open = true,
                    answers = listOf(
                        a("I want to try surfing.", "サーフィンをやってみたいです。"),
                        b("I'd like to learn how to cook Italian food.", "イタリア料理の作り方を習いたいです。"),
                        c("I've always wanted to try pottery, partly because working with my hands would balance out my desk job.", "ずっと陶芸をやってみたいと思っています。手を動かすことが、デスクワークとのバランスになりそうだからです。"),
                    ),
                    tipJa = "I'd like to learn how to 〜. で「〜のやり方を学びたい」と言えます。",
                    reply = p("That sounds amazing. I hope you get to try it soon! It was really fun talking with you.", "すてき！早く挑戦できるといいね。話せて本当に楽しかったよ。"),
                ),
            ),
        ),
        Script(
            scenarioId = "free:travel",
            openerJa = "こんにちは、アレックスです！旅行の話を聞くのが大好きなんだ。今までで一番よかった旅行は？",
            steps = listOf(
                ScriptStep(
                    taskJa = "一番よかった旅行先を答えよう",
                    open = true,
                    answers = listOf(
                        a("I liked Hokkaido the best.", "北海道が一番よかったです。"),
                        b("The best trip was to Taiwan with my friends last year.", "一番よかったのは、去年友達と行った台湾です。"),
                        c("Probably the trip I took to Portugal a few years ago. It completely exceeded my expectations.", "たぶん数年前に行ったポルトガル旅行です。期待をはるかに超えていました。"),
                    ),
                    tipJa = "The best trip was to 〜. で「一番の旅行は〜」と答えられます。",
                    reply = p("Oh, I've heard great things about it! What did you do there?", "へえ、すごくいいって聞くよ！そこで何をしたの？"),
                ),
                ScriptStep(
                    taskJa = "旅先でしたことを話そう（過去形で）",
                    open = true,
                    answers = listOf(
                        a("I went skiing and ate a lot of seafood.", "スキーをして、海鮮をたくさん食べました。"),
                        b("We visited night markets and tried a lot of street food.", "夜市に行って、屋台の食べ物をたくさん食べました。"),
                        c("We wandered around Lisbon without much of a plan, which turned out to be the best way to see it.", "計画もあまり立てずにリスボンを歩き回りましたが、それが結局一番いい見て回り方でした。"),
                    ),
                    tipJa = "旅行の思い出は went / visited / tried など過去形で話しましょう。",
                    reply = p("That sounds so fun! What was the most memorable moment?", "すごく楽しそう！一番思い出に残っている瞬間は？"),
                ),
                ScriptStep(
                    taskJa = "一番思い出に残っていることを話そう",
                    open = true,
                    answers = listOf(
                        a("The view from the mountain was beautiful.", "山からの景色がきれいでした。"),
                        b("A local family invited us to dinner. They were so kind.", "地元の家族が夕食に招いてくれました。とても親切でした。"),
                        c("Watching the sunset from a tiny bar by the sea, with music playing in the background. I'll never forget it.", "海辺の小さなバーで、音楽を聴きながら夕日を眺めたことです。一生忘れません。"),
                    ),
                    tipJa = "memorable は「思い出に残る」。The 〜 was beautiful. のように感想を添えましょう。",
                    reply = p("Wow, I can imagine that. Where do you want to go next?", "わあ、目に浮かぶよ。次はどこに行きたい？"),
                ),
                ScriptStep(
                    taskJa = "次に行きたい場所と理由を話そう",
                    open = true,
                    answers = listOf(
                        a("I want to go to Italy. I like pasta.", "イタリアに行きたいです。パスタが好きなので。"),
                        b("I'd like to visit New York someday to see a musical.", "いつかミュージカルを見にニューヨークに行きたいです。"),
                        c("I'm dying to go to Iceland, mainly because I'd love to see the northern lights before I get too old.", "アイスランドにすごく行きたいです。年を取りすぎる前にオーロラを見たいというのが一番の理由です。"),
                    ),
                    tipJa = "I want to go to 〜 because …. で行き先と理由をセットで話しましょう。",
                    reply = p("Great choice! Do you prefer traveling alone or with other people?", "いいね！一人旅と誰かと一緒の旅、どっちが好き？"),
                ),
                ScriptStep(
                    taskJa = "一人旅と、誰かと行く旅のどちらが好きか話そう",
                    open = true,
                    answers = listOf(
                        a("I like traveling with friends. It is more fun.", "友達との旅行が好きです。もっと楽しいので。"),
                        b("I prefer traveling alone because I can go wherever I want.", "行きたいところにどこでも行けるので、一人旅のほうが好きです。"),
                        c("It depends on the trip. I like going solo for cities, but for nature trips, it's nicer to share the experience.", "旅によります。街なら一人がいいですが、自然を楽しむ旅なら誰かと体験を分かち合うほうがいいです。"),
                    ),
                    tipJa = "I prefer 〜. で「〜のほうが好き」と好みを伝えられます。",
                    reply = p("That makes sense. Thanks for sharing your stories. I hope your next trip is amazing!", "なるほどね。旅の話をしてくれてありがとう。次の旅行もすてきになりますように！"),
                ),
            ),
        ),
        Script(
            scenarioId = "free:work",
            openerJa = "こんにちは、アレックスです！ふだんは何をしているの？働いてる？それとも学生？",
            steps = listOf(
                ScriptStep(
                    taskJa = "仕事や勉強していることを答えよう",
                    open = true,
                    answers = listOf(
                        a("I work at an IT company.", "IT 企業で働いています。"),
                        b("I'm a university student. I study economics.", "大学生です。経済学を勉強しています。"),
                        c("I work in marketing for a mid-sized tech company, mostly handling our overseas campaigns.", "中規模の IT 企業でマーケティングをしていて、主に海外向けのキャンペーンを担当しています。"),
                    ),
                    tipJa = "I work at 〜（会社）／ I work in 〜（業界・部門）。学生なら I study 〜. と専攻を言いましょう。",
                    reply = p("Oh, interesting! What's a typical day like for you?", "へえ、面白いね！ふだんはどんな一日なの？"),
                ),
                ScriptStep(
                    taskJa = "ふだんの 1 日の流れを話そう",
                    open = true,
                    answers = listOf(
                        a("I start work at nine and go home at six.", "9 時に仕事を始めて、6 時に帰ります。"),
                        b("I usually have meetings in the morning and do my own work in the afternoon.", "たいてい午前は会議で、午後は自分の仕事をします。"),
                        c("My mornings are packed with calls, so I try to block off the afternoons for work that needs real focus.", "午前は打ち合わせで埋まっているので、集中が必要な仕事のために午後は予定を入れないようにしています。"),
                    ),
                    tipJa = "usually で習慣を、in the morning / in the afternoon で時間帯を表します。",
                    reply = p("Sounds busy! What do you enjoy most about it?", "忙しそう！一番楽しいところは？"),
                ),
                ScriptStep(
                    taskJa = "楽しいこと・やりがいを話そう",
                    open = true,
                    answers = listOf(
                        a("I like my team.", "チームが好きです。"),
                        b("I enjoy working with my team and solving problems together.", "チームで一緒に問題を解決するのが楽しいです。"),
                        c("What I find most rewarding is seeing a project I was involved in actually make a difference for customers.", "一番やりがいを感じるのは、自分が関わったプロジェクトが実際にお客さんの役に立つのを見るときです。"),
                    ),
                    tipJa = "I enjoy 〜ing. で「〜するのが楽しい」。rewarding は「やりがいのある」です。",
                    reply = p("That's great. And what's the most challenging part?", "いいね。じゃあ一番大変なところは？"),
                ),
                ScriptStep(
                    taskJa = "大変なことを話そう",
                    open = true,
                    answers = listOf(
                        a("I have a lot of work. I am busy.", "仕事が多くて忙しいです。"),
                        b("Speaking English in meetings is still difficult for me.", "会議で英語を話すのがまだ難しいです。"),
                        c("Juggling several deadlines at once can be overwhelming, especially when priorities keep changing.", "複数の締め切りを同時にこなすのは大変です。特に優先順位がころころ変わるときは。"),
                    ),
                    tipJa = "challenging は「大変だけどやりがいがある」という前向きな響きの言葉です。",
                    reply = p("I understand. That's not easy. What do you want to do in the future?", "わかるよ。簡単じゃないよね。将来は何をしたい？"),
                ),
                ScriptStep(
                    taskJa = "将来やりたいことを話そう",
                    open = true,
                    answers = listOf(
                        a("I want to work in another country.", "外国で働きたいです。"),
                        b("I'd like to start my own business in the future.", "将来は自分で事業を始めたいです。"),
                        c("Eventually, I'd like to move into a role where I can mentor younger colleagues and shape the team's direction.", "いずれは、後輩を育てたりチームの方向性を決めたりできる立場に就きたいです。"),
                    ),
                    tipJa = "someday（いつか）や in the future / eventually（ゆくゆくは）を使って夢を話しましょう。",
                    reply = p("That's a wonderful goal. I'm sure you can do it! Thanks for chatting with me.", "すてきな目標だね。きっとできるよ！話してくれてありがとう。"),
                ),
            ),
        ),
        Script(
            scenarioId = "free:future",
            openerJa = "こんにちは、アレックスです！目標の話をしよう。この数年で、本当にやりたいことはある？",
            steps = listOf(
                ScriptStep(
                    taskJa = "数年以内にやりたいことを話そう",
                    open = true,
                    answers = listOf(
                        a("I want to live in another country.", "外国に住みたいです。"),
                        b("I really want to live abroad for a year.", "1 年間、本当に海外に住んでみたいです。"),
                        c("Within the next few years, I'd love to work abroad, ideally somewhere I can use both English and my professional skills.", "数年以内に海外で働きたいです。できれば英語と仕事のスキルの両方を活かせるところで。"),
                    ),
                    tipJa = "I really want to 〜. で強い気持ちを伝えられます。",
                    reply = p("Oh, that's exciting! Why is that important to you?", "わあ、わくわくするね！どうしてそれが大切なの？"),
                ),
                ScriptStep(
                    taskJa = "その理由を話そう（because を使って）",
                    open = true,
                    answers = listOf(
                        a("Because I want to see a new culture.", "新しい文化を見たいからです。"),
                        b("Because I want to experience a different culture and make new friends.", "違う文化を体験して、新しい友達を作りたいからです。"),
                        c("Because I think living somewhere unfamiliar would push me out of my comfort zone in a way nothing else could.", "なじみのない場所で暮らすことは、ほかの何よりも自分を居心地のいい場所から押し出してくれると思うからです。"),
                    ),
                    tipJa = "Because 〜. で理由を答えられます。会話では Because から始めても大丈夫です。",
                    reply = p("That makes a lot of sense. What are you doing now to get there?", "すごく納得。いまはそのために何をしているの？"),
                ),
                ScriptStep(
                    taskJa = "いま取り組んでいることを話そう",
                    open = true,
                    answers = listOf(
                        a("I study English every day.", "毎日英語を勉強しています。"),
                        b("I'm studying English every day with apps, and I'm saving money.", "アプリで毎日英語を勉強して、お金も貯めています。"),
                        c("I'm brushing up on my English, and I've started reaching out to people who've made a similar move.", "英語を磨き直していて、同じような移住をした人たちに連絡を取り始めました。"),
                    ),
                    tipJa = "I'm 〜ing.（現在進行形）で、いま続けていることを表せます。",
                    reply = p("Good for you! How is English going to help you?", "えらいね！英語はどんなふうに役に立ちそう？"),
                ),
                ScriptStep(
                    taskJa = "英語がどう役立つか話そう",
                    open = true,
                    answers = listOf(
                        a("I can talk to many people.", "たくさんの人と話せます。"),
                        b("English will help me make friends from all over the world.", "世界中に友達を作るのに役立ちます。"),
                        c("It would open up far more career options and let me build relationships without relying on translation.", "仕事の選択肢がずっと広がるし、翻訳に頼らずに人間関係を築けるようになります。"),
                    ),
                    tipJa = "〜 will help me …. で「〜が…するのに役立つ」と言えます。",
                    reply = p("Absolutely. Your English is already getting better! What's one small goal for this month?", "その通り。英語はもう上達してきてるよ！今月の小さな目標を 1 つ教えて？"),
                ),
                ScriptStep(
                    taskJa = "今月の小さな目標を話そう",
                    open = true,
                    answers = listOf(
                        a("I want to speak English every day.", "毎日英語を話したいです。"),
                        b("I'm going to read one English book this month.", "今月は英語の本を 1 冊読むつもりです。"),
                        c("My goal this month is to have at least one conversation in English every day, even if it's just for a few minutes.", "今月の目標は、たとえ数分でも、毎日少なくとも 1 回は英語で会話することです。"),
                    ),
                    tipJa = "小さな目標は、具体的な数字や頻度を入れると達成しやすくなります。",
                    reply = p("Love it! I'll be cheering for you. Let's talk again soon!", "いいね！応援してるよ。またすぐ話そうね！"),
                ),
            ),
        ),
    ).associateBy { it.scenarioId }

    /** シナリオの台本 (万一ない場合はフリートークの台本) */
    fun forScenario(scenario: Scenario): Script = all[scenario.id] ?: all.getValue("free:free")
}
