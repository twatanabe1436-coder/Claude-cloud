package io.github.twatanabe1436.sodateru.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.sodateru.core.BakersMath
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.data.PhotoStore
import io.github.twatanabe1436.sodateru.ui.components.Fmt
import io.github.twatanabe1436.sodateru.ui.components.IconLabel
import io.github.twatanabe1436.sodateru.ui.components.InfoPill
import io.github.twatanabe1436.sodateru.ui.components.PhotoStrip
import io.github.twatanabe1436.sodateru.ui.components.PillRow
import io.github.twatanabe1436.sodateru.ui.components.SmallStars

/** 焼成ログの主な条件を短いラベルにする。 */
fun bakePills(log: CookLog, recipe: Recipe?): List<String> {
    val bake = log.bake ?: return emptyList()
    val ingredients = bake.ingredients.ifEmpty { recipe?.version(log.versionNumber)?.ingredients.orEmpty() }
    val hydration = BakersMath.hydration(ingredients)
    return buildList {
        bake.roomTemp?.let { add("室温 ${Fmt.temp(it)}") }
        bake.humidity?.let { add("湿度 ${Fmt.num(it)}%") }
        hydration?.let { add("加水 ${Fmt.percent(it)}") }
        bake.waterTemp?.let { add("水温 ${Fmt.temp(it)}") }
        bake.doughTemp?.let { add("こね上げ ${Fmt.temp(it)}") }
        bake.firstProofMinutes?.let { add("一次 ${it}分") }
        bake.secondProofMinutes?.let { add("二次 ${it}分") }
        bake.bakeTemp?.let { t -> add("焼成 ${t}℃" + (bake.bakeMinutes?.let { "・${it}分" } ?: "")) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCard(
    log: CookLog,
    recipe: Recipe?,
    photos: PhotoStore,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRecipeTitle: Boolean = false,
    onApply: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (log.bake != null) "🍞" else "🍳")
                Spacer(Modifier.width(6.dp))
                Text(Fmt.date(log.date), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                SmallStars(log.rating)
                Spacer(Modifier.weight(1f))
                Text("v${log.versionNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showRecipeTitle && recipe != null) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (log.arrangement.isNotBlank()) {
                Text(
                    "アレンジ: ${log.arrangement}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (log.notes.isNotBlank()) {
                Text(
                    log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PillRow(bakePills(log, recipe))
            PhotoStrip(log.photos, photos, size = 64.dp)
            val applied = log.appliedVersion
            when {
                applied != null -> InfoPill("✓ v${applied}に反映済み", color = MaterialTheme.colorScheme.primaryContainer)
                onApply != null && log.arrangement.isNotBlank() -> FilledTonalButton(onClick = onApply) {
                    IconLabel(Icons.Filled.AutoAwesome, "このアレンジをレシピに反映")
                }
            }
        }
    }
}
