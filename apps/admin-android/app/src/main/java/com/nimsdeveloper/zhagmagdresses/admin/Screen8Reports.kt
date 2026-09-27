package com.nimsdeveloper.zhagmagdresses.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCompactScrollableTabs
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDatePickerField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppMultiSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSingleSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LabeledSectionCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveActionTriple
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate


private fun screen8DashboardFilterLabel(value: String): String = when (value.uppercase()) {
    "AVAILABLE_NOW" -> "Available Now"
    "PICKUP_PENDING" -> "Pickup Pending"
    "LOW_STOCK" -> "Low Stock"
    "UNAVAILABLE" -> "Unavailable"
    "NEW" -> "New Customers"
    "RETURNING" -> "Returning Customers"
    "TODAY" -> "Today"
    "ALL_TIME" -> "All Time"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

private enum class Screen8PdfAction { VIEW, DOWNLOAD, SHARE }

@Composable
internal fun Screen8Reports(
    currentUser: SessionUser,
    branding: AppBranding,
    businessDate: String,
    viewModel: Screen8ReportsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    val context = LocalContext.current
    var presetDialog by remember { mutableStateOf<Screen8Preset?>(null) }
    var newPresetDialog by remember { mutableStateOf(false) }
    var deletePreset by remember { mutableStateOf<Screen8Preset?>(null) }
    var pendingPdfAction by remember { mutableStateOf<Screen8PdfAction?>(null) }
    var pdfMessage by remember { mutableStateOf<String?>(null) }
    var pdfError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearFeedback()
        viewModel.ensureLoaded()
    }

    BackHandler {
        if (!state.presetBusy && !state.exportLoading && pendingPdfAction == null) onBack()
    }

    LaunchedEffect(pendingPdfAction, state.exportData, state.exportLoading, state.error) {
        val action = pendingPdfAction ?: return@LaunchedEffect
        if (state.exportLoading) return@LaunchedEffect
        val export = state.exportData
        if (export == null) {
            if (!state.error.isNullOrBlank() || state.generatedConfig == null || !state.hasRows) {
                pendingPdfAction = null
            }
            return@LaunchedEffect
        }

        try {
            val file = withContext(Dispatchers.IO) {
                Screen8PdfExporter.create(context, branding, currentUser, export)
            }
            when (action) {
                Screen8PdfAction.VIEW -> Screen8PdfExporter.view(context, file)
                Screen8PdfAction.SHARE -> Screen8PdfExporter.share(context, file, export.title)
                Screen8PdfAction.DOWNLOAD -> {
                    val message = withContext(Dispatchers.IO) {
                        Screen8PdfExporter.download(context, file)
                    }
                    pdfMessage = message
                    pdfError = false
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            pdfMessage = "Unable to create PDF. Please try again."
            pdfError = true
        } finally {
            if (pendingPdfAction == action) pendingPdfAction = null
        }
    }

    fun requestPdf(action: Screen8PdfAction) {
        if (!state.hasRows || state.exportLoading || pendingPdfAction != null) return
        pdfMessage = null
        pendingPdfAction = action
        viewModel.requestExport()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.xs,
            bottom = AppSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        item { MainScreenDateRow(businessDate) }
        item {
            AppBackHeader(
                title = "Reports",
                onBack = onBack,
                enabled = !state.presetBusy && !state.exportLoading && pendingPdfAction == null
            )
        }
        if (state.config.dashboardFilter.isNotBlank()) {
            item {
                StatusBadge(
                    text = "Dashboard Filter · " + screen8DashboardFilterLabel(state.config.dashboardFilter),
                    tone = BadgeTone.INFO
                )
            }
        }
        item {
            Screen8SectionTabs(
                selected = screen8SectionForType(state.config.type),
                isOwner = currentUser.role == UserRole.OWNER,
                onSelect = { section ->
                    viewModel.setSection(section, currentUser.role == UserRole.OWNER)
                }
            )
        }

        if (state.loadingBootstrap && state.bootstrap == null) item { LoadingState() }
        if (!state.loadError.isNullOrBlank() && state.bootstrap == null && !state.loadingBootstrap) {
            item {
                LoadFailureState(
                    message = state.loadError.orEmpty(),
                    onRetry = viewModel::ensureLoaded
                )
            }
        } else if (!state.loadError.isNullOrBlank()) {
            item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
        }

        state.bootstrap?.let { bootstrap ->
            item {
                ReportGeneratorCard(
                    state = state,
                    bootstrap = bootstrap,
                    onType = viewModel::setType,
                    onDateBasis = viewModel::setDateBasis,
                    onDatePreset = viewModel::setDatePreset,
                    onFromDate = viewModel::setFromDate,
                    onToDate = viewModel::setToDate,
                    isOwner = currentUser.role == UserRole.OWNER,
                    onCategory = viewModel::setCategory,
                    onCustomFilter = viewModel::setCustomFilter,
                    onItemSearch = viewModel::setItemSearch,
                    onCustomerSearch = viewModel::setCustomerSearch,
                    onStatus = viewModel::setStatus,
                    onStaff = viewModel::setStaff,
                    onGrouping = viewModel::setGrouping,
                    onSort = viewModel::setSort,
                    onSearch = viewModel::setSearch,
                    onClearFilters = viewModel::resetCurrentFilters,
                    onGenerate = viewModel::generate,
                    onSavePreset = { newPresetDialog = true }
                )
            }

            if (bootstrap.presets.isNotEmpty()) {
                item { Text("Saved Presets", style = MaterialTheme.typography.titleMedium) }
                items(bootstrap.presets, key = { "preset-" + it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        currentUser = currentUser,
                        busy = state.presetBusy,
                        onUse = { viewModel.applyPreset(preset) },
                        onEdit = {
                            viewModel.applyPreset(preset)
                            presetDialog = preset
                        },
                        onDelete = { deletePreset = preset }
                    )
                }
            }
        }

        if (state.generated) {
            item {
                ReportResultHeader(
                    state = state,
                    onRefresh = viewModel::refreshGenerated,
                    pendingPdfAction = pendingPdfAction,
                    onViewPdf = { requestPdf(Screen8PdfAction.VIEW) },
                    onDownloadPdf = { requestPdf(Screen8PdfAction.DOWNLOAD) },
                    onSharePdf = { requestPdf(Screen8PdfAction.SHARE) }
                )
            }

            if (state.loadingReport && state.rows.isEmpty()) item { LoadingState() }
            if (!state.loadingReport && state.rows.isEmpty()) {
                item { EmptyState("No records found for the selected filters.") }
            }

            if (state.summary.isNotEmpty()) {
                items(
                    items = state.summary.chunked(2),
                    key = { chunk -> chunk.joinToString("|") { it.label } }
                ) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        chunk.forEach { summary -> SummaryCard8(summary, Modifier.weight(1f)) }
                        if (chunk.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            itemsIndexed(
                items = state.rows,
                key = { index, row -> "report-" + index + "-" + row.values.hashCode() }
            ) { index, row ->
                val grouping = state.generatedConfig?.grouping ?: "NONE"
                val group = screen8GroupLabel(grouping, row)
                val previousGroup = if (index > 0) {
                    screen8GroupLabel(grouping, state.rows[index - 1])
                } else null

                if (grouping != "NONE" && group.isNotBlank() && group != previousGroup) {
                    Text(group, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(AppSpacing.xxs))
                }
                ReportRowCard8(row, state.columns)

                if (index == state.rows.lastIndex && state.canLoadMore) {
                    LaunchedEffect(state.page, state.rows.size) { viewModel.loadMore() }
                }
            }

            if (state.loadingMore) item { LoadingState() }
            if (!state.loadMoreError.isNullOrBlank()) {
                item {
                    InlineRetryMessage(
                        message = state.loadMoreError.orEmpty(),
                        onRetry = viewModel::retryLoadMore,
                        retryLabel = "Retry loading more"
                    )
                }
            }
            if (state.rows.isNotEmpty()) {
                item {
                    Text(
                        "Showing " + state.rows.size + " of " + state.total,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextMuted
                    )
                }
            }
        }

        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    AppFeedbackHost(
        successMessage = state.message ?: pdfMessage.takeIf { !pdfError },
        errorMessage = state.error ?: pdfMessage.takeIf { pdfError }
    )

    if (newPresetDialog) {
        PresetEditorDialog8(
            preset = null,
            currentUser = currentUser,
            busy = state.presetBusy,
            onDismiss = { newPresetDialog = false },
            onSave = { name, visibility ->
                viewModel.savePreset(null, name, visibility)
                newPresetDialog = false
            }
        )
    }

    presetDialog?.let { preset ->
        PresetEditorDialog8(
            preset = preset,
            currentUser = currentUser,
            busy = state.presetBusy,
            onDismiss = { presetDialog = null },
            onSave = { name, visibility ->
                viewModel.savePreset(preset.id, name, visibility)
                presetDialog = null
            }
        )
    }

    deletePreset?.let { preset ->
        AppDestructiveConfirmDialog(
            title = "Delete preset?",
            message = "Delete “${preset.name}”? This saved preset will be removed permanently.",
            confirmLabel = "Delete",
            busy = state.presetBusy,
            onConfirm = {
                viewModel.deletePreset(preset.id)
                deletePreset = null
            },
            onDismiss = { deletePreset = null }
        )
    }
}

@Composable
private fun Screen8SectionTabs(
    selected: String,
    isOwner: Boolean,
    onSelect: (String) -> Unit
) {
    val sections = screen8Sections.filter { isOwner || it.value != "BILLING" }
    val selectedIndex = sections.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    AppCompactScrollableTabs(
        labels = sections.map { it.label },
        selectedIndex = selectedIndex,
        onSelect = { index -> onSelect(sections[index].value) }
    )
}

@Composable
private fun Screen8CustomFieldFilter(
    field: Screen8CategoryFieldOption,
    value: String,
    onChange: (String) -> Unit
) {
    when (field.type) {
        "DROPDOWN" -> Screen8SharedSingleSelect(
            label = field.name,
            selected = value,
            options = listOf(Screen8Option("", "All")) + field.options.map { Screen8Option(it, it) },
            onSelect = onChange
        )
        "MULTI_SELECT" -> {
            val selected = value.split(',').map(String::trim).filter(String::isNotBlank).toSet()
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(field.name, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                AppMultiSelectFilter(
                    label = field.name,
                    selected = selected,
                    options = field.options.map { AppFilterOption(it, it) },
                    onApply = { next ->
                        onChange(field.options.filter { it in next }.joinToString(","))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    title = "Select " + field.name
                )
            }
        }
        "YES_NO" -> Screen8SharedSingleSelect(
            label = field.name,
            selected = value,
            options = listOf(
                Screen8Option("", "All"),
                Screen8Option("1", "Yes"),
                Screen8Option("0", "No")
            ),
            onSelect = onChange
        )
        "DATE" -> AppDatePickerField(
            label = field.name,
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            allowClear = true
        )
        "NUMBER" -> AppTextField(
            value = value,
            onValueChange = onChange,
            label = field.name,
            placeholder = "Any value",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        else -> AppTextField(
            value = value,
            onValueChange = onChange,
            label = field.name,
            placeholder = "Any value",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
    }
}

@Composable
private fun Screen8SharedSingleSelect(
    label: String,
    selected: String,
    options: List<Screen8Option>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
        AppSingleSelectFilter(
            label = label,
            selected = selected,
            options = options.map { AppFilterOption(it.value, it.label) },
            resetValue = options.firstOrNull()?.value.orEmpty(),
            onApply = onSelect,
            modifier = Modifier.fillMaxWidth(),
            title = "Select " + label,
            enabled = enabled
        )
    }
}

@Composable
private fun ReportGeneratorCard(
    state: Screen8ReportsState,
    bootstrap: Screen8Bootstrap,
    isOwner: Boolean,
    onType: (String) -> Unit,
    onDateBasis: (String) -> Unit,
    onDatePreset: (String) -> Unit,
    onFromDate: (String) -> Unit,
    onToDate: (String) -> Unit,
    onCategory: (String) -> Unit,
    onCustomFilter: (String, String) -> Unit,
    onItemSearch: (String) -> Unit,
    onCustomerSearch: (String) -> Unit,
    onStatus: (String) -> Unit,
    onStaff: (String) -> Unit,
    onGrouping: (String) -> Unit,
    onSort: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearFilters: () -> Unit,
    onGenerate: () -> Unit,
    onSavePreset: () -> Unit
) {
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    val config = state.config
    val type = config.type
    val section = screen8SectionForType(type)
    val reportOptions = screen8ReportOptionsForSection(section, isOwner)
    val dateBasisOptions = screen8DateBasisOptions(type)
    val groupingOptions = screen8GroupingOptions(type)
    val supportsDates = screen8SupportsDates(type)
    val supportsCategory = screen8SupportsCategory(type)
    val supportsItem = screen8SupportsItem(type)
    val supportsCustomer = screen8SupportsCustomer(type)
    val supportsStatus = screen8SupportsStatus(type)
    val supportsStaff = screen8SupportsStaff(type)
    val customFields = if (screen8SupportsCustomFields(type) && config.categoryId.isNotBlank()) {
        bootstrap.categoryFields.filter { it.categoryId == config.categoryId }
    } else {
        emptyList()
    }

    LabeledSectionCard(title = "Report Options") {
        if (reportOptions.size > 1) {
            Screen8SharedSingleSelect(
                label = "Report",
                selected = config.type,
                options = reportOptions,
                onSelect = onType,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppSpacing.sm))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Screen8SharedSingleSelect(
                label = "Grouping",
                selected = config.grouping,
                options = groupingOptions,
                onSelect = onGrouping,
                modifier = Modifier.weight(1f)
            )
            Screen8SharedSingleSelect(
                label = "Sort",
                selected = config.sort,
                options = screen8SortOptionsFor(config.type),
                onSelect = onSort,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))
        if (supportsDates) {
            Screen8SharedSingleSelect(
                label = "Date Range",
                selected = config.datePreset,
                options = screen8DatePresets,
                onSelect = onDatePreset,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (supportsDates) {
            if (dateBasisOptions.size > 1) {
                Spacer(Modifier.height(AppSpacing.sm))
                Screen8SharedSingleSelect(
                    label = "Date Based On",
                    selected = config.dateBasis,
                    options = dateBasisOptions,
                    onSelect = onDateBasis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (config.datePreset == "CUSTOM") {
                Spacer(Modifier.height(AppSpacing.sm))
                val parsedFrom = runCatching { LocalDate.parse(config.fromDate) }.getOrNull()
                val parsedTo = runCatching { LocalDate.parse(config.toDate) }.getOrNull()
                ResponsiveCompactPair(
                    first = { child ->
                        AppDatePickerField(
                            label = "From Date",
                            value = config.fromDate,
                            onValueChange = onFromDate,
                            modifier = child,
                            maxDate = parsedTo
                        )
                    },
                    second = { child ->
                        AppDatePickerField(
                            label = "To Date",
                            value = config.toDate,
                            onValueChange = onToDate,
                            modifier = child,
                            minDate = parsedFrom
                        )
                    }
                )
            }
        }

        if (supportsCategory) {
            Spacer(Modifier.height(AppSpacing.sm))
            Screen8SharedSingleSelect(
                label = "Category",
                selected = config.categoryId,
                options = listOf(Screen8Option("", "All Categories")) +
                    bootstrap.categories.map { Screen8Option(it.id, it.name) },
                onSelect = onCategory,
                modifier = Modifier.fillMaxWidth()
            )
        }

        customFields.forEach { field ->
            Spacer(Modifier.height(AppSpacing.sm))
            Screen8CustomFieldFilter(
                field = field,
                value = config.customFilters[field.id].orEmpty(),
                onChange = { value -> onCustomFilter(field.id, value) }
            )
        }

        if (supportsItem) {
            Spacer(Modifier.height(AppSpacing.sm))
            AppTextField(
                value = config.itemSearch,
                onValueChange = onItemSearch,
                label = "Item",
                placeholder = "All items or search item/code",
                leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        if (supportsCustomer) {
            Spacer(Modifier.height(AppSpacing.sm))
            AppTextField(
                value = config.customerSearch,
                onValueChange = onCustomerSearch,
                label = "Customer",
                placeholder = "All customers or search name/mobile",
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        if (supportsStatus) {
            Spacer(Modifier.height(AppSpacing.sm))
            Screen8SharedSingleSelect(
                label = "Status",
                selected = config.status,
                options = listOf(Screen8Option("", "All Statuses")) +
                    screen8StatusOptions(type, bootstrap.statuses)
                        .map { Screen8Option(it, screen8StatusLabel(it)) },
                onSelect = onStatus,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (supportsStaff) {
            Spacer(Modifier.height(AppSpacing.sm))
            Screen8SharedSingleSelect(
                label = "Staff / User",
                selected = config.staffUserId,
                options = listOf(Screen8Option("", "All Users")) +
                    bootstrap.staff.map { Screen8Option(it.id, it.name + " · " + it.role) },
                onSelect = onStaff,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))
        AppTextField(
            value = config.search,
            onValueChange = onSearch,
            label = "Search",
            placeholder = "Optional report search",
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dismissKeyboard() })
        )

        Spacer(Modifier.height(AppSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            SecondaryButton(
                text = "Clear Filters",
                onClick = onClearFilters,
                modifier = Modifier.weight(1f),
                enabled = !state.loadingReport && !state.loadingMore
            )
            SecondaryButton(
                text = "Save Preset",
                onClick = onSavePreset,
                modifier = Modifier.weight(1f),
                enabled = !state.presetBusy,
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) }
            )
        }
        Spacer(Modifier.height(AppSpacing.sm))
        PrimaryButton(
            text = if (state.loadingReport) "Generating…" else "Generate Report",
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loadingReport && !state.loadingMore,
            loading = state.loadingReport,
            icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) }
        )
    }
}

@Composable
private fun ReportResultHeader(
    state: Screen8ReportsState,
    onRefresh: () -> Unit,
    pendingPdfAction: Screen8PdfAction?,
    onViewPdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    val pdfBusy = state.exportLoading || pendingPdfAction != null

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.title.ifBlank { "Generated Report" }, style = MaterialTheme.typography.titleLarge)
                Text(state.total.toString() + " records", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
            }
            SoftActionButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh report",
                onClick = onRefresh,
                enabled = !state.loadingReport && !state.loadingMore,
                tone = ActionTone.INFO
            )
        }

        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            SecondaryButton(
                text = "View",
                onClick = onViewPdf,
                modifier = Modifier.weight(1f),
                enabled = state.hasRows && !pdfBusy,
                loading = pendingPdfAction == Screen8PdfAction.VIEW,
                icon = { Icon(Icons.Rounded.Visibility, contentDescription = null) }
            )
            SecondaryButton(
                text = "Share",
                onClick = onSharePdf,
                modifier = Modifier.weight(1f),
                enabled = state.hasRows && !pdfBusy,
                loading = pendingPdfAction == Screen8PdfAction.SHARE,
                icon = { Icon(Icons.Rounded.Share, contentDescription = null) }
            )
        }
        Spacer(Modifier.height(AppSpacing.xxs))
        SecondaryButton(
            text = "Download",
            onClick = onDownloadPdf,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hasRows && !pdfBusy,
            loading = pendingPdfAction == Screen8PdfAction.DOWNLOAD,
            icon = { Icon(Icons.Rounded.Download, contentDescription = null) }
        )
    }
}

@Composable
private fun SummaryCard8(item: Screen8SummaryItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = AppTextMuted,
                maxLines = 2
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReportRowCard8(row: Screen8ReportRow, columns: List<Screen8Column>) {
    AppCard {
        columns.take(6).forEachIndexed { index, column ->
            val value = row[column.key].ifBlank { "—" }
            if (index == 0) {
                if (screen8SemanticColumn(column.key)) {
                    StatusBadge(screen8StatusLabel(value), screen8BadgeTone(value))
                } else {
                    Text(value, style = MaterialTheme.typography.titleSmall)
                }
                Text(column.label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            } else {
                Spacer(Modifier.height(AppSpacing.xxs))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        column.label,
                        modifier = Modifier.weight(0.42f),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTextMuted
                    )
                    if (screen8SemanticColumn(column.key)) {
                        Box(modifier = Modifier.weight(0.58f)) {
                            StatusBadge(screen8StatusLabel(value), screen8BadgeTone(value))
                        }
                    } else {
                        Text(
                            value,
                            modifier = Modifier.weight(0.58f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        if (columns.size > 6) {
            Spacer(Modifier.height(AppSpacing.xxs))
            Text(
                "+ " + (columns.size - 6) + " more fields in PDF",
                style = MaterialTheme.typography.labelSmall,
                color = AppTextMuted
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: Screen8Preset,
    currentUser: SessionUser,
    busy: Boolean,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                val reportLabel = screen8ReportTypes.firstOrNull { it.value == preset.config.type }?.label
                    ?: preset.config.type
                Text(
                    reportLabel + " · " + preset.ownerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextMuted
                )
            }
            StatusBadge(
                if (preset.visibility == "SHARED") "SHARED" else "MY",
                if (preset.visibility == "SHARED") BadgeTone.INFO else BadgeTone.NEUTRAL
            )
        }
        Spacer(Modifier.height(AppSpacing.xs))
        if (preset.canEdit) {
            ResponsiveActionTriple(
                first = { child ->
                    PrimaryButton("Use", onUse, child, enabled = !busy)
                },
                second = { child ->
                    SecondaryButton(
                        "Edit",
                        onEdit,
                        child,
                        enabled = !busy,
                        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                    )
                },
                third = { child ->
                    DangerButton(
                        text = "Delete",
                        onClick = onDelete,
                        modifier = child,
                        enabled = !busy,
                        icon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                    )
                }
            )
        } else {
            PrimaryButton(
                text = "Use",
                onClick = onUse,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )
        }
    }
}

@Composable
private fun PresetEditorDialog8(
    preset: Screen8Preset?,
    currentUser: SessionUser,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by rememberSaveable(preset?.id) { mutableStateOf(preset?.name.orEmpty()) }
    var nameTouched by rememberSaveable(preset?.id) { mutableStateOf(false) }
    var visibility by rememberSaveable(preset?.id) {
        mutableStateOf(if (currentUser.role == UserRole.OWNER) preset?.visibility ?: "MY" else "MY")
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (preset == null) "Save Report Preset" else "Edit Report Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it.take(80)
                    },
                    label = "Preset Name *",
                    enabled = !busy,
                    isError = nameTouched && name.trim().isBlank(),
                    supportingText = if (nameTouched && name.trim().isBlank()) "Preset name is required." else null
                )
                if (currentUser.role == UserRole.OWNER) {
                    Screen8SharedSingleSelect(
                        label = "Visibility",
                        selected = visibility,
                        options = listOf(
                            Screen8Option("MY", "My Preset"),
                            Screen8Option("SHARED", "Shared Preset")
                        ),
                        onSelect = { visibility = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                } else {
                    Text(
                        "Visibility: My Preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextMuted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, visibility) },
                enabled = !busy && name.trim().isNotBlank()
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}

private fun screen8SemanticColumn(key: String): Boolean =
    key in setOf("status", "action", "availability_status", "customer_type", "outcome", "exception_type")

private fun screen8BadgeTone(value: String): BadgeTone = when (value.uppercase()) {
    "FULL_RETURN", "RETURNED", "AVAILABLE", "PREPARED" -> BadgeTone.SUCCESS
    "MISSED_PICKUP", "OVERDUE", "CANCELLED", "FAILED", "FULLY_BOOKED" -> BadgeTone.ERROR
    "RETURN_PENDING", "PICKUP_PENDING" -> BadgeTone.WARNING
    "PART_PICKUP", "PARTIALLY_GIVEN", "FULL_PICKUP", "GIVEN", "PART_RETURN",
    "PARTIALLY_RETURNED", "PARTIAL" -> BadgeTone.WARNING
    "RESERVED", "BOOKED", "NEW", "RETURNING" -> BadgeTone.INFO
    else -> BadgeTone.NEUTRAL
}

private fun screen8GroupLabel(grouping: String, row: Screen8ReportRow): String = when (grouping) {
    "DATE" -> row["activity_at"].take(10)
        .ifBlank { row["due_date"] }
        .ifBlank { row["pickup_date"] }
        .ifBlank { row["return_date"] }
        .ifBlank { row["booking_date"] }
    "CATEGORY" -> row["category_name"]
    "ITEM" -> row["item_name"]
    "CUSTOMER" -> row["customer_name"]
    "STAFF" -> row["staff_name"]
    "STATUS" -> row["status"]
        .ifBlank { row["outcome"] }
        .ifBlank { row["exception_type"] }
        .ifBlank { row["customer_type"] }
        .ifBlank { row["availability_status"] }
        .takeIf(String::isNotBlank)
        ?.let(::screen8StatusLabel)
        .orEmpty()
    else -> ""
}
