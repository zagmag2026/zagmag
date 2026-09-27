package com.nimsdeveloper.zhagmagdresses.admin

import org.json.JSONObject

internal data class Screen8Option(
    val value: String,
    val label: String
)

internal data class Screen8CategoryOption(
    val id: String,
    val name: String
)

internal data class Screen8CategoryFieldOption(
    val id: String,
    val categoryId: String,
    val name: String,
    val type: String,
    val options: List<String>
)

internal data class Screen8StaffOption(
    val id: String,
    val name: String,
    val role: String
)

internal data class Screen8ReportConfig(
    val type: String = "OPERATIONS_OVERVIEW",
    val dateBasis: String = "BOOKING_DATE",
    val datePreset: String = "TODAY",
    val fromDate: String = "",
    val toDate: String = "",
    val categoryId: String = "",
    val itemSearch: String = "",
    val customerSearch: String = "",
    val status: String = "",
    val staffUserId: String = "",
    val grouping: String = "NONE",
    val sort: String = "NEWEST",
    val search: String = "",
    val dashboardFilter: String = "",
    val customFilters: Map<String, String> = emptyMap()
)

internal data class Screen8Preset(
    val id: String,
    val ownerUserId: String,
    val ownerName: String,
    val name: String,
    val visibility: String,
    val config: Screen8ReportConfig,
    val updatedAt: String?,
    val canEdit: Boolean
)

internal data class Screen8Bootstrap(
    val today: String,
    val categories: List<Screen8CategoryOption>,
    val categoryFields: List<Screen8CategoryFieldOption>,
    val staff: List<Screen8StaffOption>,
    val statuses: List<String>,
    val presets: List<Screen8Preset>
)

internal data class Screen8Column(
    val key: String,
    val label: String
)

internal data class Screen8SummaryItem(
    val label: String,
    val value: String
)

internal data class Screen8ReportRow(
    val values: Map<String, String>
) {
    operator fun get(key: String): String = values[key].orEmpty()
}

internal data class Screen8ReportPage(
    val type: String,
    val title: String,
    val columns: List<Screen8Column>,
    val rows: List<Screen8ReportRow>,
    val summary: List<Screen8SummaryItem>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
    val appliedFilters: Screen8ReportConfig
)

internal data class Screen8ReportDefinition(
    val section: String,
    val option: Screen8Option,
    val ownerOnly: Boolean = false
)

internal val screen8Sections = listOf(
    Screen8Option("OVERVIEW", "Overview"),
    Screen8Option("OPERATIONS", "Operations"),
    Screen8Option("INVENTORY", "Inventory"),
    Screen8Option("CUSTOMERS", "Customers"),
    Screen8Option("BILLING", "Billing"),
    Screen8Option("ACTIVITY", "Activity")
)

internal val screen8ReportDefinitions = listOf(
    Screen8ReportDefinition("OVERVIEW", Screen8Option("OPERATIONS_OVERVIEW", "Operations Overview")),

    Screen8ReportDefinition("OPERATIONS", Screen8Option("BOOKINGS", "Bookings")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("UPCOMING_BOOKINGS", "Upcoming Bookings")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("CANCELLED_BOOKINGS", "Cancelled Bookings")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("PICKUPS", "Pickup Report")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("MISSED_PICKUPS", "Missed Pickup")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("RETURNS", "Return Report")),
    Screen8ReportDefinition("OPERATIONS", Screen8Option("OVERDUE", "Overdue Return")),

    Screen8ReportDefinition("INVENTORY", Screen8Option("AVAILABILITY", "Availability")),
    Screen8ReportDefinition("INVENTORY", Screen8Option("CURRENTLY_OUT", "Currently Out")),
    Screen8ReportDefinition("INVENTORY", Screen8Option("ITEM_UTILIZATION", "Item Utilization")),
    Screen8ReportDefinition("INVENTORY", Screen8Option("LOW_USE_ITEMS", "Low-use / Idle Items")),
    Screen8ReportDefinition("INVENTORY", Screen8Option("ITEM_HISTORY", "Item Rental History")),
    Screen8ReportDefinition("INVENTORY", Screen8Option("CATEGORY_STOCK", "Category Summary")),

    Screen8ReportDefinition("CUSTOMERS", Screen8Option("CUSTOMER_HISTORY", "Customer Rental History")),
    Screen8ReportDefinition("CUSTOMERS", Screen8Option("ACTIVE_RENTALS", "Active Rentals")),
    Screen8ReportDefinition("CUSTOMERS", Screen8Option("FREQUENT_CUSTOMERS", "Frequent Customers")),
    Screen8ReportDefinition("CUSTOMERS", Screen8Option("NEW_RETURNING_CUSTOMERS", "New vs Returning")),
    Screen8ReportDefinition("CUSTOMERS", Screen8Option("CUSTOMER_EXCEPTIONS", "Customer Exceptions")),

    Screen8ReportDefinition("BILLING", Screen8Option("BILLING_OVERVIEW", "Billing Overview"), ownerOnly = true),
    Screen8ReportDefinition("BILLING", Screen8Option("BILLS_REPORT", "Bills Report"), ownerOnly = true),
    Screen8ReportDefinition("BILLING", Screen8Option("PENDING_BALANCE", "Pending Balance"), ownerOnly = true),
    Screen8ReportDefinition("BILLING", Screen8Option("FULL_AMOUNT_RECEIVED", "Full Amount Received"), ownerOnly = true),

    Screen8ReportDefinition("ACTIVITY", Screen8Option("STAFF_ACTIVITY", "Staff Activity")),
    Screen8ReportDefinition("ACTIVITY", Screen8Option("WHATSAPP_ACTIVITY", "WhatsApp Activity")),
    Screen8ReportDefinition("ACTIVITY", Screen8Option("AUDIT_REPORT", "Audit Report"), ownerOnly = true),
    Screen8ReportDefinition("ACTIVITY", Screen8Option("EXCEPTIONS", "Exception Report"))
)

internal val screen8ReportTypes: List<Screen8Option>
    get() = screen8ReportDefinitions.map { it.option }

internal fun screen8ReportOptionsForSection(section: String, isOwner: Boolean): List<Screen8Option> =
    screen8ReportDefinitions
        .filter { it.section == section.uppercase() && (isOwner || !it.ownerOnly) }
        .map { it.option }

internal fun screen8SectionForType(type: String): String =
    screen8ReportDefinitions.firstOrNull { it.option.value == type.uppercase() }?.section ?: "OVERVIEW"

internal fun screen8DefaultTypeForSection(section: String, isOwner: Boolean): String =
    screen8ReportOptionsForSection(section, isOwner).firstOrNull()?.value ?: "OPERATIONS_OVERVIEW"

internal val screen8DatePresets = listOf(
    Screen8Option("TODAY", "Today"),
    Screen8Option("YESTERDAY", "Yesterday"),
    Screen8Option("THIS_WEEK", "This Week"),
    Screen8Option("THIS_MONTH", "This Month"),
    Screen8Option("CUSTOM", "Custom")
)

internal val screen8SortOptions = listOf(
    Screen8Option("NEWEST", "Newest"),
    Screen8Option("OLDEST", "Oldest"),
    Screen8Option("NAME", "Name"),
    Screen8Option("QUANTITY", "Quantity"),
    Screen8Option("DUE_ASC", "Due Soonest"),
    Screen8Option("DUE_DESC", "Due Latest")
)

internal fun screen8SortOptionsFor(type: String): List<Screen8Option> =
    if (type.uppercase() in setOf("BILLING_OVERVIEW", "BILLS_REPORT", "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED")) {
        listOf(
            Screen8Option("NEWEST", "Newest"),
            Screen8Option("OLDEST", "Oldest"),
            Screen8Option("NAME", "Customer Name"),
            Screen8Option("QUANTITY", "Net Amount High-Low")
        )
    } else {
        screen8SortOptions
    }

internal fun screen8DateBasisOptions(type: String): List<Screen8Option> = when (type.uppercase()) {
    "OPERATIONS_OVERVIEW" -> listOf(Screen8Option("BOOKING_DATE", "Selected Period"))
    "BOOKINGS" -> listOf(
        Screen8Option("BOOKING_DATE", "Booking Date"),
        Screen8Option("PICKUP_DATE", "Pickup Date"),
        Screen8Option("RETURN_DATE", "Return Date")
    )
    "UPCOMING_BOOKINGS" -> listOf(Screen8Option("PICKUP_DATE", "Pickup Date"))
    "CANCELLED_BOOKINGS" -> listOf(Screen8Option("BOOKING_DATE", "Booking Date"))
    "PICKUPS", "MISSED_PICKUPS" -> listOf(Screen8Option("PICKUP_DATE", "Pickup Date"))
    "RETURNS", "OVERDUE" -> listOf(Screen8Option("RETURN_DATE", "Return Date"))
    "AVAILABILITY" -> listOf(Screen8Option("AVAILABILITY_DATE", "Availability Period"))
    "ITEM_UTILIZATION", "LOW_USE_ITEMS", "ITEM_HISTORY",
    "CUSTOMER_HISTORY", "FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS" ->
        listOf(Screen8Option("BOOKING_DATE", "Booking Date"))
    "STAFF_ACTIVITY", "WHATSAPP_ACTIVITY", "AUDIT_REPORT" ->
        listOf(Screen8Option("ACTIVITY_DATE", "Activity Date"))
    "BILLING_OVERVIEW", "BILLS_REPORT", "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED" ->
        listOf(Screen8Option("BILL_DATE", "Bill Date"))
    "EXCEPTIONS" -> listOf(Screen8Option("DUE_DATE", "Due Date"))
    else -> emptyList()
}

internal fun screen8SupportsDates(type: String): Boolean =
    type.uppercase() !in setOf(
        "CURRENTLY_OUT", "CATEGORY_STOCK", "ACTIVE_RENTALS", "CUSTOMER_EXCEPTIONS"
    )

internal fun screen8SupportsCategory(type: String): Boolean =
    type.uppercase() !in setOf(
        "STAFF_ACTIVITY", "WHATSAPP_ACTIVITY", "AUDIT_REPORT",
        "BILLING_OVERVIEW", "BILLS_REPORT", "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED"
    )

internal fun screen8SupportsItem(type: String): Boolean =
    type.uppercase() !in setOf(
        "OPERATIONS_OVERVIEW", "CATEGORY_STOCK", "STAFF_ACTIVITY",
        "WHATSAPP_ACTIVITY", "AUDIT_REPORT",
        "BILLING_OVERVIEW", "BILLS_REPORT", "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED",
        "FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS", "CUSTOMER_EXCEPTIONS"
    )

internal fun screen8SupportsCustomer(type: String): Boolean =
    type.uppercase() !in setOf(
        "OPERATIONS_OVERVIEW", "AVAILABILITY", "ITEM_UTILIZATION",
        "LOW_USE_ITEMS", "CATEGORY_STOCK", "STAFF_ACTIVITY"
    )

internal fun screen8SupportsStatus(type: String): Boolean =
    type.uppercase() !in setOf(
        "OPERATIONS_OVERVIEW", "CANCELLED_BOOKINGS", "MISSED_PICKUPS", "OVERDUE",
        "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED",
        "AVAILABILITY", "ITEM_UTILIZATION", "LOW_USE_ITEMS",
        "CATEGORY_STOCK", "STAFF_ACTIVITY",
        "FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS", "CUSTOMER_EXCEPTIONS"
    )

internal fun screen8StatusOptions(type: String, fallback: List<String>): List<String> = when (type.uppercase()) {
    "WHATSAPP_ACTIVITY" -> listOf("PREPARED", "FAILED")
    "AUDIT_REPORT" -> listOf("CREATE", "UPDATE", "DELETE", "ARCHIVE", "RESTORE", "FINALIZE", "PAYMENT_UPDATE", "CANCEL")
    "BILLING_OVERVIEW" -> listOf("PENDING", "FULL_AMOUNT_RECEIVED")
    "BILLS_REPORT" -> listOf("PENDING", "FULL_AMOUNT_RECEIVED", "DRAFT", "FINAL", "CANCELLED")
    "PENDING_BALANCE" -> listOf("PENDING")
    "FULL_AMOUNT_RECEIVED" -> listOf("FULL_AMOUNT_RECEIVED")
    "EXCEPTIONS", "CUSTOMER_EXCEPTIONS" -> listOf("MISSED_PICKUP", "OVERDUE")
    else -> fallback.filter {
        it in setOf(
            "RESERVED", "BOOKED", "PART_PICKUP", "FULL_PICKUP", "PART_RETURN",
            "FULL_RETURN", "MISSED_PICKUP", "OVERDUE", "CANCELLED"
        )
    }
}

internal fun screen8SupportsStaff(type: String): Boolean =
    type.uppercase() !in setOf(
        "AVAILABILITY", "CURRENTLY_OUT", "ITEM_UTILIZATION", "LOW_USE_ITEMS",
        "CATEGORY_STOCK", "ACTIVE_RENTALS", "CUSTOMER_EXCEPTIONS"
    )

internal fun screen8SupportsCustomFields(type: String): Boolean =
    screen8SupportsCategory(type) && type.uppercase() != "CATEGORY_STOCK"

internal fun screen8GroupingOptions(type: String): List<Screen8Option> = when (type.uppercase()) {
    "OPERATIONS_OVERVIEW" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("STATUS", "Action")
    )
    "BOOKINGS", "UPCOMING_BOOKINGS", "CANCELLED_BOOKINGS",
    "PICKUPS", "MISSED_PICKUPS", "RETURNS", "OVERDUE" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("DATE", "Date"),
        Screen8Option("CATEGORY", "Category"),
        Screen8Option("ITEM", "Item"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STAFF", "Staff"),
        Screen8Option("STATUS", "Status")
    )
    "AVAILABILITY", "ITEM_UTILIZATION", "LOW_USE_ITEMS" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("CATEGORY", "Category"),
        Screen8Option("ITEM", "Item")
    )
    "CURRENTLY_OUT", "ITEM_HISTORY", "ACTIVE_RENTALS" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("CATEGORY", "Category"),
        Screen8Option("ITEM", "Item"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STATUS", "Status")
    )
    "CATEGORY_STOCK" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("CATEGORY", "Category")
    )
    "BILLING_OVERVIEW", "BILLS_REPORT", "PENDING_BALANCE", "FULL_AMOUNT_RECEIVED" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("DATE", "Date"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STAFF", "Staff"),
        Screen8Option("STATUS", "Status")
    )
    "CUSTOMER_HISTORY" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("CATEGORY", "Category"),
        Screen8Option("ITEM", "Item"),
        Screen8Option("STATUS", "Status")
    )
    "FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS", "CUSTOMER_EXCEPTIONS" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STATUS", "Status")
    )
    "STAFF_ACTIVITY" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("STAFF", "Staff")
    )
    "WHATSAPP_ACTIVITY" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("DATE", "Date"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STAFF", "Staff"),
        Screen8Option("STATUS", "Outcome")
    )
    "AUDIT_REPORT" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("DATE", "Date"),
        Screen8Option("STAFF", "User"),
        Screen8Option("STATUS", "Action")
    )
    "EXCEPTIONS" -> listOf(
        Screen8Option("NONE", "None"),
        Screen8Option("DATE", "Due Date"),
        Screen8Option("CATEGORY", "Category"),
        Screen8Option("ITEM", "Item"),
        Screen8Option("CUSTOMER", "Customer"),
        Screen8Option("STATUS", "Exception")
    )
    else -> listOf(Screen8Option("NONE", "None"))
}

internal fun Screen8ReportConfig.toJson(): JSONObject {
    val custom = JSONObject()
    customFilters.forEach { (key, value) ->
        if (key.isNotBlank() && value.isNotBlank()) custom.put(key, value)
    }
    return JSONObject()
        .put("type", type)
        .put("dateBasis", dateBasis)
        .put("datePreset", datePreset)
        .put("fromDate", fromDate)
        .put("toDate", toDate)
        .put("categoryId", categoryId)
        .put("itemSearch", itemSearch)
        .put("customerSearch", customerSearch)
        .put("status", status)
        .put("staffUserId", staffUserId)
        .put("grouping", grouping)
        .put("sort", sort)
        .put("search", search)
        .put("dashboardFilter", dashboardFilter)
        .put("customFilters", custom)
}

internal fun JSONObject.toScreen8ReportConfig(): Screen8ReportConfig {
    val custom = linkedMapOf<String, String>()
    optJSONObject("customFilters")?.let { objectValue ->
        val keys = objectValue.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = objectValue.optString(key).trim()
            if (key.isNotBlank() && value.isNotBlank()) custom[key] = value
        }
    }
    return Screen8ReportConfig(
        type = optString("type", "OPERATIONS_OVERVIEW").uppercase(),
        dateBasis = optString("dateBasis", "BOOKING_DATE").uppercase(),
        datePreset = optString("datePreset", "CUSTOM").uppercase(),
        fromDate = optString("fromDate"),
        toDate = optString("toDate"),
        categoryId = optString("categoryId"),
        itemSearch = optString("itemSearch"),
        customerSearch = optString("customerSearch"),
        status = optString("status").uppercase(),
        staffUserId = optString("staffUserId"),
        grouping = optString("grouping", "NONE").uppercase(),
        sort = optString("sort", "NEWEST").uppercase(),
        search = optString("search"),
        dashboardFilter = optString("dashboardFilter").uppercase(),
        customFilters = custom
    )
}

internal fun JSONObject.toScreen8Bootstrap(): Screen8Bootstrap {
    val categories = buildList {
        val array = optJSONArray("categories")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            add(Screen8CategoryOption(row.optString("id"), row.optString("name")))
        }
    }
    val categoryFields = buildList {
        val array = optJSONArray("categoryFields")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val options = buildList {
                val values = row.optJSONArray("options")
                if (values != null) for (optionIndex in 0 until values.length()) {
                    values.optString(optionIndex).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
            add(
                Screen8CategoryFieldOption(
                    id = row.optString("id"),
                    categoryId = row.optString("categoryId"),
                    name = row.optString("name"),
                    type = row.optString("type").uppercase(),
                    options = options
                )
            )
        }
    }
    val staff = buildList {
        val array = optJSONArray("staff")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            add(Screen8StaffOption(row.optString("id"), row.optString("name"), row.optString("role")))
        }
    }
    val statuses = buildList {
        val array = optJSONArray("statuses")
        if (array != null) for (index in 0 until array.length()) {
            array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
        }
    }
    val presets = buildList {
        val array = optJSONArray("presets")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            add(
                Screen8Preset(
                    id = row.optString("id"),
                    ownerUserId = row.optString("ownerUserId"),
                    ownerName = row.optString("ownerName"),
                    name = row.optString("name"),
                    visibility = row.optString("visibility", "MY"),
                    config = (row.optJSONObject("config") ?: JSONObject()).toScreen8ReportConfig(),
                    updatedAt = row.optString("updatedAt").takeIf(String::isNotBlank),
                    canEdit = row.optBoolean("canEdit", false)
                )
            )
        }
    }
    return Screen8Bootstrap(
        today = optString("today"),
        categories = categories,
        categoryFields = categoryFields,
        staff = staff,
        statuses = statuses,
        presets = presets
    )
}

internal fun JSONObject.toScreen8ReportPage(): Screen8ReportPage {
    val columns = buildList {
        val array = optJSONArray("columns")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            add(Screen8Column(row.optString("key"), row.optString("label")))
        }
    }
    val rows = buildList {
        val array = optJSONArray("rows")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val values = linkedMapOf<String, String>()
            val keys = row.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val raw = row.opt(key)
                values[key] = when (raw) {
                    null, JSONObject.NULL -> ""
                    is Boolean -> if (raw) "Yes" else "No"
                    else -> raw.toString()
                }
            }
            add(Screen8ReportRow(values))
        }
    }
    val summary = buildList {
        val array = optJSONArray("summary")
        if (array != null) for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            add(Screen8SummaryItem(row.optString("label"), row.optString("value")))
        }
    }
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val filters = optJSONObject("appliedFilters") ?: JSONObject()
    return Screen8ReportPage(
        type = optString("type", "OPERATIONS_OVERVIEW"),
        title = optString("title", "Report"),
        columns = columns,
        rows = rows,
        summary = summary,
        page = pagination.optInt("page", 1).coerceAtLeast(1),
        pageSize = pagination.optInt("pageSize", 10).coerceAtLeast(1),
        total = pagination.optInt("total", rows.size).coerceAtLeast(0),
        totalPages = pagination.optInt("pages", 1).coerceAtLeast(1),
        appliedFilters = filters.toScreen8ReportConfig()
    )
}

internal fun screen8StatusLabel(value: String): String = when (value.uppercase()) {
    "RESERVED" -> "Reserved"
    "BOOKED" -> "Booked"
    "PART_PICKUP", "PARTIALLY_GIVEN" -> "Part Picked Up"
    "FULL_PICKUP", "GIVEN" -> "Full Picked Up"
    "PART_RETURN", "PARTIALLY_RETURNED" -> "Part Return"
    "FULL_RETURN", "RETURNED" -> "Full Returned"
    "MISSED_PICKUP" -> "Missed Pickup"
    "OVERDUE" -> "Overdue"
    "PICKUP_PENDING" -> "Pickup Pending"
    "RETURN_PENDING" -> "Return Pending"
    "CANCELLED" -> "Cancelled"
    "AVAILABLE" -> "Available"
    "PARTIAL" -> "Partially Available"
    "FULLY_BOOKED" -> "Fully Booked"
    "NEW" -> "New"
    "RETURNING" -> "Returning"
    "PREPARED" -> "Prepared"
    "FAILED" -> "Failed"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
