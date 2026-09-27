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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionItem
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppUnsavedChangesDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDatePickerField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemListDivider
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSingleSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppScreenTitle
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyStateVariant
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LabeledSectionCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import java.time.LocalDate
import java.time.ZoneId

/** Current Screen 4 booking workspace: compact New/Edit wizard + hardened lifecycle details. */
@Composable
fun BookingWorkspaceScreen5(
    viewModel: BookingLifecycleViewModel,
    currentUser: com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser,
    branding: com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding,
    businessDate: String,
    composeWhatsApp: (String, String?, String?, (String, String) -> Unit) -> Unit,
    contactBusy: Boolean = false,
    canManageCustomers: Boolean = true,
    canPickup: Boolean = true,
    canReturn: Boolean = true,
    onBilling: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = viewModel.editorStartsInEditMode && viewModel.detailState.data == null) {
        viewModel.backFromEditor()
    }

    when {
        viewModel.editorStartsInEditMode && viewModel.detailState.data == null -> {
            Column(
                modifier.fillMaxSize().padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                MainScreenDateRow(businessDate)
                AppBackHeader(
                    title = "Edit Booking",
                    onBack = viewModel::backFromEditor,
                    enabled = !viewModel.actionBusy
                )
                when {
                    viewModel.detailState.loading -> LoadingState()
                    !viewModel.detailState.error.isNullOrBlank() -> LoadFailureState(
                        message = viewModel.detailState.error.orEmpty(),
                        onRetry = viewModel::refreshEditor
                    )
                    else -> LoadFailureState(
                        message = "Unable to load booking details.",
                        onRetry = viewModel::refreshEditor
                    )
                }
            }
        }
        viewModel.editorStartsInEditMode -> NewBookingWizard5(
            viewModel = viewModel,
            businessDate = businessDate,
            editMode = true,
            canManageCustomers = canManageCustomers,
            canPickup = canPickup,
            modifier = modifier
        )
        viewModel.detailState.loading || viewModel.detailState.data != null -> BookingDetailsScreen6(
            viewModel = viewModel,
            currentUser = currentUser,
            branding = branding,
            businessDate = businessDate,
            composeWhatsApp = composeWhatsApp,
            contactBusy = contactBusy,
            canContactCustomers = canManageCustomers,
            canPickup = canPickup,
            canReturn = canReturn,
            onBilling = onBilling,
            modifier = modifier
        )
        else -> NewBookingWizard5(
            viewModel = viewModel,
            businessDate = businessDate,
            editMode = false,
            canManageCustomers = canManageCustomers,
            canPickup = canPickup,
            modifier = modifier
        )
    }
}

@Composable
private fun NewBookingWizard5(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    editMode: Boolean,
    canManageCustomers: Boolean,
    canPickup: Boolean,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable(editMode) { mutableIntStateOf(1) }
    var showCustomerForm by rememberSaveable(editMode) { mutableStateOf(false) }
    var customerPrefillName by rememberSaveable(editMode) { mutableStateOf("") }
    var customerPrefillMobile by rememberSaveable(editMode) { mutableStateOf("") }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var showDiscardChanges by rememberSaveable(editMode) { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        viewModel.clearActionFeedback()
        if (viewModel.hasUnsavedEditorChanges()) showDiscardChanges = true
        else viewModel.backFromEditor()
    }

    BackHandler(enabled = !viewModel.actionBusy && !viewModel.newCustomerBusy && !viewModel.availabilityLoading) {
        viewModel.clearActionFeedback()
        if (step > 1) step -= 1 else requestExit()
    }

    LaunchedEffect(viewModel.actionBusy) {
        if (!viewModel.actionBusy) busyAction = null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = AppSpacing.md),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = AppSpacing.xs, bottom = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        item { MainScreenDateRow(businessDate) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppScreenTitle(
                    title = if (editMode) "Edit Booking" else "New Booking",
                    modifier = Modifier.weight(1f)
                )
                if (step == 1 && canManageCustomers) {
                    CompactNewActionButton(
                        label = "New Customer",
                        enabled = !viewModel.newCustomerBusy && !viewModel.actionBusy,
                        onClick = {
                            val (name, mobile) = customerPrefillFromQuery5(viewModel.customerSearch)
                            customerPrefillName = name
                            customerPrefillMobile = mobile
                            viewModel.clearNewCustomerFeedback()
                            showCustomerForm = true
                        }
                    )
                }
            }
        }
        item { BookingStepper5(step) }

        when (step) {
            1 -> item {
                BookingCustomerStep5(
                    viewModel = viewModel,
                    canManageCustomers = canManageCustomers,
                    onQuickCreate = { name, mobile ->
                        customerPrefillName = name
                        customerPrefillMobile = mobile
                        viewModel.clearNewCustomerFeedback()
                        showCustomerForm = true
                    }
                )
            }
            2 -> item { BookingDetailsStep5(viewModel) }
            3 -> item { BookingItemsStep5(viewModel) }
            4 -> item { BookingPreviewStep5(viewModel = viewModel, editMode = editMode) }
            else -> item {
                BookingPaymentStep5(
                    viewModel = viewModel,
                    editMode = editMode,
                    busyAction = busyAction,
                    canPickup = canPickup,
                    setBusyAction = { busyAction = it },
                    onBack = {
                        if (!viewModel.actionBusy) {
                            viewModel.clearActionFeedback()
                            step = 4
                        }
                    },
                    onCancel = requestExit
                )
            }
        }

        if (step < 5) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    SecondaryButton(
                        text = if (step == 1) "Cancel" else "Back",
                        onClick = {
                            viewModel.clearActionFeedback()
                            if (step == 1) requestExit() else step -= 1
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.actionBusy && !viewModel.availabilityLoading,
                        icon = {
                            Icon(
                                if (step == 1) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    PrimaryButton(
                        text = "Next",
                        onClick = {
                            when (step) {
                                1 -> if (viewModel.validateCustomerStep()) {
                                    viewModel.clearActionFeedback()
                                    step = 2
                                }
                                2 -> viewModel.loadAvailabilityForSelectedDates {
                                    viewModel.clearActionFeedback()
                                    step = 3
                                }
                                3 -> if (viewModel.validateItemsStep()) {
                                    viewModel.clearActionFeedback()
                                    step = 4
                                }
                                4 -> {
                                    viewModel.clearActionFeedback()
                                    step = 5
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = when (step) {
                            1 -> viewModel.selectedCustomerId.isNotBlank()
                            2 -> !viewModel.availabilityLoading
                            3 -> viewModel.draftLines.isNotEmpty()
                            else -> true
                        },
                        loading = step == 2 && viewModel.availabilityLoading,
                        icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    AppFeedbackHost(
        successMessage = viewModel.actionNotice,
        errorMessage = (viewModel.actionError ?: viewModel.availabilityError)
    )

    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                viewModel.backFromEditor()
            }
        )
    }

    if (showCustomerForm) {
        CustomerFormSheet(
            title = "New Customer",
            initialName = customerPrefillName,
            initialMobile = customerPrefillMobile,
            busy = viewModel.newCustomerBusy,
            error = viewModel.newCustomerError,
            onDismiss = {
                if (!viewModel.newCustomerBusy) {
                    showCustomerForm = false
                    customerPrefillName = ""
                    customerPrefillMobile = ""
                }
            },
            onSave = { name, mobile, _, address ->
                viewModel.createCustomerForBooking(
                    name = name,
                    mobile = mobile,
                    alternateMobile = "",
                    address = address
                ) {
                    showCustomerForm = false
                    customerPrefillName = ""
                    customerPrefillMobile = ""
                }
            }
        )
    }
}

@Composable
private fun BookingStepper5(step: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Customer", "Details", "Items", "Preview", "Payment").forEachIndexed { index, label ->
                val number = index + 1
                val active = number == step
                val complete = number < step
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = when {
                            active -> MaterialTheme.colorScheme.primary
                            complete -> StatusSuccessSoft
                            else -> BrandSoft
                        },
                        contentColor = when {
                            active -> MaterialTheme.colorScheme.onPrimary
                            complete -> StatusSuccess
                            else -> AppTextMuted
                        }
                    ) {
                        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                            if (complete) Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            else Text(number.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else AppTextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun customerPrefillFromQuery5(query: String): Pair<String, String> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return "" to ""
    val mobileLike = trimmed.all { it.isDigit() || it.isWhitespace() || it in "+-()" }
    if (mobileLike) {
        val digits = trimmed.filter(Char::isDigit).takeLast(10)
        return "" to digits
    }
    return trimmed to ""
}

@Composable
private fun BookingCustomerStep5(
    viewModel: BookingLifecycleViewModel,
    canManageCustomers: Boolean,
    onQuickCreate: (String, String) -> Unit
) {
    val query = viewModel.customerSearch
    var visibleCount by remember(query, viewModel.customerOptions.size) { mutableIntStateOf(10) }

    if (viewModel.bootstrapState.loading && viewModel.customerOptions.isEmpty()) {
        LoadingState("Loading customers…")
        return
    }
    if (!viewModel.bootstrapState.error.isNullOrBlank() && viewModel.bootstrapState.data == null && viewModel.customerOptions.isEmpty()) {
        LoadFailureState(
            message = viewModel.bootstrapState.error.orEmpty(),
            onRetry = viewModel::refreshEditor
        )
        return
    }

    val customers = viewModel.customerOptions.take(visibleCount)
    val trimmedQuery = query.trim()
    val searchState = viewModel.customerSearchUiState
    val (quickCreateName, quickCreateMobile) = customerPrefillFromQuery5(trimmedQuery)
    val showQuickCreate = canManageCustomers &&
        trimmedQuery.isNotBlank() &&
        searchState == CustomerSearchUiState.NO_RESULT

    LabeledSectionCard(title = "Customer") {
        CompactSearchField(
            value = query,
            onValueChange = { value ->
                viewModel.setCustomerSearch(value, allowRemoteSearch = canManageCustomers)
            },
            placeholder = "Search customer",
            modifier = Modifier.fillMaxWidth(),
            searching = searchState == CustomerSearchUiState.SEARCHING
        )
        Spacer(Modifier.height(AppSpacing.xs))
        when (searchState) {
            CustomerSearchUiState.SEARCHING -> {
                InlineStatusMessage("Searching customers…", MessageTone.INFO)
                Spacer(Modifier.height(AppSpacing.xs))
            }
            CustomerSearchUiState.ERROR -> {
                InlineStatusMessage(viewModel.customerSearchError ?: "Customer search failed. Try again.", MessageTone.ERROR)
                Spacer(Modifier.height(AppSpacing.xs))
            }
            else -> Unit
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(customers, key = { it.id }) { customer ->
                val selected = customer.id == viewModel.selectedCustomerId
                Surface(
                    onClick = { viewModel.setSelectedCustomer(customer.id) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selected) BrandSoft else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = AppTextMuted)
                                Text(customer.mobile, color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = StatusSuccess)
                            SoftActionButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = "Clear selected customer",
                                onClick = { viewModel.clearSelectedCustomer() },
                                tone = ActionTone.NEUTRAL
                            )
                        }
                    }
                }
                HorizontalDivider(color = AppBorder)
            }
            if (visibleCount < viewModel.customerOptions.size) {
                item(key = "customer-select-load-more-$visibleCount") {
                    LaunchedEffect(visibleCount, viewModel.customerOptions.size) {
                        visibleCount = (visibleCount + 10).coerceAtMost(viewModel.customerOptions.size)
                    }
                }
            }

            if (showQuickCreate) {
                item(key = "customer-quick-create-$trimmedQuery") {
                    val value = quickCreateMobile.ifBlank { quickCreateName }
                    Surface(
                        onClick = {
                            onQuickCreate(
                                quickCreateName,
                                quickCreateMobile
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandSoft,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text("Create new customer", fontWeight = FontWeight.SemiBold)
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTextMuted
                                )
                            }
                        }
                    }
                }
            } else if (
                searchState == CustomerSearchUiState.NO_RESULT &&
                trimmedQuery.isNotBlank() &&
                !canManageCustomers
            ) {
                item(key = "customer-no-result-$trimmedQuery") {
                    EmptyState(
                        "No customer found. Customer creation access is not enabled for this account.",
                        variant = EmptyStateVariant.SEARCH_NO_RESULT
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsStep5(viewModel: BookingLifecycleViewModel) {
    LabeledSectionCard(title = "Details") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AppDatePickerField(
                label = "Pickup Date",
                value = viewModel.pickupDate,
                onValueChange = viewModel::setPickupDate,
                modifier = Modifier.fillMaxWidth(),
                minDate = LocalDate.now(ZoneId.of("Asia/Kolkata")),
                showDayOfWeek = true
            )
            AppDatePickerField(
                label = "Return Date",
                value = viewModel.returnDate,
                onValueChange = viewModel::setReturnDate,
                modifier = Modifier.fillMaxWidth(),
                minDate = runCatching { LocalDate.parse(viewModel.pickupDate).plusDays(1) }
                    .getOrElse { LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1) },
                showDayOfWeek = true
            )
        }
        Spacer(Modifier.height(AppSpacing.sm))
        AppTextField(
            value = viewModel.notes,
            onValueChange = viewModel::setNotes,
            label = "Notes (Optional)",
            singleLine = false,
            minLines = 2,
            maxLines = 3
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingItemsStep5(viewModel: BookingLifecycleViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("ALL") }
    val categories = remember(viewModel.itemOptions) {
        viewModel.itemOptions.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    }
    val selectedIds = viewModel.draftLines.map { it.itemId }.toSet()
    val shown = viewModel.itemOptions.filter { item ->
        (category == "ALL" || item.categoryName == category) &&
            (search.isBlank() || item.itemName.contains(search, true) || item.itemCode.contains(search, true))
    }
    var visibleCount by remember(search, category) { mutableIntStateOf(10) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        LabeledSectionCard(title = "Items") {
            ResponsiveCompactPair(
                first = { child ->
                    CompactSearchField(
                        value = search,
                        onValueChange = { search = it.take(120) },
                        placeholder = "Search item",
                        modifier = child
                    )
                },
                second = { child ->
                    AppSingleSelectFilter(
                        label = "Category",
                        selected = category,
                        options = listOf(AppFilterOption("ALL", "All Categories")) +
                            categories.map { AppFilterOption(it, it) },
                        resetValue = "ALL",
                        onApply = { category = it },
                        title = "Filter Items",
                        modifier = child
                    )
                }
            )
            if (category != "ALL") {
                Spacer(Modifier.height(AppSpacing.xs))
                StatusBadge(category, BadgeTone.INFO)
            }
        }

        if (!viewModel.bootstrapState.error.isNullOrBlank() && viewModel.bootstrapState.data == null && viewModel.itemOptions.isEmpty()) {
            LoadFailureState(
                message = viewModel.bootstrapState.error.orEmpty(),
                onRetry = viewModel::refreshEditor
            )
        } else if (shown.isEmpty()) {
            EmptyState(
                "No items found.",
                variant = when {
                    search.isNotBlank() -> EmptyStateVariant.SEARCH_NO_RESULT
                    category != "ALL" -> EmptyStateVariant.FILTERED_NO_RESULT
                    else -> EmptyStateVariant.NORMAL
                }
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(shown.take(visibleCount), key = { it.id }) { item ->
                    BookingItemResultCard5(
                        item = item,
                        available = viewModel.availableQuantity(item.id),
                        selected = item.id in selectedIds,
                        onSelect = { viewModel.addOrUpdateItem(item.id, 1) },
                        onUnselect = { viewModel.removeItem(item.id) }
                    )
                }
                if (visibleCount < shown.size) {
                    item(key = "booking-item-more-$visibleCount") {
                        LaunchedEffect(visibleCount, shown.size) {
                            visibleCount = (visibleCount + 10).coerceAtMost(shown.size)
                        }
                    }
                }
            }
        }

        if (viewModel.draftLines.isNotEmpty()) {
            LabeledSectionCard(title = "Selected Items (${viewModel.draftLines.size})") {
                val selectedIds = viewModel.draftLines.map { it.itemId }.toSet()
                val topLevelLines = viewModel.draftLines.filter { viewModel.relatedParentId(it.itemId) == null }

                topLevelLines.forEachIndexed { mainIndex, line ->
                    val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId } ?: return@forEachIndexed
                    SelectedItemRow5(
                        item = item,
                        quantity = line.quantity,
                        max = viewModel.availableQuantity(item.id).coerceAtLeast(line.quantity),
                        onQuantity = { viewModel.addOrUpdateItem(item.id, it) },
                        onDelete = { viewModel.removeItem(item.id) }
                    )

                    val related = viewModel.relatedItemsFor(item.id)
                    if (related.isNotEmpty()) {
                        Text(
                            "Related Items",
                            modifier = Modifier.padding(start = AppSpacing.md, top = AppSpacing.xs),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        related.forEach { relatedItem ->
                            val childLine = viewModel.draftLines.firstOrNull {
                                it.itemId == relatedItem.id && viewModel.relatedParentId(it.itemId) == item.id
                            }
                            when {
                                childLine != null -> SelectedItemRow5(
                                    item = relatedItem,
                                    quantity = childLine.quantity,
                                    max = viewModel.availableQuantity(relatedItem.id).coerceAtLeast(childLine.quantity),
                                    onQuantity = { viewModel.addOrUpdateItem(relatedItem.id, it) },
                                    onDelete = { viewModel.removeItem(relatedItem.id) },
                                    nested = true
                                )
                                relatedItem.id in selectedIds -> RelatedSuggestionRow5(
                                    item = relatedItem,
                                    available = viewModel.availableQuantity(relatedItem.id),
                                    alreadyAdded = true,
                                    onAdd = {}
                                )
                                else -> RelatedSuggestionRow5(
                                    item = relatedItem,
                                    available = viewModel.availableQuantity(relatedItem.id),
                                    alreadyAdded = false,
                                    onAdd = { viewModel.addRelatedItem(item.id, relatedItem.id) }
                                )
                            }
                        }
                    }

                    if (mainIndex != topLevelLines.lastIndex) AppItemListDivider()
                }
            }
        }
    }

}
@Composable
private fun BookingItemResultCard5(
    item: BookingOptionItem,
    available: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onUnselect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) BrandSoft else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else AppBorder)
    ) {
        AppItemDisplayRow(
            imageUrl = item.imageUrl,
            imageUrls = item.imageUrls,
            itemName = item.itemName,
            itemCode = item.itemCode,
            categoryName = item.categoryName,
            quantity = 1,
            statusLine = if (available > 0) "Available $available" else "Not Available",
            modifier = Modifier.padding(horizontal = AppSpacing.xs),
            trailing = {
                if (selected) {
                    Surface(
                        onClick = onUnselect,
                        color = StatusSuccessSoft,
                        contentColor = StatusSuccess,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Text("Selected", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                } else {
                    Surface(
                        onClick = onSelect,
                        enabled = available > 0,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(1.dp, AppBorder),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "+ Select",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        )
    }
}
@Composable
private fun SelectedItemRow5(
    item: BookingOptionItem,
    quantity: Int,
    max: Int,
    onQuantity: (Int) -> Unit,
    onDelete: () -> Unit,
    nested: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        AppItemDisplayRow(
            imageUrl = item.imageUrl,
            imageUrls = item.imageUrls,
            itemName = item.itemName,
            itemCode = item.itemCode,
            categoryName = item.categoryName,
            quantity = quantity,
            nested = nested,
            trailing = {
                SoftActionButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = "Remove item",
                    onClick = onDelete,
                    tone = ActionTone.DANGER
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (nested) AppSpacing.md else 0.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Qty", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            EditableQuantityControl5(quantity, max, onQuantity)
        }
    }
}

@Composable
private fun RelatedSuggestionRow5(
    item: BookingOptionItem,
    available: Int,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    AppItemDisplayRow(
        imageUrl = item.imageUrl,
        imageUrls = item.imageUrls,
        itemName = item.itemName,
        itemCode = item.itemCode,
        categoryName = item.categoryName,
        quantity = 1,
        statusLine = if (available > 0) "Available $available" else "Not Available",
        nested = true,
        trailing = {
            if (alreadyAdded) {
                StatusBadge("Added", BadgeTone.SUCCESS)
            } else {
                SoftActionButton(
                    icon = Icons.Rounded.Add,
                    label = "Add",
                    contentDescription = "Add related item",
                    onClick = onAdd,
                    enabled = available > 0,
                    tone = ActionTone.BRAND
                )
            }
        }
    )
}
@Composable
private fun EditableQuantityControl5(value: Int, max: Int, onValue: (Int) -> Unit) {
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    var field by remember(value) {
        val text = value.toString()
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }
    var hadFocus by remember { mutableStateOf(false) }

    fun restoreCurrent() {
        val text = value.toString()
        field = TextFieldValue(text, TextRange(0, text.length))
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SoftActionButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease",
            onClick = { onValue((value - 1).coerceAtLeast(1)) },
            enabled = value > 1,
            tone = ActionTone.INFO
        )
        OutlinedTextField(
            value = field,
            onValueChange = { candidate ->
                val digits = candidate.text.filter(Char::isDigit).take(5)
                if (digits.isBlank()) {
                    field = TextFieldValue("")
                } else {
                    val parsed = digits.toIntOrNull()
                    if (parsed != null && parsed in 1..max) {
                        field = TextFieldValue(digits, TextRange(digits.length))
                        onValue(parsed)
                    }
                }
            },
            modifier = Modifier
                .width(72.dp)
                .onFocusChanged { focus ->
                    if (focus.isFocused && !hadFocus) {
                        field = field.copy(selection = TextRange(0, field.text.length))
                    } else if (!focus.isFocused && hadFocus && field.text.toIntOrNull() !in 1..max) {
                        restoreCurrent()
                    }
                    hadFocus = focus.isFocused
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val parsed = field.text.toIntOrNull()
                    if (parsed != null && parsed in 1..max) onValue(parsed) else restoreCurrent()
                    dismissKeyboard()
                }
            ),
            textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        SoftActionButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase",
            onClick = { onValue((value + 1).coerceAtMost(max)) },
            enabled = value < max,
            tone = ActionTone.INFO
        )
    }
}

@Composable
private fun BookingPreviewStep5(
    viewModel: BookingLifecycleViewModel,
    editMode: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        LabeledSectionCard(title = if (editMode) "Preview Changes" else "Preview Booking") {
            val customer = viewModel.selectedCustomer
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(customer?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppTextMuted)
                Text(customer?.mobile.orEmpty(), color = AppTextMuted)
            }
            Spacer(Modifier.height(AppSpacing.xs))
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                DateBlock4(
                    label = "Pickup",
                    date = viewModel.pickupDate,
                    background = com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft,
                    foreground = StatusInfo,
                    modifier = Modifier.fillMaxWidth()
                )
                DateBlock4(
                    label = "Return",
                    date = viewModel.returnDate,
                    background = com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft,
                    foreground = com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LabeledSectionCard(title = "Items (${viewModel.draftLines.size})") {
            viewModel.draftLines.forEachIndexed { index, line ->
                val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId } ?: return@forEachIndexed
                AppItemDisplayRow(
                    imageUrl = item.imageUrl,
                    imageUrls = item.imageUrls,
                    itemName = item.itemName,
                    itemCode = item.itemCode,
                    categoryName = item.categoryName,
                    quantity = line.quantity,
                    nested = viewModel.relatedParentId(item.id) != null
                )
                if (index != viewModel.draftLines.lastIndex) AppItemListDivider()
            }
        }
    }
}

@Composable
private fun BookingPaymentStep5(
    viewModel: BookingLifecycleViewModel,
    editMode: Boolean,
    busyAction: String?,
    canPickup: Boolean,
    setBusyAction: (String?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        LabeledSectionCard(title = "Payment") {
        AppTextField(
            value = viewModel.advanceAmount,
            onValueChange = viewModel::setAdvanceAmount,
            label = "Advance Amount",
            placeholder = "Advance Amount",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            enabled = !viewModel.actionBusy,
            selectAllOnFocus = true
        )
        }


        if (editMode) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = { Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                PrimaryButton(
                    text = "Save",
                    onClick = {
                        setBusyAction("Save")
                        viewModel.saveChanges()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Save",
                    icon = { Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            SecondaryButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.actionBusy,
                icon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                SecondaryButton(
                    text = "Reserve",
                    onClick = {
                        setBusyAction("Reserve")
                        viewModel.reserve()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Reserve",
                    icon = { Icon(Icons.Rounded.EventNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                PrimaryButton(
                    text = "Confirm",
                    onClick = {
                        setBusyAction("Confirm")
                        viewModel.createConfirmed()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Confirm",
                    icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            if (canPickup) {
                PrimaryButton(
                    text = "Directly Pickup",
                    onClick = {
                        setBusyAction("Directly Pickup")
                        viewModel.directPickup()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Directly Pickup",
                    icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = { Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                SecondaryButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}


