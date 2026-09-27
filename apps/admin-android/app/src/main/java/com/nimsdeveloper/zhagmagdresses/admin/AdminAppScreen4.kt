package com.nimsdeveloper.zhagmagdresses.admin

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingListItemPreview
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionCustomer
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.StaffAccess
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.data.WhatsAppTemplateBundle
import com.nimsdeveloper.zhagmagdresses.admin.data.hasAccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCompactScrollableTabs
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppPageHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppScreenTitle
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyStateVariant
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactMetaBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
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
import kotlinx.coroutines.delay

private enum class Screen4Destination { DASHBOARD, CUSTOMERS, BOOKINGS, MORE }

private data class PendingWhatsAppPreview4(
    val customerName: String,
    val mobile: String,
    val message: String,
    val onConfirm: () -> Unit
)

private data class PendingWhatsAppTemplateList4(
    val bundle: WhatsAppTemplateBundle,
    val onReady: (String, String) -> Unit
)

@Composable
internal fun ZhagmagAdminRootScreen4(
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel,
    bookingListViewModel: BookingListViewModel4,
    customerViewModel: CustomerScreen3ViewModel,
    reportsViewModel: Screen8ReportsViewModel
) {
    when (val auth = adminViewModel.authState) {
        AuthState.Checking, AuthState.SignedOut, is AuthState.SessionCheckFailed -> ZhagmagAdminRoot(adminViewModel, bookingViewModel)
        is AuthState.SignedIn -> Screen4AuthenticatedApp(
            user = auth.user,
            adminViewModel = adminViewModel,
            bookingViewModel = bookingViewModel,
            bookingListViewModel = bookingListViewModel,
            customerViewModel = customerViewModel,
            reportsViewModel = reportsViewModel
        )
    }
}

@Composable
private fun Screen4AuthenticatedApp(
    user: SessionUser,
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel,
    bookingListViewModel: BookingListViewModel4,
    customerViewModel: CustomerScreen3ViewModel,
    reportsViewModel: Screen8ReportsViewModel
) {
    val canDashboard = user.hasAccess(StaffAccess.DASHBOARD)
    val canCustomers = user.hasAccess(StaffAccess.CUSTOMERS)
    val canBookings = user.hasAccess(StaffAccess.BOOKINGS)
    val canPickups = user.hasAccess(StaffAccess.PICKUPS)
    val canReturns = user.hasAccess(StaffAccess.RETURNS)
    val canItems = user.hasAccess(StaffAccess.ITEMS)
    val canReports = user.hasAccess(StaffAccess.REPORTS)
    val canBillingReports = user.role == UserRole.OWNER && canReports
    val homeDestination = when {
        canDashboard -> Screen4Destination.DASHBOARD
        canCustomers -> Screen4Destination.CUSTOMERS
        canBookings -> Screen4Destination.BOOKINGS
        else -> Screen4Destination.MORE
    }
    var destination by rememberSaveable(user.id) { mutableStateOf(homeDestination) }
    var moreHubRequest by rememberSaveable(user.id) { mutableStateOf(0) }
    var moreNestedOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    var billingBookingId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var dashboardDrilldownActive by rememberSaveable(user.id) { mutableStateOf(false) }
    var dashboardMoreDestination by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var dashboardItemItemsTab by rememberSaveable(user.id) { mutableStateOf(false) }
    var dashboardItemCategoryId by rememberSaveable(user.id) { mutableStateOf("") }
    var dashboardBillingPaymentFilter by rememberSaveable(user.id) { mutableStateOf("") }
    var dashboardBillingStatusFilter by rememberSaveable(user.id) { mutableStateOf("") }
    var consumedMutationVersion by rememberSaveable { mutableStateOf(bookingViewModel.mutationVersion) }
    var confirmExit by remember { mutableStateOf(false) }
    var whatsappPreparing by remember { mutableStateOf(false) }
    var whatsappError by remember { mutableStateOf<String?>(null) }
    var pendingWhatsAppTemplates by remember { mutableStateOf<PendingWhatsAppTemplateList4?>(null) }
    var pendingWhatsAppPreview by remember { mutableStateOf<PendingWhatsAppPreview4?>(null) }
    val dashboardScreenState = rememberDashboardScreenState()
    val customerListState = rememberLazyListState()
    val bookingListState = rememberLazyListState()
    val editorOpen = bookingViewModel.editorOpen && canBookings
    val context = LocalContext.current
    val activity = context as? Activity

    val returnToDashboardFromDrilldown: () -> Unit = {
        bookingListViewModel.clearDashboardFilter()
        reportsViewModel.clearDashboardDrilldown()
        dashboardDrilldownActive = false
        dashboardMoreDestination = null
        dashboardItemItemsTab = false
        dashboardItemCategoryId = ""
        dashboardBillingPaymentFilter = ""
        dashboardBillingStatusFilter = ""
        billingBookingId = null
        moreNestedOpen = false
        destination = Screen4Destination.DASHBOARD
    }

    val openDashboardBookingFilter: (BookingListFilter4) -> Unit = { filter ->
        bookingListViewModel.setDashboardFilter(filter)
        dashboardDrilldownActive = true
        dashboardMoreDestination = null
        destination = Screen4Destination.BOOKINGS
    }

    val openDashboardMore: (MoreDestination4, Boolean, String, String, String) -> Unit =
        { moreDestination, itemsTab, categoryId, paymentFilter, statusFilter ->
            dashboardDrilldownActive = true
            dashboardMoreDestination = moreDestination.name
            dashboardItemItemsTab = itemsTab
            dashboardItemCategoryId = categoryId
            dashboardBillingPaymentFilter = paymentFilter
            dashboardBillingStatusFilter = statusFilter
            destination = Screen4Destination.MORE
        }

    val openDashboardReport: (String, String, String, String) -> Unit =
        { type, datePreset, dashboardFilter, status ->
            reportsViewModel.openFromDashboard(
                type = type,
                datePreset = datePreset,
                dashboardFilter = dashboardFilter,
                status = status
            )
            openDashboardMore(MoreDestination4.REPORTS, false, "", "", "")
        }

    val onDashboardKpiClick: (DashboardKpiAction, String?) -> Unit = { action, categoryId ->
        when (action) {
            DashboardKpiAction.TODAY_PICKUPS -> openDashboardBookingFilter(BookingListFilter4.TODAY_PICKUP)
            DashboardKpiAction.TODAY_RETURNS -> openDashboardBookingFilter(BookingListFilter4.TODAY_RETURN)
            DashboardKpiAction.MISSED_PICKUPS -> openDashboardBookingFilter(BookingListFilter4.MISSED_PICKUP)
            DashboardKpiAction.OVERDUE_RETURNS -> openDashboardBookingFilter(BookingListFilter4.OVERDUE)

            DashboardKpiAction.RESERVED -> openDashboardBookingFilter(BookingListFilter4.RESERVED)
            DashboardKpiAction.BOOKED -> openDashboardBookingFilter(BookingListFilter4.BOOKED)
            DashboardKpiAction.PART_PICKED_UP -> openDashboardBookingFilter(BookingListFilter4.PART_PICKUP)
            DashboardKpiAction.FULL_PICKED_UP -> openDashboardBookingFilter(BookingListFilter4.FULL_PICKUP)
            DashboardKpiAction.PART_RETURN -> openDashboardBookingFilter(BookingListFilter4.PART_RETURN)
            DashboardKpiAction.FULL_RETURNED -> openDashboardBookingFilter(BookingListFilter4.FULL_RETURN)
            DashboardKpiAction.CANCELLED -> openDashboardBookingFilter(BookingListFilter4.CANCELLED)
            DashboardKpiAction.ACTIVE_RENTAL_ORDERS -> openDashboardBookingFilter(BookingListFilter4.ACTIVE_RENTAL)

            DashboardKpiAction.PENDING_PAYMENT -> openDashboardBookingFilter(BookingListFilter4.PAYMENT_PENDING)
            DashboardKpiAction.PART_PAYMENT -> openDashboardBookingFilter(BookingListFilter4.PAYMENT_PART)
            DashboardKpiAction.FULL_PAYMENT -> openDashboardBookingFilter(BookingListFilter4.PAYMENT_FULL)
            DashboardKpiAction.DRAFT_BILLS ->
                openDashboardMore(MoreDestination4.BILLING, false, "", "", "DRAFT")
            DashboardKpiAction.FINAL_BILLS ->
                openDashboardMore(MoreDestination4.BILLING, false, "", "", "FINAL")
            DashboardKpiAction.BILLS_TODAY ->
                openDashboardReport("BILLS_REPORT", "TODAY", "TODAY", "")
            DashboardKpiAction.PENDING_BALANCE ->
                openDashboardReport("PENDING_BALANCE", "ALL_TIME", "ALL_TIME", "")
            DashboardKpiAction.TOTAL_RECEIVED ->
                openDashboardReport("BILLING_OVERVIEW", "ALL_TIME", "ALL_TIME", "")

            DashboardKpiAction.AVAILABLE_NOW ->
                openDashboardReport("AVAILABILITY", "TODAY", "AVAILABLE_NOW", "")
            DashboardKpiAction.PICKUP_PENDING_QTY ->
                openDashboardReport("AVAILABILITY", "TODAY", "PICKUP_PENDING", "")
            DashboardKpiAction.CURRENTLY_OUT_QTY ->
                openDashboardReport("CURRENTLY_OUT", "ALL_TIME", "ALL_TIME", "")
            DashboardKpiAction.TOTAL_QUANTITY,
            DashboardKpiAction.TOTAL_ITEMS ->
                openDashboardMore(MoreDestination4.ITEM_MANAGEMENT, true, "", "", "")
            DashboardKpiAction.CATEGORIES ->
                openDashboardMore(MoreDestination4.ITEM_MANAGEMENT, false, "", "", "")
            DashboardKpiAction.LOW_STOCK ->
                openDashboardReport("AVAILABILITY", "TODAY", "LOW_STOCK", "")
            DashboardKpiAction.UNAVAILABLE ->
                openDashboardReport("AVAILABILITY", "TODAY", "UNAVAILABLE", "")

            DashboardKpiAction.TOTAL_CUSTOMERS -> {
                dashboardDrilldownActive = true
                dashboardMoreDestination = null
                destination = Screen4Destination.CUSTOMERS
            }
            DashboardKpiAction.NEW_CUSTOMERS ->
                openDashboardReport("NEW_RETURNING_CUSTOMERS", "THIS_MONTH", "NEW", "")
            DashboardKpiAction.RETURNING_CUSTOMERS ->
                openDashboardReport("NEW_RETURNING_CUSTOMERS", "THIS_MONTH", "RETURNING", "")
            DashboardKpiAction.FREQUENT_CUSTOMERS ->
                openDashboardReport("FREQUENT_CUSTOMERS", "ALL_TIME", "ALL_TIME", "")
            DashboardKpiAction.ACTIVE_RENTAL_CUSTOMERS ->
                openDashboardReport("ACTIVE_RENTALS", "ALL_TIME", "ALL_TIME", "")
            DashboardKpiAction.CUSTOMER_EXCEPTIONS ->
                openDashboardReport("CUSTOMER_EXCEPTIONS", "ALL_TIME", "ALL_TIME", "")

            DashboardKpiAction.CATEGORY_INVENTORY ->
                openDashboardMore(
                    MoreDestination4.ITEM_MANAGEMENT,
                    true,
                    categoryId.orEmpty(),
                    "",
                    ""
                )
        }
    }

    LaunchedEffect(whatsappError) {
        if (!whatsappError.isNullOrBlank()) {
            delay(5100)
            whatsappError = null
        }
    }

    LaunchedEffect(user.id, user.staffPermissions, destination) {
        val allowed = when (destination) {
            Screen4Destination.DASHBOARD -> canDashboard
            Screen4Destination.CUSTOMERS -> canCustomers
            Screen4Destination.BOOKINGS -> canBookings
            Screen4Destination.MORE -> true
        }
        if (!allowed) destination = homeDestination
    }

    LaunchedEffect(destination) {
        if (destination != Screen4Destination.MORE) moreNestedOpen = false
    }

    LaunchedEffect(destination, editorOpen, bookingViewModel.mutationVersion) {
        if (!editorOpen) {
            val currentBusinessDay = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()
            val cachedBusinessDay = adminViewModel.dashboardState.data?.today
            if (canDashboard && !cachedBusinessDay.isNullOrBlank() && cachedBusinessDay != currentBusinessDay && !adminViewModel.dashboardState.loading) {
                adminViewModel.loadDashboard()
            }
            when (destination) {
                Screen4Destination.CUSTOMERS -> customerViewModel.ensureLoaded()
                Screen4Destination.BOOKINGS -> {
                    bookingListViewModel.ensureLoaded()
                    if (bookingViewModel.mutationVersion != consumedMutationVersion) {
                        consumedMutationVersion = bookingViewModel.mutationVersion
                        bookingListViewModel.refresh()
                    }
                }
                Screen4Destination.DASHBOARD -> {
                    if (bookingViewModel.mutationVersion != consumedMutationVersion) {
                        consumedMutationVersion = bookingViewModel.mutationVersion
                    }
                    adminViewModel.ensureDashboard()
                }
                Screen4Destination.MORE -> Unit
            }
        }
    }

    BackHandler {
        when {
            editorOpen -> Unit
            dashboardDrilldownActive && destination != Screen4Destination.MORE ->
                returnToDashboardFromDrilldown()
            moreNestedOpen -> Unit
            confirmExit -> confirmExit = false
            destination != homeDestination -> destination = homeDestination
            else -> confirmExit = true
        }
    }

    val businessDate = adminViewModel.dashboardState.data?.today.orEmpty()
    val refreshBusy = if (editorOpen) {
        bookingViewModel.detailState.loading ||
            bookingViewModel.availabilityLoading ||
            bookingViewModel.actionBusy ||
            bookingViewModel.newCustomerBusy
    } else {
        when (destination) {
            Screen4Destination.DASHBOARD -> adminViewModel.dashboardState.loading
            Screen4Destination.CUSTOMERS -> customerViewModel.state.loading || customerViewModel.state.actionBusy
            Screen4Destination.BOOKINGS -> bookingListViewModel.state.loading
            Screen4Destination.MORE -> false
        }
    }
    val refreshAction: () -> Unit = if (editorOpen) {
        bookingViewModel::refreshEditor
    } else {
        when (destination) {
            Screen4Destination.DASHBOARD -> adminViewModel::loadDashboard
            Screen4Destination.CUSTOMERS -> customerViewModel::refresh
            Screen4Destination.BOOKINGS -> bookingListViewModel::refresh
            Screen4Destination.MORE -> ({})
        }
    }
    val confirmedWhatsAppComposer: (String, String?, String?, (String, String) -> Unit) -> Unit =
        { customerId, bookingId, _, onReady ->
            if (!canCustomers) {
                whatsappError = "Customer access is not enabled for this account."
            } else if (!whatsappPreparing && !customerViewModel.state.actionBusy) {
                whatsappPreparing = true
                whatsappError = null
                pendingWhatsAppTemplates = null
                pendingWhatsAppPreview = null
                customerViewModel.prepareWhatsAppTemplates(
                    customerId = customerId,
                    bookingId = bookingId,
                    onSuccess = { bundle ->
                        whatsappPreparing = false
                        val only = bundle.templates.singleOrNull()
                        when {
                            only != null -> {
                                pendingWhatsAppPreview = PendingWhatsAppPreview4(
                                    customerName = bundle.customerName,
                                    mobile = bundle.mobile,
                                    message = only.message,
                                    onConfirm = { onReady(bundle.mobile, only.message) }
                                )
                            }
                            bundle.templates.isNotEmpty() -> {
                                pendingWhatsAppTemplates = PendingWhatsAppTemplateList4(bundle, onReady)
                            }
                            else -> {
                                whatsappError = "No active WhatsApp template is available for this order status."
                            }
                        }
                    },
                    onFailure = { message ->
                        whatsappPreparing = false
                        if (editorOpen || destination != Screen4Destination.CUSTOMERS) {
                            whatsappError = message
                        }
                    }
                )
            }
        }

    Scaffold(
        topBar = {
            if (!editorOpen && destination == Screen4Destination.MORE) {
                AdminMainTopBar(
                    branding = adminViewModel.branding,
                    user = user,
                    onLogout = adminViewModel::logout
                )
            } else {
                AdminMainTopBar(
                    branding = adminViewModel.branding,
                    user = user,
                    onRefresh = refreshAction,
                    refreshing = refreshBusy
                )
            }
        },
        bottomBar = {
            if (!editorOpen && !moreNestedOpen && !dashboardDrilldownActive) {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    if (canDashboard) {
                        NavigationBarItem(
                            selected = destination == Screen4Destination.DASHBOARD,
                            onClick = { destination = Screen4Destination.DASHBOARD },
                            icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                            label = { Text("Dashboard") }
                        )
                    }
                    if (canCustomers) {
                        NavigationBarItem(
                            selected = destination == Screen4Destination.CUSTOMERS,
                            onClick = { destination = Screen4Destination.CUSTOMERS },
                            icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                            label = { Text("Customers") }
                        )
                    }
                    if (canBookings) {
                        NavigationBarItem(
                            selected = destination == Screen4Destination.BOOKINGS,
                            onClick = { destination = Screen4Destination.BOOKINGS },
                            icon = { Icon(Icons.Rounded.EventNote, contentDescription = null) },
                            label = { Text("Bookings") }
                        )
                    }
                    NavigationBarItem(
                        selected = destination == Screen4Destination.MORE,
                        onClick = {
                            moreHubRequest += 1
                            destination = Screen4Destination.MORE
                        },
                        icon = { Icon(Icons.Rounded.Menu, contentDescription = null) },
                        label = { Text("More") }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (editorOpen) {
            BookingWorkspaceScreen5(
                viewModel = bookingViewModel,
                currentUser = user,
                branding = adminViewModel.branding,
                businessDate = businessDate,
                composeWhatsApp = confirmedWhatsAppComposer,
                contactBusy = whatsappPreparing || customerViewModel.state.actionBusy,
                canManageCustomers = canCustomers,
                canPickup = canPickups,
                canReturn = canReturns,
                onBilling = { bookingId ->
                    billingBookingId = bookingId
                    bookingViewModel.backFromEditor()
                    destination = Screen4Destination.MORE
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (destination) {
                Screen4Destination.DASHBOARD -> PullToRefreshBox(
                    isRefreshing = adminViewModel.dashboardState.loading && adminViewModel.dashboardState.data != null,
                    onRefresh = adminViewModel::loadDashboard,
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                ) {
                    DashboardScreenV2(
                        state = adminViewModel.dashboardState,
                        revision = adminViewModel.dashboardRevision,
                        uiState = dashboardScreenState,
                        onRefresh = adminViewModel::loadDashboard,
                        onNewBooking = { bookingViewModel.openNew() },
                        onKpiClick = onDashboardKpiClick,
                        canBookings = canBookings,
                        canItems = canItems,
                        canCustomers = canCustomers,
                        canReports = canReports,
                        canBillingReports = canBillingReports
                    )
                }

                Screen4Destination.CUSTOMERS -> CustomerScreenV4(
                    state = customerViewModel.state,
                    role = user.role,
                    businessDate = businessDate,
                    onSearch = customerViewModel::search,
                    onSort = customerViewModel::sort,
                    onArchivedTab = customerViewModel::setArchived,
                    onLoadMore = customerViewModel::loadMore,
                    onRetryLoadMore = customerViewModel::retryLoadMore,
                    onRefresh = customerViewModel::refresh,
                    onSave = customerViewModel::save,
                    onArchive = customerViewModel::archive,
                    onRestore = customerViewModel::restore,
                    onDeletePermanent = customerViewModel::deletePermanently,
                    onCreateBooking = { customer ->
                        bookingViewModel.openNew(
                            BookingOptionCustomer(customer.id, customer.name, customer.mobile)
                        )
                    },
                    onWhatsApp = confirmedWhatsAppComposer,
                    canCreateBooking = canBookings,
                    listState = customerListState,
                    modifier = Modifier.padding(innerPadding)
                )

                Screen4Destination.BOOKINGS -> BookingListScreen4(
                    listViewModel = bookingListViewModel,
                    bookingViewModel = bookingViewModel,
                    businessDate = businessDate,
                    composeWhatsApp = confirmedWhatsAppComposer,
                    contactBusy = whatsappPreparing || customerViewModel.state.actionBusy,
                    canContactCustomers = canCustomers,
                    onBilling = { bookingId ->
                        billingBookingId = bookingId
                        destination = Screen4Destination.MORE
                    },
                    listState = bookingListState,
                    modifier = Modifier.padding(innerPadding)
                )

                Screen4Destination.MORE -> Screen4More(
                    user = user,
                    branding = adminViewModel.branding,
                    businessDate = businessDate,
                    hubRequest = moreHubRequest,
                    reportsViewModel = reportsViewModel,
                    openBillingBookingId = billingBookingId,
                    onBillingBookingConsumed = { billingBookingId = null },
                    directDestination = dashboardMoreDestination,
                    dashboardDrilldown = dashboardDrilldownActive,
                    dashboardItemItemsTab = dashboardItemItemsTab,
                    dashboardItemCategoryId = dashboardItemCategoryId,
                    dashboardBillingPaymentFilter = dashboardBillingPaymentFilter,
                    dashboardBillingStatusFilter = dashboardBillingStatusFilter,
                    onDashboardBack = returnToDashboardFromDrilldown,
                    onLogout = adminViewModel::logout,
                    onBrandingChanged = adminViewModel::applySettingsBranding,
                    onNestedChanged = { moreNestedOpen = it },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (!whatsappError.isNullOrBlank() && (editorOpen || destination != Screen4Destination.CUSTOMERS)) {
        PopupMessage(whatsappError, MessageTone.ERROR)
    }

    if (whatsappPreparing) {
        WhatsAppPreparingSheet()
    }

    pendingWhatsAppTemplates?.let { pending ->
        WhatsAppTemplateSelectionSheet(
            statusGroupLabel = pending.bundle.statusGroupLabel,
            templates = pending.bundle.templates,
            onDismiss = { pendingWhatsAppTemplates = null },
            onSelect = { template ->
                pendingWhatsAppTemplates = null
                pendingWhatsAppPreview = PendingWhatsAppPreview4(
                    customerName = pending.bundle.customerName,
                    mobile = pending.bundle.mobile,
                    message = template.message,
                    onConfirm = { pending.onReady(pending.bundle.mobile, template.message) }
                )
            }
        )
    }

    pendingWhatsAppPreview?.let { preview ->
        CustomerContactConfirmationSheet(
            customerName = preview.customerName,
            mobile = preview.mobile,
            action = CustomerContactAction.WHATSAPP,
            message = preview.message,
            busy = customerViewModel.state.actionBusy,
            onDismiss = { pendingWhatsAppPreview = null },
            onConfirm = {
                pendingWhatsAppPreview = null
                preview.onConfirm()
            }
        )
    }

    if (confirmExit) {
        AppConfirmDialog(
            title = "Exit app?",
            message = "Close Zhagmag Dresses Admin?",
            confirmLabel = "Exit",
            onConfirm = {
                confirmExit = false
                activity?.finish()
            },
            onDismiss = { confirmExit = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingListScreen4(
    listViewModel: BookingListViewModel4,
    bookingViewModel: BookingLifecycleViewModel,
    businessDate: String,
    composeWhatsApp: (String, String?, String?, (String, String) -> Unit) -> Unit,
    contactBusy: Boolean,
    canContactCustomers: Boolean,
    onBilling: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val state = listViewModel.state
    val context = LocalContext.current
    var sortOpen by remember { mutableStateOf(false) }
    var contactBooking by remember { mutableStateOf<LifecycleBookingSummary?>(null) }
    var contactAction by remember { mutableStateOf<CustomerContactAction?>(null) }
    var contactLaunchError by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = listViewModel::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
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
                    AppScreenTitle("Bookings", modifier = Modifier.weight(1f))
                    CompactNewBookingButton(
                        enabled = !bookingViewModel.actionBusy,
                        onClick = bookingViewModel::openNew
                    )
                }
            }
            item {
                val filters = BookingListFilter4.values().filterNot { it.dashboardOnly }
                AppCompactScrollableTabs(
                    labels = filters.map { it.label },
                    selectedIndex = filters.indexOf(state.filter).coerceAtLeast(0),
                    onSelect = { index -> listViewModel.setFilter(filters[index]) }
                )
                if (state.filter.dashboardOnly) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    StatusBadge("Dashboard Filter · ${state.filter.label}", BadgeTone.INFO)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactSearchField(
                        value = state.search,
                        onValueChange = listViewModel::search,
                        placeholder = "Search",
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        SoftActionButton(
                            icon = Icons.Rounded.Sort,
                            contentDescription = "Sort",
                            onClick = { sortOpen = true },
                            tone = ActionTone.INFO
                        )
                        DropdownMenu(
                            expanded = sortOpen,
                            onDismissRequest = { sortOpen = false }
                        ) {
                            listOf(
                                "NEWEST" to "Created Newest",
                                "OLDEST" to "Created Oldest",
                                "PICKUP_ASC" to "Pickup Earliest",
                                "PICKUP_DESC" to "Pickup Latest",
                                "RETURN_ASC" to "Return Earliest",
                                "RETURN_DESC" to "Return Latest"
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (state.sort == value) "✓ $label" else label) },
                                    onClick = {
                                        sortOpen = false
                                        listViewModel.setSort(value)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank() && !state.loaded) {
                item {
                    LoadFailureState(
                        message = state.error.orEmpty(),
                        onRetry = listViewModel::refresh
                    )
                }
            } else if (!state.error.isNullOrBlank()) {
                item { InlineStatusMessage(state.error, MessageTone.ERROR) }
            }
            if (state.loading && !state.loaded) item { LoadingState() }
            if (state.loaded && state.items.isEmpty() && !state.loading && state.error.isNullOrBlank()) {
                item {
                    EmptyState(
                        "No bookings found.",
                        variant = if (state.search.isNotBlank()) EmptyStateVariant.SEARCH_NO_RESULT else EmptyStateVariant.NORMAL
                    )
                }
            }

            state.items.forEach { booking ->
                item(key = booking.id) {
                    BookingListCard4(
                        booking = booking,
                        busy = bookingViewModel.actionBusy || contactBusy,
                        canContactCustomers = canContactCustomers,
                        onView = { bookingViewModel.openBooking(booking.id) },
                        onEdit = { bookingViewModel.openBooking(booking.id, edit = true) },
                        onCall = {
                            contactBooking = booking
                            contactAction = CustomerContactAction.CALL
                        },
                        onWhatsApp = {
                            contactBooking = booking
                            contactAction = CustomerContactAction.WHATSAPP
                        },
                        onBilling = { onBilling(booking.id) }
                    )
                }
            }

            if (state.canLoadMore) {
                item(key = "booking-load-more-${state.page}") {
                    LaunchedEffect(state.page, state.items.size) { listViewModel.loadMore() }
                    LoadingState()
                }
            }
            if (!state.loadMoreError.isNullOrBlank()) {
                item(key = "booking-load-more-retry-${state.page}") {
                    InlineRetryMessage(
                        message = state.loadMoreError.orEmpty(),
                        onRetry = listViewModel::retryLoadMore,
                        retryLabel = "Retry loading more"
                    )
                }
            }
            item { Spacer(Modifier.height(AppSpacing.lg)) }
        }
    }

    AppFeedbackHost(errorMessage = contactLaunchError)

    val booking = contactBooking
    val action = contactAction
    if (booking != null && action != null && canContactCustomers) {
        CustomerContactChooserSheet(
            customerName = booking.customerName,
            primary = booking.customerMobile,
            action = action,
            onSelect = { selected ->
                contactBooking = null
                contactAction = null
                if (action == CustomerContactAction.CALL) {
                    if (!bookingOpenDial4(context, selected)) {
                        contactLaunchError = "Unable to open the Phone app."
                    }
                } else {
                    composeWhatsApp(booking.customerId, booking.id, null) { _, message ->
                        if (!bookingOpenWhatsApp4(context, selected, message)) {
                            contactLaunchError = "Unable to open WhatsApp."
                        }
                    }
                }
            },
            onDismiss = {
                contactBooking = null
                contactAction = null
            }
        )
    }
}

@Composable
private fun BookingListCard4(
    booking: LifecycleBookingSummary,
    busy: Boolean,
    canContactCustomers: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onBilling: () -> Unit
) {
    BookingSummaryCard(
        id = booking.id,
        customerName = booking.customerName,
        customerMobile = booking.customerMobile,
        customerAddress = booking.customerAddress,
        bookingNo = booking.bookingNo,
        displayStatus = booking.displayStatus,
        paymentStatus = booking.paymentStatus,
        bookingDate = booking.bookingDate,
        pickupDate = booking.pickupDate,
        returnDate = booking.returnDate,
        items = booking.itemPreviews.map { item ->
            BookingSummaryItemUi(
                imageUrl = item.imageUrl,
                itemName = item.itemName,
                imageUrls = item.imageUrls,
                itemCode = item.itemCode,
                categoryName = item.categoryName,
                quantity = item.quantity,
                statusLine = "Booked ${item.quantity} · Picked ${item.givenQty} · Returned ${item.returnedQty}"
            )
        },
        totalItemCount = booking.itemCount,
        fallbackItemsSummary = booking.itemsSummary
    ) {
        BookingCardActions(
            editable = booking.displayStatus.equals("RESERVED", true),
            busy = busy,
            bookingActionsVisible = true,
            contactActionsVisible = canContactCustomers,
            billVisible = !booking.displayStatus.equals("RESERVED", true) && (!booking.rawStatus.equals("CANCELLED", true) || !booking.billId.isNullOrBlank() || booking.advanceAmount > 0),
            onView = onView,
            onEdit = onEdit,
            onBill = onBilling,
            onCall = onCall,
            onWhatsApp = onWhatsApp
        )
    }
}

@Composable
private fun BookingPreviewRow4(item: BookingListItemPreview) {
    AppItemDisplayRow(
        imageUrl = item.imageUrl,
        imageUrls = item.imageUrls,
        itemName = item.itemName,
        itemCode = item.itemCode,
        categoryName = item.categoryName,
        quantity = item.quantity,
        statusLine = "Booked ${item.quantity} · Picked ${item.givenQty} · Returned ${item.returnedQty}"
    )
}

@Composable
internal fun CompactInfoPill4(
    icon: ImageVector,
    label: String,
    value: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    CompactMetaBadge(
        icon = icon,
        label = label,
        value = value,
        background = background,
        foreground = foreground,
        modifier = modifier
    )
}

@Composable
internal fun DateBlock4(
    label: String,
    date: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    CompactMetaBadge(
        icon = Icons.Rounded.CalendarMonth,
        label = label,
        value = formatDateDay4(date),
        background = background,
        foreground = foreground,
        modifier = modifier
    )
}

private enum class MoreDestination4 { HUB, ITEM_MANAGEMENT, BILLING, REPORTS, USERS, SETTINGS, WHATSAPP_MANAGEMENT }

@Composable
private fun Screen4More(
    user: SessionUser,
    branding: com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding,
    businessDate: String,
    hubRequest: Int,
    reportsViewModel: Screen8ReportsViewModel,
    openBillingBookingId: String? = null,
    onBillingBookingConsumed: () -> Unit = {},
    directDestination: String? = null,
    dashboardDrilldown: Boolean = false,
    dashboardItemItemsTab: Boolean = false,
    dashboardItemCategoryId: String = "",
    dashboardBillingPaymentFilter: String = "",
    dashboardBillingStatusFilter: String = "",
    onDashboardBack: () -> Unit = {},
    onLogout: () -> Unit,
    onBrandingChanged: (String, String) -> Unit,
    onNestedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by rememberSaveable(user.id) { mutableStateOf(MoreDestination4.HUB) }
    val saveableStateHolder = rememberSaveableStateHolder()
    LaunchedEffect(hubRequest, directDestination) {
        if (hubRequest > 0 && directDestination.isNullOrBlank()) {
            destination = MoreDestination4.HUB
        }
    }
    LaunchedEffect(openBillingBookingId) {
        if (!openBillingBookingId.isNullOrBlank()) destination = MoreDestination4.BILLING
    }
    LaunchedEffect(directDestination) {
        val requested = directDestination?.let { value ->
            runCatching { MoreDestination4.valueOf(value) }.getOrNull()
        }
        if (requested != null && requested != MoreDestination4.HUB) {
            destination = requested
        }
    }
    LaunchedEffect(destination) {
        onNestedChanged(destination != MoreDestination4.HUB)
    }
    val nestedBack: () -> Unit = {
        if (dashboardDrilldown) onDashboardBack() else destination = MoreDestination4.HUB
    }

    BackHandler(enabled = destination != MoreDestination4.HUB) {
        nestedBack()
    }

    saveableStateHolder.SaveableStateProvider(destination.name) {
        when (destination) {
            MoreDestination4.ITEM_MANAGEMENT -> Screen5ItemManagement(
                user = user,
                businessDate = businessDate,
                onBack = nestedBack,
                initialItemsTab = dashboardDrilldown && dashboardItemItemsTab,
                initialCategoryId = if (dashboardDrilldown) dashboardItemCategoryId else "",
                modifier = modifier
            )
            MoreDestination4.BILLING -> Screen11Billing(
                currentUser = user,
                branding = branding,
                businessDate = businessDate,
                onBack = nestedBack,
                initialBookingId = openBillingBookingId,
                onInitialBookingConsumed = onBillingBookingConsumed,
                initialPaymentFilter = if (dashboardDrilldown) dashboardBillingPaymentFilter else "",
                initialStatusFilter = if (dashboardDrilldown) dashboardBillingStatusFilter else "",
                modifier = modifier
            )
            MoreDestination4.REPORTS -> Screen8Reports(
                currentUser = user,
                branding = branding,
                businessDate = businessDate,
                viewModel = reportsViewModel,
                onBack = nestedBack,
                modifier = modifier
            )
            MoreDestination4.USERS -> Screen9Users(
                currentUser = user,
                businessDate = businessDate,
                onBack = { destination = MoreDestination4.HUB },
                modifier = modifier
            )
            MoreDestination4.SETTINGS -> Screen10Settings(
                currentUser = user,
                businessDate = businessDate,
                onBack = { destination = MoreDestination4.HUB },
                onBrandingChanged = onBrandingChanged,
                modifier = modifier
            )
            MoreDestination4.WHATSAPP_MANAGEMENT -> Screen10WhatsAppManagement(
                currentUser = user,
                businessDate = businessDate,
                onBack = { destination = MoreDestination4.HUB },
                modifier = modifier
            )
            MoreDestination4.HUB -> Screen4MoreHub(
                user = user,
                businessDate = businessDate,
                onItems = { destination = MoreDestination4.ITEM_MANAGEMENT },
                onBilling = { destination = MoreDestination4.BILLING },
                onReports = { destination = MoreDestination4.REPORTS },
                onUsers = { destination = MoreDestination4.USERS },
                onSettings = { destination = MoreDestination4.SETTINGS },
                onWhatsAppManagement = { destination = MoreDestination4.WHATSAPP_MANAGEMENT },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun Screen4MoreHub(
    user: SessionUser,
    businessDate: String,
    onItems: () -> Unit,
    onBilling: () -> Unit,
    onReports: () -> Unit,
    onUsers: () -> Unit,
    onSettings: () -> Unit,
    onWhatsAppManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canItems = user.hasAccess(StaffAccess.ITEMS)
    val canBilling = user.hasAccess(StaffAccess.BOOKINGS)
    val canReports = user.hasAccess(StaffAccess.REPORTS)
    val canUsers = user.role == UserRole.OWNER
    val canWhatsApp = user.role == UserRole.OWNER
    val canSettings = user.role == UserRole.OWNER

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = AppSpacing.md,
            end = AppSpacing.md,
            top = AppSpacing.xs,
            bottom = AppSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item { MainScreenDateRow(businessDate) }
        item { AppScreenTitle("More") }

        if (canItems || canReports) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (canItems) {
                        MoreHubCard4(
                            title = "Category & Items",
                            subtitle = "Categories, items & related items",
                            icon = Icons.Rounded.Category,
                            tone = ActionTone.BRAND,
                            onClick = onItems,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))

                    if (canReports) {
                        MoreHubCard4(
                            title = "Reports",
                            subtitle = "Generate, save & export PDF",
                            icon = Icons.Rounded.Assessment,
                            tone = ActionTone.PURPLE,
                            onClick = onReports,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))
                }
            }
        }

        if (canBilling || canWhatsApp) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (canBilling) {
                        MoreHubCard4(
                            title = "Bills",
                            subtitle = "Create bills, collect cash & PDF",
                            icon = Icons.Rounded.ReceiptLong,
                            tone = ActionTone.INFO,
                            onClick = onBilling,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))

                    if (canWhatsApp) {
                        MoreHubCard4(
                            title = "WhatsApp Centre",
                            subtitle = "Templates, actions & language",
                            icon = Icons.Rounded.Chat,
                            tone = ActionTone.SUCCESS,
                            onClick = onWhatsAppManagement,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))
                }
            }
        }

        if (canUsers || canSettings) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (canUsers) {
                        MoreHubCard4(
                            title = "Users",
                            subtitle = "Users & Staff access",
                            icon = Icons.Rounded.ManageAccounts,
                            tone = ActionTone.INFO,
                            onClick = onUsers,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))

                    if (canSettings) {
                        MoreHubCard4(
                            title = "Settings",
                            subtitle = "Business, website & audit",
                            icon = Icons.Rounded.Settings,
                            tone = ActionTone.WARNING,
                            onClick = onSettings,
                            modifier = Modifier.weight(1f)
                        )
                    } else Spacer(Modifier.weight(1f))
                }
            }
        }

        if (!canItems && !canBilling && !canReports && !canUsers && !canWhatsApp && !canSettings) {
            item { EmptyState("No More modules are enabled for this Staff account.") }
        }
    }
}

@Composable
private fun MoreHubCard4(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: ActionTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pair = when (tone) {
        ActionTone.BRAND -> BrandSoft to MaterialTheme.colorScheme.primary
        ActionTone.INFO -> StatusInfoSoft to StatusInfo
        ActionTone.WARNING -> StatusWarningSoft to StatusWarning
        ActionTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = pair.first,
                    contentColor = pair.second,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppTextMuted)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted,
                minLines = 2,
                maxLines = 2
            )
        }
    }
}

internal fun lifecycleLabel4(status: String): String = when (status.uppercase()) {
    "RESERVED" -> "Reserved"
    "BOOKED" -> "Booked"
    "PART_PICKUP" -> "Part Picked Up"
    "FULL_PICKUP" -> "Full Picked Up"
    "PART_RETURN" -> "Part Return"
    "FULL_RETURN" -> "Full Returned"
    "CANCELLED" -> "Cancelled"
    else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

internal fun lifecycleTone4(status: String): BadgeTone = when (status.uppercase()) {
    "FULL_RETURN" -> BadgeTone.SUCCESS
    "CANCELLED" -> BadgeTone.ERROR
    "PART_PICKUP", "FULL_PICKUP", "PART_RETURN" -> BadgeTone.WARNING
    "RESERVED", "BOOKED" -> BadgeTone.INFO
    else -> BadgeTone.NEUTRAL
}

private fun lifecycleUpcoming4(status: String): String? = when (status.uppercase()) {
    "RESERVED" -> "Confirmation Pending"
    "BOOKED" -> "Pickup Pending"
    "PART_PICKUP" -> "Pickup / Return Pending"
    "FULL_PICKUP" -> "Return Pending"
    "PART_RETURN" -> "Return Pending"
    else -> null
}

internal fun formatDateDay4(raw: String): String {
    val date = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() ?: return raw
    return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy · EEEE", Locale.ENGLISH))
}

internal fun bookingOpenDial4(context: Context, number: String): Boolean =
    launchCustomerCall(context, number)

internal fun bookingOpenWhatsApp4(context: Context, number: String, message: String): Boolean =
    launchCustomerWhatsApp(context, number, message)
