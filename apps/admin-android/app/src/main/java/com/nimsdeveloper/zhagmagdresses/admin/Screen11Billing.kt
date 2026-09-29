package com.nimsdeveloper.zhagmagdresses.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.StaffAccess
import com.nimsdeveloper.zhagmagdresses.admin.data.hasAccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSingleSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSelectField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppUnsavedChangesDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InfoValueRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LabeledSectionCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemListDivider
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.rememberReliableKeyboardDismiss
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun Screen11Billing(
    currentUser: SessionUser,
    branding: AppBranding,
    businessDate: String,
    onBack: () -> Unit,
    initialBookingId: String? = null,
    onInitialBookingConsumed: () -> Unit = {},
    initialPaymentFilter: String = "",
    initialStatusFilter: String = "",
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) {
        Screen11BillingRepository(
            ApiClient(BuildConfig.API_BASE_URL, SecureSessionStore(context.applicationContext))
        )
    }
    val factory = remember(repository) { Screen11BillingViewModel.Factory(repository) }
    val vm: Screen11BillingViewModel = viewModel(
        key = if (embedded) "billing-embedded-${initialBookingId.orEmpty()}" else "billing-main",
        factory = factory
    )
    val state = vm.state
    var showDeleteDraft by rememberSaveable { mutableStateOf(false) }
    var pdfSuccess by remember { mutableStateOf<String?>(null) }
    var pdfError by remember { mutableStateOf<String?>(null) }
    var contactLaunchError by remember { mutableStateOf<String?>(null) }
    var showDiscardChanges by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialBookingId, initialPaymentFilter, initialStatusFilter) {
        val id = initialBookingId?.trim().orEmpty()
        if (id.isNotBlank()) {
            vm.openBookingBill(id)
        } else if (initialPaymentFilter.isNotBlank() || initialStatusFilter.isNotBlank()) {
            vm.openFilteredList(initialPaymentFilter, initialStatusFilter)
        } else if (state.list.bills.isEmpty() && !state.loading && state.screen == BillingScreen.LIST) {
            vm.loadList(1)
        }
    }
    LaunchedEffect(initialBookingId, state.screen, state.loading) {
        if (!initialBookingId.isNullOrBlank() && state.screen != BillingScreen.LIST && !state.loading) {
            onInitialBookingConsumed()
        }
    }

    val performBack: () -> Unit = {
        if (!state.busy && !state.loading) {
            if (embedded) {
                when (state.screen) {
                    BillingScreen.EDIT -> onBack()
                    BillingScreen.DETAIL, BillingScreen.SETTLEMENT, BillingScreen.SELECT_ORDER, BillingScreen.LIST -> onBack()
                }
            } else {
                if (state.screen == BillingScreen.LIST) {
                    if (initialPaymentFilter.isNotBlank() || initialStatusFilter.isNotBlank()) {
                        vm.clearExternalListFilters()
                    }
                    onBack()
                } else vm.back()
            }
        }
    }
    val requestBack: () -> Unit = {
        if (!state.busy && !state.loading) {
            if (state.screen == BillingScreen.EDIT && vm.hasUnsavedEditorChanges()) {
                showDiscardChanges = true
            } else {
                performBack()
            }
        }
    }
    BackHandler { requestBack() }

    LaunchedEffect(embedded, initialBookingId, state.screen, state.loading, state.message) {
        if (
            embedded &&
            !initialBookingId.isNullOrBlank() &&
            state.screen == BillingScreen.LIST &&
            !state.loading &&
            state.message == "Quotation deleted."
        ) {
            vm.openBookingBill(initialBookingId)
        }
    }

    if (!initialBookingId.isNullOrBlank() && state.screen == BillingScreen.LIST) {
        when {
            state.loading -> BillingCenteredLoading("Opening Bill…")
            !state.detailLoadError.isNullOrBlank() -> BillingLoadFailureScreen(
                message = state.detailLoadError.orEmpty(),
                onRetry = { vm.retryBookingBill(initialBookingId) },
                onBack = performBack,
                modifier = modifier
            )
            else -> BillingCenteredLoading("Opening Bill…")
        }
    } else when (state.screen) {
        BillingScreen.LIST -> BillingListScreen(
            state = state,
            businessDate = businessDate,
            onBack = requestBack,
            onAdd = vm::openOrderSelector,
            onSearch = vm::setSearch,
            onApplySearch = vm::applySearch,
            onPaymentFilter = vm::setPaymentFilter,
            onStatusFilter = vm::setStatusFilter,
            onOpen = vm::openDetail,
            onPreviewOrder = vm::openOrderPreview,
            onRefresh = vm::refreshList,
            onLoadMore = vm::loadMore,
            onRetryLoadMore = vm::retryLoadMore,
            modifier = modifier
        )
        BillingScreen.SELECT_ORDER -> BillingOrderSelectorScreen(
            state = state,
            businessDate = businessDate,
            onBack = requestBack,
            onSearch = vm::setOrderSearch,
            onApplySearch = vm::applyOrderSearch,
            onPreviewOrder = vm::openOrderPreview,
            onCreateDraft = vm::createDraftFromOrder,
            onLoadMore = vm::loadMoreEligibleOrders,
            onRetry = vm::retryEligibleOrders,
            onRetryLoadMore = vm::retryLoadMoreEligibleOrders,
            modifier = modifier
        )
        BillingScreen.EDIT -> BillingEditScreen(
            state = state,
            branding = branding,
            businessDate = businessDate,
            onBack = requestBack,
            onDate = vm::setBillDate,
            onQuantity = vm::setLineQuantity,
            onRent = vm::setLineRent,
            onDiscount = vm::setDiscount,
            onAdvance = vm::setAdvance,
            onOtherReceived = vm::setOtherReceived,
            onNotes = vm::setNotes,
            onSaveDraft = { vm.saveDraft(false) },
            onSaveFinal = { vm.saveDraft(true) },
            onPdfResult = { success, error ->
                pdfSuccess = success
                pdfError = error
            },
            embedded = embedded,
            modifier = modifier
        )
        BillingScreen.SETTLEMENT -> BillingAdvanceSettlementScreen(
            state = state,
            businessDate = businessDate,
            onBack = requestBack,
            embedded = embedded,
            modifier = modifier
        )
        BillingScreen.DETAIL -> when {
            state.loading -> BillingCenteredLoading()
            state.detail == null && !state.detailLoadError.isNullOrBlank() -> BillingLoadFailureScreen(
                message = state.detailLoadError.orEmpty(),
                onRetry = vm::retryDetail,
                onBack = performBack,
                modifier = modifier
            )
            else -> BillingDetailScreen(
                state = state,
                branding = branding,
                businessDate = businessDate,
                onBack = requestBack,
                onPdfResult = { success, error ->
                    pdfSuccess = success
                    pdfError = error
                },
                embedded = embedded,
                modifier = modifier
            )
        }
    }

    state.orderPreview?.let { detail ->
        BookingOrderPreviewSheet(detail = detail, onDismiss = vm::closeOrderPreview)
    }

    AppFeedbackHost(
        successMessage = state.message ?: pdfSuccess,
        errorMessage = contactLaunchError ?: state.error ?: pdfError
    )

    if (showDeleteDraft) {
        AppDestructiveConfirmDialog(
            title = "Delete Quotation?",
            message = "This Quotation will be permanently deleted.",
            confirmLabel = "Delete Quotation",
            busy = state.busy,
            onConfirm = {
                showDeleteDraft = false
                vm.deleteDraft()
            },
            onDismiss = { showDeleteDraft = false }
        )
    }


    state.whatsapp?.let { bundle ->
        BillingWhatsAppDialog(
            bundle = bundle,
            onDismiss = vm::clearWhatsApp,
            onLaunchError = { contactLaunchError = it }
        )
    }

    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                performBack()
            }
        )
    }
}

@Composable
private fun BillingCenteredLoading(
    label: String = "Loading…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted
            )
        }
    }
}

@Composable
private fun BillingLoadFailureScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        LoadFailureState(message = message, onRetry = onRetry)
        SecondaryButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingListScreen(
    state: Screen11BillingState,
    businessDate: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSearch: (String) -> Unit,
    onApplySearch: () -> Unit,
    onPaymentFilter: (String) -> Unit,
    onStatusFilter: (String) -> Unit,
    onOpen: (String) -> Unit,
    onPreviewOrder: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = state.listRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
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
                title = "Bills",
                onBack = onBack,
                enabled = !state.loading && !state.busy,
                action = {
                    CompactNewActionButton(
                        label = "Add Quotation",
                        enabled = !state.loading && !state.busy,
                        onClick = onAdd
                    )
                }
            )
        }
        item {
            CompactSearchField(
                value = state.search,
                onValueChange = onSearch,
                placeholder = "Search bill no, customer or mobile",
                enabled = !state.loading,
                searching = state.loading && state.search.isNotBlank(),
                onSubmit = onApplySearch,
                onClear = {
                    onSearch("")
                    onApplySearch()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ResponsiveCompactPair(
                first = { child ->
                    AppSingleSelectFilter(
                        label = "Payment", selected = state.paymentFilter,
                        options = listOf(AppFilterOption("", "All Payments"), AppFilterOption("PENDING", "Pending"), AppFilterOption("PART_RECEIVED", "Part Received"), AppFilterOption("FULL_AMOUNT_RECEIVED", "Full Received")),
                        resetValue = "", onApply = onPaymentFilter, modifier = child, title = "Payment Status"
                    )
                },
                second = { child ->
                    AppSingleSelectFilter(
                        label = "Bill Status", selected = state.statusFilter,
                        options = listOf(AppFilterOption("", "All Status"), AppFilterOption("DRAFT", "Quotation"), AppFilterOption("FINAL", "Final"), AppFilterOption("CANCELLED", "Cancelled")),
                        resetValue = "", onApply = onStatusFilter, modifier = child, title = "Bill Status"
                    )
                }
            )
        }

        if (!state.listLoadError.isNullOrBlank() && state.list.bills.isNotEmpty()) {
            item {
                InlineRetryMessage(
                    message = state.listLoadError.orEmpty(),
                    onRetry = onRefresh
                )
            }
        }
        if (state.loading && !state.listLoaded && state.list.bills.isEmpty()) {
            item { LoadingState() }
        } else if (!state.listLoadError.isNullOrBlank() && state.list.bills.isEmpty()) {
            item {
                LoadFailureState(
                    message = state.listLoadError.orEmpty(),
                    onRetry = onRefresh
                )
            }
        } else if (state.listLoaded && !state.loading && state.list.bills.isEmpty()) {
            item { EmptyState("No Bills found.") }
        } else {
            items(state.list.bills, key = { it.id }) { bill ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                        ) {
                            Text(
                                bill.billNo ?: "Quotation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            InfoValueRow(Icons.Rounded.Person, bill.customerName, emphasized = true)
                            InfoValueRow(Icons.Rounded.Phone, bill.customerMobile, muted = true)
                            if (bill.customerAddress.isNotBlank()) {
                                InfoValueRow(Icons.Rounded.LocationOn, bill.customerAddress, muted = true)
                            }
                            if (!bill.bookingNo.isNullOrBlank()) {
                                InfoValueRow(Icons.Rounded.ReceiptLong, bill.bookingNo.orEmpty())
                            }
                            InfoValueRow(
                                Icons.Rounded.CalendarMonth,
                                billingDisplayDate(bill.billDate),
                                muted = true
                            )
                            Text(
                                "Bill Amount ₹${bill.netAmount} · Balance Due ₹${bill.balanceAmount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column {
                            StatusBadge(
                                if (bill.status == "CANCELLED") "Cancelled" else bill.status.lowercase().replaceFirstChar(Char::uppercase),
                                if (bill.status == "CANCELLED") BadgeTone.ERROR else if (bill.status == "FINAL") BadgeTone.SUCCESS else BadgeTone.WARNING
                            )
                            Spacer(Modifier.size(4.dp))
                            StatusBadge(
                                billingPaymentLabel(bill.paymentStatus),
                                billingPaymentTone(bill.paymentStatus)
                            )
                        }
                    }
                    Spacer(Modifier.size(AppSpacing.xs))
                    if (!bill.bookingId.isNullOrBlank()) {
                        ResponsiveCompactPair(
                            first = { child ->
                                SecondaryButton(
                                    text = "Order Preview",
                                    onClick = { onPreviewOrder(bill.bookingId.orEmpty()) },
                                    modifier = child,
                                    enabled = !state.orderPreviewLoading && !state.busy,
                                    icon = {
                                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            },
                            second = { child ->
                                SecondaryButton(
                                    text = if (bill.status == "DRAFT") "Edit" else "View",
                                    onClick = { onOpen(bill.id) },
                                    modifier = child,
                                    icon = {
                                        Icon(
                                            if (bill.status == "DRAFT") Icons.Rounded.Edit else Icons.Rounded.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        )
                    } else {
                        SecondaryButton(
                            text = if (bill.status == "DRAFT") "Edit" else "View",
                            onClick = { onOpen(bill.id) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(
                                    if (bill.status == "DRAFT") Icons.Rounded.Edit else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (state.listLoadMoreError.isNullOrBlank() && state.list.page < state.list.totalPages) {
            item(key = "billing-load-more-${state.list.page}") {
                LaunchedEffect(state.list.page, state.list.bills.size) {
                    onLoadMore()
                }
                LoadingState()
            }
        }
        if (!state.listLoadMoreError.isNullOrBlank()) {
            item(key = "billing-load-more-retry-${state.list.page}") {
                InlineRetryMessage(
                    message = state.listLoadMoreError.orEmpty(),
                    onRetry = onRetryLoadMore,
                    retryLabel = "Retry loading more"
                )
            }
        }
    }
    }
}

@Composable
private fun BillingOrderSelectorScreen(
    state: Screen11BillingState,
    businessDate: String,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onApplySearch: () -> Unit,
    onPreviewOrder: (String) -> Unit,
    onCreateDraft: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                title = "Select Order",
                onBack = onBack,
                enabled = !state.loading && !state.busy
            )
        }
        item {
            CompactSearchField(
                value = state.orderSearch,
                onValueChange = onSearch,
                placeholder = "Search Order ID, customer or mobile",
                enabled = !state.busy,
                searching = state.loading && state.orderSearch.isNotBlank(),
                onSubmit = onApplySearch,
                onClear = {
                    onSearch("")
                    onApplySearch()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!state.eligibleLoadError.isNullOrBlank() && state.eligibleOrders.orders.isNotEmpty()) {
            item {
                InlineRetryMessage(
                    message = state.eligibleLoadError.orEmpty(),
                    onRetry = onRetry
                )
            }
        }
        when {
            state.loading && !state.eligibleLoaded && state.eligibleOrders.orders.isEmpty() -> item { LoadingState() }
            !state.eligibleLoadError.isNullOrBlank() && state.eligibleOrders.orders.isEmpty() -> item {
                LoadFailureState(
                    message = state.eligibleLoadError.orEmpty(),
                    onRetry = onRetry
                )
            }
            state.eligibleLoaded && !state.loading && state.eligibleOrders.orders.isEmpty() -> item {
                EmptyState(
                    if (state.orderSearch.isBlank()) "No Orders without a Bill." else "No matching Order without a Bill."
                )
            }
            else -> items(state.eligibleOrders.orders, key = { it.id }) { order ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                        ) {
                            InfoValueRow(Icons.Rounded.Person, order.customerName, emphasized = true)
                            InfoValueRow(Icons.Rounded.Phone, order.customerMobile, muted = true)
                            if (order.customerAddress.isNotBlank()) {
                                InfoValueRow(Icons.Rounded.LocationOn, order.customerAddress, muted = true)
                            }
                            InfoValueRow(Icons.Rounded.ReceiptLong, order.bookingNo)
                            if (order.advanceAmount > 0) {
                                Spacer(Modifier.size(4.dp))
                                StatusBadge("Advance ₹${order.advanceAmount}", BadgeTone.INFO)
                            }
                        }
                        val orderPaymentStatus = if (order.advanceAmount > 0) "PART_RECEIVED" else "PENDING"
                        StatusBadge(
                            bookingPaymentLabel(orderPaymentStatus),
                            bookingPaymentTone(orderPaymentStatus)
                        )
                    }
                    Spacer(Modifier.size(AppSpacing.xs))
                    ResponsiveCompactPair(
                        first = { child ->
                            SecondaryButton(
                                text = "Order Preview",
                                onClick = { onPreviewOrder(order.id) },
                                modifier = child,
                                enabled = !state.orderPreviewLoading && !state.busy,
                                icon = {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                        },
                        second = { child ->
                            PrimaryButton(
                                text = "Create Quotation",
                                onClick = { onCreateDraft(order.id) },
                                modifier = child,
                                enabled = !state.busy && !state.orderPreviewLoading,
                                loading = state.busy
                            )
                        }
                    )
                }
            }
        }
        if (state.eligibleLoadMoreError.isNullOrBlank() && state.eligibleOrders.page < state.eligibleOrders.totalPages) {
            item(key = "eligible-order-load-more-${state.eligibleOrders.page}") {
                LaunchedEffect(state.eligibleOrders.page, state.eligibleOrders.orders.size) { onLoadMore() }
                LoadingState()
            }
        }
        if (!state.eligibleLoadMoreError.isNullOrBlank()) {
            item(key = "eligible-order-load-more-retry-${state.eligibleOrders.page}") {
                InlineRetryMessage(
                    message = state.eligibleLoadMoreError.orEmpty(),
                    onRetry = onRetryLoadMore,
                    retryLabel = "Retry loading more"
                )
            }
        }
    }
}

@Composable
private fun BillingAdvanceSettlementScreen(
    state: Screen11BillingState,
    businessDate: String,
    onBack: () -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val booking = state.settlementBooking
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.xs,
            bottom = AppSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        if (!embedded) {
            item { MainScreenDateRow(businessDate) }
            item { AppBackHeader(title = "Advance Settlement", onBack = onBack, enabled = !state.loading && !state.busy) }
        }
        if (booking == null) {
            item { EmptyState("Advance settlement is unavailable.") }
        } else {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text(booking.bookingNo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${booking.customerName} · ${booking.customerMobile}", color = AppTextMuted)
                    Spacer(Modifier.size(AppSpacing.xs))
                    BillingAmountRow("Advance Received", booking.advanceAmount)
                    BillingAmountRow("Refund Amount", booking.advanceRefundAmount)
                    BillingAmountRow("Retained Amount", booking.advanceRetainedAmount, strong = true)
                    Spacer(Modifier.size(AppSpacing.xs))
                    StatusBadge(
                        when (booking.advanceSettlementStatus) {
                            "FULL_REFUND" -> "Full Refund"
                            "PARTIAL_REFUND" -> "Partial Refund"
                            "NO_REFUND" -> "No Refund"
                            else -> "Not Settled"
                        },
                        if (booking.advanceSettlementStatus == null) BadgeTone.WARNING else BadgeTone.INFO
                    )
                }
            }
            item {
                Text(
                    "This cancelled Order has no Bill. The original Advance and its cancellation settlement remain preserved as financial history.",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BillingEditScreen(
    state: Screen11BillingState,
    branding: AppBranding,
    businessDate: String,
    onBack: () -> Unit,
    onDate: (String) -> Unit,
    onQuantity: (String, String) -> Unit,
    onRent: (String, String) -> Unit,
    onDiscount: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onOtherReceived: (String) -> Unit,
    onNotes: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onSaveFinal: () -> Unit,
    onPdfResult: (String?, String?) -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val booking = state.bootstrap?.booking
    val context = LocalContext.current
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    val focusRequesters = remember(state.lines.map { it.itemId }) {
        List(state.lines.size * 2 + 3) { FocusRequester() }
    }
    val discountFocus = focusRequesters[state.lines.size * 2]
    val advanceFocus = focusRequesters[state.lines.size * 2 + 1]
    val otherReceivedFocus = focusRequesters[state.lines.size * 2 + 2]
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
        if (!embedded) {
            item { MainScreenDateRow(businessDate) }
            item {
                AppBackHeader(
                    title = if (state.billId.isNullOrBlank()) "Create Quotation" else "Edit Quotation",
                    onBack = onBack,
                    enabled = !state.busy
                )
            }
        } else {
            item {
                Text(
                    if (state.billId.isNullOrBlank()) "Create Quotation" else "Edit Quotation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            LabeledSectionCard(title = "Quotation Header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                    ) {
                        InfoValueRow(
                            Icons.Rounded.ReceiptLong,
                            "Quotation · ${state.detail?.bill?.bookingNo ?: state.bookingNo ?: "-"}",
                            emphasized = true
                        )
                        StatusBadge("Quotation", BadgeTone.WARNING)
                    }
                    BillingCompactDatePicker(
                        value = state.billDate,
                        onValueChange = onDate,
                        enabled = !state.busy,
                        label = "Quotation Date"
                    )
                }
            }
        }

        item {
            LabeledSectionCard(title = "Booking & Customer") {
                if (booking != null) {
                    InfoValueRow(Icons.Rounded.Person, booking.customerName, emphasized = true)
                    InfoValueRow(Icons.Rounded.Phone, booking.customerMobile)
                    if (booking.customerAddress.isNotBlank()) {
                        InfoValueRow(Icons.Rounded.LocationOn, booking.customerAddress, muted = true)
                    }
                    InfoValueRow(Icons.Rounded.ReceiptLong, booking.bookingNo)
                } else {
                    InfoValueRow(Icons.Rounded.ReceiptLong, state.bookingNo ?: "Linked Booking")
                }
            }
        }

        item {
            LabeledSectionCard(title = "Items (${state.lines.size})") {
                state.lines.forEachIndexed { index, line ->
                    AppItemDisplayRow(
                        imageUrl = line.imageUrl,
                        imageUrls = line.imageUrls,
                        itemName = line.itemName,
                        itemCode = line.itemCode,
                        categoryName = line.categoryName,
                        quantity = line.quantity,
                        statusLine = "Qty ${line.quantity} × ₹${line.rentRate} · Amount ₹${line.amount}"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        val quantityFocus = focusRequesters[index * 2]
                        val rentFocus = focusRequesters[index * 2 + 1]
                        AppTextField(
                            value = line.quantity.toString(),
                            onValueChange = { onQuantity(line.itemId, it) },
                            label = "Qty",
                            modifier = Modifier.weight(1f).focusRequester(quantityFocus),
                            enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { rentFocus.requestFocus() }),
                            selectAllOnFocus = true
                        )
                        AppTextField(
                            value = line.rentRate.toString(),
                            onValueChange = { onRent(line.itemId, it) },
                            label = "Rent Rate",
                            modifier = Modifier.weight(1f).focusRequester(rentFocus),
                            enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    val next = if (index < state.lines.lastIndex) {
                                        focusRequesters[(index + 1) * 2]
                                    } else {
                                        discountFocus
                                    }
                                    next.requestFocus()
                                }
                            ),
                            selectAllOnFocus = true
                        )
                    }
                    if (index != state.lines.lastIndex) AppItemListDivider()
                }
            }
        }

        item {
            LabeledSectionCard(title = "Pickup / Return Status") {
                ResponsiveCompactPair(
                    first = { child ->
                        BillingLifecycleStatusBlock(
                            title = "Pickup",
                            done = billingPickupDone(booking?.status.orEmpty()),
                            date = booking?.pickupDate.orEmpty(),
                            icon = Icons.Rounded.LocalShipping,
                            modifier = child
                        )
                    },
                    second = { child ->
                        BillingLifecycleStatusBlock(
                            title = "Return",
                            done = booking?.returnComplete == true,
                            date = booking?.returnDate.orEmpty(),
                            icon = Icons.Rounded.Reply,
                            modifier = child
                        )
                    }
                )
            }
        }

        item {
            LabeledSectionCard(title = "Amount Summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    StatusBadge(billingPaymentLabel(state.paymentStatus), billingPaymentTone(state.paymentStatus))
                }
                BillingAmountRow("Item Total", state.totalRent)
                AppTextField(
                    value = state.discountText,
                    onValueChange = onDiscount,
                    label = "Discount",
                    modifier = Modifier.focusRequester(discountFocus),
                    placeholder = "0",
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { advanceFocus.requestFocus() }),
                    selectAllOnFocus = true
                )
                BillingAmountRow("Bill Amount", state.netAmount, strong = true)
                AppTextField(
                    value = state.advanceText,
                    onValueChange = onAdvance,
                    label = "Advance Received",
                    modifier = Modifier.focusRequester(advanceFocus),
                    placeholder = "Advance Amount",
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { otherReceivedFocus.requestFocus() }),
                    selectAllOnFocus = true
                )
                AppTextField(
                    value = state.otherReceivedText,
                    onValueChange = onOtherReceived,
                    label = "Other Received",
                    modifier = Modifier.focusRequester(otherReceivedFocus),
                    placeholder = "0",
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    selectAllOnFocus = true
                )
                BillingAmountRow("Total Received", state.totalReceivedAmount, strong = true)
                AppItemListDivider()
                BillingBalanceDue(state.balanceAmount)
                Text("Payment collection: Cash", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            LabeledSectionCard(title = "Notes") {
                AppTextField(
                    value = state.notes,
                    onValueChange = onNotes,
                    label = "Notes",
                    placeholder = "Enter bill notes",
                    enabled = !state.busy,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null) }
                )
            }
        }

            if (state.detail?.bill?.status == "DRAFT" && state.detail != null) {
                item {
                    val quotationDetail = state.detail
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        ResponsiveCompactPair(
                            first = { child ->
                                SecondaryButton(
                                    text = "View",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, quotationDetail)
                                            Screen8PdfExporter.view(context, file)
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, "Unable to open Quotation PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            },
                            second = { child ->
                                SecondaryButton(
                                    text = "Share",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, quotationDetail)
                                            Screen11BillingPdfExporter.shareDirectToCustomerWhatsApp(
                                                context = context,
                                                file = file,
                                                customerMobile = quotationDetail.bill.customerMobile,
                                                title = "Quotation ${quotationDetail.bill.bookingNo ?: ""}"
                                            )
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, "Unable to share Quotation PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        )
                        ResponsiveCompactPair(
                            first = { child ->
                                SecondaryButton(
                                    text = "Download",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, quotationDetail)
                                            Screen8PdfExporter.download(context, file)
                                        }.onSuccess { onPdfResult(it, null) }
                                            .onFailure { onPdfResult(null, "Unable to download Quotation PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            },
                            second = { child ->
                                SecondaryButton(
                                    text = "Print",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, quotationDetail)
                                            Screen8PdfExporter.print(
                                                context,
                                                file,
                                                "Quotation ${quotationDetail.bill.bookingNo ?: ""}"
                                            )
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, "Unable to print Quotation PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        )
                    }
                }
            }

        item {
            val canFinalize = booking?.returnComplete == true
            if (!canFinalize) {
                Text(
                    "Finalize Bill becomes available after 100% Return is complete.",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ResponsiveCompactPair(
                first = { child ->
                    SecondaryButton(
                        text = "Save Quotation",
                        onClick = {
                            dismissKeyboard()
                            onSaveDraft()
                        },
                        modifier = child,
                        enabled = !state.busy
                    )
                },
                second = { child ->
                    PrimaryButton(
                        text = "Finalize Bill",
                        onClick = {
                            dismissKeyboard()
                            onSaveFinal()
                        },
                        modifier = child,
                        enabled = !state.busy && canFinalize,
                        loading = state.busy
                    )
                }
            )
        }
    }
}

@Composable
private fun BillingDetailScreen(
    state: Screen11BillingState,
    branding: AppBranding,
    businessDate: String,
    onBack: () -> Unit,
    onPdfResult: (String?, String?) -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detail = state.detail
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.xs,
            bottom = AppSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        if (!embedded) {
            item { MainScreenDateRow(businessDate) }
            item { AppBackHeader(title = if (detail?.bill?.status == "DRAFT") "Quotation Details" else "Bill Details", onBack = onBack, enabled = !state.busy) }
        } else {
            item {
                Text(if (detail?.bill?.status == "DRAFT") "Quotation Details" else "Bill Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        if (state.loading && detail == null) {
            item { LoadingState() }
        } else if (detail == null) {
            item { EmptyState("Bill details are unavailable.") }
        } else {
            val bill = detail.bill
            item {
                LabeledSectionCard(title = if (bill.status == "DRAFT") "Quotation Header" else "Bill Header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                        ) {
                            InfoValueRow(
                                Icons.Rounded.ReceiptLong,
                                if (bill.status == "DRAFT") "Quotation · ${bill.bookingNo ?: "-"}" else "Bill No. · ${bill.billNo}",
                                emphasized = true
                            )
                            InfoValueRow(
                                Icons.Rounded.CalendarMonth,
                                "Bill Date · ${billingDisplayDate(bill.billDate)}"
                            )
                        }
                        StatusBadge(
                            if (bill.status == "FINAL") "Finalized" else bill.status.lowercase().replaceFirstChar(Char::uppercase),
                            when (bill.status) {
                                "FINAL" -> BadgeTone.SUCCESS
                                "CANCELLED" -> BadgeTone.ERROR
                                else -> BadgeTone.WARNING
                            }
                        )
                    }
                }
            }

            item {
                LabeledSectionCard(title = "Booking & Customer") {
                    InfoValueRow(Icons.Rounded.Person, bill.customerName, emphasized = true)
                    InfoValueRow(Icons.Rounded.Phone, bill.customerMobile)
                    if (bill.customerAddress.isNotBlank()) {
                        InfoValueRow(Icons.Rounded.LocationOn, bill.customerAddress, muted = true)
                    }
                    InfoValueRow(Icons.Rounded.ReceiptLong, bill.bookingNo ?: "Legacy standalone")
                }
            }

            item {
                LabeledSectionCard(title = "Items (${detail.items.size})") {
                    detail.items.forEachIndexed { index, line ->
                        AppItemDisplayRow(
                            imageUrl = line.imageUrl,
                        imageUrls = line.imageUrls,
                            itemName = line.itemName,
                            itemCode = line.itemCode,
                            categoryName = line.categoryName,
                            quantity = line.quantity,
                            statusLine = "Qty ${line.quantity} × ₹${line.rentRate} · Amount ₹${line.amount}"
                        )
                        if (index != detail.items.lastIndex) AppItemListDivider()
                    }
                }
            }

            item {
                val otherReceived = (bill.receivedAmount - bill.advanceAmount).coerceAtLeast(0)
                LabeledSectionCard(title = "Amount Summary") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        StatusBadge(billingPaymentLabel(bill.paymentStatus), billingPaymentTone(bill.paymentStatus))
                    }
                    BillingAmountRow("Item Total", bill.totalRent)
                    BillingAmountRow("− Discount", bill.discountAmount)
                    BillingAmountRow("Bill Amount", bill.netAmount, strong = true)
                    BillingAmountRow("Advance Received", bill.advanceAmount)
                    BillingAmountRow("Other Received", otherReceived)
                    BillingAmountRow("Total Received", bill.receivedAmount, strong = true)
                    AppItemListDivider()
                    BillingBalanceDue(bill.balanceAmount)
                }
            }

            item {
                LabeledSectionCard(title = "Notes") {
                    Text(
                        bill.notes.ifBlank { "No notes." },
                        color = if (bill.notes.isBlank()) AppTextMuted else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (bill.status == "DRAFT" || !bill.billNo.isNullOrBlank()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        ResponsiveCompactPair(
                            first = { child ->
                                SecondaryButton(
                                    text = "View",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, detail)
                                            Screen8PdfExporter.view(context, file)
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, if (bill.status == "DRAFT") "Unable to open Quotation PDF." else "Unable to open Bill PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            },
                            second = { child ->
                                SecondaryButton(
                                    text = "Share",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, detail)
                                            Screen8PdfExporter.shareDirectToCustomerWhatsApp(
                                                context = context,
                                                file = file,
                                                customerMobile = bill.customerMobile,
                                                title = if (bill.status == "DRAFT") "Quotation ${bill.bookingNo ?: ""}" else "Final Bill ${bill.billNo}"
                                            )
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, if (bill.status == "DRAFT") "Unable to share Quotation PDF." else "Unable to share Bill PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        )
                        ResponsiveCompactPair(
                            first = { child ->
                                SecondaryButton(
                                    text = "Download",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, detail)
                                            Screen8PdfExporter.download(context, file)
                                        }.onSuccess { onPdfResult(it, null) }
                                            .onFailure { onPdfResult(null, if (bill.status == "DRAFT") "Unable to download Quotation PDF." else "Unable to download Bill PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            },
                            second = { child ->
                                SecondaryButton(
                                    text = "Print",
                                    onClick = {
                                        runCatching {
                                            val file = Screen11BillingPdfExporter.create(context, branding, detail)
                                            Screen8PdfExporter.print(context, file, if (bill.status == "DRAFT") "Quotation ${bill.bookingNo ?: ""}" else "Final Bill ${bill.billNo}")
                                        }.onSuccess { onPdfResult(null, null) }
                                            .onFailure { onPdfResult(null, if (bill.status == "DRAFT") "Unable to print Quotation PDF." else "Unable to print Bill PDF.") }
                                    },
                                    modifier = child,
                                    icon = { Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingCompactDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Bill Date"
) {
    val context = LocalContext.current
    val selected = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }

    Surface(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onValueChange(LocalDate.of(year, month + 1, day).toString())
                },
                selected.year,
                selected.monthValue - 1,
                selected.dayOfMonth
            ).show()
        },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Text(
                    billingDisplayDate(value),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BillingLifecycleStatusBlock(
    title: String,
    done: Boolean,
    date: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xxs, vertical = AppSpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        InfoValueRow(icon, title, emphasized = true)
        InfoValueRow(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.CalendarMonth,
            if (done) "Done" else "Pending"
        )
        InfoValueRow(Icons.Rounded.CalendarMonth, billingDisplayDate(date))
        InfoValueRow(Icons.Rounded.EventNote, billingDisplayDay(date), muted = true)
    }
}

private fun billingPickupDone(status: String): Boolean =
    status.uppercase() in setOf("GIVEN", "PARTIALLY_RETURNED", "RETURNED", "FULL_PICKUP", "PART_RETURN", "FULL_RETURN")

private fun billingPickupDoneFromDatesAndReturn(bill: BillingBill): Boolean =
    bill.returnComplete || bill.status == "FINAL" || bill.status == "CANCELLED"

@Composable
private fun BillingBalanceDue(amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Balance Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "₹$amount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (amount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

private fun billingDisplayDay(value: String): String =
    runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEEE"))
    }.getOrDefault(value)

private val billingDisplayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun billingDisplayDate(value: String): String =
    runCatching { LocalDate.parse(value).format(billingDisplayDateFormatter) }.getOrDefault(value)

private fun billingPaymentLabel(status: String): String = when (status.uppercase()) {
    "FULL_AMOUNT_RECEIVED" -> "Full Payment"
    "PART_RECEIVED" -> "Part Payment"
    else -> "Pending Payment"
}

private fun billingPaymentTone(status: String): BadgeTone = when (status.uppercase()) {
    "FULL_AMOUNT_RECEIVED" -> BadgeTone.SUCCESS
    "PART_RECEIVED" -> BadgeTone.INFO
    else -> BadgeTone.WARNING
}

@Composable
private fun BillingAmountRow(label: String, amount: Int, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (strong) MaterialTheme.colorScheme.onSurface else AppTextMuted)
        Text(
            "₹$amount",
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun BillingWhatsAppDialog(
    bundle: BillingWhatsAppBundle,
    onDismiss: () -> Unit,
    onLaunchError: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WhatsApp · ${bundle.customerName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                if (bundle.templates.isEmpty()) {
                    Text("No Billing WhatsApp template is available.")
                } else {
                    bundle.templates.forEach { template ->
                        AppCard(Modifier.fillMaxWidth()) {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            Text(template.message, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.size(AppSpacing.xs))
                            PrimaryButton(
                                text = "Open WhatsApp",
                                onClick = {
                                    if (launchCustomerWhatsApp(context, bundle.mobile, template.message)) onDismiss()
                                    else onLaunchError("Unable to open WhatsApp.")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
