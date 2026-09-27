package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted

private enum class CustomerConfirmAction { ARCHIVE, RESTORE, DELETE_PERMANENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreenV3(
    state: CustomerScreenState,
    role: UserRole,
    businessDate: String,
    onSearch: (String) -> Unit,
    onSort: (String) -> Unit,
    onArchivedTab: (Boolean) -> Unit,
    onPage: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSave: (String?, String, String, String, String, () -> Unit) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeletePermanent: (String) -> Unit,
    onCreateBooking: (CustomerCardRow) -> Unit,
    onWhatsApp: (String, String?, (String, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canAdmin = role != UserRole.STAFF
    var formCustomer by remember { mutableStateOf<CustomerCardRow?>(null) }
    var newFormOpen by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<Pair<CustomerConfirmAction, CustomerCardRow>?>(null) }
    var bookingSelectCustomer by remember { mutableStateOf<CustomerCardRow?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    fun openWhatsApp(customer: CustomerCardRow, bookingId: String?) {
        onWhatsApp(customer.id, bookingId) { mobile, message ->
            val digits = mobile.filter(Char::isDigit)
            val number = if (digits.length == 10) "91$digits" else digits
            if (number.isBlank()) {
                Toast.makeText(context, "Valid mobile number is not available.", Toast.LENGTH_SHORT).show()
                return@onWhatsApp
            }
            val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
            if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isFailure) {
                Toast.makeText(context, "Unable to open WhatsApp.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            item { Spacer(Modifier.height(AppSpacing.xxs)) }
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Customers", style = MaterialTheme.typography.headlineMedium)
                    CustomerIconAction("Refresh", !state.loading, onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            item { MainScreenDateRow(businessDate) }

            if (canAdmin) item {
                TabRow(selectedTabIndex = if (state.archived) 1 else 0) {
                    Tab(!state.archived, { if (state.archived) onArchivedTab(false) }, text = { Text("Active") })
                    Tab(state.archived, { if (!state.archived) onArchivedTab(true) }, text = { Text("Archived") })
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    CustomerSummaryCell("Total", state.summary.totalCustomers, Modifier.weight(1f))
                    CustomerSummaryCell("Active", state.summary.activeCustomers, Modifier.weight(1f))
                    CustomerSummaryCell("Archived", state.summary.archivedCustomers, Modifier.weight(1f))
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    AppTextField(
                        value = state.search,
                        onValueChange = onSearch,
                        label = "Search name or mobile",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                    Box {
                        OutlinedButton(onClick = { sortMenuOpen = true }) { Text("Sort: ${customerSortLabel(state.sort)}") }
                        DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                            listOf(
                                "NAME_ASC" to "Name A–Z",
                                "NAME_DESC" to "Name Z–A",
                                "NEWEST" to "Newest",
                                "OLDEST" to "Oldest"
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { sortMenuOpen = false; onSort(key) }
                                )
                            }
                        }
                    }
                    if (!state.archived) {
                        PrimaryButton("New Customer", { newFormOpen = true }, icon = { Icon(Icons.Rounded.Add, null) })
                    }
                }
            }

            if (state.loading && !state.loaded) item { LoadingState() }
            if (!state.notice.isNullOrBlank()) item { InlineMessage(state.notice, false) }
            if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, true) }
            if (!state.loaded && !state.loading && !state.error.isNullOrBlank()) item { SecondaryButton("Retry", onRefresh) }

            if (state.loaded && state.items.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    EmptyState(if (state.archived) "No archived customers" else "No customers found")
                    if (!state.archived) PrimaryButton("New Customer", { newFormOpen = true })
                }
            }

            items(state.items, key = { it.id }) { customer ->
                CustomerCardV3(
                    customer = customer,
                    highlighted = state.highlightId == customer.id,
                    archived = state.archived,
                    canAdmin = canAdmin,
                    busy = state.actionBusy,
                    onEdit = { formCustomer = customer },
                    onArchive = { confirmAction = CustomerConfirmAction.ARCHIVE to customer },
                    onRestore = { confirmAction = CustomerConfirmAction.RESTORE to customer },
                    onDeletePermanent = { confirmAction = CustomerConfirmAction.DELETE_PERMANENT to customer },
                    onCall = {
                        val mobile = customer.mobile.filter(Char::isDigit)
                        if (mobile.length == 10) runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$mobile")))
                        }
                    },
                    onWhatsApp = {
                        when {
                            customer.bookingOptions.size > 1 -> bookingSelectCustomer = customer
                            customer.bookingOptions.size == 1 -> openWhatsApp(customer, customer.bookingOptions.first().id)
                            else -> openWhatsApp(customer, null)
                        }
                    },
                    onCreateBooking = { onCreateBooking(customer) }
                )
            }

            if (state.loaded) item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
                    Arrangement.Center,
                    Alignment.CenterVertically
                ) {
                    SecondaryButton("Previous", { onPage(state.page - 1) }, enabled = !state.loading && state.page > 1)
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text("Page ${state.page} of ${state.totalPages}", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(AppSpacing.xs))
                    SecondaryButton("Next", { onPage(state.page + 1) }, enabled = !state.loading && state.page < state.totalPages)
                }
            }
            item { Spacer(Modifier.height(AppSpacing.md)) }
        }
    }

    if (newFormOpen) CustomerFormDialogV3(
        customer = null,
        busy = state.actionBusy,
        onDismiss = { newFormOpen = false },
        onSave = { name, mobile, alt, address -> onSave(null, name, mobile, alt, address) { newFormOpen = false } }
    )

    formCustomer?.let { customer ->
        CustomerFormDialogV3(
            customer = customer,
            busy = state.actionBusy,
            onDismiss = { formCustomer = null },
            onSave = { name, mobile, alt, address -> onSave(customer.id, name, mobile, alt, address) { formCustomer = null } }
        )
    }

    confirmAction?.let { (action, customer) ->
        val title = when (action) {
            CustomerConfirmAction.ARCHIVE -> "Delete customer?"
            CustomerConfirmAction.RESTORE -> "Restore customer?"
            CustomerConfirmAction.DELETE_PERMANENT -> "Delete permanently?"
        }
        val message = when (action) {
            CustomerConfirmAction.ARCHIVE -> "Customer and completed/cancelled booking history will be archived. Active bookings must be cancelled or completed first."
            CustomerConfirmAction.RESTORE -> "Restore this customer and the archived booking history?"
            CustomerConfirmAction.DELETE_PERMANENT -> "Customer, bookings, pickup/return history and related records will be permanently deleted."
        }
        AlertDialog(
            onDismissRequest = { if (!state.actionBusy) confirmAction = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    enabled = !state.actionBusy,
                    onClick = {
                        confirmAction = null
                        when (action) {
                            CustomerConfirmAction.ARCHIVE -> onArchive(customer.id)
                            CustomerConfirmAction.RESTORE -> onRestore(customer.id)
                            CustomerConfirmAction.DELETE_PERMANENT -> onDeletePermanent(customer.id)
                        }
                    }
                ) {
                    if (action == CustomerConfirmAction.DELETE_PERMANENT) {
                        Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                    } else Text(if (action == CustomerConfirmAction.RESTORE) "Restore" else "Delete")
                }
            },
            dismissButton = { TextButton({ confirmAction = null }, enabled = !state.actionBusy) { Text("Cancel") } }
        )
    }

    bookingSelectCustomer?.let { customer ->
        AlertDialog(
            onDismissRequest = { bookingSelectCustomer = null },
            title = { Text("Select booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    customer.bookingOptions.forEach { booking ->
                        OutlinedButton(
                            onClick = { bookingSelectCustomer = null; openWhatsApp(customer, booking.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(booking.bookingNo, fontWeight = FontWeight.SemiBold)
                                Text("${booking.displayStatus.replace('_', ' ')} · ${booking.pickupDate} → ${booking.returnDate}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ bookingSelectCustomer = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CustomerSummaryCell(label: String, value: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
    }
}

@Composable
private fun CustomerCardV3(
    customer: CustomerCardRow,
    highlighted: Boolean,
    archived: Boolean,
    canAdmin: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onCreateBooking: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .30f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary else AppBorder),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.md)) {
            Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(customer.mobile, style = MaterialTheme.typography.bodyMedium)
            if (!customer.alternateMobile.isNullOrBlank()) Text("Alt · ${customer.alternateMobile}", style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
            if (!customer.address.isNullOrBlank()) {
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(customer.address, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text("Total Bookings ${customer.totalBookings} · Active ${customer.activeBookings}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!archived) {
                    CustomerIconAction("Edit", !busy, onEdit) { Icon(Icons.Rounded.Edit, "Edit customer") }
                    if (canAdmin) CustomerIconAction("Delete", !busy, onArchive) { Icon(Icons.Rounded.Delete, "Delete customer") }
                } else if (canAdmin) {
                    CustomerIconAction("Restore", !busy, onRestore) { Icon(Icons.Rounded.Restore, "Restore customer") }
                    CustomerIconAction("Delete permanently", !busy, onDeletePermanent, danger = true) { Icon(Icons.Rounded.DeleteForever, "Delete permanently") }
                }
                CustomerIconAction("Call", !busy, onCall) { Icon(Icons.Rounded.Call, "Call customer") }
                CustomerIconAction("WhatsApp", !busy, onWhatsApp) { Icon(Icons.Rounded.Chat, "WhatsApp customer") }
            }
            if (!archived) {
                Spacer(Modifier.height(AppSpacing.xs))
                PrimaryButton("Create Booking", onCreateBooking, Modifier.fillMaxWidth(), enabled = !busy)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerIconAction(
    tooltip: String,
    enabled: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
    icon: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            if (danger) CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) { icon() }
            else icon()
        }
    }
}

@Composable
private fun CustomerFormDialogV3(
    customer: CustomerCardRow?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val initialName = customer?.name.orEmpty()
    val initialMobile = customer?.mobile.orEmpty()
    val initialAlt = customer?.alternateMobile.orEmpty()
    val initialAddress = customer?.address.orEmpty()
    var name by rememberSaveable(customer?.id) { mutableStateOf(initialName) }
    var mobile by rememberSaveable(customer?.id) { mutableStateOf(initialMobile) }
    var alternate by rememberSaveable(customer?.id) { mutableStateOf(initialAlt) }
    var address by rememberSaveable(customer?.id) { mutableStateOf(initialAddress) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val dirty = name != initialName || mobile != initialMobile || alternate != initialAlt || address != initialAddress
    val valid = name.trim().isNotBlank() && mobile.length == 10 && (alternate.isBlank() || alternate.length == 10)
    val requestDismiss = { if (dirty) confirmDiscard = true else onDismiss() }

    AlertDialog(
        onDismissRequest = { if (!busy) requestDismiss() },
        title = { Text(if (customer == null) "New Customer" else "Edit Customer") },
        text = {
            Column(Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                AppTextField(name, { name = it.take(120) }, "Name", enabled = !busy)
                AppTextField(
                    mobile,
                    { mobile = it.filter(Char::isDigit).take(10) },
                    "Mobile Number",
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    isError = mobile.isNotEmpty() && mobile.length != 10,
                    supportingText = if (mobile.isNotEmpty() && mobile.length != 10) "10 digits required" else null
                )
                AppTextField(
                    alternate,
                    { alternate = it.filter(Char::isDigit).take(10) },
                    "Alternative Mobile Number",
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    isError = alternate.isNotEmpty() && alternate.length != 10,
                    supportingText = if (alternate.isNotEmpty() && alternate.length != 10) "10 digits required" else null
                )
                AppTextField(address, { address = it.take(500) }, "Address", enabled = !busy, singleLine = false)
            }
        },
        confirmButton = { TextButton({ onSave(name.trim(), mobile, alternate, address.trim()) }, enabled = valid && !busy) { Text("Save") } },
        dismissButton = { TextButton(requestDismiss, enabled = !busy) { Text("Cancel") } }
    )

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard changes?") },
        confirmButton = { TextButton({ confirmDiscard = false; onDismiss() }) { Text("Discard") } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep editing") } }
    )
}

private fun customerSortLabel(sort: String): String = when (sort) {
    "NAME_DESC" -> "Name Z–A"
    "NEWEST" -> "Newest"
    "OLDEST" -> "Oldest"
    else -> "Name A–Z"
}
