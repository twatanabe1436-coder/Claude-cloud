@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.twatanabe1436.sodateru.ui.recipes

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.data.OcrEngine
import io.github.twatanabe1436.sodateru.data.PendingDraft
import io.github.twatanabe1436.sodateru.data.PhotoStore
import io.github.twatanabe1436.sodateru.ui.HomeTab
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.IconLabel
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.rememberImagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 本・ノートの写真からレシピを読み取る。 */
@Composable
fun ScanScreen(container: AppContainer, navigator: Navigator, category: Category?) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val hasKey by container.secrets.hasApiKey.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var uris by remember { mutableStateOf(listOf<Uri>()) }
    var engine by remember { mutableStateOf(if (settings.ocrEngine == OcrEngine.CLAUDE && hasKey) OcrEngine.CLAUDE else OcrEngine.DEVICE) }
    var attach by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberImagePicker(container.photos, maxItems = 5) { picked -> uris = (uris + picked).distinct().take(5) }

    fun read() {
        busy = true
        error = null
        scope.launch {
            try {
                val draft = container.textReader.readRecipe(uris, engine)
                val names = if (attach) uris.map { container.photos.import(it) } else emptyList()
                container.drafts.pending = PendingDraft(draft, names, if (engine == OcrEngine.CLAUDE) "Claude" else "端末内の文字認識")
                navigator.replace(Route.EditRecipe(useDraft = true, category = category))
            } catch (e: Exception) {
                error = e.message ?: "読み取りに失敗しました"
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("写真から読み取る") },
                navigationIcon = { BackButton { navigator.pop() } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "レシピ本のページや手書きのノートを撮影すると、材料と作り方に分けて下書きを作ります。" +
                    "ページが分かれている場合は、5枚までまとめて読み取れます。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = picker.takePhoto, modifier = Modifier.weight(1f), enabled = !busy) {
                    IconLabel(Icons.Filled.CameraAlt, "撮影する")
                }
                FilledTonalButton(onClick = picker.pickFromGallery, modifier = Modifier.weight(1f), enabled = !busy) {
                    IconLabel(Icons.Filled.PhotoLibrary, "写真を選ぶ")
                }
            }
            if (uris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(uris) { i, uri ->
                        Box {
                            UriThumb(uri, container.photos, Modifier.size(110.dp))
                            IconButton(onClick = { uris = uris.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Filled.Close, contentDescription = "外す")
                            }
                        }
                    }
                }
            }
            SectionCard(title = "読み取り方") {
                OcrEngine.entries.forEach { e ->
                    val enabled = e == OcrEngine.DEVICE || hasKey
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = engine == e, onClick = { engine = e }, enabled = enabled && !busy)
                        Column(Modifier.weight(1f)) {
                            Text(e.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                when (e) {
                                    OcrEngine.DEVICE -> "印刷された文字向き。分け方は自動推測なので、保存前に確認してください"
                                    OcrEngine.CLAUDE -> if (hasKey) {
                                        "手書きのノートも読み取り、材料・手順・メモに振り分けます（通信・少額の従量課金あり）"
                                    } else {
                                        "設定で API キーを入れると使えます"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (!hasKey) {
                    TextButton(onClick = { navigator.goHome(HomeTab.SETTINGS) }) { Text("API キーを設定する") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = attach, onCheckedChange = { attach = it })
                Text("写真をレシピに添付する", style = MaterialTheme.typography.bodyMedium)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { read() },
                enabled = uris.isNotEmpty() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("読み取り中…", fontWeight = FontWeight.Bold)
                } else {
                    IconLabel(Icons.Filled.AutoAwesome, "読み取る", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { navigator.replace(Route.EditRecipe(category = category)) }) {
                Text("写真を使わずに手で入力する")
            }
        }
    }
}

@Composable
fun UriThumb(uri: Uri, store: PhotoStore, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { store.decode(uri, 360)?.asImageBitmap() }.getOrNull() }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let { Image(it, contentDescription = "選んだ写真", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
