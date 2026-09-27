package com.nimsdeveloper.zhagmagdresses.admin

import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData
import com.nimsdeveloper.zhagmagdresses.admin.data.toBookingDetailData
import org.json.JSONArray
import org.json.JSONObject

internal class Screen11BillingRepository(
    private val api: ApiClient
) {
    suspend fun bootstrap(bookingId: String? = null): BillingBootstrap =
        api.get(
            api.queryPath(
                "/api/admin/billing/bootstrap",
                mapOf("bookingId" to bookingId?.takeIf { it.isNotBlank() })
            )
        ).toBillingBootstrap()

    suspend fun bills(
        page: Int,
        search: String,
        paymentStatus: String,
        status: String
    ): BillingPage =
        api.get(
            api.queryPath(
                "/api/admin/bills",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to "10",
                    "search" to search.trim().takeIf { it.isNotBlank() },
                    "paymentStatus" to paymentStatus.takeIf { it.isNotBlank() },
                    "status" to status.takeIf { it.isNotBlank() }
                )
            )
        ).toBillingPage()

    suspend fun detail(id: String): BillingDetail =
        api.get("/api/admin/bills/$id").toBillingDetail()

    suspend fun bookingDetail(id: String): BookingDetailData =
        api.get("/api/admin/bookings/$id").toBookingDetailData()

    suspend fun eligibleOrders(
        page: Int,
        search: String
    ): BillingEligiblePage =
        api.get(
            api.queryPath(
                "/api/admin/billing/eligible-orders",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() }
                )
            )
        ).toBillingEligiblePage()

    suspend fun ensureDraft(bookingId: String): BillingEnsureDraftResult =
        api.post("/api/admin/billing/ensure-draft/$bookingId").toBillingEnsureDraftResult()

    suspend fun saveDraft(
        id: String?,
        requestKey: String,
        bookingId: String?,
        customerId: String,
        billDate: String,
        discountAmount: Int,
        advanceAmount: Int,
        otherReceivedAmount: Int,
        notes: String,
        lines: List<BillingDraftLine>,
        expectedUpdatedAt: String?
    ): String {
        val body = JSONObject()
            .put("requestKey", requestKey)
            .put("customerId", customerId)
            .put("billDate", billDate)
            .put("discountAmount", discountAmount.coerceAtLeast(0))
            .put("advanceAmount", advanceAmount.coerceAtLeast(0))
            .put("otherReceivedAmount", otherReceivedAmount.coerceAtLeast(0))
            .put("notes", notes.trim().take(500))
            .put(
                "items",
                JSONArray().apply {
                    lines.forEach { line ->
                        put(
                            JSONObject()
                                .put("itemId", line.itemId)
                                .put("quantity", line.quantity.coerceAtLeast(1))
                                .put("rentRate", line.rentRate.coerceAtLeast(0))
                        )
                    }
                }
            )
        bookingId?.takeIf { it.isNotBlank() }?.let { body.put("bookingId", it) }
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }

        val response = if (id.isNullOrBlank()) {
            api.post("/api/admin/bills", body)
        } else {
            api.request("PUT", "/api/admin/bills/$id", body)
        }
        return response.optString("id").ifBlank { id.orEmpty() }
    }

    suspend fun finalize(id: String) {
        api.post("/api/admin/bills/$id/finalize")
    }



    suspend fun deleteDraft(id: String) {
        api.request("DELETE", "/api/admin/bills/$id")
    }

    suspend fun createCustomer(
        name: String,
        mobile: String,
        address: String
    ): BillingCustomer {
        val cleanName = name.trim()
        val cleanMobile = mobile.filter(Char::isDigit)
        require(cleanName.isNotBlank()) { "Customer name is required." }
        require(cleanMobile.length == 10) { "Enter a valid 10-digit mobile number." }
        val result = api.post(
            "/api/admin/customers",
            JSONObject()
                .put("name", cleanName)
                .put("mobile", cleanMobile)
                .put("alternateMobile", "")
                .put("address", address.trim())
        )
        val id = result.optString("id")
        require(id.isNotBlank()) { "Customer was saved but could not be selected." }
        return BillingCustomer(id, cleanName, cleanMobile, address.trim())
    }

    suspend fun whatsapp(customerId: String, billId: String): BillingWhatsAppBundle =
        api.get(
            api.queryPath(
                "/api/admin/customers/$customerId/whatsapp",
                mapOf(
                    "billId" to billId,
                    "mode" to "templates"
                )
            )
        ).toBillingWhatsAppBundle()
}
