package com.nimsdeveloper.zhagmagdresses.admin

import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.data.boolFlexible
import com.nimsdeveloper.zhagmagdresses.admin.data.intFlexible
import com.nimsdeveloper.zhagmagdresses.admin.data.objects
import com.nimsdeveloper.zhagmagdresses.admin.data.stringOrNull
import org.json.JSONObject

internal data class Screen9ManagedUser(
    val id: String,
    val name: String,
    val mobile: String,
    val role: UserRole,
    val active: Boolean,
    val archivedAt: String?,
    val lastLoginAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val staffPermissions: Set<String>,
    val bookingsCreated: Int,
    val pickupsHandled: Int,
    val returnsHandled: Int
)

internal data class Screen9UsersPage(
    val users: List<Screen9ManagedUser>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
    val permissionOptions: List<String>
)

internal fun JSONObject.toScreen9UsersPage(): Screen9UsersPage {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val options = buildList {
        val array = optJSONArray("permissionOptions")
        if (array != null) {
            for (index in 0 until array.length()) {
                array.optString(index).trim().uppercase().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
    val rows = optJSONArray("users")?.objects().orEmpty().map { row ->
        val role = if (row.optString("role").equals("OWNER", true)) UserRole.OWNER else UserRole.STAFF
        val permissions = buildSet {
            val array = row.optJSONArray("staffPermissions")
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().uppercase().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        Screen9ManagedUser(
            id = row.optString("id"),
            name = row.optString("name"),
            mobile = row.optString("mobile"),
            role = role,
            active = row.boolFlexible("isActive", true),
            archivedAt = row.stringOrNull("archivedAt"),
            lastLoginAt = row.stringOrNull("lastLoginAt"),
            createdAt = row.stringOrNull("createdAt"),
            updatedAt = row.stringOrNull("updatedAt"),
            staffPermissions = if (role == UserRole.OWNER) emptySet() else permissions,
            bookingsCreated = row.intFlexible("bookingsCreated"),
            pickupsHandled = row.intFlexible("pickupsHandled"),
            returnsHandled = row.intFlexible("returnsHandled")
        )
    }
    return Screen9UsersPage(
        users = rows,
        page = pagination.intFlexible("page").coerceAtLeast(1),
        totalPages = pagination.intFlexible("pages").coerceAtLeast(1),
        total = pagination.intFlexible("total"),
        permissionOptions = options
    )
}
