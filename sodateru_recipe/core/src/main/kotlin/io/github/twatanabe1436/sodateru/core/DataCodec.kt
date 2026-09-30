package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** 端末内保存とバックアップファイルで使う JSON 形式。 */
object DataCodec {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun encodeRecipe(recipe: Recipe): String = json.encodeToString(Recipe.serializer(), recipe)
    fun decodeRecipe(text: String): Recipe = json.decodeFromString(Recipe.serializer(), text)
    fun encodeLog(log: CookLog): String = json.encodeToString(CookLog.serializer(), log)
    fun decodeLog(text: String): CookLog = json.decodeFromString(CookLog.serializer(), text)

    fun encodeBackup(data: AppData): String = json.encodeToString(AppData.serializer(), data)

    fun decodeBackup(text: String): AppData {
        val data = try {
            json.decodeFromString(AppData.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("バックアップファイルの形式が正しくありません", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("バックアップファイルの内容が壊れています", e)
        }
        if (data.schemaVersion > AppData.SCHEMA_VERSION) {
            throw BackupFormatException("新しい版のアプリで作られたバックアップです。アプリを更新してから読み込んでください")
        }
        return data
    }

    /**
     * バックアップを今のデータに取り込む。同じ ID のものは更新日時が新しいほうを残す。
     */
    fun merge(current: AppData, incoming: AppData): AppData {
        val recipes = LinkedHashMap<String, Recipe>()
        current.recipes.forEach { recipes[it.id] = it }
        incoming.recipes.forEach { r ->
            val existing = recipes[r.id]
            if (existing == null || r.updatedAt > existing.updatedAt) recipes[r.id] = r
        }
        val logs = LinkedHashMap<String, CookLog>()
        current.logs.forEach { logs[it.id] = it }
        incoming.logs.forEach { l ->
            val existing = logs[l.id]
            if (existing == null || l.updatedAt > existing.updatedAt) logs[l.id] = l
        }
        return AppData(recipes = recipes.values.toList(), logs = logs.values.toList())
    }

    /** データが参照している写真ファイル名。 */
    fun photoNames(data: AppData): Set<String> =
        (data.recipes.flatMap { r -> r.versions.flatMap { it.photos } } + data.logs.flatMap { it.photos }).toSet()
}
