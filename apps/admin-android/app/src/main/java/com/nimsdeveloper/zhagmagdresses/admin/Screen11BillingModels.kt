package com.nimsdeveloper.zhagmagdresses.admin

import org.json.JSONArray
import org.json.JSONObject

internal data class BillingCustomer(
    val id: String,
    val name: String,
    val mobile: String,
    val address: String = ""
)

internal data class BillingItemOption(
    val id: String,
    val itemCode: String,
    val itemName: String,
    val categoryName: String,
    val rentAmount: Int,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

internal data class BillingDraftLine(
    val itemId: String,
    val itemCode: String,
    val itemName: String,
    val categoryName: String,
    val quantity: Int,
    val rentRate: Int,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList()
) {
    val amount: Int get() = quantity.coerceAtLeast(0) * rentRate.coerceAtLeast(0)
}

internal data class BillingBookingSeed(
    val id: String,
    val bookingNo: String,
    val customerId: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,
    val status: String,
    val confirmationState: String = "BOOKED",
    val pickupDate: String = "",
    val returnDate: String = "",
    val advanceAmount: Int,
    val returnComplete: Boolean = false,
    val receivedAmount: Int = 0,
    val advanceRefundAmount: Int = 0,
    val advanceSettlementStatus: String? = null
) {
    val advanceRetainedAmount: Int get() = (advanceAmount - advanceRefundAmount).coerceAtLeast(0)
}

internal data class BillingEligibleOrder(
    val id: String,
    val bookingNo: String,
    val bookingDate: String,
    val pickupDate: String,
    val returnDate: String,
    val status: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String = "",
    val advanceAmount: Int
)

internal data class BillingEligiblePage(
    val orders: List<BillingEligibleOrder>,
    val page: Int,
    val totalPages: Int,
    val total: Int
)

internal data class BillingEnsureDraftResult(
    val id: String,
    val status: String
)

internal data class BillingBootstrap(
    val today: String,
    val customers: List<BillingCustomer>,
    val items: List<BillingItemOption>,
    val booking: BillingBookingSeed?,
    val bookingItems: List<BillingDraftLine>,
    val existingBillId: String?
)

internal data class BillingBusinessInfo(
    val shopName: String = "",
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val websiteUrl: String = ""
)

internal data class BillingBill(
    val id: String,
    val billNo: String?,
    val bookingId: String?,
    val bookingNo: String?,
    val customerId: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,
    val billDate: String,
    val pickupDate: String = "",
    val returnDate: String = "",
    val pickupAt: String = "",
    val returnAt: String = "",
    val pickupComplete: Boolean = false,
    val notes: String = "",
    val status: String,
    val paymentStatus: String,
    val totalRent: Int,
    val discountAmount: Int,
    val netAmount: Int,
    val advanceAmount: Int,
    val receivedAmount: Int = 0,
    val advanceRefundAmount: Int = 0,
    val advanceSettlementStatus: String? = null,
    val balanceAmount: Int,
    val returnComplete: Boolean = false,
    val updatedAt: String?
)

internal data class BillingDetail(
    val bill: BillingBill,
    val items: List<BillingDraftLine>,
    val business: BillingBusinessInfo = BillingBusinessInfo()
)

internal data class BillingPage(
    val bills: List<BillingBill>,
    val page: Int,
    val totalPages: Int,
    val total: Int
)

internal data class BillingWhatsAppTemplate(
    val name: String,
    val message: String
)

internal data class BillingWhatsAppBundle(
    val customerName: String,
    val mobile: String,
    val templates: List<BillingWhatsAppTemplate>
)

private fun JSONObject.intValue(key: String): Int =
    optInt(key, 0).coerceAtLeast(0)

private fun JSONObject.nullableString(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotBlank() } else null

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }.distinct()
}

private fun JSONArray.objectsList(): List<JSONObject> = buildList {
    for (index in 0 until length()) optJSONObject(index)?.let(::add)
}

internal fun JSONObject.toBillingBootstrap(): BillingBootstrap {
    val customers = optJSONArray("customers")?.objectsList().orEmpty().map { row ->
        BillingCustomer(
            id = row.optString("id"),
            name = row.optString("name"),
            mobile = row.optString("mobile"),
            address = row.optString("address")
        )
    }
    val items = optJSONArray("items")?.objectsList().orEmpty().map { row ->
        BillingItemOption(
            id = row.optString("id"),
            itemCode = row.optString("item_code"),
            itemName = row.optString("item_name"),
            categoryName = row.optString("category_name"),
            rentAmount = row.intValue("rent_amount"),
            imageUrl = row.nullableString("image_url"),
            imageUrls = row.stringList("image_urls").ifEmpty {
                row.nullableString("image_url")?.let(::listOf).orEmpty()
            }
        )
    }
    val bookingJson = optJSONObject("booking")
    val booking = bookingJson?.let { row ->
        BillingBookingSeed(
            id = row.optString("id"),
            bookingNo = row.optString("booking_no"),
            customerId = row.optString("customer_id"),
            customerName = row.optString("customer_name"),
            customerMobile = row.optString("customer_mobile"),
            customerAddress = row.optString("customer_address"),
            status = row.optString("status").uppercase(),
            confirmationState = row.optString("confirmation_state").ifBlank { "BOOKED" }.uppercase(),
            pickupDate = row.optString("pickup_date"),
            returnDate = row.optString("return_date"),
            advanceAmount = row.intValue("advance_amount"),
            returnComplete = row.optInt("return_complete", 0) == 1,
            advanceRefundAmount = row.intValue("advance_refund_amount"),
            advanceSettlementStatus = row.nullableString("advance_settlement_status")
        )
    }
    val bookingItems = optJSONArray("bookingItems")?.objectsList().orEmpty().map { row ->
        val qty = row.optInt("quantity", 1).coerceAtLeast(1)
        val rate = row.intValue("rent_amount")
        BillingDraftLine(
            itemId = row.optString("item_id"),
            itemCode = row.optString("item_code"),
            itemName = row.optString("item_name"),
            categoryName = row.optString("category_name"),
            quantity = qty,
            rentRate = rate,
            imageUrl = row.nullableString("image_url"),
            imageUrls = row.stringList("image_urls").ifEmpty {
                row.nullableString("image_url")?.let(::listOf).orEmpty()
            }
        )
    }
    return BillingBootstrap(
        today = optString("today"),
        customers = customers,
        items = items,
        booking = booking,
        bookingItems = bookingItems,
        existingBillId = nullableString("existingBillId")
    )
}

private fun JSONObject.toBillingBill(): BillingBill = BillingBill(
    id = optString("id"),
    billNo = nullableString("bill_no"),
    bookingId = nullableString("booking_id"),
    bookingNo = nullableString("booking_no_snapshot"),
    customerId = optString("customer_id"),
    customerName = optString("customer_name_snapshot"),
    customerMobile = optString("customer_mobile_snapshot"),
    customerAddress = optString("customer_address_snapshot"),
    billDate = optString("bill_date"),
    pickupDate = optString("pickup_date"),
    returnDate = optString("return_date"),
    pickupAt = optString("pickup_at"),
    returnAt = optString("return_at"),
    pickupComplete = intValue("pickup_complete") == 1,
    notes = optString("notes"),
    status = optString("status").uppercase(),
    paymentStatus = optString("payment_status").uppercase(),
    totalRent = intValue("total_rent"),
    discountAmount = intValue("discount_amount"),
    netAmount = intValue("net_amount"),
    advanceAmount = intValue("advance_amount"),
    receivedAmount = intValue("received_amount"),
    advanceRefundAmount = intValue("advance_refund_amount"),
    advanceSettlementStatus = nullableString("advance_settlement_status"),
    balanceAmount = intValue("balance_amount"),
    returnComplete = optInt("return_complete", 0) == 1,
    updatedAt = nullableString("updated_at")
)

internal fun JSONObject.toBillingDetail(): BillingDetail {
    val bill = (optJSONObject("bill") ?: JSONObject()).toBillingBill()
    val items = optJSONArray("items")?.objectsList().orEmpty().map { row ->
        BillingDraftLine(
            itemId = row.optString("item_id"),
            itemCode = row.optString("item_code_snapshot"),
            itemName = row.optString("item_name_snapshot"),
            categoryName = row.optString("category_name_snapshot"),
            quantity = row.optInt("quantity", 1).coerceAtLeast(1),
            rentRate = row.intValue("rent_rate"),
            imageUrl = row.nullableString("image_url"),
            imageUrls = row.stringList("image_urls").ifEmpty {
                row.nullableString("image_url")?.let(::listOf).orEmpty()
            }
        )
    }
    val businessJson = optJSONObject("business") ?: JSONObject()
    return BillingDetail(
        bill = bill,
        items = items,
        business = BillingBusinessInfo(
            shopName = businessJson.optString("shopName"),
            contactNumber = businessJson.optString("contactNumber"),
            whatsappNumber = businessJson.optString("whatsappNumber"),
            address = businessJson.optString("address"),
            websiteUrl = businessJson.optString("websiteUrl")
        )
    )
}

internal fun JSONObject.toBillingPage(): BillingPage {
    val paging = optJSONObject("pagination") ?: JSONObject()
    return BillingPage(
        bills = optJSONArray("bills")?.objectsList().orEmpty().map { it.toBillingBill() },
        page = paging.optInt("page", 1).coerceAtLeast(1),
        totalPages = paging.optInt("totalPages", 1).coerceAtLeast(1),
        total = paging.optInt("total", 0).coerceAtLeast(0)
    )
}

internal fun JSONObject.toBillingWhatsAppBundle(): BillingWhatsAppBundle =
    BillingWhatsAppBundle(
        customerName = optString("customerName"),
        mobile = optString("mobile"),
        templates = optJSONArray("templates")?.objectsList().orEmpty().map { row ->
            BillingWhatsAppTemplate(
                name = row.optString("templateName").ifBlank { row.optString("linkedAction") },
                message = row.optString("message")
            )
        }.filter { it.message.isNotBlank() }
    )

internal fun JSONObject.toBillingEligiblePage(): BillingEligiblePage {
    val paging = optJSONObject("pagination") ?: JSONObject()
    return BillingEligiblePage(
        orders = optJSONArray("orders")?.objectsList().orEmpty().map { row ->
            BillingEligibleOrder(
                id = row.optString("id"),
                bookingNo = row.optString("booking_no"),
                bookingDate = row.optString("booking_date"),
                pickupDate = row.optString("pickup_date"),
                returnDate = row.optString("return_date"),
                status = row.optString("status").uppercase(),
                customerName = row.optString("customer_name"),
                customerMobile = row.optString("customer_mobile"),
                customerAddress = row.optString("customer_address"),
                advanceAmount = row.intValue("advance_amount")
            )
        },
        page = paging.optInt("page", 1).coerceAtLeast(1),
        totalPages = paging.optInt("totalPages", 1).coerceAtLeast(1),
        total = paging.optInt("total", 0).coerceAtLeast(0)
    )
}

internal fun JSONObject.toBillingEnsureDraftResult(): BillingEnsureDraftResult =
    BillingEnsureDraftResult(
        id = optString("id"),
        status = optString("status").ifBlank { "DRAFT" }.uppercase()
    )
