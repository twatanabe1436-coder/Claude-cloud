@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package io.github.twatanabe1436.sodateru.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.ui.theme.StarColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

/** 日付・数値の表示。 */
object Fmt {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun date(ms: Long): String {
        val d = localDate(ms)
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE)
        return "${d.year}/${d.monthValue}/${d.dayOfMonth}（$dow）"
    }

    fun shortDate(ms: Long): String {
        val d = localDate(ms)
        return "${d.monthValue}/${d.dayOfMonth}"
    }

    fun month(ms: Long): String {
        val d = localDate(ms)
        return "${d.year}年${d.monthValue}月"
    }

    fun num(value: Double?): String = value?.let { Amounts.formatNumber(it) } ?: "—"

    fun temp(value: Double?): String = value?.let { "${Amounts.formatNumber(it)}℃" } ?: "—"

    fun percent(value: Double?): String = value?.let { "${"%.1f".format(Locale.US, it).removeSuffix(".0")}%" } ?: "—"

    fun minutes(value: Double?): String = value?.let { "${Amounts.formatNumber(it)}分" } ?: "—"

    /** 「今日」「3日前」など。 */
    fun ago(ms: Long, now: Long = System.currentTimeMillis()): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(localDate(ms), localDate(now))
        return when {
            days <= 0 -> "今日"
            days == 1L -> "昨日"
            days < 31 -> "${days}日前"
            days < 365 -> "${days / 30}か月前"
            else -> "${days / 365}年前"
        }
    }
}

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    action?.invoke()
                }
            }
            content()
        }
    }
}

/** ★評価。[onChange] が null なら表示だけ。同じ★をもう一度押すと未評価に戻る。 */
@Composable
fun StarRating(
    value: Int,
    onChange: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    label: String? = null,
) {
    Row(
        modifier.semantics { contentDescription = "${label ?: "評価"} ★$value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(72.dp))
        }
        for (i in 1..5) {
            val filled = i <= value
            Icon(
                if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (filled) StarColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(size)
                    .then(if (onChange != null) Modifier.clickable { onChange(if (i == value) 0 else i) } else Modifier),
            )
        }
    }
}

@Composable
fun SmallStars(value: Int) {
    if (value <= 0) return
    Text("★".repeat(value), color = StarColor, style = MaterialTheme.typography.labelMedium)
}

/**
 * 数値の入力欄。入力途中の「12.」なども崩さないよう、文字列を自前で持つ。
 */
@Composable
fun NumberField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    placeholder: String? = null,
) {
    var text by remember { mutableStateOf(value?.let { Amounts.formatNumber(it) } ?: "") }
    LaunchedEffect(value) {
        if (Amounts.parse(text) != value) text = value?.let { Amounts.formatNumber(it) } ?: ""
    }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            val cleaned = Amounts.normalize(t).filter { it.isDigit() || it == '.' || it == '-' }
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull())
        },
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        suffix = if (suffix.isNotEmpty()) {
            { Text(suffix) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun IntField(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    NumberField(
        value = value?.toDouble(),
        onValueChange = { onValueChange(it?.toInt()) },
        label = label,
        modifier = modifier,
        suffix = suffix,
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(emoji, style = MaterialTheme.typography.displayMedium)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "キャンセル",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/** 選択肢をチップで並べる。[allowDeselect] なら選択中のものを押すと null に戻る。 */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allowDeselect: Boolean = false,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(if (option == selected && allowDeselect) null else option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/** 小さなラベル (「加水 68%」など)。 */
@Composable
fun InfoPill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = color, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun PillRow(texts: List<String>, modifier: Modifier = Modifier) {
    if (texts.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        texts.forEach { InfoPill(it) }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** 日付を選ぶダイアログ。時刻 (時・分) は元の値のまま日付だけ変える。 */
@Composable
fun DateDialog(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val initialDate = Fmt.localDate(initial)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { utcMillis ->
                    val picked = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val time = Instant.ofEpochMilli(initial).atZone(ZoneId.systemDefault()).toLocalTime()
                    onPick(picked.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    ) {
        DatePicker(state = state)
    }
}
