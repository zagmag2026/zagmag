package com.nimsdeveloper.zhagmagdresses.admin

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingTimelineEvent
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.RoundedItemImage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private const val DETAIL_TAB_DETAILS_4 = 0
private const val DETAIL_TAB_PICKUP_4 = 1
private const val DETAIL_TAB_RETURN_4 = 2
private const val DETAIL_TAB_HISTORY_4 = 3

@Composable
fun BookingWorkspaceScreen4(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    composeWhatsApp: (String, String?, (String, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !viewModel.actionBusy && !viewModel.newCustomerBusy) {
        viewModel.backFromEditor()
    }

    when {
        viewModel.detailState.loading && viewModel.detailState.data == null -> {
            Column(modifier.fillMaxSize().padding(AppSpacing.md)) {
                MainScreenDateRow(businessDate)
                LoadingState()
            }
        }
        viewModel.detailState.data != null && !viewModel.editorStartsInEditMode -> {
            BookingDetailsScreen4(
                viewModel = viewModel,
                businessDate = businessDate,
                composeWhatsApp = composeWhatsApp,
                modifier = modifier
            )
        }
        else -> {
            BookingWizard4(
                viewModel = viewModel,
                businessDate = businessDate,
                editMode = viewModel.editorStartsInEditMode,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BookingWizard4(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    editMode: Boolean,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable(editMode) { mutableIntStateOf(1) }
    var showCustomerForm by remember { mutableStateOf(false) }
    var customerPrefillName by remember { mutableStateOf("") }
    var customerPrefillMobile by remember { mutableStateOf("") }
    var busyAction by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = !viewModel.actionBusy && !viewModel.newCustomerBusy) {
        if (step > 1) step -= 1 else viewModel.backFromEditor()
    }

    LaunchedEffect(viewModel.actionBusy) {
        if (!viewModel.actionBusy) busyAction = null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item { Spacer(Modifier.height(AppSpacing.xxs)) }
        item { MainScreenDateRow(businessDate) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (editMode) "Edit Booking" else "New Booking",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (step == 1) {
                    Surface(
                        onClick = {
                            viewModel.clearNewCustomerFeedback()
                            customerPrefillName = ""
                            customerPrefillMobile = ""
                            showCustomerForm = true
                        },
                        enabled = !viewModel.newCustomerBusy && !viewModel.actionBusy,
                        color = BrandSoft,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("New Customer", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item { BookingStepper4(step) }

        if (!viewModel.actionError.isNullOrBlank()) {
            item { InlineMessage(viewModel.actionError, error = true) }
        }
        if (!viewModel.availabilityError.isNullOrBlank()) {
            item { InlineMessage(viewModel.availabilityError, error = true) }
        }
        if (!viewModel.newCustomerError.isNullOrBlank()) {
            item { InlineMessage(viewModel.newCustomerError, error = true) }
        }

        when (step) {
            1 -> item {
                BookingCustomerStep4(
                    viewModel = viewModel,
                    onQuickCreate = { name, mobile ->
                        viewModel.clearNewCustomerFeedback()
                        customerPrefillName = name
                        customerPrefillMobile = mobile
                        showCustomerForm = true
                    }
                )
            }
            2 -> item { BookingDetailsStep4(viewModel) }
            3 -> item { BookingItemsStep4(viewModel) }
            else -> {
                item {
                    BookingConfirmStep4(
                        viewModel = viewModel,
                        editMode = editMode,
                        busyAction = busyAction,
                        setBusyAction = { busyAction = it },
                        onBack = {
                            if (!viewModel.actionBusy) {
                                viewModel.clearActionFeedback()
                                step = 3
                            }
                        },
                        onCancel = viewModel::backFromEditor
                    )
                }
            }
        }

        if (step < 4) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    SecondaryButton(
                        text = if (step == 1) "Cancel" else "Back",
                        onClick = {
                            viewModel.clearActionFeedback()
                            if (step == 1) viewModel.backFromEditor() else step -= 1
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
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = when (step) {
                            1 -> viewModel.selectedCustomerId.isNotBlank()
                            2 -> !viewModel.availabilityLoading
                            else -> viewModel.draftLines.isNotEmpty()
                        },
                        loading = step == 2 && viewModel.availabilityLoading,
                        icon = {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    if (showCustomerForm) {
        CustomerFormSheet(
            title = "New Customer",
            initialName = customerPrefillName,
            initialMobile = customerPrefillMobile,
            busy = viewModel.newCustomerBusy,
            error = viewModel.newCustomerError,
            onDismiss = {
                if (!viewModel.newCustomerBusy) showCustomerForm = false
            },
            onSave = { name, mobile, alternate, address ->
                viewModel.createCustomerForBooking(
                    name = name,
                    mobile = mobile,
                    alternateMobile = alternate,
                    address = address
                ) {
                    showCustomerForm = false
                }
            }
        )
    }
}

@Composable
private fun BookingStepper4(step: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Customer", "Details", "Items", "Confirm").forEachIndexed { index, label ->
                val number = index + 1
                val active = number == step
                val complete = number < step
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
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
                            if (complete) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(number.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else AppTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCustomerStep4(
    viewModel: BookingLifecycleViewModel,
    onQuickCreate: (String, String) -> Unit
) {
    val query = viewModel.customerSearch
    var visibleCount by remember(query, viewModel.customerOptions.size) { mutableIntStateOf(10) }

    val customers = viewModel.customerOptions.take(visibleCount)
    val trimmedQuery = query.trim()
    val mobileLike = trimmedQuery.isNotBlank() && trimmedQuery.all {
        it.isDigit() || it.isWhitespace() || it in "+-()"
    }
    val normalizedMobile = if (mobileLike) normalizedCustomerMobile(trimmedQuery) else null
    val queryDigits = trimmedQuery.filter(Char::isDigit)
    val hasMatchingCustomer = trimmedQuery.isNotBlank() && viewModel.customerOptions.any { customer ->
        customer.name.contains(trimmedQuery, ignoreCase = true) ||
            (queryDigits.isNotBlank() && customer.mobile.contains(queryDigits))
    }
    val searchSettled = viewModel.customerSearchCompletedQuery == trimmedQuery
    val quickCreateName = when {
        !searchSettled -> null
        trimmedQuery.length < 2 -> null
        mobileLike -> null
        hasMatchingCustomer -> null
        viewModel.customerSearchLoading -> null
        else -> trimmedQuery
    }
    val quickCreateMobile = when {
        !searchSettled -> null
        normalizedMobile == null -> null
        hasMatchingCustomer -> null
        viewModel.customerSearchLoading -> null
        else -> normalizedMobile
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Customer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(AppSpacing.sm))
        CompactSearchField(
            value = query,
            onValueChange = { value -> viewModel.setCustomerSearch(value) },
            placeholder = "Search customer",
            modifier = Modifier.fillMaxWidth()
        )
        if (!viewModel.customerSearchError.isNullOrBlank()) {
            Spacer(Modifier.height(AppSpacing.xs))
            InlineStatusMessage(
                message = viewModel.customerSearchError.orEmpty(),
                tone = MessageTone.ERROR
            )
        }
        Spacer(Modifier.height(AppSpacing.xs))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
        ) {
            items(customers, key = { it.id }) { customer ->
                val selected = customer.id == viewModel.selectedCustomerId
                Surface(
                    onClick = { viewModel.setSelectedCustomer(customer.id) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selected) BrandSoft else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                customer.mobile,
                                color = AppTextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = StatusSuccess
                            )
                        }
                    }
                }
                HorizontalDivider(color = AppBorder)
            }

            if (visibleCount < viewModel.customerOptions.size) {
                item(key = "customer-select-load-more-$visibleCount") {
                    LaunchedEffect(visibleCount, viewModel.customerOptions.size) {
                        visibleCount = (visibleCount + 10)
                            .coerceAtMost(viewModel.customerOptions.size)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Loading...", color = AppTextMuted)
                    }
                }
            }

            if (quickCreateMobile != null || quickCreateName != null) {
                item(key = "customer-quick-create-$trimmedQuery") {
                    val value = quickCreateMobile ?: quickCreateName.orEmpty()
                    Surface(
                        onClick = {
                            onQuickCreate(
                                quickCreateName.orEmpty(),
                                quickCreateMobile.orEmpty()
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
                                Text(
                                    "Create new customer",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsStep4(viewModel: BookingLifecycleViewModel) {
    val context = LocalContext.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            DatePickerField4(
                label = "Pickup Date",
                date = viewModel.pickupDate,
                onDate = viewModel::setPickupDate,
                modifier = Modifier.weight(1f),
                context = context,
                minDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            )
            DatePickerField4(
                label = "Return Date",
                date = viewModel.returnDate,
                onDate = viewModel::setReturnDate,
                modifier = Modifier.weight(1f),
                context = context,
                minDate = runCatching {
                    LocalDate.parse(viewModel.pickupDate).plusDays(1)
                }.getOrElse {
                    LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1)
                }
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

@Composable
private fun DatePickerField4(
    label: String,
    date: String,
    onDate: (String) -> Unit,
    modifier: Modifier,
    context: Context,
    minDate: LocalDate
) {
    Surface(
        onClick = { showDatePicker4(context, date, minDate, onDate) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppBorder),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    formatDateDay4(date),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun BookingItemsStep4(viewModel: BookingLifecycleViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("ALL") }
    val qty = remember { mutableStateMapOf<String, Int>() }
    val categories = remember(viewModel.itemOptions) {
        viewModel.itemOptions.map { it.categoryName }.distinct()
    }
    val selectedIds = viewModel.draftLines.map { it.itemId }.toSet()
    val shown = viewModel.itemOptions.filter { item ->
        (category == "ALL" || item.categoryName == category) &&
            (search.isBlank() ||
                item.itemName.contains(search, ignoreCase = true) ||
                item.itemCode.contains(search, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(AppSpacing.sm))
            CompactSearchField(
                value = search,
                onValueChange = { search = it.take(120) },
                placeholder = "Search item",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppSpacing.sm))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryChip4("All", category == "ALL") { category = "ALL" }
                categories.forEach { value ->
                    CategoryChip4(value, category == value) { category = value }
                }
            }
        }

        if (viewModel.draftLines.isNotEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Selected Items (${viewModel.draftLines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(AppSpacing.xs))
                viewModel.draftLines.forEach { line ->
                    val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId }
                        ?: return@forEach
                    SelectedItemRow4(
                        item = item,
                        quantity = line.quantity,
                        max = viewModel.availableQuantity(item.id),
                        onQuantity = { viewModel.addOrUpdateItem(item.id, it) },
                        onDelete = { viewModel.removeItem(item.id) }
                    )
                    HorizontalDivider(color = AppBorder)
                }
            }
        }

        shown.forEach { item ->
            val available = viewModel.availableQuantity(item.id)
            val selected = item.id in selectedIds
            val currentQty = qty[item.id] ?: 1
            AppCard(modifier = Modifier.fillMaxWidth()) {
                ItemHeader4(item, available)
                Spacer(Modifier.height(AppSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuantityControl4(
                        value = currentQty.coerceAtMost(available.coerceAtLeast(1)),
                        max = available,
                        enabled = available > 0 && !selected,
                        min = 1,
                        onValue = { qty[item.id] = it }
                    )
                    if (selected) {
                        StatusBadge("Added", BadgeTone.SUCCESS)
                    } else {
                        PrimaryButton(
                            text = "Add",
                            onClick = {
                                if (viewModel.addOrUpdateItem(item.id, currentQty)) {
                                    qty[item.id] = 1
                                }
                            },
                            enabled = available > 0,
                            icon = {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            EmptyState("No items found.")
        }
    }
}

@Composable
private fun CategoryChip4(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else BrandSoft,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else AppTextMuted,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ItemHeader4(item: BookingOptionItem, available: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(54.dp),
            galleryUrls = item.imageUrls
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${item.itemCode} · ${item.categoryName}",
                color = AppTextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                if (available > 0) "Available: $available" else "Unavailable",
                color = if (available > 0) StatusSuccess else StatusError,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SelectedItemRow4(
    item: BookingOptionItem,
    quantity: Int,
    max: Int,
    onQuantity: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(46.dp),
            galleryUrls = item.imageUrls
        )
            Column(Modifier.weight(1f)) {
                Text(item.itemName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.itemCode} · ${item.categoryName}",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SoftActionButton(
                icon = Icons.Rounded.Delete,
                contentDescription = "Remove item",
                onClick = onDelete,
                tone = ActionTone.DANGER
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quantity", style = MaterialTheme.typography.labelMedium)
            QuantityControl4(
                value = quantity,
                max = max.coerceAtLeast(quantity),
                enabled = true,
                min = 1,
                onValue = onQuantity
            )
        }
    }
}

@Composable
private fun QuantityControl4(
    value: Int,
    max: Int,
    enabled: Boolean,
    min: Int,
    onValue: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SoftActionButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease",
            onClick = { onValue((value - 1).coerceAtLeast(min)) },
            enabled = enabled && value > min,
            tone = ActionTone.INFO
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Text(
                value.toString(),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontWeight = FontWeight.Bold
            )
        }
        SoftActionButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase",
            onClick = { onValue((value + 1).coerceAtMost(max)) },
            enabled = enabled && value < max,
            tone = ActionTone.INFO
        )
    }
}

@Composable
private fun BookingConfirmStep4(
    viewModel: BookingLifecycleViewModel,
    editMode: Boolean,
    busyAction: String?,
    setBusyAction: (String?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                if (editMode) "Review Changes" else "Confirm Booking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(AppSpacing.xs))
            val customer = viewModel.selectedCustomer
            Text(
                customer?.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(customer?.mobile.orEmpty(), color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                DateBlock4(
                    "Pickup",
                    viewModel.pickupDate,
                    StatusInfoSoft,
                    StatusInfo,
                    Modifier.weight(1f)
                )
                DateBlock4(
                    "Return",
                    viewModel.returnDate,
                    StatusWarningSoft,
                    StatusWarning,
                    Modifier.weight(1f)
                )
            }
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Items (${viewModel.draftLines.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            viewModel.draftLines.forEach { line ->
                val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId }
                    ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(44.dp),
            galleryUrls = item.imageUrls
        )
                    Column(Modifier.weight(1f).padding(start = AppSpacing.sm)) {
                        Text(item.itemName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${item.itemCode} · ${item.categoryName}",
                            color = AppTextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("× ${line.quantity}", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (editMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                    icon = {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            SecondaryButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.actionBusy,
                icon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SecondaryButton(
                    text = "Reserve",
                    onClick = {
                        setBusyAction("Reserve")
                        viewModel.reserve()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Reserve",
                    icon = {
                        Icon(
                            Icons.Rounded.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                    icon = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                PrimaryButton(
                    text = "Pickup",
                    onClick = {
                        setBusyAction("Pickup")
                        viewModel.directPickup()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy && busyAction == "Pickup",
                    icon = {
                        Icon(
                            Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                SecondaryButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.actionBusy,
                    icon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsScreen4(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    composeWhatsApp: (String, String?, (String, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = viewModel.detailState.data ?: return
    val context = LocalContext.current
    var tab by rememberSaveable(detail.booking.id) { mutableIntStateOf(DETAIL_TAB_DETAILS_4) }
    var contactAction by remember { mutableStateOf<CustomerContactAction?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item { Spacer(Modifier.height(AppSpacing.xxs)) }
        item { MainScreenDateRow(businessDate) }
        item { Text("Booking Details", style = MaterialTheme.typography.headlineMedium) }
        if (!viewModel.actionNotice.isNullOrBlank()) {
            item { PopupMessage(viewModel.actionNotice, MessageTone.SUCCESS) }
        }
        if (!viewModel.actionError.isNullOrBlank()) {
            item { PopupMessage(viewModel.actionError, MessageTone.ERROR) }
        }
        item {
            TabRow(selectedTabIndex = tab) {
                listOf("Details", "Pickup", "Return", "History").forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = {
                            viewModel.clearActionFeedback()
                            tab = index
                        },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (tab == index) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }

        when (tab) {
            DETAIL_TAB_DETAILS_4 -> item {
                BookingDetailOverview4(
                    detail = detail,
                    busy = viewModel.actionBusy,
                    onEdit = { viewModel.openBooking(detail.booking.id, edit = true) },
                    onCall = {
                        val alt = usableAlternateMobile(
                            detail.booking.customerMobile,
                            detail.booking.customerAlternateMobile
                        )
                        if (alt == null) {
                            bookingOpenDial4(context, detail.booking.customerMobile)
                        } else {
                            contactAction = CustomerContactAction.CALL
                        }
                    },
                    onWhatsApp = {
                        val alt = usableAlternateMobile(
                            detail.booking.customerMobile,
                            detail.booking.customerAlternateMobile
                        )
                        if (alt == null) {
                            composeWhatsApp(
                                detail.booking.customerId,
                                detail.booking.id
                            ) { _, message ->
                                bookingOpenWhatsApp4(
                                    context,
                                    detail.booking.customerMobile,
                                    message
                                )
                            }
                        } else {
                            contactAction = CustomerContactAction.WHATSAPP
                        }
                    },
                    onCancel = { confirmCancel = true },
                    onConfirm = viewModel::confirmReserved,
                    onPickup = { tab = DETAIL_TAB_PICKUP_4 },
                    onReturn = { tab = DETAIL_TAB_RETURN_4 }
                )
            }
            DETAIL_TAB_PICKUP_4 -> item { BookingPickupTab4(viewModel, detail) }
            DETAIL_TAB_RETURN_4 -> item { BookingReturnTab4(viewModel, detail) }
            DETAIL_TAB_HISTORY_4 -> item { BookingHistoryTab4(detail) }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    contactAction?.let { action ->
        CustomerContactChooserSheet(
            customerName = detail.booking.customerName,
            primary = detail.booking.customerMobile,
            alternate = detail.booking.customerAlternateMobile,
            action = action,
            onSelect = { selected ->
                contactAction = null
                if (action == CustomerContactAction.CALL) {
                    bookingOpenDial4(context, selected)
                } else {
                    composeWhatsApp(detail.booking.customerId, detail.booking.id) { _, message ->
                        bookingOpenWhatsApp4(context, selected, message)
                    }
                }
            },
            onDismiss = { contactAction = null }
        )
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = {
                if (!viewModel.actionBusy) confirmCancel = false
            },
            title = { Text("Cancel booking?") },
            text = { Text(detail.booking.bookingNo) },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.actionBusy,
                    onClick = {
                        confirmCancel = false
                        viewModel.cancelBooking()
                    }
                ) {
                    Icon(
                        Icons.Rounded.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel Booking")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("Back") }
            }
        )
    }
}

@Composable
private fun BookingDetailOverview4(
    detail: BookingDetailData,
    busy: Boolean,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onPickup: () -> Unit,
    onReturn: () -> Unit
) {
    val booking = detail.booking
    val status = booking.displayStatus.uppercase()
    val editable = booking.rawStatus.equals("BOOKED", true)

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        booking.bookingNo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        booking.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(booking.customerMobile, color = AppTextMuted)
                }
                StatusBadge(lifecycleLabel4(status), lifecycleTone4(status))
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                DateBlock4(
                    "Pickup",
                    booking.pickupDate,
                    StatusInfoSoft,
                    StatusInfo,
                    Modifier.weight(1f)
                )
                DateBlock4(
                    "Return",
                    booking.returnDate,
                    StatusWarningSoft,
                    StatusWarning,
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                "Items (${detail.items.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            detail.items.forEachIndexed { index, item ->
                DetailItemSummaryRow4(item)
                if (index != detail.items.lastIndex) {
                    HorizontalDivider(color = AppBorder)
                }
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (editable) {
                    SoftActionButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.INFO
                    )
                }
                SoftActionButton(
                    icon = Icons.Rounded.Chat,
                    label = "WhatsApp",
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.TEAL
                )
                SoftActionButton(
                    icon = Icons.Rounded.Call,
                    label = "Call",
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.SUCCESS
                )
            }
        }

        if (editable) {
            SecondaryButton(
                text = "Cancel Booking",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = {
                    Icon(
                        Icons.Rounded.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = StatusError
                    )
                }
            )
        }

        when (status) {
            "RESERVED" -> PrimaryButton(
                text = "Confirm",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                loading = busy,
                icon = {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            "BOOKED", "PART_PICKUP" -> PrimaryButton(
                text = "Pickup",
                onClick = onPickup,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = {
                    Icon(
                        Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            "FULL_PICKUP", "PART_RETURN" -> PrimaryButton(
                text = "Return",
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = {
                    Icon(
                        Icons.Rounded.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DetailItemSummaryRow4(item: BookingDetailItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(48.dp),
            galleryUrls = item.imageUrls
        )
        Column(Modifier.weight(1f)) {
            Text(item.itemName, fontWeight = FontWeight.SemiBold)
            Text(
                "${item.itemCode} · ${item.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
            Text(
                "Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text("× ${item.bookedQty}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BookingPickupTab4(
    viewModel: BookingLifecycleViewModel,
    detail: BookingDetailData
) {
    val status = detail.booking.displayStatus.uppercase()
    val canPickup = status in setOf("BOOKED", "PART_PICKUP") &&
        detail.items.any { it.remainingToGive > 0 }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Pickup (Handover)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Pickup · ${formatDateDay4(detail.booking.pickupDate)}",
            color = AppTextMuted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(AppSpacing.sm))

        if (canPickup) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SecondaryButton(
                    text = "All",
                    onClick = viewModel::fillAllPickup,
                    enabled = !viewModel.actionBusy,
                    icon = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            detail.items.forEach { item ->
                ActionQuantityRow4(
                    item = item,
                    value = viewModel.pickupNow[item.bookingItemId] ?: 0,
                    max = item.remainingToGive,
                    label = "Pickup Qty",
                    onValue = { viewModel.setPickupNow(item.bookingItemId, it) }
                )
            }
            AppTextField(
                value = viewModel.pickupNotes,
                onValueChange = viewModel::setPickupNotes,
                label = "Pickup Notes (Optional)",
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
            Spacer(Modifier.height(AppSpacing.sm))
            PrimaryButton(
                text = "Confirm Pickup",
                onClick = viewModel::savePickup,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.pickupNow.values.any { it > 0 } && !viewModel.actionBusy,
                loading = viewModel.actionBusy,
                icon = {
                    Icon(
                        Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        } else {
            InlineStatusMessage(
                if (detail.items.any { it.givenQty > 0 }) {
                    "Pickup completed."
                } else {
                    "No pickup has been recorded."
                },
                tone = if (detail.items.any { it.givenQty > 0 }) MessageTone.SUCCESS else MessageTone.INFO
            )
            detail.items.filter { it.givenQty > 0 }.forEach { item ->
                ReadOnlyLifecycleItemRow4(item, "Picked", item.givenQty)
            }
        }
    }
}

@Composable
private fun BookingReturnTab4(
    viewModel: BookingLifecycleViewModel,
    detail: BookingDetailData
) {
    val canReturn = detail.booking.rawStatus != "CANCELLED" &&
        detail.items.any { it.pendingToReturn > 0 }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Return Items",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Return · ${formatDateDay4(detail.booking.returnDate)}",
            color = AppTextMuted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(AppSpacing.sm))

        if (canReturn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                SecondaryButton(
                    text = "All",
                    onClick = viewModel::fillAllReturn,
                    enabled = !viewModel.actionBusy,
                    icon = {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            detail.items.forEach { item ->
                ActionQuantityRow4(
                    item = item,
                    value = viewModel.returnNow[item.bookingItemId] ?: 0,
                    max = item.pendingToReturn,
                    label = "Return Qty",
                    onValue = { viewModel.setReturnNow(item.bookingItemId, it) }
                )
            }
            AppTextField(
                value = viewModel.returnNotes,
                onValueChange = viewModel::setReturnNotes,
                label = "Return Notes (Optional)",
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
            Spacer(Modifier.height(AppSpacing.sm))
            PrimaryButton(
                text = "Confirm Return",
                onClick = viewModel::saveReturn,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.returnNow.values.any { it > 0 } && !viewModel.actionBusy,
                loading = viewModel.actionBusy,
                icon = {
                    Icon(
                        Icons.Rounded.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        } else {
            InlineStatusMessage(
                if (detail.booking.displayStatus.equals("FULL_RETURN", true)) {
                    "Return completed."
                } else {
                    "No items are ready to return."
                },
                tone = if (detail.booking.displayStatus.equals("FULL_RETURN", true)) MessageTone.SUCCESS else MessageTone.INFO
            )
            detail.items.filter { it.returnedQty > 0 }.forEach { item ->
                ReadOnlyLifecycleItemRow4(item, "Returned", item.returnedQty)
            }
        }
    }
}

@Composable
private fun ActionQuantityRow4(
    item: BookingDetailItem,
    value: Int,
    max: Int,
    label: String,
    onValue: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(48.dp),
            galleryUrls = item.imageUrls
        )
            Column(Modifier.weight(1f)) {
                Text(item.itemName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.itemCode} · ${item.categoryName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
                Text(
                    "Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
                Text(
                    "Remaining $max",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusInfo
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            QuantityControl4(
                value = value,
                max = max,
                enabled = true,
                min = 0,
                onValue = onValue
            )
        }
    }
}

@Composable
private fun ReadOnlyLifecycleItemRow4(
    item: BookingDetailItem,
    label: String,
    quantity: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        RoundedItemImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(46.dp),
            galleryUrls = item.imageUrls
        )
        Column(Modifier.weight(1f)) {
            Text(item.itemName, fontWeight = FontWeight.SemiBold)
            Text(
                "${item.itemCode} · ${item.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
        }
        Text(
            "$label × $quantity",
            color = StatusSuccess,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BookingHistoryTab4(detail: BookingDetailData) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = StatusSuccess
            )
            Text(
                "Booking History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(AppSpacing.sm))

        val events = detail.timeline.asReversed()
        if (events.isEmpty()) {
            Text("No history available.", color = AppTextMuted)
        } else {
            events.forEachIndexed { index, event ->
                BookingHistoryRow4(event)
                if (index != events.lastIndex) {
                    HorizontalDivider(color = AppBorder)
                }
            }
        }
    }
}

@Composable
private fun BookingHistoryRow4(event: BookingTimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = StatusSuccessSoft,
            shape = RoundedCornerShape(999.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.padding(7.dp).size(18.dp),
                tint = StatusSuccess
            )
        }
        Column(Modifier.weight(1f)) {
            Text(historyActionLabel4(event.action), fontWeight = FontWeight.SemiBold)
            Text(
                formatTimestamp4(event.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
            if (!event.userName.isNullOrBlank()) {
                Text(
                    event.userName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextMuted
                )
            }
        }
    }
}

private fun historyActionLabel4(action: String): String = when (action.uppercase()) {
    "RESERVE_BOOKING" -> "Reserved"
    "CREATE" -> "Booked"
    "CONFIRM_BOOKING" -> "Booked"
    "UPDATE" -> "Booking Updated"
    "PICKUP" -> "Picked Up"
    "CORRECT_PICKUP" -> "Pickup Corrected"
    "RETURN" -> "Returned"
    "CORRECT_RETURN" -> "Return Corrected"
    "CANCEL" -> "Cancelled"
    else -> action.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

private fun formatTimestamp4(raw: String): String {
    val zone = ZoneId.of("Asia/Kolkata")
    val zoned = runCatching {
        Instant.parse(raw).atZone(zone)
    }.getOrNull() ?: runCatching {
        LocalDateTime.parse(raw.replace(' ', 'T').take(19))
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(zone)
    }.getOrNull() ?: return raw

    return zoned.format(
        DateTimeFormatter.ofPattern("dd-MM-yyyy · EEEE · h:mm a", Locale.ENGLISH)
    ).lowercase(Locale.ENGLISH)
}

private fun showDatePicker4(
    context: Context,
    current: String,
    minDate: LocalDate,
    onDate: (String) -> Unit
) {
    val initial = runCatching { LocalDate.parse(current) }.getOrDefault(minDate)
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            onDate(LocalDate.of(year, month + 1, day).toString())
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    )
    dialog.datePicker.minDate = minDate
        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
        .toInstant()
        .toEpochMilli()
    dialog.show()
}
