package com.nimsdeveloper.zhagmagdresses.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCompactFixedTabs
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFormSheetScaffold
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSingleSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSelectField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppMultiSelectField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDatePickerField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppUnsavedChangesDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyStateVariant
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.RoundedItemImage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Screen5StringMapSaver = Saver<Map<String, String>, String>(
    save = { JSONObject(it).toString() },
    restore = { raw ->
        runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }
)
private val Screen5StringSetSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() }
)
private val Screen5StringListSaver = Saver<List<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toList() }
)
private val Screen5PhotoListSaver = Saver<List<Screen5UploadedAsset>, String>(
    save = { list ->
        JSONArray().apply {
            list.forEach { put(JSONObject().put("url", it.url).put("publicId", it.publicId)) }
        }.toString()
    },
    restore = { raw ->
        runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    val url = row.optString("url")
                    if (url.isNotBlank()) add(Screen5UploadedAsset(url, row.optString("publicId")))
                }
            }
        }.getOrDefault(emptyList())
    }
)

private enum class Screen5Tab(val label: String) {
    CATEGORIES("Categories"), ITEMS("Items"), RELATED("Related Items")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Screen5ItemManagement(
    user: SessionUser,
    businessDate: String,
    onBack: () -> Unit,
    initialItemsTab: Boolean = false,
    initialCategoryId: String = "",
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) {
        Screen5ItemManagementRepository(
            ApiClient(BuildConfig.API_BASE_URL, SecureSessionStore(appContext)),
            appContext.contentResolver
        )
    }
    val factory = remember(repository) { Screen5ItemManagementViewModel.Factory(repository) }
    val vm: Screen5ItemManagementViewModel = viewModel(factory = factory)
    val state = vm.state
    val canEdit = user.role != UserRole.STAFF
    var tab by rememberSaveable(initialItemsTab, initialCategoryId) {
        mutableStateOf(
            if (initialItemsTab || initialCategoryId.isNotBlank()) Screen5Tab.ITEMS
            else Screen5Tab.CATEGORIES
        )
    }

    LaunchedEffect(Unit) {
        vm.clearActionFeedback()
        vm.ensureLoaded()
    }

    BackHandler {
        if (!state.busy) onBack()
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = vm::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.loading && !state.loaded -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    MainScreenDateRow(businessDate)
                    AppBackHeader(title = "Category & Items", onBack = onBack, enabled = !state.busy)
                    LoadingState()
                }
            }
            !state.loaded && !state.loadError.isNullOrBlank() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    MainScreenDateRow(businessDate)
                    AppBackHeader(title = "Category & Items", onBack = onBack, enabled = !state.busy)
                    LoadFailureState(
                        message = state.loadError.orEmpty(),
                        onRetry = vm::ensureLoaded
                    )
                }
            }
            else -> {
                Screen5Workspace(
                    state = state,
                    tab = tab,
                    canEdit = canEdit,
                    businessDate = businessDate,
                    onBack = onBack,
                    initialCategoryId = initialCategoryId,
                    onTab = {
                        vm.clearActionFeedback()
                        tab = it
                    },
                    vm = vm
                )
            }
        }
    }

    AppFeedbackHost(
        successMessage = state.notice,
        errorMessage = state.error
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5Workspace(
    state: Screen5ItemManagementState,
    tab: Screen5Tab,
    canEdit: Boolean,
    businessDate: String,
    onBack: () -> Unit,
    initialCategoryId: String,
    onTab: (Screen5Tab) -> Unit,
    vm: Screen5ItemManagementViewModel
) {
    var search by rememberSaveable(tab) { mutableStateOf("") }
    var visibleLimit by remember(tab, search) { mutableIntStateOf(10) }
    var relatedVisibleLimit by remember(tab) { mutableIntStateOf(10) }
    var expandedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryEditor by remember { mutableStateOf<Screen5Category?>(null) }
    var createCategory by remember { mutableStateOf(false) }
    var fieldEditor by remember { mutableStateOf<Pair<String, Screen5CategoryField?>?>(null) }
    var itemEditor by remember { mutableStateOf<Screen5Item?>(null) }
    var viewItem by remember { mutableStateOf<Screen5Item?>(null) }
    var deleteItem by remember { mutableStateOf<Screen5Item?>(null) }
    var createItem by remember { mutableStateOf(false) }
    var deleteCategory by remember { mutableStateOf<Screen5Category?>(null) }
    var deleteField by remember { mutableStateOf<Screen5CategoryField?>(null) }
    var strongDeleteField by remember { mutableStateOf<Screen5CategoryField?>(null) }
    var strongDeleteText by remember { mutableStateOf("") }
    var sourceItemId by rememberSaveable { mutableStateOf("") }
    var relatedEditorOpen by rememberSaveable { mutableStateOf(false) }
    var relatedInitialSourceId by rememberSaveable { mutableStateOf("") }
    var relatedSelectionDirty by rememberSaveable { mutableStateOf(false) }
    var showRelatedDiscard by rememberSaveable { mutableStateOf(false) }
    var relatedViewSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var relatedDeleteSourceId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeSourceItems = state.data.items.filter { !it.archived && it.active }
    LaunchedEffect(state.loaded, initialCategoryId) {
        if (
            state.loaded &&
            initialCategoryId.isNotBlank() &&
            (state.itemCategoryId != initialCategoryId || state.itemSearch.isNotBlank())
        ) {
            vm.openItemCategory(initialCategoryId)
        }
    }
    LaunchedEffect(activeSourceItems.map { it.id }.joinToString("|")) {
        if (sourceItemId.isNotBlank() && activeSourceItems.none { it.id == sourceItemId }) {
            sourceItemId = ""
        }
    }

    val relatedEditorDirty = relatedSelectionDirty || sourceItemId != relatedInitialSourceId
    val closeRelatedEditor: () -> Unit = {
        relatedEditorOpen = false
        sourceItemId = ""
        relatedInitialSourceId = ""
        relatedSelectionDirty = false
    }
    val requestCloseRelatedEditor: () -> Unit = {
        if (!state.busy) {
            if (relatedEditorDirty) showRelatedDiscard = true else closeRelatedEditor()
        }
    }

    BackHandler(enabled = relatedEditorOpen) {
        requestCloseRelatedEditor()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = AppSpacing.xs, bottom = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        item { MainScreenDateRow(businessDate) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppBackHeader(
                    title = "Category & Items",
                    onBack = {
                        if (relatedEditorOpen) requestCloseRelatedEditor() else onBack()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy
                )
                if (canEdit && !(tab == Screen5Tab.RELATED && relatedEditorOpen)) {
                    CompactNewActionButton(
                        label = when (tab) {
                            Screen5Tab.CATEGORIES -> "New Category"
                            Screen5Tab.ITEMS -> "New Item"
                            Screen5Tab.RELATED -> "New Group"
                        },
                        enabled = !state.busy,
                        onClick = {
                            when (tab) {
                                Screen5Tab.CATEGORIES -> createCategory = true
                                Screen5Tab.ITEMS -> createItem = true
                                Screen5Tab.RELATED -> {
                                    sourceItemId = ""
                                    relatedInitialSourceId = ""
                                    relatedSelectionDirty = false
                                    relatedEditorOpen = true
                                }
                            }
                        }
                    )
                }
            }
        }
        item {
            val tabs = Screen5Tab.values().toList()
            AppCompactFixedTabs(
                labels = tabs.map { it.label },
                selectedIndex = tabs.indexOf(tab),
                onSelect = { index -> onTab(tabs[index]) }
            )
        }
        if (!state.loadError.isNullOrBlank()) {
            item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
        }

        when (tab) {
            Screen5Tab.CATEGORIES -> {
                item {
                    CompactSearchField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search categories",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val filtered = state.data.categories.filter {
                    search.isBlank() || it.name.contains(search, true) || it.codePrefix.contains(search, true)
                }
                if (filtered.isEmpty()) item {
                    EmptyState(
                        "No categories found.",
                        variant = if (search.isNotBlank()) EmptyStateVariant.SEARCH_NO_RESULT else EmptyStateVariant.NORMAL
                    )
                }
                filtered.take(visibleLimit).forEach { category ->
                    item(key = "category-${category.id}") {
                        Screen5CategoryCard(
                            category = category,
                            expanded = expandedCategoryId == category.id,
                            canEdit = canEdit,
                            busy = state.busy,
                            onToggleFields = {
                                expandedCategoryId = if (expandedCategoryId == category.id) null else category.id
                            },
                            onEdit = { categoryEditor = category },
                            onAddField = { fieldEditor = category.id to null },
                            onEditField = { fieldEditor = category.id to it },
                            onDeleteField = { deleteField = it },
                            onDelete = { deleteCategory = category }
                        )
                    }
                }
                if (filtered.size > visibleLimit) item(key = "category-more-$visibleLimit") {
                    LaunchedEffect(filtered.size, visibleLimit) {
                        visibleLimit = (visibleLimit + 10).coerceAtMost(filtered.size)
                    }
                }
            }

            Screen5Tab.ITEMS -> {
                item {
                    ResponsiveCompactPair(
                        first = { child ->
                            CompactSearchField(
                                value = state.itemSearch,
                                onValueChange = vm::searchItems,
                                placeholder = "Search items",
                                searching = state.itemLoading && state.itemSearch.isNotBlank(),
                                modifier = child
                            )
                        },
                        second = { child ->
                            AppSingleSelectFilter(
                                label = "Category",
                                selected = state.itemCategoryId,
                                options = listOf(AppFilterOption("", "All Categories")) +
                                    state.data.categories.map { AppFilterOption(it.id, it.name) },
                                resetValue = "",
                                onApply = vm::setItemCategory,
                                title = "Filter Items",
                                modifier = child
                            )
                        }
                    )
                    val selectedCategoryName = state.data.categories.firstOrNull { it.id == state.itemCategoryId }?.name
                    if (selectedCategoryName != null) {
                        Spacer(Modifier.height(AppSpacing.xs))
                        StatusBadge("Category · $selectedCategoryName", BadgeTone.INFO)
                    }
                }

                if (!state.itemLoadError.isNullOrBlank() && state.itemLoaded) {
                    item { InlineStatusMessage(state.itemLoadError, MessageTone.ERROR) }
                }

                when {
                    !state.itemLoaded && !state.itemLoadError.isNullOrBlank() -> item {
                        LoadFailureState(
                            message = state.itemLoadError.orEmpty(),
                            onRetry = vm::retryItems
                        )
                    }
                    state.itemLoading && !state.itemLoaded -> item { LoadingState() }
                    state.itemLoaded && state.itemItems.isEmpty() -> item {
                        EmptyState(
                            "No items found.",
                            variant = when {
                                state.itemSearch.isNotBlank() -> EmptyStateVariant.SEARCH_NO_RESULT
                                state.itemCategoryId.isNotBlank() -> EmptyStateVariant.FILTERED_NO_RESULT
                                else -> EmptyStateVariant.NORMAL
                            }
                        )
                    }
                    else -> {
                        state.itemItems.forEach { item ->
                            item(key = "item-${item.id}") {
                                Screen5ItemCard(
                                    item = item,
                                    canEdit = canEdit,
                                    busy = state.busy,
                                    onEdit = { itemEditor = item },
                                    onView = { viewItem = item },
                                    onDelete = if (canEdit) ({ deleteItem = item }) else null
                                )
                            }
                        }
                    }
                }

                if (state.canLoadMoreItems) {
                    item(key = "item-page-more-${state.itemPage}") {
                        LaunchedEffect(state.itemPage, state.itemItems.size) { vm.loadMoreItems() }
                        LoadingState()
                    }
                } else if (state.itemLoading && state.itemLoaded) {
                    item { LoadingState() }
                }
                if (!state.itemLoadMoreError.isNullOrBlank()) {
                    item(key = "item-page-retry-${state.itemPage}") {
                        InlineRetryMessage(
                            message = state.itemLoadMoreError.orEmpty(),
                            onRetry = vm::retryLoadMoreItems,
                            retryLabel = "Retry loading more"
                        )
                    }
                }
            }

            Screen5Tab.RELATED -> {
                val relationGroups = state.data.relations
                    .groupBy { it.sourceItemId }
                    .mapNotNull { (sourceId, relations) ->
                        state.data.items.firstOrNull { it.id == sourceId }?.let { source ->
                            source to relations.sortedBy { it.displayOrder }
                        }
                    }
                    .sortedBy { (source, _) -> source.itemName.lowercase() }

                if (relatedEditorOpen) {
                    item {
                        AppBackHeader(
                            title = if (sourceItemId.isBlank()) "New Related Group" else "Edit Related Group",
                            onBack = requestCloseRelatedEditor,
                            enabled = !state.busy
                        )
                    }
                    item {
                        Screen5MainItemSection(
                            sourceItem = state.data.items.firstOrNull { it.id == sourceItemId },
                            categories = state.data.categories,
                            activeSourceItems = activeSourceItems,
                            busy = state.busy,
                            onSelect = { sourceItemId = it },
                            onRemove = { sourceItemId = "" }
                        )
                    }
                    item {
                        Screen5RelatedEditor(
                            sourceItemId = sourceItemId,
                            items = state.data.items,
                            categories = state.data.categories,
                            relations = state.data.relations,
                            canEdit = canEdit,
                            busy = state.busy,
                            onDirtyChange = { relatedSelectionDirty = it },
                            onSave = { sourceId, ids ->
                                vm.replaceRelatedItems(sourceId, ids)
                                relatedInitialSourceId = sourceId
                                relatedSelectionDirty = false
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            "Related Groups (${relationGroups.size} ${if (relationGroups.size == 1) "Group" else "Groups"})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (relationGroups.isEmpty()) {
                        item { EmptyState("No related groups found.") }
                    } else {
                        relationGroups.take(relatedVisibleLimit).forEach { (source, relations) ->
                            item(key = "related-group-${source.id}") {
                                Screen5RelatedGroupCard(
                                    sourceItem = source,
                                    relatedCount = relations.size,
                                    canEdit = canEdit,
                                    busy = state.busy,
                                    onView = { relatedViewSourceId = source.id },
                                    onEdit = {
                                        sourceItemId = source.id
                                        relatedInitialSourceId = source.id
                                        relatedSelectionDirty = false
                                        relatedEditorOpen = true
                                    },
                                    onDelete = { relatedDeleteSourceId = source.id }
                                )
                            }
                        }
                        if (relationGroups.size > relatedVisibleLimit) {
                            item(key = "related-group-more-$relatedVisibleLimit") {
                                LaunchedEffect(relationGroups.size, relatedVisibleLimit) {
                                    relatedVisibleLimit = (relatedVisibleLimit + 10).coerceAtMost(relationGroups.size)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    if (showRelatedDiscard) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showRelatedDiscard = false },
            onDiscard = {
                showRelatedDiscard = false
                closeRelatedEditor()
            }
        )
    }

    relatedViewSourceId?.let { sourceId ->
        val source = state.data.items.firstOrNull { it.id == sourceId }
        if (source != null) {
            val relatedIds = state.data.relations
                .filter { it.sourceItemId == sourceId }
                .sortedBy { it.displayOrder }
                .map { it.relatedItemId }
            val byId = state.data.items.associateBy { it.id }
            Screen5RelatedGroupViewSheet(
                sourceItem = source,
                relatedItems = relatedIds.mapNotNull(byId::get),
                onDismiss = { relatedViewSourceId = null }
            )
        } else {
            LaunchedEffect(sourceId) { relatedViewSourceId = null }
        }
    }
    relatedDeleteSourceId?.let { sourceId ->
        val source = state.data.items.firstOrNull { it.id == sourceId }
        AppDestructiveConfirmDialog(
            title = "Delete related group?",
            message = source?.let {
                "Clear all related-item mappings for \"${it.itemName}\"? Item Master records will not be deleted."
            } ?: "Clear this related-item mapping? Item Master records will not be deleted.",
            confirmLabel = "Delete",
            busy = state.busy,
            onConfirm = {
                vm.replaceRelatedItems(sourceId, emptyList())
                relatedDeleteSourceId = null
            },
            onDismiss = { relatedDeleteSourceId = null }
        )
    }

    if (createCategory || categoryEditor != null) {
        Screen5CategorySheet(
            existing = categoryEditor,
            categories = state.data.categories,
            busy = state.busy,
            onDismiss = { createCategory = false; categoryEditor = null },
            onSave = { name, prefix, order, active, publicVisible ->
                vm.saveCategory(categoryEditor?.id, name, prefix, order, active, publicVisible)
                createCategory = false
                categoryEditor = null
            }
        )
    }
    fieldEditor?.let { (categoryId, field) ->
        Screen5FieldSheet(
            existing = field,
            busy = state.busy,
            onDismiss = { fieldEditor = null },
            onSave = { name, type, required, defaultValue, options, publicVisible, active, order ->
                vm.saveField(categoryId, field?.id, name, type, required, defaultValue, options, publicVisible, active, order)
                fieldEditor = null
            }
        )
    }
    if (createItem || itemEditor != null) {
        Screen5ItemSheet(
            existing = itemEditor,
            categories = state.data.categories,
            busy = state.busy,
            onDismiss = { createItem = false; itemEditor = null },
            onUploadImage = vm::uploadItemImage,
            onDiscardUploads = vm::discardItemUploads,
            onSave = { code, name, categoryId, quantity, rent, active, publicVisible, fieldValues, clearFieldIds, imageUrls, cloudinaryAssets ->
                vm.saveItem(
                    itemEditor,
                    code,
                    name,
                    categoryId,
                    quantity,
                    rent,
                    active,
                    publicVisible,
                    fieldValues,
                    clearFieldIds,
                    imageUrls,
                    cloudinaryAssets,
                    onSuccess = {
                        createItem = false
                        itemEditor = null
                    }
                )
            }
        )
    }
    viewItem?.let { item ->
        Screen5ItemViewSheet(
            item = item,
            category = state.data.categories.firstOrNull { it.id == item.categoryId },
            onDismiss = { viewItem = null }
        )
    }
    deleteItem?.let { item ->
        AppDestructiveConfirmDialog(
            title = "Delete item?",
            message = "Delete \"${item.itemName}\"? Only an unused Item can be permanently deleted. If it has booking history, deletion will be blocked and the existing archive rule remains unchanged.",
            confirmLabel = "Delete",
            busy = state.busy,
            onConfirm = {
                vm.deleteItem(item.id)
                deleteItem = null
            },
            onDismiss = { deleteItem = null }
        )
    }

    deleteCategory?.let { category ->
        val linked = category.linkedItemCount
        AlertDialog(
            onDismissRequest = { if (!state.busy) deleteCategory = null },
            title = {
                Text(if (linked > 0) "Category has linked Items" else "Delete category?")
            },
            text = {
                Text(
                    if (linked > 0) {
                        "\"${category.name}\" has $linked linked Item${if (linked == 1) "" else "s"}. " +
                            "Delete those Items first, then delete this category."
                    } else {
                        "Delete \"${category.name}\"? This empty category and its custom-field schema will be permanently deleted."
                    }
                )
            },
            confirmButton = {
                if (linked == 0) {
                    DangerTextButton(
                        text = "Delete",
                        onClick = {
                            vm.deleteCategory(category.id)
                            deleteCategory = null
                        },
                        enabled = !state.busy
                    )
                } else {
                    TextButton(
                        onClick = { deleteCategory = null },
                        enabled = !state.busy
                    ) { Text("Close") }
                }
            },
            dismissButton = {
                if (linked == 0) {
                    TextButton(
                        onClick = { deleteCategory = null },
                        enabled = !state.busy
                    ) { Text("Cancel") }
                }
            }
        )
    }
    deleteField?.let { field ->
        AlertDialog(
            onDismissRequest = { if (!state.busy) deleteField = null },
            title = { Text("Custom field action") },
            text = {
                Text(
                    "\"${field.name}\" is used by ${field.valueCount} Item${if (field.valueCount == 1) "" else "s"}. " +
                        "Deactivate keeps existing Item data. Remove Data & Delete Field removes only this field's values; Items themselves are not deleted."
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = {
                            vm.deactivateField(field)
                            deleteField = null
                        },
                        enabled = !state.busy && field.active
                    ) { Text("Deactivate") }
                    DangerTextButton(
                        text = "Remove Data & Delete Field",
                        onClick = {
                            strongDeleteField = field
                            strongDeleteText = ""
                            deleteField = null
                        },
                        enabled = !state.busy
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteField = null },
                    enabled = !state.busy
                ) { Text("Cancel") }
            }
        )
    }
    strongDeleteField?.let { field ->
        AlertDialog(
            onDismissRequest = {
                if (!state.busy) {
                    strongDeleteField = null
                    strongDeleteText = ""
                }
            },
            title = { Text("Remove field data & delete?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        "This permanently removes only \"${field.name}\" values from ${field.valueCount} affected " +
                            "Item${if (field.valueCount == 1) "" else "s"} and then deletes the custom field. " +
                            "No Item record will be deleted."
                    )
                    AppTextField(
                        value = strongDeleteText,
                        onValueChange = { strongDeleteText = it },
                        label = "Type DELETE FIELD",
                        placeholder = "DELETE FIELD",
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                    )
                }
            },
            confirmButton = {
                DangerTextButton(
                    text = "Delete Field & Data",
                    onClick = {
                        vm.removeFieldDataAndDelete(field.id)
                        strongDeleteField = null
                        strongDeleteText = ""
                    },
                    enabled = !state.busy && strongDeleteText == "DELETE FIELD"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        strongDeleteField = null
                        strongDeleteText = ""
                    },
                    enabled = !state.busy
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Screen5CategoryCard(
    category: Screen5Category,
    expanded: Boolean,
    canEdit: Boolean,
    busy: Boolean,
    onToggleFields: () -> Unit,
    onEdit: () -> Unit,
    onAddField: () -> Unit,
    onEditField: (Screen5CategoryField) -> Unit,
    onDeleteField: (Screen5CategoryField) -> Unit,
    onDelete: () -> Unit
) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(category.codePrefix, color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(if (category.active) "Active" else "Inactive", if (category.active) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
        }
        Spacer(Modifier.height(AppSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            StatusBadge("${category.itemCount} Items", BadgeTone.INFO)
            StatusBadge("${category.fieldCount} Fields", BadgeTone.INFO)
            StatusBadge(if (category.publicVisible) "Public" else "Hidden", BadgeTone.NEUTRAL)
        }
        Spacer(Modifier.height(AppSpacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            SoftActionButton(
                icon = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                label = "Fields",
                onClick = onToggleFields,
                modifier = Modifier.weight(1f),
                tone = ActionTone.INFO
            )
            if (canEdit) {
                SoftActionButton(Icons.Rounded.Edit, "Edit", onClick = onEdit, modifier = Modifier.weight(1f), enabled = !busy, tone = ActionTone.PURPLE)
                SoftActionButton(Icons.Rounded.Delete, "Delete", onClick = onDelete, modifier = Modifier.weight(1f), enabled = !busy, tone = ActionTone.DANGER)
            }
        }
        if (expanded) {
            HorizontalDivider(Modifier.padding(vertical = AppSpacing.sm), color = AppBorder)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Custom Fields", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (canEdit) {
                    CompactNewActionButton(
                        label = "New Custom Field",
                        enabled = !busy,
                        onClick = onAddField
                    )
                }
            }
            if (category.fields.isEmpty()) {
                Text("No custom fields.", color = AppTextMuted, modifier = Modifier.padding(top = AppSpacing.sm))
            } else {
                category.fields.forEach { field ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(field.name, fontWeight = FontWeight.Medium)
                            Text(
                                buildString {
                                    append(field.type.replace('_', ' '))
                                    if (field.required) append(" · Required")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTextMuted
                            )
                        }
                        StatusBadge(if (field.active) "Active" else "Inactive", if (field.active) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
                        if (canEdit) {
                            SoftActionButton(Icons.Rounded.Edit, contentDescription = "Edit field", onClick = { onEditField(field) }, enabled = !busy, tone = ActionTone.PURPLE)
                            SoftActionButton(Icons.Rounded.Delete, contentDescription = "Delete field", onClick = { onDeleteField(field) }, enabled = !busy, tone = ActionTone.DANGER)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen5ItemCard(
    item: Screen5Item,
    canEdit: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onView: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    selectionState: Boolean? = null,
    selectionEnabled: Boolean = true,
    onSelectionToggle: (() -> Unit)? = null,
    onRemoveSelection: (() -> Unit)? = null,
    footerActions: (@Composable RowScope.() -> Unit)? = null
) {
    AppCard(Modifier.fillMaxWidth()) {
        AppItemDisplayRow(
            imageUrl = item.primaryImageUrl,
            imageUrls = item.imageUrls,
            itemName = item.itemName,
            itemCode = item.itemCode,
            categoryName = item.categoryName,
            quantity = item.totalQuantity,
            statusLine = "Available ${item.availableQuantity} · Booked ${item.bookedQuantity} · Given ${item.givenQuantity}",
            trailing = {
                when {
                    onSelectionToggle != null && selectionState != null -> SoftActionButton(
                        icon = if (selectionState) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        label = if (selectionState) "Selected" else "Select",
                        onClick = onSelectionToggle,
                        enabled = selectionEnabled && !busy,
                        tone = if (selectionState) ActionTone.SUCCESS else ActionTone.INFO
                    )
                    onRemoveSelection != null -> SoftActionButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Remove related item",
                        onClick = onRemoveSelection,
                        enabled = !busy,
                        tone = ActionTone.DANGER
                    )
                }
            }
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            StatusBadge("Rent ₹${item.rentAmount}", BadgeTone.INFO)
            StatusBadge(if (item.active) "Active" else "Inactive", if (item.active) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
            StatusBadge(if (item.publicVisible) "Public" else "Hidden", BadgeTone.NEUTRAL)
        }
        if (onView != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                SoftActionButton(
                    icon = Icons.Rounded.Visibility,
                    label = "View",
                    onClick = onView,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.INFO
                )
                if (canEdit) {
                    SoftActionButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.PURPLE
                    )
                    if (onDelete != null) {
                        SoftActionButton(
                            icon = Icons.Rounded.Delete,
                            label = "Delete",
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            tone = ActionTone.DANGER
                        )
                    }
                }
            }
        }
        footerActions?.let { actions ->
            Spacer(Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                content = actions
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5ItemViewSheet(
    item: Screen5Item,
    category: Screen5Category?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text("Item Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
            Screen5ItemCard(
                item = item,
                canEdit = false,
                busy = false,
                onEdit = {}
            )
            AppCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rent", color = AppTextMuted)
                    Text("₹${item.rentAmount}", fontWeight = FontWeight.SemiBold)
                }
            }
            val values = category?.fields.orEmpty()
                .mapNotNull { field ->
                    item.fieldValues[field.id]?.takeIf { it.isNotBlank() }?.let { field to it }
                }
            if (values.isNotEmpty()) {
                Text("Custom Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AppCard(Modifier.fillMaxWidth()) {
                    values.forEachIndexed { index, (field, raw) ->
                        val display = when (field.type) {
                            "YES_NO" -> if (screen5NormalizeValueForType("YES_NO", raw) == "1") "Yes" else "No"
                            "MULTI_SELECT" -> screen5ParseMultiValues(raw).joinToString(", ")
                            else -> raw
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(field.name, modifier = Modifier.weight(1f), color = AppTextMuted)
                            Text(display, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        }
                        if (index != values.lastIndex) {
                            HorizontalDivider(Modifier.padding(vertical = AppSpacing.xs), color = AppBorder)
                        }
                    }
                }
            }
            }
            SecondaryButton("Close", onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

@Composable
private fun Screen5RelatedGroupCard(
    sourceItem: Screen5Item,
    relatedCount: Int,
    canEdit: Boolean,
    busy: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(Modifier.fillMaxWidth()) {
        AppItemDisplayRow(
            imageUrl = sourceItem.primaryImageUrl,
            imageUrls = sourceItem.imageUrls,
            itemName = sourceItem.itemName,
            itemCode = sourceItem.itemCode,
            categoryName = sourceItem.categoryName,
            quantity = sourceItem.totalQuantity,
            statusLine = "$relatedCount Related"
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            SoftActionButton(
                icon = Icons.Rounded.Visibility,
                label = "View",
                onClick = onView,
                modifier = Modifier.weight(1f),
                enabled = !busy,
                tone = ActionTone.INFO
            )
            if (canEdit) {
                SoftActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.PURPLE
                )
                SoftActionButton(
                    icon = Icons.Rounded.Delete,
                    label = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.DANGER
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5RelatedGroupViewSheet(
    sourceItem: Screen5Item,
    relatedItems: List<Screen5Item>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text("Related Group", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
            Text("Main Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Screen5ItemCard(
                item = sourceItem,
                canEdit = false,
                busy = false,
                onEdit = {}
            )
            Text(
                "Related Items (${relatedItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (relatedItems.isEmpty()) {
                EmptyState("No related items.")
            } else {
                relatedItems.forEach { item ->
                    Screen5ItemCard(
                        item = item,
                        canEdit = false,
                        busy = false,
                        onEdit = {}
                    )
                }
            }
            }
            SecondaryButton("Close", onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

@Composable
private fun Screen5RelationSectionHeader(
    title: String,
    helper: String,
    actionLabel: String?,
    busy: Boolean,
    onAction: () -> Unit
) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(helper, style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
            }
            if (!actionLabel.isNullOrBlank()) {
                CompactNewActionButton(
                    label = actionLabel,
                    enabled = !busy,
                    onClick = onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5MainItemSection(
    sourceItem: Screen5Item?,
    categories: List<Screen5Category>,
    activeSourceItems: List<Screen5Item>,
    busy: Boolean,
    onSelect: (String) -> Unit,
    onRemove: () -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Screen5RelationSectionHeader(
            title = "Main Item",
            helper = if (sourceItem == null) "Choose category, then select one item." else "Main Item selected.",
            actionLabel = if (sourceItem == null) "Select" else "Change",
            busy = busy,
            onAction = { pickerOpen = true }
        )
        if (sourceItem != null) {
            Screen5ItemCard(
                item = sourceItem,
                canEdit = false,
                busy = busy,
                onEdit = {},
                onRemoveSelection = onRemove
            )
        }
    }

    if (pickerOpen) {
        Screen5MainItemPickerSheet(
            categories = categories,
            items = activeSourceItems,
            selectedItemId = sourceItem?.id.orEmpty(),
            onDismiss = { pickerOpen = false },
            onSelect = { selectedId ->
                onSelect(selectedId)
                pickerOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5MainItemPickerSheet(
    categories: List<Screen5Category>,
    items: List<Screen5Item>,
    selectedItemId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var categoryId by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val pickerCategories = categories.filter { category -> items.any { it.categoryId == category.id } }
    val visibleItems = if (categoryId.isBlank()) {
        emptyList()
    } else {
        items.filter { item ->
            item.categoryId == categoryId &&
                (search.isBlank() || item.itemName.contains(search, true) || item.itemCode.contains(search, true))
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text("Select Main Item", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Screen5DropdownField(
                label = "Category *",
                value = categoryId,
                options = pickerCategories.map { it.id to it.name },
                prompt = "Select Category",
                leadingIcon = Icons.Rounded.Category,
                onSelected = {
                    categoryId = it
                    search = ""
                }
            )
            if (categoryId.isNotBlank()) {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search items",
                    modifier = Modifier.fillMaxWidth()
                )
                if (visibleItems.isEmpty()) {
                    EmptyState("No items found in this category.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        visibleItems.forEach { item ->
                            Screen5ItemCard(
                                item = item,
                                canEdit = false,
                                busy = false,
                                onEdit = {},
                                selectionState = item.id == selectedItemId,
                                onSelectionToggle = { onSelect(item.id) }
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Select a category to view items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
            }
            SecondaryButton("Cancel", onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5RelatedEditor(
    sourceItemId: String,
    items: List<Screen5Item>,
    categories: List<Screen5Category>,
    relations: List<Screen5RelatedItem>,
    canEdit: Boolean,
    busy: Boolean,
    onDirtyChange: (Boolean) -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    val existing = relations.filter { it.sourceItemId == sourceItemId }
        .sortedBy { it.displayOrder }
        .map { it.relatedItemId }
    val signature = existing.joinToString("|")
    var selectedIds by rememberSaveable(sourceItemId, signature, stateSaver = Screen5StringListSaver) { mutableStateOf(existing.distinct()) }
    var pickerOpen by remember(sourceItemId) { mutableStateOf(false) }
    val byId = items.associateBy { it.id }
    val selectedItems = selectedIds.mapNotNull(byId::get)
    LaunchedEffect(sourceItemId, signature, selectedIds) {
        onDirtyChange(selectedIds != existing.distinct())
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Screen5RelationSectionHeader(
            title = "Related Items",
            helper = when {
                sourceItemId.isBlank() -> "Select Main Item first."
                selectedIds.isEmpty() -> "No related items selected."
                else -> "${selectedIds.size} item${if (selectedIds.size == 1) "" else "s"} selected"
            },
            actionLabel = if (canEdit && sourceItemId.isNotBlank()) "Add" else null,
            busy = busy,
            onAction = { pickerOpen = true }
        )

        if (sourceItemId.isNotBlank()) {
            selectedItems.forEach { item ->
                Screen5ItemCard(
                    item = item,
                    canEdit = false,
                    busy = busy,
                    onEdit = {},
                    onRemoveSelection = if (canEdit) {
                        { selectedIds = selectedIds.filterNot { it == item.id } }
                    } else {
                        null
                    }
                )
            }
        }

        if (canEdit && sourceItemId.isNotBlank()) {
            PrimaryButton(
                text = "Save Related Items",
                onClick = { onSave(sourceItemId, selectedIds) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )
        }
    }

    if (pickerOpen && sourceItemId.isNotBlank()) {
        Screen5RelatedItemsPickerSheet(
            sourceItemId = sourceItemId,
            categories = categories,
            items = items,
            initialSelectedIds = selectedIds,
            onDismiss = { pickerOpen = false },
            onApply = { chosen ->
                selectedIds = chosen
                pickerOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5RelatedItemsPickerSheet(
    sourceItemId: String,
    categories: List<Screen5Category>,
    items: List<Screen5Item>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {
    val initialSignature = initialSelectedIds.joinToString("|")
    var selectedIds by rememberSaveable(sourceItemId, initialSignature, stateSaver = Screen5StringListSaver) { mutableStateOf(initialSelectedIds.distinct()) }
    var categoryId by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val pickerCategories = categories.filter { category ->
        items.any { item ->
            item.categoryId == category.id &&
                item.id != sourceItemId &&
                !item.archived &&
                (item.id in selectedIds || (item.active && item.availableQuantity > 0))
        }
    }
    val visibleItems = if (categoryId.isBlank()) {
        emptyList()
    } else {
        items.filter { item ->
            item.categoryId == categoryId &&
                item.id != sourceItemId &&
                !item.archived &&
                (item.id in selectedIds || (item.active && item.availableQuantity > 0)) &&
                (search.isBlank() || item.itemName.contains(search, true) || item.itemCode.contains(search, true))
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Add Related Items", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${selectedIds.size} item${if (selectedIds.size == 1) "" else "s"} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextMuted
                    )
                }
            }
            Screen5DropdownField(
                label = "Category",
                value = categoryId,
                options = pickerCategories.map { it.id to it.name },
                prompt = "Select Category",
                leadingIcon = Icons.Rounded.Category,
                onSelected = {
                    categoryId = it
                    search = ""
                }
            )
            if (categoryId.isNotBlank()) {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search items",
                    modifier = Modifier.fillMaxWidth()
                )
                if (visibleItems.isEmpty()) {
                    EmptyState("No eligible items found in this category.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        visibleItems.forEach { item ->
                            val selected = item.id in selectedIds
                            Screen5ItemCard(
                                item = item,
                                canEdit = false,
                                busy = false,
                                onEdit = {},
                                selectionState = selected,
                                selectionEnabled = selected || (item.active && item.availableQuantity > 0),
                                onSelectionToggle = {
                                    selectedIds = if (selected) {
                                        selectedIds.filterNot { it == item.id }
                                    } else {
                                        selectedIds + item.id
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Select a category to view items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton("Cancel", onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Add Selected",
                    onClick = { onApply(selectedIds) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(AppSpacing.md))
        }
    }
}

@Composable
private fun Screen5DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    prompt: String,
    leadingIcon: ImageVector,
    onSelected: (String) -> Unit,
    required: Boolean = false,
    enabled: Boolean = true
) {
    AppSelectField(
        label = label,
        selected = value,
        options = options.map { (optionValue, optionLabel) -> AppFilterOption(optionValue, optionLabel) },
        onSelected = onSelected,
        prompt = prompt,
        enabled = enabled,
        required = required,
        icon = leadingIcon
    )
}

@Composable
private fun Screen5MultiSelectField(
    label: String,
    value: String,
    options: List<String>,
    prompt: String,
    leadingIcon: ImageVector,
    onValueChange: (String) -> Unit,
    required: Boolean = false,
    enabled: Boolean = true
) {
    val selected = screen5ParseMultiValues(value).toSet()
    AppMultiSelectField(
        label = label,
        selected = selected,
        options = options.map { AppFilterOption(it, it) },
        onSelected = { next -> onValueChange(options.filter { it in next }.joinToString(",")) },
        prompt = prompt,
        enabled = enabled,
        required = required,
        icon = leadingIcon
    )
}

@Composable
private fun Screen5DateField(
    label: String,
    value: String,
    prompt: String,
    onValueChange: (String) -> Unit,
    required: Boolean = false,
    enabled: Boolean = true
) {
    val showError = required && value.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        AppDatePickerField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            allowClear = !required,
            placeholder = prompt
        )
        if (showError) {
            Text("Required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun screen5ParseMultiValues(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptyList()
    if (trimmed.startsWith("[")) {
        return try {
            val array = JSONArray(trimmed)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinct()
        } catch (_: Throwable) {
            trimmed.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        }
    }
    return trimmed.split(',').map(String::trim).filter(String::isNotBlank).distinct()
}

private fun screen5NormalizeValueForType(type: String, raw: String, options: List<String> = emptyList()): String {
    val trimmed = raw.trim()
    return when (type) {
        "MULTI_SELECT" -> screen5ParseMultiValues(trimmed).filter { it in options }.joinToString(",")
        "YES_NO" -> when {
            trimmed == "1" || trimmed.equals("yes", true) || trimmed.equals("true", true) -> "1"
            trimmed == "0" || trimmed.equals("no", true) || trimmed.equals("false", true) -> "0"
            else -> ""
        }
        else -> trimmed
    }
}

private fun screen5OptionalValueValid(type: String, raw: String, options: List<String>): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return true
    return when (type) {
        "TEXT" -> true
        "NUMBER" -> trimmed.toDoubleOrNull() != null
        "DROPDOWN" -> trimmed in options
        "MULTI_SELECT" -> {
            val selected = screen5ParseMultiValues(trimmed)
            selected.isNotEmpty() && selected.all { it in options }
        }
        "YES_NO" -> screen5NormalizeValueForType("YES_NO", trimmed).isNotBlank()
        "DATE" -> Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)
        else -> true
    }
}

private fun screen5FieldHasValue(field: Screen5CategoryField, raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return !field.required
    return when (field.type) {
        "TEXT" -> true
        "NUMBER" -> trimmed.toDoubleOrNull() != null
        "DROPDOWN" -> trimmed in field.options
        "MULTI_SELECT" -> {
            val selected = screen5ParseMultiValues(trimmed)
            selected.isNotEmpty() && selected.all { it in field.options }
        }
        "YES_NO" -> screen5NormalizeValueForType("YES_NO", trimmed).isNotBlank()
        "DATE" -> Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)
        else -> true
    }
}

private fun screen5NormalizeFieldValues(
    fields: List<Screen5CategoryField>,
    values: Map<String, String>
): Map<String, String> = buildMap {
    fields.forEach { field ->
        put(
            field.id,
            screen5NormalizeValueForType(field.type, values[field.id].orEmpty(), field.options)
        )
    }
}

@Composable
private fun Screen5DefaultValueInput(
    type: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    when (type) {
        "TEXT" -> AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Default Value",
            placeholder = "Enter default value",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) }
        )
        "NUMBER" -> {
            val invalidNumber = value.isNotBlank() && value.toDoubleOrNull() == null
            AppTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }) },
                label = "Default Value",
                placeholder = "Enter default number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                isError = invalidNumber,
                supportingText = if (invalidNumber) "Enter a valid number." else null
            )
        }
        "DROPDOWN" -> Screen5DropdownField(
            label = "Default Value",
            value = value,
            options = options.map { it to it },
            prompt = "Select Option",
            leadingIcon = Icons.Rounded.FormatListBulleted,
            onSelected = onValueChange
        )
        "MULTI_SELECT" -> Screen5MultiSelectField(
            label = "Default Value",
            value = value,
            options = options,
            prompt = "Select Options",
            leadingIcon = Icons.Rounded.FormatListBulleted,
            onValueChange = onValueChange
        )
        "YES_NO" -> Screen5DropdownField(
            label = "Default Value",
            value = screen5NormalizeValueForType("YES_NO", value),
            options = listOf("1" to "Yes", "0" to "No"),
            prompt = "Select Yes or No",
            leadingIcon = Icons.Rounded.ToggleOn,
            onSelected = onValueChange
        )
        "DATE" -> Screen5DateField(
            label = "Default Value",
            value = value,
            prompt = "Select Date",
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun Screen5DynamicFieldInput(
    field: Screen5CategoryField,
    value: String,
    onValueChange: (String) -> Unit,
    allowControlledClear: Boolean = false,
    controlledClear: Boolean = false,
    onClear: () -> Unit = {},
    onUndoClear: () -> Unit = {}
) {
    val label = field.name + if (field.required) " *" else ""
    val effectiveRequired = field.required && !controlledClear
    val displayValue = if (controlledClear) "" else value

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        when (field.type) {
            "TEXT" -> AppTextField(
                value = displayValue,
                onValueChange = onValueChange,
                label = label,
                placeholder = "Enter value",
                leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                isError = effectiveRequired && displayValue.isBlank(),
                supportingText = if (effectiveRequired && displayValue.isBlank()) "Required" else null
            )
            "NUMBER" -> {
                val invalidNumber = displayValue.isNotBlank() && displayValue.toDoubleOrNull() == null
                AppTextField(
                    value = displayValue,
                    onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }) },
                    label = label,
                    placeholder = "Enter number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                    isError = invalidNumber || (effectiveRequired && displayValue.isBlank()),
                    supportingText = when {
                        invalidNumber -> "Enter a valid number."
                        effectiveRequired && displayValue.isBlank() -> "Required"
                        else -> null
                    }
                )
            }
            "DROPDOWN" -> Screen5DropdownField(
                label = label,
                value = displayValue,
                options = field.options.map { it to it },
                prompt = "Select Option",
                leadingIcon = Icons.Rounded.FormatListBulleted,
                onSelected = onValueChange,
                required = effectiveRequired
            )
            "MULTI_SELECT" -> Screen5MultiSelectField(
                label = label,
                value = displayValue,
                options = field.options,
                prompt = "Select Options",
                leadingIcon = Icons.Rounded.FormatListBulleted,
                onValueChange = onValueChange,
                required = effectiveRequired
            )
            "YES_NO" -> Screen5DropdownField(
                label = label,
                value = screen5NormalizeValueForType("YES_NO", displayValue),
                options = listOf("1" to "Yes", "0" to "No"),
                prompt = "Select Yes or No",
                leadingIcon = Icons.Rounded.ToggleOn,
                onSelected = onValueChange,
                required = effectiveRequired
            )
            "DATE" -> Screen5DateField(
                label = label,
                value = displayValue,
                prompt = "Select Date",
                onValueChange = onValueChange,
                required = effectiveRequired
            )
            else -> AppTextField(
                value = displayValue,
                onValueChange = onValueChange,
                label = label,
                placeholder = "Enter value",
                leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) }
            )
        }

        if (allowControlledClear) {
            if (controlledClear) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusBadge("Value will be removed", BadgeTone.WARNING)
                    TextButton(onClick = onUndoClear) { Text("Undo Clear") }
                }
            } else if (value.isNotBlank()) {
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(AppSpacing.xxs))
                    Text("Clear Value")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5CategorySheet(
    existing: Screen5Category?,
    categories: List<Screen5Category>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Boolean, Boolean) -> Unit
) {
    val nextDisplayOrder = remember(categories) { (categories.maxOfOrNull { it.displayOrder } ?: 0) + 1 }
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var prefix by rememberSaveable(existing?.id) { mutableStateOf(existing?.codePrefix.orEmpty()) }
    var order by rememberSaveable(existing?.id, nextDisplayOrder) {
        mutableStateOf(existing?.displayOrder?.toString() ?: nextDisplayOrder.toString())
    }
    var active by rememberSaveable(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var publicVisible by rememberSaveable(existing?.id) { mutableStateOf(existing?.publicVisible ?: true) }
    var nameTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var prefixTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var orderTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val orderNumber = order.toIntOrNull()
    val duplicateCategory = orderNumber?.let { selectedOrder ->
        categories.firstOrNull { it.id != existing?.id && it.displayOrder == selectedOrder }
    }
    val duplicateMessage = duplicateCategory?.let {
        "Order $orderNumber is already used by \"${it.name}\"."
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dirty =
        name != existing?.name.orEmpty() ||
        prefix != existing?.codePrefix.orEmpty() ||
        order != (existing?.displayOrder?.toString() ?: nextDisplayOrder.toString()) ||
        active != (existing?.active ?: true) ||
        publicVisible != (existing?.publicVisible ?: true)
    var showDiscardChanges by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!busy) {
            if (dirty) showDiscardChanges = true else onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = requestDismiss, sheetState = sheetState) {
        Screen5SheetFrame(
            title = if (existing == null) "Add Category" else "Edit Category",
            busy = busy,
            onDismiss = requestDismiss,
            onSave = { onSave(name, prefix, orderNumber ?: 0, active, publicVisible) },
            saveEnabled = name.trim().isNotBlank() && prefix.trim().isNotBlank() && orderNumber != null && duplicateCategory == null
        ) {
            AppTextField(
                value = name,
                onValueChange = {
                    nameTouched = true
                    name = it
                },
                label = "Category Name *",
                placeholder = "Enter category name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null) },
                isError = nameTouched && name.trim().isBlank(),
                supportingText = if (nameTouched && name.trim().isBlank()) "Category name is required." else null
            )
            AppTextField(
                value = prefix,
                onValueChange = {
                    prefixTouched = true
                    prefix = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(8)
                },
                label = "Code Prefix *",
                placeholder = "Enter code prefix",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Rounded.Tag, contentDescription = null) },
                isError = prefixTouched && prefix.trim().isBlank(),
                supportingText = if (prefixTouched && prefix.trim().isBlank()) "Code prefix is required." else null
            )
            AppTextField(
                value = order,
                onValueChange = {
                    orderTouched = true
                    order = it.filter(Char::isDigit)
                },
                label = "Website Display Order *",
                placeholder = "Enter display order",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null) },
                supportingText = when {
                    orderTouched && orderNumber == null -> "Display order is required."
                    duplicateMessage != null -> duplicateMessage
                    else -> null
                },
                isError = (orderTouched && orderNumber == null) || duplicateCategory != null
            )
            Screen5SwitchRow("Active", active) { active = it }
            Screen5SwitchRow("Show on Public Website", publicVisible) { publicVisible = it }
        }
    }
    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                onDismiss()
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5FieldSheet(
    existing: Screen5CategoryField?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String, List<String>, Boolean, Boolean, Int) -> Unit
) {
    val types = listOf("TEXT", "NUMBER", "DROPDOWN", "MULTI_SELECT", "YES_NO", "DATE")
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.type.orEmpty()) }
    var required by rememberSaveable(existing?.id) { mutableStateOf(existing?.required ?: false) }
    var defaultValue by rememberSaveable(existing?.id) { mutableStateOf(existing?.defaultValue.orEmpty()) }
    var optionsText by rememberSaveable(existing?.id) { mutableStateOf(existing?.options?.joinToString(", ").orEmpty()) }
    var publicVisible by rememberSaveable(existing?.id) { mutableStateOf(existing?.publicVisible ?: true) }
    var active by rememberSaveable(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var order by rememberSaveable(existing?.id) { mutableStateOf(existing?.displayOrder?.toString() ?: "0") }
    var nameTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var optionsTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var orderTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val options = optionsText.split(',').map(String::trim).filter(String::isNotBlank).distinct()
    val orderNumber = order.toIntOrNull()
    val optionsRequired = type in listOf("DROPDOWN", "MULTI_SELECT")
    val defaultValueValid = screen5OptionalValueValid(type, defaultValue, options)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dirty =
        name != existing?.name.orEmpty() ||
        type != existing?.type.orEmpty() ||
        required != (existing?.required ?: false) ||
        defaultValue != existing?.defaultValue.orEmpty() ||
        optionsText != existing?.options?.joinToString(", ").orEmpty() ||
        publicVisible != (existing?.publicVisible ?: true) ||
        active != (existing?.active ?: true) ||
        order != (existing?.displayOrder?.toString() ?: "0")
    var showDiscardChanges by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!busy) {
            if (dirty) showDiscardChanges = true else onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = requestDismiss, sheetState = sheetState) {
        Screen5SheetFrame(
            title = if (existing == null) "Add Custom Field" else "Edit Custom Field",
            busy = busy,
            onDismiss = requestDismiss,
            onSave = {
                onSave(
                    name,
                    type,
                    required,
                    screen5NormalizeValueForType(type, defaultValue, options),
                    options,
                    publicVisible,
                    active,
                    orderNumber ?: 0
                )
            },
            saveEnabled = name.trim().isNotBlank() &&
                type.isNotBlank() &&
                (!optionsRequired || options.isNotEmpty()) &&
                orderNumber != null &&
                defaultValueValid,
            expanded = true
        ) {
            AppTextField(
                value = name,
                onValueChange = {
                    nameTouched = true
                    name = it
                },
                label = "Field Name *",
                placeholder = "Enter field name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                isError = nameTouched && name.trim().isBlank(),
                supportingText = if (nameTouched && name.trim().isBlank()) "Field name is required." else null
            )
            Screen5DropdownField(
                label = "Field Type *",
                value = type,
                options = types.map { it to it.replace('_', ' ') },
                prompt = "Select Field Type",
                leadingIcon = Icons.Rounded.Tune,
                onSelected = {
                    if (type != it) defaultValue = ""
                    type = it
                },
                required = true
            )
            if (type == "DROPDOWN" || type == "MULTI_SELECT") {
                AppTextField(
                    value = optionsText,
                    onValueChange = {
                        optionsTouched = true
                        optionsText = it
                    },
                    label = "Options *",
                    placeholder = "Enter options separated by commas",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    leadingIcon = { Icon(Icons.Rounded.FormatListBulleted, contentDescription = null) },
                    isError = optionsTouched && options.isEmpty(),
                    supportingText = if (optionsTouched && options.isEmpty()) "Enter at least one option." else null
                )
            }
            if (type.isNotBlank()) {
                Screen5DefaultValueInput(
                    type = type,
                    value = defaultValue,
                    options = options,
                    onValueChange = { defaultValue = it }
                )
                if (!defaultValueValid && type == "DATE") {
                    Text(
                        "Select a valid default date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            AppTextField(
                value = order,
                onValueChange = {
                    orderTouched = true
                    order = it.filter(Char::isDigit)
                },
                label = "Display Order *",
                placeholder = "Enter display order",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null) },
                isError = orderTouched && orderNumber == null,
                supportingText = if (orderTouched && orderNumber == null) "Display order is required." else null
            )
            Screen5SwitchRow("Required", required) { required = it }
            Screen5SwitchRow("Public Website", publicVisible) { publicVisible = it }
            Screen5SwitchRow("Active", active) { active = it }
        }
    }
    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                onDismiss()
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen5ItemSheet(
    existing: Screen5Item?,
    categories: List<Screen5Category>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onUploadImage: suspend (Uri) -> Screen5UploadedAsset,
    onDiscardUploads: suspend (List<Screen5UploadedAsset>) -> Unit,
    onSave: (
        String,
        String,
        String,
        Int,
        Int,
        Boolean,
        Boolean,
        Map<String, String>,
        Set<String>,
        List<String>,
        List<Screen5UploadedAsset>
    ) -> Unit
) {
    var code by rememberSaveable(existing?.id) { mutableStateOf(existing?.itemCode.orEmpty()) }
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.itemName.orEmpty()) }
    var categoryId by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.categoryId.orEmpty())
    }
    var quantity by rememberSaveable(existing?.id) { mutableStateOf(existing?.totalQuantity?.toString().orEmpty()) }
    var rent by rememberSaveable(existing?.id) { mutableStateOf(existing?.rentAmount?.toString() ?: "0") }
    var codeTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var nameTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var quantityTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var rentTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var active by rememberSaveable(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var publicVisible by rememberSaveable(existing?.id) { mutableStateOf(existing?.publicVisible ?: true) }
    var fieldValues by rememberSaveable(existing?.id, categoryId, stateSaver = Screen5StringMapSaver) {
        mutableStateOf(existing?.fieldValues?.takeIf { existing.categoryId == categoryId }.orEmpty())
    }
    var clearFieldIds by rememberSaveable(existing?.id, categoryId, stateSaver = Screen5StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var photos by rememberSaveable(existing?.id, stateSaver = Screen5PhotoListSaver) {
        mutableStateOf(
            existing?.images
                ?.sortedWith(compareByDescending<Screen5ItemImage> { it.primary }.thenBy { it.displayOrder })
                ?.map { Screen5UploadedAsset(it.url) }
                .orEmpty()
        )
    }
    var uploading by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var uploadStatus by rememberSaveable(existing?.id) { mutableStateOf("") }
    var uploadError by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val category = categories.firstOrNull { it.id == categoryId }
    val fields = category?.fields?.filter { it.active }.orEmpty()
    val quantityNumber = quantity.toIntOrNull()
    val rentNumber = rent.toIntOrNull()
    val requiredFieldsValid = fields.all { field ->
        !field.required ||
            (existing != null && field.id in clearFieldIds) ||
            screen5FieldHasValue(field, fieldValues[field.id].orEmpty())
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialPhotoUrls = existing?.images
        ?.sortedWith(compareByDescending<Screen5ItemImage> { it.primary }.thenBy { it.displayOrder })
        ?.map { it.url }
        .orEmpty()
    val initialFieldValues = existing?.fieldValues.orEmpty()
    val dirty =
        code != existing?.itemCode.orEmpty() ||
        name != existing?.itemName.orEmpty() ||
        categoryId != existing?.categoryId.orEmpty() ||
        quantity != existing?.totalQuantity?.toString().orEmpty() ||
        rent != (existing?.rentAmount?.toString() ?: "0") ||
        active != (existing?.active ?: true) ||
        publicVisible != (existing?.publicVisible ?: true) ||
        fieldValues != initialFieldValues ||
        clearFieldIds.isNotEmpty() ||
        photos.map { it.url } != initialPhotoUrls
    var showDiscardChanges by rememberSaveable(existing?.id) { mutableStateOf(false) }

    val performDismiss: () -> Unit = {
        if (!busy && !uploading) {
            val pendingUploads = photos.filter { it.publicId.isNotBlank() }
            if (pendingUploads.isNotEmpty()) {
                scope.launch { onDiscardUploads(pendingUploads) }
            }
            onDismiss()
        }
    }
    val requestDismiss: () -> Unit = {
        if (!busy && !uploading) {
            if (dirty) showDiscardChanges = true else performDismiss()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { selected ->
        val unique = selected.distinct()
        val remaining = (MAX_ITEM_PHOTOS - photos.size).coerceAtLeast(0)
        val accepted = unique.take(remaining)
        uploadError = when {
            unique.isNotEmpty() && remaining == 0 -> "Maximum 8 item photos are allowed."
            unique.size > accepted.size -> "Only ${accepted.size} photo(s) can be added because the 8-photo limit was reached."
            else -> null
        }
        if (accepted.isNotEmpty()) {
            scope.launch {
                uploading = true
                try {
                    accepted.forEachIndexed { index, uri ->
                        uploadStatus = "Uploading ${index + 1}/${accepted.size}..."
                        val uploaded = onUploadImage(uri)
                        photos = (photos + uploaded).distinctBy { it.url }.take(MAX_ITEM_PHOTOS)
                    }
                } catch (error: Throwable) {
                    uploadError = error.message?.takeIf { it.isNotBlank() } ?: "Image upload failed."
                } finally {
                    uploadStatus = ""
                    uploading = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState
    ) {
        Screen5SheetFrame(
            title = if (existing == null) "Add Item" else "Edit Item",
            busy = busy || uploading,
            onDismiss = requestDismiss,
            onSave = {
                onSave(
                    code,
                    name,
                    categoryId,
                    quantityNumber ?: 0,
                    rentNumber ?: 0,
                    active,
                    publicVisible,
                    screen5NormalizeFieldValues(fields, fieldValues)
                        .filterKeys { it !in clearFieldIds },
                    clearFieldIds,
                    photos.map { it.url },
                    photos.filter { it.publicId.isNotBlank() }
                )
            },
            saveEnabled = code.trim().isNotBlank() &&
                name.trim().isNotBlank() &&
                categoryId.isNotBlank() &&
                quantityNumber != null &&
                rentNumber != null &&
                requiredFieldsValid &&
                !uploading,
            expanded = true
        ) {
            AppTextField(
                value = code,
                onValueChange = {
                    codeTouched = true
                    code = it.uppercase().replace(' ', '-')
                },
                label = "Item Code *",
                placeholder = "Enter item code",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Rounded.Tag, contentDescription = null) },
                isError = codeTouched && code.trim().isBlank(),
                supportingText = if (codeTouched && code.trim().isBlank()) "Item code is required." else null
            )
            AppTextField(
                value = name,
                onValueChange = {
                    nameTouched = true
                    name = it
                },
                label = "Item Name *",
                placeholder = "Enter item name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                isError = nameTouched && name.trim().isBlank(),
                supportingText = if (nameTouched && name.trim().isBlank()) "Item name is required." else null
            )
            Screen5DropdownField(
                label = "Category",
                value = categoryId,
                options = categories
                    .filter { it.active || it.id == existing?.categoryId }
                    .map { it.id to it.name },
                prompt = "Select Category",
                leadingIcon = Icons.Rounded.Category,
                onSelected = { selectedId ->
                    categoryId = selectedId
                    fieldValues = if (existing?.categoryId == selectedId) existing.fieldValues else emptyMap()
                    clearFieldIds = emptySet()
                },
                required = true
            )
            AppTextField(
                value = quantity,
                onValueChange = {
                    quantityTouched = true
                    quantity = it.filter(Char::isDigit)
                },
                label = "Total Quantity *",
                placeholder = "Enter quantity",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                isError = quantityTouched && quantityNumber == null,
                supportingText = when {
                    !quantityTouched || quantityNumber != null -> null
                    quantity.isBlank() -> "Quantity is required. Enter 0 if there is currently no stock."
                    else -> "Enter a valid whole quantity."
                },
                selectAllOnFocus = true
            )
            AppTextField(
                value = rent,
                onValueChange = {
                    rentTouched = true
                    rent = it.filter(Char::isDigit)
                },
                label = "Rent (₹) *",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (fields.isEmpty()) ImeAction.Done else ImeAction.Next
                ),
                leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                isError = rentTouched && rentNumber == null,
                supportingText = if (rentTouched && rentNumber == null) "Enter a valid Rent amount." else null,
                selectAllOnFocus = true
            )
            fields.forEach { field ->
                Screen5DynamicFieldInput(
                    field = field,
                    value = fieldValues[field.id].orEmpty(),
                    onValueChange = { value ->
                        clearFieldIds = clearFieldIds - field.id
                        fieldValues = fieldValues + (field.id to value)
                    },
                    allowControlledClear = existing != null && existing.categoryId == categoryId,
                    controlledClear = field.id in clearFieldIds,
                    onClear = { clearFieldIds = clearFieldIds + field.id },
                    onUndoClear = { clearFieldIds = clearFieldIds - field.id }
                )
            }
            Screen5SwitchRow("Active", active) { active = it }
            Screen5SwitchRow("Show on Public Website", publicVisible) { publicVisible = it }

            HorizontalDivider(color = AppBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Item Photos", fontWeight = FontWeight.SemiBold)
                    Text("${photos.size}/8 photos", style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
                }
                SecondaryButton(
                    text = if (uploading) "Uploading..." else "Choose Photos",
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !busy && !uploading && photos.size < MAX_ITEM_PHOTOS,
                    icon = {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            if (uploading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(uploadStatus.ifBlank { "Uploading photo..." }, style = MaterialTheme.typography.bodySmall)
                }
            }
            uploadError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (photos.isEmpty()) {
                Text("No photos selected.", style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
            } else {
                photos.forEachIndexed { index, photo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        RoundedItemImage(
                            model = photo.url,
                            contentDescription = "Item photo ${index + 1}",
                            modifier = Modifier.size(72.dp),
                            galleryUrls = photos.map { it.url },
                            initialIndex = index
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                        ) {
                            if (index == 0) {
                                StatusBadge("Primary", BadgeTone.SUCCESS)
                            } else {
                                TextButton(
                                    onClick = {
                                        val picked = photos[index]
                                        photos = listOf(picked) + photos.filterIndexed { itemIndex, _ -> itemIndex != index }
                                    },
                                    enabled = !busy && !uploading
                                ) {
                                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Make Primary")
                                }
                            }
                        }
                        SoftActionButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Remove photo",
                            onClick = {
                                val removed = photos[index]
                                photos = photos.filterIndexed { itemIndex, _ -> itemIndex != index }
                                if (removed.publicId.isNotBlank()) {
                                    scope.launch { onDiscardUploads(listOf(removed)) }
                                }
                            },
                            enabled = !busy && !uploading,
                            tone = ActionTone.DANGER
                        )
                    }
                }
            }
            Text(
                "Up to 8 images · max 8 MB each · first image is Primary.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
        }
    }
    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                performDismiss()
            }
        )
    }

}

private const val MAX_ITEM_PHOTOS = 8

@Composable
private fun Screen5SheetFrame(
    title: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    expanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    AppFormSheetScaffold(
        title = title,
        busy = busy,
        onDismiss = onDismiss,
        onSave = onSave,
        saveEnabled = saveEnabled,
        expanded = expanded,
        content = content
    )
}

@Composable
private fun Screen5SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
