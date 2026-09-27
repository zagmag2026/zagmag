package com.nimsdeveloper.zhagmagdresses.admin.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGiven
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGivenSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItems
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItemsSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusErrorSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppPageHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
        }
        if (action != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { action() }
        }
    }
}

@Composable
fun AppScreenTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun AppBackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
        }
        AppScreenTitle(title = title, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun CompactNewActionButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.wrapContentWidth().defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        color = BrandSoft,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppFormSheetScaffold(
    title: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    saveLabel: String = "Save",
    expanded: Boolean = false,
    showCloseAction: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    val rootModifier = if (expanded) {
        modifier.fillMaxWidth().fillMaxHeight(0.92f)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = rootModifier
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showCloseAction) {
                IconButton(
                    onClick = {
                        dismissKeyboard()
                        onDismiss()
                    },
                    enabled = !busy
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                content = content
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                content = content
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = {
                    dismissKeyboard()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = !busy
            )
            PrimaryButton(
                text = saveLabel,
                onClick = {
                    dismissKeyboard()
                    onSave()
                },
                modifier = Modifier.weight(1f),
                enabled = saveEnabled && !busy,
                loading = busy
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
    }
}

@Composable
fun AppCompactFixedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeIndex = selectedIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
    TabRow(
        selectedTabIndex = safeIndex,
        modifier = modifier.fillMaxWidth(),
        divider = { HorizontalDivider(color = AppBorder) }
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == safeIndex
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.height(44.dp),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = AppTextMuted,
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}

@Composable
fun AppCompactScrollableTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minTabWidth: androidx.compose.ui.unit.Dp = 96.dp
) {
    val safeIndex = selectedIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
    ScrollableTabRow(
        selectedTabIndex = safeIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        divider = { HorizontalDivider(color = AppBorder) }
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == safeIndex
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.widthIn(min = minTabWidth).height(44.dp),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = AppTextMuted,
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}

data class AppFilterOption(
    val value: String,
    val label: String
)

@Composable
fun AppSelectField(
    label: String,
    selected: String,
    options: List<AppFilterOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    prompt: String = "Select Option",
    enabled: Boolean = true,
    required: Boolean = false,
    icon: ImageVector? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label.orEmpty()
    val invalidSelection = selected.isNotBlank() && selectedLabel.isBlank()
    val showError = invalidSelection || (required && selectedLabel.isBlank())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.xs))
                }
                Text(
                    selectedLabel.ifBlank { prompt },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedLabel.isBlank()) AppTextMuted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text(prompt, color = AppTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {},
                    enabled = false
                )
                HorizontalDivider(color = AppBorder)
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No options found.", color = AppTextMuted) },
                        onClick = {},
                        enabled = false
                    )
                } else {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (selected == option.value) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                expanded = false
                                onSelected(option.value)
                            }
                        )
                    }
                }
            }
        }
        if (showError) {
            Text(
                if (invalidSelection) "Select a valid option." else "Required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AppMultiSelectField(
    label: String,
    selected: Set<String>,
    options: List<AppFilterOption>,
    onSelected: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    prompt: String = "Select Options",
    enabled: Boolean = true,
    required: Boolean = false,
    icon: ImageVector? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val validValues = options.map { it.value }.toSet()
    val invalidSelection = selected.any { it !in validValues }
    val selectedLabels = options.filter { it.value in selected }.map { it.label }
    val showError = invalidSelection || (required && selectedLabels.isEmpty())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.xs))
                }
                Text(
                    if (selectedLabels.isEmpty()) prompt else selectedLabels.joinToString(", "),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedLabels.isEmpty()) AppTextMuted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text(prompt, color = AppTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {},
                    enabled = false
                )
                HorizontalDivider(color = AppBorder)
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No options found.", color = AppTextMuted) },
                        onClick = {},
                        enabled = false
                    )
                } else {
                    options.forEach { option ->
                        val checked = option.value in selected
                        DropdownMenuItem(
                            text = { Text(option.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (checked) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                onSelected(if (checked) selected - option.value else selected + option.value)
                            }
                        )
                    }
                }
            }
        }
        if (showError) {
            Text(
                if (invalidSelection) "Select valid options." else "Required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSingleSelectFilter(
    label: String,
    selected: String,
    options: List<AppFilterOption>,
    resetValue: String,
    onApply: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Filter",
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.FilterList,
    searchThreshold: Int = 8
) {
    var open by remember { mutableStateOf(false) }
    var pending by rememberSaveable(selected) { mutableStateOf(selected) }
    var query by rememberSaveable { mutableStateOf("") }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label ?: label
    val selectedIsReset = selected == resetValue
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    OutlinedButton(
        onClick = {
            pending = selected
            query = ""
            open = true
        },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            selectedLabel,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (!selectedIsReset) {
            Spacer(Modifier.width(AppSpacing.xs))
            Surface(
                color = StatusInfoSoft,
                contentColor = StatusInfo,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("1", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (open) {
        val filtered = if (query.isBlank()) options else options.filter { it.label.contains(query, ignoreCase = true) }
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (options.size > searchThreshold) {
                    CompactSearchField(
                        value = query,
                        onValueChange = { query = it.take(120) },
                        placeholder = "Search options",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                ) {
                    if (filtered.isEmpty()) {
                        item(key = "no-filter-options") { EmptyState("No options found.", variant = EmptyStateVariant.FILTERED_NO_RESULT) }
                    }
                    items(filtered, key = { it.value }) { option ->
                        Surface(
                            onClick = { pending = option.value },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (pending == option.value) StatusInfoSoft else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                RadioButton(selected = pending == option.value, onClick = null)
                                Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (pending == option.value) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = StatusInfo)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    TextButton(
                        onClick = {
                            pending = selected
                            open = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    SecondaryButton(
                        text = "Clear",
                        onClick = {
                            pending = resetValue
                            query = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Apply",
                        onClick = {
                            onApply(pending)
                            open = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(AppSpacing.xs))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMultiSelectFilter(
    label: String,
    selected: Set<String>,
    options: List<AppFilterOption>,
    onApply: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Filter",
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.FilterList,
    searchThreshold: Int = 8
) {
    var open by remember { mutableStateOf(false) }
    var pendingValues by rememberSaveable(selected) { mutableStateOf(selected.toList()) }
    val pending = pendingValues.toSet()
    var query by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedLabels = options.filter { it.value in selected }.map { it.label }

    OutlinedButton(
        onClick = {
            pendingValues = selected.toList()
            query = ""
            open = true
        },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            if (selectedLabels.isEmpty()) label else selectedLabels.take(2).joinToString(", "),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (selected.isNotEmpty()) {
            Spacer(Modifier.width(AppSpacing.xs))
            Surface(
                color = StatusInfoSoft,
                contentColor = StatusInfo,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    selected.size.toString(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    if (open) {
        val filtered = if (query.isBlank()) options else options.filter {
            it.label.contains(query, ignoreCase = true)
        }
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (options.size > searchThreshold) {
                    CompactSearchField(
                        value = query,
                        onValueChange = { query = it.take(120) },
                        placeholder = "Search options",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                ) {
                    if (filtered.isEmpty()) {
                        item(key = "no-filter-options") { EmptyState("No options found.", variant = EmptyStateVariant.FILTERED_NO_RESULT) }
                    }
                    items(filtered, key = { it.value }) { option ->
                        val checked = option.value in pending
                        Surface(
                            onClick = {
                                pendingValues = (if (checked) pending - option.value else pending + option.value).toList()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (checked) StatusInfoSoft else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(option.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (checked) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = StatusInfo
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    TextButton(
                        onClick = {
                            pendingValues = selected.toList()
                            open = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    SecondaryButton(
                        text = "Clear",
                        onClick = {
                            pendingValues = emptyList()
                            query = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Apply",
                        onClick = {
                            onApply(pending)
                            open = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(AppSpacing.xs))
            }
        }
    }
}

@Composable
fun AppDatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allowClear: Boolean = false,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    showDayOfWeek: Boolean = false,
    placeholder: String = "Select date"
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.of("Asia/Kolkata") }
    val pickerZone = zone
    val today = LocalDate.now(zone)
    val parsed = runCatching { LocalDate.parse(value) }.getOrNull()
    val selected = (parsed ?: minDate ?: today).let { candidate ->
        when {
            minDate != null && candidate.isBefore(minDate) -> minDate
            maxDate != null && candidate.isAfter(maxDate) -> maxDate
            else -> candidate
        }
    }
    val displayDate = parsed?.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH))
        ?: placeholder
    val displayDay = parsed?.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)).orEmpty()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onValueChange(LocalDate.of(year, month + 1, day).toString())
                    },
                    selected.year,
                    selected.monthValue - 1,
                    selected.dayOfMonth
                ).apply {
                    minDate?.let {
                        datePicker.minDate = it.atStartOfDay(pickerZone).toInstant().toEpochMilli()
                    }
                    maxDate?.let {
                        datePicker.maxDate = it.atStartOfDay(pickerZone).toInstant().toEpochMilli()
                    }
                }.show()
            },
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            enabled = enabled,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.xs))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Text(
                    displayDate,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showDayOfWeek && displayDay.isNotBlank()) {
                    Text(
                        displayDay,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (allowClear && value.isNotBlank()) {
            IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear $label")
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) { content() }
    }
}

@Composable
fun InfoValueRow(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    emphasized: Boolean = false,
    muted: Boolean = false
) {
    if (value.isBlank()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = if (muted) AppTextMuted else MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (muted) AppTextMuted else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun rememberReliableKeyboardDismiss(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    return remember(focusManager, keyboardController, view) {
        {
            val inputMethodManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            focusManager.clearFocus(force = true)
            view.clearFocus()
            keyboardController?.hide()
            inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
            view.post {
                keyboardController?.hide()
                inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    selectAllOnFocus: Boolean = false
) {
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    val effectiveKeyboardOptions = if (singleLine && keyboardOptions.imeAction == ImeAction.Default) {
        keyboardOptions.copy(imeAction = ImeAction.Done)
    } else keyboardOptions
    val effectiveKeyboardActions = if (effectiveKeyboardOptions.imeAction == ImeAction.Done) {
        KeyboardActions(
            onDone = {
                dismissKeyboard()
                keyboardActions.onDone?.invoke(this)
            },
            onGo = keyboardActions.onGo,
            onNext = keyboardActions.onNext,
            onPrevious = keyboardActions.onPrevious,
            onSearch = keyboardActions.onSearch,
            onSend = keyboardActions.onSend
        )
    } else keyboardActions
    val effectiveSelectAllOnFocus = selectAllOnFocus ||
        effectiveKeyboardOptions.keyboardType == KeyboardType.Number ||
        effectiveKeyboardOptions.keyboardType == KeyboardType.Decimal

    if (!effectiveSelectAllOnFocus) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            label = { Text(label) },
            placeholder = placeholder?.let { text -> { Text(text) } },
            visualTransformation = visualTransformation,
            keyboardOptions = effectiveKeyboardOptions,
            keyboardActions = effectiveKeyboardActions,
            supportingText = supportingText?.let { text -> { Text(text) } },
            isError = isError,
            trailingIcon = trailingIcon,
            leadingIcon = leadingIcon,
            minLines = minLines,
            maxLines = maxLines,
            shape = MaterialTheme.shapes.medium
        )
        return
    }

    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) fieldValue = TextFieldValue(value, TextRange(value.length))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { next ->
            fieldValue = next
            onValueChange(next.text)
        },
        modifier = modifier.fillMaxWidth().onFocusChanged { focus ->
            if (focus.isFocused && !hadFocus) {
                fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
            }
            hadFocus = focus.isFocused
        },
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        visualTransformation = visualTransformation,
        keyboardOptions = effectiveKeyboardOptions,
        keyboardActions = effectiveKeyboardActions,
        supportingText = supportingText?.let { text -> { Text(text) } },
        isError = isError,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        minLines = minLines,
        maxLines = maxLines,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searching: Boolean = false,
    onClear: (() -> Unit)? = { onValueChange("") },
    onSubmit: (() -> Unit)? = null
) {
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit == null) ImeAction.Done else ImeAction.Search),
        keyboardActions = KeyboardActions(
            onDone = { dismissKeyboard() },
            onSearch = {
                onSubmit?.invoke()
                dismissKeyboard()
            }
        ),
        placeholder = { Text(placeholder, maxLines = 1) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            when {
                searching -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                value.isNotBlank() && onClear != null -> IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                }
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun AppButtonContent(
    text: String,
    loading: Boolean,
    icon: (@Composable () -> Unit)? = null,
    progressColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = progressColor
        )
        Spacer(Modifier.width(AppSpacing.xs))
    } else if (icon != null) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(Modifier.width(AppSpacing.xs))
    }
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium
    ) {
        AppButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            progressColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    loading: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        AppButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            progressColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    dismissLabel: String = "Cancel",
    confirmEnabled: Boolean = true,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (destructive) {
                DangerTextButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled && !busy,
                    loading = busy
                )
            } else {
                TextButton(
                    onClick = onConfirm,
                    enabled = confirmEnabled && !busy
                ) { Text(confirmLabel) }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) { Text(dismissLabel) }
        }
    )
}

@Composable
fun AppUnsavedChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    title: String = "Discard unsaved changes?",
    message: String = "Your unsaved changes will be lost."
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            DangerTextButton(
                text = "Discard",
                onClick = onDiscard
            )
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text("Keep Editing") }
        }
    )
}

@Composable
fun InlineRetryMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StatusErrorSoft,
        contentColor = StatusError,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(retryLabel) }
        }
    }
}

@Composable
fun AppDestructiveConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    dismissLabel: String = "Cancel",
    requiredPhrase: String? = null,
    confirmEnabled: Boolean = true,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    var typedPhrase by remember(title, requiredPhrase) { mutableStateOf("") }
    val phraseMatches = requiredPhrase == null || typedPhrase == requiredPhrase

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(message)
                extraContent?.invoke(this)
                if (requiredPhrase != null) {
                    AppTextField(
                        value = typedPhrase,
                        onValueChange = { typedPhrase = it.take(80) },
                        label = "Type $requiredPhrase",
                        placeholder = requiredPhrase,
                        enabled = !busy
                    )
                }
            }
        },
        confirmButton = {
            DangerTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = !busy && confirmEnabled && phraseMatches,
                loading = busy
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) { Text(dismissLabel) }
        }
    )
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, StatusError),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
    ) {
        AppButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            progressColor = StatusError
        )
    }
}

@Composable
fun DangerTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled && !loading,
        colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = StatusError
            )
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class ActionTone { BRAND, INFO, SUCCESS, WARNING, DANGER, PURPLE, TEAL, NEUTRAL }

@Composable
fun SoftActionButton(
    icon: ImageVector,
    label: String? = null,
    contentDescription: String = label.orEmpty(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ActionTone = ActionTone.INFO
) {
    val colors = when (tone) {
        ActionTone.BRAND -> BrandSoft to MaterialTheme.colorScheme.primary
        ActionTone.INFO -> StatusInfoSoft to StatusInfo
        ActionTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        ActionTone.WARNING -> StatusWarningSoft to StatusWarning
        ActionTone.DANGER -> StatusErrorSoft to StatusError
        ActionTone.PURPLE -> DashboardGivenSoft to DashboardGiven
        ActionTone.TEAL -> DashboardItemsSoft to DashboardItems
        ActionTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (label.isNullOrBlank()) {
            Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

enum class BadgeTone { NEUTRAL, SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val pair = when (tone) {
        BadgeTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        BadgeTone.WARNING -> StatusWarningSoft to StatusWarning
        BadgeTone.ERROR -> StatusErrorSoft to StatusError
        BadgeTone.INFO -> StatusInfoSoft to StatusInfo
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = pair.first,
        contentColor = pair.second,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class MessageTone { SUCCESS, ERROR, WARNING, INFO }

private object FeedbackDeduper {
    private val lastShown = mutableMapOf<String, Long>()

    @Synchronized
    fun shouldShow(message: String, tone: MessageTone): Boolean {
        val now = System.currentTimeMillis()
        val key = "${tone.name}:$message"
        val previous = lastShown[key] ?: 0L
        lastShown[key] = now
        if (lastShown.size > 64) {
            val cutoff = now - 10_000L
            lastShown.entries.removeAll { it.value < cutoff }
        }
        return now - previous > 500L
    }
}

/**
 * One canonical transient feedback surface for Success / Error / Warning / Info.
 * Placement, radius, animation, timeout and dismiss behavior are intentionally identical across tones.
 */
@Composable
fun FeedbackMessage(
    message: String?,
    tone: MessageTone = MessageTone.SUCCESS,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    durationMillis: Long = 3500L
) {
    if (message.isNullOrBlank()) return
    val allowed by remember(message, tone) { mutableStateOf(FeedbackDeduper.shouldShow(message, tone)) }
    if (!allowed) return

    var visible by remember(message, tone) { mutableStateOf(true) }
    LaunchedEffect(message, tone, durationMillis) {
        delay(durationMillis)
        visible = false
    }

    val background = when (tone) {
        MessageTone.SUCCESS -> StatusSuccessSoft
        MessageTone.ERROR -> StatusErrorSoft
        MessageTone.WARNING -> StatusWarningSoft
        MessageTone.INFO -> StatusInfoSoft
    }
    val foreground = when (tone) {
        MessageTone.SUCCESS -> StatusSuccess
        MessageTone.ERROR -> StatusError
        MessageTone.WARNING -> StatusWarning
        MessageTone.INFO -> StatusInfo
    }
    val icon = when (tone) {
        MessageTone.SUCCESS -> Icons.Rounded.CheckCircle
        MessageTone.ERROR -> Icons.Rounded.Error
        MessageTone.WARNING -> Icons.Rounded.Warning
        MessageTone.INFO -> Icons.Rounded.Info
    }
    val density = LocalDensity.current
    val popupOffset = IntOffset(
        x = 0,
        y = WindowInsets.statusBars.getTop(density) + with(density) { 56.dp.roundToPx() }
    )

    Box(Modifier.size(0.dp)) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = popupOffset,
            properties = PopupProperties(focusable = false)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = background,
                        contentColor = foreground,
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = AppSpacing.md, end = AppSpacing.xs, top = AppSpacing.sm, bottom = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                message,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!actionLabel.isNullOrBlank() && onAction != null) {
                                TextButton(
                                    onClick = {
                                        visible = false
                                        onAction()
                                    }
                                ) {
                                    Text(actionLabel, color = foreground)
                                }
                            }
                            IconButton(
                                onClick = { visible = false },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp),
                                    tint = foreground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compatibility alias retained for existing screens while routing through the global feedback pattern. */
@Composable
fun PopupMessage(message: String?, tone: MessageTone = MessageTone.SUCCESS) {
    FeedbackMessage(message = message, tone = tone)
}


@Composable
fun AppFeedbackHost(
    successMessage: String? = null,
    errorMessage: String? = null,
    warningMessage: String? = null,
    infoMessage: String? = null
) {
    when {
        !errorMessage.isNullOrBlank() -> PopupMessage(errorMessage, MessageTone.ERROR)
        !warningMessage.isNullOrBlank() -> PopupMessage(warningMessage, MessageTone.WARNING)
        !successMessage.isNullOrBlank() -> PopupMessage(successMessage, MessageTone.SUCCESS)
        !infoMessage.isNullOrBlank() -> PopupMessage(infoMessage, MessageTone.INFO)
    }
}

@Composable
fun InlineStatusMessage(
    message: String?,
    tone: MessageTone = MessageTone.INFO
) {
    if (message.isNullOrBlank()) return
    val background = when (tone) {
        MessageTone.SUCCESS -> StatusSuccessSoft
        MessageTone.ERROR -> StatusErrorSoft
        MessageTone.WARNING -> StatusWarningSoft
        MessageTone.INFO -> StatusInfoSoft
    }
    val foreground = when (tone) {
        MessageTone.SUCCESS -> StatusSuccess
        MessageTone.ERROR -> StatusError
        MessageTone.WARNING -> StatusWarning
        MessageTone.INFO -> StatusInfo
    }
    val icon = when (tone) {
        MessageTone.SUCCESS -> Icons.Rounded.CheckCircle
        MessageTone.ERROR -> Icons.Rounded.Error
        MessageTone.WARNING -> Icons.Rounded.Warning
        MessageTone.INFO -> Icons.Rounded.Info
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Legacy transient-feedback alias. Use InlineStatusMessage for passive state/information content.
 */
@Composable
fun InlineMessage(
    message: String?,
    error: Boolean = false,
    tone: MessageTone? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    FeedbackMessage(
        message = message,
        tone = tone ?: if (error) MessageTone.ERROR else MessageTone.SUCCESS,
        actionLabel = actionLabel,
        onAction = onAction
    )
}

enum class EmptyStateVariant { NORMAL, SEARCH_NO_RESULT, FILTERED_NO_RESULT }

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    variant: EmptyStateVariant = EmptyStateVariant.NORMAL,
    action: (@Composable () -> Unit)? = null
) {
    val topPadding = when (variant) {
        EmptyStateVariant.NORMAL -> AppSpacing.xl
        EmptyStateVariant.SEARCH_NO_RESULT, EmptyStateVariant.FILTERED_NO_RESULT -> AppSpacing.lg
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        action?.invoke()
    }
}

@Composable
fun LoadingState(label: String = "Loading…") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(AppSpacing.sm))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
    }
}

@Composable
fun LoadFailureState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Icon(
            Icons.Rounded.Error,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = StatusError
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = StatusError,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        SecondaryButton(
            text = retryLabel,
            onClick = onRetry
        )
    }
}

fun statusTone(status: String): BadgeTone = when (status.uppercase()) {
    "COMPLETED", "RETURNED", "FULL_RETURN", "ACTIVE" -> BadgeTone.SUCCESS
    "CANCELLED", "OVERDUE" -> BadgeTone.ERROR
    "CONFIRMED", "BOOKED", "PART_PICKUP", "FULL_PICKUP", "PART_RETURN", "RESERVED" -> BadgeTone.WARNING
    else -> BadgeTone.INFO
}
