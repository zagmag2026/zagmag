package com.nimsdeveloper.zhagmagdresses.admin.data

import org.json.JSONObject

class CustomerScreen3Repository(private val api: ApiClient) {
    suspend fun customers(
        page: Int,
        search: String,
        sort: String,
        archived: Boolean,
        pinCustomerId: String? = null
    ): CustomerPageBundle = api.get(
        api.queryPath(
            "/api/admin/customers",
            mapOf(
                "page" to page.coerceAtLeast(1).toString(),
                "pageSize" to "10",
                "search" to search.trim().takeIf { it.isNotBlank() },
                "sort" to sort,
                "archived" to if (archived) "1" else "0",
                "pin" to pinCustomerId?.takeIf { it.isNotBlank() }
            )
        )
    ).toCustomerPageBundle()

    suspend fun create(name: String, mobile: String, alternateMobile: String?, address: String?): CustomerMutationResult =
        api.post(
            "/api/admin/customers",
            customerBody(name, mobile, alternateMobile, address)
        ).toCustomerMutationResult()

    suspend fun update(
        id: String,
        name: String,
        mobile: String,
        alternateMobile: String?,
        address: String?,
        expectedUpdatedAt: String?
    ): CustomerMutationResult {
        val body = customerBody(name, mobile, alternateMobile, address)
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        return api.request(
            "PUT",
            "/api/admin/customers/$id",
            body
        ).toCustomerMutationResult()
    }

    suspend fun archive(id: String): CustomerMutationResult =
        api.post("/api/admin/customers/$id/archive").toCustomerMutationResult()

    suspend fun restore(id: String): CustomerMutationResult =
        api.post("/api/admin/customers/$id/restore").toCustomerMutationResult()

    suspend fun deletePermanently(id: String): CustomerMutationResult =
        api.request("DELETE", "/api/admin/customers/$id").toCustomerMutationResult()

    suspend fun composeWhatsApp(
        customerId: String,
        bookingId: String?,
        context: String? = null
    ): WhatsAppComposeResult =
        api.get(
            api.queryPath(
                "/api/admin/customers/$customerId/whatsapp",
                mapOf(
                    "bookingId" to bookingId?.takeIf { it.isNotBlank() },
                    "context" to context?.takeIf { it.isNotBlank() }
                )
            )
        ).toWhatsAppComposeResult()

    suspend fun whatsAppTemplates(
        customerId: String,
        bookingId: String?
    ): WhatsAppTemplateBundle =
        api.get(
            api.queryPath(
                "/api/admin/customers/$customerId/whatsapp",
                mapOf(
                    "bookingId" to bookingId?.takeIf { it.isNotBlank() },
                    "mode" to "templates"
                )
            )
        ).toWhatsAppTemplateBundle()

    private fun customerBody(name: String, mobile: String, alternateMobile: String?, address: String?) =
        JSONObject()
            .put("name", name.trim())
            .put("mobile", mobile.filter(Char::isDigit))
            .put("alternateMobile", alternateMobile.orEmpty().filter(Char::isDigit))
            .put("address", address.orEmpty().trim())
}
