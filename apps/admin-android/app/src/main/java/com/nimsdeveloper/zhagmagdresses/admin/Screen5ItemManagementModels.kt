package com.nimsdeveloper.zhagmagdresses.admin

import org.json.JSONArray
import org.json.JSONObject

internal data class Screen5CategoryField(
    val id: String,
    val categoryId: String,
    val name: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String?,
    val options: List<String>,
    val publicVisible: Boolean,
    val active: Boolean,
    val displayOrder: Int,
    val valueCount: Int
)

internal data class Screen5Category(
    val id: String,
    val name: String,
    val codePrefix: String,
    val active: Boolean,
    val publicVisible: Boolean,
    val displayOrder: Int,
    val itemCount: Int,
    val linkedItemCount: Int,
    val fieldCount: Int,
    val fields: List<Screen5CategoryField>
)

internal data class Screen5ItemImage(
    val id: String,
    val url: String,
    val primary: Boolean,
    val displayOrder: Int
)

internal data class Screen5UploadedAsset(
    val url: String,
    val publicId: String = ""
)

internal data class Screen5Item(
    val id: String,
    val itemCode: String,
    val itemName: String,
    val categoryId: String,
    val categoryName: String,
    val totalQuantity: Int,
    val rentAmount: Int,
    val bookedQuantity: Int,
    val givenQuantity: Int,
    val availableQuantity: Int,
    val active: Boolean,
    val publicVisible: Boolean,
    val archived: Boolean,
    val fieldValues: Map<String, String>,
    val images: List<Screen5ItemImage>,
    val updatedAt: String? = null
) {
    val imageUrls: List<String>
        get() = images
            .sortedWith(compareByDescending<Screen5ItemImage> { it.primary }.thenBy { it.displayOrder })
            .map { it.url }
            .filter(String::isNotBlank)
            .distinct()

    val primaryImageUrl: String?
        get() = imageUrls.firstOrNull()
}

internal data class Screen5RelatedItem(
    val sourceItemId: String,
    val relatedItemId: String,
    val displayOrder: Int
)

internal data class Screen5Bootstrap(
    val categories: List<Screen5Category>,
    val items: List<Screen5Item>,
    val relations: List<Screen5RelatedItem>
)

internal data class Screen5ItemPage(
    val items: List<Screen5Item>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int
)

internal fun screen5JsonObjects(array: JSONArray?): List<JSONObject> = buildList {
    if (array == null) return@buildList
    for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add)
}

internal fun screen5Boolean(json: JSONObject, key: String, fallback: Boolean = false): Boolean {
    if (!json.has(key) || json.isNull(key)) return fallback
    return when (val raw = json.opt(key)) {
        is Boolean -> raw
        is Number -> raw.toInt() == 1
        is String -> raw == "1" || raw.equals("true", ignoreCase = true)
        else -> fallback
    }
}

private fun JSONObject.toScreen5Item(): Screen5Item {
    val values = screen5JsonObjects(optJSONArray("field_values")).associate { value ->
        value.optString("category_field_id") to value.optString("value_text")
    }.filterKeys { it.isNotBlank() }
    val images = screen5JsonObjects(optJSONArray("images")).map { image ->
        Screen5ItemImage(
            id = image.optString("id"),
            url = image.optString("image_url"),
            primary = screen5Boolean(image, "is_primary"),
            displayOrder = image.optInt("display_order", 0)
        )
    }.filter { it.url.isNotBlank() }
    return Screen5Item(
        id = optString("id"),
        itemCode = optString("item_code"),
        itemName = optString("item_name"),
        categoryId = optString("category_id"),
        categoryName = optString("category_name"),
        totalQuantity = optInt("total_quantity", 0),
        rentAmount = optInt("rent_amount", 0).coerceAtLeast(0),
        bookedQuantity = optInt("booked_qty", 0),
        givenQuantity = optInt("given_qty", 0),
        availableQuantity = optInt("available_qty", 0),
        active = screen5Boolean(this, "is_active", true),
        publicVisible = screen5Boolean(this, "public_visible", true),
        archived = !isNull("archived_at") && optString("archived_at").isNotBlank(),
        updatedAt = optString("updated_at").takeIf { it.isNotBlank() },
        fieldValues = values,
        images = images
    )
}

internal fun JSONObject.toScreen5ItemPage(): Screen5ItemPage {
    val parsedItems = screen5JsonObjects(optJSONArray("items")).map { it.toScreen5Item() }
    return Screen5ItemPage(
        items = parsedItems,
        page = optInt("page", 1).coerceAtLeast(1),
        pageSize = optInt("pageSize", 10).coerceAtLeast(1),
        total = optInt("total", parsedItems.size).coerceAtLeast(0),
        totalPages = optInt("totalPages", 1).coerceAtLeast(1)
    )
}

internal fun JSONObject.toScreen5Bootstrap(): Screen5Bootstrap {
    val categories = screen5JsonObjects(optJSONArray("categories")).map { category ->
        val fields = screen5JsonObjects(category.optJSONArray("fields")).map { field ->
            Screen5CategoryField(
                id = field.optString("id"),
                categoryId = field.optString("category_id"),
                name = field.optString("field_name"),
                type = field.optString("field_type").uppercase(),
                required = screen5Boolean(field, "is_required"),
                defaultValue = field.optString("default_value").takeIf { it.isNotBlank() },
                options = buildList {
                    val options = field.optJSONArray("options")
                    if (options != null) for (index in 0 until options.length()) {
                        options.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                publicVisible = screen5Boolean(field, "public_visible", true),
                active = screen5Boolean(field, "is_active", true),
                displayOrder = field.optInt("display_order", 0),
                valueCount = field.optInt("value_count", 0)
            )
        }.sortedWith(compareBy<Screen5CategoryField> { it.displayOrder }.thenBy { it.name.lowercase() })
        Screen5Category(
            id = category.optString("id"),
            name = category.optString("name"),
            codePrefix = category.optString("code_prefix"),
            active = screen5Boolean(category, "is_active", true),
            publicVisible = screen5Boolean(category, "public_visible", true),
            displayOrder = category.optInt("display_order", 0),
            itemCount = category.optInt("item_count", 0),
            linkedItemCount = category.optInt("linked_item_count", category.optInt("item_count", 0)),
            fieldCount = category.optInt("field_count", fields.size),
            fields = fields
        )
    }.sortedWith(compareBy<Screen5Category> { it.displayOrder }.thenBy { it.name.lowercase() })

    val items = screen5JsonObjects(optJSONArray("items"))
        .map { it.toScreen5Item() }
        .sortedWith(compareBy<Screen5Item> { it.categoryName.lowercase() }.thenBy { it.itemName.lowercase() })

    val relations = screen5JsonObjects(optJSONArray("relations")).mapNotNull { relation ->
        val source = relation.optString("source_item_id")
        val related = relation.optString("related_item_id")
        if (source.isBlank() || related.isBlank()) null else Screen5RelatedItem(
            sourceItemId = source,
            relatedItemId = related,
            displayOrder = relation.optInt("display_order", 0)
        )
    }
    return Screen5Bootstrap(categories, items, relations)
}
