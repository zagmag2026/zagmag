package com.nimsdeveloper.zhagmagdresses.admin

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

internal class Screen5ItemManagementRepository(
    private val api: ApiClient,
    private val contentResolver: ContentResolver
) {
    private val uploadSignatureMutex = Mutex()
    private var cachedUploadSignature: JSONObject? = null
    private var cachedUploadSignatureAtMs: Long = 0L
    suspend fun bootstrap(): Screen5Bootstrap =
        api.get("/api/admin/item-management/bootstrap").toScreen5Bootstrap()

    suspend fun items(
        page: Int,
        search: String,
        categoryId: String,
        pageSize: Int = 10
    ): Screen5ItemPage =
        api.get(
            api.queryPath(
                "/api/admin/item-management/items",
                mapOf(
                    "page" to page.coerceAtLeast(1).toString(),
                    "pageSize" to pageSize.coerceIn(10, 50).toString(),
                    "search" to search.trim().takeIf { it.isNotBlank() },
                    "categoryId" to categoryId.trim().takeIf { it.isNotBlank() }
                )
            )
        ).toScreen5ItemPage()

    suspend fun saveCategory(
        existingId: String?,
        name: String,
        codePrefix: String,
        displayOrder: Int,
        active: Boolean,
        publicVisible: Boolean
    ) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("codePrefix", codePrefix.trim())
            .put("displayOrder", displayOrder.coerceAtLeast(0))
            .put("isActive", active)
            .put("publicVisible", publicVisible)
        if (existingId.isNullOrBlank()) api.post("/api/admin/categories", body)
        else api.request("PUT", "/api/admin/categories/$existingId", body)
    }

    suspend fun deleteCategory(id: String) {
        api.request("DELETE", "/api/admin/categories/$id")
    }

    suspend fun saveField(
        categoryId: String,
        existingId: String?,
        name: String,
        type: String,
        required: Boolean,
        defaultValue: String,
        options: List<String>,
        publicVisible: Boolean,
        active: Boolean,
        displayOrder: Int
    ) {
        val body = JSONObject()
            .put("fieldName", name.trim())
            .put("fieldType", type.uppercase())
            .put("isRequired", required)
            .put("defaultValue", defaultValue.trim())
            .put("options", JSONArray(options.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("publicVisible", publicVisible)
            .put("isActive", active)
            .put("displayOrder", displayOrder.coerceAtLeast(0))
        if (existingId.isNullOrBlank()) api.post("/api/admin/categories/$categoryId/fields", body)
        else api.request("PUT", "/api/admin/category-fields/$existingId", body)
    }

    suspend fun deleteField(id: String) {
        api.request("DELETE", "/api/admin/category-fields/$id")
    }

    suspend fun fieldDeleteAction(id: String, action: String, confirmation: String = "") {
        api.post(
            "/api/admin/category-fields/$id/delete-action",
            JSONObject()
                .put("action", action)
                .put("confirmation", confirmation)
        )
    }

    suspend fun saveItem(
        existing: Screen5Item?,
        itemCode: String,
        itemName: String,
        categoryId: String,
        totalQuantity: Int,
        rentAmount: Int,
        active: Boolean,
        publicVisible: Boolean,
        fieldValues: Map<String, String>,
        clearFieldIds: Set<String>,
        imageUrls: List<String>,
        cloudinaryAssets: List<Screen5UploadedAsset>
    ) {
        val values = JSONObject()
        fieldValues.forEach { (key, value) -> values.put(key, value) }
        val normalizedImageUrls = imageUrls
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_IMAGE_FILES)
        val assetPayload = JSONArray()
        cloudinaryAssets
            .filter { it.publicId.isNotBlank() && it.url in normalizedImageUrls }
            .distinctBy { it.url }
            .take(MAX_IMAGE_FILES)
            .forEach { asset ->
                assetPayload.put(
                    JSONObject()
                        .put("url", asset.url)
                        .put("publicId", asset.publicId)
                )
            }
        val body = JSONObject()
            .put("itemCode", itemCode.trim())
            .put("itemName", itemName.trim())
            .put("categoryId", categoryId)
            .put("totalQuantity", totalQuantity.coerceAtLeast(0))
            .put("rentAmount", rentAmount.coerceAtLeast(0))
            .put("isActive", active)
            .put("publicVisible", publicVisible)
            .put("fieldValues", values)
            .put("clearFieldIds", JSONArray(clearFieldIds.toList()))
            .put("imageUrls", JSONArray(normalizedImageUrls))
            .put("cloudinaryAssets", assetPayload)
        existing?.updatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        if (existing == null) api.post("/api/admin/items", body)
        else api.request("PUT", "/api/admin/items/${existing.id}", body)
    }

    suspend fun deleteItem(id: String) {
        api.request("DELETE", "/api/admin/items/$id")
    }

    suspend fun discardItemUploads(assets: List<Screen5UploadedAsset>) {
        val payload = JSONArray()
        assets
            .filter { it.url.isNotBlank() && it.publicId.isNotBlank() }
            .distinctBy { it.publicId }
            .take(MAX_IMAGE_FILES)
            .forEach { asset ->
                payload.put(
                    JSONObject()
                        .put("url", asset.url)
                        .put("publicId", asset.publicId)
                )
            }
        if (payload.length() == 0) return
        runCatching {
            api.post(
                "/api/admin/cloudinary/discard",
                JSONObject().put("assets", payload)
            )
        }
    }

    suspend fun uploadItemImage(uri: Uri): Screen5UploadedAsset = withContext(Dispatchers.IO) {
        val meta = imageMeta(uri)
        val supportedMime = meta.mimeType.lowercase() in SUPPORTED_IMAGE_MIME_TYPES
        val supportedExtension = SUPPORTED_IMAGE_EXTENSIONS.any { meta.fileName.endsWith(it, ignoreCase = true) }
        if (!supportedMime && !supportedExtension) {
            throw IllegalArgumentException("${meta.fileName} must be JPG, PNG or WebP.")
        }

        val bytes = readImageBytes(uri, meta.fileName, CLIENT_MAX_IMAGE_BYTES)
        val signed = uploadSignature()
        if (!signed.optBoolean("ok", false)) {
            throw ApiException(500, "INVALID_UPLOAD_SIGNATURE", "Image upload configuration response is incomplete.")
        }
        val cloudName = signed.optString("cloudName").trim()
        val apiKey = signed.optString("apiKey").trim()
        val timestamp = signed.optLong("timestamp", 0L)
        val folder = signed.optString("folder").trim()
        val signature = signed.optString("signature").trim()
        val allowedFormats = signed.optString("allowedFormats").trim()
        val maxBytes = signed.optLong("maxBytes", CLIENT_MAX_IMAGE_BYTES.toLong())
            .coerceAtMost(CLIENT_MAX_IMAGE_BYTES.toLong())
        if (cloudName.isBlank() || apiKey.isBlank() || timestamp <= 0L || folder.isBlank() || signature.isBlank()) {
            throw ApiException(500, "INVALID_UPLOAD_SIGNATURE", "Image upload configuration response is incomplete.")
        }
        if (bytes.size.toLong() > maxBytes) {
            throw IllegalArgumentException("${meta.fileName} is larger than ${maxBytes / 1024 / 1024} MB.")
        }

        uploadToCloudinary(
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

    private suspend fun uploadSignature(): JSONObject = uploadSignatureMutex.withLock {
        val now = System.currentTimeMillis()
        cachedUploadSignature
            ?.takeIf { now - cachedUploadSignatureAtMs < UPLOAD_SIGNATURE_CACHE_MS }
            ?: api.post("/api/admin/cloudinary/signature").also {
                cachedUploadSignature = it
                cachedUploadSignatureAtMs = now
            }
    }

    private fun imageMeta(uri: Uri): ImageMeta {
        var fileName = "item-photo"
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
            .ifBlank { "item-photo" }
        return ImageMeta(safeName, contentResolver.getType(uri).orEmpty().lowercase())
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

    private fun uploadToCloudinary(
        cloudName: String,
        apiKey: String,
        timestamp: Long,
        folder: String,
        signature: String,
        allowedFormats: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Screen5UploadedAsset {
        val boundary = "ZhagmagAndroid${System.currentTimeMillis()}"
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
                    ?: "Image upload failed."
                throw ApiException(status, "CLOUDINARY_UPLOAD_FAILED", message)
            }
            return Screen5UploadedAsset(uploadedUrl, publicId)
        } catch (timeout: SocketTimeoutException) {
            throw ApiException(408, "UPLOAD_TIMEOUT", "Image upload timed out.")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun replaceRelatedItems(
        sourceItemId: String,
        relatedItemIds: List<String>,
        expectedUpdatedAt: String?
    ) {
        val body = JSONObject().put("relatedItemIds", JSONArray(relatedItemIds.distinct()))
        expectedUpdatedAt?.takeIf { it.isNotBlank() }?.let { body.put("expectedUpdatedAt", it) }
        api.request("PUT", "/api/admin/related-items/$sourceItemId", body)
    }

    private data class ImageMeta(
        val fileName: String,
        val mimeType: String
    )

    private companion object {
        const val MAX_IMAGE_FILES = 8
        const val CLIENT_MAX_IMAGE_BYTES = 8 * 1024 * 1024
        const val UPLOAD_TIMEOUT_MS = 30_000
        const val UPLOAD_SIGNATURE_CACHE_MS = 90_000L
        val SUPPORTED_IMAGE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        val SUPPORTED_IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
    }
}
