package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.AdminRepository
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BookingListFilter4(
    val apiValue: String,
    val label: String,
    val dashboardOnly: Boolean = false
) {
    ALL("ALL", "All"),
    RESERVED("RESERVED", "Reserved"),
    BOOKED("BOOKED", "Booked"),
    PART_PICKUP("PART_PICKUP", "Part Picked Up"),
    FULL_PICKUP("FULL_PICKUP", "Full Picked Up"),
    PART_RETURN("PART_RETURN", "Part Return"),
    FULL_RETURN("FULL_RETURN", "Full Returned"),
    OVERDUE("OVERDUE", "Overdue"),
    CANCELLED("CANCELLED", "Cancelled"),
    PAYMENT_PENDING("PAYMENT_PENDING", "Pending Payment"),
    PAYMENT_PART("PAYMENT_PART", "Part Payment"),
    PAYMENT_FULL("PAYMENT_FULL", "Full Payment"),

    TODAY_PICKUP("TODAY_PICKUP", "Today Pickups", dashboardOnly = true),
    TODAY_RETURN("TODAY_RETURN", "Today Returns", dashboardOnly = true),
    MISSED_PICKUP("MISSED_PICKUP", "Missed Pickups", dashboardOnly = true),
    ACTIVE_RENTAL("ACTIVE_RENTAL", "Active Rental Orders", dashboardOnly = true)
}

data class BookingListState4(
    val items: List<LifecycleBookingSummary> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val search: String = "",
    val filter: BookingListFilter4 = BookingListFilter4.ALL,
    val sort: String = "NEWEST",
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val loadMoreError: String? = null
) {
    val canLoadMore: Boolean get() = loaded && !loading && error == null && loadMoreError == null && page < totalPages
}

private data class PendingBookingListRequest4(
    val page: Int,
    val reset: Boolean,
    val search: String,
    val filter: BookingListFilter4,
    val sort: String
)

class BookingListViewModel4(
    private val repository: AdminRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var state by mutableStateOf(
        BookingListState4(
            page = savedStateHandle["bookingListPage"] ?: 1,
            search = savedStateHandle["bookingListSearch"] ?: "",
            filter = runCatching { BookingListFilter4.valueOf(savedStateHandle["bookingListFilter"] ?: "ALL") }.getOrDefault(BookingListFilter4.ALL),
            sort = savedStateHandle["bookingListSort"] ?: "NEWEST"
        )
    )
        private set

    private var searchJob: Job? = null
    private var pendingLoad: PendingBookingListRequest4? = null
    private var requestSerial = 0L
    private var seenFreshnessRevision = AdminDataFreshness.bookingsRevision

    fun ensureLoaded() {
        val stale = seenFreshnessRevision != AdminDataFreshness.bookingsRevision
        if (!state.loaded || stale) requestLoad(page = state.page.coerceAtLeast(1), reset = true)
    }

    fun search(value: String) {
        val clean = value.take(120)
        state = state.copy(search = clean, error = null)
        savedStateHandle["bookingListSearch"] = clean
        savedStateHandle["bookingListPage"] = 1
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            requestLoad(page = 1, reset = true)
        }
    }

    fun setFilter(filter: BookingListFilter4) {
        if (state.filter == filter && state.loaded) return
        searchJob?.cancel()
        state = state.copy(filter = filter, page = 1, items = emptyList(), loaded = false, error = null)
        savedStateHandle["bookingListFilter"] = filter.name
        savedStateHandle["bookingListPage"] = 1
        requestLoad(page = 1, reset = true)
    }

    fun setDashboardFilter(filter: BookingListFilter4) {
        require(filter.dashboardOnly || filter != BookingListFilter4.ALL) { "Dashboard filter is required." }
        searchJob?.cancel()
        state = state.copy(
            search = "",
            filter = filter,
            sort = "NEWEST",
            page = 1,
            items = emptyList(),
            loaded = false,
            error = null
        )
        savedStateHandle["bookingListSearch"] = ""
        savedStateHandle["bookingListFilter"] = filter.name
        savedStateHandle["bookingListSort"] = "NEWEST"
        savedStateHandle["bookingListPage"] = 1
        requestLoad(page = 1, reset = true)
    }

    fun clearDashboardFilter() {
        searchJob?.cancel()
        pendingLoad = null
        requestSerial += 1
        state = state.copy(
            items = emptyList(),
            page = 1,
            totalPages = 1,
            total = 0,
            search = "",
            filter = BookingListFilter4.ALL,
            sort = "NEWEST",
            loading = false,
            loaded = false,
            error = null
        )
        savedStateHandle["bookingListSearch"] = ""
        savedStateHandle["bookingListFilter"] = BookingListFilter4.ALL.name
        savedStateHandle["bookingListSort"] = "NEWEST"
        savedStateHandle["bookingListPage"] = 1
    }

    fun setSort(sort: String) {
        val normalized = sort.uppercase()
        if (normalized !in setOf("NEWEST", "OLDEST", "PICKUP_ASC", "PICKUP_DESC", "RETURN_ASC", "RETURN_DESC")) return
        if (state.sort == normalized && state.loaded) return
        searchJob?.cancel()
        state = state.copy(sort = normalized, page = 1, items = emptyList(), loaded = false, error = null)
        savedStateHandle["bookingListSort"] = normalized
        savedStateHandle["bookingListPage"] = 1
        requestLoad(page = 1, reset = true)
    }

    fun refresh() {
        searchJob?.cancel()
        requestLoad(page = 1, reset = true)
    }

    fun loadMore() {
        if (!state.canLoadMore) return
        requestLoad(page = state.page + 1, reset = false)
    }

    fun retryLoadMore() {
        if (state.loading || state.loadMoreError.isNullOrBlank() || state.page >= state.totalPages) return
        state = state.copy(loadMoreError = null)
        requestLoad(page = state.page + 1, reset = false)
    }

    private fun defensiveItems(
        items: List<LifecycleBookingSummary>,
        filter: BookingListFilter4
    ): List<LifecycleBookingSummary> = when (filter) {
        BookingListFilter4.CANCELLED -> items.filter {
            it.rawStatus.equals("CANCELLED", true) || it.displayStatus.equals("CANCELLED", true)
        }
        else -> items
    }

    private fun requestLoad(page: Int, reset: Boolean) {
        val request = PendingBookingListRequest4(
            page = page.coerceAtLeast(1),
            reset = reset,
            search = state.search,
            filter = state.filter,
            sort = state.sort
        )
        if (state.loading) {
            pendingLoad = request
            return
        }
        load(request)
    }

    private fun load(request: PendingBookingListRequest4) {
        if (state.loading) {
            pendingLoad = request
            return
        }
        val serial = ++requestSerial
        val hadLoadedContent = state.loaded
        state = state.copy(
            loading = true,
            error = if (request.reset) null else state.error,
            loadMoreError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                repository.lifecycleBookings(
                    page = request.page,
                    pageSize = 10,
                    search = request.search,
                    status = "",
                    view = request.filter.apiValue,
                    sort = request.sort
                )
            }
            val stillCurrent =
                state.search == request.search &&
                    state.filter == request.filter &&
                    state.sort == request.sort

            result.onSuccess { page ->
                if (serial == requestSerial && stillCurrent) {
                    val safeItems = defensiveItems(page.items, request.filter)
                    val merged = if (request.reset || request.page == 1) safeItems else {
                        val seen = state.items.asSequence().map { it.id }.toMutableSet()
                        state.items + safeItems.filter { seen.add(it.id) }
                    }
                    seenFreshnessRevision = AdminDataFreshness.bookingsRevision
                    savedStateHandle["bookingListPage"] = page.page
                    state = state.copy(
                        items = merged,
                        page = page.page,
                        totalPages = page.totalPages,
                        total = if (request.filter == BookingListFilter4.CANCELLED && safeItems.size != page.items.size) merged.size else page.total,
                        loading = false,
                        loaded = true,
                        error = null,
                        loadMoreError = null
                    )
                } else if (serial == requestSerial) {
                    state = state.copy(loading = false)
                }
            }
            result.onFailure { error ->
                if (serial == requestSerial) {
                    state = if (stillCurrent) {
                        val message = userMessage(error)
                        if (!request.reset && hadLoadedContent) {
                            state.copy(loading = false, loaded = true, loadMoreError = message)
                        } else {
                            state.copy(loading = false, loaded = hadLoadedContent, error = message, loadMoreError = null)
                        }
                    } else {
                        state.copy(loading = false)
                    }
                }
            }

            val next = pendingLoad
            pendingLoad = null
            if (next != null && next != request) load(next)
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message.ifBlank { "Unable to load bookings." }
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Unable to load bookings. Please try again."
    }

    class Factory(private val repository: AdminRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(BookingListViewModel4::class.java)) {
                return BookingListViewModel4(repository, extras.createSavedStateHandle()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
