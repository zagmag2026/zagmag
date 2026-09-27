package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONArray
import org.json.JSONObject

enum class UserRole { OWNER, STAFF }

object StaffAccess {
    const val DASHBOARD = "DASHBOARD"
    const val ITEMS = "ITEMS"
    const val CUSTOMERS = "CUSTOMERS"
    const val BOOKINGS = "BOOKINGS"
    const val PICKUPS = "PICKUPS"
    const val RETURNS = "RETURNS"
    const val REPORTS = "REPORTS"

    val operationalDefaults = listOf(DASHBOARD, ITEMS, CUSTOMERS, BOOKINGS, PICKUPS, RETURNS, REPORTS)
}

data class SessionUser(
    val id: String,
    val name: String,
    val email: String?,
    val mobile: String?,
    val role: UserRole,
    val staffPermissions: Set<String> = emptySet()
)

fun SessionUser.hasAccess(permission: String): Boolean =
    role == UserRole.OWNER || staffPermissions.contains(permission.uppercase())

data class DashboardSummary(
    val totalCategories: Int = 0,
    val totalItems: Int = 0,
    val totalQuantity: Int = 0,
    val availableQuantity: Int = 0,
    val bookedQuantity: Int = 0,
    val missedPickups: Int = 0,
    val givenQuantity: Int = 0,
    val overdueReturns: Int = 0
)

data class DashboardSectionCounts(
    val missedPickups: Int = 0,
    val todayBookings: Int = 0,
    val todayPickups: Int = 0,
    val todayReturns: Int = 0,
    val overdueReturns: Int = 0
)

data class DashboardItem(
    val itemId: String,
    val itemName: String,
    val itemCode: String,
    val categoryName: String,
    val imageUrl: String?,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val pendingPickupQty: Int,
    val pendingReturnQty: Int,
    val imageUrls: List<String> = emptyList()
)

data class DashboardEntry(
    val id: String,
    val customerId: String,
    val bookingNo: String,
    val bookingDate: String?,
    val pickupDate: String,
    val returnDate: String,
    val status: String,
    val displayStatus: String,
    val paymentStatus: String? = null,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String = "",
    val itemsSummary: String,
    val items: List<DashboardItem>,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val pendingQty: Int,
    val remainingQty: Int,
    val overdueDays: Int,
    val missedDays: Int
)

data class DashboardTodayKpis(
    val todayPickups: Int = 0,
    val todayReturns: Int = 0,
    val missedPickups: Int = 0,
    val overdueReturns: Int = 0
)

data class DashboardBookingKpis(
    val reserved: Int = 0,
    val booked: Int = 0,
    val partPickedUp: Int = 0,
    val fullPickedUp: Int = 0,
    val partReturn: Int = 0,
    val fullReturned: Int = 0,
    val cancelled: Int = 0,
    val activeRentalOrders: Int = 0
)

data class DashboardPaymentBillingKpis(
    val pendingPayment: Int = 0,
    val partPayment: Int = 0,
    val fullPayment: Int = 0,
    val draftBills: Int = 0,
    val finalBills: Int = 0,
    val billsToday: Int = 0,
    val pendingBalance: Int = 0,
    val totalReceived: Int = 0
)

data class DashboardInventoryKpis(
    val availableNow: Int = 0,
    val pickupPendingQty: Int = 0,
    val currentlyOutQty: Int = 0,
    val totalQuantity: Int = 0,
    val totalItems: Int = 0,
    val categories: Int = 0,
    val lowStock: Int = 0,
    val unavailable: Int = 0
)

data class DashboardCustomerKpis(
    val totalCustomers: Int = 0,
    val newCustomers: Int = 0,
    val returningCustomers: Int = 0,
    val frequentCustomers: Int = 0,
    val activeRentalCustomers: Int = 0,
    val customerExceptions: Int = 0
)

data class DashboardKpis(
    val todayOverview: DashboardTodayKpis = DashboardTodayKpis(),
    val bookingStatus: DashboardBookingKpis = DashboardBookingKpis(),
    val paymentBilling: DashboardPaymentBillingKpis = DashboardPaymentBillingKpis(),
    val inventory: DashboardInventoryKpis = DashboardInventoryKpis(),
    val customers: DashboardCustomerKpis = DashboardCustomerKpis()
)

data class DashboardCategoryInventory(
    val categoryId: String,
    val categoryName: String,
    val items: Int,
    val totalQty: Int,
    val available: Int,
    val pickupPending: Int,
    val currentlyOut: Int
)

data class DashboardAccess(
    val bookings: Boolean = false,
    val billing: Boolean = false,
    val items: Boolean = false,
    val customers: Boolean = false,
    val reports: Boolean = false
)

data class DashboardPayload(
    val today: String,
    val kpis: DashboardKpis = DashboardKpis(),
    val categoryInventory: List<DashboardCategoryInventory> = emptyList(),
    val access: DashboardAccess = DashboardAccess(),
    val summary: DashboardSummary,
    val sectionCounts: DashboardSectionCounts,
    val missedPickups: List<DashboardEntry>,
    val todayBookings: List<DashboardEntry>,
    val todayPickups: List<DashboardEntry>,
    val todayReturns: List<DashboardEntry>,
    val overdueReturns: List<DashboardEntry>
)

data class Customer(
    val id: String,
    val name: String,
    val mobile: String,
    val alternateMobile: String?,
    val address: String?,
    val notes: String?,
    val isActive: Boolean,
    val archivedAt: String?,
    val totalBookings: Int,
    val activeBookings: Int,
    val lastBookingDate: String?
)

data class BookingRow(
    val id: String,
    val bookingNo: String,
    val bookingDate: String,
    val pickupDate: String,
    val returnDate: String,
    val status: String,
    val notes: String?,
    val customerId: String,
    val customerName: String,
    val customerMobile: String,
    val itemsSummary: String,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int
)

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
    val total: Int
)

internal fun JSONObject.stringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

internal fun JSONObject.boolFlexible(key: String, fallback: Boolean = false): Boolean {
    if (!has(key) || isNull(key)) return fallback
    return when (val value = opt(key)) {
        is Boolean -> value
        is Number -> value.toInt() == 1
        is String -> value.equals("true", true) || value == "1"
        else -> fallback
    }
}

internal fun JSONObject.intFlexible(key: String): Int = when (val value = opt(key)) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull() ?: 0
    else -> 0
}

internal fun JSONArray.objects(): List<JSONObject> =
    buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(::add)
        }
    }

fun JSONObject.toSessionUser(): SessionUser {
    val role = if (optString("role").equals("OWNER", true)) UserRole.OWNER else UserRole.STAFF
    val permissionSet = buildSet {
        val array = optJSONArray("staffPermissions")
        if (array != null) {
            for (index in 0 until array.length()) {
                array.optString(index).trim().uppercase().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
    return SessionUser(
        id = optString("id"),
        name = optString("name"),
        email = stringOrNull("email"),
        mobile = stringOrNull("mobile"),
        role = role,
        staffPermissions = if (role == UserRole.OWNER) emptySet() else permissionSet
    )
}

private fun itemNameWithSecondary(name: String, code: String, category: String): String {
    val secondary = listOf(code.trim(), category.trim()).filter { it.isNotBlank() }.joinToString(" · ")
    return if (secondary.isBlank()) name else "$name\n$secondary"
}

private fun JSONObject.dashboardItemImageUrls(): List<String> {
    val array = optJSONArray("image_urls")
        ?: stringOrNull("images_json")?.let { raw -> runCatching { JSONArray(raw) }.getOrNull() }
    val primary = stringOrNull("image_url")
    return buildList {
        if (array != null) {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf(String::isNotBlank)?.let { if (it !in this) add(it) }
            }
        }
        if (!primary.isNullOrBlank() && primary !in this) add(0, primary)
    }
}

private fun JSONObject.toDashboardItem() = DashboardItem(
    itemId = optString("item_id"),
    itemName = optString("item_name"),
    itemCode = optString("item_code"),
    categoryName = optString("category_name"),
    imageUrl = stringOrNull("image_url"),
    imageUrls = dashboardItemImageUrls(),
    bookedQty = intFlexible("booked_qty"),
    givenQty = intFlexible("given_qty"),
    returnedQty = intFlexible("returned_qty"),
    pendingPickupQty = intFlexible("pending_pickup_qty"),
    pendingReturnQty = intFlexible("pending_return_qty")
)

private fun JSONObject.toDashboardEntry() = DashboardEntry(
    id = optString("id"),
    customerId = optString("customer_id"),
    bookingNo = optString("booking_no"),
    bookingDate = stringOrNull("booking_date"),
    pickupDate = optString("pickup_date"),
    returnDate = optString("return_date"),
    status = optString("status"),
    displayStatus = optString("display_status").ifBlank { optString("status") },
    paymentStatus = stringOrNull("payment_status"),
    customerName = optString("customer_name"),
    customerMobile = optString("customer_mobile"),
    customerAddress = optString("customer_address"),
    itemsSummary = optString("items_summary"),
    items = optJSONArray("items")?.objects()?.map { it.toDashboardItem() }.orEmpty(),
    bookedQty = intFlexible("booked_qty"),
    givenQty = intFlexible("given_qty"),
    returnedQty = intFlexible("returned_qty"),
    pendingQty = intFlexible("pending_qty"),
    remainingQty = if (has("remaining_qty")) intFlexible("remaining_qty") else intFlexible("remaining_to_give"),
    overdueDays = intFlexible("overdue_days"),
    missedDays = intFlexible("missed_days")
)

fun JSONObject.toDashboardPayload(): DashboardPayload {
    val summaryJson = optJSONObject("summary") ?: JSONObject()
    val summary = DashboardSummary(
        totalCategories = summaryJson.intFlexible("totalCategories"),
        totalItems = summaryJson.intFlexible("totalItems"),
        totalQuantity = summaryJson.intFlexible("totalQuantity"),
        availableQuantity = summaryJson.intFlexible("availableQuantity"),
        bookedQuantity = summaryJson.intFlexible("bookedQuantity"),
        missedPickups = summaryJson.intFlexible("missedPickups"),
        givenQuantity = summaryJson.intFlexible("givenQuantity"),
        overdueReturns = summaryJson.intFlexible("overdueReturns")
    )

    fun entries(key: String) = optJSONArray(key)?.objects()?.map { it.toDashboardEntry() }.orEmpty()
    val missed = entries("missedPickups")
    val bookings = entries("todayBookings")
    val pickups = entries("todayPickups")
    val returns = entries("todayReturns")
    val overdue = entries("overdueReturns")
    val countsJson = optJSONObject("sectionCounts") ?: JSONObject()
    val counts = DashboardSectionCounts(
        missedPickups = if (countsJson.has("missedPickups")) countsJson.intFlexible("missedPickups") else missed.size,
        todayBookings = if (countsJson.has("todayBookings")) countsJson.intFlexible("todayBookings") else bookings.size,
        todayPickups = if (countsJson.has("todayPickups")) countsJson.intFlexible("todayPickups") else pickups.size,
        todayReturns = if (countsJson.has("todayReturns")) countsJson.intFlexible("todayReturns") else returns.size,
        overdueReturns = if (countsJson.has("overdueReturns")) countsJson.intFlexible("overdueReturns") else overdue.size
    )

    val kpisJson = optJSONObject("kpis") ?: JSONObject()
    val todayJson = kpisJson.optJSONObject("todayOverview") ?: JSONObject()
    val bookingJson = kpisJson.optJSONObject("bookingStatus") ?: JSONObject()
    val paymentJson = kpisJson.optJSONObject("paymentBilling") ?: JSONObject()
    val inventoryJson = kpisJson.optJSONObject("inventory") ?: JSONObject()
    val customerJson = kpisJson.optJSONObject("customers") ?: JSONObject()

    val kpis = DashboardKpis(
        todayOverview = DashboardTodayKpis(
            todayPickups = todayJson.intFlexible("todayPickups"),
            todayReturns = todayJson.intFlexible("todayReturns"),
            missedPickups = todayJson.intFlexible("missedPickups"),
            overdueReturns = todayJson.intFlexible("overdueReturns")
        ),
        bookingStatus = DashboardBookingKpis(
            reserved = bookingJson.intFlexible("reserved"),
            booked = bookingJson.intFlexible("booked"),
            partPickedUp = bookingJson.intFlexible("partPickedUp"),
            fullPickedUp = bookingJson.intFlexible("fullPickedUp"),
            partReturn = bookingJson.intFlexible("partReturn"),
            fullReturned = bookingJson.intFlexible("fullReturned"),
            cancelled = bookingJson.intFlexible("cancelled"),
            activeRentalOrders = bookingJson.intFlexible("activeRentalOrders")
        ),
        paymentBilling = DashboardPaymentBillingKpis(
            pendingPayment = paymentJson.intFlexible("pendingPayment"),
            partPayment = paymentJson.intFlexible("partPayment"),
            fullPayment = paymentJson.intFlexible("fullPayment"),
            draftBills = paymentJson.intFlexible("draftBills"),
            finalBills = paymentJson.intFlexible("finalBills"),
            billsToday = paymentJson.intFlexible("billsToday"),
            pendingBalance = paymentJson.intFlexible("pendingBalance"),
            totalReceived = paymentJson.intFlexible("totalReceived")
        ),
        inventory = DashboardInventoryKpis(
            availableNow = inventoryJson.intFlexible("availableNow"),
            pickupPendingQty = inventoryJson.intFlexible("pickupPendingQty"),
            currentlyOutQty = inventoryJson.intFlexible("currentlyOutQty"),
            totalQuantity = inventoryJson.intFlexible("totalQuantity"),
            totalItems = inventoryJson.intFlexible("totalItems"),
            categories = inventoryJson.intFlexible("categories"),
            lowStock = inventoryJson.intFlexible("lowStock"),
            unavailable = inventoryJson.intFlexible("unavailable")
        ),
        customers = DashboardCustomerKpis(
            totalCustomers = customerJson.intFlexible("totalCustomers"),
            newCustomers = customerJson.intFlexible("newCustomers"),
            returningCustomers = customerJson.intFlexible("returningCustomers"),
            frequentCustomers = customerJson.intFlexible("frequentCustomers"),
            activeRentalCustomers = customerJson.intFlexible("activeRentalCustomers"),
            customerExceptions = customerJson.intFlexible("customerExceptions")
        )
    )

    val categoryInventory = optJSONArray("categoryInventory")?.objects()?.map { row ->
        DashboardCategoryInventory(
            categoryId = row.optString("categoryId"),
            categoryName = row.optString("categoryName"),
            items = row.intFlexible("items"),
            totalQty = row.intFlexible("totalQty"),
            available = row.intFlexible("available"),
            pickupPending = row.intFlexible("pickupPending"),
            currentlyOut = row.intFlexible("currentlyOut")
        )
    }.orEmpty()
    val accessJson = optJSONObject("access") ?: JSONObject()
    val access = DashboardAccess(
        bookings = accessJson.boolFlexible("bookings"),
        billing = accessJson.boolFlexible("billing"),
        items = accessJson.boolFlexible("items"),
        customers = accessJson.boolFlexible("customers"),
        reports = accessJson.boolFlexible("reports")
    )

    return DashboardPayload(
        today = optString("today"),
        kpis = kpis,
        categoryInventory = categoryInventory,
        access = access,
        summary = summary,
        sectionCounts = counts,
        missedPickups = missed,
        todayBookings = bookings,
        todayPickups = pickups,
        todayReturns = returns,
        overdueReturns = overdue
    )
}

private fun JSONObject.toCustomer() = Customer(
    id = optString("id"),
    name = optString("name"),
    mobile = optString("mobile"),
    alternateMobile = stringOrNull("alternate_mobile"),
    address = stringOrNull("address"),
    notes = stringOrNull("notes"),
    isActive = boolFlexible("is_active", true),
    archivedAt = stringOrNull("archived_at"),
    totalBookings = intFlexible("total_bookings"),
    activeBookings = intFlexible("active_bookings"),
    lastBookingDate = stringOrNull("last_booking_date")
)

fun JSONObject.toCustomerPage(): PageResult<Customer> {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val page = pagination.intFlexible("page").coerceAtLeast(1)
    val totalPages = (
        if (pagination.has("totalPages")) pagination.intFlexible("totalPages")
        else pagination.intFlexible("pages")
    ).coerceAtLeast(1)

    return PageResult(
        items = optJSONArray("customers")?.objects()?.map { it.toCustomer() }.orEmpty(),
        page = page,
        totalPages = totalPages,
        total = pagination.intFlexible("total")
    )
}

private fun JSONObject.toBookingRow() = BookingRow(
    id = optString("id"),
    bookingNo = optString("booking_no"),
    bookingDate = optString("booking_date"),
    pickupDate = optString("pickup_date"),
    returnDate = optString("return_date"),
    status = optString("status"),
    notes = stringOrNull("notes"),
    customerId = optString("customer_id"),
    customerName = optString("customer_name"),
    customerMobile = optString("customer_mobile"),
    itemsSummary = optString("items_summary"),
    bookedQty = intFlexible("booked_qty"),
    givenQty = intFlexible("given_qty"),
    returnedQty = intFlexible("returned_qty")
)

fun JSONObject.toBookingPage(): PageResult<BookingRow> {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val page = pagination.intFlexible("page").coerceAtLeast(1)
    val totalPages = (
        if (pagination.has("totalPages")) pagination.intFlexible("totalPages")
        else pagination.intFlexible("pages")
    ).coerceAtLeast(1)

    return PageResult(
        items = optJSONArray("bookings")?.objects()?.map { it.toBookingRow() }.orEmpty(),
        page = page,
        totalPages = totalPages,
        total = pagination.intFlexible("total")
    )
}
