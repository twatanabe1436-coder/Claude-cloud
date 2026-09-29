package io.github.twatanabe1436.biyotimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.biyotimer.runtime.VoiceStatus
import io.github.twatanabe1436.biyotimer.timer.Phrases
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: TimerSettings,
    voiceStatus: VoiceStatus,
    onChange: ((TimerSettings) -> TimerSettings) -> Unit,
    onPreview: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onBack: () -> Unit,
) {
    var showAddAnnouncement by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    // 日本語入力の変換中に表示がずれないよう、入力欄は画面内の状態を正とする
    var startPhrase by rememberSaveable { mutableStateOf(settings.startPhrase) }
    var endPhrase by rememberSaveable { mutableStateOf(settings.endPhrase) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("読み上げるタイミング", "残り時間がこの値になったときに「残り◯分」と読み上げます。作業時間より長いものは読み上げません。") {
                val options = (TimerSettings.ANNOUNCEMENT_CHOICES + settings.announceAtSec).distinct().sortedDescending()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { sec ->
                        val selected = sec in settings.announceAtSec
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onChange { s ->
                                    s.copy(announceAtSec = if (selected) s.announceAtSec - sec else s.announceAtSec + sec)
                                }
                            },
                            label = { Text("残り${Phrases.duration(sec)}") },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else {
                                null
                            },
                        )
                    }
                    AssistChip(
                        onClick = { showAddAnnouncement = true },
                        label = { Text("追加") },
                        leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }

            Section("開始・終了の合図", "オフにしたときや言葉が空欄のときはビープ音で合図します。") {
                SwitchRow("開始時に読み上げる", settings.startPhraseEnabled) { on ->
                    onChange { it.copy(startPhraseEnabled = on) }
                }
                OutlinedTextField(
                    value = startPhrase,
                    onValueChange = { text ->
                        startPhrase = text.take(TimerSettings.MAX_PHRASE_LENGTH)
                        onChange { it.copy(startPhrase = startPhrase) }
                    },
                    label = { Text("開始の言葉") },
                    enabled = settings.startPhraseEnabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SwitchRow("終了時に読み上げる", settings.endPhraseEnabled) { on ->
                    onChange { it.copy(endPhraseEnabled = on) }
                }
                OutlinedTextField(
                    value = endPhrase,
                    onValueChange = { text ->
                        endPhrase = text.take(TimerSettings.MAX_PHRASE_LENGTH)
                        onChange { it.copy(endPhrase = endPhrase) }
                    },
                    label = { Text("終了の言葉") },
                    enabled = settings.endPhraseEnabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("開始前のカウントダウン", "スタートを押してから作業開始までの猶予です。") {
                val choices = TimerSettings.COUNTDOWN_CHOICES
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    choices.forEachIndexed { index, sec ->
                        SegmentedButton(
                            selected = settings.countdownSec == sec,
                            onClick = { onChange { it.copy(countdownSec = sec) } },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                        ) {
                            Text(if (sec == 0) "なし" else "${sec}秒")
                        }
                    }
                }
                SwitchRow(
                    label = "カウントダウン中にビープ音を鳴らす",
                    checked = settings.countdownBeep,
                    enabled = settings.countdownSec > 0,
                ) { on -> onChange { it.copy(countdownBeep = on) } }
            }

            Section("音声") {
                VoiceStatusRow(voiceStatus, onOpenVoiceSettings, onInstallVoiceData)
                Text(
                    text = "読み上げの速さ　${"%.1f".format(Locale.ROOT, settings.speechRate)}倍",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = { v -> onChange { it.copy(speechRate = (v * 10).roundToInt() / 10f) } },
                    valueRange = TimerSettings.MIN_SPEECH_RATE..TimerSettings.MAX_SPEECH_RATE,
                    steps = 9,
                )
                OutlinedButton(onClick = onPreview) {
                    Icon(AppIcons.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("テスト再生")
                }
            }

            Section("その他") {
                SwitchRow("終了時にバイブレーション", settings.vibrateOnFinish) { on ->
                    onChange { it.copy(vibrateOnFinish = on) }
                }
                SwitchRow("タイマー作動中は画面を消さない", settings.keepScreenOn) { on ->
                    onChange { it.copy(keepScreenOn = on) }
                }
            }

            TextButton(onClick = { showResetConfirm = true }) { Text("設定を初期状態に戻す") }
            Text(
                text = "プリセットの時間は一般的な実技試験の目安です。受験する年度の実施要項もあわせて確認してください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAddAnnouncement) {
        DurationDialog(
            title = "読み上げるタイミングを追加",
            initialSec = 7 * 60,
            onDismiss = { showAddAnnouncement = false },
            onConfirm = { sec ->
                onChange { it.copy(announceAtSec = it.announceAtSec + sec) }
                showAddAnnouncement = false
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("設定を初期状態に戻しますか？") },
            text = { Text("作業時間以外の設定が初期状態に戻ります。") },
            confirmButton = {
                TextButton(onClick = {
                    val defaults = TimerSettings()
                    startPhrase = defaults.startPhrase
                    endPhrase = defaults.endPhrase
                    onChange { TimerSettings(durationSec = it.durationSec, presetName = it.presetName) }
                    showResetConfirm = false
                }) { Text("戻す") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun Section(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun VoiceStatusRow(
    status: VoiceStatus,
    onOpenVoiceSettings: () -> Unit,
    onInstallVoiceData: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        VoiceStatus.READY -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text("日本語の音声で読み上げます", style = MaterialTheme.typography.bodyLarge)
        }
        VoiceStatus.INITIALIZING -> Text("読み上げエンジンを準備しています…", style = MaterialTheme.typography.bodyLarge)
        VoiceStatus.NO_JAPANESE, VoiceStatus.UNAVAILABLE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (status == VoiceStatus.NO_JAPANESE) {
                        "日本語の読み上げ音声が入っていません。いまはビープ音でお知らせします。"
                    } else {
                        "読み上げエンジンが使えません。いまはビープ音でお知らせします。"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = "「Google 音声サービス」(Speech Services by Google) を入れて、優先するエンジンに選び、日本語の音声データをダウンロードすると読み上げられるようになります。",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onInstallVoiceData) { Text("音声データを入手") }
                OutlinedButton(onClick = onOpenVoiceSettings) { Text("読み上げの設定") }
            }
        }
    }
}
