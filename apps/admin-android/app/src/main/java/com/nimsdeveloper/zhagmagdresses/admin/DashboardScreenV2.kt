package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardCategoryInventory
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardPayload
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppScreenTitle
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardCategories
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardCategoriesSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGiven
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGivenSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItems
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItemsSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardTotal
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardTotalSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusErrorSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import java.text.NumberFormat
import java.util.Locale

class DashboardScreenState internal constructor(
    val listState: LazyListState,
    // Kept only for process-state compatibility with the pre-KPI Dashboard. No expandable
    // Dashboard section consumes this state after Issue 6.
    private val expandedKeysState: MutableState<ArrayList<String>>,
    private val appliedRevisionState: MutableIntState
) {
    var appliedRevision: Int
        get() = appliedRevisionState.intValue
        set(value) { appliedRevisionState.intValue = value }
}

@Composable
fun rememberDashboardScreenState(): DashboardScreenState {
    val listState = rememberLazyListState()
    val expandedKeys = rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val appliedRevision = rememberSaveable { mutableIntStateOf(-1) }
    return remember(listState, expandedKeys, appliedRevision) {
        DashboardScreenState(listState, expandedKeys, appliedRevision)
    }
}

internal enum class DashboardKpiAction {
    TODAY_PICKUPS,
    TODAY_RETURNS,
    MISSED_PICKUPS,
    OVERDUE_RETURNS,

    RESERVED,
    BOOKED,
    PART_PICKED_UP,
    FULL_PICKED_UP,
    PART_RETURN,
    FULL_RETURNED,
    CANCELLED,
    ACTIVE_RENTAL_ORDERS,

    PENDING_PAYMENT,
    PART_PAYMENT,
    FULL_PAYMENT,
    DRAFT_BILLS,
    FINAL_BILLS,
    BILLS_TODAY,
    PENDING_BALANCE,
    TOTAL_RECEIVED,

    AVAILABLE_NOW,
    PICKUP_PENDING_QTY,
    CURRENTLY_OUT_QTY,
    TOTAL_QUANTITY,
    TOTAL_ITEMS,
    CATEGORIES,
    LOW_STOCK,
    UNAVAILABLE,

    TOTAL_CUSTOMERS,
    NEW_CUSTOMERS,
    RETURNING_CUSTOMERS,
    FREQUENT_CUSTOMERS,
    ACTIVE_RENTAL_CUSTOMERS,
    CUSTOMER_EXCEPTIONS,

    CATEGORY_INVENTORY
}

private data class DashboardKpiSpec(
    val label: String,
    val value: Int,
    val icon: ImageVector,
    val background: Color,
    val foreground: Color,
    val action: DashboardKpiAction,
    val money: Boolean = false
)

@Composable
internal fun DashboardScreenV2(
    state: DataState<DashboardPayload>,
    revision: Int,
    uiState: DashboardScreenState,
    onRefresh: () -> Unit,
    onNewBooking: () -> Unit,
    onKpiClick: (DashboardKpiAction, String?) -> Unit,
    canBookings: Boolean,
    canItems: Boolean,
    canCustomers: Boolean,
    canReports: Boolean,
    canBillingReports: Boolean,
    modifier: Modifier = Modifier
) {
    val dashboard = state.data
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(revision) {
        if (revision != uiState.appliedRevision) {
            uiState.appliedRevision = revision
        }
    }

    LazyColumn(
        state = uiState.listState,
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = AppSpacing.xs,
            bottom = AppSpacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item { MainScreenDateRow(dashboard?.today.orEmpty()) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppScreenTitle("Dashboard", modifier = Modifier.weight(1f))
                if (canBookings) {
                    CompactNewBookingButton(
                        enabled = true,
                        onClick = onNewBooking
                    )
                }
            }
        }

        if (state.loading && dashboard == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.lg),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!state.error.isNullOrBlank()) {
            item {
                if (dashboard == null) {
                    LoadFailureState(message = state.error.orEmpty(), onRetry = onRefresh)
                } else {
                    InlineStatusMessage(state.error, MessageTone.ERROR)
                }
            }
        }

        if (dashboard != null) {
            val kpis = dashboard.kpis

            if (canBookings && dashboard.access.bookings) {
                item {
                    DashboardKpiSection(
                        title = "Today Overview",
                        specs = listOf(
                            DashboardKpiSpec("Today Pickups", kpis.todayOverview.todayPickups, Icons.Rounded.LocalShipping, StatusInfoSoft, StatusInfo, DashboardKpiAction.TODAY_PICKUPS),
                            DashboardKpiSpec("Today Returns", kpis.todayOverview.todayReturns, Icons.Rounded.Reply, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.TODAY_RETURNS),
                            DashboardKpiSpec("Missed Pickups", kpis.todayOverview.missedPickups, Icons.Rounded.Schedule, StatusWarningSoft, StatusWarning, DashboardKpiAction.MISSED_PICKUPS),
                            DashboardKpiSpec("Overdue Returns", kpis.todayOverview.overdueReturns, Icons.Rounded.Warning, StatusErrorSoft, StatusError, DashboardKpiAction.OVERDUE_RETURNS)
                        ),
                        onClick = onKpiClick
                    )
                }

                item {
                    DashboardKpiSection(
                        title = "Booking Status",
                        specs = listOf(
                            DashboardKpiSpec("Reserved", kpis.bookingStatus.reserved, Icons.Rounded.EventNote, StatusInfoSoft, StatusInfo, DashboardKpiAction.RESERVED),
                            DashboardKpiSpec("Booked", kpis.bookingStatus.booked, Icons.Rounded.EventNote, BrandSoft, primaryColor, DashboardKpiAction.BOOKED),
                            DashboardKpiSpec("Part Picked Up", kpis.bookingStatus.partPickedUp, Icons.Rounded.LocalShipping, StatusWarningSoft, StatusWarning, DashboardKpiAction.PART_PICKED_UP),
                            DashboardKpiSpec("Full Picked Up", kpis.bookingStatus.fullPickedUp, Icons.Rounded.LocalShipping, DashboardGivenSoft, DashboardGiven, DashboardKpiAction.FULL_PICKED_UP),
                            DashboardKpiSpec("Part Return", kpis.bookingStatus.partReturn, Icons.Rounded.Reply, StatusWarningSoft, StatusWarning, DashboardKpiAction.PART_RETURN),
                            DashboardKpiSpec("Full Returned", kpis.bookingStatus.fullReturned, Icons.Rounded.CheckCircle, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.FULL_RETURNED),
                            DashboardKpiSpec("Cancelled", kpis.bookingStatus.cancelled, Icons.Rounded.Warning, StatusErrorSoft, StatusError, DashboardKpiAction.CANCELLED),
                            DashboardKpiSpec("Active Rental Orders", kpis.bookingStatus.activeRentalOrders, Icons.Rounded.EventNote, DashboardTotalSoft, DashboardTotal, DashboardKpiAction.ACTIVE_RENTAL_ORDERS)
                        ),
                        onClick = onKpiClick
                    )
                }

                if (dashboard.access.billing) {
                    val paymentSpecs = buildList {
                        add(DashboardKpiSpec("Pending Payment", kpis.paymentBilling.pendingPayment, Icons.Rounded.ReceiptLong, StatusWarningSoft, StatusWarning, DashboardKpiAction.PENDING_PAYMENT))
                        add(DashboardKpiSpec("Part Payment", kpis.paymentBilling.partPayment, Icons.Rounded.ReceiptLong, StatusInfoSoft, StatusInfo, DashboardKpiAction.PART_PAYMENT))
                        add(DashboardKpiSpec("Full Payment", kpis.paymentBilling.fullPayment, Icons.Rounded.CheckCircle, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.FULL_PAYMENT))
                        add(DashboardKpiSpec("Draft Bills", kpis.paymentBilling.draftBills, Icons.Rounded.ReceiptLong, BrandSoft, primaryColor, DashboardKpiAction.DRAFT_BILLS))
                        add(DashboardKpiSpec("Final Bills", kpis.paymentBilling.finalBills, Icons.Rounded.ReceiptLong, DashboardTotalSoft, DashboardTotal, DashboardKpiAction.FINAL_BILLS))
                        if (canBillingReports && dashboard.access.reports) {
                            add(DashboardKpiSpec("Bills Today", kpis.paymentBilling.billsToday, Icons.Rounded.EventNote, DashboardItemsSoft, DashboardItems, DashboardKpiAction.BILLS_TODAY))
                            add(DashboardKpiSpec("Pending Balance", kpis.paymentBilling.pendingBalance, Icons.Rounded.Warning, StatusErrorSoft, StatusError, DashboardKpiAction.PENDING_BALANCE, money = true))
                            add(DashboardKpiSpec("Total Received", kpis.paymentBilling.totalReceived, Icons.Rounded.CheckCircle, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.TOTAL_RECEIVED, money = true))
                        }
                    }
                    item {
                        DashboardKpiSection(
                            title = "Payment & Billing",
                            specs = paymentSpecs,
                            onClick = onKpiClick
                        )
                    }
                }
            }

            if (canItems && dashboard.access.items) {
                val inventorySpecs = buildList {
                    if (canReports && dashboard.access.reports) {
                        add(DashboardKpiSpec("Available Now", kpis.inventory.availableNow, Icons.Rounded.CheckCircle, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.AVAILABLE_NOW))
                        add(DashboardKpiSpec("Pickup Pending Qty", kpis.inventory.pickupPendingQty, Icons.Rounded.Schedule, StatusWarningSoft, StatusWarning, DashboardKpiAction.PICKUP_PENDING_QTY))
                        add(DashboardKpiSpec("Currently Out Qty", kpis.inventory.currentlyOutQty, Icons.Rounded.LocalShipping, DashboardGivenSoft, DashboardGiven, DashboardKpiAction.CURRENTLY_OUT_QTY))
                    }
                    add(DashboardKpiSpec("Total Quantity", kpis.inventory.totalQuantity, Icons.Rounded.Numbers, DashboardTotalSoft, DashboardTotal, DashboardKpiAction.TOTAL_QUANTITY))
                    add(DashboardKpiSpec("Total Items", kpis.inventory.totalItems, Icons.Rounded.Inventory2, DashboardItemsSoft, DashboardItems, DashboardKpiAction.TOTAL_ITEMS))
                    add(DashboardKpiSpec("Categories", kpis.inventory.categories, Icons.Rounded.Category, DashboardCategoriesSoft, DashboardCategories, DashboardKpiAction.CATEGORIES))
                    if (canReports && dashboard.access.reports) {
                        add(DashboardKpiSpec("Low Stock", kpis.inventory.lowStock, Icons.Rounded.Warning, StatusWarningSoft, StatusWarning, DashboardKpiAction.LOW_STOCK))
                        add(DashboardKpiSpec("Unavailable", kpis.inventory.unavailable, Icons.Rounded.Warning, StatusErrorSoft, StatusError, DashboardKpiAction.UNAVAILABLE))
                    }
                }
                item {
                    DashboardKpiSection(
                        title = "Inventory",
                        specs = inventorySpecs,
                        onClick = onKpiClick
                    )
                }
            }

            if (canCustomers && dashboard.access.customers) {
                val customerSpecs = buildList {
                    add(DashboardKpiSpec("Total Customers", kpis.customers.totalCustomers, Icons.Rounded.People, BrandSoft, primaryColor, DashboardKpiAction.TOTAL_CUSTOMERS))
                    if (canReports && dashboard.access.reports) {
                        add(DashboardKpiSpec("New Customers", kpis.customers.newCustomers, Icons.Rounded.People, StatusSuccessSoft, StatusSuccess, DashboardKpiAction.NEW_CUSTOMERS))
                        add(DashboardKpiSpec("Returning Customers", kpis.customers.returningCustomers, Icons.Rounded.People, StatusInfoSoft, StatusInfo, DashboardKpiAction.RETURNING_CUSTOMERS))
                        add(DashboardKpiSpec("Frequent Customers", kpis.customers.frequentCustomers, Icons.Rounded.People, DashboardTotalSoft, DashboardTotal, DashboardKpiAction.FREQUENT_CUSTOMERS))
                        add(DashboardKpiSpec("Active Rental Customers", kpis.customers.activeRentalCustomers, Icons.Rounded.LocalShipping, DashboardGivenSoft, DashboardGiven, DashboardKpiAction.ACTIVE_RENTAL_CUSTOMERS))
                        add(DashboardKpiSpec("Customer Exceptions", kpis.customers.customerExceptions, Icons.Rounded.Warning, StatusErrorSoft, StatusError, DashboardKpiAction.CUSTOMER_EXCEPTIONS))
                    }
                }
                item {
                    DashboardKpiSection(
                        title = "Customers",
                        specs = customerSpecs,
                        onClick = onKpiClick
                    )
                }
            }

            if (canItems && dashboard.access.items) {
                item {
                    DashboardCategorySection(
                        categories = dashboard.categoryInventory,
                        onClick = { categoryId ->
                            onKpiClick(DashboardKpiAction.CATEGORY_INVENTORY, categoryId)
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(AppSpacing.md)) }
    }
}

@Composable
private fun DashboardKpiSection(
    title: String,
    specs: List<DashboardKpiSpec>,
    onClick: (DashboardKpiAction, String?) -> Unit
) {
    if (specs.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        DashboardGrid(specs) { spec ->
            DashboardKpiCard(
                label = spec.label,
                value = if (spec.money) formatDashboardMoney(spec.value) else spec.value.toString(),
                icon = spec.icon,
                background = spec.background,
                foreground = spec.foreground,
                onClick = { onClick(spec.action, null) }
            )
        }
    }
}

@Composable
private fun DashboardGrid(
    specs: List<DashboardKpiSpec>,
    content: @Composable (DashboardKpiSpec) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 720.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            specs.chunked(columns).forEach { rowSpecs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    rowSpecs.forEach { spec ->
                        Column(Modifier.weight(1f)) { content(spec) }
                    }
                    repeat(columns - rowSpecs.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = background,
            contentColor = foreground
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = foreground
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = foreground
            )
        }
    }
}

@Composable
private fun DashboardCategorySection(
    categories: List<DashboardCategoryInventory>,
    onClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = "Category-wise Inventory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (categories.isEmpty()) {
            Text(
                "No active categories.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 720.dp) 4 else 2
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    categories.chunked(columns).forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            rowCategories.forEach { category ->
                                Column(Modifier.weight(1f)) {
                                    DashboardCategoryCard(category, onClick)
                                }
                            }
                            repeat(columns - rowCategories.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCategoryCard(
    category: DashboardCategoryInventory,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(category.categoryId) },
        colors = CardDefaults.cardColors(
            containerColor = DashboardCategoriesSoft,
            contentColor = DashboardCategories
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DashboardCategoryMetric("Items", category.items)
            DashboardCategoryMetric("Total Qty", category.totalQty)
            DashboardCategoryMetric("Available", category.available)
            DashboardCategoryMetric("Pickup Pending", category.pickupPending)
            DashboardCategoryMetric("Currently Out", category.currentlyOut)
        }
    }
}

@Composable
private fun DashboardCategoryMetric(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
        Text(value.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDashboardMoney(value: Int): String =
    "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(value)
