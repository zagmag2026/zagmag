package com.nimsdeveloper.zhagmagdresses.admin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.R
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted

@Composable
fun LabeledSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val notchOffset = (9f * fontScale).dp
    Box(modifier = modifier.fillMaxWidth().padding(top = notchOffset)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = AppSpacing.md,
                    end = AppSpacing.md,
                    top = AppSpacing.md,
                    bottom = AppSpacing.md
                ),
                content = content
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = AppSpacing.md, y = -notchOffset),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RoundedItemImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    galleryUrls: List<String> = emptyList(),
    initialIndex: Int = 0
) {
    val modelUrl = (model as? String)?.trim().orEmpty()
    val resolvedUrls = remember(modelUrl, galleryUrls) {
        buildList {
            galleryUrls.map(String::trim).filter(String::isNotBlank).distinct().forEach(::add)
            if (modelUrl.isNotBlank() && modelUrl !in this) add(0, modelUrl)
        }
    }
    val resolvedInitial = remember(modelUrl, resolvedUrls, initialIndex) {
        val modelIndex = if (modelUrl.isBlank()) -1 else resolvedUrls.indexOf(modelUrl)
        when {
            initialIndex > 0 && initialIndex in resolvedUrls.indices -> initialIndex
            modelIndex >= 0 -> modelIndex
            initialIndex in resolvedUrls.indices -> initialIndex
            else -> 0
        }
    }
    var showGallery by remember { mutableStateOf(false) }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (resolvedUrls.isNotEmpty()) {
                    Modifier.clickable(
                        onClickLabel = "View item images",
                        onClick = { showGallery = true }
                    )
                } else Modifier
            ),
        contentScale = contentScale,
        placeholder = painterResource(R.drawable.ic_launcher),
        error = painterResource(R.drawable.ic_launcher),
        fallback = painterResource(R.drawable.ic_launcher)
    )

    if (showGallery && resolvedUrls.isNotEmpty()) {
        ItemImageGalleryDialog(
            itemName = contentDescription.orEmpty().ifBlank { "Item" },
            imageUrls = resolvedUrls,
            initialIndex = resolvedInitial,
            onDismiss = { showGallery = false }
        )
    }
}


/**
 * Canonical compact Item display used across current booking/item surfaces.
 * Line 3 is intentionally contextual and may be omitted when it has no useful operational meaning.
 */
@Composable
fun AppItemDisplayRow(
    imageUrl: Any?,
    itemName: String,
    itemCode: String,
    categoryName: String,
    quantity: Int,
    imageUrls: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    statusLine: String? = null,
    nested: Boolean = false,
    showImage: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val secondary = listOf(itemCode.trim(), categoryName.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (nested) AppSpacing.md else 0.dp,
                top = AppSpacing.xs,
                bottom = AppSpacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (showImage) {
            RoundedItemImage(
                model = imageUrl,
                contentDescription = itemName,
                modifier = Modifier.size(48.dp),
                galleryUrls = imageUrls
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                itemName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondary.isNotBlank()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted,
                    maxLines = 1
                )
            }
            if (!statusLine.isNullOrBlank()) {
                Text(
                    statusLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextMuted,
                    maxLines = 1
                )
            }
        }
        Text(
            "× $quantity",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        trailing?.invoke(this)
    }
}

@Composable
fun CompactExpandCollapseAction(
    hiddenCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hiddenCount <= 0) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(
                if (expanded) "Show less" else "+ $hiddenCount more",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CompactMetaBadge(
    icon: ImageVector,
    label: String,
    value: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = foreground)
            Text(
                "$label · $value",
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class SoftActionSpec(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val tone: ActionTone = ActionTone.INFO
)

@Composable
fun ResponsiveSoftActionGrid(
    actions: List<SoftActionSpec>,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 82.dp,
    maxColumns: Int = 4
) {
    if (actions.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacingWidth = AppSpacing.xs
        val availableColumns = ((maxWidth + spacingWidth).value / (minCellWidth + spacingWidth).value)
            .toInt()
            .coerceAtLeast(1)
        val columns = minOf(actions.size, maxColumns.coerceAtLeast(1), availableColumns)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    rowActions.forEach { action ->
                        SoftActionButton(
                            icon = action.icon,
                            label = action.label,
                            onClick = action.onClick,
                            modifier = Modifier.weight(1f),
                            enabled = action.enabled,
                            tone = action.tone
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResponsiveActionTriple(
    modifier: Modifier = Modifier,
    minSideBySideWidth: Dp = 300.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    third: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= minSideBySideWidth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
                third(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    first(Modifier.weight(1f))
                    second(Modifier.weight(1f))
                }
                third(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ResponsiveCompactPair(
    modifier: Modifier = Modifier,
    minSideBySideWidth: androidx.compose.ui.unit.Dp = 300.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= minSideBySideWidth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun AppItemListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = AppBorder)
}
