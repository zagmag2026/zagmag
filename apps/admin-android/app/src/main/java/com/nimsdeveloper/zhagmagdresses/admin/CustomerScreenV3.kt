package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole

/**
 * Legacy Screen 3 compatibility entry. Customer UI has one canonical implementation in V4 so
 * pull-to-refresh, primary-mobile-only behavior, popup messages and information-row styling cannot
 * drift between routes.
 */
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
    val listState = rememberLazyListState()
    CustomerScreenV4(
        state = state,
        role = role,
        businessDate = businessDate,
        onSearch = onSearch,
        onSort = onSort,
        onArchivedTab = onArchivedTab,
        onLoadMore = { if (state.canLoadMore) onPage(state.page + 1) },
        onRetryLoadMore = { if (state.canLoadMore) onPage(state.page + 1) },
        onRefresh = onRefresh,
        onSave = onSave,
        onArchive = onArchive,
        onRestore = onRestore,
        onDeletePermanent = onDeletePermanent,
        onCreateBooking = onCreateBooking,
        onWhatsApp = { customerId, bookingId, _, onReady ->
            onWhatsApp(customerId, bookingId, onReady)
        },
        listState = listState,
        modifier = modifier
    )
}
