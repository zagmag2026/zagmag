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
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerMutationResult
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerScreen3Repository
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.WhatsAppComposeResult
import com.nimsdeveloper.zhagmagdresses.admin.data.WhatsAppTemplateBundle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CustomerScreenState(
    val items: List<CustomerCardRow> = emptyList(),
    val summary: CustomerSummary = CustomerSummary(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val search: String = "",
    val sort: String = "NAME_ASC",
    val archived: Boolean = false,
    val loading: Boolean = false,
    val loadError: String? = null,
    val loadMoreError: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val loaded: Boolean = false,
    val highlightId: String? = null,
    val actionBusy: Boolean = false
) {
    val canLoadMore: Boolean get() = loaded && !loading && loadError == null && loadMoreError == null && page < totalPages
}

private data class PendingCustomerLoad(
    val page: Int,
    val pinCustomerId: String?,
    val keepNotice: Boolean,
    val append: Boolean
)

class CustomerScreen3ViewModel(
    private val repository: CustomerScreen3Repository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var state by mutableStateOf(
        CustomerScreenState(
            page = savedStateHandle["customerPage"] ?: 1,
            search = savedStateHandle["customerSearch"] ?: "",
            sort = savedStateHandle["customerSort"] ?: "NAME_ASC",
            archived = savedStateHandle["customerArchived"] ?: false
        )
    )
        private set

    private var queuedLoadJob: Job? = null
    private var feedbackClearJob: Job? = null
    private var pendingLoad: PendingCustomerLoad? = null
    private var seenFreshnessRevision = AdminDataFreshness.customersRevision

    fun ensureLoaded() {
        if (state.notice != null || state.error != null) state = state.copy(notice = null, error = null)
        val stale = seenFreshnessRevision != AdminDataFreshness.customersRevision
        if ((!state.loaded || stale) && !state.loading) load(state.page.coerceAtLeast(1))
    }

    fun search(value: String) {
        val nextSearch = value.take(120)
        savedStateHandle["customerSearch"] = nextSearch
        savedStateHandle["customerPage"] = 1
        state = state.copy(search = nextSearch, page = 1, loadError = null, error = null, notice = null, highlightId = null)
        scheduleSearchLoad()
    }

    fun sort(value: String) {
        if (value !in setOf("NAME_ASC", "NAME_DESC", "NEWEST", "OLDEST")) return
        savedStateHandle["customerSort"] = value
        savedStateHandle["customerPage"] = 1
        state = state.copy(sort = value, page = 1, items = emptyList(), loaded = false, loadError = null, error = null, notice = null, highlightId = null)
        scheduleLoad(1)
    }

    fun setArchived(archived: Boolean) {
        savedStateHandle["customerArchived"] = archived
        savedStateHandle["customerPage"] = 1
        state = state.copy(archived = archived, page = 1, items = emptyList(), loaded = false, loadError = null, error = null, notice = null, highlightId = null)
        scheduleLoad(1)
    }

    fun page(page: Int) {
        val safe = page.coerceAtLeast(1)
        savedStateHandle["customerPage"] = safe
        scheduleLoad(safe, append = false)
    }

    fun loadMore() {
        if (!state.canLoadMore) return
        scheduleLoad(state.page + 1, append = true)
    }

    fun retryLoadMore() {
        if (state.loading || state.loadMoreError.isNullOrBlank() || state.page >= state.totalPages) return
        state = state.copy(loadMoreError = null)
        scheduleLoad(state.page + 1, append = true)
    }

    fun refresh() {
        if (state.loading) return
        pendingLoad = null
        queuedLoadJob?.cancel()
        requestLoad(1, append = false)
    }

    fun save(
        id: String?,
        name: String,
        mobile: String,
        alternateMobile: String,
        address: String,
        onSuccess: () -> Unit = {}
    ) {
        if (state.actionBusy) return
        val primary = mobile.filter(Char::isDigit)
        val alternate = if (id == null) {
            alternateMobile.filter(Char::isDigit)
        } else {
            // Alternative Mobile is retired from the Android UI. Preserve any
            // legacy value that may have been created on an older/Web surface.
            state.items.firstOrNull { it.id == id }?.alternateMobile.orEmpty().filter(Char::isDigit)
        }
        when {
            name.trim().isBlank() -> state = state.copy(error = "Name is required.", notice = null)
            primary.length != 10 -> state = state.copy(error = "Enter a valid 10-digit mobile number.", notice = null)
            alternate.isNotBlank() && alternate.length != 10 -> state = state.copy(error = "Enter a valid 10-digit alternative mobile number.", notice = null)
            alternate.isNotBlank() && alternate == primary -> state = state.copy(error = "Alternative mobile must be different from primary mobile.", notice = null)
            else -> runMutation(
                request = {
                    if (id == null) repository.create(name, primary, alternate, address)
                    else repository.update(
                        id,
                        name,
                        primary,
                        alternate,
                        address,
                        state.items.firstOrNull { it.id == id }?.updatedAt
                    )
                },
                onSuccess = { result ->
                    onSuccess()
                    val highlight = result.id ?: id
                    AdminDataFreshness.markCustomerMutation()
                    val successMessage = if (id == null) "Customer added successfully." else "Customer updated successfully."
                    state = state.copy(
                        actionBusy = false,
                        archived = false,
                        page = 1,
                        search = "",
                        items = emptyList(),
                        loaded = false,
                        notice = successMessage,
                        error = null,
                        highlightId = highlight
                    )
                    scheduleLoad(1, pinCustomerId = highlight, keepNotice = true)
                    scheduleNoticeClear(successMessage)
                }
            )
        }
    }

    fun archive(id: String) = mutateAndReset { repository.archive(id) }
    fun restore(id: String) = mutateAndReset { repository.restore(id) }

    fun deletePermanently(id: String) {
        if (state.actionBusy) return
        feedbackClearJob?.cancel()
        queuedLoadJob?.cancel()
        pendingLoad = null
        val snapshot = state
        state = state.copy(actionBusy = true, error = null, notice = null, loadError = null)

        viewModelScope.launch {
            val deletion = runCatching { repository.deletePermanently(id) }
            val mutation = deletion.getOrElse { error ->
                val message = userMessage(error)
                state = state.copy(actionBusy = false, error = message)
                scheduleErrorClear(message)
                return@launch
            }

            AdminDataFreshness.markCustomerMutation()

            val archivedPage = runCatching {
                repository.customers(
                    page = 1,
                    search = snapshot.search,
                    sort = snapshot.sort,
                    archived = true
                )
            }

            archivedPage.onFailure { error ->
                val message = userMessage(error)
                state = state.copy(
                    actionBusy = false,
                    archived = true,
                    items = snapshot.items.filterNot { it.id == id },
                    notice = mutation.message,
                    loadError = message,
                    highlightId = null
                )
                scheduleNoticeClear(mutation.message)
            }

            archivedPage.onSuccess { archivedResponse ->
                if (archivedResponse.summary.archivedCustomers > 0) {
                    seenFreshnessRevision = AdminDataFreshness.customersRevision
                    savedStateHandle["customerPage"] = archivedResponse.page
                    state = state.copy(
                        actionBusy = false,
                        archived = true,
                        items = archivedResponse.items,
                        summary = archivedResponse.summary,
                        page = archivedResponse.page,
                        totalPages = archivedResponse.totalPages,
                        total = archivedResponse.total,
                        search = snapshot.search,
                        sort = snapshot.sort,
                        loaded = true,
                        loadError = null,
                        error = null,
                        notice = mutation.message,
                        highlightId = null
                    )
                    scheduleNoticeClear(mutation.message)
                } else {
                    val activePage = runCatching {
                        repository.customers(
                            page = 1,
                            search = "",
                            sort = snapshot.sort,
                            archived = false
                        )
                    }

                    activePage.onSuccess { activeResponse ->
                        seenFreshnessRevision = AdminDataFreshness.customersRevision
                        state = state.copy(
                            actionBusy = false,
                            archived = false,
                            items = activeResponse.items,
                            summary = activeResponse.summary,
                            page = activeResponse.page,
                            totalPages = activeResponse.totalPages,
                            total = activeResponse.total,
                            search = "",
                            sort = snapshot.sort,
                            loaded = true,
                            loadError = null,
                            error = null,
                            notice = mutation.message,
                            highlightId = null
                        )
                        scheduleNoticeClear(mutation.message)
                    }.onFailure { error ->
                        state = state.copy(
                            actionBusy = false,
                            archived = false,
                            items = emptyList(),
                            page = 1,
                            totalPages = 1,
                            total = 0,
                            search = "",
                            sort = snapshot.sort,
                            loaded = false,
                            loadError = userMessage(error),
                            error = null,
                            notice = mutation.message,
                            highlightId = null
                        )
                        scheduleNoticeClear(mutation.message)
                    }
                }
            }
        }
    }

    fun composeWhatsApp(
        customerId: String,
        bookingId: String?,
        context: String? = null,
        onFailure: (String) -> Unit = {},
        onSuccess: (WhatsAppComposeResult) -> Unit
    ) {
        if (state.actionBusy) {
            onFailure("Please wait for the current action to finish.")
            return
        }
        state = state.copy(actionBusy = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching { repository.composeWhatsApp(customerId, bookingId, context) }
                .onSuccess {
                    state = state.copy(actionBusy = false)
                    onSuccess(it)
                }
                .onFailure {
                    val message = userMessage(it)
                    state = state.copy(actionBusy = false, error = message)
                    scheduleErrorClear(message)
                    onFailure(message)
                }
        }
    }

    fun prepareWhatsAppTemplates(
        customerId: String,
        bookingId: String?,
        onFailure: (String) -> Unit = {},
        onSuccess: (WhatsAppTemplateBundle) -> Unit
    ) {
        if (state.actionBusy) {
            onFailure("Please wait for the current action to finish.")
            return
        }
        state = state.copy(actionBusy = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching { repository.whatsAppTemplates(customerId, bookingId) }
                .onSuccess {
                    state = state.copy(actionBusy = false)
                    onSuccess(it)
                }
                .onFailure {
                    val message = userMessage(it)
                    state = state.copy(actionBusy = false, error = message)
                    scheduleErrorClear(message)
                    onFailure(message)
                }
        }
    }

    private fun mutateAndReset(block: suspend () -> CustomerMutationResult) {
        if (state.actionBusy) return
        runMutation(block) { result ->
            AdminDataFreshness.markCustomerMutation()
            state = state.copy(
                actionBusy = false,
                archived = false,
                page = 1,
                search = "",
                sort = "NAME_ASC",
                items = emptyList(),
                loaded = false,
                highlightId = null,
                error = null,
                notice = result.message
            )
            scheduleLoad(1, keepNotice = true)
            scheduleNoticeClear(result.message)
        }
    }

    private fun runMutation(
        request: suspend () -> CustomerMutationResult,
        onSuccess: (CustomerMutationResult) -> Unit
    ) {
        if (state.actionBusy) return
        feedbackClearJob?.cancel()
        state = state.copy(actionBusy = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching { request() }
                .onSuccess(onSuccess)
                .onFailure {
                    val message = userMessage(it)
                    state = state.copy(actionBusy = false, error = message, notice = null)
                    scheduleErrorClear(message)
                }
        }
    }

    private fun scheduleNoticeClear(message: String?) {
        if (message.isNullOrBlank()) return
        feedbackClearJob?.cancel()
        feedbackClearJob = viewModelScope.launch {
            delay(2600)
            if (state.notice == message) state = state.copy(notice = null)
        }
    }

    private fun scheduleErrorClear(message: String?) {
        if (message.isNullOrBlank()) return
        feedbackClearJob?.cancel()
        feedbackClearJob = viewModelScope.launch {
            delay(5100)
            if (state.error == message) state = state.copy(error = null)
        }
    }

    private fun scheduleSearchLoad() {
        pendingLoad = null
        queuedLoadJob?.cancel()
        queuedLoadJob = viewModelScope.launch {
            delay(300)
            requestLoad(1, append = false)
        }
    }

    private fun scheduleLoad(
        page: Int,
        pinCustomerId: String? = null,
        keepNotice: Boolean = false,
        append: Boolean = false
    ) {
        pendingLoad = null
        queuedLoadJob?.cancel()
        requestLoad(page, pinCustomerId, keepNotice, append)
    }

    private fun requestLoad(
        page: Int,
        pinCustomerId: String? = null,
        keepNotice: Boolean = false,
        append: Boolean = false
    ) {
        val request = PendingCustomerLoad(page.coerceAtLeast(1), pinCustomerId, keepNotice, append)
        if (state.loading) {
            pendingLoad = request
            return
        }
        load(request.page, request.pinCustomerId, request.keepNotice, request.append)
    }

    private fun drainPendingLoad() {
        val next = pendingLoad ?: return
        pendingLoad = null
        requestLoad(next.page, next.pinCustomerId, next.keepNotice, next.append)
    }

    private fun load(
        page: Int,
        pinCustomerId: String? = null,
        keepNotice: Boolean = false,
        append: Boolean = false
    ) {
        if (state.loading) return
        val snapshot = state
        state = state.copy(
            loading = true,
            loadError = if (append) state.loadError else null,
            loadMoreError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                repository.customers(
                    page = page.coerceAtLeast(1),
                    search = snapshot.search,
                    sort = snapshot.sort,
                    archived = snapshot.archived,
                    pinCustomerId = pinCustomerId
                )
            }

            result.onSuccess { response ->
                val filtersStillCurrent =
                    state.search == snapshot.search &&
                        state.sort == snapshot.sort &&
                        state.archived == snapshot.archived
                if (filtersStillCurrent) {
                    val merged = if (append && page > 1) {
                        val seen = state.items.asSequence().map { it.id }.toMutableSet()
                        state.items + response.items.filter { seen.add(it.id) }
                    } else response.items
                    seenFreshnessRevision = AdminDataFreshness.customersRevision
                    state = state.copy(
                        items = merged,
                        summary = response.summary,
                        page = response.page,
                        totalPages = response.totalPages,
                        total = response.total,
                        loading = false,
                        loadError = null,
                        loadMoreError = null,
                        loaded = true,
                        notice = if (keepNotice) state.notice else null,
                        highlightId = pinCustomerId ?: state.highlightId
                    )
                } else {
                    state = state.copy(loading = false)
                }
            }

            result.onFailure {
                val message = userMessage(it)
                state = if (append && state.loaded) {
                    state.copy(loading = false, loadMoreError = message)
                } else {
                    state.copy(loading = false, loadError = message, loadMoreError = null)
                }
            }

            drainPendingLoad()
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message.ifBlank { "Something went wrong. Please try again." }
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    class Factory(private val repository: CustomerScreen3Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(CustomerScreen3ViewModel::class.java)) {
                return CustomerScreen3ViewModel(repository, extras.createSavedStateHandle()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
