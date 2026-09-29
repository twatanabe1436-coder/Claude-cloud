package io.github.twatanabe1436.biyotimer.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.twatanabe1436.biyotimer.runtime.VoiceStatus
import io.github.twatanabe1436.biyotimer.timer.Phase
import io.github.twatanabe1436.biyotimer.timer.Phrases
import io.github.twatanabe1436.biyotimer.timer.Preset
import io.github.twatanabe1436.biyotimer.timer.Presets
import io.github.twatanabe1436.biyotimer.timer.TimerSettings
import io.github.twatanabe1436.biyotimer.timer.TimerSnapshot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    snapshot: TimerSnapshot,
    settings: TimerSettings,
    voiceStatus: VoiceStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSelectPreset: (Preset) -> Unit,
    onSetDuration: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }
    val editable = snapshot.phase == Phase.IDLE || snapshot.phase == Phase.FINISHED
    val accent = accentColor(snapshot)

    val start: () -> Unit = {
        if (isMediaVolumeMuted(context)) {
            scope.launch { snackbar.showSnackbar("メディアの音量が 0 です。音量ボタンで上げてください") }
        }
        onStart()
    }
    val openDurationDialog = { showDurationDialog = true }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("美容師 実技タイマー") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Row/Column の中からは BoxWithConstraints のスコープを暗黙に参照できないので取り出しておく
            val width = maxWidth
            val height = maxHeight
            val landscape = width > height
            val info: @Composable () -> Unit = {
                VoiceWarning(voiceStatus, onClick = onOpenSettings)
                StatusRow(snapshot, settings, accent)
                AnnouncementLine(snapshot, settings)
                PresetChips(
                    settings = settings,
                    enabled = editable,
                    onSelect = onSelectPreset,
                    onCustom = openDurationDialog,
                )
            }
            val controls: @Composable (Modifier) -> Unit = { modifier ->
                ControlButtons(snapshot.phase, start, onPause, onResume, onReset, modifier)
            }

            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    TimerDial(
                        snapshot = snapshot,
                        accent = accent,
                        size = min(height - 16.dp, 360.dp),
                        onClick = if (editable) openDurationDialog else null,
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        ) { info() }
                        controls(Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        VoiceWarning(voiceStatus, onClick = onOpenSettings)
                        StatusRow(snapshot, settings, accent)
                        TimerDial(
                            snapshot = snapshot,
                            accent = accent,
                            size = min(min(width - 48.dp, height * 0.5f), 360.dp),
                            onClick = if (editable) openDurationDialog else null,
                        )
                        AnnouncementLine(snapshot, settings)
                        PresetChips(
                            settings = settings,
                            enabled = editable,
                            onSelect = onSelectPreset,
                            onCustom = openDurationDialog,
                        )
                    }
                    controls(Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showDurationDialog) {
        DurationDialog(
            title = "作業時間",
            initialSec = settings.durationSec,
            onDismiss = { showDurationDialog = false },
            onConfirm = {
                onSetDuration(it)
                showDurationDialog = false
            },
        )
    }
}

@Composable
private fun accentColor(snapshot: TimerSnapshot): Color {
    val colors = MaterialTheme.colorScheme
    return when (snapshot.phase) {
        Phase.FINISHED -> colors.error
        Phase.PAUSED -> colors.tertiary
        Phase.RUNNING -> if (snapshot.remainingMs <= LAST_MINUTE_MS) colors.error else colors.primary
        Phase.IDLE, Phase.COUNTDOWN -> colors.primary
    }
}

private fun phaseLabel(phase: Phase): String = when (phase) {
    Phase.IDLE -> "待機中"
    Phase.COUNTDOWN -> "まもなく開始"
    Phase.RUNNING -> "作業中"
    Phase.PAUSED -> "一時停止中"
    Phase.FINISHED -> "終了"
}

@Composable
private fun StatusRow(snapshot: TimerSnapshot, settings: TimerSettings, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = settings.activePreset?.name ?: "カスタム",
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = accent.copy(alpha = 0.14f),
            contentColor = accent,
        ) {
            Text(
                text = phaseLabel(snapshot.phase),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TimerDial(
    snapshot: TimerSnapshot,
    accent: Color,
    size: Dp,
    onClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainerHighest
    val fraction = when (snapshot.phase) {
        Phase.IDLE, Phase.COUNTDOWN -> 1f
        else -> snapshot.remainingFraction
    }
    val density = LocalDensity.current
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClickLabel = "時間を変更", role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.05f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (fraction > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 文字の大きさは円の大きさに合わせる (端末の文字サイズ設定ではみ出さないように dp 基準)
            if (snapshot.phase == Phase.COUNTDOWN) {
                Text(
                    text = "${snapshot.countdownSecondsLeft}",
                    fontSize = with(density) { (size * 0.40f).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    text = "作業時間 ${Phrases.clock(snapshot.remainingDisplaySec)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            } else {
                val urgent = snapshot.phase == Phase.FINISHED ||
                    (snapshot.phase == Phase.RUNNING && snapshot.remainingMs <= LAST_MINUTE_MS)
                Text(
                    text = Phrases.clock(snapshot.remainingDisplaySec),
                    fontSize = with(density) { (size * 0.24f).toSp() },
                    fontWeight = FontWeight.Medium,
                    color = if (urgent) accent else colors.onSurface,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                )
                Text(
                    text = "経過 ${Phrases.clock(snapshot.elapsedDisplaySec)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                if (onClick != null) {
                    Text(
                        text = "タップして時間を変更",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementLine(snapshot: TimerSnapshot, settings: TimerSettings) {
    val text = when (snapshot.phase) {
        Phase.IDLE -> {
            val times = settings.announceAtSec.filter { it < settings.durationSec }.sortedDescending()
            if (times.isEmpty()) {
                "読み上げ：開始と終了のみ"
            } else {
                "読み上げ：残り" + times.joinToString("・") { Phrases.duration(it) }
            }
        }
        Phase.FINISHED -> "お疲れさまでした"
        Phase.COUNTDOWN, Phase.RUNNING, Phase.PAUSED ->
            snapshot.nextAnnouncementSec?.let { "次の読み上げ：残り${Phrases.duration(it)}" } ?: "次の合図：終了"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PresetChips(
    settings: TimerSettings,
    enabled: Boolean,
    onSelect: (Preset) -> Unit,
    onCustom: () -> Unit,
) {
    val active = settings.activePreset
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Presets.all.forEach { preset ->
            FilterChip(
                selected = active == preset,
                onClick = { onSelect(preset) },
                enabled = enabled,
                label = { Text("${preset.name} ${Phrases.duration(preset.durationSec)}") },
            )
        }
        FilterChip(
            selected = active == null,
            onClick = onCustom,
            enabled = enabled,
            label = {
                Text(if (active == null) "カスタム ${Phrases.duration(settings.durationSec)}" else "時間を指定")
            },
            leadingIcon = {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceWarning(status: VoiceStatus, onClick: () -> Unit) {
    if (status != VoiceStatus.NO_JAPANESE && status != VoiceStatus.UNAVAILABLE) return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(
                text = "日本語の読み上げ音声が使えないため、ビープ音でお知らせします。タップして設定を確認",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ControlButtons(
    phase: Phase,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = Modifier.height(64.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (phase) {
            Phase.IDLE -> Button(onClick = onStart, modifier = buttonModifier.weight(1f)) {
                ButtonContent(Icons.Filled.PlayArrow, "スタート")
            }
            Phase.COUNTDOWN -> OutlinedButton(onClick = onReset, modifier = buttonModifier.weight(1f)) {
                ButtonContent(Icons.Filled.Close, "キャンセル")
            }
            Phase.RUNNING -> FilledTonalButton(onClick = onPause, modifier = buttonModifier.weight(1f)) {
                ButtonContent(AppIcons.Pause, "一時停止")
            }
            Phase.PAUSED -> {
                OutlinedButton(onClick = onReset, modifier = buttonModifier.weight(1f)) {
                    ButtonContent(Icons.Filled.Refresh, "リセット")
                }
                Button(onClick = onResume, modifier = buttonModifier.weight(1f)) {
                    ButtonContent(Icons.Filled.PlayArrow, "再開")
                }
            }
            Phase.FINISHED -> {
                OutlinedButton(onClick = onReset, modifier = buttonModifier.weight(1f)) {
                    ButtonContent(Icons.Filled.Refresh, "リセット")
                }
                Button(onClick = onStart, modifier = buttonModifier.weight(1f)) {
                    ButtonContent(Icons.Filled.PlayArrow, "もう一度")
                }
            }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(label, style = MaterialTheme.typography.titleMedium)
}

private fun isMediaVolumeMuted(context: Context): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
}

private const val LAST_MINUTE_MS = 60_000L
