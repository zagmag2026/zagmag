package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailItem
import com.nimsdeveloper.zhagmagdresses.admin.data.Customer
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppPageHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted

private enum class CompactDestination { DASHBOARD, CUSTOMERS, BOOKINGS, MORE }

@Composable
fun AdminAppCompact(adminViewModel: AdminViewModel, bookingViewModel: BookingLifecycleViewModel) {
    when (val auth = adminViewModel.authState) {
        AuthState.Checking, AuthState.SignedOut, is AuthState.SessionCheckFailed -> AdminApp(adminViewModel)
        is AuthState.SignedIn -> CompactAuthenticatedApp(auth.user, adminViewModel, bookingViewModel)
    }
}

@Composable
private fun CompactAuthenticatedApp(
    user: SessionUser,
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel
) {
    var destination by rememberSaveable { mutableStateOf(CompactDestination.DASHBOARD) }
    var consumedMutationVersion by rememberSaveable { mutableStateOf(bookingViewModel.mutationVersion) }
    val dashboardScreenState = rememberDashboardScreenState()

    LaunchedEffect(destination, bookingViewModel.editorOpen, bookingViewModel.mutationVersion) {
        if (!bookingViewModel.editorOpen) {
            when (destination) {
                CompactDestination.CUSTOMERS -> adminViewModel.ensureCustomers()
                CompactDestination.BOOKINGS -> bookingViewModel.ensureList()
                CompactDestination.DASHBOARD -> {
                    if (bookingViewModel.mutationVersion != consumedMutationVersion) {
                        consumedMutationVersion = bookingViewModel.mutationVersion
                        adminViewModel.loadDashboard()
                    }
                }
                CompactDestination.MORE -> Unit
            }
        }
    }

    if (bookingViewModel.editorOpen) {
        BookingEditorScreen(bookingViewModel)
        return
    }

    val businessDate = adminViewModel.dashboardState.data?.today.orEmpty()

    Scaffold(
        topBar = {
            AdminMainTopBar(
                branding = adminViewModel.branding,
                user = user,
                onLogout = adminViewModel::logout
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(destination == CompactDestination.DASHBOARD, { destination = CompactDestination.DASHBOARD }, { Icon(Icons.Rounded.Dashboard, null) }, label = { Text("Dashboard") })
                NavigationBarItem(destination == CompactDestination.CUSTOMERS, { destination = CompactDestination.CUSTOMERS }, { Icon(Icons.Rounded.People, null) }, label = { Text("Customers") })
                NavigationBarItem(destination == CompactDestination.BOOKINGS, { destination = CompactDestination.BOOKINGS }, { Icon(Icons.Rounded.EventNote, null) }, label = { Text("Bookings") })
                NavigationBarItem(destination == CompactDestination.MORE, { destination = CompactDestination.MORE }, { Icon(Icons.Rounded.Menu, null) }, label = { Text("More") })
            }
        }
    ) { innerPadding ->
        when (destination) {
            CompactDestination.DASHBOARD -> DashboardScreenV2(
                state = adminViewModel.dashboardState,
                revision = adminViewModel.dashboardRevision,
                uiState = dashboardScreenState,
                shopName = adminViewModel.branding.shopName,
                onRefresh = adminViewModel::loadDashboard,
                onNewBooking = bookingViewModel::openNew,
                onOpenBooking = bookingViewModel::openBooking,
                modifier = Modifier.padding(innerPadding)
            )
            CompactDestination.CUSTOMERS -> CompactCustomerScreen(
                state = adminViewModel.customerState,
                onSearch = adminViewModel::searchCustomers,
                onPage = adminViewModel::customerPage,
                onRefresh = adminViewModel::refreshCustomers,
                businessDate = businessDate,
                modifier = Modifier.padding(innerPadding)
            )
            CompactDestination.BOOKINGS -> CompactBookingScreen(bookingViewModel, businessDate, Modifier.padding(innerPadding))
            CompactDestination.MORE -> CompactMoreScreen(user.role, businessDate, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun CompactCustomerScreen(
    state: PagedState<Customer>,
    onSearch: (String) -> Unit,
    onPage: (Int) -> Unit,
    onRefresh: () -> Unit,
    businessDate: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        item {
            Spacer(Modifier.height(AppSpacing.xxs))
            AppPageHeader("Customer Master", "Customers", action = { SecondaryButton("Refresh", onRefresh, enabled = !state.loading) })
        }
        item { MainScreenDateRow(businessDate) }
        item {
            AppTextField(
                value = query,
                onValueChange = { value ->
                    query = value
                    val normalized = value.trim()
                    when {
                        normalized.isEmpty() || normalized.length >= 2 -> onSearch(normalized)
                        state.search.isNotEmpty() -> onSearch("")
                    }
                },
                label = "Search customer",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (state.loading && !state.loaded) item { LoadingState() }
        if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, true) }
        if (state.loaded && state.items.isEmpty()) item { AppCard { EmptyState("No records found.") } }
        items(state.items, key = { it.id }) { customer ->
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(customer.name, style = MaterialTheme.typography.titleSmall)
                        Text(customer.mobile, style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
                    }
                    StatusBadge(if (customer.isActive) "ACTIVE" else "INACTIVE", if (customer.isActive) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
                }
                Spacer(Modifier.height(AppSpacing.xs))
                Text("Bookings ${customer.totalBookings} · Active ${customer.activeBookings}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Spacer(Modifier.height(AppSpacing.xs))
                CompactContactActions(context, customer.mobile)
            }
        }
        if (state.loaded) item { CompactPager(state.page, state.totalPages, state.loading, onPage) }
        item { Spacer(Modifier.height(AppSpacing.md)) }
    }
}

@Composable
private fun CompactBookingScreen(viewModel: BookingLifecycleViewModel, businessDate: String, modifier: Modifier = Modifier) {
    val state = viewModel.listState
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        item {
            Spacer(Modifier.height(AppSpacing.xxs))
            AppPageHeader(
                "Booking",
                "Bookings",
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        SecondaryButton("Refresh", viewModel::refreshList, enabled = !state.loading)
                        PrimaryButton("New", viewModel::openNew)
                    }
                }
            )
        }
        item { MainScreenDateRow(businessDate) }
        item {
            AppTextField(
                value = query,
                onValueChange = { value ->
                    query = value
                    val normalized = value.trim()
                    when {
                        normalized.isEmpty() || normalized.length >= 2 -> viewModel.searchBookings(normalized)
                        state.search.isNotEmpty() -> viewModel.searchBookings("")
                    }
                },
                label = "Search booking",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (state.loading && !state.loaded) item { LoadingState() }
        if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, true) }
        if (state.loaded && state.items.isEmpty()) item { AppCard { EmptyState("No records found.") } }
        items(state.items, key = { it.id }) { booking ->
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(booking.customerName, style = MaterialTheme.typography.titleSmall)
                        Text(booking.bookingNo, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    }
                    StatusBadge(lifecycleLabel(booking), lifecycleTone(booking.displayStatus))
                }
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(booking.itemsSummary.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
                Text("${booking.pickupDate} → ${booking.returnDate} · Qty ${booking.bookedQty}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Spacer(Modifier.height(AppSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    SecondaryButton("Open", { viewModel.openBooking(booking.id) }, Modifier.weight(1f))
                    SecondaryButton("Call", { context.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.customerMobile}"))) }, Modifier.weight(1f))
                }
            }
        }
        if (state.loaded) item { CompactPager(state.page, state.totalPages, state.loading, viewModel::bookingPage) }
        item { Spacer(Modifier.height(AppSpacing.md)) }
    }
}

@Composable
private fun BookingEditorScreen(viewModel: BookingLifecycleViewModel) {
    val detail = viewModel.detailState.data
    val isNew = detail == null && !viewModel.detailState.loading && viewModel.detailState.error == null
    val canEdit = isNew || detail?.booking?.rawStatus == "BOOKED"
    val confirmed = detail?.booking?.confirmationState != "RESERVED"

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (isNew) "New Booking" else detail?.booking?.bookingNo ?: "Booking", style = MaterialTheme.typography.titleMedium)
                    detail?.let { StatusBadge(lifecycleLabel(it.booking), lifecycleTone(it.booking.displayStatus)) }
                }
                SecondaryButton("Back", viewModel::backFromEditor)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = AppSpacing.md).imePadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            if (viewModel.bootstrapState.loading || viewModel.detailState.loading) item { LoadingState() }
            if (!viewModel.bootstrapState.error.isNullOrBlank()) item { InlineMessage(viewModel.bootstrapState.error, true) }
            if (!viewModel.detailState.error.isNullOrBlank()) item { InlineMessage(viewModel.detailState.error, true) }
            if (!viewModel.actionError.isNullOrBlank()) item { InlineMessage(viewModel.actionError, true) }
            if (!viewModel.actionNotice.isNullOrBlank()) item { InlineMessage(viewModel.actionNotice, false) }

            item {
                AppCard {
                    AppTextField(
                        value = viewModel.customerSearch,
                        onValueChange = viewModel::setCustomerSearch,
                        label = "Customer search",
                        enabled = canEdit && !viewModel.actionBusy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                    Spacer(Modifier.height(AppSpacing.xs))
                    CompactSelector(
                        label = "Customer",
                        selectedId = viewModel.selectedCustomerId,
                        options = viewModel.customerOptions.map { it.id to "${it.name} · ${it.mobile}" },
                        onSelect = viewModel::setSelectedCustomer,
                        enabled = canEdit && !viewModel.actionBusy
                    )
                    Spacer(Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        AppTextField(viewModel.pickupDate, viewModel::setPickupDate, "Pickup Date", Modifier.weight(1f), enabled = canEdit && !viewModel.actionBusy)
                        AppTextField(viewModel.returnDate, viewModel::setReturnDate, "Return Date", Modifier.weight(1f), enabled = canEdit && !viewModel.actionBusy)
                    }
                    Spacer(Modifier.height(AppSpacing.xs))
                    AppTextField(viewModel.notes, viewModel::setNotes, "Notes", enabled = canEdit && !viewModel.actionBusy)
                }
            }

            if (canEdit) {
                itemsIndexed(viewModel.draftLines) { index, line ->
                    AppCard {
                        val categories = viewModel.itemOptions.distinctBy { it.categoryId }.map { it.categoryId to it.categoryName }
                        val used = viewModel.draftLines.mapIndexedNotNull { i, value -> if (i != index) value.itemId.takeIf(String::isNotBlank) else null }.toSet()
                        val itemChoices = viewModel.itemOptions.filter { item -> item.categoryId == line.categoryId && (item.id == line.itemId || item.id !in used) }
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            CompactSelector("Category", line.categoryId, categories, { viewModel.setLineCategory(index, it) }, Modifier.weight(1f), !viewModel.actionBusy)
                            CompactSelector("Item", line.itemId, itemChoices.map { it.id to "${it.itemCode} · ${it.itemName}" }, { viewModel.setLineItem(index, it) }, Modifier.weight(1.4f), !viewModel.actionBusy && line.categoryId.isNotBlank())
                        }
                        Spacer(Modifier.height(AppSpacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = line.quantity.toString(),
                                onValueChange = { viewModel.setLineQuantity(index, it.toIntOrNull() ?: 1) },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !viewModel.actionBusy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            SecondaryButton("Remove", { viewModel.removeLine(index) }, enabled = viewModel.draftLines.size > 1 && !viewModel.actionBusy)
                        }
                    }
                }
                item { SecondaryButton("Add Item", viewModel::addLine, Modifier.fillMaxWidth(), enabled = !viewModel.actionBusy) }
            } else if (detail != null) {
                items(detail.items, key = { it.bookingItemId }) { item -> BookingProgressRow(item) }
            }

            if (isNew) {
                item {
                    AppCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            SecondaryButton("Reserve", viewModel::reserve, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                            PrimaryButton("Booked", viewModel::createConfirmed, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                        }
                        Spacer(Modifier.height(AppSpacing.xs))
                        PrimaryButton("Direct Pickup", viewModel::directPickup, Modifier.fillMaxWidth(), enabled = !viewModel.actionBusy)
                    }
                }
            } else if (detail != null && detail.booking.rawStatus == "BOOKED") {
                item {
                    AppCard {
                        PrimaryButton("Save", viewModel::saveChanges, Modifier.fillMaxWidth(), enabled = !viewModel.actionBusy)
                        Spacer(Modifier.height(AppSpacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            if (!confirmed) PrimaryButton("Confirm Booking", viewModel::confirmReserved, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                            SecondaryButton("Cancel", viewModel::cancelBooking, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                        }
                    }
                }
            }

            if (detail != null && confirmed && detail.booking.rawStatus in setOf("BOOKED", "PARTIALLY_GIVEN")) item { PickupActionCard(detail, viewModel) }
            if (detail != null && detail.booking.rawStatus in setOf("PARTIALLY_GIVEN", "GIVEN", "PARTIALLY_RETURNED")) item { ReturnActionCard(detail, viewModel) }
            item { Spacer(Modifier.height(AppSpacing.md)) }
        }
    }
}

@Composable
private fun PickupActionCard(detail: BookingDetailData, viewModel: BookingLifecycleViewModel) {
    val remaining = detail.items.filter { it.remainingToGive > 0 }
    if (remaining.isEmpty()) return
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Pickup", style = MaterialTheme.typography.titleSmall)
            SecondaryButton("All", viewModel::fillAllPickup, enabled = !viewModel.actionBusy)
        }
        remaining.forEach { item ->
            Spacer(Modifier.height(AppSpacing.xs))
            QuantityActionRow(item, viewModel.pickupNow[item.bookingItemId] ?: 0, item.remainingToGive) { viewModel.setPickupNow(item.bookingItemId, it) }
        }
        Spacer(Modifier.height(AppSpacing.xs))
        PrimaryButton("Save Pickup", viewModel::savePickup, Modifier.fillMaxWidth(), enabled = !viewModel.actionBusy)
    }
}

@Composable
private fun ReturnActionCard(detail: BookingDetailData, viewModel: BookingLifecycleViewModel) {
    val pending = detail.items.filter { it.pendingToReturn > 0 }
    if (pending.isEmpty()) return
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Return", style = MaterialTheme.typography.titleSmall)
            SecondaryButton("All", viewModel::fillAllReturn, enabled = !viewModel.actionBusy)
        }
        pending.forEach { item ->
            Spacer(Modifier.height(AppSpacing.xs))
            QuantityActionRow(item, viewModel.returnNow[item.bookingItemId] ?: 0, item.pendingToReturn) { viewModel.setReturnNow(item.bookingItemId, it) }
        }
        Spacer(Modifier.height(AppSpacing.xs))
        PrimaryButton("Save Return", viewModel::saveReturn, Modifier.fillMaxWidth(), enabled = !viewModel.actionBusy)
    }
}

@Composable
private fun QuantityActionRow(item: BookingDetailItem, value: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${item.itemCode} · ${item.itemName}", style = MaterialTheme.typography.bodyMedium)
            Text("Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onChange((it.toIntOrNull() ?: 0).coerceIn(0, max)) },
            label = { Text("Qty") },
            singleLine = true,
            modifier = Modifier.width(88.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun BookingProgressRow(item: BookingDetailItem) {
    AppCard {
        Text("${item.itemCode} · ${item.itemName}", style = MaterialTheme.typography.titleSmall)
        Text("Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
    }
}

@Composable
private fun CompactSelector(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.first == selectedId }?.second ?: "Select $label"
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = enabled) { Text(selected, maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.second) }, onClick = { onSelect(option.first); expanded = false })
            }
        }
    }
}

@Composable
private fun CompactMoreScreen(role: UserRole, businessDate: String, modifier: Modifier = Modifier) {
    val modules = buildList {
        addAll(listOf("Items", "Pickup", "Returns", "Reports"))
        if (role != UserRole.STAFF) addAll(listOf("Categories", "Users", "Settings", "Audit Logs"))
    }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        item { Spacer(Modifier.height(AppSpacing.xxs)); AppPageHeader("Modules", "More") }
        item { MainScreenDateRow(businessDate) }
        items(modules) { name -> AppCard { Text(name, style = MaterialTheme.typography.titleSmall) } }
        item { Spacer(Modifier.height(AppSpacing.md)) }
    }
}

@Composable
private fun CompactContactActions(context: Context, mobile: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SecondaryButton("Call", { context.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$mobile"))) }, Modifier.weight(1f))
        PrimaryButton("WhatsApp", {
            val number = if (mobile.length == 10) "91$mobile" else mobile
            context.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
        }, Modifier.weight(1f))
    }
}

@Composable
private fun CompactPager(page: Int, totalPages: Int, loading: Boolean, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        SecondaryButton("Previous", { onPage(page - 1) }, enabled = !loading && page > 1)
        Spacer(Modifier.width(AppSpacing.xs))
        Text("$page / $totalPages", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(AppSpacing.xs))
        SecondaryButton("Next", { onPage(page + 1) }, enabled = !loading && page < totalPages)
    }
}

private fun lifecycleLabel(booking: LifecycleBookingSummary): String = when (booking.displayStatus.uppercase()) {
    "RESERVED" -> "Reserved"
    "BOOKED" -> "Booked"
    "PART_PICKUP" -> "Part Pickup"
    "FULL_PICKUP" -> "Full Pickup"
    "PART_RETURN" -> "Part Return"
    "FULL_RETURN" -> "Full Return"
    "CANCELLED" -> "Cancelled"
    else -> booking.displayStatus.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun lifecycleTone(status: String): BadgeTone = when (status.uppercase()) {
    "RESERVED" -> BadgeTone.INFO
    "BOOKED", "PART_PICKUP", "PART_RETURN" -> BadgeTone.WARNING
    "FULL_PICKUP" -> BadgeTone.INFO
    "FULL_RETURN" -> BadgeTone.SUCCESS
    "CANCELLED" -> BadgeTone.ERROR
    else -> BadgeTone.NEUTRAL
}

private fun Context.safeStart(intent: Intent) { runCatching { startActivity(intent) } }
