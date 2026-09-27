package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
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

private enum class Screen3Destination { DASHBOARD, CUSTOMERS, BOOKINGS, MORE }

@Composable
fun ZhagmagAdminRootScreen3(
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel,
    customerViewModel: CustomerScreen3ViewModel
) {
    when (val auth = adminViewModel.authState) {
        AuthState.Checking, AuthState.SignedOut -> ZhagmagAdminRoot(adminViewModel, bookingViewModel)
        is AuthState.SignedIn -> Screen3AuthenticatedApp(auth.user, adminViewModel, bookingViewModel, customerViewModel)
    }
}

@Composable
private fun Screen3AuthenticatedApp(
    user: SessionUser,
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel,
    customerViewModel: CustomerScreen3ViewModel
) {
    var destination by rememberSaveable { mutableStateOf(Screen3Destination.DASHBOARD) }
    var consumedMutationVersion by rememberSaveable { mutableStateOf(bookingViewModel.mutationVersion) }
    val dashboardScreenState = rememberDashboardScreenState()

    LaunchedEffect(destination, bookingViewModel.editorOpen, bookingViewModel.mutationVersion) {
        if (!bookingViewModel.editorOpen) {
            when (destination) {
                Screen3Destination.CUSTOMERS -> customerViewModel.ensureLoaded()
                Screen3Destination.BOOKINGS -> bookingViewModel.ensureList()
                Screen3Destination.DASHBOARD -> if (bookingViewModel.mutationVersion != consumedMutationVersion) {
                    consumedMutationVersion = bookingViewModel.mutationVersion
                    adminViewModel.loadDashboard()
                }
                Screen3Destination.MORE -> Unit
            }
        }
    }

    if (bookingViewModel.editorOpen) {
        AdminAppCompact(adminViewModel, bookingViewModel)
        return
    }

    val businessDate = adminViewModel.dashboardState.data?.today.orEmpty()

    Scaffold(
        topBar = { AdminMainTopBar(adminViewModel.branding, user, adminViewModel::logout) },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(destination == Screen3Destination.DASHBOARD, { destination = Screen3Destination.DASHBOARD }, { Icon(Icons.Rounded.Dashboard, null) }, label = { Text("Dashboard") })
                NavigationBarItem(destination == Screen3Destination.CUSTOMERS, { destination = Screen3Destination.CUSTOMERS }, { Icon(Icons.Rounded.People, null) }, label = { Text("Customers") })
                NavigationBarItem(destination == Screen3Destination.BOOKINGS, { destination = Screen3Destination.BOOKINGS }, { Icon(Icons.Rounded.EventNote, null) }, label = { Text("Bookings") })
                NavigationBarItem(destination == Screen3Destination.MORE, { destination = Screen3Destination.MORE }, { Icon(Icons.Rounded.Menu, null) }, label = { Text("More") })
            }
        }
    ) { innerPadding ->
        when (destination) {
            Screen3Destination.DASHBOARD -> PullToRefreshBox(
                isRefreshing = adminViewModel.dashboardState.loading && adminViewModel.dashboardState.data != null,
                onRefresh = adminViewModel::loadDashboard,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                DashboardScreenV2(
                    state = adminViewModel.dashboardState,
                    revision = adminViewModel.dashboardRevision,
                    uiState = dashboardScreenState,
                    shopName = adminViewModel.branding.shopName,
                    onRefresh = adminViewModel::loadDashboard,
                    onNewBooking = { destination = Screen3Destination.BOOKINGS; bookingViewModel.openNew() },
                    onOpenBooking = { id -> destination = Screen3Destination.BOOKINGS; bookingViewModel.openBooking(id) }
                )
            }

            Screen3Destination.CUSTOMERS -> CustomerScreenV3(
                state = customerViewModel.state,
                role = user.role,
                businessDate = businessDate,
                onSearch = customerViewModel::search,
                onSort = customerViewModel::sort,
                onArchivedTab = customerViewModel::setArchived,
                onPage = customerViewModel::page,
                onRefresh = customerViewModel::refresh,
                onSave = customerViewModel::save,
                onArchive = customerViewModel::archive,
                onRestore = customerViewModel::restore,
                onDeletePermanent = customerViewModel::deletePermanently,
                onCreateBooking = { customer ->
                    destination = Screen3Destination.BOOKINGS
                    bookingViewModel.openNew()
                    bookingViewModel.setSelectedCustomer(customer.id)
                },
                onWhatsApp = { customerId, bookingId, onReady ->
                    customerViewModel.composeWhatsApp(customerId, bookingId) { onReady(it.mobile, it.message) }
                },
                modifier = Modifier.padding(innerPadding)
            )

            Screen3Destination.BOOKINGS -> Screen3BookingList(bookingViewModel, businessDate, Modifier.padding(innerPadding))
            Screen3Destination.MORE -> Screen3More(user.role, businessDate, Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen3BookingList(viewModel: BookingLifecycleViewModel, businessDate: String, modifier: Modifier = Modifier) {
    val state = viewModel.listState
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = viewModel::refreshList,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            item { Spacer(Modifier.height(AppSpacing.xxs)) }
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Bookings", style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Screen3RefreshAction(!state.loading, viewModel::refreshList)
                        PrimaryButton("New Booking", viewModel::openNew)
                    }
                }
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
            if (state.loaded && state.items.isEmpty()) item { EmptyState("No records found.") }
            items(state.items, key = { it.id }) { booking ->
                AppCard {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(booking.customerName, style = MaterialTheme.typography.titleSmall)
                            Text(booking.bookingNo, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                        }
                        StatusBadge(screen3LifecycleLabel(booking), screen3LifecycleTone(booking.displayStatus))
                    }
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(booking.itemsSummary.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
                    Text("${booking.pickupDate} → ${booking.returnDate} · Qty ${booking.bookedQty}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    Spacer(Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        SecondaryButton("Open", { viewModel.openBooking(booking.id) }, Modifier.weight(1f))
                        SecondaryButton(
                            "Call",
                            {
                                val mobile = booking.customerMobile.filter(Char::isDigit)
                                if (mobile.isNotBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$mobile"))) }
                            },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
            if (state.loaded) item {
                Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                    SecondaryButton("Previous", { viewModel.bookingPage(state.page - 1) }, enabled = !state.loading && state.page > 1)
                    Text("  Page ${state.page} of ${state.totalPages}  ", style = MaterialTheme.typography.labelMedium)
                    SecondaryButton("Next", { viewModel.bookingPage(state.page + 1) }, enabled = !state.loading && state.page < state.totalPages)
                }
            }
            item { Spacer(Modifier.height(AppSpacing.md)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen3RefreshAction(enabled: Boolean, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text("Refresh") } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, enabled = enabled) { Icon(Icons.Rounded.Refresh, "Refresh") }
    }
}

@Composable
private fun Screen3More(role: UserRole, businessDate: String, modifier: Modifier = Modifier) {
    val modules = buildList {
        addAll(listOf("Items", "Pickup", "Returns", "Reports"))
        if (role != UserRole.STAFF) addAll(listOf("Categories", "Users", "Settings", "Audit Logs"))
    }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        item { Spacer(Modifier.height(AppSpacing.xxs)) }
        item { Text("More", style = MaterialTheme.typography.headlineMedium) }
        item { MainScreenDateRow(businessDate) }
        items(modules) { name -> AppCard { Text(name, style = MaterialTheme.typography.titleSmall) } }
        item { Spacer(Modifier.height(AppSpacing.md)) }
    }
}

private fun screen3LifecycleLabel(booking: LifecycleBookingSummary): String = when (booking.displayStatus.uppercase()) {
    "RESERVED" -> "Reserved"
    "BOOKED" -> "Booked"
    "PART_PICKUP" -> "Part Pickup"
    "FULL_PICKUP" -> "Full Pickup"
    "PART_RETURN" -> "Part Return"
    "FULL_RETURN" -> "Full Return"
    "CANCELLED" -> "Cancelled"
    else -> booking.displayStatus.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun screen3LifecycleTone(status: String): BadgeTone = when (status.uppercase()) {
    "RESERVED", "FULL_PICKUP" -> BadgeTone.INFO
    "BOOKED", "PART_PICKUP", "PART_RETURN" -> BadgeTone.WARNING
    "FULL_RETURN" -> BadgeTone.SUCCESS
    "CANCELLED" -> BadgeTone.ERROR
    else -> BadgeTone.NEUTRAL
}
