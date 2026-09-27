package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGiven
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGivenSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItems
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItemsSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft

private enum class CustomerConfirmAction { ARCHIVE, RESTORE, DELETE_PERMANENT }
private enum class CustomerContactAction { CALL, WHATSAPP }
private data class PendingWhatsAppSelection(val customer: CustomerCardRow, val targetMobile: String)

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
    var contactChoice by remember { mutableStateOf<Pair<CustomerContactAction, CustomerCardRow>?>(null) }
    var bookingSelectCustomer by remember { mutableStateOf<PendingWhatsAppSelection?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    fun launchDial(mobile: String) {
        val digits = mobile.filter(Char::isDigit)
        if (digits.length == 10) {
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
        } else {
            Toast.makeText(context, "Valid mobile number is not available.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(customer: CustomerCardRow, bookingId: String?, targetMobile: String) {
        onWhatsApp(customer.id, bookingId) { _, message ->
            val digits = targetMobile.filter(Char::isDigit)
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

    fun beginWhatsApp(customer: CustomerCardRow, targetMobile: String) {
        when {
            customer.bookingOptions.size > 1 -> bookingSelectCustomer = PendingWhatsAppSelection(customer, targetMobile)
            customer.bookingOptions.size == 1 -> openWhatsApp(customer, customer.bookingOptions.first().id, targetMobile)
            else -> openWhatsApp(customer, null, targetMobile)
        }
    }

    fun executeContact(action: CustomerContactAction, customer: CustomerCardRow, targetMobile: String) {
        when (action) {
            CustomerContactAction.CALL -> launchDial(targetMobile)
            CustomerContactAction.WHATSAPP -> beginWhatsApp(customer, targetMobile)
        }
    }

    fun requestContact(action: CustomerContactAction, customer: CustomerCardRow) {
        if (!customer.alternateMobile.isNullOrBlank()) {
            contactChoice = action to customer
        } else {
            executeContact(action, customer, customer.mobile)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.md)
                    .padding(bottom = if (state.loaded && state.items.isEmpty()) 72.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                item { Spacer(Modifier.height(AppSpacing.xxs)) }
                item { MainScreenDateRow(businessDate) }
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Customers", style = MaterialTheme.typography.headlineMedium)
                        if (!state.archived) {
                            Surface(
                                onClick = { newFormOpen = true },
                                enabled = !state.actionBusy,
                                color = BrandSoft,
                                contentColor = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("New Customer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (canAdmin) item {
                    TabRow(selectedTabIndex = if (state.archived) 1 else 0) {
                        Tab(!state.archived, { if (state.archived) onArchivedTab(false) }, text = { Text("Active") })
                        Tab(state.archived, { if (!state.archived) onArchivedTab(true) }, text = { Text("Archived") })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTextField(
                            value = state.search,
                            onValueChange = onSearch,
                            label = "Search name or mobile",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                        )
                        Box {
                            SoftActionButton(
                                icon = Icons.Rounded.Sort,
                                contentDescription = "Sort",
                                onClick = { sortMenuOpen = true },
                                enabled = !state.loading,
                                tone = ActionTone.INFO
                            )
                            DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                                listOf(
                                    "NAME_ASC" to "Name A–Z",
                                    "NAME_DESC" to "Name Z–A",
                                    "NEWEST" to "Newest",
                                    "OLDEST" to "Oldest"
                                ).forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(if (state.sort == key) "✓  $label" else label) },
                                        onClick = {
                                            sortMenuOpen = false
                                            onSort(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.loading && !state.loaded) item { LoadingState() }
                if (!state.notice.isNullOrBlank()) item { InlineMessage(state.notice, false) }
                if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, true) }
                if (!state.loaded && !state.loading && !state.error.isNullOrBlank()) item {
                    SecondaryButton("Retry", onRefresh)
                }

                if (state.loaded && state.items.isEmpty()) item {
                    EmptyState(if (state.archived) "No archived customers" else "No customers found")
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
                        onCall = { requestContact(CustomerContactAction.CALL, customer) },
                        onWhatsApp = { requestContact(CustomerContactAction.WHATSAPP, customer) },
                        onCreateBooking = { onCreateBooking(customer) }
                    )
                }

                if (state.loaded && state.items.isNotEmpty()) item {
                    CustomerPaginationRow(
                        page = state.page,
                        totalPages = state.totalPages,
                        busy = state.loading,
                        onPage = onPage
                    )
                }
                item { Spacer(Modifier.height(AppSpacing.md)) }
            }

            if (state.loaded && state.items.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomerPaginationRow(
                        page = state.page,
                        totalPages = state.totalPages,
                        busy = state.loading,
                        onPage = onPage,
                        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    )
                }
            }
        }
    }

    if (newFormOpen) CustomerFormDialogV3(
        customer = null,
        busy = state.actionBusy,
        onDismiss = { newFormOpen = false },
        onSave = { name, mobile, alt, address ->
            onSave(null, name, mobile, alt, address) { newFormOpen = false }
        }
    )

    formCustomer?.let { customer ->
        CustomerFormDialogV3(
            customer = customer,
            busy = state.actionBusy,
            onDismiss = { formCustomer = null },
            onSave = { name, mobile, alt, address ->
                onSave(customer.id, name, mobile, alt, address) { formCustomer = null }
            }
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
                    } else {
                        Text(if (action == CustomerConfirmAction.RESTORE) "Restore" else "Delete")
                    }
                }
            },
            dismissButton = {
                TextButton({ confirmAction = null }, enabled = !state.actionBusy) { Text("Cancel") }
            }
        )
    }

    contactChoice?.let { (action, customer) ->
        val alternate = customer.alternateMobile.orEmpty()
        val contactSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val contactIcon = if (action == CustomerContactAction.CALL) Icons.Rounded.Call else Icons.Rounded.Chat
        val contactSoft = if (action == CustomerContactAction.CALL) StatusSuccessSoft else DashboardItemsSoft
        val contactForeground = if (action == CustomerContactAction.CALL) StatusSuccess else DashboardItems

        ModalBottomSheet(
            onDismissRequest = { contactChoice = null },
            sheetState = contactSheetState
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .padding(horizontal = AppSpacing.md)
                        .padding(bottom = AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (action == CustomerContactAction.CALL) "Choose number to call" else "Choose WhatsApp number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        SoftActionButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Close",
                            onClick = { contactChoice = null },
                            tone = ActionTone.NEUTRAL
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            contactChoice = null
                            executeContact(action, customer, customer.mobile)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = AppSpacing.sm, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Surface(color = contactSoft, contentColor = contactForeground, shape = MaterialTheme.shapes.small) {
                                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Icon(contactIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Primary", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                                Text(customer.mobile, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            contactChoice = null
                            executeContact(action, customer, alternate)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = AppSpacing.sm, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Surface(color = contactSoft, contentColor = contactForeground, shape = MaterialTheme.shapes.small) {
                                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Icon(contactIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Alternative", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                                Text(alternate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { contactChoice = null }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    bookingSelectCustomer?.let { pending ->
        val customer = pending.customer
        AlertDialog(
            onDismissRequest = { bookingSelectCustomer = null },
            title = { Text("Select booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    customer.bookingOptions.forEach { booking ->
                        OutlinedButton(
                            onClick = {
                                bookingSelectCustomer = null
                                openWhatsApp(customer, booking.id, pending.targetMobile)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(booking.bookingNo, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${booking.displayStatus.replace('_', ' ')} · ${booking.pickupDate} → ${booking.returnDate}",
                                    style = MaterialTheme.typography.labelSmall
                                )
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
private fun CustomerPaginationRow(
    page: Int,
    totalPages: Int,
    busy: Boolean,
    onPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryButton("Previous", { onPage(page - 1) }, enabled = !busy && page > 1)
        Spacer(Modifier.width(AppSpacing.xs))
        Text("Page $page of $totalPages", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(AppSpacing.xs))
        SecondaryButton("Next", { onPage(page + 1) }, enabled = !busy && page < totalPages)
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .30f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary else AppBorder),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.Top
            ) {
                CustomerInfoRow(
                    icon = Icons.Rounded.Person,
                    label = "Name",
                    value = customer.name,
                    iconBackground = BrandSoft,
                    iconForeground = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    emphasize = true
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge("Total Booking · ${customer.totalBookings}", BadgeTone.INFO)
                    StatusBadge("Active Booking · ${customer.activeBookings}", BadgeTone.SUCCESS)
                }
            }

            CustomerInfoRow(
                icon = Icons.Rounded.Call,
                label = "Mobile Number",
                value = customer.mobile,
                iconBackground = StatusInfoSoft,
                iconForeground = StatusInfo
            )
            if (!customer.alternateMobile.isNullOrBlank()) {
                CustomerInfoRow(
                    icon = Icons.Rounded.Call,
                    label = "Alternative Mobile Number",
                    value = customer.alternateMobile,
                    iconBackground = DashboardItemsSoft,
                    iconForeground = DashboardItems
                )
            }
            if (!customer.address.isNullOrBlank()) {
                CustomerInfoRow(
                    icon = Icons.Rounded.LocationOn,
                    label = "Address",
                    value = customer.address,
                    iconBackground = DashboardGivenSoft,
                    iconForeground = DashboardGiven
                )
            }

            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!archived) {
                    SoftActionButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.INFO
                    )
                    if (canAdmin) {
                        SoftActionButton(
                            icon = Icons.Rounded.Delete,
                            label = "Delete",
                            onClick = onArchive,
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            tone = ActionTone.DANGER
                        )
                    }
                } else if (canAdmin) {
                    SoftActionButton(
                        icon = Icons.Rounded.Restore,
                        label = "Restore",
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.SUCCESS
                    )
                    SoftActionButton(
                        icon = Icons.Rounded.DeleteForever,
                        label = "Delete",
                        contentDescription = "Delete permanently",
                        onClick = onDeletePermanent,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.DANGER
                    )
                }

                SoftActionButton(
                    icon = Icons.Rounded.Call,
                    label = "Call",
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.SUCCESS
                )
                SoftActionButton(
                    icon = Icons.Rounded.Chat,
                    label = "WhatsApp",
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.TEAL
                )
                if (!archived) {
                    SoftActionButton(
                        icon = Icons.Rounded.Add,
                        label = "Booking",
                        contentDescription = "New Booking",
                        onClick = onCreateBooking,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        tone = ActionTone.PURPLE
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconBackground: Color,
    iconForeground: Color,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(color = iconBackground, contentColor = iconForeground, shape = MaterialTheme.shapes.small) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dirty = name != initialName || mobile != initialMobile || alternate != initialAlt || address != initialAddress
    val valid = name.trim().isNotBlank() && mobile.length == 10 && (alternate.isBlank() || alternate.length == 10)
    val requestDismiss = { if (dirty) confirmDiscard = true else onDismiss() }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) requestDismiss() },
        sheetState = sheetState
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .imePadding()
                    .padding(horizontal = AppSpacing.md)
                    .padding(bottom = AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (customer == null) "New Customer" else "Edit Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    SoftActionButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        onClick = requestDismiss,
                        enabled = !busy,
                        tone = ActionTone.NEUTRAL
                    )
                }

                AppTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = "Name",
                    enabled = !busy,
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AppTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                    label = "Mobile Number",
                    enabled = !busy,
                    leadingIcon = { Icon(Icons.Rounded.Call, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    isError = mobile.isNotEmpty() && mobile.length != 10,
                    supportingText = if (mobile.isNotEmpty() && mobile.length != 10) "10 digits required" else null
                )
                AppTextField(
                    value = alternate,
                    onValueChange = { alternate = it.filter(Char::isDigit).take(10) },
                    label = "Alternative Mobile Number",
                    enabled = !busy,
                    leadingIcon = { Icon(Icons.Rounded.Call, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    isError = alternate.isNotEmpty() && alternate.length != 10,
                    supportingText = if (alternate.isNotEmpty() && alternate.length != 10) "10 digits required" else null
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it.take(500) },
                    label = "Address",
                    enabled = !busy,
                    singleLine = false,
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    minLines = 2,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = requestDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !busy
                    )
                    PrimaryButton(
                        text = "Save",
                        onClick = { onSave(name.trim(), mobile, alternate, address.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = valid && !busy,
                        loading = busy,
                        icon = { Icon(Icons.Rounded.Save, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            confirmButton = {
                TextButton({
                    confirmDiscard = false
                    onDismiss()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep editing") } }
        )
    }
}
