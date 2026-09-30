package io.github.twatanabe1436.sodateru.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.twatanabe1436.sodateru.data.PhotoStore
import kotlinx.coroutines.launch

@Composable
fun PhotoThumb(
    name: String,
    store: PhotoStore,
    modifier: Modifier = Modifier,
    maxPx: Int = 480,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxPx) {
        value = store.thumbnail(name, maxPx)?.asImageBitmap()
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let { Image(it, contentDescription = "写真", contentScale = contentScale, modifier = Modifier.fillMaxSize()) }
    }
}

/** カメラ・アルバムから画像を選ぶ。 */
class ImagePicker(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

@Composable
fun rememberImagePicker(store: PhotoStore, maxItems: Int = 5, onPicked: (List<Uri>) -> Unit): ImagePicker {
    val context = LocalContext.current
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCamera?.let(Uri::parse)
        pendingCamera = null
        if (ok && uri != null) onPicked(listOf(uri))
    }
    val multi = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems.coerceAtLeast(2)),
    ) { uris -> if (uris.isNotEmpty()) onPicked(uris.take(maxItems)) }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(listOf(uri))
    }
    return remember(store, maxItems) {
        ImagePicker(
            takePhoto = {
                val uri = store.newCameraUri()
                pendingCamera = uri.toString()
                try {
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    pendingCamera = null
                    Toast.makeText(context, "カメラアプリが見つかりません", Toast.LENGTH_SHORT).show()
                }
            },
            pickFromGallery = {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (maxItems > 1) multi.launch(request) else single.launch(request)
            },
        )
    }
}

/** 「カメラで撮る / アルバムから選ぶ」の選択ダイアログ。 */
@Composable
fun ImageSourceDialog(picker: ImagePicker, onDismiss: () -> Unit, title: String = "写真を追加") {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onDismiss(); picker.takePhoto() }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(Icons.Filled.CameraAlt, "カメラで撮る", modifier = Modifier.weight(1f))
                }
                TextButton(onClick = { onDismiss(); picker.pickFromGallery() }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(Icons.Filled.PhotoLibrary, "アルバムから選ぶ", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

/** 写真の一覧 + 追加・削除。取り込んだ写真はアプリ内にコピーされる。 */
@Composable
fun PhotoEditorRow(
    photos: List<String>,
    store: PhotoStore,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var importing by remember { mutableStateOf(false) }
    var chooseSource by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val picker = rememberImagePicker(store) { uris ->
        scope.launch {
            importing = true
            val names = uris.mapNotNull { runCatching { store.import(it) }.getOrNull() }
            importing = false
            if (names.size < uris.size) Toast.makeText(context, "読み込めない写真がありました", Toast.LENGTH_SHORT).show()
            onChange(photos + names)
        }
    }
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos, key = { it }) { name ->
            Box {
                PhotoThumb(name, store, Modifier.size(size).clickable { viewing = name })
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onChange(photos - name) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "写真を外す", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        item {
            Box(
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(enabled = !importing) { chooseSource = true },
                contentAlignment = Alignment.Center,
            ) {
                if (importing) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                } else {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "写真を追加", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (chooseSource) ImageSourceDialog(picker, onDismiss = { chooseSource = false })
    viewing?.let { PhotoViewer(it, store) { viewing = null } }
}

/** 写真の一覧 (表示だけ)。タップで大きく表示する。 */
@Composable
fun PhotoStrip(photos: List<String>, store: PhotoStore, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    if (photos.isEmpty()) return
    var viewing by remember { mutableStateOf<String?>(null) }
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos, key = { it }) { name ->
            PhotoThumb(name, store, Modifier.size(size).clickable { viewing = name })
        }
    }
    viewing?.let { PhotoViewer(it, store) { viewing = null } }
}

@Composable
fun PhotoViewer(name: String, store: PhotoStore, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            PhotoThumb(
                name,
                store,
                Modifier.fillMaxSize().padding(8.dp),
                maxPx = PhotoStore.MAX_STORED_PX,
                contentScale = ContentScale.Fit,
            )
        }
    }
}
