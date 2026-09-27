package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerCardRow
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerMutationResult
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerScreen3Repository
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.WhatsAppComposeResult
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
    val error: String? = null,
    val notice: String? = null,
    val loaded: Boolean = false,
    val highlightId: String? = null,
    val actionBusy: Boolean = false
)

private data class PendingCustomerLoad(
    val page: Int,
    val pinCustomerId: String?,
    val keepNotice: Boolean
)

class CustomerScreen3ViewModel(private val repository: CustomerScreen3Repository) : ViewModel() {
    var state by mutableStateOf(CustomerScreenState())
        private set

    private var queuedLoadJob: Job? = null
    private var pendingLoad: PendingCustomerLoad? = null

    fun ensureLoaded() {
        if (!state.loaded && !state.loading) load(1)
    }

    fun search(value: String) {
        state = state.copy(search = value, page = 1, error = null, highlightId = null)
        scheduleSearchLoad()
    }

    fun sort(value: String) {
        if (value !in setOf("NAME_ASC", "NAME_DESC", "NEWEST", "OLDEST")) return
        state = state.copy(sort = value, page = 1, highlightId = null)
        scheduleLoad(1)
    }

    fun setArchived(archived: Boolean) {
        state = state.copy(archived = archived, page = 1, highlightId = null)
        scheduleLoad(1)
    }

    fun page(page: Int) {
        scheduleLoad(page.coerceAtLeast(1))
    }

    fun refresh() {
        state = state.copy(
            page = 1,
            search = "",
            sort = "NAME_ASC",
            archived = false,
            error = null,
            notice = null,
            highlightId = null
        )
        scheduleLoad(1)
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
        val alternate = alternateMobile.filter(Char::isDigit)
        when {
            name.trim().isBlank() -> state = state.copy(error = "Name is required.")
            primary.length != 10 -> state = state.copy(error = "Enter a valid 10-digit mobile number.")
            alternate.isNotBlank() && alternate.length != 10 -> state = state.copy(error = "Enter a valid 10-digit alternative mobile number.")
            else -> runMutation(
                request = {
                    if (id == null) repository.create(name, primary, alternate, address)
                    else repository.update(id, name, primary, alternate, address)
                },
                onSuccess = { result ->
                    onSuccess()
                    val highlight = result.id ?: id
                    state = state.copy(
                        actionBusy = false,
                        archived = false,
                        page = 1,
                        search = "",
                        notice = result.message,
                        highlightId = highlight
                    )
                    scheduleLoad(1, pinCustomerId = highlight, keepNotice = true)
                }
            )
        }
    }

    fun archive(id: String) = mutateAndReset { repository.archive(id) }
    fun restore(id: String) = mutateAndReset { repository.restore(id) }
    fun deletePermanently(id: String) = mutateAndReset { repository.deletePermanently(id) }

    fun composeWhatsApp(customerId: String, bookingId: String?, onSuccess: (WhatsAppComposeResult) -> Unit) {
        if (state.actionBusy) return
        viewModelScope.launch {
            state = state.copy(actionBusy = true, error = null)
            runCatching { repository.composeWhatsApp(customerId, bookingId) }
                .onSuccess {
                    state = state.copy(actionBusy = false)
                    onSuccess(it)
                }
                .onFailure { state = state.copy(actionBusy = false, error = userMessage(it)) }
        }
    }

    private fun mutateAndReset(block: suspend () -> CustomerMutationResult) {
        if (state.actionBusy) return
        runMutation(block) { result ->
            state = state.copy(
                actionBusy = false,
                archived = false,
                page = 1,
                search = "",
                sort = "NAME_ASC",
                highlightId = null,
                notice = result.message
            )
            scheduleLoad(1, keepNotice = true)
        }
    }

    private fun runMutation(
        request: suspend () -> CustomerMutationResult,
        onSuccess: (CustomerMutationResult) -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(actionBusy = true, error = null, notice = null)
            runCatching { request() }
                .onSuccess(onSuccess)
                .onFailure { state = state.copy(actionBusy = false, error = userMessage(it)) }
        }
    }

    private fun scheduleSearchLoad() {
        pendingLoad = null
        queuedLoadJob?.cancel()
        queuedLoadJob = viewModelScope.launch {
            delay(350)
            requestLoad(1)
        }
    }

    private fun scheduleLoad(
        page: Int,
        pinCustomerId: String? = null,
        keepNotice: Boolean = false
    ) {
        pendingLoad = null
        queuedLoadJob?.cancel()
        requestLoad(page, pinCustomerId, keepNotice)
    }

    private fun requestLoad(page: Int, pinCustomerId: String? = null, keepNotice: Boolean = false) {
        val request = PendingCustomerLoad(page.coerceAtLeast(1), pinCustomerId, keepNotice)
        if (state.loading) {
            pendingLoad = request
            return
        }
        load(request.page, request.pinCustomerId, request.keepNotice)
    }

    private fun drainPendingLoad() {
        val next = pendingLoad ?: return
        pendingLoad = null
        requestLoad(next.page, next.pinCustomerId, next.keepNotice)
    }

    private fun load(page: Int, pinCustomerId: String? = null, keepNotice: Boolean = false) {
        if (state.loading) return
        val snapshot = state
        state = state.copy(loading = true, error = null)
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
                    state = state.copy(
                        items = response.items,
                        summary = response.summary,
                        page = response.page,
                        totalPages = response.totalPages,
                        total = response.total,
                        loading = false,
                        error = null,
                        loaded = true,
                        notice = if (keepNotice) state.notice else null,
                        highlightId = pinCustomerId ?: state.highlightId
                    )
                } else {
                    state = state.copy(loading = false)
                }
            }

            result.onFailure {
                state = state.copy(loading = false, error = userMessage(it))
            }

            drainPendingLoad()
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message.ifBlank { "Request failed." }
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Unable to complete the request."
    }

    class Factory(private val repository: CustomerScreen3Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerScreen3ViewModel::class.java)) {
                return CustomerScreen3ViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
