package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.AdminRepository
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingActionLine
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingBootstrapData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionCustomer
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingRequestLine
import com.nimsdeveloper.zhagmagdresses.admin.data.Customer
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.PageResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class BookingDraftLine(
    val categoryId: String = "",
    val itemId: String = "",
    val quantity: Int = 1
)

private data class PendingBookingListLoad(
    val page: Int,
    val search: String,
    val status: String,
    val view: String,
    val sort: String
)

class BookingLifecycleViewModel(private val repository: AdminRepository) : ViewModel() {
    var listState by mutableStateOf(PagedState<LifecycleBookingSummary>())
        private set
    private var listStatusState by mutableStateOf("")
    val listStatus: String get() = listStatusState
    private var listViewState by mutableStateOf("RESERVED")
    val listView: String get() = listViewState
    private var listSortState by mutableStateOf("NEWEST")
    val listSort: String get() = listSortState

    var editorOpen by mutableStateOf(false)
        private set
    var editorStartsInEditMode by mutableStateOf(false)
        private set
    var bootstrapState by mutableStateOf(DataState<BookingBootstrapData>())
        private set
    var detailState by mutableStateOf(DataState<BookingDetailData>())
        private set
    var actionBusy by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set
    var actionNotice by mutableStateOf<String?>(null)
        private set
    var mutationVersion by mutableStateOf(0)
        private set

    private var customerSearchState by mutableStateOf("")
    val customerSearch: String get() = customerSearchState
    var customerOptions by mutableStateOf<List<BookingOptionCustomer>>(emptyList())
        private set
    var selectedCustomerId by mutableStateOf("")
        private set

    var newCustomerBusy by mutableStateOf(false)
        private set
    var newCustomerError by mutableStateOf<String?>(null)
        private set

    private var pickupDateState by mutableStateOf("")
    val pickupDate: String get() = pickupDateState
    private var returnDateState by mutableStateOf("")
    val returnDate: String get() = returnDateState
    private var notesState by mutableStateOf("")
    val notes: String get() = notesState

    var availabilityLoading by mutableStateOf(false)
        private set
    var availabilityError by mutableStateOf<String?>(null)
        private set
    private var availabilityByItemState by mutableStateOf<Map<String, Int>>(emptyMap())
    val availabilityByItem: Map<String, Int> get() = availabilityByItemState

    private var pickupNotesState by mutableStateOf("")
    val pickupNotes: String get() = pickupNotesState
    private var returnNotesState by mutableStateOf("")
    val returnNotes: String get() = returnNotesState

    var draftLines by mutableStateOf<List<BookingDraftLine>>(emptyList())
        private set
    var pickupNow by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var returnNow by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var pendingListLoad: PendingBookingListLoad? = null
    private var customerSearchLoading = false
    private var pendingCustomerSearch: String? = null

    val itemOptions: List<BookingOptionItem>
        get() = bootstrapState.data?.items.orEmpty()

    val selectedCustomer: BookingOptionCustomer?
        get() = customerOptions.firstOrNull { it.id == selectedCustomerId }
            ?: bootstrapState.data?.customers?.firstOrNull { it.id == selectedCustomerId }

    fun ensureList() {
        if (!listState.loaded && !listState.loading) {
            requestList(1, listState.search, listStatus, listView, listSort)
        }
    }

    fun searchBookings(value: String) {
        val normalized = value.trim()
        if (listState.loaded && listState.search == normalized && !listState.loading) return
        requestList(1, normalized, listStatus, listView, listSort)
    }

    fun setListView(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("RESERVED", "BOOKED", "PICKED_UP", "RETURNED", "ALL")) return
        if (normalized == listView && listState.loaded) return
        listViewState = normalized
        listStatusState = ""
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun setListStatus(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("", "RESERVED", "BOOKED", "PARTIALLY_GIVEN", "GIVEN", "PARTIALLY_RETURNED", "RETURNED", "CANCELLED")) return
        if (normalized == listStatus && listState.loaded) return
        listStatusState = normalized
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun setListSort(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("NEWEST", "OLDEST", "PICKUP_ASC", "PICKUP_DESC", "RETURN_ASC", "RETURN_DESC")) return
        if (normalized == listSort && listState.loaded) return
        listSortState = normalized
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun bookingPage(page: Int) = requestList(page, listState.search, listStatus, listView, listSort)
    fun refreshList() = requestList(listState.page, listState.search, listStatus, listView, listSort)

    fun openNew() {
        ensureBootstrap()
        editorOpen = true
        editorStartsInEditMode = false
        detailState = DataState()
        actionError = null
        actionNotice = null
        selectedCustomerId = ""
        customerSearchState = ""
        newCustomerError = null
        val today = businessToday()
        pickupDateState = today.toString()
        returnDateState = today.plusDays(1).toString()
        notesState = ""
        availabilityByItemState = emptyMap()
        availabilityError = null
        pickupNotesState = ""
        returnNotesState = ""
        draftLines = emptyList()
        pickupNow = emptyMap()
        returnNow = emptyMap()
    }

    fun openBooking(id: String, edit: Boolean = false) {
        ensureBootstrap()
        editorOpen = true
        editorStartsInEditMode = edit
        actionError = null
        actionNotice = null
        availabilityError = null
        availabilityByItemState = emptyMap()
        pickupNotesState = ""
        returnNotesState = ""
        detailState = DataState(loading = true)
        viewModelScope.launch {
            runCatching { repository.bookingDetail(id) }
                .onSuccess { detail -> applyDetail(detail) }
                .onFailure { detailState = DataState(error = userMessage(it), loaded = true) }
        }
    }

    fun refreshEditor() {
        val id = detailState.data?.booking?.id
        if (!id.isNullOrBlank()) reloadDetail(id) else ensureBootstrap(force = true)
    }

    fun backFromEditor() {
        if (actionBusy || newCustomerBusy || availabilityLoading) return
        editorOpen = false
        editorStartsInEditMode = false
        actionError = null
        actionNotice = null
        newCustomerError = null
        availabilityError = null
        pickupNotesState = ""
        returnNotesState = ""
        requestList(listState.page, listState.search, listStatus, listView, listSort)
    }

    fun setSelectedCustomer(id: String) {
        selectedCustomerId = id
        actionError = null
    }

    fun clearSelectedCustomer() {
        selectedCustomerId = ""
        customerSearchState = ""
        customerOptions = bootstrapState.data?.customers.orEmpty()
        actionError = null
    }

    fun setPickupDate(value: String) {
        pickupDateState = value.trim()
        val pickup = runCatching { LocalDate.parse(pickupDateState) }.getOrNull()
        val returns = runCatching { LocalDate.parse(returnDateState) }.getOrNull()
        if (pickup != null && (returns == null || !returns.isAfter(pickup))) {
            returnDateState = pickup.plusDays(1).toString()
        }
        clearAvailability()
    }

    fun setReturnDate(value: String) {
        returnDateState = value.trim()
        clearAvailability()
    }

    fun setNotes(value: String) { notesState = value.take(500) }
    fun setPickupNotes(value: String) { pickupNotesState = value.take(500) }
    fun setReturnNotes(value: String) { returnNotesState = value.take(500) }

    fun setCustomerSearch(value: String) {
        customerSearchState = value
        val normalized = value.trim()
        actionError = null
        when {
            normalized.isEmpty() -> customerOptions = bootstrapState.data?.customers.orEmpty()
            normalized.length >= 2 -> searchCustomerOptions(normalized)
        }
    }

    fun createCustomerForBooking(
        name: String,
        mobile: String,
        alternateMobile: String,
        address: String,
        onSuccess: () -> Unit
    ) {
        if (newCustomerBusy || actionBusy) return
        val cleanName = name.trim()
        val primary = mobile.filter(Char::isDigit)
        val alternate = alternateMobile.filter(Char::isDigit)
        newCustomerError = when {
            cleanName.isBlank() -> "Name is required."
            primary.length != 10 -> "Enter a valid 10-digit primary mobile number."
            alternate.isNotBlank() && alternate.length != 10 -> "Enter a valid 10-digit alternate mobile number."
            else -> null
        }
        if (newCustomerError != null) return

        viewModelScope.launch {
            newCustomerBusy = true
            newCustomerError = null
            runCatching { repository.createBookingCustomer(cleanName, primary, alternate, address) }
                .onSuccess { result ->
                    val id = result.id
                    if (id.isNullOrBlank()) {
                        newCustomerError = "Customer was saved but could not be selected."
                    } else {
                        val option = BookingOptionCustomer(id, cleanName, primary)
                        customerOptions = listOf(option) + customerOptions.filterNot { it.id == id }
                        selectedCustomerId = id
                        customerSearchState = ""
                        actionError = null
                        onSuccess()
                    }
                }
                .onFailure { newCustomerError = userMessage(it) }
            newCustomerBusy = false
        }
    }

    fun validateCustomerStep(): Boolean {
        actionError = if (selectedCustomerId.isBlank()) "Select a customer before continuing." else null
        return actionError == null
    }

    fun validateDetailsStep(): Boolean {
        actionError = detailsValidationError()
        return actionError == null
    }

    fun loadAvailabilityForSelectedDates(onSuccess: () -> Unit = {}) {
        if (availabilityLoading || !validateDetailsStep()) return
        val ids = itemOptions.map { it.id }.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) {
            availabilityByItemState = emptyMap()
            availabilityError = null
            onSuccess()
            return
        }
        availabilityLoading = true
        availabilityError = null
        viewModelScope.launch {
            runCatching {
                repository.bookingAvailability(
                    pickupDate = pickupDate,
                    returnDate = returnDate,
                    itemIds = ids,
                    excludeBookingId = detailState.data?.booking?.id
                )
            }.onSuccess { result ->
                availabilityByItemState = result
                availabilityError = null
                onSuccess()
            }.onFailure {
                availabilityError = userMessage(it)
            }
            availabilityLoading = false
        }
    }

    fun availableQuantity(itemId: String): Int =
        availabilityByItemState[itemId]
            ?: itemOptions.firstOrNull { it.id == itemId }?.totalQuantity
            ?: 0

    fun addOrUpdateItem(itemId: String, quantity: Int): Boolean {
        actionError = null
        val item = itemOptions.firstOrNull { it.id == itemId }
        if (item == null) {
            actionError = "Select an item."
            return false
        }
        val available = availableQuantity(itemId)
        if (available <= 0) {
            actionError = "${item.itemName} is not available for the selected dates."
            return false
        }
        if (quantity !in 1..available) {
            actionError = "${item.itemName}: maximum available quantity is $available."
            return false
        }
        val existing = draftLines.indexOfFirst { it.itemId == itemId }
        val line = BookingDraftLine(item.categoryId, itemId, quantity)
        draftLines = if (existing >= 0) {
            draftLines.mapIndexed { index, current -> if (index == existing) line else current }
        } else {
            draftLines + line
        }
        return true
    }

    fun removeItem(itemId: String) {
        draftLines = draftLines.filterNot { it.itemId == itemId }
        actionError = null
    }

    fun validateItemsStep(): Boolean {
        actionError = when {
            draftLines.isEmpty() -> "Add at least one item before continuing."
            draftLines.any { it.itemId.isBlank() || it.quantity < 1 } -> "Every selected item needs a valid quantity."
            draftLines.any { it.quantity > availableQuantity(it.itemId) } -> "One or more item quantities exceed current availability."
            else -> null
        }
        return actionError == null
    }

    fun addLine() {
        if (draftLines.size >= 20) return
        draftLines = draftLines + BookingDraftLine()
    }

    fun removeLine(index: Int) {
        if (index !in draftLines.indices) return
        draftLines = draftLines.filterIndexed { lineIndex, _ -> lineIndex != index }
    }

    fun setLineCategory(index: Int, categoryId: String) {
        if (index !in draftLines.indices) return
        draftLines = draftLines.mapIndexed { lineIndex, line ->
            if (lineIndex != index) line
            else {
                val currentItem = itemOptions.firstOrNull { it.id == line.itemId }
                line.copy(
                    categoryId = categoryId,
                    itemId = if (currentItem?.categoryId == categoryId) line.itemId else ""
                )
            }
        }
    }

    fun setLineItem(index: Int, itemId: String) {
        if (index !in draftLines.indices) return
        val item = itemOptions.firstOrNull { it.id == itemId }
        draftLines = draftLines.mapIndexed { lineIndex, line ->
            if (lineIndex == index) line.copy(itemId = itemId, categoryId = item?.categoryId ?: line.categoryId) else line
        }
    }

    fun setLineQuantity(index: Int, quantity: Int) {
        if (index !in draftLines.indices) return
        val itemId = draftLines[index].itemId
        val max = if (itemId.isBlank()) 100000 else availableQuantity(itemId).coerceAtLeast(1)
        val safe = quantity.coerceIn(1, max)
        draftLines = draftLines.mapIndexed { lineIndex, line -> if (lineIndex == index) line.copy(quantity = safe) else line }
    }

    fun setPickupNow(bookingItemId: String, quantity: Int) {
        val max = detailState.data?.items?.firstOrNull { it.bookingItemId == bookingItemId }?.remainingToGive ?: 0
        pickupNow = pickupNow + (bookingItemId to quantity.coerceIn(0, max))
    }

    fun setReturnNow(bookingItemId: String, quantity: Int) {
        val max = detailState.data?.items?.firstOrNull { it.bookingItemId == bookingItemId }?.pendingToReturn ?: 0
        returnNow = returnNow + (bookingItemId to quantity.coerceIn(0, max))
    }

    fun fillAllPickup() {
        pickupNow = detailState.data?.items.orEmpty().associate { it.bookingItemId to it.remainingToGive }
    }

    fun fillAllReturn() {
        returnNow = detailState.data?.items.orEmpty().associate { it.bookingItemId to it.pendingToReturn }
    }

    fun reserve() = createBooking("RESERVED", directPickup = false)
    fun createConfirmed() = createBooking("BOOKED", directPickup = false)
    fun directPickup() = createBooking("BOOKED", directPickup = true)

    fun saveChanges() {
        val detail = detailState.data ?: return
        val lines = validatedRequestLines() ?: return
        runAction {
            repository.updateBooking(
                id = detail.booking.id,
                customerId = selectedCustomerId,
                pickupDate = pickupDate,
                returnDate = returnDate,
                notes = notes,
                lines = lines
            )
            editorStartsInEditMode = false
            actionNotice = "Booking updated."
            reloadDetail(detail.booking.id)
        }
    }

    fun confirmReserved() {
        val id = detailState.data?.booking?.id ?: return
        runAction {
            actionNotice = repository.confirmBooking(id).message
            reloadDetail(id)
        }
    }

    fun cancelBooking() {
        val id = detailState.data?.booking?.id ?: return
        runAction {
            actionNotice = repository.cancelBooking(id).message
            reloadDetail(id)
        }
    }

    fun savePickup() {
        val detail = detailState.data ?: return
        val lines = detail.items.mapNotNull { item ->
            val quantity = pickupNow[item.bookingItemId] ?: 0
            quantity.takeIf { it > 0 }?.let { BookingActionLine(item.bookingItemId, it) }
        }
        if (lines.isEmpty()) {
            actionError = "Enter at least one pickup quantity."
            return
        }
        runAction {
            actionNotice = repository.savePickup(
                detail.booking.id,
                lines,
                requestKey("pickup"),
                pickupNotes
            ).message
            pickupNotesState = ""
            reloadDetail(detail.booking.id)
        }
    }

    fun saveReturn() {
        val detail = detailState.data ?: return
        val lines = detail.items.mapNotNull { item ->
            val quantity = returnNow[item.bookingItemId] ?: 0
            quantity.takeIf { it > 0 }?.let { BookingActionLine(item.bookingItemId, it) }
        }
        if (lines.isEmpty()) {
            actionError = "Enter at least one return quantity."
            return
        }
        runAction {
            actionNotice = repository.saveReturn(
                detail.booking.id,
                lines,
                requestKey("return"),
                returnNotes
            ).message
            returnNotesState = ""
            reloadDetail(detail.booking.id)
        }
    }

    private fun createBooking(confirmationState: String, directPickup: Boolean) {
        val lines = validatedRequestLines() ?: return
        runAction {
            val saved = repository.createBooking(
                customerId = selectedCustomerId,
                pickupDate = pickupDate,
                returnDate = returnDate,
                notes = notes,
                lines = lines,
                confirmationState = confirmationState,
                requestKey = requestKey("booking")
            )
            if (!directPickup) {
                actionNotice = saved.message
                reloadDetail(saved.id)
                return@runAction
            }

            val detail = repository.bookingDetail(saved.id)
            val giveAll = detail.items.mapNotNull { item ->
                item.remainingToGive.takeIf { it > 0 }?.let { BookingActionLine(item.bookingItemId, it) }
            }
            if (giveAll.isEmpty()) {
                actionNotice = saved.message
                applyDetail(detail)
                return@runAction
            }
            runCatching { repository.savePickup(saved.id, giveAll, requestKey("pickup")) }
                .onSuccess {
                    actionNotice = "Direct pickup saved."
                    reloadDetail(saved.id)
                }
                .onFailure { error ->
                    actionError = "Booking saved. Pickup failed: ${userMessage(error)}"
                    reloadDetail(saved.id)
                }
        }
    }

    private fun validatedRequestLines(): List<BookingRequestLine>? {
        actionError = null
        if (selectedCustomerId.isBlank()) {
            actionError = "Select a customer."
            return null
        }
        val detailsError = detailsValidationError()
        if (detailsError != null) {
            actionError = detailsError
            return null
        }
        if (draftLines.isEmpty() || draftLines.any { it.itemId.isBlank() || it.quantity < 1 }) {
            actionError = "Select an item and quantity for every line."
            return null
        }
        val ids = draftLines.map { it.itemId }
        if (ids.toSet().size != ids.size) {
            actionError = "The same item cannot be added twice."
            return null
        }
        if (availabilityByItemState.isNotEmpty()) {
            val unavailable = draftLines.firstOrNull { it.quantity > availableQuantity(it.itemId) }
            if (unavailable != null) {
                val name = itemOptions.firstOrNull { it.id == unavailable.itemId }?.itemName ?: "Selected item"
                actionError = "$name: maximum available quantity is ${availableQuantity(unavailable.itemId)}."
                return null
            }
        }
        return draftLines.map { BookingRequestLine(it.itemId, it.quantity) }
    }

    private fun detailsValidationError(): String? {
        val pickup = runCatching { LocalDate.parse(pickupDate) }.getOrNull()
            ?: return "Select a valid pickup date."
        val returns = runCatching { LocalDate.parse(returnDate) }.getOrNull()
            ?: return "Select a valid return date."
        if (pickup.isBefore(businessToday())) return "Past pickup dates are not allowed."
        if (!returns.isAfter(pickup)) return "Return date must be after pickup date."
        return null
    }

    private fun clearAvailability() {
        availabilityByItemState = emptyMap()
        availabilityError = null
    }

    private fun ensureBootstrap(force: Boolean = false) {
        if (!force && (bootstrapState.loaded || bootstrapState.loading)) return
        if (bootstrapState.loading) return
        viewModelScope.launch {
            bootstrapState = bootstrapState.copy(loading = true, error = null)
            runCatching { repository.bookingBootstrap() }
                .onSuccess { data ->
                    bootstrapState = DataState(data = data, loaded = true)
                    customerOptions = data.customers
                    hydrateLineCategories()
                }
                .onFailure { bootstrapState = DataState(error = userMessage(it), loaded = true) }
        }
    }

    private fun hydrateLineCategories() {
        if (itemOptions.isEmpty()) return
        draftLines = draftLines.map { line ->
            if (line.categoryId.isNotBlank()) line
            else line.copy(categoryId = itemOptions.firstOrNull { it.id == line.itemId }?.categoryId.orEmpty())
        }
    }

    private fun searchCustomerOptions(search: String) {
        if (customerSearchLoading) {
            pendingCustomerSearch = search
            return
        }
        customerSearchLoading = true
        viewModelScope.launch {
            val requested = search
            runCatching { repository.customers(1, requested, 30) }
                .onSuccess { result ->
                    if (customerSearch.trim() == requested) {
                        customerOptions = result.items.map { customer -> customer.toBookingOption() }
                    }
                }
                .onFailure { actionError = userMessage(it) }
            customerSearchLoading = false
            val next = pendingCustomerSearch
            pendingCustomerSearch = null
            if (next != null && next != requested && customerSearch.trim() == next) searchCustomerOptions(next)
        }
    }

    private fun requestList(page: Int, search: String, status: String, view: String, sort: String) {
        val request = PendingBookingListLoad(
            page = page.coerceAtLeast(1),
            search = search.trim(),
            status = status.trim().uppercase(),
            view = view.trim().uppercase().ifBlank { "RESERVED" },
            sort = sort.trim().uppercase().ifBlank { "NEWEST" }
        )
        if (listState.loading) {
            pendingListLoad = request
            return
        }
        loadList(request)
    }

    private fun loadList(request: PendingBookingListLoad) {
        viewModelScope.launch {
            listState = listState.copy(loading = true, error = null, search = request.search)
            runCatching {
                repository.lifecycleBookings(
                    page = request.page,
                    search = request.search,
                    status = request.status,
                    view = request.view,
                    sort = request.sort
                )
            }.onSuccess { result ->
                if (
                    request.status == listStatus &&
                    request.view == listView &&
                    request.sort == listSort &&
                    request.search == listState.search
                ) {
                    listState = result.toPagedState(request.search)
                } else {
                    listState = listState.copy(loading = false)
                }
            }.onFailure {
                listState = listState.copy(loading = false, error = userMessage(it))
            }

            val next = pendingListLoad
            pendingListLoad = null
            if (next != null && next != request) {
                loadList(next)
            }
        }
    }

    private fun reloadDetail(id: String) {
        detailState = detailState.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.bookingDetail(id) }
                .onSuccess(::applyDetail)
                .onFailure { detailState = detailState.copy(loading = false, error = userMessage(it), loaded = true) }
        }
    }

    private fun applyDetail(detail: BookingDetailData) {
        detailState = DataState(data = detail, loaded = true)
        selectedCustomerId = detail.booking.customerId
        val existingCustomer = customerOptions.firstOrNull { it.id == detail.booking.customerId }
        if (existingCustomer == null) {
            customerOptions = listOf(
                BookingOptionCustomer(detail.booking.customerId, detail.booking.customerName, detail.booking.customerMobile)
            ) + customerOptions
        }
        pickupDateState = detail.booking.pickupDate
        returnDateState = detail.booking.returnDate
        notesState = detail.booking.notes.orEmpty()
        draftLines = detail.items.map { item ->
            BookingDraftLine(
                categoryId = itemOptions.firstOrNull { it.id == item.itemId }?.categoryId.orEmpty(),
                itemId = item.itemId,
                quantity = item.bookedQty
            )
        }
        pickupNow = detail.items.associate { it.bookingItemId to 0 }
        returnNow = detail.items.associate { it.bookingItemId to 0 }
        hydrateLineCategories()
    }

    private fun runAction(block: suspend () -> Unit) {
        if (actionBusy) return
        viewModelScope.launch {
            actionBusy = true
            actionError = null
            actionNotice = null
            try {
                block()
                mutationVersion += 1
            } catch (error: Throwable) {
                actionError = userMessage(error)
            }
            actionBusy = false
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        else -> error.message ?: "Unable to complete the request."
    }

    private fun businessToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))

    private fun requestKey(prefix: String) = "$prefix-${UUID.randomUUID()}"

    private fun Customer.toBookingOption() = BookingOptionCustomer(id, name, mobile)

    private fun <T> PageResult<T>.toPagedState(search: String) = PagedState(
        items = items,
        page = page,
        totalPages = totalPages,
        total = total,
        search = search,
        loaded = true
    )

    class Factory(private val repository: AdminRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BookingLifecycleViewModel(repository) as T
    }
}
