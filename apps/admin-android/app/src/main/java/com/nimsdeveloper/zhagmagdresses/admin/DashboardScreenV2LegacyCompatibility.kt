package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardPayload

/**
 * Compatibility overload for older Admin shells.
 *
 * The KPI Dashboard no longer owns booking-card actions. Legacy callers can still render the
 * current KPI surface; destination-specific drilldowns stay disabled in this compatibility path.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DashboardScreenV2(
    state: DataState<DashboardPayload>,
    revision: Int,
    uiState: DashboardScreenState,
    shopName: String,
    onRefresh: () -> Unit,
    onNewBooking: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val access = state.data?.access
    DashboardScreenV2(
        state = state,
        revision = revision,
        uiState = uiState,
        onRefresh = onRefresh,
        onNewBooking = onNewBooking,
        onKpiClick = { _, _ -> },
        canBookings = access?.bookings == true,
        canItems = access?.items == true,
        canCustomers = access?.customers == true,
        canReports = access?.reports == true,
        canBillingReports = false,
        modifier = modifier
    )
}
