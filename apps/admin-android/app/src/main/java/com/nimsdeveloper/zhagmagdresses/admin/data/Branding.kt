package com.nimsdeveloper.zhagmagdresses.admin.data

import android.content.Context
import org.json.JSONObject

data class AppBranding(
    val shopName: String = DEFAULT_SHOP_NAME,
    val logoUrl: String? = null
) {
    companion object {
        const val DEFAULT_SHOP_NAME = "Your Shop Name"
    }
}

data class SessionEnvelope(
    val user: SessionUser,
    val branding: AppBranding
)

class BrandingStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): AppBranding = AppBranding(
        shopName = preferences.getString(KEY_SHOP_NAME, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: AppBranding.DEFAULT_SHOP_NAME,
        logoUrl = preferences.getString(KEY_LOGO_URL, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    )

    fun save(branding: AppBranding) {
        preferences.edit()
            .putString(KEY_SHOP_NAME, branding.shopName.trim().ifBlank { AppBranding.DEFAULT_SHOP_NAME })
            .apply {
                if (branding.logoUrl.isNullOrBlank()) remove(KEY_LOGO_URL)
                else putString(KEY_LOGO_URL, branding.logoUrl.trim())
            }
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "zhagmag_admin_branding"
        const val KEY_SHOP_NAME = "shop_name"
        const val KEY_LOGO_URL = "logo_url"
    }
}

fun JSONObject.toAppBranding(): AppBranding {
    val branding = optJSONObject("branding") ?: JSONObject()
    val shop = optJSONObject("shop") ?: JSONObject()

    val shopName = branding.stringOrNull("shopNameEn")
        ?: stringOrNull("shopNameEn")
        ?: shop.stringOrNull("nameEn")
        ?: branding.stringOrNull("fallbackShopName")
        ?: AppBranding.DEFAULT_SHOP_NAME

    val logoUrl = branding.stringOrNull("logoUrl")
        ?: stringOrNull("logoUrl")
        ?: shop.stringOrNull("logoUrl")

    return AppBranding(shopName = shopName, logoUrl = logoUrl)
}

fun JSONObject.toSessionEnvelope(): SessionEnvelope = SessionEnvelope(
    user = getJSONObject("user").toSessionUser(),
    branding = toAppBranding()
)
