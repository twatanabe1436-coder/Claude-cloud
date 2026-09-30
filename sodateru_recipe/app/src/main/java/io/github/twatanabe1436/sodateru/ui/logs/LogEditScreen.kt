@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.logs

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.sodateru.AppContainer
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.DoughTemperature
import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.BakeScores
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.data.OcrEngine
import io.github.twatanabe1436.sodateru.data.Repository
import io.github.twatanabe1436.sodateru.ui.Navigator
import io.github.twatanabe1436.sodateru.ui.Route
import io.github.twatanabe1436.sodateru.ui.components.BackButton
import io.github.twatanabe1436.sodateru.ui.components.BakersSummaryLine
import io.github.twatanabe1436.sodateru.ui.components.ChoiceChips
import io.github.twatanabe1436.sodateru.ui.components.ConfirmDialog
import io.github.twatanabe1436.sodateru.ui.components.DateDialog
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.IconLabel
import io.github.twatanabe1436.sodateru.ui.components.ImageSourceDialog
import io.github.twatanabe1436.sodateru.ui.components.IntField
import io.github.twatanabe1436.sodateru.ui.components.NumberField
import io.github.twatanabe1436.sodateru.ui.components.PhotoEditorRow
import io.github.twatanabe1436.sodateru.ui.components.SectionCard
import io.github.twatanabe1436.sodateru.ui.components.StarRating
import io.github.twatanabe1436.sodateru.ui.components.ingredientEditorItems
import io.github.twatanabe1436.sodateru.ui.components.rememberImagePicker
import kotlinx.coroutines.launch

private val WEATHERS = listOf("晴れ", "くもり", "雨", "雪")

@Composable
fun LogEditScreen(container: AppContainer, navigator: Navigator, route: Route.EditLog) {
    val repo = container.repository
    val data by repo.data.collectAsStateWithLifecycle()
    val recipe = data.recipes.firstOrNull { it.id == route.recipeId }
    val existing = remember { route.logId?.let { repo.log(it) } }
    if (recipe == null || (route.logId != null && existing == null)) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }
    val bread = recipe.category == Category.BREAD
    val settings = container.settings.current
    val initial = remember { existing ?: newLog(recipe, route, settings.targetDoughTemp, settings.mixingMethod) }
    var log by remember { mutableStateOf(initial) }
    var customFormula by remember { mutableStateOf(initial.bake?.ingredients?.isNotEmpty() == true) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var askApply by remember { mutableStateOf<CookLog?>(null) }
    val scope = rememberCoroutineScope()
    val dirty = log != initial

    BackHandler(enabled = dirty) { confirmDiscard = true }

    val version = recipe.version(log.versionNumber) ?: recipe.current
    val allRecords = remember(data.logs) { data.logs.mapNotNull { it.bake } }

    fun updateBake(transform: (BakeRecord) -> BakeRecord) {
        log = log.copy(bake = transform(log.bake ?: BakeRecord()))
    }

    fun save() {
        scope.launch {
            val now = System.currentTimeMillis()
            val bake = log.bake?.let { if (customFormula) it else it.copy(ingredients = emptyList()) }
            val toSave = log.copy(
                arrangement = log.arrangement.trim(),
                notes = log.notes.trim(),
                bake = bake,
                updatedAt = now,
                createdAt = if (existing == null) now else log.createdAt,
            )
            repo.saveLog(toSave)
            bake?.let { b ->
                container.settings.update { it.copy(lastRoomTemp = b.roomTemp ?: it.lastRoomTemp, lastHumidity = b.humidity ?: it.lastHumidity) }
            }
            if (existing == null && toSave.arrangement.isNotBlank()) {
                askApply = toSave
            } else {
                navigator.pop()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "記録する" else "記録を編集") },
                navigationIcon = { BackButton { if (dirty) confirmDiscard = true else navigator.pop() } },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "記録を削除") }
                    }
                    TextButton(onClick = { save() }) { Text("保存", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "head") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recipe.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { pickDate = true }) {
                            IconLabel(Icons.Filled.CalendarMonth, "${Fmt.date(log.date)}")
                        }
                        Text("作った版", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
                        recipe.versions.takeLast(4).forEach { v ->
                            TextButton(onClick = { log = log.copy(versionNumber = v.number) }) {
                                Text(
                                    "v${v.number}",
                                    fontWeight = if (v.number == log.versionNumber) FontWeight.Bold else FontWeight.Normal,
                                    color = if (v.number == log.versionNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    StarRating(log.rating, { log = log.copy(rating = it) }, label = "総合評価")
                }
            }
            item(key = "text") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = log.arrangement,
                        onValueChange = { log = log.copy(arrangement = it) },
                        label = { Text("アレンジしたこと") },
                        placeholder = { Text("例：砂糖を大さじ2→1.5に。しょうがを倍に") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = log.notes,
                        onValueChange = { log = log.copy(notes = it) },
                        label = { Text("感想・次回へのメモ") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NoteScanButton(container) { text, target ->
                        log = when (target) {
                            NoteTarget.ARRANGEMENT -> log.copy(arrangement = join(log.arrangement, text))
                            NoteTarget.NOTES -> log.copy(notes = join(log.notes, text))
                        }
                    }
                    PhotoEditorRow(log.photos, container.photos, onChange = { log = log.copy(photos = it) })
                }
            }
            if (bread) {
                val bake = log.bake ?: BakeRecord()
                item(key = "env") { EnvironmentSection(bake, allRecords, ::updateBake) }
                item(key = "formula") {
                    SectionCard(title = "配合", modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("レシピ（v${version.number}）から配合を変えた", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "水の量を調整したときなどはオンにして、実際の量を入れてください",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = customFormula, onCheckedChange = { on ->
                                customFormula = on
                                if (on && bake.ingredients.isEmpty()) updateBake { it.copy(ingredients = version.ingredients) }
                            })
                        }
                        if (!customFormula) BakersSummaryLine(BakersMath.summarize(version.ingredients))
                    }
                }
                if (customFormula) {
                    ingredientEditorItems(
                        items = bake.ingredients,
                        onChange = { list -> updateBake { it.copy(ingredients = list) } },
                        bread = true,
                        keyPrefix = "bake-ing",
                    )
                }
                item(key = "process") { ProcessSection(bake, ::updateBake) }
                item(key = "scores") {
                    SectionCard(title = "焼き上がり", modifier = Modifier.padding(horizontal = 16.dp)) {
                        val s = bake.scores
                        StarRating(s.rise, { v -> updateBake { it.copy(scores = s.copy(rise = v)) } }, size = 28.dp, label = "ふくらみ")
                        StarRating(s.crumb, { v -> updateBake { it.copy(scores = s.copy(crumb = v)) } }, size = 28.dp, label = "内相")
                        StarRating(s.crust, { v -> updateBake { it.copy(scores = s.copy(crust = v)) } }, size = 28.dp, label = "皮")
                        StarRating(s.taste, { v -> updateBake { it.copy(scores = s.copy(taste = v)) } }, size = 28.dp, label = "味")
                    }
                }
            }
        }
    }

    if (pickDate) DateDialog(log.date, onPick = { log = log.copy(date = it) }, onDismiss = { pickDate = false })
    if (confirmDiscard) {
        ConfirmDialog(
            title = "記録をやめますか？",
            text = "入力した内容は保存されません。",
            confirmLabel = "やめる",
            dismissLabel = "入力を続ける",
            destructive = true,
            onConfirm = { navigator.pop() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "この記録を削除しますか？",
            text = "${Fmt.date(existing.date)} の記録を削除します。元に戻せません。",
            confirmLabel = "削除する",
            destructive = true,
            onConfirm = { scope.launch { repo.deleteLog(existing.id); navigator.pop() } },
            onDismiss = { confirmDelete = false },
        )
    }
    askApply?.let { saved ->
        AlertDialog(
            onDismissRequest = { askApply = null; navigator.pop() },
            title = { Text("レシピに反映しますか？") },
            text = { Text("今回のアレンジ「${saved.arrangement}」をレシピの新しい版（v${recipe.current.number + 1}）として残せます。") },
            confirmButton = {
                TextButton(onClick = {
                    askApply = null
                    navigator.replace(Route.EditRecipe(recipeId = recipe.id, fromLogId = saved.id))
                }) { Text("反映する") }
            },
            dismissButton = { TextButton(onClick = { askApply = null; navigator.pop() }) { Text("あとで") } },
        )
    }
}

private fun join(current: String, add: String): String = if (current.isBlank()) add.trim() else current.trimEnd() + "\n" + add.trim()

private fun newLog(
    recipe: Recipe,
    route: Route.EditLog,
    defaultTarget: Double,
    defaultMethod: io.github.twatanabe1436.sodateru.core.model.MixingMethod,
): CookLog {
    val now = System.currentTimeMillis()
    val version = recipe.current
    val bake = if (recipe.category == Category.BREAD) {
        val p = route.prefill
        val process = version.process
        BakeRecord(
            roomTemp = p?.roomTemp,
            humidity = p?.humidity,
            flourTemp = p?.flourTemp,
            waterTemp = p?.waterTemp,
            targetDoughTemp = p?.targetDoughTemp ?: process?.targetDoughTemp ?: defaultTarget,
            mixingMethod = p?.mixingMethod ?: process?.mixingMethod ?: defaultMethod,
            ingredients = p?.ingredients.orEmpty(),
            firstProofMinutes = p?.firstProofMinutes ?: process?.firstProofMinutes,
            firstProofTemp = p?.firstProofTemp ?: process?.firstProofTemp,
            benchMinutes = process?.benchMinutes,
            secondProofMinutes = p?.secondProofMinutes ?: process?.secondProofMinutes,
            secondProofTemp = p?.secondProofTemp ?: process?.secondProofTemp,
            bakeTemp = process?.bakeTemp,
            bakeMinutes = process?.bakeMinutes,
            scores = BakeScores(),
        )
    } else {
        null
    }
    return CookLog(
        id = Repository.newId(),
        recipeId = recipe.id,
        versionNumber = version.number,
        date = now,
        bake = bake,
        createdAt = now,
        updatedAt = now,
    )
}

@Composable
private fun EnvironmentSection(bake: BakeRecord, allRecords: List<BakeRecord>, update: ((BakeRecord) -> BakeRecord) -> Unit) {
    SectionCard(title = "環境と仕込み", icon = Icons.Filled.Thermostat, modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(bake.roomTemp, { v -> update { it.copy(roomTemp = v) } }, "室温", Modifier.weight(1f), "℃")
            NumberField(bake.humidity, { v -> update { it.copy(humidity = v) } }, "湿度", Modifier.weight(1f), "%")
            NumberField(bake.flourTemp, { v -> update { it.copy(flourTemp = v) } }, "粉温", Modifier.weight(1f), "℃")
        }
        ChoiceChips(WEATHERS, bake.weather.ifEmpty { null }, { it }, { w -> update { it.copy(weather = w.orEmpty()) } }, allowDeselect = true)
        Text("こね方", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            io.github.twatanabe1436.sodateru.core.model.MixingMethod.entries,
            bake.mixingMethod,
            { it.label },
            { m -> update { it.copy(mixingMethod = m) } },
            allowDeselect = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(bake.targetDoughTemp, { v -> update { it.copy(targetDoughTemp = v) } }, "目標こね上げ", Modifier.weight(1f), "℃")
            NumberField(bake.waterTemp, { v -> update { it.copy(waterTemp = v) } }, "仕込み水温", Modifier.weight(1f), "℃")
        }
        val target = bake.targetDoughTemp
        val room = bake.roomTemp
        if (target != null && room != null) {
            val rise = DoughTemperature.learnedRise(allRecords, bake.mixingMethod)
            val w = DoughTemperature.waterTemp(target, room, bake.flourTemp ?: room, rise.value)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("計算上の仕込み水温 ${Fmt.temp(Math.round(w.waterTemp * 2) / 2.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "上昇温度 +${Fmt.num(rise.value)}℃" + if (rise.isDefault) "（目安）" else "（記録${rise.samples}回から）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    w.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                FilledTonalButton(onClick = { update { it.copy(waterTemp = Math.round(w.waterTemp * 2) / 2.0) } }) { Text("入れる") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(bake.mixingMinutes, { v -> update { it.copy(mixingMinutes = v) } }, "こね時間", Modifier.weight(1f), "分")
            NumberField(bake.doughTemp, { v -> update { it.copy(doughTemp = v) } }, "こね上げ温度", Modifier.weight(1f), "℃")
        }
        DoughTemperature.observedRise(bake)?.let {
            Text(
                "今回のこねによる上昇温度: +${Fmt.num(it)}℃（次回からの水温計算に使われます）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ProcessSection(bake: BakeRecord, update: ((BakeRecord) -> BakeRecord) -> Unit) {
    SectionCard(title = "発酵と焼成", modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(bake.firstProofMinutes, { v -> update { it.copy(firstProofMinutes = v) } }, "一次発酵", Modifier.weight(1f), "分")
            NumberField(bake.firstProofTemp, { v -> update { it.copy(firstProofTemp = v) } }, "発酵温度", Modifier.weight(1f), "℃", placeholder = "空=室温")
        }
        IntField(bake.benchMinutes, { v -> update { it.copy(benchMinutes = v) } }, "ベンチタイム", Modifier.fillMaxWidth(), "分")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(bake.secondProofMinutes, { v -> update { it.copy(secondProofMinutes = v) } }, "二次発酵", Modifier.weight(1f), "分")
            NumberField(bake.secondProofTemp, { v -> update { it.copy(secondProofTemp = v) } }, "発酵温度", Modifier.weight(1f), "℃", placeholder = "空=室温")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField(bake.bakeTemp, { v -> update { it.copy(bakeTemp = v) } }, "焼成温度", Modifier.weight(1f), "℃")
            IntField(bake.bakeMinutes, { v -> update { it.copy(bakeMinutes = v) } }, "焼成時間", Modifier.weight(1f), "分")
        }
    }
}

enum class NoteTarget { ARRANGEMENT, NOTES }

/** ノートの写真を文字に起こして、アレンジ欄か感想欄に追加する。 */
@Composable
fun NoteScanButton(container: AppContainer, onText: (String, NoteTarget) -> Unit) {
    val hasKey by container.secrets.hasApiKey.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var chooseSource by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var engine by remember {
        mutableStateOf(if (container.settings.current.ocrEngine == OcrEngine.CLAUDE && hasKey) OcrEngine.CLAUDE else OcrEngine.DEVICE)
    }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberImagePicker(container.photos, maxItems = 3) { picked = it }

    OutlinedButton(onClick = { chooseSource = true }) {
        IconLabel(Icons.Filled.DocumentScanner, "ノートの写真から文字起こし")
    }
    if (chooseSource) ImageSourceDialog(picker, onDismiss = { chooseSource = false }, title = "ノートの写真")

    if (picked.isNotEmpty() || result != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) { picked = emptyList(); result = null; error = null } },
            title = { Text(if (result == null) "文字起こし" else "読み取った文字") },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val r = result
                    if (r == null) {
                        Text("${picked.size}枚の写真を読み取ります", style = MaterialTheme.typography.bodyMedium)
                        OcrEngine.entries.forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = engine == e, onClick = { engine = e }, enabled = !busy && (e == OcrEngine.DEVICE || hasKey))
                                Text(e.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (busy) CircularProgressIndicator(Modifier.size(28.dp))
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    } else {
                        OutlinedTextField(value = r, onValueChange = { result = it }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                        Text("どこに追加しますか？", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                val r = result
                if (r == null) {
                    TextButton(enabled = !busy, onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                result = container.textReader.readNote(picked, engine)
                            } catch (e: Exception) {
                                error = e.message ?: "読み取りに失敗しました"
                            } finally {
                                busy = false
                            }
                        }
                    }) { Text("読み取る") }
                } else {
                    Row {
                        TextButton(onClick = { onText(r, NoteTarget.ARRANGEMENT); result = null; picked = emptyList() }) { Text("アレンジに") }
                        TextButton(onClick = { onText(r, NoteTarget.NOTES); result = null; picked = emptyList() }) { Text("感想に") }
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { picked = emptyList(); result = null; error = null }) { Text("やめる") }
            },
        )
    }
}
