package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONArray
import org.json.JSONObject

data class BookingOptionCustomer(
    val id: String,
    val name: String,
    val mobile: String,
    val alternateMobile: String? = null
)

data class BookingOptionItem(
    val id: String,
    val itemCode: String,
    val itemName: String,
    val categoryId: String,
    val categoryName: String,
    val totalQuantity: Int,
    val rentAmount: Int = 0,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

data class BookingRelatedItemLink(
    val sourceItemId: String,
    val relatedItemId: String,
    val displayOrder: Int = 0
)

data class BookingBootstrapData(
    val customers: List<BookingOptionCustomer>,
    val items: List<BookingOptionItem>,
    val relatedItems: List<BookingRelatedItemLink> = emptyList()
)

data class BookingListItemPreview(
    val itemName: String,
    val itemCode: String,
    val categoryName: String,
    val imageUrl: String?,
    val quantity: Int,
    val givenQty: Int = 0,
    val returnedQty: Int = 0,
    val imageUrls: List<String> = emptyList()
)

data class LifecycleBookingSummary(
    val id: String,
    val bookingNo: String,
    val bookingDate: String,
    val pickupDate: String,
    val returnDate: String,
    val rawStatus: String,
    val confirmationState: String,
    val displayStatus: String,
    val paymentStatus: String? = null,
    val notes: String?,
    val customerId: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String = "",
    val customerAlternateMobile: String? = null,
    val itemsSummary: String,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val itemPreviews: List<BookingListItemPreview> = emptyList(),
    val itemCount: Int = 0,
    val advanceAmount: Int = 0,
    val advanceRefundAmount: Int = 0,
    val advanceSettlementStatus: String? = null,
    val billId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class BookingDetailItem(
    val bookingItemId: String,
    val itemId: String,
    /** Original booked quantity. Closed remaining quantity stays visible in history. */
    val bookedQty: Int,
    /** Active quantity after any remaining unpicked quantity was closed. */
    val effectiveBookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val closedQty: Int,
    val itemCode: String,
    val itemName: String,
    val categoryName: String,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList()
) {
    val remainingToGive: Int get() = (effectiveBookedQty - givenQty).coerceAtLeast(0)
    val pendingToReturn: Int get() = (givenQty - returnedQty).coerceAtLeast(0)
}

data class BookingHistoryItem(
    val itemName: String,
    val quantity: Int,
    val kind: String
)

data class BookingTimelineEvent(
    val id: String,
    val action: String,
    val createdAt: String,
    val userName: String? = null,
    val oldValueJson: String? = null,
    val newValueJson: String? = null,
    val items: List<BookingHistoryItem> = emptyList()
)

data class BookingDetailData(
    val booking: LifecycleBookingSummary,
    val items: List<BookingDetailItem>,
    val timeline: List<BookingTimelineEvent> = emptyList()
)

data class BookingRequestLine(val itemId: String, val quantity: Int)
data class BookingActionLine(val bookingItemId: String, val quantity: Int)

data class BookingSaveResult(
    val id: String,
    val bookingNo: String,
    val message: String,
    val duplicate: Boolean
)

data class BookingActionResult(
    val message: String,
    val billingHandoffBookingId: String? = null,
    val billingBillId: String? = null,
    val billingBillStatus: String? = null,
    val billingDraftError: String? = null
)

private fun fallbackDisplayStatus(rawStatus: String, confirmationState: String): String = when {
    rawStatus.equals("BOOKED", true) && confirmationState.equals("RESERVED", true) -> "RESERVED"
    rawStatus.equals("BOOKED", true) -> "BOOKED"
    rawStatus.equals("PARTIALLY_GIVEN", true) -> "PART_PICKUP"
    rawStatus.equals("GIVEN", true) -> "FULL_PICKUP"
    rawStatus.equals("PARTIALLY_RETURNED", true) -> "PART_RETURN"
    rawStatus.equals("RETURNED", true) -> "FULL_RETURN"
    rawStatus.equals("CANCELLED", true) -> "CANCELLED"
    else -> rawStatus.uppercase()
}

private fun itemNameWithSecondary(name: String, code: String, category: String): String {
    val secondary = listOf(code.trim(), category.trim()).filter { it.isNotBlank() }.joinToString(" · ")
    return if (secondary.isBlank()) name else "$name\n$secondary"
}

private fun JSONObject.itemImageUrls(): List<String> {
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

private fun JSONObject.toLifecycleBookingSummary(): LifecycleBookingSummary {
    val rawStatus = optString("status").uppercase()
    val confirmationState = optString("confirmation_state").ifBlank { "BOOKED" }.uppercase()
    val itemPreviews = optJSONArray("item_previews")?.objects()?.map {
        BookingListItemPreview(
            itemName = it.optString("item_name"),
            itemCode = it.optString("item_code"),
            categoryName = it.optString("category_name"),
            imageUrl = it.stringOrNull("image_url"),
            imageUrls = it.itemImageUrls(),
            quantity = it.intFlexible("quantity").coerceAtLeast(0),
            givenQty = it.intFlexible("given_qty").coerceAtLeast(0),
            returnedQty = it.intFlexible("returned_qty").coerceAtLeast(0)
        )
    }.orEmpty()
    val parsedItemCount = intFlexible("item_count").coerceAtLeast(0)
    return LifecycleBookingSummary(
        id = optString("id"),
        bookingNo = optString("booking_no"),
        bookingDate = optString("booking_date"),
        pickupDate = optString("pickup_date"),
        returnDate = optString("return_date"),
        rawStatus = rawStatus,
        confirmationState = confirmationState,
        displayStatus = optString("display_status").ifBlank { fallbackDisplayStatus(rawStatus, confirmationState) },
        paymentStatus = stringOrNull("payment_status"),
        notes = stringOrNull("notes"),
        customerId = optString("customer_id"),
        customerName = optString("customer_name"),
        customerMobile = optString("customer_mobile"),
        customerAddress = optString("customer_address"),
        customerAlternateMobile = stringOrNull("customer_alternate_mobile"),
        itemsSummary = optString("items_summary"),
        bookedQty = intFlexible("booked_qty"),
        givenQty = intFlexible("given_qty"),
        returnedQty = intFlexible("returned_qty"),
        itemPreviews = itemPreviews,
        itemCount = if (parsedItemCount > 0) parsedItemCount else itemPreviews.size,
        advanceAmount = intFlexible("advance_amount").coerceAtLeast(0),
        advanceRefundAmount = intFlexible("advance_refund_amount").coerceAtLeast(0),
        advanceSettlementStatus = stringOrNull("advance_settlement_status"),
        billId = stringOrNull("bill_id"),
        createdAt = stringOrNull("created_at"),
        updatedAt = stringOrNull("updated_at")
    )
}

fun JSONObject.toLifecycleBookingPage(): PageResult<LifecycleBookingSummary> {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val page = pagination.intFlexible("page").coerceAtLeast(1)
    val totalPages = (
        if (pagination.has("totalPages")) pagination.intFlexible("totalPages")
        else pagination.intFlexible("pages")
    ).coerceAtLeast(1)
    return PageResult(
        items = optJSONArray("bookings")?.objects()?.map { it.toLifecycleBookingSummary() }.orEmpty(),
        page = page,
        totalPages = totalPages,
        total = pagination.intFlexible("total")
    )
}

fun JSONObject.toBookingBootstrapData(): BookingBootstrapData = BookingBootstrapData(
    customers = optJSONArray("customers")?.objects()?.map {
        BookingOptionCustomer(
            id = it.optString("id"),
            name = it.optString("name"),
            mobile = it.optString("mobile"),
            alternateMobile = it.stringOrNull("alternate_mobile")
        )
    }.orEmpty(),
    items = optJSONArray("items")?.objects()?.map {
        BookingOptionItem(
            id = it.optString("id"),
            itemCode = it.optString("item_code"),
            itemName = it.optString("item_name"),
            categoryId = it.optString("category_id"),
            categoryName = it.optString("category_name"),
            totalQuantity = it.intFlexible("total_quantity"),
            rentAmount = it.intFlexible("rent_amount").coerceAtLeast(0),
            imageUrl = it.stringOrNull("image_url"),
            imageUrls = it.itemImageUrls()
        )
    }.orEmpty(),
    relatedItems = optJSONArray("relatedItems")?.objects()?.map {
        BookingRelatedItemLink(
            sourceItemId = it.optString("source_item_id"),
            relatedItemId = it.optString("related_item_id"),
            displayOrder = it.intFlexible("display_order")
        )
    }.orEmpty()
)

fun JSONObject.toBookingDetailData(): BookingDetailData {
    val bookingJson = optJSONObject("booking") ?: JSONObject()
    val booking = bookingJson.toLifecycleBookingSummary()
    val items = optJSONArray("items")?.objects()?.map {
        BookingDetailItem(
            bookingItemId = it.optString("booking_item_id"),
            itemId = it.optString("item_id"),
            bookedQty = if (it.has("original_booked_qty")) it.intFlexible("original_booked_qty") else it.intFlexible("booked_qty"),
            effectiveBookedQty = it.intFlexible("booked_qty"),
            givenQty = it.intFlexible("given_qty"),
            returnedQty = it.intFlexible("returned_qty"),
            closedQty = it.intFlexible("closed_qty"),
            itemCode = it.optString("item_code"),
            itemName = it.optString("item_name"),
            categoryName = it.optString("category_name"),
            imageUrl = it.stringOrNull("image_url"),
            imageUrls = it.itemImageUrls()
        )
    }.orEmpty()
    val timeline = optJSONArray("timeline")?.objects()?.map {
        BookingTimelineEvent(
            id = it.optString("id"),
            action = it.optString("action"),
            createdAt = it.optString("created_at"),
            userName = it.stringOrNull("user_name"),
            oldValueJson = it.stringOrNull("old_value_json"),
            newValueJson = it.stringOrNull("new_value_json"),
            items = it.optJSONArray("items")?.objects()?.map { row ->
                BookingHistoryItem(
                    itemName = row.optString("item_name"),
                    quantity = row.intFlexible("quantity"),
                    kind = row.optString("kind")
                )
            }.orEmpty()
        )
    }.orEmpty()
    return BookingDetailData(booking, items, timeline)
}

fun JSONObject.toBookingSaveResult(): BookingSaveResult = BookingSaveResult(
    id = optString("id"),
    bookingNo = optString("bookingNo"),
    message = optString("message").ifBlank { "Booking saved." },
    duplicate = boolFlexible("duplicate", false)
)

fun JSONObject.toBookingActionResult(defaultMessage: String): BookingActionResult =
    BookingActionResult(
        message = optString("message").ifBlank { defaultMessage },
        billingHandoffBookingId = stringOrNull("billingHandoffBookingId"),
        billingBillId = stringOrNull("billingBillId"),
        billingBillStatus = stringOrNull("billingBillStatus"),
        billingDraftError = stringOrNull("billingDraftError")
    )
