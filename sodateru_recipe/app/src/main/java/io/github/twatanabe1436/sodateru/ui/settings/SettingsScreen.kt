@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.BuildConfig
import io.github.twatanabe1436.sodateru.core.claude.ClaudeReadException
import io.github.twatanabe1436.sodateru.core.claude.ClaudeRecipeReader
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.data.OcrEngine
import io.github.twatanabe1436.sodateru.data.SampleData
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.components.ChoiceChips
import io.github.twatanabe1436.sodateru.ui.components.ConfirmDialog
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.NumberField
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(container: AppContainer, navigator: Navigator, bottomBar: @Composable () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val hasKey by container.secrets.hasApiKey.collectAsStateWithLifecycle()
    val data by container.repository.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyDialog by remember { mutableStateOf(false) }
    var confirmClearKey by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmRemoveSamples by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val s = container.backup.export(uri)
                toast("書き出しました（レシピ${s.recipes}件・記録${s.logs}件・写真${s.photos}枚）")
            } catch (e: Exception) {
                toast("書き出しに失敗しました: ${e.message}")
            } finally {
                busy = false
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri = uri
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("設定", fontWeight = FontWeight.Bold) }) },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "ocr") {
                SectionCard(title = "写真の文字起こし", icon = Icons.Filled.DocumentScanner) {
                    Text("はじめに選ばれる読み取り方", style = MaterialTheme.typography.labelLarge)
                    OcrEngine.entries.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.ocrEngine == e, onClick = { container.settings.update { it.copy(ocrEngine = e) } })
                            Text(e.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            if (hasKey) "  Claude の API キー: 設定済み" else "  Claude の API キー: 未設定",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { keyDialog = true }) { Text(if (hasKey) "キーを変更" else "キーを入力") }
                        if (hasKey) OutlinedButton(onClick = { confirmClearKey = true }) { Text("キーを削除") }
                    }
                    Text("使うモデル", style = MaterialTheme.typography.labelLarge)
                    ClaudeRecipeReader.MODELS.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.claudeModel == m.id, onClick = { container.settings.update { it.copy(claudeModel = m.id) } })
                            Text(m.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        "高精度モードは、写真を Anthropic の Claude API に送って読み取ります。API キーは Anthropic Console " +
                            "（console.anthropic.com）で発行でき、利用した分だけ料金がかかります（写真1枚でおおむね数円〜十数円）。" +
                            "キーはこの端末の中に暗号化して保存し、バックアップファイルには含めません。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "bread") {
                SectionCard(title = "パンの初期値", icon = Icons.Filled.BakeryDining) {
                    NumberField(
                        settings.targetDoughTemp,
                        { v -> if (v != null) container.settings.update { it.copy(targetDoughTemp = v) } },
                        "目標こね上げ温度",
                        Modifier.fillMaxWidth(),
                        "℃",
                    )
                    Text("いつものこね方", style = MaterialTheme.typography.labelLarge)
                    ChoiceChips(MixingMethod.entries, settings.mixingMethod, { it.label }, { m ->
                        if (m != null) container.settings.update { it.copy(mixingMethod = m) }
                    })
                }
            }
            item(key = "backup") {
                SectionCard(title = "バックアップ", icon = Icons.Filled.Backup) {
                    Text(
                        if (settings.lastBackupAt > 0) "最後の書き出し: ${Fmt.date(settings.lastBackupAt)}（${Fmt.ago(settings.lastBackupAt)}）" else "まだ書き出していません",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "レシピ・記録・写真をまとめて1つのファイル（.zip）に書き出します。Google ドライブなどに保存しておくと、" +
                            "機種変更やアプリの入れ直しのときに読み込んで元に戻せます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (busy) CircularProgressIndicator(Modifier.size(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = { exportLauncher.launch("sodateru-backup-${LocalDate.now()}.zip") }) {
                            Icon(Icons.Filled.Backup, contentDescription = null)
                            Text(" 書き出す")
                        }
                        OutlinedButton(enabled = !busy, onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Text(" 読み込む")
                        }
                    }
                    Text(
                        "※ Android の自動バックアップ（Google ドライブ）にもレシピと記録は含まれますが、写真は含まれません。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "sample") {
                SectionCard(title = "お試し", icon = Icons.Filled.Science) {
                    Text(
                        "使い方を試せるサンプル（山型食パンの焼成ログ7回分、生姜焼きの2つの版）を追加できます。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                container.repository.mergeIn(SampleData.create())
                                toast("サンプルを追加しました")
                            }
                        }) { Text("サンプルを追加") }
                        if (data.recipes.any { it.id.startsWith("sample-") }) {
                            TextButton(onClick = { confirmRemoveSamples = true }) { Text("サンプルを削除") }
                        }
                    }
                }
            }
            item(key = "about") {
                SectionCard(title = "このアプリについて", icon = Icons.Filled.Info) {
                    Text(
                        "そだてるレシピ ${BuildConfig.VERSION_NAME}\n" +
                            "作るたびのアレンジを記録してレシピに反映し、レシピを育てていくノートです。" +
                            "パンは焼成ログから、室温・湿度に合わせた加水・水温・発酵時間を提案します。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "レシピ ${data.recipes.size}件 ・ 記録 ${data.logs.size}件",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (keyDialog) {
        ApiKeyDialog(
            onDismiss = { keyDialog = false },
            onSave = { key, verify ->
                if (!verify) {
                    container.secrets.setApiKey(key)
                    toast("API キーを保存しました")
                    keyDialog = false
                    null
                } else {
                    try {
                        container.textReader.verifyKey(key, settings.claudeModel)
                        container.secrets.setApiKey(key)
                        toast("API キーを確認して保存しました")
                        keyDialog = false
                        null
                    } catch (e: ClaudeReadException) {
                        e.message
                    } catch (e: Exception) {
                        "確認できませんでした（${e.message}）"
                    }
                }
            },
        )
    }
    if (confirmClearKey) {
        ConfirmDialog(
            title = "API キーを削除しますか？",
            text = "高精度モード（Claude）の読み取りが使えなくなります。",
            confirmLabel = "削除する",
            destructive = true,
            onConfirm = { container.secrets.clearApiKey() },
            onDismiss = { confirmClearKey = false },
        )
    }
    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text("バックアップを読み込む") },
            text = {
                Text(
                    "「追加する」は今のデータを残したまま、バックアップの内容を足します（同じレシピは新しいほうを残します）。\n" +
                        "「置き換える」は今のデータを消して、バックアップの内容だけにします。",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { importUri = null; runImport(container, uri, false, { busy = it }, ::toast, scope) }) { Text("追加する") }
                    TextButton(onClick = { importUri = null; runImport(container, uri, true, { busy = it }, ::toast, scope) }) {
                        Text("置き換える", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("キャンセル") } },
        )
    }
    if (confirmRemoveSamples) {
        ConfirmDialog(
            title = "サンプルを削除しますか？",
            text = "サンプルのレシピと、その記録を削除します。",
            confirmLabel = "削除する",
            destructive = true,
            onConfirm = {
                scope.launch {
                    data.recipes.filter { it.id.startsWith("sample-") }.forEach { container.repository.deleteRecipe(it.id) }
                }
            },
            onDismiss = { confirmRemoveSamples = false },
        )
    }
}

private fun runImport(
    container: AppContainer,
    uri: Uri,
    replace: Boolean,
    setBusy: (Boolean) -> Unit,
    toast: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    setBusy(true)
    scope.launch {
        try {
            val s = container.backup.import(uri, replace)
            toast("読み込みました（レシピ${s.recipes}件・記録${s.logs}件・写真${s.photos}枚）")
        } catch (e: Exception) {
            toast("読み込みに失敗しました: ${e.message}")
        } finally {
            setBusy(false)
        }
    }
}

@Composable
private fun ApiKeyDialog(onDismiss: () -> Unit, onSave: suspend (String, Boolean) -> String?) {
    var key by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!checking) onDismiss() },
        title = { Text("Claude の API キー") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim(); error = null },
                    label = { Text("sk-ant- で始まるキー") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (checking) CircularProgressIndicator(Modifier.size(24.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "「確認して保存」はキーが使えるかを通信して確かめます（料金はかかりません）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(enabled = key.isNotBlank() && !checking, onClick = {
                    scope.launch { error = onSave(key, false) }
                }) { Text("そのまま保存") }
                TextButton(enabled = key.isNotBlank() && !checking, onClick = {
                    checking = true
                    scope.launch {
                        error = onSave(key, true)
                        checking = false
                    }
                }) { Text("確認して保存") }
            }
        },
        dismissButton = { TextButton(enabled = !checking, onClick = onDismiss) { Text("キャンセル") } },
    )
}
