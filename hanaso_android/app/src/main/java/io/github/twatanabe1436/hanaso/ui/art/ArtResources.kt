// art/build.py で生成。手で直さない
package io.github.twatanabe1436.hanaso.ui.art

import androidx.annotation.DrawableRes
import io.github.twatanabe1436.hanaso.R

/** イラストの画像 (人物・場面) */
internal object ArtResources {
    /** 場面 id → 相手の人物 id */
    val partner: Map<String, String> = mapOf(
        "cafe" to "jamie",
        "directions" to "pat",
        "restaurant" to "marco",
        "immigration" to "reed",
        "hotel" to "olivia",
        "shopping" to "sam",
        "party" to "chris",
        "weekend" to "taylor",
        "doctor" to "drlee",
        "coworker" to "jordan",
        "meeting" to "morgan",
        "interview" to "carter",
        "negotiation" to "dana",
        "complaint" to "casey",
        "debate" to "riley",
        "qanda" to "patel",
        "airport" to "nina",
        "lostfound" to "greg",
        "reservation" to "lucy",
        "neighbor" to "helen",
        "homestay" to "linda",
        "favor" to "ben",
        "deadline" to "karen",
        "feedback" to "leo",
        "flight" to "grace",
        "fraud" to "kevin",
        "noise" to "jake",
        "movie" to "mia",
        "free" to "alex",
    )

    /** リーグの表彰台に立つ人物 (1 位・2 位・3 位) と位置 (x, y, 大きさ。720x320 の座標) */
    val podium: List<Placement> = listOf(
        Placement("alex", 260f, 40f, 200f),
        Placement("jamie", 102f, 92f, 175f),
        Placement("marco", 448f, 118f, 165f),
    )

    @DrawableRes
    fun character(person: String, mood: Mood): Int? = when (person) {
        "jamie" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_jamie_normal
            Mood.HAPPY -> R.drawable.art_char_jamie_happy
            Mood.PUZZLED -> R.drawable.art_char_jamie_puzzled
        }
        "pat" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_pat_normal
            Mood.HAPPY -> R.drawable.art_char_pat_happy
            Mood.PUZZLED -> R.drawable.art_char_pat_puzzled
        }
        "marco" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_marco_normal
            Mood.HAPPY -> R.drawable.art_char_marco_happy
            Mood.PUZZLED -> R.drawable.art_char_marco_puzzled
        }
        "reed" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_reed_normal
            Mood.HAPPY -> R.drawable.art_char_reed_happy
            Mood.PUZZLED -> R.drawable.art_char_reed_puzzled
        }
        "olivia" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_olivia_normal
            Mood.HAPPY -> R.drawable.art_char_olivia_happy
            Mood.PUZZLED -> R.drawable.art_char_olivia_puzzled
        }
        "sam" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_sam_normal
            Mood.HAPPY -> R.drawable.art_char_sam_happy
            Mood.PUZZLED -> R.drawable.art_char_sam_puzzled
        }
        "chris" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_chris_normal
            Mood.HAPPY -> R.drawable.art_char_chris_happy
            Mood.PUZZLED -> R.drawable.art_char_chris_puzzled
        }
        "taylor" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_taylor_normal
            Mood.HAPPY -> R.drawable.art_char_taylor_happy
            Mood.PUZZLED -> R.drawable.art_char_taylor_puzzled
        }
        "drlee" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_drlee_normal
            Mood.HAPPY -> R.drawable.art_char_drlee_happy
            Mood.PUZZLED -> R.drawable.art_char_drlee_puzzled
        }
        "jordan" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_jordan_normal
            Mood.HAPPY -> R.drawable.art_char_jordan_happy
            Mood.PUZZLED -> R.drawable.art_char_jordan_puzzled
        }
        "morgan" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_morgan_normal
            Mood.HAPPY -> R.drawable.art_char_morgan_happy
            Mood.PUZZLED -> R.drawable.art_char_morgan_puzzled
        }
        "carter" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_carter_normal
            Mood.HAPPY -> R.drawable.art_char_carter_happy
            Mood.PUZZLED -> R.drawable.art_char_carter_puzzled
        }
        "dana" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_dana_normal
            Mood.HAPPY -> R.drawable.art_char_dana_happy
            Mood.PUZZLED -> R.drawable.art_char_dana_puzzled
        }
        "casey" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_casey_normal
            Mood.HAPPY -> R.drawable.art_char_casey_happy
            Mood.PUZZLED -> R.drawable.art_char_casey_puzzled
        }
        "riley" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_riley_normal
            Mood.HAPPY -> R.drawable.art_char_riley_happy
            Mood.PUZZLED -> R.drawable.art_char_riley_puzzled
        }
        "patel" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_patel_normal
            Mood.HAPPY -> R.drawable.art_char_patel_happy
            Mood.PUZZLED -> R.drawable.art_char_patel_puzzled
        }
        "alex" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_alex_normal
            Mood.HAPPY -> R.drawable.art_char_alex_happy
            Mood.PUZZLED -> R.drawable.art_char_alex_puzzled
        }
        "nina" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_nina_normal
            Mood.HAPPY -> R.drawable.art_char_nina_happy
            Mood.PUZZLED -> R.drawable.art_char_nina_puzzled
        }
        "greg" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_greg_normal
            Mood.HAPPY -> R.drawable.art_char_greg_happy
            Mood.PUZZLED -> R.drawable.art_char_greg_puzzled
        }
        "lucy" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_lucy_normal
            Mood.HAPPY -> R.drawable.art_char_lucy_happy
            Mood.PUZZLED -> R.drawable.art_char_lucy_puzzled
        }
        "helen" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_helen_normal
            Mood.HAPPY -> R.drawable.art_char_helen_happy
            Mood.PUZZLED -> R.drawable.art_char_helen_puzzled
        }
        "linda" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_linda_normal
            Mood.HAPPY -> R.drawable.art_char_linda_happy
            Mood.PUZZLED -> R.drawable.art_char_linda_puzzled
        }
        "ben" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_ben_normal
            Mood.HAPPY -> R.drawable.art_char_ben_happy
            Mood.PUZZLED -> R.drawable.art_char_ben_puzzled
        }
        "karen" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_karen_normal
            Mood.HAPPY -> R.drawable.art_char_karen_happy
            Mood.PUZZLED -> R.drawable.art_char_karen_puzzled
        }
        "leo" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_leo_normal
            Mood.HAPPY -> R.drawable.art_char_leo_happy
            Mood.PUZZLED -> R.drawable.art_char_leo_puzzled
        }
        "grace" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_grace_normal
            Mood.HAPPY -> R.drawable.art_char_grace_happy
            Mood.PUZZLED -> R.drawable.art_char_grace_puzzled
        }
        "kevin" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_kevin_normal
            Mood.HAPPY -> R.drawable.art_char_kevin_happy
            Mood.PUZZLED -> R.drawable.art_char_kevin_puzzled
        }
        "jake" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_jake_normal
            Mood.HAPPY -> R.drawable.art_char_jake_happy
            Mood.PUZZLED -> R.drawable.art_char_jake_puzzled
        }
        "mia" -> when (mood) {
            Mood.NORMAL -> R.drawable.art_char_mia_normal
            Mood.HAPPY -> R.drawable.art_char_mia_happy
            Mood.PUZZLED -> R.drawable.art_char_mia_puzzled
        }
        else -> null
    }

    @DrawableRes
    fun sceneBackground(scene: String): Int? = when (scene) {
        "cafe" -> R.drawable.art_scene_cafe_bg
        "directions" -> R.drawable.art_scene_directions_bg
        "restaurant" -> R.drawable.art_scene_restaurant_bg
        "immigration" -> R.drawable.art_scene_immigration_bg
        "hotel" -> R.drawable.art_scene_hotel_bg
        "shopping" -> R.drawable.art_scene_shopping_bg
        "party" -> R.drawable.art_scene_party_bg
        "weekend" -> R.drawable.art_scene_weekend_bg
        "doctor" -> R.drawable.art_scene_doctor_bg
        "coworker" -> R.drawable.art_scene_coworker_bg
        "meeting" -> R.drawable.art_scene_meeting_bg
        "interview" -> R.drawable.art_scene_interview_bg
        "negotiation" -> R.drawable.art_scene_negotiation_bg
        "complaint" -> R.drawable.art_scene_complaint_bg
        "debate" -> R.drawable.art_scene_debate_bg
        "qanda" -> R.drawable.art_scene_qanda_bg
        "airport" -> R.drawable.art_scene_airport_bg
        "lostfound" -> R.drawable.art_scene_lostfound_bg
        "reservation" -> R.drawable.art_scene_reservation_bg
        "neighbor" -> R.drawable.art_scene_neighbor_bg
        "homestay" -> R.drawable.art_scene_homestay_bg
        "favor" -> R.drawable.art_scene_favor_bg
        "deadline" -> R.drawable.art_scene_deadline_bg
        "feedback" -> R.drawable.art_scene_feedback_bg
        "flight" -> R.drawable.art_scene_flight_bg
        "fraud" -> R.drawable.art_scene_fraud_bg
        "noise" -> R.drawable.art_scene_noise_bg
        "movie" -> R.drawable.art_scene_movie_bg
        "free" -> R.drawable.art_scene_free_bg
        else -> null
    }

    @DrawableRes
    fun sceneForeground(scene: String): Int? = when (scene) {
        "cafe" -> R.drawable.art_scene_cafe_fg
        "directions" -> R.drawable.art_scene_directions_fg
        "restaurant" -> R.drawable.art_scene_restaurant_fg
        "immigration" -> R.drawable.art_scene_immigration_fg
        "hotel" -> R.drawable.art_scene_hotel_fg
        "shopping" -> R.drawable.art_scene_shopping_fg
        "party" -> R.drawable.art_scene_party_fg
        "weekend" -> R.drawable.art_scene_weekend_fg
        "doctor" -> R.drawable.art_scene_doctor_fg
        "coworker" -> R.drawable.art_scene_coworker_fg
        "meeting" -> R.drawable.art_scene_meeting_fg
        "interview" -> R.drawable.art_scene_interview_fg
        "negotiation" -> R.drawable.art_scene_negotiation_fg
        "complaint" -> R.drawable.art_scene_complaint_fg
        "debate" -> R.drawable.art_scene_debate_fg
        "qanda" -> R.drawable.art_scene_qanda_fg
        "airport" -> R.drawable.art_scene_airport_fg
        "lostfound" -> R.drawable.art_scene_lostfound_fg
        "reservation" -> R.drawable.art_scene_reservation_fg
        "neighbor" -> R.drawable.art_scene_neighbor_fg
        "homestay" -> R.drawable.art_scene_homestay_fg
        "favor" -> R.drawable.art_scene_favor_fg
        "deadline" -> R.drawable.art_scene_deadline_fg
        "feedback" -> R.drawable.art_scene_feedback_fg
        "flight" -> R.drawable.art_scene_flight_fg
        "fraud" -> R.drawable.art_scene_fraud_fg
        "noise" -> R.drawable.art_scene_noise_fg
        "movie" -> R.drawable.art_scene_movie_fg
        "free" -> R.drawable.art_scene_free_fg
        else -> null
    }
}
