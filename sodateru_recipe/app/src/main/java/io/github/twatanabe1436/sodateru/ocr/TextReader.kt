package io.github.twatanabe1436.sodateru.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import io.github.twatanabe1436.sodateru.core.RecipeDraft
import io.github.twatanabe1436.sodateru.core.RecipeTextParser
import io.github.twatanabe1436.sodateru.core.claude.ClaudeReadException
import io.github.twatanabe1436.sodateru.core.claude.ClaudeRecipeReader
import io.github.twatanabe1436.sodateru.core.claude.ImageInput
import io.github.twatanabe1436.sodateru.core.claude.ReadMode
import io.github.twatanabe1436.sodateru.data.OcrEngine
import io.github.twatanabe1436.sodateru.data.PhotoStore
import io.github.twatanabe1436.sodateru.data.SecretStore
import io.github.twatanabe1436.sodateru.data.SettingsStore
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * 写真の文字起こし。端末内 (ML Kit・日本語モデル同梱) と Claude API (高精度) の 2 通り。
 */
class TextReader(
    private val context: Context,
    private val photos: PhotoStore,
    private val settings: SettingsStore,
    private val secrets: SecretStore,
) {

    /** レシピとして読み取り、材料・手順に振り分けた下書きを返す。 */
    suspend fun readRecipe(uris: List<Uri>, engine: OcrEngine): RecipeDraft = when (engine) {
        OcrEngine.DEVICE -> RecipeTextParser.parse(readTextOnDevice(uris))
        OcrEngine.CLAUDE -> readWithClaude(uris, ReadMode.RECIPE)
    }

    /** メモとして読み取り、文字だけを返す。 */
    suspend fun readNote(uris: List<Uri>, engine: OcrEngine): String = when (engine) {
        OcrEngine.DEVICE -> readTextOnDevice(uris)
        OcrEngine.CLAUDE -> readWithClaude(uris, ReadMode.NOTE).transcript
    }

    suspend fun readTextOnDevice(uris: List<Uri>): String {
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        try {
            val texts = uris.map { uri ->
                val bitmap = withContext(Dispatchers.IO) { photos.decode(uri, 2048) }
                    ?: throw IOException("画像を読み込めませんでした")
                recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
            }
            return texts.joinToString("\n\n").trim()
        } finally {
            recognizer.close()
        }
    }

    private suspend fun readWithClaude(uris: List<Uri>, mode: ReadMode): RecipeDraft {
        val key = secrets.apiKey() ?: throw ClaudeReadException("Claude の API キーが設定されていません。設定画面で入力してください")
        val images = uris.map { ImageInput(photos.jpegBase64(it)) }
        val model = settings.current.claudeModel
        return withContext(Dispatchers.IO) { ClaudeRecipeReader(key, model).read(images, mode) }
    }

    suspend fun verifyKey(key: String, model: String) {
        withContext(Dispatchers.IO) { ClaudeRecipeReader(key, model).verify() }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
