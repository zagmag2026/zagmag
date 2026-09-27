package com.nimsdeveloper.zhagmagdresses.admin

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

internal class Screen10SettingsRepository(
    private val api: ApiClient,
    private val contentResolver: ContentResolver
) {
    suspend fun bootstrap(): Screen10Bootstrap =
        api.get("/api/admin/settings/bootstrap").toScreen10Bootstrap()

    suspend fun saveSettings(settings: Screen10SiteSettings, expectedUpdatedAt: String?): String? {
        val body = JSONObject()
            .put("shopName", settings.shopName.trim())
            .put("websiteTitle", settings.websiteTitle.trim())
            .put("websiteUrl", settings.websiteUrl.trim())
            .put("logoUrl", settings.logoUrl.trim())
            .put("logoPublicId", settings.logoPublicId.trim())
            .put("contactNumber", settings.contactNumber.filter(Char::isDigit))
            .put("whatsappNumber", settings.whatsappNumber.filter(Char::isDigit))
            .put("address", settings.address.trim())
            .put("defaultLanguage", settings.defaultLanguage)
            .put("dateFormat", settings.dateFormat)
            .put(
                "workingHours",
                JSONObject().apply {
                    SCREEN10_WORKING_DAYS.forEach { (dayKey, _) ->
                        val day = settings.workingHours[dayKey] ?: Screen10WorkingDay()
                        put(
                            dayKey,
                            JSONObject()
                                .put("isOpen", day.isOpen)
                                .put("openTime", day.openTime)
                                .put("closeTime", day.closeTime)
                        )
                    }
                }
            )
            .put("publicCatalogEnabled", settings.publicCatalogEnabled)
            .put("showWhatsApp", settings.showWhatsApp)
            .put("showCall", settings.showCall)
            .put("availabilityMode", settings.availabilityMode)
            .put("fewLeftThreshold", settings.fewLeftThreshold)
            .put("whatsappTemplateLanguage", settings.whatsappTemplateLanguage.uppercase())
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        return api.request("PUT", "/api/admin/settings", body)
            .optString("updatedAt").takeIf { it.isNotBlank() }
    }

    suspend fun uploadLogo(uri: Uri): Screen10UploadedLogo = withContext(Dispatchers.IO) {
        val meta = imageMeta(uri)
        val supportedMime = meta.mimeType in SUPPORTED_LOGO_MIME_TYPES
        val supportedExtension = SUPPORTED_LOGO_EXTENSIONS.any { meta.fileName.endsWith(it, ignoreCase = true) }
        if (!supportedMime && !supportedExtension) {
            throw IllegalArgumentException("Logo must be JPG, PNG or WebP.")
        }

        val bytes = readImageBytes(uri, meta.fileName, CLIENT_MAX_LOGO_BYTES)
        val signed = api.post("/api/admin/cloudinary/signature?purpose=logo")
        if (!signed.optBoolean("ok", false)) {
            throw ApiException(500, "INVALID_UPLOAD_SIGNATURE", "Logo upload configuration response is incomplete.")
        }
        val cloudName = signed.optString("cloudName").trim()
        val apiKey = signed.optString("apiKey").trim()
        val timestamp = signed.optLong("timestamp", 0L)
        val folder = signed.optString("folder").trim()
        val signature = signed.optString("signature").trim()
        val allowedFormats = signed.optString("allowedFormats").trim()
        val maxBytes = signed.optLong("maxBytes", CLIENT_MAX_LOGO_BYTES.toLong())
            .coerceAtMost(CLIENT_MAX_LOGO_BYTES.toLong())
        if (cloudName.isBlank() || apiKey.isBlank() || timestamp <= 0L || folder.isBlank() || signature.isBlank()) {
            throw ApiException(500, "INVALID_UPLOAD_SIGNATURE", "Logo upload configuration response is incomplete.")
        }
        if (bytes.size.toLong() > maxBytes) {
            throw IllegalArgumentException("${meta.fileName} is larger than ${maxBytes / 1024 / 1024} MB.")
        }

        uploadLogoToCloudinary(
            cloudName = cloudName,
            apiKey = apiKey,
            timestamp = timestamp,
            folder = folder,
            signature = signature,
            allowedFormats = allowedFormats,
            fileName = meta.fileName,
            mimeType = meta.mimeType.ifBlank { "application/octet-stream" },
            bytes = bytes
        )
    }

    suspend fun discardLogoUpload(asset: Screen10UploadedLogo) {
        if (asset.url.isBlank() || asset.publicId.isBlank()) return
        runCatching {
            api.post(
                "/api/admin/cloudinary/discard",
                JSONObject().put(
                    "assets",
                    JSONArray().put(
                        JSONObject()
                            .put("url", asset.url)
                            .put("publicId", asset.publicId)
                    )
                )
            )
        }
    }

    suspend fun createTemplate(
        templateKey: String,
        templateName: String,
        linkedAction: String?,
        messageGu: String,
        messageEn: String,
        active: Boolean
    ) {
        api.post(
            "/api/admin/whatsapp-templates",
            templateBody(templateKey, templateName, linkedAction, messageGu, messageEn, active)
        )
    }

    suspend fun updateTemplate(
        id: String,
        templateKey: String,
        templateName: String,
        linkedAction: String?,
        messageGu: String,
        messageEn: String,
        active: Boolean,
        expectedUpdatedAt: String?
    ) {
        val body = templateBody(templateKey, templateName, linkedAction, messageGu, messageEn, active)
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        api.request("PUT", "/api/admin/whatsapp-templates/$id", body)
    }

    suspend fun deleteTemplate(id: String) {
        api.request("DELETE", "/api/admin/whatsapp-templates/$id")
    }

    suspend fun auditLogs(
        page: Int,
        search: String,
        module: String,
        action: String,
        userId: String,
        fromDate: String,
        toDate: String,
        pageSize: Int = 10
    ): Screen10AuditPage =
        api.get(
            api.queryPath(
                "/api/admin/audit-logs",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.coerceIn(10, 50).toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() },
                    "module" to module.takeIf { it.isNotBlank() && it != "ALL" },
                    "action" to action.takeIf { it.isNotBlank() && it != "ALL" },
                    "userId" to userId.takeIf { it.isNotBlank() && it != "ALL" },
                    "fromDate" to fromDate.takeIf { it.isNotBlank() },
                    "toDate" to toDate.takeIf { it.isNotBlank() }
                )
            )
        ).toScreen10AuditPage()

    private fun imageMeta(uri: Uri): LogoImageMeta {
        var fileName = "business-logo"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)?.trim()?.takeIf { it.isNotBlank() }?.let { fileName = it }
            }
        }
        val safeName = fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .take(120)
            .ifBlank { "business-logo" }
        return LogoImageMeta(safeName, contentResolver.getType(uri).orEmpty().lowercase())
    }

    private fun readImageBytes(uri: Uri, fileName: String, maxBytes: Int): ByteArray {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to read $fileName.")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) {
                    throw IllegalArgumentException("$fileName is larger than ${maxBytes / 1024 / 1024} MB.")
                }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun uploadLogoToCloudinary(
        cloudName: String,
        apiKey: String,
        timestamp: Long,
        folder: String,
        signature: String,
        allowedFormats: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Screen10UploadedLogo {
        val boundary = "ZhagmagLogo${System.currentTimeMillis()}"
        val connection = (URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = UPLOAD_TIMEOUT_MS
            readTimeout = UPLOAD_TIMEOUT_MS
            useCaches = false
            doOutput = true
            setChunkedStreamingMode(64 * 1024)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("User-Agent", "ZhagmagAdminAndroid/0.14.5")
        }

        try {
            DataOutputStream(connection.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("api_key", apiKey)
                field("timestamp", timestamp.toString())
                field("folder", folder)
                if (allowedFormats.isNotBlank()) field("allowed_formats", allowedFormats)
                field("signature", signature)
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                out.writeBytes("Content-Type: $mimeType\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(responseText) }.getOrDefault(JSONObject())
            val uploadedUrl = json.optString("secure_url").trim()
            val publicId = json.optString("public_id").trim()
            if (status !in 200..299 || uploadedUrl.isBlank() || publicId.isBlank()) {
                val message = json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "Logo upload failed."
                throw ApiException(status, "CLOUDINARY_UPLOAD_FAILED", message)
            }
            return Screen10UploadedLogo(uploadedUrl, publicId)
        } catch (error: ApiException) {
            throw error
        } catch (timeout: SocketTimeoutException) {
            throw ApiException(408, "UPLOAD_TIMEOUT", "Logo upload timed out.")
        } catch (network: IOException) {
            throw ApiException(0, "NETWORK", "Unable to connect.")
        } finally {
            connection.disconnect()
        }
    }

    private data class LogoImageMeta(
        val fileName: String,
        val mimeType: String
    )

    private fun templateBody(
        templateKey: String,
        templateName: String,
        linkedAction: String?,
        messageGu: String,
        messageEn: String,
        active: Boolean
    ) = JSONObject()
        .put("templateKey", templateKey.trim())
        .put("templateName", templateName.trim())
        .put("languageMode", "ALL")
        .put("linkedAction", linkedAction.orEmpty().uppercase())
        .put("messageGu", messageGu.trim())
        .put("messageEn", messageEn.trim())
        .put("isActive", active)

    private companion object {
        const val CLIENT_MAX_LOGO_BYTES = 8 * 1024 * 1024
        const val UPLOAD_TIMEOUT_MS = 30_000
        val SUPPORTED_LOGO_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        val SUPPORTED_LOGO_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
    }
}
