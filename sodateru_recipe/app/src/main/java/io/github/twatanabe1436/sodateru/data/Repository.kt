package io.github.twatanabe1436.sodateru.data

import io.github.twatanabe1436.sodateru.core.DataCodec
import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** レシピと記録の読み書き。全件をメモリに持ち、変更は SQLite に書いてから反映する。 */
class Repository(
    private val db: LocalDatabase,
    private val photos: PhotoStore,
    scope: CoroutineScope,
) {
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val mutex = Mutex()

    init {
        scope.launch {
            val loadedData = withContext(Dispatchers.IO) {
                db.loadAll().also { photos.cleanupOrphans(DataCodec.photoNames(it)) }
            }
            mutex.withLock { _data.value = loadedData }
            _loaded.value = true
        }
    }

    fun recipe(id: String): Recipe? = _data.value.recipes.firstOrNull { it.id == id }

    fun log(id: String): CookLog? = _data.value.logs.firstOrNull { it.id == id }

    suspend fun saveRecipe(recipe: Recipe) = mutex.withLock {
        withContext(Dispatchers.IO) { db.upsertRecipe(recipe) }
        val list = _data.value.recipes
        val idx = list.indexOfFirst { it.id == recipe.id }
        _data.value = _data.value.copy(recipes = if (idx < 0) list + recipe else list.toMutableList().also { it[idx] = recipe })
    }

    suspend fun saveLog(log: CookLog) = mutex.withLock {
        withContext(Dispatchers.IO) { db.upsertLog(log) }
        val list = _data.value.logs
        val idx = list.indexOfFirst { it.id == log.id }
        _data.value = _data.value.copy(logs = if (idx < 0) list + log else list.toMutableList().also { it[idx] = log })
    }

    suspend fun deleteRecipe(id: String) = mutex.withLock {
        val before = _data.value
        withContext(Dispatchers.IO) { db.deleteRecipe(id) }
        val after = before.copy(recipes = before.recipes.filterNot { it.id == id }, logs = before.logs.filterNot { it.recipeId == id })
        _data.value = after
        deleteUnreferencedPhotos(before, after)
    }

    suspend fun deleteLog(id: String) = mutex.withLock {
        val before = _data.value
        withContext(Dispatchers.IO) { db.deleteLog(id) }
        val after = before.copy(logs = before.logs.filterNot { it.id == id })
        _data.value = after
        deleteUnreferencedPhotos(before, after)
    }

    suspend fun replaceAll(newData: AppData) = mutex.withLock {
        withContext(Dispatchers.IO) { db.replaceAll(newData) }
        _data.value = newData.copy(exportedAt = 0)
    }

    suspend fun mergeIn(incoming: AppData) {
        val merged = mutex.withLock { DataCodec.merge(_data.value, incoming) }
        replaceAll(merged)
    }

    private suspend fun deleteUnreferencedPhotos(before: AppData, after: AppData) {
        val removed = DataCodec.photoNames(before) - DataCodec.photoNames(after)
        withContext(Dispatchers.IO) { removed.forEach(photos::delete) }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** 写真の読み取り結果を、読み取り画面からレシピ編集画面へ渡すための置き場所。 */
class DraftHolder {
    var pending: PendingDraft? = null
}

data class PendingDraft(
    val draft: io.github.twatanabe1436.sodateru.core.RecipeDraft,
    val photos: List<String>,
    val engineLabel: String,
)
