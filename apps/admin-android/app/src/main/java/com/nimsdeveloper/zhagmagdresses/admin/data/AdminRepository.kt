package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONArray
import org.json.JSONObject

class AdminRepository(private val api: ApiClient) {
    suspend fun currentSession(): SessionEnvelope =
        api.get("/api/auth/me").toSessionEnvelope()

    suspend fun currentUser(): SessionUser = currentSession().user

    suspend fun publicBranding(): AppBranding =
        api.get("/api/public/bootstrap").toAppBranding()

    suspend fun loginWithBranding(identifier: String, password: String): SessionEnvelope =
        api.post(
            "/api/auth/login",
            JSONObject()
                .put("identifier", identifier.trim())
                .put("password", password)
        ).toSessionEnvelope()

    suspend fun login(identifier: String, password: String): SessionUser =
        loginWithBranding(identifier, password).user

    suspend fun logout() {
        runCatching { api.post("/api/auth/logout") }
    }

    suspend fun dashboard(): DashboardPayload =
        api.get("/api/admin/dashboard").toDashboardPayload()

    suspend fun customers(page: Int, search: String, pageSize: Int = 20): PageResult<Customer> =
        api.get(
            api.queryPath(
                "/api/admin/customers",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() }
                )
            )
        ).toCustomerPage()

    suspend fun createBookingCustomer(
        name: String,
        mobile: String,
        alternateMobile: String,
        address: String
    ): CustomerMutationResult =
        api.post(
            "/api/admin/customers",
            JSONObject()
                .put("name", name.trim())
                .put("mobile", mobile.filter(Char::isDigit))
                .put("alternateMobile", alternateMobile.filter(Char::isDigit))
                .put("address", address.trim())
        ).toCustomerMutationResult()

    suspend fun bookings(page: Int, search: String, pageSize: Int = 20): PageResult<BookingRow> =
        api.get(
            api.queryPath(
                "/api/admin/bookings",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() }
                )
            )
        ).toBookingPage()

    suspend fun lifecycleBookings(
        page: Int,
        search: String,
        status: String = "",
        view: String = "ALL",
        sort: String = "NEWEST",
        pageSize: Int = 10
    ): PageResult<LifecycleBookingSummary> =
        api.get(
            api.queryPath(
                "/api/admin/bookings",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.coerceIn(1, 10).toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() },
                    "status" to status.trim().takeIf { it.isNotBlank() },
                    "view" to view.trim().takeIf { it.isNotBlank() && !it.equals("ALL", true) },
                    "sort" to sort.trim().takeIf { it.isNotBlank() }
                )
            )
        ).toLifecycleBookingPage()

    suspend fun bookingBootstrap(): BookingBootstrapData =
        api.get("/api/admin/bookings/bootstrap").toBookingBootstrapData()

    suspend fun bookingAvailability(
        pickupDate: String,
        returnDate: String,
        itemIds: List<String>,
        excludeBookingId: String? = null
    ): Map<String, Int> {
        if (itemIds.isEmpty()) return emptyMap()
        val items = JSONArray().apply {
            itemIds.distinct().take(300).forEach { itemId -> put(itemId) }
        }
        val body = JSONObject()
            .put("pickupDate", pickupDate.trim())
            .put("returnDate", returnDate.trim())
            .put("itemIds", items)
        if (!excludeBookingId.isNullOrBlank()) body.put("excludeBookingId", excludeBookingId)

        val json = api.post("/api/admin/bookings/availability", body)
        val rows = json.optJSONArray("availability") ?: JSONArray()
        return buildMap {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val itemId = row.optString("itemId").trim()
                if (itemId.isNotBlank()) {
                    put(itemId, row.optInt("availableQuantity", 0).coerceAtLeast(0))
                }
            }
        }
    }

    suspend fun bookingDetail(id: String): BookingDetailData =
        api.get("/api/admin/bookings/$id").toBookingDetailData()

    suspend fun createBooking(
        customerId: String,
        pickupDate: String,
        returnDate: String,
        notes: String,
        advanceAmount: Int,
        lines: List<BookingRequestLine>,
        confirmationState: String,
        requestKey: String
    ): BookingSaveResult =
        api.post(
            "/api/admin/bookings",
            bookingBody(customerId, pickupDate, returnDate, notes, advanceAmount, lines)
                .put("confirmationState", confirmationState)
                .put("requestKey", requestKey)
        ).toBookingSaveResult()

    suspend fun updateBooking(
        id: String,
        customerId: String,
        pickupDate: String,
        returnDate: String,
        notes: String,
        advanceAmount: Int,
        lines: List<BookingRequestLine>,
        expectedUpdatedAt: String?
    ): BookingActionResult {
        val body = bookingBody(customerId, pickupDate, returnDate, notes, advanceAmount, lines)
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        return api.request(
            "PUT",
            "/api/admin/bookings/$id",
            body
        ).toBookingActionResult("Booking updated.")
    }

    suspend fun confirmBooking(id: String): BookingActionResult =
        api.post("/api/admin/bookings/$id/confirm").toBookingActionResult("Booking confirmed.")

    suspend fun cancelBooking(
        id: String,
        advanceSettlementStatus: String? = null,
        advanceRefundAmount: Int = 0
    ): BookingActionResult =
        api.post(
            "/api/admin/bookings/$id/cancel",
            JSONObject().apply {
                advanceSettlementStatus?.takeIf { it.isNotBlank() }?.let { put("advanceSettlementStatus", it) }
                put("advanceRefundAmount", advanceRefundAmount.coerceAtLeast(0))
            }
        ).toBookingActionResult("Booking cancelled.")

    suspend fun savePickup(
        bookingId: String,
        lines: List<BookingActionLine>,
        requestKey: String,
        notes: String = ""
    ): BookingActionResult =
        api.post(
            "/api/admin/pickups/$bookingId",
            JSONObject()
                .put("requestKey", requestKey)
                .put("notes", notes.trim())
                .put("items", JSONArray().apply {
                    lines.forEach { line ->
                        put(JSONObject().put("bookingItemId", line.bookingItemId).put("quantity", line.quantity))
                    }
                })
        ).toBookingActionResult("Pickup saved.")

    suspend fun saveReturn(
        bookingId: String,
        lines: List<BookingActionLine>,
        requestKey: String,
        notes: String = "",
        pendingPickupAction: String? = null
    ): BookingActionResult =
        api.post(
            "/api/admin/returns/$bookingId",
            JSONObject()
                .put("requestKey", requestKey)
                .put("notes", notes.trim())
                .apply {
                    if (!pendingPickupAction.isNullOrBlank()) put("pendingPickupAction", pendingPickupAction)
                }
                .put("items", JSONArray().apply {
                    lines.forEach { line ->
                        put(
                            JSONObject()
                                .put("bookingItemId", line.bookingItemId)
                                .put("quantity", line.quantity)
                                .put("conditionNote", "")
                        )
                    }
                })
        ).toBookingActionResult("Return saved.")

    private fun bookingBody(
        customerId: String,
        pickupDate: String,
        returnDate: String,
        notes: String,
        advanceAmount: Int,
        lines: List<BookingRequestLine>
    ) = JSONObject()
        .put("customerId", customerId)
        .put("pickupDate", pickupDate)
        .put("returnDate", returnDate)
        .put("notes", notes.trim())
        .put("advanceAmount", advanceAmount.coerceAtLeast(0))
        .put("items", JSONArray().apply {
            lines.forEach { line ->
                put(JSONObject().put("itemId", line.itemId).put("quantity", line.quantity))
            }
        })
}
