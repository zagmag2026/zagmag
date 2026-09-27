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
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingActionLine
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingBootstrapData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionCustomer
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingOptionItem
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingRequestLine
import com.nimsdeveloper.zhagmagdresses.admin.data.Customer
import com.nimsdeveloper.zhagmagdresses.admin.data.LifecycleBookingSummary
import com.nimsdeveloper.zhagmagdresses.admin.data.PageResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class BookingDraftLine(
    val categoryId: String = "",
    val itemId: String = "",
    val quantity: Int = 1
)

enum class CustomerSearchUiState { IDLE, SEARCHING, RESULTS, NO_RESULT, ERROR }

private data class PendingBookingListLoad(
    val page: Int,
    val search: String,
    val status: String,
    val view: String,
    val sort: String
)

class BookingLifecycleViewModel(
    private val repository: AdminRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var listState by mutableStateOf(
        PagedState<LifecycleBookingSummary>(
            page = savedStateHandle["bookingLifecyclePage"] ?: 1,
            search = savedStateHandle["bookingLifecycleSearch"] ?: ""
        )
    )
        private set
    private var listStatusState by mutableStateOf(savedStateHandle["bookingLifecycleStatus"] ?: "")
    val listStatus: String get() = listStatusState
    private var listViewState by mutableStateOf(savedStateHandle["bookingLifecycleView"] ?: "RESERVED")
    val listView: String get() = listViewState
    private var listSortState by mutableStateOf(savedStateHandle["bookingLifecycleSort"] ?: "NEWEST")
    val listSort: String get() = listSortState

    var editorOpen by mutableStateOf(savedStateHandle["bookingEditorOpen"] ?: false)
        private set
    var editorStartsInEditMode by mutableStateOf(savedStateHandle["bookingEditorEdit"] ?: false)
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

    var billingHandoffBookingId by mutableStateOf<String?>(null)
        private set
    var detailRefreshBlocked by mutableStateOf(false)
        private set
    /** Legacy compatibility marker; current Screen 4 freshness is domain-revision based. */
    var mutationVersion by mutableStateOf(0)
        private set

    private var customerSearchState by mutableStateOf(savedStateHandle["bookingCustomerSearch"] ?: "")
    val customerSearch: String get() = customerSearchState
    private var customerSearchCompletedState by mutableStateOf(savedStateHandle["bookingCustomerCompletedSearch"] ?: "")
    val customerSearchCompletedQuery: String get() = customerSearchCompletedState
    var customerOptions by mutableStateOf<List<BookingOptionCustomer>>(emptyList())
        private set
    var selectedCustomerId by mutableStateOf(savedStateHandle["bookingCustomerId"] ?: "")
        private set

    var newCustomerBusy by mutableStateOf(false)
        private set
    var newCustomerError by mutableStateOf<String?>(null)
        private set
    var customerSearchLoading by mutableStateOf(false)
        private set
    var customerSearchError by mutableStateOf<String?>(null)
        private set

    val customerSearchUiState: CustomerSearchUiState
        get() {
            val query = customerSearch.trim()
            if (query.isBlank()) return CustomerSearchUiState.IDLE
            if (customerSearchLoading || customerSearchCompletedQuery != query) return CustomerSearchUiState.SEARCHING
            if (!customerSearchError.isNullOrBlank()) return CustomerSearchUiState.ERROR
            val queryDigits = query.filter(Char::isDigit)
            val hasMatch = customerOptions.any { customer ->
                customer.name.contains(query, ignoreCase = true) ||
                    (queryDigits.isNotBlank() && customer.mobile.contains(queryDigits))
            }
            return if (hasMatch) CustomerSearchUiState.RESULTS else CustomerSearchUiState.NO_RESULT
        }

    private var pickupDateState by mutableStateOf(savedStateHandle["bookingPickupDate"] ?: "")
    val pickupDate: String get() = pickupDateState
    private var returnDateState by mutableStateOf(savedStateHandle["bookingReturnDate"] ?: "")
    val returnDate: String get() = returnDateState
    private var notesState by mutableStateOf(savedStateHandle["bookingNotes"] ?: "")
    val notes: String get() = notesState
    private var advanceAmountState by mutableStateOf(savedStateHandle["bookingAdvanceAmount"] ?: "0")
    val advanceAmount: String get() = advanceAmountState

    var availabilityLoading by mutableStateOf(false)
        private set
    var availabilityError by mutableStateOf<String?>(null)
        private set
    private var availabilityByItemState by mutableStateOf<Map<String, Int>>(emptyMap())
    val availabilityByItem: Map<String, Int> get() = availabilityByItemState

    private var pickupNotesState by mutableStateOf(savedStateHandle["bookingPickupNotes"] ?: "")
    val pickupNotes: String get() = pickupNotesState
    private var returnNotesState by mutableStateOf(savedStateHandle["bookingReturnNotes"] ?: "")
    val returnNotes: String get() = returnNotesState

    var draftLines by mutableStateOf<List<BookingDraftLine>>(
        savedStateHandle.get<String>("bookingDraftLines")
            ?.let(::decodeDraftLines)
            .orEmpty()
    )
        private set
    private var relatedParentByItemState by mutableStateOf<Map<String, String>>(emptyMap())
    val relatedParentByItem: Map<String, String> get() = relatedParentByItemState
    var pickupNow by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var returnNow by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var pendingListLoad: PendingBookingListLoad? = null
    private var detailRequestId: String? = savedStateHandle["bookingDetailId"]
    private var customerSearchJob: Job? = null
    private var customerSearchRequestSerial = 0
    private var seenBootstrapFreshnessRevision = AdminDataFreshness.bookingBootstrapRevision
    private val stableMutationKeys = mutableMapOf<String, Pair<String, String>>()
    private var editorBaselineSignature by mutableStateOf(savedStateHandle["bookingEditorBaseline"] ?: "")

    init {
        if (editorOpen) {
            ensureBootstrap()
            val detailId = detailRequestId
            if (!detailId.isNullOrBlank()) {
                detailState = DataState(loading = true)
                viewModelScope.launch {
                    runCatching { repository.bookingDetail(detailId) }
                        .onSuccess { applyDetail(it) }
                        .onFailure { detailState = DataState(error = userMessage(it), loaded = true) }
                }
            }
        }
    }

    private fun encodeDraftLines(lines: List<BookingDraftLine>): String =
        JSONArray().apply {
            lines.forEach { line ->
                put(JSONObject()
                    .put("categoryId", line.categoryId)
                    .put("itemId", line.itemId)
                    .put("quantity", line.quantity))
            }
        }.toString()

    private fun decodeDraftLines(raw: String): List<BookingDraftLine> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                add(BookingDraftLine(
                    categoryId = row.optString("categoryId"),
                    itemId = row.optString("itemId"),
                    quantity = row.optInt("quantity", 1).coerceAtLeast(1)
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun persistListState() {
        savedStateHandle["bookingLifecyclePage"] = listState.page
        savedStateHandle["bookingLifecycleSearch"] = listState.search
        savedStateHandle["bookingLifecycleStatus"] = listStatusState
        savedStateHandle["bookingLifecycleView"] = listViewState
        savedStateHandle["bookingLifecycleSort"] = listSortState
    }

    private fun persistEditorDraft() {
        savedStateHandle["bookingEditorOpen"] = editorOpen
        savedStateHandle["bookingEditorEdit"] = editorStartsInEditMode
        savedStateHandle["bookingDetailId"] = detailRequestId
        savedStateHandle["bookingCustomerSearch"] = customerSearchState
        savedStateHandle["bookingCustomerCompletedSearch"] = customerSearchCompletedState
        savedStateHandle["bookingCustomerId"] = selectedCustomerId
        savedStateHandle["bookingPickupDate"] = pickupDateState
        savedStateHandle["bookingReturnDate"] = returnDateState
        savedStateHandle["bookingNotes"] = notesState
        savedStateHandle["bookingAdvanceAmount"] = advanceAmountState
        savedStateHandle["bookingPickupNotes"] = pickupNotesState
        savedStateHandle["bookingReturnNotes"] = returnNotesState
        savedStateHandle["bookingDraftLines"] = encodeDraftLines(draftLines)
    }

    private fun clearPersistedEditor() {
        listOf(
            "bookingEditorOpen","bookingEditorEdit","bookingDetailId","bookingCustomerSearch",
            "bookingCustomerCompletedSearch","bookingCustomerId","bookingPickupDate","bookingReturnDate","bookingNotes",
            "bookingAdvanceAmount","bookingPickupNotes","bookingReturnNotes","bookingDraftLines","bookingEditorBaseline"
        ).forEach { key -> savedStateHandle.remove<Any>(key) }
        editorBaselineSignature = ""
    }

    private fun currentEditorSignature(): String = buildString {
        append(selectedCustomerId).append('|')
        append(pickupDateState).append('|')
        append(returnDateState).append('|')
        append(notesState).append('|')
        append(advanceAmountState).append('|')
        append(draftLines.joinToString(",") { "${it.itemId}:${it.quantity}" })
    }

    private fun captureEditorBaseline() {
        editorBaselineSignature = currentEditorSignature()
        savedStateHandle["bookingEditorBaseline"] = editorBaselineSignature
    }

    fun hasUnsavedEditorChanges(): Boolean {
        val editable = editorStartsInEditMode || detailRequestId == null
        return editorOpen && editable && editorBaselineSignature.isNotBlank() &&
            currentEditorSignature() != editorBaselineSignature
    }

    val itemOptions: List<BookingOptionItem>
        get() = bootstrapState.data?.items.orEmpty()

    val selectedCustomer: BookingOptionCustomer?
        get() = customerOptions.firstOrNull { it.id == selectedCustomerId }
            ?: bootstrapState.data?.customers?.firstOrNull { it.id == selectedCustomerId }

    fun ensureList() {
        if (!listState.loaded && !listState.loading) {
            requestList(listState.page.coerceAtLeast(1), listState.search, listStatus, listView, listSort)
        }
    }

    fun searchBookings(value: String) {
        val normalized = value.trim()
        if (listState.loaded && listState.search == normalized && !listState.loading) return
        savedStateHandle["bookingLifecycleSearch"] = normalized
        savedStateHandle["bookingLifecyclePage"] = 1
        requestList(1, normalized, listStatus, listView, listSort)
    }

    fun setListView(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("RESERVED", "BOOKED", "PART_PICKUP", "FULL_PICKUP", "PART_RETURN", "FULL_RETURN", "OVERDUE", "CANCELLED", "ALL")) return
        if (normalized == listView && listState.loaded) return
        listViewState = normalized
        listStatusState = ""
        savedStateHandle["bookingLifecyclePage"] = 1
        persistListState()
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun setListStatus(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("", "RESERVED", "BOOKED", "PARTIALLY_GIVEN", "GIVEN", "PARTIALLY_RETURNED", "RETURNED", "CANCELLED")) return
        if (normalized == listStatus && listState.loaded) return
        listStatusState = normalized
        savedStateHandle["bookingLifecyclePage"] = 1
        persistListState()
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun setListSort(value: String) {
        val normalized = value.trim().uppercase()
        if (normalized !in setOf("NEWEST", "OLDEST", "PICKUP_ASC", "PICKUP_DESC", "RETURN_ASC", "RETURN_DESC")) return
        if (normalized == listSort && listState.loaded) return
        listSortState = normalized
        savedStateHandle["bookingLifecyclePage"] = 1
        persistListState()
        requestList(1, listState.search, listStatus, listView, listSort)
    }

    fun bookingPage(page: Int) {
        savedStateHandle["bookingLifecyclePage"] = page.coerceAtLeast(1)
        requestList(page, listState.search, listStatus, listView, listSort)
    }
    fun refreshList() = requestList(listState.page, listState.search, listStatus, listView, listSort)

    fun openNew(preselectedCustomer: BookingOptionCustomer? = null) {
        ensureBootstrap()
        editorOpen = true
        editorStartsInEditMode = false
        detailRequestId = null
        detailState = DataState()
        actionError = null
        actionNotice = null
        selectedCustomerId = preselectedCustomer?.id.orEmpty()
        customerSearchJob?.cancel()
        customerSearchRequestSerial += 1
        customerSearchState = ""
        customerSearchCompletedState = ""
        customerSearchLoading = false
        val baseCustomers = bootstrapState.data?.customers ?: customerOptions
        customerOptions = if (preselectedCustomer == null) {
            baseCustomers
        } else {
            listOf(preselectedCustomer) + baseCustomers.filterNot { it.id == preselectedCustomer.id }
        }
        newCustomerError = null
        customerSearchError = null
        val today = businessToday()
        pickupDateState = today.toString()
        returnDateState = today.plusDays(1).toString()
        notesState = ""
        advanceAmountState = "0"
        availabilityByItemState = emptyMap()
        availabilityError = null
        pickupNotesState = ""
        returnNotesState = ""
        draftLines = emptyList()
        relatedParentByItemState = emptyMap()
        pickupNow = emptyMap()
        returnNow = emptyMap()
        persistEditorDraft()
        captureEditorBaseline()
    }

    fun openBooking(id: String, edit: Boolean = false) {
        ensureBootstrap()
        editorOpen = true
        editorStartsInEditMode = edit
        detailRequestId = id
        actionError = null
        actionNotice = null
        detailRefreshBlocked = false
        availabilityError = null
        availabilityByItemState = emptyMap()
        pickupNotesState = ""
        returnNotesState = ""
        detailState = DataState(loading = true)
        persistEditorDraft()
        viewModelScope.launch {
            runCatching { repository.bookingDetail(id) }
                .onSuccess { detail -> applyDetail(detail) }
                .onFailure { detailState = DataState(error = userMessage(it), loaded = true) }
        }
    }

    fun refreshEditor() {
        val id = detailState.data?.booking?.id ?: detailRequestId
        if (!id.isNullOrBlank()) reloadDetail(id) else ensureBootstrap(force = true)
    }

    fun clearActionFeedback() {
        actionError = null
        actionNotice = null
    }

    fun backFromEditor() {
        if (actionBusy || newCustomerBusy || availabilityLoading) return
        editorOpen = false
        editorStartsInEditMode = false
        detailRequestId = null
        actionError = null
        actionNotice = null
        newCustomerError = null
        customerSearchJob?.cancel()
        customerSearchRequestSerial += 1
        customerSearchLoading = false
        customerSearchError = null
        availabilityError = null
        pickupNotesState = ""
        returnNotesState = ""
        clearPersistedEditor()
        // Current Screen 4 uses BookingListViewModel4. Its domain revision performs one targeted
        // refresh on re-entry, so do not issue the retired second booking-list request here.
    }

    fun setSelectedCustomer(id: String) {
        selectedCustomerId = id
        persistEditorDraft()
        actionError = null
    }

    fun clearSelectedCustomer() {
        selectedCustomerId = ""
        setCustomerSearch("")
        actionError = null
    }

    fun setPickupDate(value: String) {
        pickupDateState = value.trim()
        val pickup = runCatching { LocalDate.parse(pickupDateState) }.getOrNull()
        val returns = runCatching { LocalDate.parse(returnDateState) }.getOrNull()
        if (pickup != null && (returns == null || !returns.isAfter(pickup))) {
            returnDateState = pickup.plusDays(1).toString()
        }
        persistEditorDraft()
        clearAvailability()
    }

    fun setReturnDate(value: String) {
        returnDateState = value.trim()
        persistEditorDraft()
        clearAvailability()
    }

    fun setNotes(value: String) { notesState = value.take(500); persistEditorDraft() }
    fun setAdvanceAmount(value: String) {
        advanceAmountState = value.filter(Char::isDigit).take(9)
        persistEditorDraft()
    }
    fun setPickupNotes(value: String) { pickupNotesState = value.take(500); persistEditorDraft() }
    fun setReturnNotes(value: String) { returnNotesState = value.take(500); persistEditorDraft() }

    fun clearNewCustomerFeedback() {
        newCustomerError = null
    }

    fun setCustomerSearch(value: String, allowRemoteSearch: Boolean = true) {
        val nextValue = value.take(120)
        customerSearchState = nextValue
        actionError = null
        customerSearchError = null
        customerSearchJob?.cancel()
        customerSearchRequestSerial += 1
        val requestSerial = customerSearchRequestSerial
        val normalized = nextValue.trim()
        val selected = selectedCustomerOption()
        val base = bootstrapState.data?.customers.orEmpty()

        when {
            normalized.isEmpty() -> {
                customerSearchLoading = false
                customerSearchCompletedState = normalized
                customerOptions = if (selected == null) base else listOf(selected) + base.filterNot { it.id == selected.id }
            }
            !allowRemoteSearch -> {
                customerSearchLoading = false
                val queryDigits = normalized.filter(Char::isDigit)
                val filtered = base.filter {
                    it.name.contains(normalized, true) ||
                        (queryDigits.isNotBlank() && it.mobile.contains(queryDigits))
                }
                customerSearchCompletedState = normalized
                customerOptions = if (selected == null) filtered else listOf(selected) + filtered.filterNot { it.id == selected.id }
            }
            normalized.length >= 2 -> {
                customerSearchLoading = true
                customerOptions = listOfNotNull(selected)
                customerSearchJob = viewModelScope.launch {
                    delay(300)
                    searchCustomerOptions(
                        requested = normalized,
                        requestSerial = requestSerial,
                        selected = selected
                    )
                }
            }
            else -> {
                customerSearchLoading = false
                customerSearchCompletedState = normalized
                customerOptions = listOfNotNull(selected)
            }
        }
        persistEditorDraft()
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

        newCustomerBusy = true
        newCustomerError = null
        viewModelScope.launch {
            try {
                val result = repository.createBookingCustomer(cleanName, primary, alternate, address)
                val id = result.id
                if (id.isNullOrBlank()) {
                    newCustomerError = "Customer was saved but could not be selected."
                } else {
                    selectBookingCustomer(
                        BookingOptionCustomer(id, cleanName, primary)
                    )
                    AdminDataFreshness.markCustomerMutation()
                    onSuccess()
                }
            } catch (error: Throwable) {
                val duplicateMobile = error is ApiException && error.apiCode == "DUPLICATE_MOBILE"
                if (duplicateMobile) {
                    val existing = runCatching {
                        repository.customers(1, primary, 30).items
                            .firstOrNull { it.mobile.filter(Char::isDigit) == primary }
                            ?.toBookingOption()
                    }.getOrNull()
                    if (existing != null) {
                        selectBookingCustomer(existing)
                        newCustomerError = null
                        onSuccess()
                    } else {
                        newCustomerError = "This mobile number already belongs to a customer. Search again and select the existing customer."
                    }
                } else {
                    newCustomerError = userMessage(error)
                }
            } finally {
                newCustomerBusy = false
            }
        }
    }

    private fun selectBookingCustomer(customer: BookingOptionCustomer) {
        customerOptions = listOf(customer) + customerOptions.filterNot { it.id == customer.id }
        selectedCustomerId = customer.id
        persistEditorDraft()
        actionError = null
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
        persistEditorDraft()
        return true
    }

    fun relatedItemsFor(sourceItemId: String): List<BookingOptionItem> {
        val data = bootstrapState.data ?: return emptyList()
        val byId = itemOptions.associateBy { it.id }
        return data.relatedItems
            .asSequence()
            .filter { it.sourceItemId == sourceItemId && it.relatedItemId != sourceItemId }
            .sortedWith(compareBy({ it.displayOrder }, { it.relatedItemId }))
            .mapNotNull { byId[it.relatedItemId] }
            .distinctBy { it.id }
            .toList()
    }

    fun addRelatedItem(sourceItemId: String, relatedItemId: String): Boolean {
        if (sourceItemId == relatedItemId) return false
        val allowed = bootstrapState.data?.relatedItems.orEmpty().any {
            it.sourceItemId == sourceItemId && it.relatedItemId == relatedItemId
        }
        if (!allowed) {
            actionError = "This Related Item is no longer configured."
            return false
        }
        val added = addOrUpdateItem(relatedItemId, 1)
        if (added) {
            relatedParentByItemState = relatedParentByItemState + (relatedItemId to sourceItemId)
        }
        return added
    }

    fun relatedParentId(itemId: String): String? =
        relatedParentByItemState[itemId]?.takeIf { parentId ->
            draftLines.any { it.itemId == parentId }
        }

    fun removeItem(itemId: String) {
        draftLines = draftLines.filterNot { it.itemId == itemId }
        relatedParentByItemState = relatedParentByItemState
            .filterKeys { it != itemId }
            .filterValues { it != itemId }
        actionError = null
        persistEditorDraft()
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
        persistEditorDraft()
    }

    fun removeLine(index: Int) {
        if (index !in draftLines.indices) return
        draftLines = draftLines.filterIndexed { lineIndex, _ -> lineIndex != index }
        persistEditorDraft()
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
        persistEditorDraft()
    }

    fun setLineItem(index: Int, itemId: String) {
        if (index !in draftLines.indices) return
        val item = itemOptions.firstOrNull { it.id == itemId }
        draftLines = draftLines.mapIndexed { lineIndex, line ->
            if (lineIndex == index) line.copy(itemId = itemId, categoryId = item?.categoryId ?: line.categoryId) else line
        }
        persistEditorDraft()
    }

    fun setLineQuantity(index: Int, quantity: Int) {
        if (index !in draftLines.indices) return
        val itemId = draftLines[index].itemId
        val max = if (itemId.isBlank()) 100000 else availableQuantity(itemId).coerceAtLeast(1)
        val safe = quantity.coerceIn(1, max)
        draftLines = draftLines.mapIndexed { lineIndex, line -> if (lineIndex == index) line.copy(quantity = safe) else line }
        persistEditorDraft()
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
                advanceAmount = advanceAmount.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                lines = lines,
                expectedUpdatedAt = detail.booking.updatedAt
            )
            AdminDataFreshness.markBookingMutation()
            editorStartsInEditMode = false
            reloadDetailAwaited(detail.booking.id)
            actionNotice = "Booking updated."
        }
    }

    fun confirmReserved() {
        val id = detailState.data?.booking?.id ?: return
        runAction {
            val message = repository.confirmBooking(id).message
            AdminDataFreshness.markBookingMutation()
            reloadDetailAwaited(id)
            actionNotice = message
        }
    }

    fun confirmReservedForPickup(onReady: () -> Unit) {
        val id = detailState.data?.booking?.id ?: return
        runAction {
            repository.confirmBooking(id)
            AdminDataFreshness.markBookingMutation()
            reloadDetailAwaited(id)
            actionNotice = "Booking confirmed. Select pickup quantities."
            onReady()
        }
    }

    fun cancelBooking(advanceSettlementStatus: String? = null, advanceRefundAmount: Int = 0) {
        val id = detailState.data?.booking?.id ?: return
        runAction {
            val message = repository.cancelBooking(id, advanceSettlementStatus, advanceRefundAmount).message
            AdminDataFreshness.markBookingMutation()
            reloadDetailAwaited(id)
            actionNotice = message
        }
    }

    fun consumeBillingHandoff() {
        billingHandoffBookingId = null
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
            val pickupSignature = buildString {
                append(detail.booking.id).append('|')
                append(lines.joinToString(",") { "${it.bookingItemId}:${it.quantity}" }).append('|')
                append(pickupNotes)
            }
            val message = repository.savePickup(
                detail.booking.id,
                lines,
                requestKey("pickup", pickupSignature),
                pickupNotes
            ).message
            clearRequestKey("pickup")
            AdminDataFreshness.markBookingMutation()
            pickupNotesState = ""
            reloadDetailAwaited(detail.booking.id)
            actionNotice = message
        }
    }

    fun saveReturn(pendingPickupAction: String? = null) {
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
            val returnSignature = buildString {
                append(detail.booking.id).append('|')
                append(lines.joinToString(",") { "${it.bookingItemId}:${it.quantity}" }).append('|')
                append(returnNotes).append('|').append(pendingPickupAction.orEmpty())
            }
            val result = repository.saveReturn(
                bookingId = detail.booking.id,
                lines = lines,
                requestKey = requestKey("return", returnSignature),
                notes = returnNotes,
                pendingPickupAction = pendingPickupAction
            )
            clearRequestKey("return")
            AdminDataFreshness.markBookingMutation()
            returnNotesState = ""
            reloadDetailAwaited(detail.booking.id)
            billingHandoffBookingId = result.billingHandoffBookingId
            actionNotice = if (!result.billingDraftError.isNullOrBlank()) {
                result.message + " Bill Draft could not be prepared: " + result.billingDraftError
            } else {
                result.message
            }
        }
    }

    private fun createBooking(confirmationState: String, directPickup: Boolean) {
        val lines = validatedRequestLines() ?: return
        runAction {
            val bookingSignature = buildString {
                append(selectedCustomerId).append('|').append(pickupDate).append('|').append(returnDate).append('|')
                append(notes).append('|').append(advanceAmount).append('|').append(confirmationState).append('|')
                append(lines.joinToString(",") { "${it.itemId}:${it.quantity}" })
            }
            val saved = repository.createBooking(
                customerId = selectedCustomerId,
                pickupDate = pickupDate,
                returnDate = returnDate,
                notes = notes,
                advanceAmount = advanceAmount.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                lines = lines,
                confirmationState = confirmationState,
                requestKey = requestKey("booking", bookingSignature)
            )
            clearRequestKey("booking")
            if (!directPickup) {
                AdminDataFreshness.markBookingMutation()
                reloadDetailAwaited(saved.id)
                actionNotice = saved.message
                return@runAction
            }

            try {
                val detail = repository.bookingDetail(saved.id)
                val giveAll = detail.items.mapNotNull { item ->
                    item.remainingToGive.takeIf { it > 0 }?.let { BookingActionLine(item.bookingItemId, it) }
                }
                if (giveAll.isEmpty()) {
                    applyDetail(detail)
                    actionNotice = saved.message
                    return@runAction
                }

                try {
                    val pickupSignature = saved.id + "|" + giveAll.joinToString(",") { "${it.bookingItemId}:${it.quantity}" }
                    repository.savePickup(saved.id, giveAll, requestKey("pickup", pickupSignature))
                    clearRequestKey("pickup")
                    reloadDetailAwaited(saved.id)
                    actionNotice = "Direct pickup saved."
                } catch (error: Throwable) {
                    reloadDetailAwaited(saved.id)
                    actionError = "Booking saved. Pickup failed: ${userMessage(error)}"
                }
            } finally {
                AdminDataFreshness.markBookingMutation()
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
        val stale = seenBootstrapFreshnessRevision != AdminDataFreshness.bookingBootstrapRevision
        if (!force && bootstrapState.loaded && !stale) return
        if (bootstrapState.loading) return
        viewModelScope.launch {
            bootstrapState = bootstrapState.copy(loading = true, error = null)
            runCatching { repository.bookingBootstrap() }
                .onSuccess { data ->
                    val selected = customerOptions.firstOrNull { it.id == selectedCustomerId }
                    seenBootstrapFreshnessRevision = AdminDataFreshness.bookingBootstrapRevision
                    bootstrapState = DataState(data = data, loaded = true)
                    customerOptions = if (selected == null) {
                        data.customers
                    } else {
                        listOf(selected) + data.customers.filterNot { it.id == selected.id }
                    }
                    customerSearchError = null
                    if (customerSearchState.trim().length >= 2) {
                        setCustomerSearch(customerSearchState)
                    } else {
                        customerSearchCompletedState = customerSearchState.trim()
                    }
                    hydrateLineCategories()
                    hydrateRelatedAssociations()
                }
                .onFailure { bootstrapState = bootstrapState.copy(loading = false, error = userMessage(it), loaded = bootstrapState.data != null) }
        }
    }

    private fun hydrateLineCategories() {
        if (itemOptions.isEmpty()) return
        draftLines = draftLines.map { line ->
            if (line.categoryId.isNotBlank()) line
            else line.copy(categoryId = itemOptions.firstOrNull { it.id == line.itemId }?.categoryId.orEmpty())
        }
    }

    private fun hydrateRelatedAssociations() {
        if (draftLines.size < 2 || relatedParentByItemState.isNotEmpty()) return
        val selectedIds = draftLines.map { it.itemId }
        val selectedSet = selectedIds.toSet()
        val links = bootstrapState.data?.relatedItems.orEmpty()
        val inferred = mutableMapOf<String, String>()
        selectedIds.forEachIndexed { childIndex, childId ->
            val parent = selectedIds
                .take(childIndex)
                .firstOrNull { sourceId ->
                    sourceId in selectedSet && links.any { it.sourceItemId == sourceId && it.relatedItemId == childId }
                }
            if (parent != null) inferred[childId] = parent
        }
        relatedParentByItemState = inferred
    }

    private fun selectedCustomerOption(): BookingOptionCustomer? =
        customerOptions.firstOrNull { it.id == selectedCustomerId }
            ?: bootstrapState.data?.customers?.firstOrNull { it.id == selectedCustomerId }

    private suspend fun searchCustomerOptions(
        requested: String,
        requestSerial: Int,
        selected: BookingOptionCustomer?
    ) {
        runCatching { repository.customers(1, requested, 30) }
            .onSuccess { result ->
                if (requestSerial == customerSearchRequestSerial && customerSearch.trim() == requested) {
                    val searched = result.items.map { customer -> customer.toBookingOption() }
                    customerOptions = if (selected == null) searched else {
                        listOf(selected) + searched.filterNot { it.id == selected.id }
                    }
                    customerSearchError = null
                    customerSearchCompletedState = requested
                }
            }
            .onFailure {
                if (requestSerial == customerSearchRequestSerial && customerSearch.trim() == requested) {
                    customerOptions = listOfNotNull(selected)
                    customerSearchError = userMessage(it)
                    customerSearchCompletedState = requested
                }
            }
        if (requestSerial == customerSearchRequestSerial) {
            customerSearchLoading = false
            persistEditorDraft()
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
                    persistListState()
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

    private suspend fun reloadDetailAwaited(id: String) {
        detailState = detailState.copy(loading = true, error = null)
        try {
            val detail = repository.bookingDetail(id)
            applyDetail(detail)
        } catch (error: Throwable) {
            detailRefreshBlocked = true
            detailState = detailState.copy(
                loading = false,
                error = "Latest booking details could not be loaded. Reopen or refresh before another booking action.",
                loaded = true
            )
            throw error
        }
    }

    private fun reloadDetail(id: String) {
        if (detailState.loading) return
        detailState = detailState.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.bookingDetail(id) }
                .onSuccess(::applyDetail)
                .onFailure {
                    detailState = detailState.copy(
                        loading = false,
                        error = userMessage(it),
                        loaded = true
                    )
                }
        }
    }

    private fun applyDetail(detail: BookingDetailData) {
        detailRequestId = detail.booking.id
        detailRefreshBlocked = false
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
        advanceAmountState = detail.booking.advanceAmount.coerceAtLeast(0).toString()
        draftLines = detail.items.map { item ->
            BookingDraftLine(
                categoryId = itemOptions.firstOrNull { it.id == item.itemId }?.categoryId.orEmpty(),
                itemId = item.itemId,
                quantity = item.bookedQty
            )
        }
        pickupNow = detail.items.associate { it.bookingItemId to 0 }
        returnNow = detail.items.associate { it.bookingItemId to 0 }
        relatedParentByItemState = emptyMap()
        hydrateLineCategories()
        hydrateRelatedAssociations()
        persistEditorDraft()
        captureEditorBaseline()
    }

    private fun runAction(block: suspend () -> Unit) {
        if (actionBusy || detailRefreshBlocked) return
        actionBusy = true
        actionError = null
        actionNotice = null
        viewModelScope.launch {
            try {
                block()
            } catch (error: Throwable) {
                actionError = userMessage(error)
            } finally {
                actionBusy = false
            }
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    private fun businessToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))

    private fun requestKey(prefix: String, signature: String): String {
        val existing = stableMutationKeys[prefix]
        if (existing?.first == signature) return existing.second
        val key = "$prefix-${UUID.randomUUID()}"
        stableMutationKeys[prefix] = signature to key
        return key
    }

    private fun clearRequestKey(prefix: String) {
        stableMutationKeys.remove(prefix)
    }

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
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            BookingLifecycleViewModel(repository, extras.createSavedStateHandle()) as T
    }
}
