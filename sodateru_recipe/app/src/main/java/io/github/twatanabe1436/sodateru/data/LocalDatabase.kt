package io.github.twatanabe1436.sodateru.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import io.github.twatanabe1436.sodateru.core.DataCodec
import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe

/**
 * 端末内の保存先。レシピと記録を 1 件 1 行の JSON として SQLite に入れる。
 * 形の変更は JSON 側 (未知の項目は無視・欠けた項目は既定値) で吸収する。
 */
class LocalDatabase(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE recipes (id TEXT PRIMARY KEY, json TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL(
            "CREATE TABLE logs (id TEXT PRIMARY KEY, recipe_id TEXT NOT NULL, json TEXT NOT NULL, updated_at INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX logs_recipe ON logs(recipe_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun loadAll(): AppData {
        val db = readableDatabase
        val recipes = mutableListOf<Recipe>()
        db.rawQuery("SELECT json FROM recipes", null).use { c ->
            while (c.moveToNext()) {
                runCatching { DataCodec.decodeRecipe(c.getString(0)) }
                    .onSuccess(recipes::add)
                    .onFailure { Log.e(TAG, "skip broken recipe row", it) }
            }
        }
        val logs = mutableListOf<CookLog>()
        db.rawQuery("SELECT json FROM logs", null).use { c ->
            while (c.moveToNext()) {
                runCatching { DataCodec.decodeLog(c.getString(0)) }
                    .onSuccess(logs::add)
                    .onFailure { Log.e(TAG, "skip broken log row", it) }
            }
        }
        return AppData(recipes = recipes, logs = logs)
    }

    fun upsertRecipe(recipe: Recipe) {
        writableDatabase.insertWithOnConflict("recipes", null, recipe.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertLog(log: CookLog) {
        writableDatabase.insertWithOnConflict("logs", null, log.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteRecipe(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("logs", "recipe_id = ?", arrayOf(id))
            db.delete("recipes", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteLog(id: String) {
        writableDatabase.delete("logs", "id = ?", arrayOf(id))
    }

    fun replaceAll(data: AppData) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("logs", null, null)
            db.delete("recipes", null, null)
            data.recipes.forEach { db.insertOrThrow("recipes", null, it.toValues()) }
            data.logs.forEach { db.insertOrThrow("logs", null, it.toValues()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Recipe.toValues() = ContentValues().apply {
        put("id", id)
        put("json", DataCodec.encodeRecipe(this@toValues))
        put("updated_at", updatedAt)
    }

    private fun CookLog.toValues() = ContentValues().apply {
        put("id", id)
        put("recipe_id", recipeId)
        put("json", DataCodec.encodeLog(this@toValues))
        put("updated_at", updatedAt)
    }

    private companion object {
        const val NAME = "sodateru.db"
        const val VERSION = 1
        const val TAG = "LocalDatabase"
    }
}
