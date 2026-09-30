package io.github.twatanabe1436.sodateru.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** アプリ内に保存する写真 (filesDir/photos/*.jpg)。取り込むときに縮小・回転補正する。 */
class PhotoStore(private val context: Context) {

    private val dir: File = File(context.filesDir, "photos").apply { mkdirs() }
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun file(name: String): File = File(dir, name)

    fun exists(name: String): Boolean = isValidName(name) && file(name).exists()

    /** カメラアプリに渡す一時ファイルの URI。 */
    fun newCameraUri(): Uri {
        val camDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val f = File(camDir, "shot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }

    /** 写真を取り込んでアプリ内に保存し、ファイル名を返す。 */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decode(uri, MAX_STORED_PX) ?: throw IOException("画像を読み込めませんでした")
        val name = "${UUID.randomUUID()}.jpg"
        file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        name
    }

    /** 文字起こし用に JPEG を base64 で作る (長辺 [maxPx])。 */
    suspend fun jpegBase64(uri: Uri, maxPx: Int = 1568): String = withContext(Dispatchers.IO) {
        val bitmap = decode(uri, maxPx) ?: throw IOException("画像を読み込めませんでした")
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bitmap.recycle()
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun uriOf(name: String): Uri = Uri.fromFile(file(name))

    /** 一覧に出すサムネイル。 */
    suspend fun thumbnail(name: String, maxPx: Int): Bitmap? {
        val key = "$name@$maxPx"
        cache.get(key)?.let { return it }
        if (!exists(name)) return null
        return withContext(Dispatchers.IO) {
            decode(uriOf(name), maxPx, applyExif = false)?.also { cache.put(key, it) }
        }
    }

    /** 画像を長辺 [maxPx] 以下に縮小し、EXIF の向きを反映して読み込む。 */
    fun decode(uri: Uri, maxPx: Int, applyExif: Boolean = true): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val longest = max(bitmap.width, bitmap.height)
        if (longest > maxPx) {
            val scale = maxPx.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }
        if (applyExif) {
            val degrees = resolver.openInputStream(uri)?.use(::rotationOf) ?: 0
            if (degrees != 0) {
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height,
                    Matrix().apply { postRotate(degrees.toFloat()) }, true,
                )
                if (rotated != bitmap) bitmap.recycle()
                bitmap = rotated
            }
        }
        return bitmap
    }

    private fun rotationOf(input: InputStream): Int = try {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: IOException) {
        0
    }

    fun delete(name: String) {
        if (!isValidName(name)) return
        file(name).delete()
        cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach(cache::remove)
    }

    /** バックアップから写真を戻す。同名のファイルがあればそのまま。 */
    fun restore(name: String, input: InputStream) {
        if (!isValidName(name)) return
        val target = file(name)
        if (target.exists()) return
        val tmp = File(dir, "$name.tmp")
        tmp.outputStream().use { input.copyTo(it) }
        tmp.renameTo(target)
    }

    /** どのレシピ・記録からも使われていない写真のうち、1 日以上前のものを消す (編集途中で破棄した写真など)。 */
    fun cleanupOrphans(referenced: Set<String>) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { f ->
            if (f.name !in referenced && f.lastModified() < cutoff) f.delete()
        }
        File(context.cacheDir, "camera").listFiles()?.forEach { f -> if (f.lastModified() < cutoff) f.delete() }
    }

    companion object {
        const val MAX_STORED_PX = 1600
        private val NAME = Regex("^[A-Za-z0-9._-]+\\.jpg$")

        fun isValidName(name: String): Boolean = NAME.matches(name) && !name.startsWith(".")
    }
}
