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
        "free" -> R.drawable.art_scene_free_fg
        else -> null
    }
}
