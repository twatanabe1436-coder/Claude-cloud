package io.github.twatanabe1436.sodateru.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.BakersSummary
import io.github.twatanabe1436.sodateru.core.IngredientRoles
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole

private val UNIT_CHOICES = listOf("g", "ml", "大さじ", "小さじ", "カップ", "個", "本", "枚", "片", "かけ", "合", "cc", "kg")

/**
 * 材料の編集欄を LazyColumn に並べる。[bread] のときは役割 (粉・水分など) とベーカーズ%も出す。
 */
fun LazyListScope.ingredientEditorItems(
    items: List<Ingredient>,
    onChange: (List<Ingredient>) -> Unit,
    bread: Boolean,
    keyPrefix: String = "ing",
) {
    val summary = if (bread) BakersMath.summarize(items) else null
    itemsIndexed(items, key = { index, _ -> "$keyPrefix-$index" }) { index, ingredient ->
        IngredientRow(
            ingredient = ingredient,
            percent = summary?.lines?.getOrNull(index)?.percent,
            bread = bread,
            canMoveUp = index > 0,
            canMoveDown = index < items.lastIndex,
            onChange = { updated -> onChange(items.toMutableList().also { it[index] = updated }) },
            onMove = { delta ->
                val target = index + delta
                if (target in items.indices) {
                    onChange(items.toMutableList().also { list -> list.add(target, list.removeAt(index)) })
                }
            },
            onDelete = { onChange(items.toMutableList().also { it.removeAt(index) }) },
        )
    }
    item(key = "$keyPrefix-add") {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChange(items + Ingredient(name = "")) }) {
                IconLabel(Icons.Filled.Add, "材料を追加")
            }
            if (summary != null) BakersSummaryLine(summary)
        }
    }
}

@Composable
fun BakersSummaryLine(summary: BakersSummary, modifier: Modifier = Modifier) {
    if (!summary.hasFlour) {
        Text(
            "粉の材料 (役割が「粉」) をグラムで入れると、ベーカーズ%と加水率を計算します",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val parts = buildList {
        add("粉 ${Fmt.num(summary.flourGrams)}g")
        summary.hydration?.let { add("加水 ${Fmt.percent(it)}") }
        summary.salt?.let { add("塩 ${Fmt.percent(it)}") }
        summary.yeast?.let { add("酵母 ${Fmt.percent(it)}") }
        summary.sugar?.let { add("糖 ${Fmt.percent(it)}") }
        summary.fat?.let { add("油脂 ${Fmt.percent(it)}") }
        add("総量 ${Fmt.num(summary.totalGrams)}g")
    }
    PillRow(parts, modifier)
}

@Composable
private fun IngredientRow(
    ingredient: Ingredient,
    percent: Double?,
    bread: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (Ingredient) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { name ->
                    // 役割を手で変えていなければ、名前から推測し直す
                    val autoRole = ingredient.role == IngredientRoles.guess(ingredient.name)
                    onChange(ingredient.copy(name = name, role = if (autoRole) IngredientRoles.guess(name) else ingredient.role))
                },
                label = { Text("材料") },
                singleLine = true,
                modifier = Modifier.weight(1.5f),
            )
            OutlinedTextField(
                value = ingredient.amount,
                onValueChange = { onChange(ingredient.copy(amount = it)) },
                label = { Text("量") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (bread) KeyboardType.Decimal else KeyboardType.Text),
                modifier = Modifier.weight(0.8f),
            )
            UnitField(ingredient.unit, { onChange(ingredient.copy(unit = it)) }, Modifier.weight(0.9f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bread) {
                RoleButton(ingredient.role) { onChange(ingredient.copy(role = it)) }
                Text(
                    percent?.let { Fmt.percent(it) } ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(64.dp),
                )
            }
            OutlinedTextField(
                value = ingredient.note,
                onValueChange = { onChange(ingredient.copy(note = it)) },
                placeholder = { Text("メモ（切り方など）") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "上へ", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "下へ", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "材料を削除", modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun UnitField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("単位") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { open = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "単位を選ぶ")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            UNIT_CHOICES.forEach { unit ->
                DropdownMenuItem(text = { Text(unit) }, onClick = { onChange(unit); open = false })
            }
        }
    }
}

@Composable
private fun RoleButton(role: IngredientRole, onChange: (IngredientRole) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(role.label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "役割を選ぶ", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            IngredientRole.entries.forEach { r ->
                DropdownMenuItem(text = { Text(r.label) }, onClick = { onChange(r); open = false })
            }
        }
    }
    Spacer(Modifier.width(4.dp))
}

/** 材料の表示 (詳細画面)。[percentages] があればベーカーズ%も並べる。 */
@Composable
fun IngredientList(
    items: List<Ingredient>,
    modifier: Modifier = Modifier,
    percentages: List<Double?>? = null,
    highlightNames: Set<String> = emptySet(),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, ing ->
            val highlight = ing.name in highlightNames
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ing.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (ing.note.isNotBlank()) {
                        Text(ing.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(Amounts.display(ing), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val pct = percentages?.getOrNull(i)
                if (percentages != null) {
                    Text(
                        pct?.let { Fmt.percent(it) } ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(72.dp).padding(start = 12.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}
