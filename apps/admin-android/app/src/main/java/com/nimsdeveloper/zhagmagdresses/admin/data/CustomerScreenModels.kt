package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONObject

data class CustomerSummary(
    val totalCustomers: Int = 0,
    val activeCustomers: Int = 0,
    val archivedCustomers: Int = 0
)

data class CustomerBookingOption(
    val id: String,
    val bookingNo: String,
    val displayStatus: String,
    val pickupDate: String,
    val returnDate: String
)

data class CustomerCardRow(
    val id: String,
    val name: String,
    val mobile: String,
    val alternateMobile: String?,
    val address: String?,
    val archivedAt: String?,
    val totalBookings: Int,
    val activeBookings: Int,
    val bookingOptions: List<CustomerBookingOption>,
    val updatedAt: String? = null
)

data class CustomerPageBundle(
    val items: List<CustomerCardRow>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
    val summary: CustomerSummary
)

data class CustomerMutationResult(
    val id: String?,
    val message: String
)

object WhatsAppContext {
    const val GENERAL_INQUIRY = "GENERAL_INQUIRY"
    const val BOOKING_CONFIRMATION = "BOOKING_CONFIRMATION"
    const val RESERVATION_CONFIRMATION = "RESERVATION_CONFIRMATION"
    const val RESERVATION_CANCELLED = "RESERVATION_CANCELLED"
    const val BOOKING_UPDATED = "BOOKING_UPDATED"
    const val BOOKING_CANCELLED = "BOOKING_CANCELLED"
    const val PICKUP_READY = "PICKUP_READY"
    const val PICKUP_DUE_TODAY = "PICKUP_DUE_TODAY"
    const val PICKUP_REMINDER = "PICKUP_REMINDER"
    const val PENDING_PICKUP_REMINDER = "PENDING_PICKUP_REMINDER"
    const val MISSED_PICKUP_REMINDER = "MISSED_PICKUP_REMINDER"
    const val PICKUP_DONE = "PICKUP_DONE"
    const val PART_PICKUP_DONE = "PART_PICKUP_DONE"
    const val RETURN_DUE_TODAY = "RETURN_DUE_TODAY"
    const val RETURN_REMINDER = "RETURN_REMINDER"
    const val PENDING_RETURN_REMINDER = "PENDING_RETURN_REMINDER"
    const val OVERDUE_REMINDER = "OVERDUE_REMINDER"
    const val OVERDUE_FINAL_REMINDER = "OVERDUE_FINAL_REMINDER"
    const val RETURN_DONE = "RETURN_DONE"
    const val PART_RETURN_DONE = "PART_RETURN_DONE"
    const val ITEM_AVAILABILITY_REPLY = "ITEM_AVAILABILITY_REPLY"
    const val BOOKING_COMPLETED = "BOOKING_COMPLETED"
    const val THANK_YOU = "THANK_YOU"
}

data class WhatsAppComposeResult(
    val mobile: String,
    val message: String,
    val templateKey: String,
    val templateName: String = "",
    val linkedAction: String = "",
    val languageMode: String = "BOTH"
)

data class WhatsAppTemplateOption(
    val templateKey: String,
    val templateName: String,
    val linkedAction: String,
    val languageMode: String,
    val message: String
)

data class WhatsAppTemplateBundle(
    val customerName: String,
    val mobile: String,
    val statusGroup: String,
    val statusGroupLabel: String,
    val templates: List<WhatsAppTemplateOption>
)

private fun JSONObject.toCustomerBookingOptionV3() = CustomerBookingOption(
    id = optString("id"),
    bookingNo = optString("booking_no"),
    displayStatus = optString("display_status").ifBlank { optString("status") },
    pickupDate = optString("pickup_date"),
    returnDate = optString("return_date")
)

private fun JSONObject.toCustomerCardRowV3(): CustomerCardRow {
    val options = optJSONArray("active_booking_options")?.objects()?.map { it.toCustomerBookingOptionV3() }.orEmpty()
    return CustomerCardRow(
        id = optString("id"),
        name = optString("name"),
        mobile = optString("mobile"),
        alternateMobile = stringOrNull("alternate_mobile"),
        address = stringOrNull("address"),
        archivedAt = stringOrNull("archived_at"),
        updatedAt = stringOrNull("updated_at"),
        totalBookings = intFlexible("total_bookings"),
        activeBookings = intFlexible("active_bookings"),
        bookingOptions = options
    )
}

fun JSONObject.toCustomerPageBundle(): CustomerPageBundle {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val summary = optJSONObject("summary") ?: JSONObject()
    return CustomerPageBundle(
        items = optJSONArray("customers")?.objects()?.map { it.toCustomerCardRowV3() }.orEmpty(),
        page = pagination.intFlexible("page").coerceAtLeast(1),
        totalPages = pagination.intFlexible("totalPages").coerceAtLeast(1),
        total = pagination.intFlexible("total"),
        summary = CustomerSummary(
            totalCustomers = summary.intFlexible("totalCustomers"),
            activeCustomers = summary.intFlexible("activeCustomers"),
            archivedCustomers = summary.intFlexible("archivedCustomers")
        )
    )
}

fun JSONObject.toCustomerMutationResult(): CustomerMutationResult = CustomerMutationResult(
    id = stringOrNull("id"),
    message = optString("message").ifBlank { "Customer saved." }
)

fun JSONObject.toWhatsAppComposeResult(): WhatsAppComposeResult = WhatsAppComposeResult(
    mobile = optString("mobile"),
    message = optString("message"),
    templateKey = optString("templateKey"),
    templateName = optString("templateName"),
    linkedAction = optString("linkedAction"),
    languageMode = optString("languageMode").ifBlank { "ALL" }
)

fun JSONObject.toWhatsAppTemplateBundle(): WhatsAppTemplateBundle = WhatsAppTemplateBundle(
    customerName = optString("customerName"),
    mobile = optString("mobile"),
    statusGroup = optString("statusGroup"),
    statusGroupLabel = optString("statusGroupLabel"),
    templates = optJSONArray("templates")?.objects()?.map { row ->
        WhatsAppTemplateOption(
            templateKey = row.optString("templateKey"),
            templateName = row.optString("templateName"),
            linkedAction = row.optString("linkedAction"),
            languageMode = row.optString("languageMode").ifBlank { "BOTH" },
            message = row.optString("message")
        )
    }.orEmpty()
)
