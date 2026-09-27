package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCompactFixedTabs
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppScreenTitle
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InfoValueRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreenV4(
    state: CustomerScreenState,
    role: UserRole,
    businessDate: String,
    onSearch: (String) -> Unit,
    onSort: (String) -> Unit,
    onArchivedTab: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSave: (String?, String, String, String, String, () -> Unit) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeletePermanent: (String) -> Unit,
    onCreateBooking: (CustomerCardRow) -> Unit,
    onWhatsApp: (String, String?, String?, (String, String) -> Unit) -> Unit,
    canCreateBooking: Boolean = true,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var formCustomer by remember { mutableStateOf<CustomerCardRow?>(null) }
    var showNewCustomer by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf<CustomerCardRow?>(null) }
    var confirmRestore by remember { mutableStateOf<CustomerCardRow?>(null) }
    var confirmDelete by remember { mutableStateOf<CustomerCardRow?>(null) }
    var confirmCall by remember { mutableStateOf<CustomerCardRow?>(null) }
    var contactLaunchError by remember { mutableStateOf<String?>(null) }
    var localQuery by rememberSaveable { mutableStateOf(state.search) }

    LaunchedEffect(state.search) {
        if (state.search != localQuery) localQuery = state.search
    }
    LaunchedEffect(role, state.archived) {
        if (role == UserRole.STAFF && state.archived) onArchivedTab(false)
    }
    val archivedRestricted = role == UserRole.STAFF && state.archived

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
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
                    AppScreenTitle("Customers", modifier = Modifier.weight(1f))
                    CompactNewActionButton(
                        label = "New Customer",
                        enabled = !state.actionBusy,
                        onClick = { showNewCustomer = true }
                    )
                }
            }
            if (role == UserRole.OWNER) {
                item {
                    AppCompactFixedTabs(
                        labels = listOf("Active", "Archived"),
                        selectedIndex = if (state.archived) 1 else 0,
                        onSelect = { index -> onArchivedTab(index == 1) }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactSearchField(
                        value = localQuery,
                        onValueChange = { localQuery = it; onSearch(it) },
                        placeholder = "Search name or mobile",
                        modifier = Modifier.weight(1f),
                        searching = state.loading && state.loaded && localQuery.isNotBlank()
                    )
                    Box {
                        SoftActionButton(
                            icon = Icons.Rounded.Sort,
                            contentDescription = "Sort",
                            onClick = { sortOpen = true },
                            tone = ActionTone.INFO
                        )
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            listOf(
                                "NAME_ASC" to "Name A-Z",
                                "NAME_DESC" to "Name Z-A",
                                "NEWEST" to "Newest",
                                "OLDEST" to "Oldest"
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (state.sort == value) "✓ $label" else label) },
                                    onClick = { sortOpen = false; onSort(value) }
                                )
                            }
                        }
                    }
                }
            }

            if (!state.loadError.isNullOrBlank() && !state.loaded) {
                item {
                    LoadFailureState(
                        message = state.loadError.orEmpty(),
                        onRetry = onRefresh
                    )
                }
            } else if (!state.loadError.isNullOrBlank()) {
                item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
            }
            if (state.loading && !state.loaded) item { LoadingState() }
            if (state.loaded && state.items.isEmpty() && !state.loading && state.loadError.isNullOrBlank()) {
                item { EmptyState(if (state.archived) "No archived customers." else "No customers found.") }
            }

            if (archivedRestricted) {
                item { LoadingState() }
            }
            state.items.filter { !archivedRestricted }.forEach { customer ->
                item(key = customer.id) {
                    CustomerCard4(
                        customer = customer,
                        archived = state.archived,
                        canManage = role != UserRole.STAFF,
                        canBook = canCreateBooking,
                        busy = state.actionBusy,
                        onEdit = { formCustomer = customer },
                        onCall = { confirmCall = customer },
                        onWhatsAppClick = {
                            onWhatsApp(customer.id, null, null) { _, message ->
                                if (!launchCustomerWhatsApp(context, customer.mobile, message)) {
                                    contactLaunchError = "Unable to open WhatsApp."
                                }
                            }
                        },
                        onBooking = { onCreateBooking(customer) },
                        onArchive = { confirmArchive = customer },
                        onRestore = { confirmRestore = customer },
                        onPermanentDelete = { confirmDelete = customer }
                    )
                }
            }

            if (state.canLoadMore) {
                item(key = "customer-load-more-${state.page}") {
                    LaunchedEffect(state.page, state.items.size) { onLoadMore() }
                    LoadingState()
                }
            }
            if (!state.loadMoreError.isNullOrBlank()) {
                item(key = "customer-load-more-retry-${state.page}") {
                    InlineRetryMessage(
                        message = state.loadMoreError.orEmpty(),
                        onRetry = onRetryLoadMore,
                        retryLabel = "Retry loading more"
                    )
                }
            }
            item { Spacer(Modifier.height(AppSpacing.lg)) }
        }
    }

    AppFeedbackHost(
        successMessage = state.notice,
        errorMessage = contactLaunchError ?: state.error.takeIf { !showNewCustomer && formCustomer == null }
    )

    confirmCall?.let { customer ->
        CustomerContactConfirmationSheet(
            customerName = customer.name,
            mobile = customer.mobile,
            action = CustomerContactAction.CALL,
            onDismiss = { confirmCall = null },
            onConfirm = {
                confirmCall = null
                if (!launchCustomerCall(context, customer.mobile)) {
                    contactLaunchError = "Unable to open the Phone app."
                }
            }
        )
    }

    if (showNewCustomer || formCustomer != null) {
        val selected = formCustomer
        CustomerFormSheet(
            title = if (selected == null) "New Customer" else "Edit Customer",
            initialName = selected?.name.orEmpty(),
            initialMobile = selected?.mobile.orEmpty(),
            initialAddress = selected?.address.orEmpty(),
            busy = state.actionBusy,
            error = state.error,
            onDismiss = {
                if (!state.actionBusy) {
                    showNewCustomer = false
                    formCustomer = null
                }
            },
            onSave = { name, mobile, _, address ->
                onSave(selected?.id, name, mobile, "", address) {
                    showNewCustomer = false
                    formCustomer = null
                }
            }
        )
    }

    confirmArchive?.let { customer ->
        AppDestructiveConfirmDialog(
            title = "Archive customer?",
            message = "Archive ${customer.name}? The customer will move to Archived. Existing booking history is preserved and can be restored later.",
            confirmLabel = "Archive",
            busy = state.actionBusy,
            onConfirm = {
                confirmArchive = null
                onArchive(customer.id)
            },
            onDismiss = { confirmArchive = null }
        )
    }

    confirmRestore?.let { customer ->
        AppConfirmDialog(
            title = "Restore customer?",
            message = "Restore ${customer.name} as an active customer? Existing booking history will become visible again.",
            confirmLabel = "Restore",
            busy = state.actionBusy,
            onConfirm = {
                confirmRestore = null
                onRestore(customer.id)
            },
            onDismiss = { confirmRestore = null }
        )
    }

    confirmDelete?.let { customer ->
        AppDestructiveConfirmDialog(
            title = "Permanently delete customer?",
            message = "This permanently removes ${customer.name} and eligible booking/lifecycle history. Billing financial history is preserved. This cannot be undone.",
            confirmLabel = "Delete permanently",
            requiredPhrase = "DELETE CUSTOMER",
            busy = state.actionBusy,
            onConfirm = {
                confirmDelete = null
                onDeletePermanent(customer.id)
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun CustomerBookingStatBadge(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (active) StatusSuccessSoft else StatusInfoSoft,
        contentColor = if (active) StatusSuccess else StatusInfo,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CustomerCard4(
    customer: CustomerCardRow,
    archived: Boolean,
    canManage: Boolean,
    canBook: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onBooking: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InfoValueRow(
                icon = Icons.Rounded.Person,
                value = customer.name,
                contentDescription = "Name",
                emphasized = true
            )
            InfoValueRow(
                icon = Icons.Rounded.Call,
                value = customer.mobile,
                contentDescription = "Mobile",
                muted = true
            )
            if (!customer.address.isNullOrBlank()) {
                InfoValueRow(
                    icon = Icons.Rounded.LocationOn,
                    value = customer.address,
                    contentDescription = "Address",
                    muted = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                CustomerBookingStatBadge(
                    text = "Total Booking · ${customer.totalBookings}",
                    active = false,
                    modifier = Modifier.weight(1f)
                )
                CustomerBookingStatBadge(
                    text = "Active Booking · ${customer.activeBookings}",
                    active = customer.activeBookings > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))
        if (!archived) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SoftActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Edit",
                    onClick = onEdit,
                    enabled = !busy,
                    tone = ActionTone.INFO,
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    SoftActionButton(
                        icon = Icons.Rounded.Delete,
                        label = "Archive",
                        onClick = onArchive,
                        enabled = !busy,
                        tone = ActionTone.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canBook) {
                    SoftActionButton(
                        icon = Icons.Rounded.EventNote,
                        label = "Booking",
                        onClick = onBooking,
                        enabled = !busy,
                        tone = ActionTone.PURPLE,
                        modifier = Modifier.weight(1f)
                    )
                }
                SoftActionButton(
                    icon = Icons.Rounded.Call,
                    label = "Call",
                    onClick = onCall,
                    enabled = !busy,
                    tone = ActionTone.SUCCESS,
                    modifier = Modifier.weight(1f)
                )
                SoftActionButton(
                    icon = Icons.Rounded.Chat,
                    label = "WhatsApp",
                    onClick = onWhatsAppClick,
                    enabled = !busy,
                    tone = ActionTone.TEAL,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (canManage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                SoftActionButton(
                    icon = Icons.Rounded.CheckCircle,
                    label = "Restore",
                    onClick = onRestore,
                    enabled = !busy,
                    tone = ActionTone.SUCCESS,
                    modifier = Modifier.weight(1f)
                )
                SoftActionButton(
                    icon = Icons.Rounded.Delete,
                    label = "Delete",
                    onClick = onPermanentDelete,
                    enabled = !busy,
                    tone = ActionTone.DANGER,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
