package com.nimsdeveloper.zhagmagdresses.admin

import com.nimsdeveloper.zhagmagdresses.admin.data.boolFlexible
import com.nimsdeveloper.zhagmagdresses.admin.data.intFlexible
import com.nimsdeveloper.zhagmagdresses.admin.data.objects
import com.nimsdeveloper.zhagmagdresses.admin.data.stringOrNull
import org.json.JSONObject

internal val SCREEN10_WORKING_DAYS = listOf(
    "MONDAY" to "Monday",
    "TUESDAY" to "Tuesday",
    "WEDNESDAY" to "Wednesday",
    "THURSDAY" to "Thursday",
    "FRIDAY" to "Friday",
    "SATURDAY" to "Saturday",
    "SUNDAY" to "Sunday"
)

internal data class Screen10WorkingDay(
    val isOpen: Boolean = false,
    val openTime: String = "09:00",
    val closeTime: String = "20:00"
)

internal fun screen10DefaultWorkingHours(): Map<String, Screen10WorkingDay> =
    SCREEN10_WORKING_DAYS.associate { (key, _) -> key to Screen10WorkingDay() }

internal data class Screen10SiteSettings(
    val shopName: String = "",
    val websiteTitle: String = "",
    val websiteUrl: String = "",
    val logoUrl: String = "",
    val logoPublicId: String = "",
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val defaultLanguage: String = "GU",
    val dateFormat: String = "DD-MM-YYYY",
    val workingHours: Map<String, Screen10WorkingDay> = screen10DefaultWorkingHours(),
    val publicCatalogEnabled: Boolean = true,
    val showWhatsApp: Boolean = true,
    val showCall: Boolean = true,
    val availabilityMode: String = "EXACT",
    val fewLeftThreshold: Int = 2,
    val whatsappTemplateLanguage: String = "BOTH"
)

internal data class Screen10UploadedLogo(val url: String, val publicId: String)

internal data class Screen10WhatsAppTemplate(
    val id: String,
    val templateKey: String,
    val templateName: String,
    val messageGu: String,
    val messageEn: String,
    val languageMode: String,
    val linkedAction: String?,
    val active: Boolean,
    val updatedAt: String?
)

internal data class Screen10AuditUser(
    val id: String,
    val name: String,
    val role: String
)

internal data class Screen10Bootstrap(
    val settings: Screen10SiteSettings,
    val templates: List<Screen10WhatsAppTemplate>,
    val users: List<Screen10AuditUser>,
    val auditModules: List<String>,
    val auditActions: List<String>,
    val placeholders: List<String>,
    val placeholderRegistry: Map<String, List<String>>,
    val placeholderSources: Map<String, String>,
    val languageModes: List<String>,
    val linkedActions: List<String>,
    val updatedAt: String?
)

internal data class Screen10AuditLog(
    val id: String,
    val userId: String?,
    val userName: String?,
    val userRole: String?,
    val action: String,
    val module: String,
    val recordId: String?,
    val oldValueJson: String?,
    val newValueJson: String?,
    val createdAt: String
)

internal data class Screen10AuditPage(
    val logs: List<Screen10AuditLog>,
    val page: Int,
    val totalPages: Int,
    val total: Int
)

internal fun JSONObject.toScreen10Bootstrap(): Screen10Bootstrap {
    val settingsJson = optJSONObject("settings") ?: JSONObject()
    val workingHoursJson = settingsJson.optJSONObject("workingHours")
    val workingHours = SCREEN10_WORKING_DAYS.associate { (dayKey, _) ->
        val dayJson = workingHoursJson?.optJSONObject(dayKey)
        val openTime = dayJson?.optString("openTime").orEmpty().takeIf { it.matches(Regex("^\\d{2}:\\d{2}$")) } ?: "09:00"
        val closeTime = dayJson?.optString("closeTime").orEmpty().takeIf { it.matches(Regex("^\\d{2}:\\d{2}$")) } ?: "20:00"
        dayKey to Screen10WorkingDay(
            isOpen = dayJson?.boolFlexible("isOpen", false) ?: false,
            openTime = openTime,
            closeTime = closeTime
        )
    }
    val settings = Screen10SiteSettings(
        shopName = settingsJson.optString("shopName"),
        websiteTitle = settingsJson.optString("websiteTitle"),
        websiteUrl = settingsJson.optString("websiteUrl"),
        logoUrl = settingsJson.optString("logoUrl"),
        contactNumber = settingsJson.optString("contactNumber"),
        whatsappNumber = settingsJson.optString("whatsappNumber"),
        address = settingsJson.optString("address"),
        defaultLanguage = settingsJson.optString("defaultLanguage").ifBlank { "GU" },
        dateFormat = settingsJson.optString("dateFormat").ifBlank { "DD-MM-YYYY" },
        workingHours = workingHours,
        publicCatalogEnabled = settingsJson.boolFlexible("publicCatalogEnabled", true),
        showWhatsApp = settingsJson.boolFlexible("showWhatsApp", true),
        showCall = settingsJson.boolFlexible("showCall", true),
        availabilityMode = settingsJson.optString("availabilityMode").ifBlank { "EXACT" }.uppercase(),
        fewLeftThreshold = settingsJson.intFlexible("fewLeftThreshold").takeIf { it > 0 }?.coerceIn(1, 20) ?: 2,
        whatsappTemplateLanguage = settingsJson.optString("whatsappTemplateLanguage").ifBlank { "BOTH" }.uppercase()
            .let { if (it == "ALL") "BOTH" else it }
    )
    fun stringArray(key: String): List<String> = buildList {
        val array = optJSONArray(key)
        if (array != null) {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
    fun stringMap(key: String): Map<String, String> {
        val objectValue = optJSONObject(key) ?: return emptyMap()
        return buildMap {
            val keys = objectValue.keys()
            while (keys.hasNext()) {
                val entryKey = keys.next()
                objectValue.optString(entryKey).trim().takeIf { it.isNotBlank() }?.let { put(entryKey, it) }
            }
        }
    }
    fun stringListMap(key: String): Map<String, List<String>> {
        val objectValue = optJSONObject(key) ?: return emptyMap()
        return buildMap {
            val keys = objectValue.keys()
            while (keys.hasNext()) {
                val entryKey = keys.next()
                val array = objectValue.optJSONArray(entryKey)
                val values = buildList {
                    if (array != null) {
                        for (index in 0 until array.length()) {
                            array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                put(entryKey, values)
            }
        }
    }
    val templates = optJSONArray("templates")?.objects().orEmpty().map { row ->
        Screen10WhatsAppTemplate(
            id = row.optString("id"),
            templateKey = row.optString("template_key"),
            templateName = row.optString("template_name"),
            messageGu = row.stringOrNull("message_gu") ?: row.optString("message_text"),
            messageEn = row.stringOrNull("message_en").orEmpty(),
            languageMode = row.optString("language_mode").ifBlank { "ALL" }.uppercase(),
            linkedAction = row.stringOrNull("linked_action")?.uppercase(),
            active = row.boolFlexible("is_active", true),
            updatedAt = row.stringOrNull("updated_at")
        )
    }
    val users = optJSONArray("users")?.objects().orEmpty().map { row ->
        Screen10AuditUser(
            id = row.optString("id"),
            name = row.optString("name"),
            role = row.optString("role")
        )
    }
    return Screen10Bootstrap(
        settings = settings,
        templates = templates,
        users = users,
        auditModules = stringArray("auditModules"),
        auditActions = stringArray("auditActions"),
        placeholders = stringArray("templatePlaceholders"),
        placeholderRegistry = stringListMap("templatePlaceholderRegistry"),
        placeholderSources = stringMap("templatePlaceholderSources"),
        languageModes = stringArray("templateLanguageModes").ifEmpty { listOf("GUJARATI", "ENGLISH", "BOTH") },
        linkedActions = stringArray("templateLinkedActions"),
        updatedAt = stringOrNull("updatedAt")
    )
}

internal fun JSONObject.toScreen10AuditPage(): Screen10AuditPage {
    val pagination = optJSONObject("pagination") ?: JSONObject()
    val logs = optJSONArray("logs")?.objects().orEmpty().map { row ->
        Screen10AuditLog(
            id = row.optString("id"),
            userId = row.stringOrNull("user_id"),
            userName = row.stringOrNull("user_name"),
            userRole = row.stringOrNull("user_role"),
            action = row.optString("action"),
            module = row.optString("module"),
            recordId = row.stringOrNull("record_id"),
            oldValueJson = row.stringOrNull("old_value_json"),
            newValueJson = row.stringOrNull("new_value_json"),
            createdAt = row.optString("created_at")
        )
    }
    return Screen10AuditPage(
        logs = logs,
        page = pagination.intFlexible("page").coerceAtLeast(1),
        totalPages = pagination.intFlexible("pages").coerceAtLeast(1),
        total = pagination.intFlexible("total")
    )
}
