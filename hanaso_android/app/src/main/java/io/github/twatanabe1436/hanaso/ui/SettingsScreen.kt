package io.github.twatanabe1436.hanaso.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.core.ClaudeEngine
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.data.Settings
import io.github.twatanabe1436.hanaso.speech.VoiceStatus
import io.github.twatanabe1436.hanaso.ui.talk.errorText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

private val MODELS = listOf(
    ClaudeEngine.DEFAULT_MODEL to "高品質",
    "claude-sonnet-5-5" to "速い・安い",
)

private val SILENCE_CHOICES = listOf(1500L to "短め", 2000L to "ふつう", 3500L to "長め")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.store.settings.collectAsStateWithLifecycle()
    val apiKey by app.store.apiKey.collectAsStateWithLifecycle()
    val voice by app.speaker.status.collectAsStateWithLifecycle()
    var keyInput by rememberSaveable { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    fun update(transform: (Settings) -> Settings) = app.store.updateSettings(transform)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("設定", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SettingsCard("会話の相手") {
            val useAi = apiKey.isNotBlank() && settings.aiConversation
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !useAi,
                    onClick = { update { it.copy(aiConversation = false) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("📖 台本（無料）") }
                SegmentedButton(
                    selected = useAi,
                    onClick = {
                        update { it.copy(aiConversation = true) }
                        if (apiKey.isBlank()) context.toast("AI 会話を使うには、下で Claude の API キーを設定してください")
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("🤖 AI 会話") }
            }
            Hint(
                if (useAi) {
                    "Claude と自由に会話します（API の利用料金がかかります）。"
                } else {
                    "台本モード：日本語のお題を英語で言うと、相手が台本どおりに返事をします。AI を使わないので無料で、オフラインでも使えます。" +
                        if (apiKey.isBlank()) "\nAI と自由に会話するには、下で Claude の API キーを設定してください。" else ""
                },
            )
        }

        SettingsCard("Claude API キー") {
            Text(
                if (apiKey.isBlank()) "未設定（台本モードで動作中）" else "設定済み：${maskKey(apiKey)}",
                fontWeight = FontWeight.SemiBold,
                color = if (apiKey.isBlank()) LocalGrades.current.good else LocalGrades.current.great,
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it.trim() },
                label = { Text(if (apiKey.isBlank()) "API キー (sk-ant-…)" else "新しい API キー") },
                singleLine = true,
                visualTransformation = if (keyInput.isEmpty()) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(enabled = keyInput.isNotBlank(), onClick = {
                    app.store.setApiKey(keyInput)
                    keyInput = ""
                    testResult = null
                    context.toast("API キーを保存しました")
                }) { Text("保存") }
                OutlinedButton(enabled = apiKey.isNotBlank() && !testing, onClick = {
                    testing = true
                    testResult = "接続を確認しています…"
                    scope.launch {
                        testResult = try {
                            val tr = (app.claudeEngine() ?: error("API キーがありません")).translate("Nice to meet you!")
                            "✅ つながりました（Nice to meet you! → ${tr.ja}）"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            "❌ ${errorText(e)}"
                        }
                        testing = false
                    }
                }) { Text("接続テスト") }
                if (apiKey.isNotBlank()) {
                    TextButton(onClick = {
                        app.store.setApiKey("")
                        testResult = null
                    }) { Text("削除") }
                }
            }
            testResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
            Hint(
                "API キーは Claude Console で発行できます。キーはこの端末の中に暗号化して保存され、" +
                    "Claude API との通信にだけ使われます。会話 1 往復ごとに料金がかかります。",
            )
            TextButton(onClick = { openUrl(context, "https://platform.claude.com/settings/keys") }) { Text("Claude Console を開く") }
        }

        SettingsCard("AI モデル") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MODELS.forEachIndexed { i, (id, label) ->
                    SegmentedButton(
                        selected = settings.model == id,
                        onClick = { update { it.copy(model = id) } },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = MODELS.size),
                    ) { Text(label) }
                }
            }
            Hint("「高品質」は Claude Opus 5.5、「速い・安い」は Claude Sonnet 5.5 を使います。返事の速さと料金を優先するなら「速い・安い」がおすすめです。")
        }

        SettingsCard("英語レベル（CEFR）") {
            LevelSelector(settings.level, { level -> update { it.copy(level = level) } })
            Hint(
                "Epop などで調べた自分のレベルを選んでください。AI 会話では、相手の単語・文法・文の長さがこのレベルに合います。" +
                    "台本モードでは、このレベルに近いロールプレイがおすすめに出て、フリートークの回答例もこのレベルに近いものから表示されます。",
            )
        }

        SettingsCard("音声") {
            val (voiceText, voiceOk) = when (voice) {
                VoiceStatus.READY -> "英語の読み上げ: 使えます" to true
                VoiceStatus.INITIALIZING -> "英語の読み上げ: 準備中…" to true
                VoiceStatus.NO_ENGLISH -> "英語の読み上げ音声が入っていません" to false
                VoiceStatus.UNAVAILABLE -> "読み上げエンジンが使えません" to false
            }
            Text(voiceText, color = if (voiceOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
            if (!voiceOk) {
                Row {
                    TextButton(onClick = { openTtsSettings(context) }) { Text("読み上げの設定を開く") }
                    TextButton(onClick = { installVoiceData(context) }) { Text("音声データを入れる") }
                }
            }
            Text(
                "読み上げの速さ: ${String.format(Locale.US, "%.2f", settings.speechRate)}x",
                modifier = Modifier.padding(top = 8.dp),
            )
            Slider(
                value = settings.speechRate,
                onValueChange = { v -> update { it.copy(speechRate = (v * 20).toInt() / 20f) } },
                valueRange = 0.6f..1.3f,
            )
            OutlinedButton(onClick = {
                app.speaker.stop()
                app.speaker.say("Hi! Nice to meet you. Let's practice English together.")
            }) { Text("🔊 試しに聞く") }
            ToggleRow("AI のセリフを自動で読み上げる", null, settings.autoSpeak) { v -> update { it.copy(autoSpeak = v) } }
        }

        SettingsCard("会話モード") {
            if (!app.speechInput.available) {
                Text(
                    "この端末では音声認識が使えません。Google アプリをインストール・更新するか、キーボード入力をお使いください。",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ToggleRow(
                "マイクをもう一度押すまで聞き続ける",
                "オンにすると、途中で黙っても回答が確定しません。言い終わったらマイクをもう一度押してください。1 分間なにも話さないと自動で止まります。",
                settings.tapToFinish,
            ) { v -> update { it.copy(tapToFinish = v) } }
            ToggleRow(
                "ハンズフリー会話",
                "相手が話し終わると自動でマイクが ON になり、電話のように会話が続きます（話し終わりは無音で自動判定します）。",
                settings.handsFree,
            ) { v -> update { it.copy(handsFree = v) } }
            ToggleRow("話し終わったら自動で送信", "オフにすると、認識した文を確認・修正してから送信できます。", settings.autoSend) { v ->
                update { it.copy(autoSend = v) }
            }
            ToggleRow("AI のセリフを文字で表示", "オフにすると聞き取りの練習になります（タップで表示）。", settings.showText) { v ->
                update { it.copy(showText = v) }
            }
            ToggleRow("日本語訳を自動で表示", "AI のセリフに毎回日本語訳を付けます。", settings.autoTranslate) { v ->
                update { it.copy(autoTranslate = v) }
            }
            Text("話し終わりの待ち時間", modifier = Modifier.padding(top = 8.dp))
            Hint("自動で話し終わりを判定するとき（ハンズフリー会話、または上の「聞き続ける」がオフのとき）に使います。考えながら話すなら長めがおすすめです（端末の音声認識によっては効かないことがあります）。")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                SILENCE_CHOICES.forEachIndexed { i, (ms, label) ->
                    SegmentedButton(
                        selected = settings.silenceMs == ms,
                        onClick = { update { it.copy(silenceMs = ms) } },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = SILENCE_CHOICES.size),
                    ) { Text(label) }
                }
            }
        }

        SettingsCard("データ") {
            Hint("会話の記録とフレーズ帳は、この端末の中にだけ保存されます。")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.padding(top = 6.dp)) {
                Text("会話の記録・フレーズ帳・設定を削除", color = MaterialTheme.colorScheme.error)
            }
        }

        Text(
            "Hanaso v0.3.3・${if (app.modeFor(apiKey, settings) == EngineMode.AI) "AI: ${settings.model}" else "台本モード"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("すべて削除しますか？") },
            text = { Text("会話の記録・フレーズ帳・設定を削除します（API キーは残ります）。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    app.store.resetAll()
                    confirmReset = false
                    context.toast("削除しました")
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ToggleRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun maskKey(key: String): String = if (key.length <= 12) "••••" else "${key.take(10)}…${key.takeLast(4)}"

private fun openUrl(context: Context, url: String) = startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

private fun openTtsSettings(context: Context) = startSafely(context, Intent("com.android.settings.TTS_SETTINGS"))

private fun installVoiceData(context: Context) = startSafely(context, Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        context.toast("この端末では開けませんでした")
    }
}
