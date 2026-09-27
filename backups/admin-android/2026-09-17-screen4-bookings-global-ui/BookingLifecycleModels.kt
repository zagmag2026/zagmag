package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONObject

data class BookingOptionCustomer(
    val id: String,
    val name: String,
    val mobile: String
)

data class BookingOptionItem(
    val id: String,
    val itemCode: String,
    val itemName: String,
    val categoryId: String,
    val categoryName: String,
    val totalQuantity: Int,
    val imageUrl: String? = null
)

data class BookingBootstrapData(
    val customers: List<BookingOptionCustomer>,
    val items: List<BookingOptionItem>
)

data class BookingListItemPreview(
    val itemName: String,
    val imageUrl: String?,
    val quantity: Int
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
    val notes: String?,
    val customerId: String,
    val customerName: String,
    val customerMobile: String,
    val itemsSummary: String,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val itemPreviews: List<BookingListItemPreview> = emptyList(),
    val itemCount: Int = 0
)

data class BookingDetailItem(
    val bookingItemId: String,
    val itemId: String,
    val bookedQty: Int,
    val givenQty: Int,
    val returnedQty: Int,
    val itemCode: String,
    val itemName: String,
    val categoryName: String,
    val imageUrl: String? = null
) {
    val remainingToGive: Int get() = (bookedQty - givenQty).coerceAtLeast(0)
    val pendingToReturn: Int get() = (givenQty - returnedQty).coerceAtLeast(0)
}

data class BookingDetailData(
    val booking: LifecycleBookingSummary,
    val items: List<BookingDetailItem>
)

data class BookingRequestLine(val itemId: String, val quantity: Int)
data class BookingActionLine(val bookingItemId: String, val quantity: Int)

data class BookingSaveResult(
    val id: String,
    val bookingNo: String,
    val message: String,
    val duplicate: Boolean
)

data class BookingActionResult(val message: String)

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

private fun JSONObject.toLifecycleBookingSummary(): LifecycleBookingSummary {
    val rawStatus = optString("status").uppercase()
    val confirmationState = optString("confirmation_state").ifBlank { "BOOKED" }.uppercase()
    val itemPreviews = optJSONArray("item_previews")?.objects()?.map {
        BookingListItemPreview(
            itemName = it.optString("item_name"),
            imageUrl = it.stringOrNull("image_url"),
            quantity = it.intFlexible("quantity").coerceAtLeast(0)
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
        notes = stringOrNull("notes"),
        customerId = optString("customer_id"),
        customerName = optString("customer_name"),
        customerMobile = optString("customer_mobile"),
        itemsSummary = optString("items_summary"),
        bookedQty = intFlexible("booked_qty"),
        givenQty = intFlexible("given_qty"),
        returnedQty = intFlexible("returned_qty"),
        itemPreviews = itemPreviews,
        itemCount = if (parsedItemCount > 0) parsedItemCount else itemPreviews.size
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
        BookingOptionCustomer(it.optString("id"), it.optString("name"), it.optString("mobile"))
    }.orEmpty(),
    items = optJSONArray("items")?.objects()?.map {
        BookingOptionItem(
            id = it.optString("id"),
            itemCode = it.optString("item_code"),
            itemName = it.optString("item_name"),
            categoryId = it.optString("category_id"),
            categoryName = it.optString("category_name"),
            totalQuantity = it.intFlexible("total_quantity"),
            imageUrl = it.stringOrNull("image_url")
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
            bookedQty = it.intFlexible("booked_qty"),
            givenQty = it.intFlexible("given_qty"),
            returnedQty = it.intFlexible("returned_qty"),
            itemCode = it.optString("item_code"),
            itemName = it.optString("item_name"),
            categoryName = it.optString("category_name"),
            imageUrl = it.stringOrNull("image_url")
        )
    }.orEmpty()
    return BookingDetailData(booking, items)
}

fun JSONObject.toBookingSaveResult(): BookingSaveResult = BookingSaveResult(
    id = optString("id"),
    bookingNo = optString("bookingNo"),
    message = optString("message").ifBlank { "Booking saved." },
    duplicate = boolFlexible("duplicate", false)
)

fun JSONObject.toBookingActionResult(defaultMessage: String): BookingActionResult =
    BookingActionResult(optString("message").ifBlank { defaultMessage })
