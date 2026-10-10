package io.github.twatanabe1436.hanaso.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTest {

    @Test
    fun capitalizesAndAddsPunctuation() {
        assertEquals("I'd like a coffee.", Transcript.fix("i'd like a coffee"))
        assertEquals("Where is the station?", Transcript.fix("  where is the station "))
        assertEquals("Can I pay by card?", Transcript.fix("can i pay by card"))
        assertEquals("Thank you!", Transcript.fix("thank you!"))
        assertEquals("", Transcript.fix("   "))
    }

    @Test
    fun misheardWordsAreFixedTowardTheExpectedOnes() {
        assertEquals(
            "Can I get a medium latte please?",
            Transcript.fix("can i get a medium lotte please", listOf("Can I get a medium latte, please?")),
        )
        assertEquals(
            "I'm allergic to penicillin.",
            Transcript.fix("i'm alergic to penicilin", listOf("I'm allergic to penicillin.")),
        )
        // 固有名詞は相手のセリフにある形にそろえる
        assertEquals(
            "My name is Tanaka.",
            Transcript.fix("my name is tanaka", context = listOf("Hello, Mr. Tanaka. Please have a seat.")),
        )
        assertEquals(
            "See you on Wednesday.",
            Transcript.fix("see you on wednesday", context = listOf("Shall we meet on Wednesday?")),
        )
        // 文頭 (. の後) の大文字は固有名詞ではない
        assertEquals(
            "I think the deadline is on Friday.",
            Transcript.fix("i think the deadline is on friday", context = listOf("Sorry. The deadline is Friday.")),
        )
    }

    @Test
    fun onlyTheTasksWordsAreTargetsNotTheWholeConversation() {
        // 相手のセリフにある似た語 (right) には直さない
        assertEquals(
            "The deadline is too tight.",
            Transcript.fix("the deadline is too tight", context = listOf("That's right. Let's talk about the deadline.")),
        )
        // 語形変化 (時制・複数形) は文法なので直さない
        assertEquals(
            "I watched a movie with my friends.",
            Transcript.fix("i watched a movie with my friends", listOf("I watch movies with my friend.")),
        )
        assertEquals("She studies hard.", Transcript.fix("she studies hard", listOf("I study hard.")))
    }

    @Test
    fun grammarAndRealWordsAreLeftAlone() {
        val expected = listOf("Can I get a large latte, please?", "I'd like to check in.")
        // a の抜けや時制は直さない
        assertEquals("I want large latte.", Transcript.fix("i want large latte", expected))
        assertEquals("Yesterday I go to the hotel.", Transcript.fix("yesterday i go to the hotel", expected))
        // 綴りが近くても、よく使う語は変えない
        assertEquals("Can I got a large latte?", Transcript.fix("can i got a large latte", expected))
        // 全然違う語はそのまま
        assertEquals("I prefer tea.", Transcript.fix("i prefer tea", expected))
    }

    @Test
    fun ambiguousOrDistantWordsAreNotChanged() {
        // tight は light とも night とも 1 文字違い → 決められない
        assertEquals("A tight please.", Transcript.fix("a tight please", listOf("The light, please.", "Good night.")))
        // 短い語は 1 文字違いでも直さない (cold と言ったのかもしれない)
        assertEquals("Cold it please.", Transcript.fix("cold it please", listOf("Hold it, please.")))
        // 2 文字違いで頭の文字も違う語には直さない
        assertEquals("A latte please.", Transcript.fix("a latte please", listOf("I'd like a bottle.")))
        // 数字はそのまま
        assertEquals("Room 305.", Transcript.fix("room 305", listOf("Room 306.")))
    }

    @Test
    fun picksTheAlternativeWithTheTasksWords() {
        val expected = listOf("Can I get a medium latte, please?", "latte|coffee")
        assertEquals(
            "can I get a medium latte please",
            Transcript.pick(listOf("can I get a medium lottie please", "can I get a medium latte please"), expected),
        )
        // a のあるなしでは選ばない (学習者の言い間違いを消さない)
        assertEquals(
            "can I get medium latte",
            Transcript.pick(listOf("can I get medium latte", "can I get a medium latte"), expected),
        )
        assertEquals("hello there", Transcript.pick(listOf("hello there", "hollow there"), emptyList()))
        assertEquals("", Transcript.pick(emptyList(), expected))
    }

    @Test
    fun aiRepairsAreAcceptedOnlyForMisheardWords() {
        assertEquals(true, Transcript.acceptRepair("can i get a medium lot day please", "Can I get a medium latte, please?"))
        assertEquals(true, Transcript.acceptRepair("i'm allergic to penny selin", "I'm allergic to penicillin."))
        // 文法の直しは受け付けない (時制・冠詞・三単現・言い換え)
        assertEquals(false, Transcript.acceptRepair("i go to school yesterday", "I went to school yesterday."))
        assertEquals(false, Transcript.acceptRepair("i watch movie yesterday", "I watched a movie yesterday."))
        assertEquals(false, Transcript.acceptRepair("she study english", "She studies English."))
        assertEquals(false, Transcript.acceptRepair("my favorite food is sushi", "My favorite food is pizza and pasta with cheese."))
        // 変わっていない
        assertEquals(false, Transcript.acceptRepair("i like dogs", "I like dogs."))
        assertEquals(false, Transcript.acceptRepair("i like dogs", ""))
    }

    @Test
    fun inflections() {
        for ((a, b) in listOf("watch" to "watched", "friend" to "friends", "study" to "studies", "stop" to "stopped", "make" to "making")) {
            assertEquals("$a / $b", true, Transcript.inflected(a, b))
        }
        for ((a, b) in listOf("late" to "latte", "move" to "movie", "tight" to "right", "latte" to "latter")) {
            assertEquals("$a / $b", false, Transcript.inflected(a, b))
        }
    }

    @Test
    fun onlyRealWordChangesCount() {
        assertEquals(false, Transcript.wordsChanged("can i get a latte", "Can I get a latte?"))
        assertEquals(true, Transcript.wordsChanged("can i get a lotte", "Can I get a latte?"))
        assertEquals(
            listOf("lotte" to "latte", "penicilin" to "penicillin"),
            Transcript.corrections("i'd like a lotte, not penicilin", "I'd like a latte, not penicillin."),
        )
        assertEquals(emptyList<Pair<String, String>>(), Transcript.corrections("can i get a latte", "Can I get a latte?"))
        assertEquals(
            listOf("lot day" to "latte"),
            Transcript.corrections("can i get a medium lot day please", "Can I get a medium latte, please?"),
        )
    }

    @Test
    fun editDistance() {
        assertEquals(0, Transcript.distance("latte", "latte"))
        assertEquals(1, Transcript.distance("lotte", "latte"))
        assertEquals(1, Transcript.distance("alergic", "allergic"))
        assertEquals(3, Transcript.distance("kitten", "sitting"))
        // 上限を超えたら打ち切る
        assertEquals(2, Transcript.distance("abcdef", "uvwxyz", limit = 1))
    }
}
