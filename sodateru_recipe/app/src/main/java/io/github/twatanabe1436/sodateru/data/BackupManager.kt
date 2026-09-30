package io.github.twatanabe1436.sodateru.data

import android.content.Context
import android.net.Uri
import io.github.twatanabe1436.sodateru.core.BackupFormatException
import io.github.twatanabe1436.sodateru.core.DataCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(val recipes: Int, val logs: Int, val photos: Int)

/**
 * 写真も含めたバックアップ (ZIP: data.json と photos フォルダの JPEG) の書き出しと読み込み。
 * 機種変更のときや、アプリを入れ直すときに使う。
 */
class BackupManager(
    private val context: Context,
    private val repository: Repository,
    private val photos: PhotoStore,
    private val settings: SettingsStore,
) {

    suspend fun export(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val data = repository.data.value.copy(exportedAt = now)
        var photoCount = 0
        val out = context.contentResolver.openOutputStream(uri) ?: throw IOException("保存先を開けませんでした")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(DataCodec.encodeBackup(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (name in DataCodec.photoNames(data)) {
                if (!photos.exists(name)) continue
                zip.putNextEntry(ZipEntry("$PHOTO_DIR$name"))
                photos.file(name).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                photoCount++
            }
        }
        settings.update { it.copy(lastBackupAt = now) }
        BackupSummary(data.recipes.size, data.logs.size, photoCount)
    }

    /** @param replace true なら今のデータを消してバックアップの内容に置き換える。false なら追加・更新のみ。 */
    suspend fun import(uri: Uri, replace: Boolean): BackupSummary {
        val (data, photoCount) = withContext(Dispatchers.IO) {
            var json: String? = null
            var count = 0
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("ファイルを開けませんでした")
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == DATA_ENTRY -> json = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith(PHOTO_DIR) -> {
                            val name = entry.name.removePrefix(PHOTO_DIR)
                            if (PhotoStore.isValidName(name)) {
                                photos.restore(name, zip)
                                count++
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val text = json ?: throw BackupFormatException("そだてるレシピのバックアップファイルではありません")
            DataCodec.decodeBackup(text) to count
        }
        if (replace) repository.replaceAll(data) else repository.mergeIn(data)
        return BackupSummary(data.recipes.size, data.logs.size, photoCount)
    }

    companion object {
        const val DATA_ENTRY = "data.json"
        const val PHOTO_DIR = "photos/"
    }
}
