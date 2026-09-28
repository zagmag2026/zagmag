package com.nimsdeveloper.zhagmagdresses.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingTimelineEvent
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCompactScrollableTabs
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSelectField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InfoValueRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LabeledSectionCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionSpec
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveSoftActionGrid
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

private const val DETAIL6 = 0
private const val PICKUP6 = 1
private const val RETURN6 = 2
private const val BILL6 = 3
private const val HISTORY6 = 4

@Composable
fun BookingDetailsScreen6(
    viewModel: BookingLifecycleViewModel,
    currentUser: com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser,
    branding: com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding,
    businessDate: String,
    composeWhatsApp: (String, String?, String?, (String, String) -> Unit) -> Unit,
    contactBusy: Boolean = false,
    canContactCustomers: Boolean = true,
    canPickup: Boolean = true,
    canReturn: Boolean = true,
    onBilling: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val detail = viewModel.detailState.data
    BackHandler(enabled = !viewModel.actionBusy) {
        viewModel.backFromEditor()
    }
    if (detail == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            MainScreenDateRow(businessDate)
            AppBackHeader(
                title = "Booking Details",
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
        return
    }

    val context = LocalContext.current
    val mutationBlocked = viewModel.actionBusy || viewModel.detailRefreshBlocked
    var tab by rememberSaveable(detail.booking.id) { mutableIntStateOf(DETAIL6) }
    var confirmCall by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var cancelSettlement by rememberSaveable(detail.booking.id) { mutableStateOf("") }
    var partialRefund by rememberSaveable(detail.booking.id) { mutableStateOf("") }
    var contactLaunchError by remember(detail.booking.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.billingHandoffBookingId) {
        val bookingId = viewModel.billingHandoffBookingId
        if (!bookingId.isNullOrBlank()) {
            viewModel.consumeBillingHandoff()
            tab = BILL6
        }
    }

    val detailTabs = listOf("Details", "Pickup", "Return", "Bill", "History")

    if (tab == BILL6) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            MainScreenDateRow(businessDate)
            AppBackHeader(
                title = "Booking Details",
                onBack = viewModel::backFromEditor,
                enabled = !viewModel.actionBusy
            )
            AppCompactScrollableTabs(
                labels = detailTabs,
                selectedIndex = tab,
                onSelect = { index ->
                    viewModel.clearActionFeedback()
                    tab = index
                },
                minTabWidth = 96.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Screen11Billing(
                currentUser = currentUser,
                branding = branding,
                businessDate = businessDate,
                onBack = { tab = DETAIL6 },
                initialBookingId = detail.booking.id,
                embedded = true,
                modifier = Modifier.weight(1f)
            )
        }
        AppFeedbackHost(
            successMessage = viewModel.actionNotice,
            errorMessage = contactLaunchError ?: viewModel.actionError
        )
        return
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
            AppBackHeader(
                title = "Booking Details",
                onBack = viewModel::backFromEditor,
                enabled = !viewModel.actionBusy
            )
        }

        if (!viewModel.detailState.error.isNullOrBlank()) {
            item {
                InlineStatusMessage(viewModel.detailState.error, MessageTone.ERROR)
            }
        }
        if (viewModel.detailRefreshBlocked) {
            item {
                SecondaryButton(
                    text = "Retry Refresh",
                    onClick = viewModel::refreshEditor,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.detailState.loading,
                    loading = viewModel.detailState.loading
                )
            }
        }
        item {
            AppCompactScrollableTabs(
                labels = detailTabs,
                selectedIndex = tab,
                onSelect = { index ->
                    viewModel.clearActionFeedback()
                    tab = index
                },
                minTabWidth = 96.dp
            )
        }

        when (tab) {
            DETAIL6 -> item {
                BookingDetailOverview6(
                    detail = detail,
                    busy = viewModel.actionBusy || contactBusy,
                    mutationBlocked = mutationBlocked,
                    canContactCustomers = canContactCustomers,
                    canPickup = canPickup,
                    canReturn = canReturn,
                    onEdit = { viewModel.openBooking(detail.booking.id, edit = true) },
                    onCall = { confirmCall = true },
                    onWhatsApp = {
                        composeWhatsApp(detail.booking.customerId, detail.booking.id, null) { mobile, message ->
                            if (!launchCustomerWhatsApp(context, mobile, message)) {
                                contactLaunchError = "Unable to open WhatsApp."
                            }
                        }
                    },
                    onCancel = { confirmCancel = true },
                    onConfirm = viewModel::confirmReserved,
                    onDirectPickup = {
                        viewModel.confirmReservedForPickup { tab = PICKUP6 }
                    },
                    onPickup = { tab = PICKUP6 },
                    onReturn = { tab = RETURN6 },
                    onBilling = { tab = BILL6 }
                )
            }
            PICKUP6 -> item { BookingPickupTab6(viewModel, detail, canPickup) }
            RETURN6 -> item { BookingReturnTab6(viewModel, detail, canReturn) }
            HISTORY6 -> item { BookingHistoryTab6(detail) }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    AppFeedbackHost(
        successMessage = viewModel.actionNotice,
        errorMessage = contactLaunchError ?: viewModel.actionError
    )

    if (confirmCall && canContactCustomers) {
        CustomerContactConfirmationSheet(
            customerName = detail.booking.customerName,
            mobile = detail.booking.customerMobile,
            action = CustomerContactAction.CALL,
            onDismiss = { confirmCall = false },
            onConfirm = {
                confirmCall = false
                if (!launchCustomerCall(context, detail.booking.customerMobile)) {
                    contactLaunchError = "Unable to open the Phone app."
                }
            }
        )
    }

    if (confirmCancel) {
        val advance = detail.booking.advanceAmount
        val partialValue = partialRefund.toIntOrNull() ?: 0
        val settlementValid = advance <= 0 ||
            cancelSettlement == "FULL_REFUND" ||
            cancelSettlement == "NO_REFUND" ||
            (cancelSettlement == "PARTIAL_REFUND" && partialValue in 1 until advance)
        AppDestructiveConfirmDialog(
            title = "Cancel booking?",
            message = if (advance > 0) {
                "Cancel booking ${detail.booking.bookingNo}? Advance ₹$advance must be settled before cancellation."
            } else {
                "Cancel booking ${detail.booking.bookingNo}? This stops the booking before pickup."
            },
            confirmLabel = "Cancel Booking",
            dismissLabel = "Back",
            busy = viewModel.actionBusy,
            confirmEnabled = settlementValid,
            extraContent = if (advance > 0) {
                {
                    AppSelectField(
                        label = "Advance Settlement",
                        selected = cancelSettlement,
                        options = listOf(
                            AppFilterOption("FULL_REFUND", "Full Refund"),
                            AppFilterOption("PARTIAL_REFUND", "Partial Refund"),
                            AppFilterOption("NO_REFUND", "No Refund")
                        ),
                        onSelected = {
                            cancelSettlement = it
                            if (it != "PARTIAL_REFUND") partialRefund = ""
                        },
                        prompt = "Select settlement",
                        required = true,
                        enabled = !viewModel.actionBusy
                    )
                    if (cancelSettlement == "PARTIAL_REFUND") {
                        AppTextField(
                            value = partialRefund,
                            onValueChange = { partialRefund = it.filter(Char::isDigit).take(9) },
                            label = "Refund Amount",
                            placeholder = "Refund Amount",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            enabled = !viewModel.actionBusy,
                            selectAllOnFocus = true
                        )
                    }
                    val refund = when (cancelSettlement) {
                        "FULL_REFUND" -> advance
                        "PARTIAL_REFUND" -> partialValue
                        else -> 0
                    }
                    if (cancelSettlement.isNotBlank()) {
                        InlineStatusMessage(
                            "Advance ₹$advance · Refund ₹$refund · Retained ₹${(advance - refund).coerceAtLeast(0)}",
                            MessageTone.INFO
                        )
                    }
                }
            } else null,
            onConfirm = {
                confirmCancel = false
                val refund = when (cancelSettlement) {
                    "FULL_REFUND" -> advance
                    "PARTIAL_REFUND" -> partialValue
                    else -> 0
                }
                viewModel.cancelBooking(cancelSettlement.ifBlank { null }, refund)
                cancelSettlement = ""
                partialRefund = ""
            },
            onDismiss = {
                confirmCancel = false
                cancelSettlement = ""
                partialRefund = ""
            }
        )
    }
}

@Composable
private fun BookingDetailOverview6(
    detail: BookingDetailData,
    busy: Boolean,
    mutationBlocked: Boolean,
    canContactCustomers: Boolean,
    canPickup: Boolean,
    canReturn: Boolean,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onDirectPickup: () -> Unit,
    onPickup: () -> Unit,
    onReturn: () -> Unit,
    onBilling: () -> Unit
) {
    val booking = detail.booking
    val status = booking.displayStatus.uppercase()
    val editable = status == "RESERVED" && detail.items.all { it.givenQty == 0 }
    val cancelled = booking.rawStatus.equals("CANCELLED", true) || status == "CANCELLED"
    val showBill = status != "RESERVED" && (!cancelled || !booking.billId.isNullOrBlank() || booking.advanceAmount > 0)

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        BookingSummaryCard(
            id = "details-${booking.id}",
            customerName = booking.customerName,
            customerMobile = booking.customerMobile,
            customerAddress = booking.customerAddress,
            bookingNo = booking.bookingNo,
            displayStatus = booking.displayStatus,
            paymentStatus = booking.paymentStatus,
            bookingDate = booking.bookingDate,
            pickupDate = booking.pickupDate,
            returnDate = booking.returnDate,
            items = detail.items.map { item ->
                BookingSummaryItemUi(
                    imageUrl = item.imageUrl,
                    imageUrls = item.imageUrls,
                    itemName = item.itemName,
                    itemCode = item.itemCode,
                    categoryName = item.categoryName,
                    quantity = item.bookedQty,
                    statusLine = "Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}"
                )
            },
            totalItemCount = detail.items.size,
            showAllItems = true,
            actions = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ResponsiveSoftActionGrid(
                        actions = buildList {
                            if (editable) add(SoftActionSpec(Icons.Rounded.Edit, "Edit", onEdit, !mutationBlocked, ActionTone.INFO))
                            if (showBill) add(SoftActionSpec(Icons.Rounded.ReceiptLong, "Bill", onBilling, !busy, ActionTone.BRAND))
                            if (canContactCustomers) add(SoftActionSpec(Icons.Rounded.Call, "Call", onCall, !busy, ActionTone.SUCCESS))
                            if (canContactCustomers) add(SoftActionSpec(Icons.Rounded.Chat, "WhatsApp", onWhatsApp, !busy, ActionTone.TEAL))
                        },
                        minCellWidth = 48.dp,
                        maxColumns = 4
                    )
                }
            }
        )

        if (status == "RESERVED") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton(
                    text = "Cancel Booking",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !mutationBlocked,
                    icon = { Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp), tint = StatusError) }
                )
                PrimaryButton(
                    text = "Confirm",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = !mutationBlocked,
                    loading = busy,
                    icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            if (canPickup) {
                PrimaryButton(
                    text = "Directly Pickup",
                    onClick = onDirectPickup,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !mutationBlocked,
                    loading = busy,
                    icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        } else {
            val pickupPending = detail.items.any { it.remainingToGive > 0 }
            val returnPending = detail.items.any { it.pendingToReturn > 0 }

            if (!cancelled && editable && detail.items.all { it.givenQty == 0 }) {
                SecondaryButton(
                    text = "Cancel Booking",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !mutationBlocked,
                    icon = { Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp), tint = StatusError) }
                )
            }

            if (!cancelled && ((pickupPending && canPickup) || (returnPending && canReturn))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (pickupPending && canPickup) {
                        PrimaryButton(
                            text = "Pickup",
                            onClick = onPickup,
                            modifier = Modifier.weight(1f),
                            enabled = !mutationBlocked,
                            icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    if (returnPending && canReturn) {
                        PrimaryButton(
                            text = "Return",
                            onClick = onReturn,
                            modifier = Modifier.weight(1f),
                            enabled = !mutationBlocked,
                            icon = { Icon(Icons.Rounded.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItemSummaryRow6(item: BookingDetailItem) {
    AppItemDisplayRow(
        imageUrl = item.imageUrl,
        imageUrls = item.imageUrls,
        itemName = item.itemName,
        itemCode = item.itemCode,
        categoryName = item.categoryName,
        quantity = item.bookedQty,
        statusLine = buildString {
            append("Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}")
            if (item.closedQty > 0) append(" · Closed ${item.closedQty}")
        }
    )
}
@Composable
private fun BookingPickupTab6(
    viewModel: BookingLifecycleViewModel,
    detail: BookingDetailData,
    actionAllowed: Boolean
) {
    val mutationBlocked = viewModel.actionBusy || viewModel.detailRefreshBlocked || !actionAllowed
    val canPickup = actionAllowed &&
        !detail.booking.confirmationState.equals("RESERVED", true) &&
        !detail.booking.rawStatus.equals("CANCELLED", true) &&
        detail.items.any { it.remainingToGive > 0 }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Pickup (Handover)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Pickup · ${formatDateDay4(detail.booking.pickupDate)}", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(AppSpacing.sm))

        if (canPickup) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SecondaryButton(
                    text = "All",
                    onClick = viewModel::fillAllPickup,
                    enabled = !mutationBlocked,
                    icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            detail.items.forEach { item ->
                ActionQuantityRow6(
                    item = item,
                    value = viewModel.pickupNow[item.bookingItemId] ?: 0,
                    max = item.remainingToGive,
                    label = "Pickup Qty",
                    enabled = !mutationBlocked,
                    onValue = { viewModel.setPickupNow(item.bookingItemId, it) }
                )
            }
            AppTextField(
                value = viewModel.pickupNotes,
                onValueChange = viewModel::setPickupNotes,
                label = "Pickup Notes (Optional)",
                enabled = !mutationBlocked,
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
            Spacer(Modifier.height(AppSpacing.sm))
            PrimaryButton(
                text = "Confirm Pickup",
                onClick = viewModel::savePickup,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.pickupNow.values.any { it > 0 } && !mutationBlocked,
                loading = viewModel.actionBusy,
                icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        } else {
            InlineStatusMessage(
                when {
                    !actionAllowed -> "Pickup access is not enabled for this account."
                    detail.items.any { it.givenQty > 0 } -> "Pickup completed."
                    else -> "No pickup has been recorded."
                },
                tone = if (actionAllowed && detail.items.any { it.givenQty > 0 }) MessageTone.SUCCESS else MessageTone.INFO
            )
            detail.items.filter { it.givenQty > 0 }.forEach { item ->
                ReadOnlyLifecycleItemRow6(item, "Picked", item.givenQty)
            }
        }
    }
}

@Composable
private fun BookingReturnTab6(
    viewModel: BookingLifecycleViewModel,
    detail: BookingDetailData,
    actionAllowed: Boolean
) {
    val mutationBlocked = viewModel.actionBusy || viewModel.detailRefreshBlocked || !actionAllowed
    val canReturn = actionAllowed &&
        !detail.booking.rawStatus.equals("CANCELLED", true) && detail.items.any { it.pendingToReturn > 0 }
    val hasPendingPickup = detail.items.any { it.remainingToGive > 0 }
    var pendingPickupDecision by remember(detail.booking.id) { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Return Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Return · ${formatDateDay4(detail.booking.returnDate)}", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(AppSpacing.sm))

        if (canReturn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SecondaryButton(
                    text = "All",
                    onClick = viewModel::fillAllReturn,
                    enabled = !mutationBlocked,
                    icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            detail.items.forEach { item ->
                ActionQuantityRow6(
                    item = item,
                    value = viewModel.returnNow[item.bookingItemId] ?: 0,
                    max = item.pendingToReturn,
                    label = "Return Qty",
                    enabled = !mutationBlocked,
                    onValue = { viewModel.setReturnNow(item.bookingItemId, it) }
                )
            }
            AppTextField(
                value = viewModel.returnNotes,
                onValueChange = viewModel::setReturnNotes,
                label = "Return Notes (Optional)",
                enabled = !mutationBlocked,
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
            Spacer(Modifier.height(AppSpacing.sm))
            PrimaryButton(
                text = "Confirm Return",
                onClick = {
                    if (hasPendingPickup) pendingPickupDecision = true else viewModel.saveReturn()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.returnNow.values.any { it > 0 } && !mutationBlocked,
                loading = viewModel.actionBusy,
                icon = { Icon(Icons.Rounded.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        } else {
            InlineStatusMessage(
                when {
                    !actionAllowed -> "Return access is not enabled for this account."
                    detail.booking.displayStatus.equals("FULL_RETURN", true) -> "Return completed."
                    else -> "No items are ready to return."
                },
                tone = if (actionAllowed && detail.booking.displayStatus.equals("FULL_RETURN", true)) MessageTone.SUCCESS else MessageTone.INFO
            )
            detail.items.filter { it.returnedQty > 0 }.forEach { item ->
                ReadOnlyLifecycleItemRow6(item, "Returned", item.returnedQty)
            }
        }
    }

    if (pendingPickupDecision) {
        AlertDialog(
            onDismissRequest = { if (!mutationBlocked) pendingPickupDecision = false },
            title = { Text("Remaining pickup items") },
            text = {
                Text("Some booked items have not been picked up yet. Keep the order open if the customer will collect them later, or close the remaining items if they are no longer needed.")
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        enabled = !mutationBlocked,
                        onClick = {
                            pendingPickupDecision = false
                            viewModel.saveReturn("KEEP_OPEN")
                        }
                    ) { Text("Keep Order Open") }
                    DangerTextButton(
                        text = "Close Remaining Items",
                        enabled = !mutationBlocked,
                        onClick = {
                            pendingPickupDecision = false
                            viewModel.saveReturn("CLOSE_REMAINING")
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPickupDecision = false }) { Text("Back") }
            }
        )
    }
}

@Composable
private fun ActionQuantityRow6(
    item: BookingDetailItem,
    value: Int,
    max: Int,
    label: String,
    enabled: Boolean,
    onValue: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        AppItemDisplayRow(
            imageUrl = item.imageUrl,
            imageUrls = item.imageUrls,
            itemName = item.itemName,
            itemCode = item.itemCode,
            categoryName = item.categoryName,
            quantity = item.bookedQty,
            statusLine = buildString {
                append("Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}")
                if (item.closedQty > 0) append(" · Closed ${item.closedQty}")
                append(" · Remaining $max")
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            QuantityControl6(value = value, max = max, enabled = enabled, onValue = onValue)
        }
    }
}
@Composable
private fun QuantityControl6(value: Int, max: Int, enabled: Boolean, onValue: (Int) -> Unit) {
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
            onClick = { onValue((value - 1).coerceAtLeast(0)) },
            enabled = enabled && value > 0,
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
                    if (parsed != null && parsed in 0..max) {
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
                    } else if (!focus.isFocused && hadFocus && field.text.toIntOrNull() !in 0..max) {
                        restoreCurrent()
                    }
                    hadFocus = focus.isFocused
                },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val parsed = field.text.toIntOrNull()
                    if (parsed != null && parsed in 0..max) onValue(parsed) else restoreCurrent()
                    dismissKeyboard()
                }
            ),
            textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
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
private fun ReadOnlyLifecycleItemRow6(item: BookingDetailItem, label: String, quantity: Int) {
    AppItemDisplayRow(
        imageUrl = item.imageUrl,
        imageUrls = item.imageUrls,
        itemName = item.itemName,
        itemCode = item.itemCode,
        categoryName = item.categoryName,
        quantity = quantity,
        statusLine = "Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}"
    )
}
@Composable
private fun BookingHistoryTab6(detail: BookingDetailData) {
    val events = normalizedHistory6(detail)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = StatusSuccess)
            Text("Booking History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(AppSpacing.sm))
        if (events.isEmpty()) {
            Text("No history available.", color = AppTextMuted)
        } else {
            events.forEachIndexed { index, event ->
                BookingHistoryRow6(event)
                if (index != events.lastIndex) HorizontalDivider(color = AppBorder)
            }
        }
    }
}

@Composable
private fun BookingHistoryRow6(event: BookingTimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Surface(color = StatusSuccessSoft, shape = RoundedCornerShape(999.dp)) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.padding(7.dp).size(18.dp),
                tint = StatusSuccess
            )
        }
        Column(Modifier.weight(1f)) {
            Text(historyActionLabel6(event), fontWeight = FontWeight.SemiBold)
            Text(formatTimestamp6(event.createdAt), style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
            paymentHistoryLines6(event).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
            if (event.items.isNotEmpty()) {
                event.items.forEach { item ->
                    Text(
                        "• ${item.itemName} × ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!event.userName.isNullOrBlank()) {
                Text(event.userName, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            }
        }
    }
}

private fun normalizedHistory6(detail: BookingDetailData): List<BookingTimelineEvent> {
    val hasReserve = detail.timeline.any { it.action.equals("RESERVE_BOOKING", true) }
    return detail.timeline
        .filterNot { hasReserve && it.action.equals("CREATE", true) }
        .sortedByDescending { it.createdAt.replace(' ', 'T') }
}

private fun historyActionLabel6(event: BookingTimelineEvent): String = when (event.action.uppercase()) {
    "RESERVE_BOOKING" -> "Reserved"
    "CREATE" -> "Booked"
    "CONFIRM_BOOKING" -> "Confirmed"
    "UPDATE" -> "Booking Updated"
    "PICKUP" -> when (eventStatus6(event)) {
        "PARTIALLY_GIVEN" -> "Part Picked Up"
        "GIVEN", "RETURNED" -> "Full Picked Up"
        else -> "Pickup"
    }
    "CORRECT_PICKUP" -> "Pickup Corrected"
    "RETURN" -> when (eventStatus6(event)) {
        "PARTIALLY_RETURNED" -> "Part Return"
        "RETURNED" -> "Full Returned"
        else -> "Return"
    }
    "CLOSE_REMAINING_ITEMS" -> "Remaining Items Closed"
    "CORRECT_RETURN" -> "Return Corrected"
    "CANCEL" -> "Cancelled"
    "PAYMENT_ADVANCE" -> "Advance Received"
    "PAYMENT_CREATE" -> "Bill Draft Created"
    "PAYMENT_UPDATE" -> "Payment Updated"
    "PAYMENT_FINALIZE" -> "Bill Finalized"
    "PAYMENT_CANCEL" -> "Bill Cancelled"
    "PAYMENT_DELETE" -> "Bill Draft Deleted"
    else -> event.action.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

private data class PaymentHistorySnapshot6(
    val advance: Int,
    val totalReceived: Int,
    val billAmount: Int,
    val balanceDue: Int,
    val status: String
) {
    val otherReceived: Int get() = (totalReceived - advance).coerceAtLeast(0)
}

private fun paymentHistorySnapshot6(raw: String?): PaymentHistorySnapshot6? = runCatching {
    val json = JSONObject(raw.orEmpty())
    fun amount(camel: String, snake: String): Int = when {
        json.has(camel) -> json.optInt(camel, 0)
        json.has(snake) -> json.optInt(snake, 0)
        else -> 0
    }
    val advance = amount("advanceAmount", "advance_amount")
    val received = amount("receivedAmount", "received_amount")
    val bill = amount("netAmount", "net_amount")
    val balance = amount("balanceAmount", "balance_amount")
    val status = json.optString("paymentStatus").ifBlank { json.optString("payment_status") }
    PaymentHistorySnapshot6(advance, received, bill, balance, status)
}.getOrNull()

private fun paymentHistoryLines6(event: BookingTimelineEvent): List<String> {
    if (!event.action.startsWith("PAYMENT_", true)) return emptyList()
    val old = paymentHistorySnapshot6(event.oldValueJson)
    val new = paymentHistorySnapshot6(event.newValueJson)
    if (new == null || event.action.equals("PAYMENT_FINALIZE", true) || event.action.equals("PAYMENT_CANCEL", true) || event.action.equals("PAYMENT_DELETE", true)) return emptyList()
    if (event.action.equals("PAYMENT_ADVANCE", true)) {
        return buildList {
            add("Advance Received · ₹${new.advance}")
            add("Total Received · ₹${new.totalReceived}")
            new.status.takeIf { it.isNotBlank() }?.let { add("Payment Status · ${bookingPaymentLabel(it)}") }
        }
    }

    fun changed(label: String, oldValue: Int?, newValue: Int): String? =
        if (oldValue != null && oldValue != newValue) "$label · ₹$oldValue → ₹$newValue"
        else if (oldValue == null) "$label · ₹$newValue"
        else null

    return buildList {
        changed("Bill Amount", old?.billAmount, new.billAmount)?.let(::add)
        changed("Advance Received", old?.advance, new.advance)?.let(::add)
        changed("Other Received", old?.otherReceived, new.otherReceived)?.let(::add)
        changed("Total Received", old?.totalReceived, new.totalReceived)?.let(::add)
        changed("Balance Due", old?.balanceDue, new.balanceDue)?.let(::add)
        val oldStatus = old?.status?.takeIf { it.isNotBlank() }?.let(::bookingPaymentLabel)
        val newStatus = new.status.takeIf { it.isNotBlank() }?.let(::bookingPaymentLabel)
        if (newStatus != null && oldStatus != newStatus) {
            add(if (oldStatus == null) "Payment Status · $newStatus" else "Payment Status · $oldStatus → $newStatus")
        }
    }
}

private fun eventStatus6(event: BookingTimelineEvent): String = runCatching {
    val json = JSONObject(event.newValueJson.orEmpty())
    json.optString("movementStatus").ifBlank { json.optString("status") }.uppercase()
}.getOrDefault("")

private fun formatTimestamp6(raw: String): String {
    val zone = ZoneId.of("Asia/Kolkata")
    val zoned = runCatching { Instant.parse(raw).atZone(zone) }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(raw.replace(' ', 'T').take(19))
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(zone)
        }.getOrNull()
        ?: return raw
    return zoned.format(DateTimeFormatter.ofPattern("dd-MM-yyyy · EEEE · h:mm a", Locale.ENGLISH))
        .lowercase(Locale.ENGLISH)
}
