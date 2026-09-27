package com.nimsdeveloper.zhagmagdresses.admin

import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import org.json.JSONArray
import org.json.JSONObject

internal class Screen9UsersRepository(private val api: ApiClient) {
    suspend fun users(
        page: Int,
        search: String,
        role: String,
        status: String,
        pageSize: Int = 10
    ): Screen9UsersPage =
        api.get(
            api.queryPath(
                "/api/admin/users",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.coerceIn(1, 10).toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() },
                    "role" to role.trim().takeIf { it.isNotBlank() && !it.equals("ALL", true) },
                    "status" to status.trim().lowercase()
                )
            )
        ).toScreen9UsersPage()

    suspend fun create(
        name: String,
        mobile: String,
        role: UserRole,
        password: String,
        staffPermissions: Set<String>
    ) {
        api.post(
            "/api/admin/users",
            JSONObject()
                .put("name", name.trim())
                .put("mobile", mobile.filter(Char::isDigit))
                .put("role", role.name)
                .put("password", password)
                .put("staffPermissions", JSONArray(staffPermissions.toList().sorted()))
        )
    }

    suspend fun update(
        id: String,
        name: String,
        mobile: String,
        role: UserRole,
        active: Boolean,
        staffPermissions: Set<String>,
        expectedUpdatedAt: String?
    ) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("mobile", mobile.filter(Char::isDigit))
            .put("role", role.name)
            .put("isActive", active)
            .put("staffPermissions", JSONArray(staffPermissions.toList().sorted()))
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        api.request("PUT", "/api/admin/users/$id", body)
    }

    suspend fun resetPassword(id: String, password: String) {
        api.post(
            "/api/admin/users/$id/password",
            JSONObject().put("password", password)
        )
    }

    suspend fun archive(id: String) {
        api.post("/api/admin/users/$id/archive")
    }

    suspend fun restore(id: String) {
        api.post("/api/admin/users/$id/restore")
    }
}
