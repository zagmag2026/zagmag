package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import kotlinx.coroutines.launch
import java.util.UUID

internal enum class BillingScreen { LIST, SELECT_ORDER, EDIT, DETAIL, SETTLEMENT }

internal data class Screen11BillingState(
    val screen: BillingScreen = BillingScreen.LIST,
    val loading: Boolean = false,
    val listRefreshing: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val list: BillingPage = BillingPage(emptyList(), 1, 1, 0),
    val listLoaded: Boolean = false,
    val listLoadError: String? = null,
    val listLoadMoreError: String? = null,
    val eligibleOrders: BillingEligiblePage = BillingEligiblePage(emptyList(), 1, 1, 0),
    val eligibleLoaded: Boolean = false,
    val eligibleLoadError: String? = null,
    val eligibleLoadMoreError: String? = null,
    val detailLoadError: String? = null,
    val orderSearch: String = "",
    val settlementBooking: BillingBookingSeed? = null,
    val search: String = "",
    val paymentFilter: String = "",
    val statusFilter: String = "",
    val bootstrap: BillingBootstrap? = null,
    val detail: BillingDetail? = null,
    val billId: String? = null,
    val bookingId: String? = null,
    val bookingNo: String? = null,
    val customerId: String = "",
    val billDate: String = "",
    val lines: List<BillingDraftLine> = emptyList(),
    val discountText: String = "0",
    val advanceText: String = "0",
    val otherReceivedText: String = "0",
    val notes: String = "",
    val expectedUpdatedAt: String? = null,
    val selectedItemId: String = "",
    val whatsapp: BillingWhatsAppBundle? = null,
    val orderPreview: BookingDetailData? = null,
    val orderPreviewLoading: Boolean = false
) {
    val totalRent: Int get() = lines.sumOf { it.amount }
    val discountAmount: Int get() = discountText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val netAmount: Int get() = (totalRent - discountAmount).coerceAtLeast(0)
    val advanceAmount: Int get() = advanceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val otherReceivedAmount: Int get() = otherReceivedText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalReceivedAmount: Int get() = (advanceAmount + otherReceivedAmount).coerceAtLeast(0)
    val paymentStatus: String get() = when {
        netAmount > 0 && totalReceivedAmount >= netAmount -> "FULL_AMOUNT_RECEIVED"
        totalReceivedAmount > 0 -> "PART_RECEIVED"
        else -> "PENDING"
    }
    val balanceAmount: Int get() = (netAmount - totalReceivedAmount).coerceAtLeast(0)
}

internal class Screen11BillingViewModel(
    private val repository: Screen11BillingRepository
) : ViewModel() {
    var state by mutableStateOf(Screen11BillingState())
        private set

    private var requestKey: String = newRequestKey()
    private var editorBaselineSignature: String = ""

    private fun currentEditorSignature(): String = buildString {
        append(state.customerId).append('|')
        append(state.billDate).append('|')
        append(state.discountText).append('|')
        append(state.advanceText).append('|')
        append(state.otherReceivedText).append('|')
        append(state.notes).append('|')
        append(state.lines.joinToString(",") { line ->
            "${line.itemId}:${line.quantity}:${line.rentRate}"
        })
    }

    private fun captureEditorBaseline() {
        editorBaselineSignature = currentEditorSignature()
    }

    fun hasUnsavedEditorChanges(): Boolean =
        state.screen == BillingScreen.EDIT &&
            editorBaselineSignature.isNotBlank() &&
            currentEditorSignature() != editorBaselineSignature

    fun loadList(page: Int = 1, append: Boolean = false, refreshing: Boolean = false) {
        if (state.loading || state.busy) return
        state = state.copy(
            loading = true,
            listRefreshing = refreshing,
            listLoadError = if (append) state.listLoadError else null,
            listLoadMoreError = null,
            error = null,
            message = null,
            whatsapp = null
        )
        viewModelScope.launch {
            runCatching {
                repository.bills(
                    page = page,
                    search = state.search,
                    paymentStatus = state.paymentFilter,
                    status = state.statusFilter
                )
            }.onSuccess { pageData ->
                val merged = if (append && page > 1) {
                    (state.list.bills + pageData.bills).distinctBy { it.id }
                } else {
                    pageData.bills
                }
                state = state.copy(
                    screen = BillingScreen.LIST,
                    loading = false,
                    listRefreshing = false,
                    list = pageData.copy(bills = merged),
                    listLoaded = true,
                    listLoadError = null,
                    listLoadMoreError = null,
                    detail = null,
                    error = null
                )
            }.onFailure { error ->
                val message = userMessage(error)
                state = if (append && state.listLoaded) {
                    state.copy(
                        loading = false,
                        listRefreshing = false,
                        listLoadMoreError = message,
                        error = null
                    )
                } else {
                    state.copy(
                        loading = false,
                        listRefreshing = false,
                        listLoadError = message,
                        listLoadMoreError = null,
                        error = null
                    )
                }
            }
        }
    }

    fun refreshList() = loadList(1, refreshing = true)

    fun loadMore() {
        val page = state.list.page
        if (!state.loading && !state.busy && state.listLoadMoreError == null && page < state.list.totalPages) {
            loadList(page + 1, append = true)
        }
    }

    fun retryLoadMore() {
        if (state.loading || state.busy || state.listLoadMoreError.isNullOrBlank()) return
        state = state.copy(listLoadMoreError = null)
        loadList(state.list.page + 1, append = true)
    }

    fun setSearch(value: String) {
        state = state.copy(search = value.take(120))
    }

    fun applySearch() = loadList(1)

    fun setPaymentFilter(value: String) {
        state = state.copy(paymentFilter = value.uppercase())
        loadList(1)
    }

    fun setStatusFilter(value: String) {
        state = state.copy(statusFilter = value.uppercase())
        loadList(1)
    }

    fun openFilteredList(paymentFilter: String = "", statusFilter: String = "") {
        if (state.loading || state.busy) return
        state = state.copy(
            screen = BillingScreen.LIST,
            search = "",
            paymentFilter = paymentFilter.uppercase(),
            statusFilter = statusFilter.uppercase(),
            list = BillingPage(emptyList(), 1, 1, 0),
            listLoaded = false,
            listLoadError = null,
            listLoadMoreError = null,
            detail = null,
            error = null,
            message = null
        )
        loadList(1)
    }

    fun clearExternalListFilters() {
        state = state.copy(search = "", paymentFilter = "", statusFilter = "")
    }

    fun openOrderSelector() {
        if (state.loading || state.busy) return
        state = state.copy(
            screen = BillingScreen.SELECT_ORDER,
            orderSearch = "",
            eligibleOrders = BillingEligiblePage(emptyList(), 1, 1, 0),
            eligibleLoaded = false,
            eligibleLoadError = null,
            eligibleLoadMoreError = null,
            error = null,
            message = null,
            whatsapp = null
        )
        loadEligibleOrders(1)
    }

    fun setOrderSearch(value: String) {
        state = state.copy(orderSearch = value.take(120))
    }

    fun applyOrderSearch() = loadEligibleOrders(1)

    fun loadMoreEligibleOrders() {
        val page = state.eligibleOrders.page
        if (!state.loading && !state.busy && state.eligibleLoadMoreError == null && page < state.eligibleOrders.totalPages) {
            loadEligibleOrders(page + 1, append = true)
        }
    }

    fun retryLoadMoreEligibleOrders() {
        if (state.loading || state.busy || state.eligibleLoadMoreError.isNullOrBlank()) return
        state = state.copy(eligibleLoadMoreError = null)
        loadEligibleOrders(state.eligibleOrders.page + 1, append = true)
    }

    fun retryEligibleOrders() {
        if (state.loading || state.busy) return
        state = state.copy(eligibleLoadError = null)
        loadEligibleOrders(1)
    }

    private fun loadEligibleOrders(page: Int, append: Boolean = false) {
        if (state.loading || state.busy) return
        state = state.copy(
            loading = true,
            eligibleLoadError = if (append) state.eligibleLoadError else null,
            eligibleLoadMoreError = null,
            error = null,
            message = null
        )
        viewModelScope.launch {
            runCatching { repository.eligibleOrders(page, state.orderSearch) }
                .onSuccess { pageData ->
                    val merged = if (append && page > 1) {
                        (state.eligibleOrders.orders + pageData.orders).distinctBy { it.id }
                    } else pageData.orders
                    state = state.copy(
                        screen = BillingScreen.SELECT_ORDER,
                        loading = false,
                        eligibleOrders = pageData.copy(orders = merged),
                        eligibleLoaded = true,
                        eligibleLoadError = null,
                        eligibleLoadMoreError = null,
                        error = null
                    )
                }.onFailure { error ->
                    val message = userMessage(error)
                    state = if (append && state.eligibleLoaded) {
                        state.copy(loading = false, eligibleLoadMoreError = message, error = null)
                    } else {
                        state.copy(loading = false, eligibleLoadError = message, eligibleLoadMoreError = null, error = null)
                    }
                }
        }
    }

    fun openOrderPreview(bookingId: String) {
        if (bookingId.isBlank() || state.orderPreviewLoading || state.busy) return
        state = state.copy(orderPreviewLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.bookingDetail(bookingId) }
                .onSuccess { detail ->
                    state = state.copy(orderPreviewLoading = false, orderPreview = detail)
                }
                .onFailure { error ->
                    state = state.copy(orderPreviewLoading = false, error = userMessage(error))
                }
        }
    }

    fun closeOrderPreview() {
        state = state.copy(orderPreview = null, orderPreviewLoading = false)
    }

    fun createDraftFromOrder(bookingId: String) {
        if (state.loading || state.busy || bookingId.isBlank()) return
        state = state.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            runCatching {
                val ensured = repository.ensureDraft(bookingId)
                val data = repository.bootstrap(bookingId)
                val detail = repository.detail(ensured.id)
                Triple(ensured, data, detail)
            }.onSuccess { (_, data, detail) ->
                AdminDataFreshness.markBillingMutation()
                if (detail.bill.status == "DRAFT") {
                    openEditorState(data, detail)
                } else {
                    state = state.copy(
                        screen = BillingScreen.DETAIL,
                        loading = false,
                        busy = false,
                        detail = detail,
                        billId = detail.bill.id,
                        bookingId = detail.bill.bookingId,
                        error = null,
                        message = null
                    )
                }
            }.onFailure(::failBusy)
        }
    }

    fun openBookingBill(bookingId: String) = openCreate(bookingId)

    private fun openCreate(bookingId: String) {
        if (state.loading || state.busy || bookingId.isBlank()) return
        state = state.copy(loading = true, detailLoadError = null, error = null, message = null, whatsapp = null)
        viewModelScope.launch {
            runCatching {
                val data = repository.bootstrap(bookingId)
                val detail = data.existingBillId?.takeIf { it.isNotBlank() }?.let { repository.detail(it) }
                data to detail
            }.onSuccess { (data, existingDetail) ->
                if (existingDetail != null) {
                    if (existingDetail.bill.status == "DRAFT") openEditorState(data, existingDetail)
                    else state = state.copy(
                        screen = BillingScreen.DETAIL,
                        loading = false,
                        busy = false,
                        bootstrap = data,
                        detail = existingDetail,
                        billId = existingDetail.bill.id,
                        bookingId = existingDetail.bill.bookingId,
                        settlementBooking = null,
                        error = null,
                        message = null
                    )
                    return@onSuccess
                }
                val booking = data.booking
                if (booking?.confirmationState == "RESERVED") {
                    state = state.copy(
                        screen = BillingScreen.LIST,
                        loading = false,
                        busy = false,
                        bootstrap = data,
                        detailLoadError = "Confirm the Reserved Order before creating a Bill.",
                        error = null,
                        message = null
                    )
                    return@onSuccess
                }
                if (booking?.status == "CANCELLED") {
                    state = state.copy(
                        screen = BillingScreen.SETTLEMENT,
                        loading = false,
                        busy = false,
                        bootstrap = data,
                        settlementBooking = booking,
                        detail = null,
                        billId = null,
                        bookingId = booking.id,
                        bookingNo = booking.bookingNo,
                        error = null,
                        message = null
                    )
                    return@onSuccess
                }
                requestKey = newRequestKey()
                state = state.copy(
                    screen = BillingScreen.EDIT,
                    loading = false,
                    busy = false,
                    bootstrap = data,
                    detail = null,
                    settlementBooking = null,
                    billId = null,
                    bookingId = booking?.id,
                    bookingNo = booking?.bookingNo,
                    customerId = booking?.customerId.orEmpty(),
                    billDate = data.today,
                    lines = data.bookingItems,
                    discountText = "0",
                    advanceText = (booking?.advanceAmount ?: 0).toString(),
                    otherReceivedText = "0",
                    notes = "",
                    expectedUpdatedAt = null,
                    selectedItemId = "",
                    detailLoadError = null,
                    error = null,
                    message = null
                )
                captureEditorBaseline()
            }.onFailure { error ->
                state = state.copy(
                    screen = BillingScreen.LIST,
                    loading = false,
                    busy = false,
                    detailLoadError = userMessage(error),
                    error = null,
                    message = null
                )
            }
        }
    }

    private fun openEditorState(data: BillingBootstrap, detail: BillingDetail) {
        requestKey = newRequestKey()
        state = state.copy(
            screen = BillingScreen.EDIT,
            loading = false,
            busy = false,
            bootstrap = data,
            detail = detail,
            settlementBooking = null,
            billId = detail.bill.id,
            bookingId = detail.bill.bookingId,
            bookingNo = detail.bill.bookingNo,
            customerId = detail.bill.customerId,
            billDate = detail.bill.billDate,
            lines = detail.items,
            discountText = detail.bill.discountAmount.toString(),
            advanceText = detail.bill.advanceAmount.toString(),
            otherReceivedText = (detail.bill.receivedAmount - detail.bill.advanceAmount).coerceAtLeast(0).toString(),
            notes = detail.bill.notes,
            expectedUpdatedAt = detail.bill.updatedAt,
            selectedItemId = "",
            detailLoadError = null,
            error = null,
            message = null
        )
        captureEditorBaseline()
    }

    fun openDetail(id: String) {
        if (state.loading || state.busy) return
        state = state.copy(
            screen = BillingScreen.DETAIL,
            loading = true,
            detail = null,
            billId = id,
            detailLoadError = null,
            error = null,
            message = null,
            whatsapp = null
        )
        viewModelScope.launch { openDetailAfterLoad(id) }
    }

    fun retryDetail() {
        val id = state.billId ?: state.detail?.bill?.id ?: return
        openDetail(id)
    }

    fun retryBookingBill(bookingId: String) {
        if (bookingId.isBlank() || state.loading || state.busy) return
        state = state.copy(detailLoadError = null)
        openCreate(bookingId)
    }

    private suspend fun openDetailAfterLoad(id: String) {
        runCatching { repository.detail(id) }
            .onSuccess { detail ->
                val bookingId = detail.bill.bookingId
                if (detail.bill.status == "DRAFT" && !bookingId.isNullOrBlank()) {
                    runCatching { repository.bootstrap(bookingId) }
                        .onSuccess { data -> openEditorState(data, detail) }
                        .onFailure { error ->
                            state = state.copy(
                                loading = false,
                                detail = null,
                                billId = id,
                                detailLoadError = userMessage(error),
                                error = null
                            )
                        }
                } else {
                    state = state.copy(
                        screen = BillingScreen.DETAIL,
                        loading = false,
                        detail = detail,
                        billId = detail.bill.id,
                        detailLoadError = null,
                        error = null,
                        message = null,
                        whatsapp = null
                    )
                }
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    detailLoadError = userMessage(error),
                    error = null
                )
            }
    }

    fun editDraft() {
        val detail = state.detail ?: return
        if (detail.bill.status != "DRAFT" || detail.bill.bookingId.isNullOrBlank() || state.loading || state.busy) return
        state = state.copy(loading = true, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.bootstrap(detail.bill.bookingId) }
                .onSuccess { data -> openEditorState(data, detail) }
                .onFailure(::fail)
        }
    }

    fun back() {
        when (state.screen) {
            BillingScreen.LIST -> Unit
            BillingScreen.SELECT_ORDER, BillingScreen.SETTLEMENT -> {
                state = state.copy(screen = BillingScreen.LIST, error = null, message = null, settlementBooking = null)
                loadList(1)
            }
            BillingScreen.EDIT -> {
                state = state.copy(
                    screen = BillingScreen.LIST,
                    error = null,
                    message = null,
                    detail = null,
                    detailLoadError = null,
                    whatsapp = null
                )
                loadList(1)
            }
            BillingScreen.DETAIL -> {
                state = state.copy(screen = BillingScreen.LIST, error = null, message = null, whatsapp = null)
                loadList(1)
            }
        }
    }

    fun setCustomer(value: String) { state = state.copy(customerId = value) }
    fun setBillDate(value: String) { state = state.copy(billDate = value) }
    fun setDiscount(value: String) { state = state.copy(discountText = digits(value)) }
    fun setAdvance(value: String) { state = state.copy(advanceText = digits(value)) }
    fun setOtherReceived(value: String) { state = state.copy(otherReceivedText = digits(value)) }
    fun setNotes(value: String) { state = state.copy(notes = value.take(500)) }
    fun selectItem(value: String) { state = state.copy(selectedItemId = value) }

    fun addSelectedItem() {
        val itemId = state.selectedItemId
        val option = state.bootstrap?.items?.firstOrNull { it.id == itemId } ?: return
        if (state.lines.any { it.itemId == itemId }) {
            state = state.copy(error = "This Item is already in the Bill.")
            return
        }
        state = state.copy(
            lines = state.lines + BillingDraftLine(
                itemId = option.id,
                itemCode = option.itemCode,
                itemName = option.itemName,
                categoryName = option.categoryName,
                quantity = 1,
                rentRate = option.rentAmount,
                imageUrl = option.imageUrl,
                imageUrls = option.imageUrls
            ),
            selectedItemId = "",
            error = null
        )
    }

    fun setLineQuantity(itemId: String, value: String) {
        val qty = value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1
        state = state.copy(lines = state.lines.map { if (it.itemId == itemId) it.copy(quantity = qty) else it })
    }

    fun setLineRent(itemId: String, value: String) {
        val rate = value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(0) ?: 0
        state = state.copy(lines = state.lines.map { if (it.itemId == itemId) it.copy(rentRate = rate) else it })
    }

    fun removeLine(itemId: String) {
        state = state.copy(lines = state.lines.filterNot { it.itemId == itemId })
    }

    fun saveDraft(finalizeAfterSave: Boolean = false) {
        if (state.busy) return
        if (finalizeAfterSave && state.bootstrap?.booking?.returnComplete != true) {
            state = state.copy(
                error = "Bill can be finalized only after all picked-up items are fully returned.",
                message = null
            )
            return
        }
        val validation = validateDraft()
        if (validation != null) {
            state = state.copy(error = validation, message = null)
            return
        }
        state = state.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            runCatching {
                repository.saveDraft(
                    id = state.billId,
                    requestKey = requestKey,
                    bookingId = state.bookingId,
                    customerId = state.customerId,
                    billDate = state.billDate,
                    discountAmount = state.discountAmount,
                    advanceAmount = state.advanceAmount,
                    otherReceivedAmount = state.otherReceivedAmount,
                    notes = state.notes,
                    lines = state.lines,
                    expectedUpdatedAt = state.expectedUpdatedAt
                )
            }.onSuccess { id ->
                AdminDataFreshness.markBillingMutation()
                val resolvedId = id.ifBlank { state.billId.orEmpty() }
                if (resolvedId.isBlank()) {
                    state = state.copy(busy = false, error = "Bill was saved but could not be reopened.")
                    return@onSuccess
                }
                if (finalizeAfterSave) {
                    runCatching { repository.finalize(resolvedId) }
                        .onSuccess {
                            AdminDataFreshness.markBillingMutation()
                            state = state.copy(busy = false)
                            openDetail(resolvedId)
                        }.onFailure(::failBusy)
                } else {
                    runCatching {
                        val detail = repository.detail(resolvedId)
                        val bookingId = detail.bill.bookingId ?: state.bookingId
                        val data = bookingId?.let { repository.bootstrap(it) } ?: state.bootstrap
                        data to detail
                    }.onSuccess { (data, detail) ->
                        if (data != null) {
                            openEditorState(data, detail)
                            state = state.copy(message = "Quotation saved.")
                        } else {
                            state = state.copy(busy = false, message = "Quotation saved.", detail = detail)
                        }
                    }.onFailure(::failBusy)
                }
            }.onFailure(::failBusy)
        }
    }

    fun finalizeDraft() {
        val bill = state.detail?.bill ?: return
        val id = bill.id
        if (bill.status != "DRAFT" || state.busy) return
        if (!bill.returnComplete) {
            state = state.copy(
                error = "Bill can be finalized only after all picked-up items are fully returned.",
                message = null
            )
            return
        }
        state = state.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.finalize(id) }
                .onSuccess {
                    AdminDataFreshness.markBillingMutation()
                    state = state.copy(busy = false, message = "Bill finalized.")
                    openDetail(id)
                }.onFailure(::failBusy)
        }
    }



    fun deleteDraft() {
        val bill = state.detail?.bill ?: return
        if (bill.status != "DRAFT" || state.busy) return
        state = state.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.deleteDraft(bill.id) }
                .onSuccess {
                    AdminDataFreshness.markBillingMutation()
                    state = Screen11BillingState(message = "Quotation deleted.")
                    loadList(1)
                }.onFailure(::failBusy)
        }
    }

    fun createCustomer(name: String, mobile: String, address: String, onSaved: () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.createCustomer(name, mobile, address) }
                .onSuccess { customer ->
                    AdminDataFreshness.markCustomerMutation()
                    val bootstrap = state.bootstrap
                    state = state.copy(
                        busy = false,
                        bootstrap = bootstrap?.copy(customers = listOf(customer) + bootstrap.customers.filterNot { it.id == customer.id }),
                        customerId = customer.id,
                        message = "Customer added."
                    )
                    onSaved()
                }.onFailure(::failBusy)
        }
    }

    fun prepareWhatsApp() {
        val bill = state.detail?.bill ?: return
        if (bill.billNo.isNullOrBlank() || state.busy) return
        state = state.copy(busy = true, error = null, message = null, whatsapp = null)
        viewModelScope.launch {
            runCatching { repository.whatsapp(bill.customerId, bill.id) }
                .onSuccess { bundle -> state = state.copy(busy = false, whatsapp = bundle) }
                .onFailure(::failBusy)
        }
    }

    fun clearWhatsApp() { state = state.copy(whatsapp = null) }
    fun clearFeedback() { state = state.copy(error = null, message = null) }

    private fun validateDraft(): String? {
        if (state.bookingId.isNullOrBlank()) return "Select an Order before creating a Bill."
        if (state.customerId.isBlank()) return "Select a Customer."
        if (state.billDate.isBlank()) return "Select Bill Date."
        if (state.lines.isEmpty()) return "Add at least one Item."
        if (state.lines.any { it.quantity < 1 }) return "Item quantity must be at least 1."
        if (state.discountAmount > state.totalRent) return "Discount cannot be more than Total Rent."
        if (state.advanceAmount > state.netAmount) return "Advance Amount cannot be more than Bill Amount."
        if (state.totalReceivedAmount > state.netAmount) {
            return "Advance + Other Received cannot be more than Bill Amount."
        }
        return null
    }

    private fun digits(value: String): String =
        value.filter(Char::isDigit).take(9)

    private fun fail(error: Throwable) {
        state = state.copy(loading = false, listRefreshing = false, busy = false, error = userMessage(error))
    }

    private fun failBusy(error: Throwable) {
        state = state.copy(busy = false, loading = false, listRefreshing = false, error = userMessage(error))
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    private fun newRequestKey(): String = "bill-${UUID.randomUUID()}"

    class Factory(private val repository: Screen11BillingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            Screen11BillingViewModel(repository) as T
    }
}
