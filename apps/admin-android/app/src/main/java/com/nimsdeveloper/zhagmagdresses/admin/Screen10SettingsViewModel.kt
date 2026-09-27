package com.nimsdeveloper.zhagmagdresses.admin

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.net.URI

internal data class Screen10SettingsState(
    val settings: Screen10SiteSettings = Screen10SiteSettings(),
    val templates: List<Screen10WhatsAppTemplate> = emptyList(),
    val users: List<Screen10AuditUser> = emptyList(),
    val auditModules: List<String> = emptyList(),
    val auditActions: List<String> = emptyList(),
    val placeholders: List<String> = emptyList(),
    val placeholderRegistry: Map<String, List<String>> = emptyMap(),
    val placeholderSources: Map<String, String> = emptyMap(),
    val languageModes: List<String> = listOf("GUJARATI", "ENGLISH", "BOTH"),
    val linkedActions: List<String> = emptyList(),
    val updatedAt: String? = null,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val actionBusy: Boolean = false,
    val logoUploading: Boolean = false,
    val loadError: String? = null,
    val error: String? = null,
    val message: String? = null,

    val auditItems: List<Screen10AuditLog> = emptyList(),
    val auditPage: Int = 1,
    val auditTotalPages: Int = 1,
    val auditTotal: Int = 0,
    val auditLoaded: Boolean = false,
    val auditLoading: Boolean = false,
    val auditLoadMoreError: String? = null,
    val auditSearch: String = "",
    val auditModule: String = "ALL",
    val auditAction: String = "ALL",
    val auditUserId: String = "ALL",
    val auditFromDate: String = "",
    val auditToDate: String = ""
) {
    val canLoadMoreAudit: Boolean
        get() = auditLoaded && !auditLoading && loadError == null && auditLoadMoreError == null && auditPage < auditTotalPages
}

internal class Screen10SettingsViewModel(
    private val repository: Screen10SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var state by mutableStateOf(
        Screen10SettingsState(
            auditPage = savedStateHandle["auditPage"] ?: 1,
            auditSearch = savedStateHandle["auditSearch"] ?: "",
            auditModule = savedStateHandle["auditModule"] ?: "ALL",
            auditAction = savedStateHandle["auditAction"] ?: "ALL",
            auditUserId = savedStateHandle["auditUser"] ?: "ALL",
            auditFromDate = savedStateHandle["auditFrom"] ?: "",
            auditToDate = savedStateHandle["auditTo"] ?: ""
        )
    )
        private set

    private var auditSearchJob: Job? = null
    private var feedbackJob: Job? = null
    private var pendingAuditReset = false
    private var seenUsersRevision = AdminDataFreshness.usersRevision
    private var seenAuditRevision = AdminDataFreshness.auditRevision
    private var pendingLogoUpload: Screen10UploadedLogo? = null

    fun ensureLoaded() {
        if (!state.loaded && !state.loading) loadBootstrap()
    }

    fun ensureAuditLoaded() {
        if (seenUsersRevision != AdminDataFreshness.usersRevision && !state.loading) {
            loadBootstrap()
        }
        val auditStale = seenAuditRevision != AdminDataFreshness.auditRevision
        if ((!state.auditLoaded || auditStale) && !state.auditLoading) {
            loadAudit(reset = true, restorePage = !state.auditLoaded)
        }
    }

    fun refresh() {
        if (state.loading || state.actionBusy) return
        clearFeedback()
        loadBootstrap()
    }

    fun clearFeedback() {
        feedbackJob?.cancel()
        state = state.copy(loadError = null, error = null, message = null)
    }

    fun saveSettings(settings: Screen10SiteSettings, onSuccess: () -> Unit = {}) {
        if (state.actionBusy) return
        when {
            settings.shopName.trim().isBlank() -> showError("Shop Name is required.")
            settings.websiteTitle.trim().isBlank() -> showError("Website Title is required.")
            settings.websiteUrl.isNotBlank() && !validWebsiteUrl(settings.websiteUrl) ->
                showError("Enter a valid http:// or https:// website URL.")
            settings.logoUrl.isNotBlank() && !settings.logoUrl.startsWith("https://", ignoreCase = true) ->
                showError("Logo must use a secure HTTPS URL.")
            settings.whatsappTemplateLanguage !in setOf("GUJARATI", "ENGLISH", "BOTH") ->
                showError("WhatsApp Template Language is invalid.")
            else -> saveSettingsMutation(settings, onSuccess)
        }
    }

    fun saveTemplate(
        existingId: String?,
        templateKey: String,
        templateName: String,
        linkedAction: String?,
        messageGu: String,
        messageEn: String,
        active: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (state.actionBusy) return
        when {
            templateKey.trim().isBlank() -> showError("Template Key is required.")
            templateName.trim().isBlank() -> showError("Template Name is required.")
            linkedAction.isNullOrBlank() -> showError("Linked To is required.")
            messageGu.trim().isBlank() -> showError("Gujarati Message is required.")
            messageEn.trim().isBlank() -> showError("English Message is required.")
            else -> mutate(if (existingId == null) "Template added." else "Template updated.", onSuccess) {
                if (existingId == null) {
                    repository.createTemplate(
                        templateKey, templateName, linkedAction,
                        messageGu, messageEn, active
                    )
                } else {
                    repository.updateTemplate(
                        existingId, templateKey, templateName, linkedAction,
                        messageGu, messageEn, active,
                        state.templates.firstOrNull { it.id == existingId }?.updatedAt
                    )
                }
            }
        }
    }

    fun uploadLogo(uri: Uri, onSuccess: (Screen10UploadedLogo) -> Unit) {
        if (state.logoUploading || state.actionBusy) return
        feedbackJob?.cancel()
        state = state.copy(logoUploading = true, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.uploadLogo(uri) }
                .onSuccess { asset ->
                    pendingLogoUpload?.let { old ->
                        if (old.publicId != asset.publicId) repository.discardLogoUpload(old)
                    }
                    pendingLogoUpload = asset
                    state = state.copy(
                        logoUploading = false,
                        message = "Logo uploaded. Save Basic Settings to apply it."
                    )
                    onSuccess(asset)
                    scheduleMessageClear("Logo uploaded. Save Basic Settings to apply it.")
                }
                .onFailure { error ->
                    val message = userMessage(error)
                    state = state.copy(logoUploading = false, error = message)
                    scheduleErrorClear(message)
                }
        }
    }

    fun discardPendingLogo(publicId: String) {
        val pending = pendingLogoUpload ?: return
        if (publicId.isNotBlank() && pending.publicId != publicId) return
        pendingLogoUpload = null
        viewModelScope.launch { repository.discardLogoUpload(pending) }
    }

    fun deleteTemplate(id: String) {
        if (state.actionBusy) return
        mutate("Template deleted.") { repository.deleteTemplate(id) }
    }

    fun refreshAudit() {
        if (state.actionBusy) return
        auditSearchJob?.cancel()
        clearFeedback()
        requestAuditReset()
    }

    fun auditSearch(value: String) {
        val next = value.take(120)
        savedStateHandle["auditSearch"] = next
        state = state.copy(auditSearch = next, loadError = null, error = null, message = null)
        auditSearchJob?.cancel()
        auditSearchJob = viewModelScope.launch {
            delay(350)
            requestAuditReset()
        }
    }

    fun setAuditModule(value: String) {
        auditSearchJob?.cancel()
        if (value == state.auditModule) return
        savedStateHandle["auditModule"] = value
        state = state.copy(auditModule = value, loadError = null, error = null, message = null)
        requestAuditReset()
    }

    fun setAuditAction(value: String) {
        auditSearchJob?.cancel()
        if (value == state.auditAction) return
        savedStateHandle["auditAction"] = value
        state = state.copy(auditAction = value, loadError = null, error = null, message = null)
        requestAuditReset()
    }

    fun setAuditUser(value: String) {
        auditSearchJob?.cancel()
        if (value == state.auditUserId) return
        savedStateHandle["auditUser"] = value
        state = state.copy(auditUserId = value, loadError = null, error = null, message = null)
        requestAuditReset()
    }

    fun setAuditDates(fromDate: String, toDate: String) {
        val cleanFrom = fromDate.trim()
        val cleanTo = toDate.trim()
        savedStateHandle["auditFrom"] = cleanFrom
        savedStateHandle["auditTo"] = cleanTo
        state = state.copy(
            auditFromDate = cleanFrom,
            auditToDate = cleanTo,
            loadError = null,
            error = null,
            message = null
        )
    }

    fun applyAuditDates() {
        auditSearchJob?.cancel()
        val from = state.auditFromDate.takeIf(String::isNotBlank)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val to = state.auditToDate.takeIf(String::isNotBlank)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (from != null && to != null && from.isAfter(to)) {
            val message = "From Date cannot be after To Date."
            state = state.copy(error = message, message = null, loadError = null)
            scheduleErrorClear(message)
            return
        }
        requestAuditReset()
    }

    fun resetAuditFilters() {
        auditSearchJob?.cancel()
        savedStateHandle["auditSearch"] = ""
        savedStateHandle["auditModule"] = "ALL"
        savedStateHandle["auditAction"] = "ALL"
        savedStateHandle["auditUser"] = "ALL"
        savedStateHandle["auditFrom"] = ""
        savedStateHandle["auditTo"] = ""
        state = state.copy(
            auditSearch = "",
            auditModule = "ALL",
            auditAction = "ALL",
            auditUserId = "ALL",
            auditFromDate = "",
            auditToDate = "",
            loadError = null,
            error = null,
            message = null
        )
        requestAuditReset()
    }

    fun loadMoreAudit() {
        if (state.canLoadMoreAudit) loadAudit(reset = false)
    }

    fun retryLoadMoreAudit() {
        if (state.auditLoading || state.auditLoadMoreError.isNullOrBlank() || state.auditPage >= state.auditTotalPages) return
        state = state.copy(auditLoadMoreError = null)
        loadAudit(reset = false)
    }

    private fun loadBootstrap() {
        if (state.loading) return
        state = state.copy(loading = true, loadError = null)
        viewModelScope.launch {
            runCatching { repository.bootstrap() }
                .onSuccess { result ->
                    seenUsersRevision = AdminDataFreshness.usersRevision
                    state = state.copy(
                        settings = result.settings,
                        templates = result.templates,
                        users = result.users,
                        auditModules = result.auditModules,
                        auditActions = result.auditActions,
                        placeholders = result.placeholders,
                        placeholderRegistry = result.placeholderRegistry,
                        placeholderSources = result.placeholderSources,
                        languageModes = result.languageModes,
                        linkedActions = result.linkedActions,
                        updatedAt = result.updatedAt,
                        loading = false,
                        loaded = true,
                        loadError = null,
                    auditLoadMoreError = null
                    )
                }
                .onFailure { error ->
                    state = state.copy(loading = false, loadError = userMessage(error))
                }
        }
    }

    private fun saveSettingsMutation(
        settings: Screen10SiteSettings,
        onSuccess: () -> Unit
    ) {
        if (state.actionBusy) return
        feedbackJob?.cancel()
        state = state.copy(actionBusy = true, loadError = null, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.saveSettings(settings, state.updatedAt) }
                .onSuccess { updatedAt ->
                    pendingLogoUpload = null
                    AdminDataFreshness.markAuditMutation()
                    state = state.copy(
                        settings = settings,
                        updatedAt = updatedAt ?: state.updatedAt,
                        actionBusy = false,
                        message = "Settings updated."
                    )
                    onSuccess()
                    scheduleMessageClear("Settings updated.")
                }
                .onFailure { error ->
                    val message = userMessage(error)
                    state = state.copy(actionBusy = false, error = message)
                    scheduleErrorClear(message)
                }
        }
    }

    private fun mutate(
        successMessage: String,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        if (state.actionBusy) return
        feedbackJob?.cancel()
        state = state.copy(actionBusy = true, loadError = null, error = null, message = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    AdminDataFreshness.markAuditMutation()
                    state = state.copy(actionBusy = false, message = successMessage)
                    onSuccess()
                    loadBootstrap()
                    scheduleMessageClear(successMessage)
                }
                .onFailure { error ->
                    val message = userMessage(error)
                    state = state.copy(actionBusy = false, error = message)
                    scheduleErrorClear(message)
                }
        }
    }

    private fun requestAuditReset() {
        savedStateHandle["auditPage"] = 1
        if (state.auditLoading) {
            pendingAuditReset = true
            return
        }
        loadAudit(reset = true)
    }

    private fun loadAudit(reset: Boolean, restorePage: Boolean = false) {
        if (state.auditLoading) return
        val targetPage = if (reset) {
            if (restorePage) state.auditPage.coerceAtLeast(1) else 1
        } else state.auditPage + 1
        val snapshot = state
        viewModelScope.launch {
            state = state.copy(
                auditLoading = true,
                loadError = if (reset) null else state.loadError,
                auditLoadMoreError = null
            )
            runCatching {
                repository.auditLogs(
                    page = targetPage,
                    search = snapshot.auditSearch,
                    module = snapshot.auditModule,
                    action = snapshot.auditAction,
                    userId = snapshot.auditUserId,
                    fromDate = snapshot.auditFromDate,
                    toDate = snapshot.auditToDate
                )
            }.onSuccess { result ->
                val merged = if (reset) result.logs else (state.auditItems + result.logs).distinctBy { it.id }
                seenAuditRevision = AdminDataFreshness.auditRevision
                savedStateHandle["auditPage"] = result.page
                state = state.copy(
                    auditItems = merged,
                    auditPage = result.page,
                    auditTotalPages = result.totalPages,
                    auditTotal = result.total,
                    auditLoaded = true,
                    auditLoading = false,
                    loadError = null
                )
            }.onFailure { error ->
                val message = userMessage(error)
                state = if (!reset && state.auditLoaded) {
                    state.copy(auditLoading = false, auditLoadMoreError = message)
                } else {
                    state.copy(auditLoading = false, loadError = message, auditLoadMoreError = null)
                }
            }

            if (pendingAuditReset && !state.auditLoading) {
                pendingAuditReset = false
                loadAudit(reset = true)
            }
        }
    }

    private fun validWebsiteUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun showError(message: String) {
        state = state.copy(error = message, message = null)
        scheduleErrorClear(message)
    }

    private fun scheduleMessageClear(message: String) {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(2800)
            if (state.message == message) state = state.copy(message = null)
        }
    }

    private fun scheduleErrorClear(message: String) {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(5200)
            if (state.error == message) state = state.copy(error = null)
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message.ifBlank { "Something went wrong. Please try again." }
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    class Factory(private val repository: Screen10SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            Screen10SettingsViewModel(repository, extras.createSavedStateHandle()) as T
    }
}
