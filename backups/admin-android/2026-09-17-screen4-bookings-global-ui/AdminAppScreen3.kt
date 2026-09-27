package com.nimsdeveloper.zhagmagdresses.admin

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionCustomer
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionItem
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
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
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusErrorSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Screen3Destination { DASHBOARD, CUSTOMERS, BOOKINGS, MORE }

private const val BOOKING_TAB_DETAILS = 0
private const val BOOKING_TAB_ITEMS = 1
private const val BOOKING_TAB_PICKUP = 2
private const val BOOKING_TAB_RETURN = 3
private const val BOOKING_TAB_HISTORY = 4

private data class BookingStatusCopy(
    val current: String,
    val currentTone: BadgeTone,
    val upcoming: String?,
    val upcomingTone: BadgeTone = BadgeTone.WARNING
)

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
    var confirmExit by remember { mutableStateOf(false) }
    val dashboardScreenState = rememberDashboardScreenState()
    val editorOpen = bookingViewModel.editorOpen
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(destination, editorOpen, bookingViewModel.mutationVersion) {
        if (!editorOpen) {
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

    BackHandler(enabled = !editorOpen) {
        when {
            confirmExit -> confirmExit = false
            destination != Screen3Destination.DASHBOARD -> destination = Screen3Destination.DASHBOARD
            else -> confirmExit = true
        }
    }

    val businessDate = adminViewModel.dashboardState.data?.today.orEmpty()
    val topRefreshBusy = if (editorOpen) {
        bookingViewModel.bootstrapState.loading ||
            bookingViewModel.detailState.loading ||
            bookingViewModel.actionBusy ||
            bookingViewModel.availabilityLoading ||
            bookingViewModel.newCustomerBusy
    } else {
        when (destination) {
            Screen3Destination.DASHBOARD -> adminViewModel.dashboardState.loading
            Screen3Destination.CUSTOMERS -> customerViewModel.state.loading || customerViewModel.state.actionBusy
            Screen3Destination.BOOKINGS -> bookingViewModel.listState.loading
            Screen3Destination.MORE -> adminViewModel.dashboardState.loading
        }
    }
    val topRefreshAction: () -> Unit = if (editorOpen) {
        bookingViewModel::refreshEditor
    } else {
        when (destination) {
            Screen3Destination.DASHBOARD -> adminViewModel::loadDashboard
            Screen3Destination.CUSTOMERS -> customerViewModel::refresh
            Screen3Destination.BOOKINGS -> bookingViewModel::refreshList
            Screen3Destination.MORE -> adminViewModel::loadDashboard
        }
    }

    Scaffold(
        topBar = {
            AdminMainTopBar(
                branding = adminViewModel.branding,
                user = user,
                onRefresh = topRefreshAction,
                refreshing = topRefreshBusy
            )
        },
        bottomBar = {
            if (!editorOpen) {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(
                        destination == Screen3Destination.DASHBOARD,
                        { destination = Screen3Destination.DASHBOARD },
                        { Icon(Icons.Rounded.Dashboard, null) },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        destination == Screen3Destination.CUSTOMERS,
                        { destination = Screen3Destination.CUSTOMERS },
                        { Icon(Icons.Rounded.People, null) },
                        label = { Text("Customers") }
                    )
                    NavigationBarItem(
                        destination == Screen3Destination.BOOKINGS,
                        { destination = Screen3Destination.BOOKINGS },
                        { Icon(Icons.Rounded.EventNote, null) },
                        label = { Text("Bookings") }
                    )
                    NavigationBarItem(
                        destination == Screen3Destination.MORE,
                        { destination = Screen3Destination.MORE },
                        { Icon(Icons.Rounded.Menu, null) },
                        label = { Text("More") }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (editorOpen) {
            BookingWorkspaceScreen(
                viewModel = bookingViewModel,
                businessDate = businessDate,
                composeWhatsApp = { customerId, bookingId, onReady ->
                    customerViewModel.composeWhatsApp(customerId, bookingId) {
                        onReady(it.mobile, it.message)
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
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
                        onNewBooking = {
                            destination = Screen3Destination.BOOKINGS
                            bookingViewModel.openNew()
                        },
                        onOpenBooking = { id ->
                            destination = Screen3Destination.BOOKINGS
                            bookingViewModel.openBooking(id)
                        }
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
                        customerViewModel.composeWhatsApp(customerId, bookingId) {
                            onReady(it.mobile, it.message)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                Screen3Destination.BOOKINGS -> Screen3BookingList(
                    viewModel = bookingViewModel,
                    businessDate = businessDate,
                    composeWhatsApp = { customerId, bookingId, onReady ->
                        customerViewModel.composeWhatsApp(customerId, bookingId) {
                            onReady(it.mobile, it.message)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                Screen3Destination.MORE -> Screen3More(
                    role = user.role,
                    businessDate = businessDate,
                    onLogout = adminViewModel::logout,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Exit app?") },
            text = { Text("Close Zhagmag Dresses Admin?") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; activity?.finish() }) { Text("Exit") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen3BookingList(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    composeWhatsApp: (String, String?, (String, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.listState
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val views = bookingListViews()
    val selectedViewIndex = views.indexOfFirst { it.first == viewModel.listView }.coerceAtLeast(0)

    LaunchedEffect(state.search) {
        if (query != state.search) query = state.search
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = viewModel::refreshList,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(Modifier.height(AppSpacing.xxs)) }
            item { MainScreenDateRow(businessDate) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bookings", style = MaterialTheme.typography.headlineMedium)
                    Surface(
                        onClick = viewModel::openNew,
                        enabled = !viewModel.actionBusy,
                        color = BrandSoft,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("New Booking", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedViewIndex,
                    edgePadding = 0.dp,
                    divider = { HorizontalDivider(color = AppBorder) }
                ) {
                    views.forEach { (value, label) ->
                        Tab(
                            selected = viewModel.listView == value,
                            onClick = { viewModel.setListView(value) },
                            text = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        label = "Search",
                        placeholder = "Search",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                    )
                    Box {
                        SoftActionButton(
                            icon = Icons.Rounded.FilterList,
                            contentDescription = "Filter bookings",
                            onClick = { filterMenuOpen = true },
                            enabled = !state.loading,
                            tone = ActionTone.TEAL
                        )
                        DropdownMenu(filterMenuOpen, { filterMenuOpen = false }) {
                            bookingStatusFilters().forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (viewModel.listStatus == value) "✓  $label" else label) },
                                    onClick = {
                                        filterMenuOpen = false
                                        viewModel.setListStatus(value)
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        SoftActionButton(
                            icon = Icons.Rounded.Sort,
                            contentDescription = "Sort bookings",
                            onClick = { sortMenuOpen = true },
                            enabled = !state.loading,
                            tone = ActionTone.INFO
                        )
                        DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                            bookingSortOptions().forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (viewModel.listSort == value) "✓  $label" else label) },
                                    onClick = {
                                        sortMenuOpen = false
                                        viewModel.setListSort(value)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (viewModel.listStatus.isNotBlank()) {
                item { StatusBadge("Filter · ${bookingStatusFilterLabel(viewModel.listStatus)}", BadgeTone.INFO) }
            }
            if (state.loading && !state.loaded) item { LoadingState() }
            if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, true) }
            if (state.loaded && state.items.isEmpty()) item { EmptyState("No bookings found") }

            items(state.items, key = { it.id }) { booking ->
                BookingListCard(
                    booking = booking,
                    busy = state.loading || viewModel.actionBusy,
                    onView = { viewModel.openBooking(booking.id) },
                    onEdit = { viewModel.openBooking(booking.id, edit = true) },
                    onCall = { openDialer(context, booking.customerMobile) },
                    onWhatsApp = {
                        composeWhatsApp(booking.customerId, booking.id) { mobile, message ->
                            openWhatsApp(context, mobile, message)
                        }
                    }
                )
            }

            if (state.loaded) {
                item {
                    BookingPaginationRow(
                        page = state.page,
                        totalPages = state.totalPages,
                        busy = state.loading,
                        onPage = viewModel::bookingPage
                    )
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun BookingListCard(
    booking: LifecycleBookingSummary,
    busy: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val statusCopy = bookingStatusCopy(booking)
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.Top
        ) {
            Surface(color = BrandSoft, contentColor = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(booking.bookingNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(booking.customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(booking.customerMobile, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BookingLifecycleBadge("Current Status", statusCopy.current, statusCopy.currentTone, Modifier.weight(1f))
            statusCopy.upcoming?.let {
                BookingLifecycleBadge("Upcoming Status", it, statusCopy.upcomingTone, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(7.dp))
        BookingListItemPreviews(booking, onView)
        Spacer(Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            BookingDateInline(
                label = "Pickup",
                value = booking.pickupDate,
                background = StatusInfoSoft,
                foreground = StatusInfo,
                modifier = Modifier.weight(1f)
            )
            BookingDateInline(
                label = "Return",
                value = booking.returnDate,
                background = StatusWarningSoft,
                foreground = StatusWarning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SoftActionButton(
                icon = Icons.Rounded.EventNote,
                label = "View",
                onClick = onView,
                modifier = Modifier.weight(1f),
                enabled = !busy,
                tone = ActionTone.INFO
            )
            if (booking.rawStatus == "BOOKED") {
                SoftActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.PURPLE
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
        }
    }
}

@Composable
private fun BookingLifecycleBadge(title: String, value: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val colors = when (tone) {
        BadgeTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        BadgeTone.WARNING -> StatusWarningSoft to StatusWarning
        BadgeTone.ERROR -> StatusErrorSoft to StatusError
        BadgeTone.INFO -> StatusInfoSoft to StatusInfo
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(modifier = modifier, color = colors.first, contentColor = colors.second, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

@Composable
private fun BookingListItemPreviews(booking: LifecycleBookingSummary, onMore: () -> Unit) {
    val previews = booking.itemPreviews.take(2)
    if (previews.isEmpty()) {
        Text(booking.itemsSummary.ifBlank { "No item summary" }, style = MaterialTheme.typography.bodySmall)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        previews.forEachIndexed { index, preview ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookingItemVisual(preview.imageUrl, preview.itemName, 42.dp)
                Text(preview.itemName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 2)
                Text("× ${preview.quantity}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            if (index < previews.lastIndex) HorizontalDivider(color = AppBorder)
        }
        val more = (booking.itemCount - previews.size).coerceAtLeast(0)
        if (more > 0) {
            Surface(
                onClick = onMore,
                color = BrandSoft,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "+$more more",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BookingDateInline(
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.small) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EventNote, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
            Text(screen4Date(value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun BookingDateCell(
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = background, contentColor = foreground, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(screen4Date(value), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BookingPaginationRow(
    page: Int,
    totalPages: Int,
    busy: Boolean,
    onPage: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
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
private fun BookingWorkspaceScreen(
    viewModel: BookingLifecycleViewModel,
    businessDate: String,
    composeWhatsApp: (String, String?, (String, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detail = viewModel.detailState.data
    val isNew = detail == null && !viewModel.detailState.loading && viewModel.detailState.error == null
    var selectedTab by rememberSaveable(detail?.booking?.id) { mutableIntStateOf(BOOKING_TAB_DETAILS) }
    var editMode by rememberSaveable(detail?.booking?.id) { mutableStateOf(viewModel.editorStartsInEditMode) }
    var wizardStep by rememberSaveable { mutableIntStateOf(1) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmDiscardNew by remember { mutableStateOf(false) }
    var confirmDiscardEdit by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.actionNotice) {
        if (viewModel.actionNotice == "Booking updated.") editMode = false
    }
    LaunchedEffect(editMode, detail?.booking?.id) {
        if (editMode && detail != null) viewModel.loadAvailabilityForSelectedDates()
    }

    val canEditExisting = detail?.booking?.rawStatus == "BOOKED"
    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    val newDirty = isNew && (
        viewModel.selectedCustomerId.isNotBlank() ||
            viewModel.draftLines.isNotEmpty() ||
            viewModel.notes.isNotBlank() ||
            viewModel.pickupDate != today.toString() ||
            viewModel.returnDate != today.plusDays(1).toString()
        )
    val editDirty = if (detail == null) false else bookingEditDirty(viewModel, detail)

    fun requestCloseNew() {
        if (newDirty) confirmDiscardNew = true else viewModel.backFromEditor()
    }

    fun requestCloseEdit() {
        if (editDirty) confirmDiscardEdit = true else editMode = false
    }

    BackHandler(enabled = viewModel.editorOpen) {
        if (viewModel.actionBusy || viewModel.newCustomerBusy || viewModel.availabilityLoading) return@BackHandler
        when {
            isNew && wizardStep > 1 -> wizardStep -= 1
            isNew -> requestCloseNew()
            editMode -> requestCloseEdit()
            else -> viewModel.backFromEditor()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        item { Spacer(Modifier.height(AppSpacing.xxs)) }
        item { MainScreenDateRow(businessDate) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            isNew -> "New Booking"
                            editMode -> "Edit Booking"
                            else -> "Booking Details"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (detail != null) {
                        Text(detail.booking.bookingNo, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                    }
                }
                if (detail != null) StatusBadge(screen3LifecycleLabel(detail.booking), screen3LifecycleTone(detail.booking.displayStatus))
            }
        }

        if (viewModel.bootstrapState.loading || viewModel.detailState.loading) item { LoadingState() }
        if (!viewModel.bootstrapState.error.isNullOrBlank()) item { InlineMessage(viewModel.bootstrapState.error, true) }
        if (!viewModel.detailState.error.isNullOrBlank()) item { InlineMessage(viewModel.detailState.error, true) }
        if (!viewModel.actionError.isNullOrBlank()) item { InlineMessage(viewModel.actionError, true) }
        if (!viewModel.actionNotice.isNullOrBlank()) item { InlineMessage(viewModel.actionNotice, false) }
        if (!viewModel.availabilityError.isNullOrBlank()) item { InlineMessage(viewModel.availabilityError, true) }

        when {
            isNew -> {
                item { BookingFormStepper(wizardStep) }
                when (wizardStep) {
                    1 -> item { BookingCustomerStep(viewModel) }
                    2 -> item { BookingDetailsStep(viewModel) }
                    3 -> item { BookingItemsStep(viewModel) }
                    else -> item { BookingConfirmStep(viewModel) }
                }
                item {
                    when (wizardStep) {
                        1 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            SecondaryButton("Cancel", ::requestCloseNew, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                            PrimaryButton(
                                "Next",
                                { if (viewModel.validateCustomerStep()) wizardStep = 2 },
                                Modifier.weight(1f),
                                enabled = !viewModel.actionBusy
                            )
                        }
                        2 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            SecondaryButton("Back", { wizardStep = 1 }, Modifier.weight(1f), enabled = !viewModel.availabilityLoading)
                            PrimaryButton(
                                text = "Next",
                                onClick = { viewModel.loadAvailabilityForSelectedDates { wizardStep = 3 } },
                                modifier = Modifier.weight(1f),
                                enabled = !viewModel.actionBusy,
                                loading = viewModel.availabilityLoading
                            )
                        }
                        3 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            SecondaryButton("Back", { wizardStep = 2 }, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                            PrimaryButton(
                                "Next",
                                { if (viewModel.validateItemsStep()) wizardStep = 4 },
                                Modifier.weight(1f),
                                enabled = !viewModel.actionBusy
                            )
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                SecondaryButton("Back", { wizardStep = 3 }, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                                SecondaryButton("Reserve", viewModel::reserve, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                                PrimaryButton("Confirm", viewModel::createConfirmed, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                            }
                            PrimaryButton(
                                text = "Direct Pickup",
                                onClick = viewModel::directPickup,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !viewModel.actionBusy,
                                loading = viewModel.actionBusy,
                                icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            editMode && detail != null -> {
                item { BookingLegacyEditForm(viewModel, detail) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        SecondaryButton("Cancel", ::requestCloseEdit, Modifier.weight(1f), enabled = !viewModel.actionBusy)
                        PrimaryButton(
                            text = "Update Booking",
                            onClick = viewModel::saveChanges,
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.actionBusy,
                            loading = viewModel.actionBusy,
                            icon = { Icon(Icons.Rounded.Save, contentDescription = null) }
                        )
                    }
                }
            }

            detail != null -> {
                item {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp, divider = { HorizontalDivider(color = AppBorder) }) {
                        listOf("Details", "Items", "Pickup", "Return", "History").forEachIndexed { index, label ->
                            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label, maxLines = 1) })
                        }
                    }
                }

                when (selectedTab) {
                    BOOKING_TAB_DETAILS -> item {
                        BookingDetailsPanel(
                            detail = detail,
                            busy = viewModel.actionBusy,
                            canEdit = canEditExisting,
                            onEdit = { editMode = true },
                            onConfirm = viewModel::confirmReserved,
                            onCancel = { confirmCancel = true },
                            onPickup = { selectedTab = BOOKING_TAB_PICKUP },
                            onReturn = { selectedTab = BOOKING_TAB_RETURN },
                            onCall = { openDialer(context, detail.booking.customerMobile) },
                            onWhatsApp = {
                                composeWhatsApp(detail.booking.customerId, detail.booking.id) { mobile, message -> openWhatsApp(context, mobile, message) }
                            }
                        )
                    }
                    BOOKING_TAB_ITEMS -> item { BookingItemsPanel(detail.items) }
                    BOOKING_TAB_PICKUP -> item { BookingPickupPanel(detail, viewModel) }
                    BOOKING_TAB_RETURN -> item { BookingReturnPanel(detail, viewModel) }
                    BOOKING_TAB_HISTORY -> item { BookingHistoryPanel(detail) }
                }
            }
        }

        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }

    if (confirmDiscardNew) {
        AlertDialog(
            onDismissRequest = { confirmDiscardNew = false },
            title = { Text("Discard booking?") },
            text = { Text("Your unsaved booking changes will be lost.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscardNew = false; viewModel.backFromEditor() }) { Text("Discard", color = StatusError) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscardNew = false }) { Text("Keep editing") } }
        )
    }

    if (confirmDiscardEdit) {
        AlertDialog(
            onDismissRequest = { confirmDiscardEdit = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved booking edits will be lost.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscardEdit = false; viewModel.openBooking(detail?.booking?.id.orEmpty()) }) {
                    Text("Discard", color = StatusError)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscardEdit = false }) { Text("Keep editing") } }
        )
    }

    if (confirmCancel && detail != null) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.actionBusy) confirmCancel = false },
            title = { Text("Cancel booking?") },
            text = { Text("This booking will move to Cancelled status. This action is only available before pickup starts.") },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.actionBusy,
                    onClick = {
                        confirmCancel = false
                        viewModel.cancelBooking()
                    }
                ) { Text("Cancel booking", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmCancel = false }, enabled = !viewModel.actionBusy) { Text("Keep booking") } }
        )
    }
}

@Composable
private fun BookingFormStepper(currentStep: Int) {
    val labels = listOf("Customer", "Details", "Items", "Confirm")
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            labels.forEachIndexed { index, label ->
                val number = index + 1
                val active = number <= currentStep
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text(number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (number == currentStep) MaterialTheme.colorScheme.primary else AppTextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCustomerStep(viewModel: BookingLifecycleViewModel) {
    val selected = viewModel.selectedCustomer
    var showNewCustomer by remember { mutableStateOf(false) }

    AppCard {
        BookingSectionHeader(Icons.Rounded.Person, "Customer", BrandSoft, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(AppSpacing.xs))

        if (selected != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StatusSuccessSoft,
                contentColor = StatusSuccess,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(selected.mobile, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = viewModel::clearSelectedCustomer, enabled = !viewModel.actionBusy) { Text("Change") }
                }
            }
        } else {
            AppTextField(
                value = viewModel.customerSearch,
                onValueChange = viewModel::setCustomerSearch,
                label = "Search customer",
                placeholder = "Name or mobile",
                enabled = !viewModel.actionBusy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
            )
            Spacer(Modifier.height(6.dp))
            val results = viewModel.customerOptions.take(8)
            if (results.isEmpty() && viewModel.customerSearch.length >= 2) {
                Text("No customer found", style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
            } else {
                results.forEachIndexed { index, customer ->
                    CustomerSearchResultRow(customer) { viewModel.setSelectedCustomer(customer.id) }
                    if (index < results.lastIndex) HorizontalDivider(color = AppBorder)
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.xs))
        SecondaryButton(
            text = "New Customer",
            onClick = { showNewCustomer = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.actionBusy,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) }
        )
    }

    if (showNewCustomer) {
        NewBookingCustomerDialog(
            viewModel = viewModel,
            onDismiss = { if (!viewModel.newCustomerBusy) showNewCustomer = false },
            onSaved = { showNewCustomer = false }
        )
    }
}

@Composable
private fun CustomerSearchResultRow(customer: BookingOptionCustomer, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = BrandSoft, contentColor = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(customer.mobile, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
            }
        }
    }
}

@Composable
private fun NewBookingCustomerDialog(
    viewModel: BookingLifecycleViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var alternate by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(name, { name = it.take(120) }, "Name", enabled = !viewModel.newCustomerBusy)
                AppTextField(
                    mobile,
                    { mobile = it.filter(Char::isDigit).take(10) },
                    "Primary Mobile",
                    enabled = !viewModel.newCustomerBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )
                AppTextField(
                    alternate,
                    { alternate = it.filter(Char::isDigit).take(10) },
                    "Alternate Mobile",
                    enabled = !viewModel.newCustomerBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )
                AppTextField(
                    address,
                    { address = it.take(500) },
                    "Address",
                    enabled = !viewModel.newCustomerBusy,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )
                InlineMessage(viewModel.newCustomerError, true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !viewModel.newCustomerBusy,
                onClick = { viewModel.createCustomerForBooking(name, mobile, alternate, address, onSaved) }
            ) { Text(if (viewModel.newCustomerBusy) "Saving…" else "Save Customer") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !viewModel.newCustomerBusy) { Text("Cancel") } }
    )
}

@Composable
private fun BookingDetailsStep(viewModel: BookingLifecycleViewModel) {
    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    val pickup = runCatching { LocalDate.parse(viewModel.pickupDate) }.getOrElse { today }
    AppCard {
        BookingSectionHeader(Icons.Rounded.EventNote, "Details", StatusSuccessSoft, StatusSuccess)
        Spacer(Modifier.height(AppSpacing.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            BookingDatePickerField(
                label = "Pickup Date",
                value = viewModel.pickupDate,
                minimumDate = today,
                modifier = Modifier.weight(1f),
                enabled = !viewModel.actionBusy,
                onDate = viewModel::setPickupDate
            )
            BookingDatePickerField(
                label = "Return Date",
                value = viewModel.returnDate,
                minimumDate = pickup.plusDays(1),
                modifier = Modifier.weight(1f),
                enabled = !viewModel.actionBusy,
                onDate = viewModel::setReturnDate
            )
        }
        Spacer(Modifier.height(AppSpacing.xs))
        AppTextField(
            value = viewModel.notes,
            onValueChange = viewModel::setNotes,
            label = "Notes (Optional)",
            enabled = !viewModel.actionBusy,
            singleLine = false,
            minLines = 2,
            maxLines = 3
        )
    }
}

@Composable
private fun BookingDatePickerField(
    label: String,
    value: String,
    minimumDate: LocalDate,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onDate: (String) -> Unit
) {
    val context = LocalContext.current
    val selected = runCatching { LocalDate.parse(value) }.getOrElse { minimumDate }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        Spacer(Modifier.height(3.dp))
        OutlinedButton(
            onClick = {
                val initial = if (selected.isBefore(minimumDate)) minimumDate else selected
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth
                ).apply {
                    datePicker.minDate = minimumDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }.show()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(screen4Date(value), maxLines = 1)
        }
    }
}

@Composable
private fun BookingItemsStep(viewModel: BookingLifecycleViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    val categories = viewModel.itemOptions.distinctBy { it.categoryId }.map { it.categoryId to it.categoryName }
    val filtered = viewModel.itemOptions.filter { item ->
        (categoryId.isBlank() || item.categoryId == categoryId) &&
            (search.isBlank() || item.itemName.contains(search, true) || item.itemCode.contains(search, true))
    }
    val visible = filtered.take(40)

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        AppCard {
            BookingSectionHeader(Icons.Rounded.Inventory2, "Items", StatusInfoSoft, StatusInfo)
            Spacer(Modifier.height(AppSpacing.xs))
            AppTextField(
                value = search,
                onValueChange = { search = it.take(80) },
                label = "Search item",
                placeholder = "Item name or code",
                enabled = !viewModel.actionBusy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BookingCategoryChip("All", categoryId.isBlank()) { categoryId = "" }
                categories.forEach { (id, name) ->
                    BookingCategoryChip(name, categoryId == id) { categoryId = id }
                }
            }
        }

        if (viewModel.draftLines.isNotEmpty()) {
            AppCard {
                Text("Selected Items (${viewModel.draftLines.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                viewModel.draftLines.forEachIndexed { index, line ->
                    val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId } ?: return@forEachIndexed
                    BookingSelectedItemRow(item, line.quantity) { viewModel.removeItem(item.id) }
                    if (index < viewModel.draftLines.lastIndex) HorizontalDivider(color = AppBorder)
                }
            }
        }

        if (viewModel.availabilityLoading) LoadingState()
        if (visible.isEmpty() && !viewModel.availabilityLoading) EmptyState("No items found")
        visible.forEach { item ->
            val selectedQty = viewModel.draftLines.firstOrNull { it.itemId == item.id }?.quantity
            BookingAvailableItemCard(
                item = item,
                available = viewModel.availableQuantity(item.id),
                selectedQuantity = selectedQty,
                busy = viewModel.actionBusy || viewModel.availabilityLoading,
                onAdd = { quantity -> viewModel.addOrUpdateItem(item.id, quantity) }
            )
        }
        if (filtered.size > visible.size) {
            Text("Showing first ${visible.size} items. Use Search or Category to narrow the list.", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        }
    }
}

@Composable
private fun BookingCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun BookingAvailableItemCard(
    item: BookingOptionItem,
    available: Int,
    selectedQuantity: Int?,
    busy: Boolean,
    onAdd: (Int) -> Boolean
) {
    var quantity by rememberSaveable(item.id, selectedQuantity) {
        mutableIntStateOf((selectedQuantity ?: 1).coerceAtMost(available.coerceAtLeast(1)))
    }
    LaunchedEffect(available) {
        if (available > 0 && quantity > available) quantity = available
    }

    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            BookingItemVisual(item.imageUrl, item.itemName, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(item.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${item.itemCode} · ${item.categoryName}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Text(
                    if (available > 0) "Available: $available" else "Unavailable",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (available > 0) StatusSuccess else StatusError
                )
            }
            if (selectedQuantity != null) StatusBadge("Added", BadgeTone.SUCCESS)
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            QuantityInputControl(
                value = quantity,
                min = 1,
                max = available.coerceAtLeast(1),
                enabled = !busy && available > 0,
                onChange = { quantity = it }
            )
            PrimaryButton(
                text = if (selectedQuantity == null) "Add" else "Update",
                onClick = { onAdd(quantity) },
                enabled = !busy && available > 0
            )
        }
    }
}

@Composable
private fun BookingSelectedItemRow(item: BookingOptionItem, quantity: Int, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookingItemVisual(item.imageUrl, item.itemName, 42.dp)
        Column(Modifier.weight(1f)) {
            Text(item.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${item.itemCode} · ${item.categoryName}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        }
        Text("× $quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        SoftActionButton(Icons.Rounded.Delete, contentDescription = "Remove item", onClick = onRemove, tone = ActionTone.DANGER)
    }
}

@Composable
private fun BookingConfirmStep(viewModel: BookingLifecycleViewModel) {
    val customer = viewModel.selectedCustomer
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        AppCard {
            BookingSectionHeader(Icons.Rounded.CheckCircle, "Confirm Booking", BrandSoft, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(AppSpacing.xs))
            Text(customer?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(customer?.mobile.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                BookingDateCell("Pickup", viewModel.pickupDate, StatusInfoSoft, StatusInfo, Modifier.weight(1f))
                BookingDateCell("Return", viewModel.returnDate, StatusWarningSoft, StatusWarning, Modifier.weight(1f))
            }
            if (viewModel.notes.isNotBlank()) {
                Spacer(Modifier.height(AppSpacing.xs))
                Text("Notes", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Text(viewModel.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }

        AppCard {
            Text("Items (${viewModel.draftLines.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            viewModel.draftLines.forEachIndexed { index, line ->
                val item = viewModel.itemOptions.firstOrNull { it.id == line.itemId } ?: return@forEachIndexed
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookingItemVisual(item.imageUrl, item.itemName, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(item.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${item.itemCode} · ${item.categoryName}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    }
                    Text("× ${line.quantity}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                if (index < viewModel.draftLines.lastIndex) HorizontalDivider(color = AppBorder)
            }
        }
    }
}

@Composable
private fun BookingLegacyEditForm(viewModel: BookingLifecycleViewModel, detail: BookingDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        BookingLegacyCustomerFormCard(viewModel)
        BookingDatesAndNotesCard(viewModel)
        BookingSectionHeader(Icons.Rounded.Inventory2, "Items (${viewModel.draftLines.size})", StatusInfoSoft, StatusInfo)
        viewModel.draftLines.forEachIndexed { index, line ->
            val categories = viewModel.itemOptions.distinctBy { it.categoryId }.map { it.categoryId to it.categoryName }
            val used = viewModel.draftLines.mapIndexedNotNull { i, value -> if (i != index) value.itemId.takeIf(String::isNotBlank) else null }.toSet()
            val choices = viewModel.itemOptions.filter { item -> item.categoryId == line.categoryId && (item.id == line.itemId || item.id !in used) }
            val selectedItem = viewModel.itemOptions.firstOrNull { it.id == line.itemId }
            BookingDraftItemCard(
                index = index,
                line = line,
                selectedItem = selectedItem,
                categories = categories,
                itemChoices = choices,
                canRemove = viewModel.draftLines.size > 1,
                busy = viewModel.actionBusy,
                onCategory = { viewModel.setLineCategory(index, it) },
                onItem = { viewModel.setLineItem(index, it) },
                onQuantity = { viewModel.setLineQuantity(index, it) },
                onRemove = { viewModel.removeLine(index) }
            )
        }
        SecondaryButton(
            text = "Add Item",
            onClick = viewModel::addLine,
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.actionBusy && viewModel.draftLines.size < 20,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) }
        )
        if (viewModel.availabilityByItem.isNotEmpty()) {
            Text("Availability is based on the selected pickup and return dates.", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        }
        if (detail.booking.rawStatus != "BOOKED") {
            InlineMessage("This booking is no longer editable.", true)
        }
    }
}

@Composable
private fun BookingLegacyCustomerFormCard(viewModel: BookingLifecycleViewModel) {
    val selected = viewModel.selectedCustomer
    AppCard {
        BookingSectionHeader(Icons.Rounded.Person, "Customer Information", BrandSoft, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(AppSpacing.xs))
        AppTextField(
            value = viewModel.customerSearch,
            onValueChange = viewModel::setCustomerSearch,
            label = "Search customer",
            enabled = !viewModel.actionBusy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
        )
        Spacer(Modifier.height(AppSpacing.xs))
        BookingSelector(
            label = "Customer",
            selectedId = viewModel.selectedCustomerId,
            options = viewModel.customerOptions.map { it.id to "${it.name} · ${it.mobile}" },
            onSelect = viewModel::setSelectedCustomer,
            enabled = !viewModel.actionBusy
        )
        if (selected != null) {
            Spacer(Modifier.height(AppSpacing.xs))
            Text(selected.mobile, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
        }
    }
}

@Composable
private fun BookingDraftItemCard(
    index: Int,
    line: BookingDraftLine,
    selectedItem: BookingOptionItem?,
    categories: List<Pair<String, String>>,
    itemChoices: List<BookingOptionItem>,
    canRemove: Boolean,
    busy: Boolean,
    onCategory: (String) -> Unit,
    onItem: (String) -> Unit,
    onQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            BookingItemVisual(selectedItem?.imageUrl, selectedItem?.itemName ?: "Item ${index + 1}")
            Column(Modifier.weight(1f)) {
                Text(selectedItem?.itemName ?: "Item ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (selectedItem != null) {
                    Text("${selectedItem.categoryName} · ${selectedItem.itemCode}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    Text("Available ${viewModelSafeAvailability(selectedItem)}", style = MaterialTheme.typography.labelSmall, color = DashboardItems)
                }
            }
            if (canRemove) {
                SoftActionButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = "Remove item",
                    onClick = onRemove,
                    enabled = !busy,
                    tone = ActionTone.DANGER
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.xs))
        BookingSelector("Category", line.categoryId, categories, onCategory, enabled = !busy)
        Spacer(Modifier.height(AppSpacing.xs))
        BookingSelector(
            label = "Item",
            selectedId = line.itemId,
            options = itemChoices.map { it.id to "${it.itemCode} · ${it.itemName}" },
            onSelect = onItem,
            enabled = !busy && line.categoryId.isNotBlank()
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Quantity", style = MaterialTheme.typography.labelLarge)
            QuantityStepper(line.quantity, 1, selectedItem?.totalQuantity?.coerceAtLeast(1) ?: 100000, !busy, onQuantity)
        }
    }
}

private fun viewModelSafeAvailability(item: BookingOptionItem): Int = item.totalQuantity

@Composable
private fun BookingDatesAndNotesCard(viewModel: BookingLifecycleViewModel) {
    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    val pickup = runCatching { LocalDate.parse(viewModel.pickupDate) }.getOrElse { today }
    AppCard {
        BookingSectionHeader(Icons.Rounded.EventNote, "Booking Details", StatusSuccessSoft, StatusSuccess)
        Spacer(Modifier.height(AppSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            BookingDatePickerField("Pickup Date", viewModel.pickupDate, today, Modifier.weight(1f), !viewModel.actionBusy, viewModel::setPickupDate)
            BookingDatePickerField("Return Date", viewModel.returnDate, pickup.plusDays(1), Modifier.weight(1f), !viewModel.actionBusy, viewModel::setReturnDate)
        }
        Spacer(Modifier.height(AppSpacing.xs))
        AppTextField(
            value = viewModel.notes,
            onValueChange = viewModel::setNotes,
            label = "Notes (Optional)",
            enabled = !viewModel.actionBusy,
            singleLine = false,
            minLines = 2,
            maxLines = 3
        )
    }
}

@Composable
private fun BookingDetailsPanel(
    detail: BookingDetailData,
    busy: Boolean,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPickup: () -> Unit,
    onReturn: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val booking = detail.booking
    val confirmed = booking.confirmationState != "RESERVED"
    val canPickup = confirmed && booking.rawStatus in setOf("BOOKED", "PARTIALLY_GIVEN") && detail.items.any { it.remainingToGive > 0 }
    val canReturn = booking.rawStatus in setOf("PARTIALLY_GIVEN", "GIVEN", "PARTIALLY_RETURNED") && detail.items.any { it.pendingToReturn > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        AppCard {
            Column {
                Text(booking.bookingNo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(booking.customerName, style = MaterialTheme.typography.titleSmall)
                Text(booking.customerMobile, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                BookingDateCell("Pickup", booking.pickupDate, StatusInfoSoft, StatusInfo, Modifier.weight(1f))
                BookingDateCell("Return", booking.returnDate, StatusWarningSoft, StatusWarning, Modifier.weight(1f))
            }
            if (!booking.notes.isNullOrBlank()) {
                Spacer(Modifier.height(AppSpacing.xs))
                Text("Notes", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                Text(booking.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            HorizontalDivider(color = AppBorder)
            Spacer(Modifier.height(AppSpacing.xs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SoftActionButton(
                    Icons.Rounded.Edit,
                    "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !busy && canEdit,
                    tone = ActionTone.INFO
                )
                SoftActionButton(
                    Icons.Rounded.Chat,
                    "WhatsApp",
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.TEAL
                )
                SoftActionButton(
                    Icons.Rounded.Call,
                    "Call",
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    tone = ActionTone.SUCCESS
                )
            }
        }

        if (booking.rawStatus == "BOOKED") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                if (!confirmed) {
                    PrimaryButton("Confirm", onConfirm, Modifier.weight(1f), enabled = !busy)
                }
                SecondaryButton("Cancel Booking", onCancel, Modifier.weight(1f), enabled = !busy)
            }
        }

        if (canPickup || canReturn) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                if (canPickup) {
                    PrimaryButton(
                        text = if (booking.rawStatus == "PARTIALLY_GIVEN") "Remaining Pickup" else "Pickup",
                        onClick = onPickup,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        icon = { Icon(Icons.Rounded.LocalShipping, contentDescription = null) }
                    )
                }
                if (canReturn) {
                    PrimaryButton(
                        text = if (booking.rawStatus == "PARTIALLY_RETURNED") "Remaining Return" else "Return",
                        onClick = onReturn,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingItemsPanel(items: List<BookingDetailItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        if (items.isEmpty()) {
            EmptyState("No booking items")
        } else {
            items.forEach { item ->
                AppCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        BookingItemVisual(item.imageUrl, item.itemName)
                        Column(Modifier.weight(1f)) {
                            Text(item.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${item.categoryName} · ${item.itemCode}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                        }
                        Text("Qty ${item.bookedQty}", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(AppSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StatusBadge("Booked ${item.bookedQty}", BadgeTone.INFO)
                        StatusBadge("Picked ${item.givenQty}", if (item.givenQty > 0) BadgeTone.WARNING else BadgeTone.NEUTRAL)
                        StatusBadge("Returned ${item.returnedQty}", if (item.returnedQty == item.bookedQty) BadgeTone.SUCCESS else BadgeTone.NEUTRAL)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Remaining pickup ${item.remainingToGive} · Pending return ${item.pendingToReturn}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                }
            }
        }
    }
}

@Composable
private fun BookingPickupPanel(detail: BookingDetailData, viewModel: BookingLifecycleViewModel) {
    val confirmed = detail.booking.confirmationState != "RESERVED"
    val remaining = detail.items.filter { it.remainingToGive > 0 }

    AppCard {
        BookingSectionHeader(Icons.Rounded.LocalShipping, "Pickup (Handover)", StatusInfoSoft, StatusInfo)
        Spacer(Modifier.height(AppSpacing.xs))
        Text("Pickup date · ${screen4Date(detail.booking.pickupDate)}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)

        when {
            !confirmed -> {
                Spacer(Modifier.height(AppSpacing.sm))
                InlineMessage("Confirm the reserved booking before pickup.", true)
            }
            remaining.isEmpty() -> {
                Spacer(Modifier.height(AppSpacing.sm))
                InlineMessage("All booked items have been picked up.", false)
            }
            else -> {
                Spacer(Modifier.height(AppSpacing.sm))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Items", style = MaterialTheme.typography.titleSmall)
                    SecondaryButton("All", viewModel::fillAllPickup, enabled = !viewModel.actionBusy)
                }
                remaining.forEach { item ->
                    Spacer(Modifier.height(AppSpacing.xs))
                    BookingActionQuantityRow(
                        item = item,
                        value = viewModel.pickupNow[item.bookingItemId] ?: 0,
                        max = item.remainingToGive,
                        valueLabel = "Pickup Qty",
                        enabled = !viewModel.actionBusy,
                        onChange = { viewModel.setPickupNow(item.bookingItemId, it) }
                    )
                }
                Spacer(Modifier.height(AppSpacing.sm))
                AppTextField(
                    value = viewModel.pickupNotes,
                    onValueChange = viewModel::setPickupNotes,
                    label = "Pickup Notes (Optional)",
                    enabled = !viewModel.actionBusy,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )
                Spacer(Modifier.height(AppSpacing.xs))
                PrimaryButton(
                    text = "Confirm Pickup",
                    onClick = viewModel::savePickup,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.actionBusy,
                    loading = viewModel.actionBusy,
                    icon = { Icon(Icons.Rounded.Save, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun BookingReturnPanel(detail: BookingDetailData, viewModel: BookingLifecycleViewModel) {
    val pending = detail.items.filter { it.pendingToReturn > 0 }

    AppCard {
        BookingSectionHeader(Icons.Rounded.Inventory2, "Return Items", StatusWarningSoft, StatusWarning)
        Spacer(Modifier.height(AppSpacing.xs))
        Text("Return date · ${screen4Date(detail.booking.returnDate)}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)

        if (pending.isEmpty()) {
            Spacer(Modifier.height(AppSpacing.sm))
            InlineMessage(if (detail.booking.rawStatus == "RETURNED") "All picked items have been returned." else "No picked items are pending return.", false)
        } else {
            Spacer(Modifier.height(AppSpacing.sm))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Items", style = MaterialTheme.typography.titleSmall)
                SecondaryButton("All", viewModel::fillAllReturn, enabled = !viewModel.actionBusy)
            }
            pending.forEach { item ->
                Spacer(Modifier.height(AppSpacing.xs))
                BookingActionQuantityRow(
                    item = item,
                    value = viewModel.returnNow[item.bookingItemId] ?: 0,
                    max = item.pendingToReturn,
                    valueLabel = "Return Qty",
                    enabled = !viewModel.actionBusy,
                    onChange = { viewModel.setReturnNow(item.bookingItemId, it) }
                )
            }
            Spacer(Modifier.height(AppSpacing.sm))
            AppTextField(
                value = viewModel.returnNotes,
                onValueChange = viewModel::setReturnNotes,
                label = "Return Notes (Optional)",
                enabled = !viewModel.actionBusy,
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
            Spacer(Modifier.height(AppSpacing.xs))
            PrimaryButton(
                text = "Confirm Return",
                onClick = viewModel::saveReturn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.actionBusy,
                loading = viewModel.actionBusy,
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun BookingActionQuantityRow(
    item: BookingDetailItem,
    value: Int,
    max: Int,
    valueLabel: String,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(Modifier.padding(AppSpacing.sm), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                BookingItemVisual(item.imageUrl, item.itemName, 46.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.itemName, style = MaterialTheme.typography.titleSmall)
                    Text("Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    Text("Remaining $max", style = MaterialTheme.typography.labelSmall, color = StatusInfo)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(valueLabel, style = MaterialTheme.typography.labelMedium)
                QuantityStepper(value, 0, max, enabled, onChange)
            }
        }
    }
}

@Composable
private fun BookingHistoryPanel(detail: BookingDetailData) {
    val booking = detail.booking
    val totalGiven = detail.items.sumOf { it.givenQty }
    val totalReturned = detail.items.sumOf { it.returnedQty }
    val bookedLabel = if (booking.confirmationState == "RESERVED") "Reserved" else "Booked"
    val steps = listOf(
        Triple(bookedLabel, true, "Booking date ${screen4Date(booking.bookingDate)}"),
        Triple("Picked Up", totalGiven > 0, "Scheduled pickup ${screen4Date(booking.pickupDate)}"),
        Triple("Returned", totalReturned > 0, "Scheduled return ${screen4Date(booking.returnDate)}"),
        Triple("Completed", booking.displayStatus.equals("FULL_RETURN", true), if (booking.displayStatus.equals("FULL_RETURN", true)) "Lifecycle complete" else "Pending")
    )

    AppCard {
        BookingSectionHeader(Icons.Rounded.Schedule, "Booking History", StatusSuccessSoft, StatusSuccess)
        Spacer(Modifier.height(AppSpacing.sm))
        steps.forEachIndexed { index, (label, complete, description) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.Top) {
                Surface(
                    color = if (complete) StatusSuccessSoft else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (complete) StatusSuccess else AppTextMuted,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                }
            }
            if (index < steps.lastIndex) {
                Spacer(Modifier.height(AppSpacing.xs))
                HorizontalDivider(modifier = Modifier.padding(start = 42.dp), color = AppBorder)
                Spacer(Modifier.height(AppSpacing.xs))
            }
        }
        if (booking.rawStatus == "CANCELLED") {
            Spacer(Modifier.height(AppSpacing.sm))
            StatusBadge("Cancelled", BadgeTone.ERROR)
        }
    }
}

@Composable
private fun BookingSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = background, contentColor = foreground, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BookingItemVisual(
    imageUrl: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    Surface(modifier = Modifier.size(size), color = DashboardGivenSoft, contentColor = DashboardGiven, shape = MaterialTheme.shapes.medium) {
        if (imageUrl.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Inventory2, contentDescription = name, modifier = Modifier.size(24.dp))
            }
        } else {
            AsyncImage(model = imageUrl, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun QuantityInputControl(
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        SoftActionButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease quantity",
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = enabled && value > min,
            tone = ActionTone.INFO
        )
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, AppBorder), shape = MaterialTheme.shapes.medium) {
            Box(Modifier.size(width = 54.dp, height = 44.dp), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = text,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit).take(6)
                        text = digits
                        digits.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
                    },
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.width(48.dp)
                )
            }
        }
        SoftActionButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase quantity",
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = enabled && value < max,
            tone = ActionTone.INFO
        )
    }
}

@Composable
private fun QuantityStepper(
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) = QuantityInputControl(value, min, max.coerceAtLeast(min), enabled, onChange)

@Composable
private fun BookingSelector(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.first == selectedId }?.second ?: "Select $label"
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
            Text(selected, maxLines = 1, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.second) }, onClick = { onSelect(option.first); expanded = false })
            }
        }
    }
}

@Composable
private fun Screen3More(
    role: UserRole,
    businessDate: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modules = buildList {
        addAll(listOf("Items", "Pickup", "Returns", "Reports"))
        if (role != UserRole.STAFF) addAll(listOf("Categories", "Users", "Settings", "Audit Logs"))
    }
    var confirmLogout by remember { mutableStateOf(false) }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        item { Spacer(Modifier.height(AppSpacing.xxs)) }
        item { MainScreenDateRow(businessDate) }
        item { Text("More", style = MaterialTheme.typography.headlineMedium) }
        items(modules) { name -> AppCard { Text(name, style = MaterialTheme.typography.titleSmall) } }
        item { Spacer(Modifier.height(AppSpacing.sm)) }
        item {
            SecondaryButton(
                text = "Logout",
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Rounded.Logout, contentDescription = null) }
            )
        }
        item { Spacer(Modifier.height(AppSpacing.md)) }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You will return to the sign-in screen.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } }
        )
    }
}

private fun bookingListViews(): List<Pair<String, String>> = listOf(
    "RESERVED" to "Reserved",
    "BOOKED" to "Booked",
    "PICKED_UP" to "Picked Up",
    "RETURNED" to "Returned",
    "ALL" to "All"
)

private fun bookingStatusFilters(): List<Pair<String, String>> = listOf(
    "" to "All statuses",
    "RESERVED" to "Reserved",
    "BOOKED" to "Booked",
    "PARTIALLY_GIVEN" to "Part Pickup",
    "GIVEN" to "Full Pickup",
    "PARTIALLY_RETURNED" to "Part Return",
    "RETURNED" to "Full Return",
    "CANCELLED" to "Cancelled"
)

private fun bookingSortOptions(): List<Pair<String, String>> = listOf(
    "NEWEST" to "Newest",
    "OLDEST" to "Oldest",
    "PICKUP_ASC" to "Pickup earliest",
    "PICKUP_DESC" to "Pickup latest",
    "RETURN_ASC" to "Return earliest",
    "RETURN_DESC" to "Return latest"
)

private fun bookingStatusFilterLabel(value: String): String =
    bookingStatusFilters().firstOrNull { it.first == value }?.second ?: "All statuses"

private fun bookingStatusCopy(booking: LifecycleBookingSummary): BookingStatusCopy = when (booking.displayStatus.uppercase()) {
    "RESERVED" -> BookingStatusCopy("Reserved Done", BadgeTone.INFO, "Confirmation Pending", BadgeTone.WARNING)
    "BOOKED" -> BookingStatusCopy("Booking Confirmed", BadgeTone.SUCCESS, "Pickup Pending", BadgeTone.WARNING)
    "PART_PICKUP" -> BookingStatusCopy("Part Pickup Done", BadgeTone.INFO, "Remaining Pickup Pending", BadgeTone.WARNING)
    "FULL_PICKUP" -> BookingStatusCopy("Full Pickup Done", BadgeTone.SUCCESS, "Return Pending", BadgeTone.WARNING)
    "PART_RETURN" -> BookingStatusCopy("Part Return Done", BadgeTone.INFO, "Remaining Return Pending", BadgeTone.WARNING)
    "FULL_RETURN" -> BookingStatusCopy("Returned Done", BadgeTone.SUCCESS, "Completed", BadgeTone.SUCCESS)
    "CANCELLED" -> BookingStatusCopy("Cancelled", BadgeTone.ERROR, null)
    else -> BookingStatusCopy(screen3LifecycleLabel(booking), BadgeTone.NEUTRAL, null)
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

private fun bookingEditDirty(viewModel: BookingLifecycleViewModel, detail: BookingDetailData): Boolean {
    val originalLines = detail.items.associate { it.itemId to it.bookedQty }
    val currentLines = viewModel.draftLines.associate { it.itemId to it.quantity }
    return viewModel.selectedCustomerId != detail.booking.customerId ||
        viewModel.pickupDate != detail.booking.pickupDate ||
        viewModel.returnDate != detail.booking.returnDate ||
        viewModel.notes != detail.booking.notes.orEmpty() ||
        originalLines != currentLines
}

private fun screen4Date(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH))
}.getOrDefault(value.ifBlank { "—" })

private fun openDialer(context: Context, mobile: String) {
    val digits = mobile.filter(Char::isDigit)
    if (digits.isNotBlank()) {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
    }
}

private fun openWhatsApp(context: Context, mobile: String, message: String) {
    val digits = mobile.filter(Char::isDigit)
    val number = if (digits.length == 10) "91$digits" else digits
    if (number.isNotBlank()) {
        val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
