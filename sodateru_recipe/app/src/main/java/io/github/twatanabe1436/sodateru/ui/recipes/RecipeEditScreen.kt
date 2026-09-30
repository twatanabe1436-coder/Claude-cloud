@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.recipes

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.IngredientRoles
import io.github.twatanabe1436.sodateru.core.model.BreadProcess
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion
import io.github.twatanabe1436.sodateru.data.Repository
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.ChoiceChips
import io.github.twatanabe1436.sodateru.ui.components.ConfirmDialog
import io.github.twatanabe1436.sodateru.ui.components.IntField
import io.github.twatanabe1436.sodateru.ui.components.NumberField
import io.github.twatanabe1436.sodateru.ui.components.PhotoEditorRow
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.ingredientEditorItems
import kotlinx.coroutines.launch

/** 編集中の内容。 */
private data class RecipeForm(
    val title: String = "",
    val category: Category = Category.COOKING,
    val tags: String = "",
    val source: String = "",
    val servings: String = "",
    val ingredients: List<Ingredient> = listOf(Ingredient("")),
    val steps: List<String> = listOf(""),
    val memo: String = "",
    val photos: List<String> = emptyList(),
    val process: BreadProcess = BreadProcess(),
    val changeNote: String = "",
    /** 版を増やさずに今の版を直す (誤字の修正など)。 */
    val overwrite: Boolean = false,
)

private fun RecipeForm.cleanIngredients() = ingredients.filter { it.name.isNotBlank() }.map {
    it.copy(name = it.name.trim(), amount = it.amount.trim(), unit = it.unit.trim(), note = it.note.trim())
}

private fun RecipeForm.cleanSteps() = steps.map { it.trim() }.filter { it.isNotEmpty() }

private fun RecipeForm.processOrNull(): BreadProcess? =
    if (category == Category.BREAD && process != BreadProcess()) process else null

@Composable
fun RecipeEditScreen(container: AppContainer, navigator: Navigator, route: Route.EditRecipe) {
    val repo = container.repository
    val existing = route.recipeId?.let { repo.recipe(it) }
    val fromLog = route.fromLogId?.let { repo.log(it) }
    val pending = remember { if (route.useDraft) container.drafts.pending.also { container.drafts.pending = null } else null }
    val settings = container.settings.current

    val initial = remember {
        when {
            existing != null -> {
                val v = existing.current
                val logIngredients = fromLog?.bake?.ingredients.orEmpty()
                RecipeForm(
                    title = existing.title,
                    category = existing.category,
                    tags = existing.tags.joinToString("、"),
                    source = existing.source,
                    servings = v.servings,
                    ingredients = logIngredients.ifEmpty { v.ingredients }.ifEmpty { listOf(Ingredient("")) },
                    steps = v.steps.ifEmpty { listOf("") },
                    memo = v.memo,
                    photos = v.photos,
                    process = v.process ?: BreadProcess(),
                    changeNote = fromLog?.arrangement.orEmpty(),
                )
            }
            pending != null -> {
                val d = pending.draft
                val category = route.category ?: d.category ?: Category.COOKING
                RecipeForm(
                    title = d.title,
                    category = category,
                    servings = d.servings,
                    ingredients = d.ingredients.ifEmpty { listOf(Ingredient("")) },
                    steps = d.steps.ifEmpty { listOf("") },
                    memo = d.memo,
                    photos = pending.photos,
                    process = if (category == Category.BREAD) {
                        BreadProcess(targetDoughTemp = settings.targetDoughTemp, mixingMethod = settings.mixingMethod)
                    } else {
                        BreadProcess()
                    },
                )
            }
            else -> {
                val category = route.category ?: Category.COOKING
                RecipeForm(
                    category = category,
                    process = if (category == Category.BREAD) {
                        BreadProcess(targetDoughTemp = settings.targetDoughTemp, mixingMethod = settings.mixingMethod)
                    } else {
                        BreadProcess()
                    },
                )
            }
        }
    }
    var form by remember { mutableStateOf(initial) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dirty = form != initial || pending != null

    if (route.recipeId != null && existing == null) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }

    BackHandler(enabled = dirty) { confirmDiscard = true }

    val nextNumber = (existing?.current?.number ?: 0) + 1
    val title = when {
        existing == null -> "レシピを追加"
        form.overwrite -> "v${existing.current.number} を修正"
        else -> "v$nextNumber に育てる"
    }

    fun save() {
        val ingredients = form.cleanIngredients()
        if (form.title.isBlank()) {
            error = "料理名を入れてください"
            return
        }
        saving = true
        scope.launch {
            val now = System.currentTimeMillis()
            val tags = form.tags.split('、', ',', '，', ' ', '　').map { it.trim() }.filter { it.isNotEmpty() }
            if (existing == null) {
                val id = Repository.newId()
                val recipe = Recipe(
                    id = id,
                    title = form.title.trim(),
                    category = form.category,
                    tags = tags,
                    source = form.source.trim(),
                    createdAt = now,
                    updatedAt = now,
                    versions = listOf(
                        RecipeVersion(
                            number = 1,
                            createdAt = now,
                            servings = form.servings.trim(),
                            ingredients = ingredients,
                            steps = form.cleanSteps(),
                            memo = form.memo.trim(),
                            photos = form.photos,
                            process = form.processOrNull(),
                        ),
                    ),
                )
                repo.saveRecipe(recipe)
                navigator.replace(Route.RecipeDetail(id))
            } else {
                val base = existing.current
                val edited = base.copy(
                    servings = form.servings.trim(),
                    ingredients = ingredients,
                    steps = form.cleanSteps(),
                    memo = form.memo.trim(),
                    photos = form.photos,
                    process = form.processOrNull(),
                )
                val versions = if (form.overwrite) {
                    existing.versions.dropLast(1) + edited.copy(changeNote = if (base.number == 1) "" else form.changeNote.trim())
                } else {
                    existing.versions + edited.copy(
                        number = nextNumber,
                        createdAt = now,
                        changeNote = form.changeNote.trim(),
                        fromLogId = fromLog?.id,
                    )
                }
                repo.saveRecipe(
                    existing.copy(
                        title = form.title.trim(),
                        category = form.category,
                        tags = tags,
                        source = form.source.trim(),
                        updatedAt = now,
                        versions = versions,
                    ),
                )
                if (fromLog != null && !form.overwrite) {
                    repo.saveLog(fromLog.copy(appliedVersion = nextNumber, updatedAt = now))
                }
                navigator.pop()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton { if (dirty) confirmDiscard = true else navigator.pop() } },
                actions = {
                    TextButton(onClick = { save() }, enabled = !saving) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pending != null) {
                item(key = "draft") {
                    SectionCard(
                        title = "写真から読み取りました",
                        icon = Icons.Filled.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text(
                            "${pending.engineLabel}で読み取った内容です。間違いがないか確認・修正してから保存してください。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        var showAll by remember { mutableStateOf(false) }
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "読み取った全文を閉じる" else "読み取った全文を見る")
                        }
                        if (showAll) Text(pending.draft.transcript, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (existing != null) {
                item(key = "change") {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!form.overwrite) {
                            OutlinedTextField(
                                value = form.changeNote,
                                onValueChange = { form = form.copy(changeNote = it) },
                                label = { Text("この版で変えたこと") },
                                placeholder = { Text("例：砂糖を減らして、しょうがを増やした") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (fromLog != null) {
                                Text(
                                    "記録のアレンジを反映しています。材料や手順を実際に作ったとおりに直してください。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = form.overwrite, onCheckedChange = { form = form.copy(overwrite = it) })
                            Text("版を増やさずに今の版を直す（誤字の修正など）", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item(key = "basic") {
                val err = error
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { form = form.copy(title = it); error = null },
                        label = { Text("料理名") },
                        singleLine = true,
                        isError = err != null,
                        supportingText = if (err != null) {
                            { Text(err) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChoiceChips(
                        options = Category.entries,
                        selected = form.category,
                        label = { it.label },
                        onSelect = { c ->
                            if (c != null) {
                                val process = if (c == Category.BREAD && form.process == BreadProcess()) {
                                    BreadProcess(targetDoughTemp = settings.targetDoughTemp, mixingMethod = settings.mixingMethod)
                                } else {
                                    form.process
                                }
                                val ingredients = if (c == Category.BREAD) {
                                    form.ingredients.map { it.copy(role = if (it.role == IngredientRole.OTHER) IngredientRoles.guess(it.name) else it.role) }
                                } else {
                                    form.ingredients
                                }
                                form = form.copy(category = c, process = process, ingredients = ingredients)
                            }
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.servings,
                            onValueChange = { form = form.copy(servings = it) },
                            label = { Text("分量") },
                            placeholder = { Text("2人分・1斤型") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.source,
                            onValueChange = { form = form.copy(source = it) },
                            label = { Text("出典") },
                            placeholder = { Text("本・サイト名など") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = form.tags,
                        onValueChange = { form = form.copy(tags = it) },
                        label = { Text("タグ（、で区切る）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PhotoEditorRow(form.photos, container.photos, onChange = { form = form.copy(photos = it) })
                }
            }
            item(key = "ing-title") {
                SectionTitle(if (form.category == Category.BREAD) "材料（グラムで入れるとベーカーズ%を計算）" else "材料")
            }
            ingredientEditorItems(
                items = form.ingredients,
                onChange = { form = form.copy(ingredients = it) },
                bread = form.category == Category.BREAD,
            )
            if (form.category == Category.BREAD) {
                item(key = "process") {
                    ProcessEditor(form.process) { form = form.copy(process = it) }
                }
            }
            item(key = "steps-title") { SectionTitle("作り方") }
            stepEditorItems(form.steps) { form = form.copy(steps = it) }
            item(key = "memo") {
                OutlinedTextField(
                    value = form.memo,
                    onValueChange = { form = form.copy(memo = it) },
                    label = { Text("メモ・コツ") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "編集をやめますか？",
            text = "入力した内容は保存されません。",
            confirmLabel = "やめる",
            dismissLabel = "編集を続ける",
            destructive = true,
            onConfirm = { navigator.pop() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

private fun LazyListScope.stepEditorItems(steps: List<String>, onChange: (List<String>) -> Unit) {
    itemsIndexed(steps, key = { i, _ -> "step-$i" }) { i, step ->
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = step,
                onValueChange = { t -> onChange(steps.toMutableList().also { it[i] = t }) },
                label = { Text("手順 ${i + 1}") },
                minLines = 2,
                modifier = Modifier.weight(1f),
            )
            Column {
                IconButton(onClick = { onChange(steps.toMutableList().also { it.add(i - 1, it.removeAt(i)) }) }, enabled = i > 0) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "上へ", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { onChange(steps.toMutableList().also { it.add(i + 1, it.removeAt(i)) }) },
                    enabled = i < steps.lastIndex,
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "下へ", modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { onChange(steps.toMutableList().also { it.removeAt(i) }) }) {
                Icon(Icons.Filled.Delete, contentDescription = "手順を削除", modifier = Modifier.size(20.dp))
            }
        }
    }
    item(key = "step-add") {
        OutlinedButton(onClick = { onChange(steps + "") }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" 手順を追加")
        }
    }
}

@Composable
fun ProcessEditor(process: BreadProcess, onChange: (BreadProcess) -> Unit) {
    SectionCard(title = "工程の目安（パン）", modifier = Modifier.padding(horizontal = 16.dp)) {
        NumberField(
            process.targetDoughTemp,
            { onChange(process.copy(targetDoughTemp = it)) },
            label = "目標こね上げ温度",
            suffix = "℃",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("こね方", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            options = MixingMethod.entries,
            selected = process.mixingMethod,
            label = { it.label },
            onSelect = { onChange(process.copy(mixingMethod = it)) },
            allowDeselect = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(process.firstProofMinutes, { onChange(process.copy(firstProofMinutes = it)) }, "一次発酵", Modifier.weight(1f), "分")
            NumberField(process.firstProofTemp, { onChange(process.copy(firstProofTemp = it)) }, "温度", Modifier.weight(1f), "℃")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(process.secondProofMinutes, { onChange(process.copy(secondProofMinutes = it)) }, "二次発酵", Modifier.weight(1f), "分")
            NumberField(process.secondProofTemp, { onChange(process.copy(secondProofTemp = it)) }, "温度", Modifier.weight(1f), "℃")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(process.benchMinutes, { onChange(process.copy(benchMinutes = it)) }, "ベンチタイム", Modifier.weight(1f), "分")
            IntField(process.bakeTemp, { onChange(process.copy(bakeTemp = it)) }, "焼成温度", Modifier.weight(1f), "℃")
            IntField(process.bakeMinutes, { onChange(process.copy(bakeMinutes = it)) }, "焼成時間", Modifier.weight(1f), "分")
        }
    }
}
