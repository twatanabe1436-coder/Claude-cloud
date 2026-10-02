package io.github.twatanabe1436.hanaso.core

// 台本モードの台本。シナリオ (Catalog) ごとに、日本語のお題・お手本・相手のセリフを決めておく。
// 相手のセリフは、学習者がお手本と少し違うことを言っても不自然にならないように書く。

private fun p(en: String, ja: String) = Phrase(en, ja)

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

        // ---- フリートーク (お題はすべて自由回答) ----
        Script(
            scenarioId = "free:free",
            openerJa = "こんにちは！アレックスです。お話しできてうれしいです。今日はここまでどんな一日？",
            steps = listOf(
                ScriptStep(
                    taskJa = "今日の調子を答えよう",
                    open = true,
                    answers = listOf(
                        p("It's going pretty well, thanks. How about you?", "けっこういい感じだよ、ありがとう。あなたは？"),
                        p("I'm a little tired, but I'm good.", "ちょっと疲れてるけど、元気だよ。"),
                    ),
                    tipJa = "How about you? と聞き返すと、会話のキャッチボールになります。",
                    reply = p("I'm doing great, thanks! I just had a really good cup of coffee. So, where are you from?", "元気だよ、ありがとう！さっきすごくおいしいコーヒーを飲んだんだ。ところで、出身はどこ？"),
                ),
                ScriptStep(
                    taskJa = "出身地や住んでいる場所を答えよう",
                    open = true,
                    answers = listOf(
                        p("I'm from Osaka, Japan.", "日本の大阪出身です。"),
                        p("I live in Tokyo. It's a busy but fun city.", "東京に住んでいます。にぎやかだけど楽しい街です。"),
                    ),
                    tipJa = "I'm from 〜. は出身、I live in 〜. は今住んでいる場所です。",
                    reply = p("Oh, nice! I've always wanted to visit Japan. What do you usually do on weekends?", "へえ、いいね！ずっと日本に行ってみたいと思ってるんだ。週末はふだん何をしてるの？"),
                ),
                ScriptStep(
                    taskJa = "週末によくすることを話そう",
                    open = true,
                    answers = listOf(
                        p("I usually go to the gym and watch movies at home.", "たいていジムに行って、家で映画を見ます。"),
                        p("I often go to cafes with my friends.", "よく友達とカフェに行きます。"),
                    ),
                    tipJa = "usually（たいてい）や often（よく）で、ふだんの習慣を表せます。",
                    reply = p("That sounds nice. By the way, why are you learning English?", "いいね。ところで、どうして英語を勉強してるの？"),
                ),
                ScriptStep(
                    taskJa = "英語を勉強している理由を話そう",
                    open = true,
                    answers = listOf(
                        p("I want to travel abroad and talk with local people.", "海外旅行に行って、現地の人と話したいからです。"),
                        p("I need English for my job.", "仕事で英語が必要なんです。"),
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
                        p("I want to try a new restaurant near my house.", "家の近くの新しいレストランに行ってみたいです。"),
                        p("I'm going to see a movie with my friend on Saturday.", "土曜日に友達と映画を見に行く予定です。"),
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
                        p("I went to work and had a long meeting.", "仕事に行って、長い会議がありました。"),
                        p("I cleaned my room and went shopping.", "部屋を掃除して、買い物に行きました。"),
                    ),
                    tipJa = "今日のことは went / had / ate のように過去形で話します。",
                    reply = p("Oh, nice! Thanks for sharing. What did you have for lunch today?", "へえ、いいね！教えてくれてありがとう。今日のお昼は何を食べた？"),
                ),
                ScriptStep(
                    taskJa = "お昼に食べたものを話そう",
                    open = true,
                    answers = listOf(
                        p("I had ramen at a small shop near my office.", "会社の近くの小さなお店でラーメンを食べました。"),
                        p("I just had a sandwich at my desk.", "デスクでサンドイッチを食べただけです。"),
                    ),
                    tipJa = "「食べた」は ate でも had でも OK。会話では had がよく使われます。",
                    reply = p("Yum, that sounds good! Did anything interesting happen today?", "おいしそう！今日、何か面白いことはあった？"),
                ),
                ScriptStep(
                    taskJa = "今日あった面白いこと・うれしかったことを話そう",
                    open = true,
                    answers = listOf(
                        p("I saw a really cute dog on my way home.", "帰り道でとてもかわいい犬を見ました。"),
                        p("Not really, but my coworker brought some sweets for everyone.", "特にないけど、同僚がみんなにお菓子を持ってきてくれました。"),
                    ),
                    tipJa = "on my way home（帰り道で）や on my way to work（通勤中に）は日常の出来事を話すのに便利です。",
                    reply = p("Ha, I love that! Little things like that can make your day. How are you feeling right now?", "はは、いいね！そういう小さなことで一日が楽しくなるよね。いまはどんな気分？"),
                ),
                ScriptStep(
                    taskJa = "いまの気分を、理由と一緒に話そう",
                    open = true,
                    answers = listOf(
                        p("I'm a little tired because I worked late.", "遅くまで働いたので少し疲れています。"),
                        p("I feel relaxed because tomorrow is a holiday.", "明日は休みなので、リラックスしています。"),
                    ),
                    tipJa = "I'm 〜 because …. で、気分と理由をセットで伝えましょう。",
                    reply = p("That makes sense. What are you going to do tonight?", "なるほどね。今夜は何をする予定？"),
                ),
                ScriptStep(
                    taskJa = "今夜の予定を話そう",
                    open = true,
                    answers = listOf(
                        p("I'm going to take a bath and go to bed early.", "お風呂に入って早めに寝るつもりです。"),
                        p("I'm going to watch a drama on Netflix.", "ネットフリックスでドラマを見る予定です。"),
                    ),
                    tipJa = "be going to 〜 で「〜するつもり」と予定を話せます。",
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
                        p("I like playing the guitar.", "ギターを弾くのが好きです。"),
                        p("I love watching anime and reading manga.", "アニメを見たり、マンガを読んだりするのが大好きです。"),
                    ),
                    tipJa = "I like 〜ing. で「〜するのが好き」。love を使うと「大好き」になります。",
                    reply = p("Oh, cool! How did you get into it?", "へえ、いいね！どうやってそれにはまったの？"),
                ),
                ScriptStep(
                    taskJa = "始めたきっかけを話そう",
                    open = true,
                    answers = listOf(
                        p("My friend taught me when I was in high school.", "高校生のときに友達が教えてくれました。"),
                        p("I started it a few years ago because I had a lot of free time.", "時間がたくさんあったので、数年前に始めました。"),
                    ),
                    tipJa = "get into 〜 は「〜にはまる」。きっかけは when や because を使って説明しましょう。",
                    reply = p("That's a great story. How often do you do it?", "いい話だね。どのくらいの頻度でやってるの？"),
                ),
                ScriptStep(
                    taskJa = "どのくらいの頻度でやるか話そう",
                    open = true,
                    answers = listOf(
                        p("I do it almost every day.", "ほぼ毎日やっています。"),
                        p("About twice a week, usually on weekends.", "週に 2 回くらい、たいてい週末です。"),
                    ),
                    tipJa = "頻度は every day / twice a week / once a month のように表します。",
                    reply = p("Wow, nice! What do you like most about it?", "わあ、いいね！一番好きなところはどこ？"),
                ),
                ScriptStep(
                    taskJa = "一番好きなところを話そう",
                    open = true,
                    answers = listOf(
                        p("It helps me relax after a long day.", "長い一日のあとにリラックスできるところです。"),
                        p("I can meet a lot of people who like the same things.", "同じものが好きな人とたくさん出会えるところです。"),
                    ),
                    tipJa = "It helps me 〜. で「〜するのに役立つ」と良さを説明できます。",
                    reply = p("I totally get that. I play the piano a little myself. Is there anything new you want to try?", "すごくわかる。私も少しピアノを弾くんだ。何か新しく挑戦したいことはある？"),
                ),
                ScriptStep(
                    taskJa = "新しく挑戦したいことを話そう",
                    open = true,
                    answers = listOf(
                        p("I want to try surfing someday.", "いつかサーフィンに挑戦したいです。"),
                        p("I'd like to learn how to cook Italian food.", "イタリア料理の作り方を習いたいです。"),
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
                        p("The best trip was to Hokkaido last winter.", "一番よかったのは去年の冬の北海道です。"),
                        p("I went to Taiwan with my friends. It was amazing.", "友達と台湾に行きました。最高でした。"),
                    ),
                    tipJa = "The best trip was to 〜. で「一番の旅行は〜」と答えられます。",
                    reply = p("Oh, I've heard great things about it! What did you do there?", "へえ、すごくいいって聞くよ！そこで何をしたの？"),
                ),
                ScriptStep(
                    taskJa = "旅先でしたことを話そう（過去形で）",
                    open = true,
                    answers = listOf(
                        p("I went skiing and ate a lot of seafood.", "スキーをして、海鮮をたくさん食べました。"),
                        p("We visited night markets and tried a lot of street food.", "夜市に行って、屋台の食べ物をたくさん食べました。"),
                    ),
                    tipJa = "旅行の思い出は went / visited / tried など過去形で話しましょう。",
                    reply = p("That sounds so fun! What was the most memorable moment?", "すごく楽しそう！一番思い出に残っている瞬間は？"),
                ),
                ScriptStep(
                    taskJa = "一番思い出に残っていることを話そう",
                    open = true,
                    answers = listOf(
                        p("The view from the mountain was beautiful.", "山からの景色がきれいでした。"),
                        p("A local family invited us to dinner. They were so kind.", "地元の家族が夕食に招いてくれました。とても親切でした。"),
                    ),
                    tipJa = "memorable は「思い出に残る」。The 〜 was beautiful. のように感想を添えましょう。",
                    reply = p("Wow, I can imagine that. Where do you want to go next?", "わあ、目に浮かぶよ。次はどこに行きたい？"),
                ),
                ScriptStep(
                    taskJa = "次に行きたい場所と理由を話そう",
                    open = true,
                    answers = listOf(
                        p("I want to go to Italy because I love pasta and art.", "パスタと芸術が好きなので、イタリアに行きたいです。"),
                        p("I'd like to visit New York someday to see a musical.", "いつかミュージカルを見にニューヨークに行きたいです。"),
                    ),
                    tipJa = "I want to go to 〜 because …. で行き先と理由をセットで話しましょう。",
                    reply = p("Great choice! Do you prefer traveling alone or with other people?", "いいね！一人旅と誰かと一緒の旅、どっちが好き？"),
                ),
                ScriptStep(
                    taskJa = "一人旅と、誰かと行く旅のどちらが好きか話そう",
                    open = true,
                    answers = listOf(
                        p("I prefer traveling with friends because it's more fun.", "友達と行くほうが楽しいので好きです。"),
                        p("I like traveling alone. I can go wherever I want.", "一人旅が好きです。行きたいところにどこでも行けるので。"),
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
                        p("I work at an IT company in Tokyo.", "東京の IT 企業で働いています。"),
                        p("I'm a university student. I study economics.", "大学生です。経済学を勉強しています。"),
                    ),
                    tipJa = "I work at 〜（会社）／ I work in 〜（業界）。学生なら I study 〜. と専攻を言いましょう。",
                    reply = p("Oh, interesting! What's a typical day like for you?", "へえ、面白いね！ふだんはどんな一日なの？"),
                ),
                ScriptStep(
                    taskJa = "ふだんの 1 日の流れを話そう",
                    open = true,
                    answers = listOf(
                        p("I usually start work at nine and have meetings in the morning.", "たいてい 9 時に仕事を始めて、午前中は会議があります。"),
                        p("I go to classes in the morning and work part-time in the evening.", "午前中は授業に出て、夕方はアルバイトをしています。"),
                    ),
                    tipJa = "usually で習慣を、in the morning / in the evening で時間帯を表します。",
                    reply = p("Sounds busy! What do you enjoy most about it?", "忙しそう！一番楽しいところは？"),
                ),
                ScriptStep(
                    taskJa = "楽しいこと・やりがいを話そう",
                    open = true,
                    answers = listOf(
                        p("I enjoy working with my team.", "チームで働くのが楽しいです。"),
                        p("I like it when I can solve difficult problems.", "難しい問題を解決できたときがうれしいです。"),
                    ),
                    tipJa = "I enjoy 〜ing. で「〜するのが楽しい」と言えます。",
                    reply = p("That's great. And what's the most challenging part?", "いいね。じゃあ一番大変なところは？"),
                ),
                ScriptStep(
                    taskJa = "大変なことを話そう",
                    open = true,
                    answers = listOf(
                        p("Sometimes I have too much work, and I can't go home early.", "仕事が多すぎて、早く帰れないことがあります。"),
                        p("Speaking English in meetings is still difficult for me.", "会議で英語を話すのがまだ難しいです。"),
                    ),
                    tipJa = "challenging は「大変だけどやりがいがある」という前向きな響きの言葉です。",
                    reply = p("I understand. That's not easy. What do you want to do in the future?", "わかるよ。簡単じゃないよね。将来は何をしたい？"),
                ),
                ScriptStep(
                    taskJa = "将来やりたいことを話そう",
                    open = true,
                    answers = listOf(
                        p("I want to work abroad someday.", "いつか海外で働きたいです。"),
                        p("I'd like to start my own business in the future.", "将来は自分で事業を始めたいです。"),
                    ),
                    tipJa = "someday（いつか）や in the future（将来）を使って夢を話しましょう。",
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
                        p("I really want to live abroad for a year.", "1 年間海外に住んでみたいです。"),
                        p("I want to get a better job and save money.", "もっといい仕事に就いて、お金を貯めたいです。"),
                    ),
                    tipJa = "I really want to 〜. で強い気持ちを伝えられます。",
                    reply = p("Oh, that's exciting! Why is that important to you?", "わあ、わくわくするね！どうしてそれが大切なの？"),
                ),
                ScriptStep(
                    taskJa = "その理由を話そう（because を使って）",
                    open = true,
                    answers = listOf(
                        p("Because I want to experience a different culture.", "違う文化を体験したいからです。"),
                        p("Because I want to grow as a person.", "人として成長したいからです。"),
                    ),
                    tipJa = "Because 〜. で理由を答えられます。会話では Because から始めても大丈夫です。",
                    reply = p("That makes a lot of sense. What are you doing now to get there?", "すごく納得。いまはそのために何をしているの？"),
                ),
                ScriptStep(
                    taskJa = "いま取り組んでいることを話そう",
                    open = true,
                    answers = listOf(
                        p("I'm studying English every day with apps.", "アプリで毎日英語を勉強しています。"),
                        p("I'm saving money and reading about different countries.", "お金を貯めて、いろいろな国について調べています。"),
                    ),
                    tipJa = "I'm 〜ing.（現在進行形）で、いま続けていることを表せます。",
                    reply = p("Good for you! How is English going to help you?", "えらいね！英語はどんなふうに役に立ちそう？"),
                ),
                ScriptStep(
                    taskJa = "英語がどう役立つか話そう",
                    open = true,
                    answers = listOf(
                        p("English will help me make friends from all over the world.", "世界中に友達を作るのに役立ちます。"),
                        p("I can use English at work and get more chances.", "仕事で英語を使えて、チャンスが増えます。"),
                    ),
                    tipJa = "〜 will help me …. で「〜が…するのに役立つ」と言えます。",
                    reply = p("Absolutely. Your English is already getting better! What's one small goal for this month?", "その通り。英語はもう上達してきてるよ！今月の小さな目標を 1 つ教えて？"),
                ),
                ScriptStep(
                    taskJa = "今月の小さな目標を話そう",
                    open = true,
                    answers = listOf(
                        p("I want to practice speaking English every day this month.", "今月は毎日英語を話す練習をしたいです。"),
                        p("I'm going to read one English book.", "英語の本を 1 冊読むつもりです。"),
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
