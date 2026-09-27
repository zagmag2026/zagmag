package com.nimsdeveloper.zhagmagdresses.admin

import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import org.json.JSONObject

internal class Screen8ReportsRepository(
    private val api: ApiClient
) {
    suspend fun bootstrap(): Screen8Bootstrap =
        api.get("/api/admin/report-generator/bootstrap").toScreen8Bootstrap()

    suspend fun report(
        config: Screen8ReportConfig,
        page: Int = 1,
        pageSize: Int = 10,
        exportAll: Boolean = false
    ): Screen8ReportPage {
        val path = api.queryPath(
            "/api/admin/report-generator",
            mapOf(
                "type" to config.type,
                "dateBasis" to config.dateBasis,
                "datePreset" to config.datePreset,
                "fromDate" to config.fromDate,
                "toDate" to config.toDate,
                "categoryId" to config.categoryId,
                "itemSearch" to config.itemSearch,
                "customerSearch" to config.customerSearch,
                "status" to config.status,
                "staffUserId" to config.staffUserId,
                "grouping" to config.grouping,
                "sort" to config.sort,
                "search" to config.search,
                "dashboardFilter" to config.dashboardFilter.takeIf { it.isNotBlank() },
                "customFilters" to config.customFilters
                    .takeIf { it.isNotEmpty() }
                    ?.let { values ->
                        JSONObject().apply {
                            values.forEach { (key, value) ->
                                if (key.isNotBlank() && value.isNotBlank()) put(key, value)
                            }
                        }.toString()
                    },
                "page" to page.coerceAtLeast(1).toString(),
                "pageSize" to pageSize.coerceIn(1, 50).toString(),
                "export" to if (exportAll) "1" else null
            )
        )
        return api.get(path).toScreen8ReportPage()
    }

    suspend fun createPreset(
        name: String,
        visibility: String,
        config: Screen8ReportConfig
    ) {
        api.post(
            "/api/admin/report-presets",
            JSONObject()
                .put("name", name.trim())
                .put("visibility", visibility.uppercase())
                .put("config", config.copy(dashboardFilter = "").toJson())
        )
    }

    suspend fun updatePreset(
        id: String,
        name: String,
        visibility: String,
        config: Screen8ReportConfig,
        expectedUpdatedAt: String?
    ) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("visibility", visibility.uppercase())
            .put("config", config.copy(dashboardFilter = "").toJson())
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        api.request("PUT", "/api/admin/report-presets/$id", body)
    }

    suspend fun deletePreset(id: String) {
        api.request("DELETE", "/api/admin/report-presets/$id")
    }
}
