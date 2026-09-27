package com.nimsdeveloper.zhagmagdresses.admin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

class ApiException(
    val statusCode: Int,
    val apiCode: String?,
    message: String
) : IOException(apiUserMessage(statusCode, message)) {
    override val message: String
        get() = super.message ?: "Something went wrong. Please try again."
}

object ApiSessionEvents {
    @Volatile
    private var onSessionExpired: (() -> Unit)? = null

    fun registerSessionExpiredHandler(handler: () -> Unit) {
        onSessionExpired = handler
    }

    fun clearSessionExpiredHandler() {
        onSessionExpired = null
    }

    internal fun notifySessionExpired() {
        runCatching { onSessionExpired?.invoke() }
    }
}

private fun apiUserMessage(statusCode: Int, rawMessage: String): String = when {
    statusCode == 0 -> "Unable to connect. Check your internet connection and try again."
    statusCode == 401 -> "Your session has expired. Please sign in again."
    statusCode == 403 -> "You do not have permission to perform this action."
    statusCode == 408 -> "Request timed out. Please try again."
    statusCode == 429 -> "Too many requests. Please try again later."
    statusCode >= 500 -> "Service is temporarily unavailable. Please try again."
    else -> rawMessage.trim().takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}

class ApiClient(
    baseUrl: String,
    private val sessionStore: SecureSessionStore
) {
    private val apiBase = baseUrl.trim().removeSuffix("/")

    suspend fun get(path: String): JSONObject = request("GET", path)

    suspend fun post(path: String, body: JSONObject = JSONObject()): JSONObject =
        request("POST", path, body)

    suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            require(apiBase.isNotBlank()) { "API base URL is not configured." }

            val connection = (URL("$apiBase$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ZhagmagAdminAndroid/0.14.5")
                sessionStore.readCookie()?.let { setRequestProperty("Cookie", it) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }

            try {
                if (body != null) {
                    connection.outputStream.use { stream ->
                        stream.write(body.toString().toByteArray(Charsets.UTF_8))
                    }
                }

                val status = connection.responseCode
                updateSessionCookie(connection)
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = if (text.isBlank()) {
                    JSONObject()
                } else {
                    runCatching { JSONObject(text) }
                        .getOrElse { JSONObject().put("message", "Server returned an invalid response.") }
                }

                if (status !in 200..299) {
                    if (status == 401) {
                        sessionStore.clear()
                        ApiSessionEvents.notifySessionExpired()
                    }
                    throw ApiException(
                        statusCode = status,
                        apiCode = json.optString("error").takeIf { it.isNotBlank() },
                        message = json.optString("message").takeIf { it.isNotBlank() }
                            ?: "Request failed ($status)."
                    )
                }

                json
            } catch (error: ApiException) {
                throw error
            } catch (timeout: SocketTimeoutException) {
                throw ApiException(408, "TIMEOUT", "Request timed out.")
            } catch (network: IOException) {
                throw ApiException(0, "NETWORK", "Unable to connect.")
            } finally {
                connection.disconnect()
            }
        }

    fun queryPath(path: String, parameters: Map<String, String?>): String {
        val encoded = parameters
            .filterValues { !it.isNullOrBlank() }
            .entries
            .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value.orEmpty())}" }
        return if (encoded.isBlank()) path else "$path?$encoded"
    }

    private fun updateSessionCookie(connection: HttpURLConnection) {
        val setCookie = connection.headerFields.entries
            .firstOrNull { (key, _) -> key?.equals("Set-Cookie", ignoreCase = true) == true }
            ?.value
            ?.firstOrNull {
                it.startsWith("__Host-zhagmag_session=") || it.startsWith("zhagmag_session=")
            }
            ?: return

        val firstPair = setCookie.substringBefore(';').trim()
        val value = firstPair.substringAfter('=', "")
        if (value.isBlank() || setCookie.contains("Max-Age=0", ignoreCase = true)) {
            sessionStore.clear()
        } else {
            sessionStore.saveCookie(firstPair)
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val TIMEOUT_MS = 30_000
    }
}
