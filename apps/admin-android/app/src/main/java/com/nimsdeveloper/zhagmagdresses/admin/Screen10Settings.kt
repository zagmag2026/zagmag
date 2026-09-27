package com.nimsdeveloper.zhagmagdresses.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.*
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.net.URI
import org.json.JSONObject

private val Screen10SettingsSaver = Saver<Screen10SiteSettings, String>(
    save = { value ->
        JSONObject()
            .put("shopName", value.shopName)
            .put("websiteTitle", value.websiteTitle)
            .put("websiteUrl", value.websiteUrl)
            .put("logoUrl", value.logoUrl)
            .put("logoPublicId", value.logoPublicId)
            .put("contactNumber", value.contactNumber)
            .put("whatsappNumber", value.whatsappNumber)
            .put("address", value.address)
            .put("defaultLanguage", value.defaultLanguage)
            .put("dateFormat", value.dateFormat)
            .put("publicCatalogEnabled", value.publicCatalogEnabled)
            .put("showWhatsApp", value.showWhatsApp)
            .put("showCall", value.showCall)
            .put("availabilityMode", value.availabilityMode)
            .put("fewLeftThreshold", value.fewLeftThreshold)
            .put("whatsappTemplateLanguage", value.whatsappTemplateLanguage)
            .put(
                "workingHours",
                JSONObject().apply {
                    SCREEN10_WORKING_DAYS.forEach { (key, _) ->
                        val day = value.workingHours[key] ?: Screen10WorkingDay()
                        put(key, JSONObject()
                            .put("isOpen", day.isOpen)
                            .put("openTime", day.openTime)
                            .put("closeTime", day.closeTime))
                    }
                }
            )
            .toString()
    },
    restore = { raw ->
        runCatching {
            val json = JSONObject(raw)
            val hoursJson = json.optJSONObject("workingHours")
            Screen10SiteSettings(
                shopName = json.optString("shopName"),
                websiteTitle = json.optString("websiteTitle"),
                websiteUrl = json.optString("websiteUrl"),
                logoUrl = json.optString("logoUrl"),
                logoPublicId = json.optString("logoPublicId"),
                contactNumber = json.optString("contactNumber"),
                whatsappNumber = json.optString("whatsappNumber"),
                address = json.optString("address"),
                defaultLanguage = json.optString("defaultLanguage", "GU"),
                dateFormat = json.optString("dateFormat", "DD-MM-YYYY"),
                workingHours = SCREEN10_WORKING_DAYS.associate { (key, _) ->
                    val day = hoursJson?.optJSONObject(key)
                    key to Screen10WorkingDay(
                        isOpen = day?.optBoolean("isOpen", false) ?: false,
                        openTime = day?.optString("openTime", "09:00") ?: "09:00",
                        closeTime = day?.optString("closeTime", "20:00") ?: "20:00"
                    )
                },
                publicCatalogEnabled = json.optBoolean("publicCatalogEnabled", true),
                showWhatsApp = json.optBoolean("showWhatsApp", true),
                showCall = json.optBoolean("showCall", true),
                availabilityMode = json.optString("availabilityMode", "EXACT"),
                fewLeftThreshold = json.optInt("fewLeftThreshold", 2),
                whatsappTemplateLanguage = json.optString("whatsappTemplateLanguage", "BOTH")
            )
        }.getOrDefault(Screen10SiteSettings())
    }
)

private enum class Screen10Tab(val label: String) {
    BASIC("Basic"),
    PUBLIC("Public Website"),
    AUDIT("Audit Log")
}

private enum class Screen10WhatsAppSection(val label: String) {
    RESERVATION("Reservation"),
    BOOKING("Booking"),
    PICKUP("Pickup"),
    RETURN("Return"),
    OVERDUE("Overdue"),
    ITEM("Item"),
    BILLING("Billing"),
    GENERAL("General"),
    LANGUAGE("Language Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen10PullToRefresh(
    enabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (enabled) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
        ) { content() }
    } else {
        Box(modifier = modifier) { content() }
    }
}

private val screen10LinkedActionLabels = linkedMapOf(
    "GENERAL_INQUIRY" to "General Inquiry",
    "BOOKING_CONFIRMATION" to "Booking Confirmation",
    "RESERVATION_CONFIRMATION" to "Reservation Confirmation",
    "RESERVATION_CANCELLED" to "Reservation Cancelled",
    "BOOKING_UPDATED" to "Booking Updated",
    "BOOKING_CANCELLED" to "Booking Cancelled",
    "PICKUP_READY" to "Pickup Ready",
    "PICKUP_DUE_TODAY" to "Pickup Due Today",
    "PICKUP_REMINDER" to "Pickup Reminder",
    "PENDING_PICKUP_REMINDER" to "Pending Pickup Reminder",
    "MISSED_PICKUP_REMINDER" to "Missed Pickup Reminder",
    "PICKUP_DONE" to "Pickup Done",
    "PART_PICKUP_DONE" to "Part Pickup Done",
    "RETURN_DUE_TODAY" to "Return Due Today",
    "RETURN_REMINDER" to "Return Reminder",
    "PENDING_RETURN_REMINDER" to "Pending Return Reminder",
    "OVERDUE_REMINDER" to "Overdue Reminder",
    "OVERDUE_FINAL_REMINDER" to "Overdue Follow-up / Final Reminder",
    "RETURN_DONE" to "Return Done",
    "PART_RETURN_DONE" to "Part Return Done",
    "ITEM_AVAILABILITY_REPLY" to "Item Availability Reply",
    "BOOKING_COMPLETED" to "Booking Completed",
    "THANK_YOU" to "Thank You",
    "RESERVATION_REMINDER" to "Reservation Reminder",
    "RESERVATION_EXPIRY_REMINDER" to "Reservation Expiry Reminder",
    "BOOKING_DETAILS" to "Booking Details",
    "RETURN_DATE_TIME_UPDATE" to "Return Date/Time Update",
    "RETURN_THANK_YOU" to "Return Thank You",
    "FEEDBACK_REVIEW" to "Feedback / Review",
    "OVERDUE_URGENT_REMINDER" to "Urgent Reminder",
    "OVERDUE_FOLLOW_UP_REMINDER" to "Follow-up Reminder",
    "CANCELLATION_DETAILS" to "Cancellation Details",
    "SHOP_ADDRESS" to "Shop Address",
    "WORKING_HOURS" to "Working Hours",
    "HOLIDAY_SHOP_CLOSED" to "Holiday / Shop Closed",
    "CONTACT_US" to "Contact Us",
    "BILL_DETAILS" to "Bill Details",
    "PAYMENT_PENDING" to "Payment Pending",
    "FULL_AMOUNT_RECEIVED" to "Full Amount Received",
    "BILL_CANCELLED" to "Bill Cancelled",
    "CUSTOM_GENERAL_MESSAGE" to "Custom General Message"
)

private fun screen10WhatsAppSectionForAction(value: String?): Screen10WhatsAppSection = when {
    value.orEmpty().startsWith("RESERVATION_") -> Screen10WhatsAppSection.RESERVATION
    value.orEmpty().startsWith("PICKUP_") ||
        value.orEmpty().startsWith("PENDING_PICKUP_") ||
        value.orEmpty().startsWith("MISSED_PICKUP_") ||
        value == "PART_PICKUP_DONE" -> Screen10WhatsAppSection.PICKUP
    value.orEmpty().startsWith("OVERDUE_") -> Screen10WhatsAppSection.OVERDUE
    value.orEmpty().startsWith("RETURN_") ||
        value.orEmpty().startsWith("PENDING_RETURN_") ||
        value == "PART_RETURN_DONE" ||
        value == "FEEDBACK_REVIEW" -> Screen10WhatsAppSection.RETURN
    value.orEmpty().startsWith("BOOKING_") || value == "CANCELLATION_DETAILS" -> Screen10WhatsAppSection.BOOKING
    value.orEmpty().startsWith("ITEM_") -> Screen10WhatsAppSection.ITEM
    value.orEmpty().startsWith("BILL_") ||
        value == "PAYMENT_PENDING" ||
        value == "FULL_AMOUNT_RECEIVED" -> Screen10WhatsAppSection.BILLING
    else -> Screen10WhatsAppSection.GENERAL
}

private fun screen10WhatsAppSectionActions(
    section: Screen10WhatsAppSection,
    linkedActions: List<String>
): List<String> = linkedActions.filter { screen10WhatsAppSectionForAction(it) == section }

private fun screen10TemplateKey(action: String, name: String): String {
    val actionKey = action.lowercase(Locale.ENGLISH)
    val nameKey = name.lowercase(Locale.ENGLISH)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(36)
    return if (nameKey.isBlank()) actionKey.take(80) else "${actionKey}_${nameKey}".take(80)
}

private fun screen10AppendPlaceholder(message: String, token: String): String {
    val separator = if (message.isBlank() || message.endsWith(" ") || message.endsWith("\n")) "" else " "
    return (message + separator + token).take(3000)
}

private fun screen10LanguageLabel(value: String): String = when (value.uppercase()) {
    "GUJARATI" -> "Gujarati"
    "ENGLISH" -> "English"
    else -> "Both"
}

private data class Screen10WorkingTimeTarget(
    val dayKey: String,
    val dayLabel: String,
    val opening: Boolean
)

private fun screen10TimeMinutes(value: String): Int? {
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun screen10WorkingHoursValid(workingHours: Map<String, Screen10WorkingDay>): Boolean =
    SCREEN10_WORKING_DAYS.all { (dayKey, _) ->
        val day = workingHours[dayKey] ?: Screen10WorkingDay()
        if (!day.isOpen) true
        else {
            val open = screen10TimeMinutes(day.openTime)
            val close = screen10TimeMinutes(day.closeTime)
            open != null && close != null && open < close
        }
    }

private fun screen10WorkingTimeLabel(value: String): String =
    runCatching {
        LocalTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }.getOrDefault(value)

private fun screen10WebsiteUrlValid(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return true
    return runCatching {
        val uri = URI(trimmed)
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Screen10Settings(
    currentUser: SessionUser,
    businessDate: String,
    onBack: () -> Unit,
    onBrandingChanged: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (currentUser.role != UserRole.OWNER) {
        Column(modifier.fillMaxSize().padding(AppSpacing.md)) {
            EmptyState("Owner access is required.")
        }
        return
    }

    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) {
        Screen10SettingsRepository(
            ApiClient(BuildConfig.API_BASE_URL, SecureSessionStore(appContext)),
            appContext.contentResolver
        )
    }
    val factory = remember(repository) { Screen10SettingsViewModel.Factory(repository) }
    val vm: Screen10SettingsViewModel = viewModel(factory = factory)
    val state = vm.state
    val dismissKeyboard = rememberReliableKeyboardDismiss()

    var tabName by rememberSaveable { mutableStateOf(Screen10Tab.BASIC.name) }
    val tab = runCatching { Screen10Tab.valueOf(tabName) }.getOrDefault(Screen10Tab.BASIC)
    var settingsDraft by rememberSaveable(stateSaver = Screen10SettingsSaver) { mutableStateOf(Screen10SiteSettings()) }
    var settingsDraftInitialized by rememberSaveable { mutableStateOf(false) }
    var confirmDiscardSettings by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loaded, state.updatedAt) {
        if (state.loaded && !settingsDraftInitialized) {
            settingsDraft = state.settings
            settingsDraftInitialized = true
        }
    }
    val hasUnsavedSettings = state.loaded && settingsDraft != state.settings
    val navigationBusy = state.actionBusy || state.logoUploading
    val requestBack: () -> Unit = {
        if (!navigationBusy) {
            if (hasUnsavedSettings) confirmDiscardSettings = true else onBack()
        }
    }

    LaunchedEffect(Unit) {
        vm.clearFeedback()
        vm.ensureLoaded()
    }
    LaunchedEffect(tab) { if (tab == Screen10Tab.AUDIT) vm.ensureAuditLoaded() }
    BackHandler { requestBack() }

    Screen10PullToRefresh(
        enabled = tab == Screen10Tab.AUDIT,
        isRefreshing = state.auditLoading,
        onRefresh = vm::refreshAudit,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                top = AppSpacing.xs,
                bottom = AppSpacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            item { MainScreenDateRow(businessDate) }
            item {
                AppBackHeader(
                    title = "Settings",
                    onBack = requestBack,
                    enabled = !navigationBusy
                )
            }
            if (!state.loadError.isNullOrBlank() && !state.loaded) {
                item {
                    LoadFailureState(
                        message = state.loadError.orEmpty(),
                        onRetry = vm::refresh
                    )
                }
            } else if (!state.loadError.isNullOrBlank() && tab != Screen10Tab.AUDIT) {
                item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
            }

            if (state.loading && !state.loaded) {
                item { LoadingState() }
            } else if (state.loaded) {
                item {
                    AppCompactScrollableTabs(
                        labels = Screen10Tab.entries.map { it.label },
                        selectedIndex = Screen10Tab.entries.indexOf(tab),
                        onSelect = { index ->
                            vm.clearFeedback()
                            tabName = Screen10Tab.entries[index].name
                        }
                    )
                }

                when (tab) {
                    Screen10Tab.BASIC -> screen10BasicItems(
                        draft = settingsDraft,
                        onDraft = { settingsDraft = it },
                        busy = state.actionBusy,
                        logoUploading = state.logoUploading,
                        onUploadLogo = { uri ->
                            vm.uploadLogo(uri) { uploaded ->
                                settingsDraft = settingsDraft.copy(
                                    logoUrl = uploaded.url,
                                    logoPublicId = uploaded.publicId
                                )
                            }
                        },
                        onRemoveLogo = {
                            vm.discardPendingLogo(settingsDraft.logoPublicId)
                            settingsDraft = settingsDraft.copy(logoUrl = "", logoPublicId = "")
                        },
                        onSave = {
                            dismissKeyboard()
                            vm.saveSettings(settingsDraft) { onBrandingChanged(settingsDraft.shopName, settingsDraft.logoUrl) }
                        }
                    )
                    Screen10Tab.PUBLIC -> screen10PublicItems(
                        draft = settingsDraft,
                        onDraft = { settingsDraft = it },
                        busy = state.actionBusy,
                        onSave = {
                            dismissKeyboard()
                            vm.saveSettings(settingsDraft) { onBrandingChanged(settingsDraft.shopName, settingsDraft.logoUrl) }
                        }
                    )
                    Screen10Tab.AUDIT -> screen10AuditItems(state, vm)
                }
            }
        }
    }

    AppFeedbackHost(
        successMessage = state.message,
        errorMessage = state.error
    )

    if (confirmDiscardSettings) {
        AppDestructiveConfirmDialog(
            title = "Discard unsaved changes?",
            message = "Your unsaved Settings changes will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            busy = state.actionBusy,
            onConfirm = {
                confirmDiscardSettings = false
                vm.discardPendingLogo(settingsDraft.logoPublicId)
                onBack()
            },
            onDismiss = { confirmDiscardSettings = false }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Screen10WhatsAppManagement(
    currentUser: SessionUser,
    businessDate: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentUser.role != UserRole.OWNER) {
        Column(modifier.fillMaxSize().padding(AppSpacing.md)) {
            EmptyState("Owner access is required.")
        }
        return
    }

    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) {
        Screen10SettingsRepository(
            ApiClient(BuildConfig.API_BASE_URL, SecureSessionStore(appContext)),
            appContext.contentResolver
        )
    }
    val factory = remember(repository) { Screen10SettingsViewModel.Factory(repository) }
    val vm: Screen10SettingsViewModel = viewModel(factory = factory)
    val state = vm.state
    val dismissKeyboard = rememberReliableKeyboardDismiss()

    var settingsDraft by rememberSaveable(stateSaver = Screen10SettingsSaver) { mutableStateOf(Screen10SiteSettings()) }
    var whatsappDraftInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loaded, state.updatedAt) {
        if (state.loaded && !whatsappDraftInitialized) {
            settingsDraft = state.settings
            whatsappDraftInitialized = true
        }
    }
    var editingTemplate by remember { mutableStateOf<Screen10WhatsAppTemplate?>(null) }
    var showTemplateEditor by rememberSaveable { mutableStateOf(false) }
    var deleteTemplate by remember { mutableStateOf<Screen10WhatsAppTemplate?>(null) }
    var sectionName by rememberSaveable { mutableStateOf(Screen10WhatsAppSection.RESERVATION.name) }
    val section = runCatching { Screen10WhatsAppSection.valueOf(sectionName) }
        .getOrDefault(Screen10WhatsAppSection.RESERVATION)
    var editorSectionName by rememberSaveable { mutableStateOf(Screen10WhatsAppSection.RESERVATION.name) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val hasUnsavedLanguage = state.loaded &&
        settingsDraft.whatsappTemplateLanguage != state.settings.whatsappTemplateLanguage
    val navigationBusy = state.actionBusy

    val requestBack: () -> Unit = {
        if (!navigationBusy) {
            if (hasUnsavedLanguage) confirmDiscard = true else onBack()
        }
    }

    LaunchedEffect(Unit) {
        vm.clearFeedback()
        vm.ensureLoaded()
    }
    BackHandler { requestBack() }

    Screen10PullToRefresh(
        enabled = section != Screen10WhatsAppSection.LANGUAGE,
        isRefreshing = state.loading && state.loaded,
        onRefresh = vm::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                top = AppSpacing.xs,
                bottom = AppSpacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            item { MainScreenDateRow(businessDate) }
            item {
                AppBackHeader(
                    title = "WhatsApp Centre",
                    onBack = requestBack,
                    enabled = !navigationBusy,
                    action = {
                        if (section != Screen10WhatsAppSection.LANGUAGE) {
                            CompactNewActionButton(
                                label = "Add Template",
                                enabled = !state.actionBusy,
                                onClick = {
                                    editingTemplate = null
                                    editorSectionName = section.name
                                    showTemplateEditor = true
                                    vm.clearFeedback()
                                }
                            )
                        }
                    }
                )
            }

            if (!state.loadError.isNullOrBlank() && !state.loaded) {
                item {
                    LoadFailureState(
                        message = state.loadError.orEmpty(),
                        onRetry = vm::refresh
                    )
                }
            } else if (!state.loadError.isNullOrBlank()) {
                item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
            }

            if (state.loading && !state.loaded) {
                item { LoadingState() }
            } else if (state.loaded) {
                screen10WhatsAppItems(
                    state = state,
                    section = section,
                    onSection = { sectionName = it.name },
                    settingsDraft = settingsDraft,
                    onSettingsDraft = { settingsDraft = it },
                    onSaveLanguage = { vm.saveSettings(settingsDraft) },
                    onEdit = {
                        editingTemplate = it
                        editorSectionName = screen10WhatsAppSectionForAction(it.linkedAction).name
                        showTemplateEditor = true
                        vm.clearFeedback()
                    },
                    onDelete = { deleteTemplate = it }
                )
            }
        }
    }

    AppFeedbackHost(
        successMessage = state.message,
        errorMessage = state.error.takeUnless { showTemplateEditor }
    )

    if (confirmDiscard) {
        AppDestructiveConfirmDialog(
            title = "Discard unsaved changes?",
            message = "Your unsaved WhatsApp language change will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            busy = state.actionBusy,
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false }
        )
    }

    if (showTemplateEditor) {
        Screen10TemplateEditorSheet(
            existing = editingTemplate,
            initialSection = runCatching { Screen10WhatsAppSection.valueOf(editorSectionName) }
                .getOrDefault(Screen10WhatsAppSection.GENERAL),
            linkedActions = state.linkedActions.ifEmpty { screen10LinkedActionLabels.keys.toList() },
            placeholderRegistry = state.placeholderRegistry,
            placeholderSources = state.placeholderSources,
            busy = state.actionBusy,
            error = state.error,
            onDismiss = { showTemplateEditor = false },
            onSave = { key, name, action, gu, en, active ->
                vm.saveTemplate(
                    existingId = editingTemplate?.id,
                    templateKey = key,
                    templateName = name,
                    linkedAction = action,
                    messageGu = gu,
                    messageEn = en,
                    active = active
                ) { showTemplateEditor = false }
            }
        )
    }

    deleteTemplate?.let { template ->
        AppDestructiveConfirmDialog(
            title = "Delete template?",
            message = "Delete ${template.templateName}? This template will no longer be available for WhatsApp actions.",
            confirmLabel = "Delete",
            busy = state.actionBusy,
            onConfirm = {
                deleteTemplate = null
                vm.deleteTemplate(template.id)
            },
            onDismiss = { deleteTemplate = null }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.screen10BasicItems(
    draft: Screen10SiteSettings,
    onDraft: (Screen10SiteSettings) -> Unit,
    busy: Boolean,
    logoUploading: Boolean,
    onUploadLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onSave: () -> Unit
) {
    val websiteValid = screen10WebsiteUrlValid(draft.websiteUrl)
    val workingHoursValid = screen10WorkingHoursValid(draft.workingHours)
    item {
        AppCard {
            Text("Basic Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(AppSpacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Screen10TextField(draft.shopName, { onDraft(draft.copy(shopName = it.take(160))) }, "Shop Name", "Enter shop name", Icons.Rounded.Business)
                Screen10TextField(draft.websiteTitle, { onDraft(draft.copy(websiteTitle = it.take(160))) }, "Website Title", "Enter website title", Icons.Rounded.Title)
                AppTextField(
                    value = draft.websiteUrl,
                    onValueChange = { onDraft(draft.copy(websiteUrl = it.take(2048))) },
                    label = "Website",
                    placeholder = "Enter website URL",
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = draft.websiteUrl.isNotBlank() && !websiteValid,
                    supportingText = if (draft.websiteUrl.isNotBlank() && !websiteValid) {
                        "Enter a valid http:// or https:// website URL."
                    } else null
                )
                Screen10LogoPicker(
                    logoUrl = draft.logoUrl,
                    uploading = logoUploading,
                    enabled = !busy,
                    onUpload = onUploadLogo,
                    onRemove = onRemoveLogo
                )
                Screen10TextField(
                    draft.contactNumber,
                    { onDraft(draft.copy(contactNumber = it.filter(Char::isDigit).take(15))) },
                    "Contact / Call Number",
                    "Enter contact number",
                    Icons.Rounded.Call,
                    KeyboardType.Phone
                )
                Screen10TextField(
                    draft.whatsappNumber,
                    { onDraft(draft.copy(whatsappNumber = it.filter(Char::isDigit).take(15))) },
                    "WhatsApp Number",
                    "Enter WhatsApp number",
                    Icons.Rounded.Chat,
                    KeyboardType.Phone
                )
                AppTextField(
                    value = draft.address,
                    onValueChange = { onDraft(draft.copy(address = it.take(500))) },
                    label = "Address",
                    placeholder = "Enter address",
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
                Screen10Dropdown(
                    label = "Default Language",
                    selected = draft.defaultLanguage,
                    options = listOf("GU" to "Gujarati", "EN" to "English"),
                    icon = Icons.Rounded.Language,
                    onSelected = { onDraft(draft.copy(defaultLanguage = it)) }
                )
                Screen10Dropdown(
                    label = "Date Format",
                    selected = draft.dateFormat,
                    options = listOf("DD-MM-YYYY" to "DD-MM-YYYY", "YYYY-MM-DD" to "YYYY-MM-DD"),
                    icon = Icons.Rounded.CalendarMonth,
                    onSelected = { onDraft(draft.copy(dateFormat = it)) }
                )
            }
        }
    }
    item {
        AppCard {
            Screen10WorkingHoursEditor(
                workingHours = draft.workingHours,
                enabled = !busy,
                onWorkingHours = { onDraft(draft.copy(workingHours = it)) }
            )
        }
    }
    item {
        PrimaryButton(
            "Save Basic Settings",
            onSave,
            Modifier.fillMaxWidth(),
            enabled = !busy && websiteValid && workingHoursValid,
            loading = busy
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen10WorkingHoursEditor(
    workingHours: Map<String, Screen10WorkingDay>,
    enabled: Boolean,
    onWorkingHours: (Map<String, Screen10WorkingDay>) -> Unit
) {
    var timeTarget by remember { mutableStateOf<Screen10WorkingTimeTarget?>(null) }

    fun normalizedHours(overrides: Map<String, Screen10WorkingDay>): Map<String, Screen10WorkingDay> =
        SCREEN10_WORKING_DAYS.associate { (dayKey, _) ->
            dayKey to (overrides[dayKey] ?: Screen10WorkingDay())
        }

    fun updateDay(dayKey: String, next: Screen10WorkingDay) {
        onWorkingHours(normalizedHours(workingHours + (dayKey to next)))
    }

    Text("Working Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.size(AppSpacing.xs))
    Text(
        "Set opening and closing time for each day.",
        style = MaterialTheme.typography.bodySmall,
        color = AppTextMuted
    )
    Spacer(Modifier.size(AppSpacing.sm))

    SCREEN10_WORKING_DAYS.forEachIndexed { index, (dayKey, dayLabel) ->
        val day = workingHours[dayKey] ?: Screen10WorkingDay()
        val openMinutes = screen10TimeMinutes(day.openTime)
        val closeMinutes = screen10TimeMinutes(day.closeTime)
        val rowValid = !day.isOpen || (openMinutes != null && closeMinutes != null && openMinutes < closeMinutes)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(dayLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        if (day.isOpen) {
                            "${screen10WorkingTimeLabel(day.openTime)} – ${screen10WorkingTimeLabel(day.closeTime)}"
                        } else {
                            "Closed"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextMuted
                    )
                }
                Switch(
                    checked = day.isOpen,
                    onCheckedChange = { updateDay(dayKey, day.copy(isOpen = it)) },
                    enabled = enabled
                )
            }

            if (day.isOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = { timeTarget = Screen10WorkingTimeTarget(dayKey, dayLabel, true) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            "Open ${screen10WorkingTimeLabel(day.openTime)}",
                            modifier = Modifier.padding(start = AppSpacing.xs),
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { timeTarget = Screen10WorkingTimeTarget(dayKey, dayLabel, false) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            "Close ${screen10WorkingTimeLabel(day.closeTime)}",
                            modifier = Modifier.padding(start = AppSpacing.xs),
                            maxLines = 1
                        )
                    }
                }
            }

            if (!rowValid) {
                Text(
                    "Closing time must be later than opening time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (index < SCREEN10_WORKING_DAYS.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.sm))
        }
    }

    Spacer(Modifier.size(AppSpacing.md))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        SecondaryButton(
            text = "Copy Mon → Weekdays",
            onClick = {
                val monday = workingHours["MONDAY"] ?: Screen10WorkingDay()
                val weekdayKeys = setOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
                onWorkingHours(
                    normalizedHours(
                        SCREEN10_WORKING_DAYS.associate { (dayKey, _) ->
                            dayKey to if (dayKey in weekdayKeys) monday.copy()
                            else (workingHours[dayKey] ?: Screen10WorkingDay())
                        }
                    )
                )
            },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
        SecondaryButton(
            text = "Copy Mon → All",
            onClick = {
                val monday = workingHours["MONDAY"] ?: Screen10WorkingDay()
                onWorkingHours(
                    SCREEN10_WORKING_DAYS.associate { (dayKey, _) -> dayKey to monday.copy() }
                )
            },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }

    timeTarget?.let { target ->
        val day = workingHours[target.dayKey] ?: Screen10WorkingDay()
        val current = runCatching {
            LocalTime.parse(if (target.opening) day.openTime else day.closeTime)
        }.getOrDefault(if (target.opening) LocalTime.of(9, 0) else LocalTime.of(20, 0))
        val pickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { timeTarget = null },
            title = {
                Text("${target.dayLabel} · ${if (target.opening) "Opening Time" else "Closing Time"}")
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = String.format(Locale.ENGLISH, "%02d:%02d", pickerState.hour, pickerState.minute)
                        val next = if (target.opening) day.copy(openTime = value) else day.copy(closeTime = value)
                        updateDay(target.dayKey, next)
                        timeTarget = null
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { timeTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.screen10PublicItems(
    draft: Screen10SiteSettings,
    onDraft: (Screen10SiteSettings) -> Unit,
    busy: Boolean,
    onSave: () -> Unit
) {
    item {
        AppCard {
            Text("Public Website", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(AppSpacing.sm))
            Screen10SwitchRow("Public Catalog", draft.publicCatalogEnabled) { onDraft(draft.copy(publicCatalogEnabled = it)) }
            HorizontalDivider()
            Screen10SwitchRow("Show WhatsApp", draft.showWhatsApp) { onDraft(draft.copy(showWhatsApp = it)) }
            HorizontalDivider()
            Screen10SwitchRow("Show Call", draft.showCall) { onDraft(draft.copy(showCall = it)) }
            Spacer(Modifier.size(AppSpacing.sm))
            Screen10Dropdown(
                label = "Availability Display",
                selected = draft.availabilityMode,
                options = listOf("STATUS_ONLY" to "Status Only", "EXACT" to "Exact Quantity"),
                icon = Icons.Rounded.Public,
                onSelected = { onDraft(draft.copy(availabilityMode = it)) }
            )
            Spacer(Modifier.size(AppSpacing.sm))
            Text(
                "Status Only · 0 Not Available · 1–2 Few Left · 3–5 Limited · 6+ Available",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted
            )
        }
    }
    item {
        PrimaryButton("Save Public Website", onSave, Modifier.fillMaxWidth(), enabled = !busy, loading = busy)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.screen10WhatsAppItems(
    state: Screen10SettingsState,
    section: Screen10WhatsAppSection,
    onSection: (Screen10WhatsAppSection) -> Unit,
    settingsDraft: Screen10SiteSettings,
    onSettingsDraft: (Screen10SiteSettings) -> Unit,
    onSaveLanguage: () -> Unit,
    onEdit: (Screen10WhatsAppTemplate) -> Unit,
    onDelete: (Screen10WhatsAppTemplate) -> Unit
) {
    item {
        AppCompactScrollableTabs(
            labels = Screen10WhatsAppSection.entries.map { it.label },
            selectedIndex = Screen10WhatsAppSection.entries.indexOf(section),
            onSelect = { index -> onSection(Screen10WhatsAppSection.entries[index]) }
        )
    }

    if (section == Screen10WhatsAppSection.LANGUAGE) {
        item {
            AppCard {
                Text("Global Template Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(AppSpacing.xs))
                Screen10Dropdown(
                    label = "Send Language",
                    selected = settingsDraft.whatsappTemplateLanguage,
                    options = listOf(
                        "GUJARATI" to "Gujarati",
                        "ENGLISH" to "English",
                        "BOTH" to "Both"
                    ),
                    icon = Icons.Rounded.Language,
                    onSelected = { onSettingsDraft(settingsDraft.copy(whatsappTemplateLanguage = it)) }
                )
                Spacer(Modifier.size(AppSpacing.sm))
                PrimaryButton(
                    text = "Save Language",
                    onClick = onSaveLanguage,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.actionBusy,
                    loading = state.actionBusy
                )
            }
        }
        return
    }

    item {
        Text(
            section.label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    val sectionTemplates = state.templates.filter { screen10WhatsAppSectionForAction(it.linkedAction) == section }
    if (sectionTemplates.isEmpty()) {
        item { EmptyState("No ${section.label.lowercase(Locale.ENGLISH)} WhatsApp templates.") }
    } else {
        sectionTemplates.forEach { template ->
            item(key = template.id) { Screen10TemplateCard(template, state.actionBusy, onEdit, onDelete) }
        }
    }
}

@Composable
private fun Screen10TemplateCard(
    template: Screen10WhatsAppTemplate,
    busy: Boolean,
    onEdit: (Screen10WhatsAppTemplate) -> Unit,
    onDelete: (Screen10WhatsAppTemplate) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Column(Modifier.weight(1f)) {
                Text(template.templateName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    screen10LinkedActionLabels[template.linkedAction] ?: template.linkedAction.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextMuted
                )
            }
            StatusBadge("Bilingual", BadgeTone.INFO)
            StatusBadge(if (template.active) "Active" else "Inactive", if (template.active) BadgeTone.SUCCESS else BadgeTone.WARNING)
        }
        Spacer(Modifier.size(AppSpacing.xs))
        Text(template.messageGu, style = MaterialTheme.typography.bodyMedium)
        if (template.messageGu.isNotBlank() && template.messageEn.isNotBlank()) Spacer(Modifier.size(AppSpacing.xs))
        Text(template.messageEn, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            SecondaryButton(
                "Edit",
                { onEdit(template) },
                Modifier.weight(1f),
                enabled = !busy,
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            SecondaryButton(
                "Delete",
                { onDelete(template) },
                Modifier.weight(1f),
                enabled = !busy,
                icon = { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.screen10AuditItems(
    state: Screen10SettingsState,
    vm: Screen10SettingsViewModel
) {
    item {
        Text(
            "Audit Log",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
    item {
        CompactSearchField(
            value = state.auditSearch,
            onValueChange = vm::auditSearch,
            placeholder = "Search audit log",
            modifier = Modifier.fillMaxWidth()
        )
    }
    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Screen10CompactFilter(
                "Module",
                state.auditModule,
                listOf("ALL" to "All Modules") + state.auditModules.map { it to it },
                Modifier.weight(1f),
                vm::setAuditModule
            )
            Screen10CompactFilter(
                "Action",
                state.auditAction,
                listOf("ALL" to "All Actions") + state.auditActions.map { it to it },
                Modifier.weight(1f),
                vm::setAuditAction
            )
        }
    }
    item {
        Screen10CompactFilter(
            "User",
            state.auditUserId,
            listOf("ALL" to "All Users") + state.users.map { it.id to "${it.name} · ${it.role}" },
            Modifier.fillMaxWidth(),
            vm::setAuditUser
        )
    }
    item {
        Screen10AuditDateFilters(
            state.auditFromDate,
            state.auditToDate,
            vm::setAuditDates,
            vm::applyAuditDates,
            vm::resetAuditFilters
        )
    }
    if (state.auditLoaded) {
        item {
            Text(
                "${state.auditTotal} records",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTextMuted
            )
        }
    }
    if (!state.loadError.isNullOrBlank() && state.auditLoaded) {
        item { InlineStatusMessage(state.loadError, MessageTone.ERROR) }
    }

    if (state.auditLoading && !state.auditLoaded) {
        item { LoadingState() }
    } else if (!state.auditLoaded && !state.loadError.isNullOrBlank()) {
        item {
            LoadFailureState(
                message = state.loadError.orEmpty(),
                onRetry = vm::refreshAudit
            )
        }
    } else if (state.auditLoaded && state.auditItems.isEmpty() && state.loadError.isNullOrBlank()) {
        item { EmptyState("No audit records found.") }
    } else {
        state.auditItems.forEach { log -> item(key = log.id) { Screen10AuditCard(log) } }
    }
    if (state.canLoadMoreAudit) {
        item(key = "screen10-audit-load-more-${state.auditPage}") {
            LaunchedEffect(state.auditPage, state.auditItems.size) { vm.loadMoreAudit() }
            LoadingState()
        }
    }
    if (!state.auditLoadMoreError.isNullOrBlank()) {
        item(key = "screen10-audit-load-more-retry-${state.auditPage}") {
            InlineRetryMessage(
                message = state.auditLoadMoreError.orEmpty(),
                onRetry = vm::retryLoadMoreAudit,
                retryLabel = "Retry loading more"
            )
        }
    }
}

@Composable
private fun Screen10AuditCard(log: Screen10AuditLog) {
    var expanded by rememberSaveable(log.id) { mutableStateOf(false) }
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${log.action} · ${log.module}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    listOfNotNull(log.userName, log.userRole).joinToString(" · ").ifBlank { "System" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted
                )
                if (!log.recordId.isNullOrBlank()) {
                    Text(
                        log.recordId,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextMuted,
                        maxLines = 1
                    )
                }
            }
            Text(
                screen10FormatTimestamp(log.createdAt),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AppTextMuted
            )
        }
        if (!log.oldValueJson.isNullOrBlank() || !log.newValueJson.isNullOrBlank()) {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide changes" else "Show changes") }
            if (expanded) {
                if (!log.oldValueJson.isNullOrBlank()) {
                    Text("Before", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(log.oldValueJson, style = MaterialTheme.typography.bodySmall)
                }
                if (!log.newValueJson.isNullOrBlank()) {
                    Spacer(Modifier.size(AppSpacing.xs))
                    Text("After", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(log.newValueJson, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen10TemplateEditorSheet(
    existing: Screen10WhatsAppTemplate?,
    initialSection: Screen10WhatsAppSection,
    linkedActions: List<String>,
    placeholderRegistry: Map<String, List<String>>,
    placeholderSources: Map<String, String>,
    busy: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.templateName.orEmpty()) }
    var key by rememberSaveable(existing?.id) { mutableStateOf(existing?.templateKey.orEmpty()) }
    var linkedAction by rememberSaveable(existing?.id) { mutableStateOf(existing?.linkedAction.orEmpty()) }
    var messageGu by rememberSaveable(existing?.id) { mutableStateOf(existing?.messageGu.orEmpty()) }
    var messageEn by rememberSaveable(existing?.id) { mutableStateOf(existing?.messageEn.orEmpty()) }
    var nameTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var messageGuTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var messageEnTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var active by rememberSaveable(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var showDiscardChanges by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val dismissKeyboard = rememberReliableKeyboardDismiss()

    fun chooseLinkedAction(value: String) {
        linkedAction = value
        if (existing == null || key.isBlank()) key = value.lowercase(Locale.ENGLISH)
        if (existing == null || name.isBlank()) name = screen10LinkedActionLabels[value] ?: value
    }

    val sectionActions = remember(initialSection, linkedActions) {
        screen10WhatsAppSectionActions(initialSection, linkedActions)
    }
    val actionListState = rememberLazyListState()
    LaunchedEffect(linkedAction, sectionActions) {
        val index = sectionActions.indexOf(linkedAction)
        if (index >= 0) actionListState.animateScrollToItem(index)
    }
    val relevantPlaceholders = placeholderRegistry[linkedAction].orEmpty()
    val valid = name.trim().isNotBlank() &&
        key.trim().isNotBlank() &&
        linkedAction.isNotBlank() &&
        messageGu.trim().isNotBlank() &&
        messageEn.trim().isNotBlank()
    val dirty =
        name != existing?.templateName.orEmpty() ||
        key != existing?.templateKey.orEmpty() ||
        linkedAction != existing?.linkedAction.orEmpty() ||
        messageGu != existing?.messageGu.orEmpty() ||
        messageEn != existing?.messageEn.orEmpty() ||
        active != (existing?.active ?: true)
    val requestDismiss: () -> Unit = {
        if (!busy) {
            dismissKeyboard()
            if (dirty) showDiscardChanges = true else onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = requestDismiss, sheetState = sheetState) {
        AppFormSheetScaffold(
            title = if (existing == null) "Add WhatsApp Template" else "Edit WhatsApp Template",
            subtitle = initialSection.label,
            busy = busy,
            onDismiss = requestDismiss,
            onSave = {
                val saveKey = if (existing == null) screen10TemplateKey(linkedAction, name) else key
                onSave(saveKey, name, linkedAction, messageGu, messageEn, active)
            },
            saveEnabled = valid,
            saveLabel = if (existing == null) "Add Template" else "Save",
            expanded = true
        ) {
                Screen10TextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it.take(120)
                    },
                    label = "Template Name",
                    placeholder = "Enter template name",
                    icon = Icons.Rounded.Title,
                    required = true,
                    isError = nameTouched && name.trim().isBlank(),
                    supportingText = if (nameTouched && name.trim().isBlank()) "Template name is required." else null
                )
                Text("Linked Action * · ${initialSection.label}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    state = actionListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    itemsIndexed(sectionActions, key = { _, action -> action }) { _, action ->
                        FilterChip(
                            selected = linkedAction == action,
                            onClick = { chooseLinkedAction(action) },
                            label = { Text(screen10LinkedActionLabels[action] ?: action.replace('_', ' '), maxLines = 1) },
                            leadingIcon = if (linkedAction == action) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
                AppTextField(
                    value = messageGu,
                    onValueChange = {
                        messageGuTouched = true
                        messageGu = it.take(3000)
                    },
                    label = "Gujarati Message *",
                    placeholder = "Enter Gujarati message",
                    leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                    singleLine = false,
                    minLines = 4,
                    maxLines = 8,
                    isError = messageGuTouched && messageGu.trim().isBlank(),
                    supportingText = if (messageGuTouched && messageGu.trim().isBlank()) "Gujarati message is required." else null
                )
                Screen10PlaceholderInsertDropdown(
                    placeholders = relevantPlaceholders,
                    placeholderSources = placeholderSources,
                    onSelected = { token -> messageGu = screen10AppendPlaceholder(messageGu, token) }
                )
                AppTextField(
                    value = messageEn,
                    onValueChange = {
                        messageEnTouched = true
                        messageEn = it.take(3000)
                    },
                    label = "English Message *",
                    placeholder = "Enter English message",
                    leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                    singleLine = false,
                    minLines = 4,
                    maxLines = 8,
                    isError = messageEnTouched && messageEn.trim().isBlank(),
                    supportingText = if (messageEnTouched && messageEn.trim().isBlank()) "English message is required." else null
                )
                Screen10PlaceholderInsertDropdown(
                    placeholders = relevantPlaceholders,
                    placeholderSources = placeholderSources,
                    onSelected = { token -> messageEn = screen10AppendPlaceholder(messageEn, token) }
                )
                TextButton(
                    onClick = dismissKeyboard,
                    enabled = !busy
                ) {
                    Icon(Icons.Rounded.KeyboardHide, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Hide Keyboard")
                }
                if (!error.isNullOrBlank()) {
                    InlineStatusMessage(error, MessageTone.ERROR)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Template", style = MaterialTheme.typography.bodyLarge)
                        Text("Only one active template per linked action.", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
        }
    }
    if (showDiscardChanges) {
        AppUnsavedChangesDialog(
            onKeepEditing = { showDiscardChanges = false },
            onDiscard = {
                showDiscardChanges = false
                onDismiss()
            }
        )
    }

}

@Composable
private fun Screen10PlaceholderInsertDropdown(
    placeholders: List<String>,
    placeholderSources: Map<String, String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = placeholders.isNotEmpty()
        ) {
            Icon(Icons.Rounded.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Insert Placeholder", modifier = Modifier.weight(1f).padding(start = AppSpacing.xs))
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        if (expanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                placeholders.forEach { raw ->
                    val placeholderKey = raw.removePrefix("{").removeSuffix("}")
                    val token = "{{$placeholderKey}}"
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(token, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    placeholderSources[placeholderKey] ?: "Template data",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTextMuted
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelected(token)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Screen10LogoPicker(
    logoUrl: String,
    uploading: Boolean,
    enabled: Boolean,
    onUpload: (Uri) -> Unit,
    onRemove: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onUpload)
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text("Business Logo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            if (logoUrl.isNotBlank()) {
                RoundedItemImage(
                    model = logoUrl,
                    contentDescription = "Current business logo",
                    modifier = Modifier.size(84.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(32.dp), tint = AppTextMuted)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    if (logoUrl.isBlank()) "No logo uploaded" else "Current logo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text("JPG, PNG or WebP · max 8 MB", style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            PrimaryButton(
                text = if (logoUrl.isBlank()) "Upload Logo" else "Change Logo",
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.weight(1f),
                enabled = enabled && !uploading,
                loading = uploading,
                icon = { Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            if (logoUrl.isNotBlank()) {
                SecondaryButton(
                    text = "Remove Logo",
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    enabled = enabled && !uploading,
                    icon = { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@Composable
private fun Screen10TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    required: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (required) "$label *" else label,
        placeholder = placeholder,
        leadingIcon = { Icon(icon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = supportingText
    )
}

@Composable
private fun Screen10SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Screen10Dropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    prompt: String = "Select Option",
    onSelected: (String) -> Unit
) {
    AppSelectField(
        label = label,
        selected = selected,
        options = options.map { (value, text) -> AppFilterOption(value, text) },
        onSelected = onSelected,
        modifier = modifier,
        prompt = prompt,
        icon = icon
    )
}

@Composable
private fun Screen10CompactFilter(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    AppSingleSelectFilter(
        label = label,
        selected = selected,
        options = options.map { (value, text) -> AppFilterOption(value, text) },
        resetValue = "ALL",
        onApply = onSelected,
        modifier = modifier,
        title = "Filter Audit Log · $label"
    )
}

@Composable
private fun Screen10AuditDateFilters(
    fromDate: String,
    toDate: String,
    onDates: (String, String) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    val parsedFrom = runCatching { LocalDate.parse(fromDate) }.getOrNull()
    val parsedTo = runCatching { LocalDate.parse(toDate) }.getOrNull()

    ResponsiveCompactPair(
        first = { child ->
            AppDatePickerField(
                label = "From Date",
                value = fromDate,
                onValueChange = { onDates(it, toDate) },
                modifier = child,
                allowClear = true,
                maxDate = parsedTo
            )
        },
        second = { child ->
            AppDatePickerField(
                label = "To Date",
                value = toDate,
                onValueChange = { onDates(fromDate, it) },
                modifier = child,
                allowClear = true,
                minDate = parsedFrom
            )
        }
    )
    Spacer(Modifier.size(AppSpacing.xs))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SecondaryButton("Reset", onReset, Modifier.weight(1f))
        PrimaryButton("Apply", onApply, Modifier.weight(1f))
    }
}

private fun screen10FormatTimestamp(raw: String): String {
    if (raw.isBlank()) return ""
    val utcDateTime = runCatching { java.time.LocalDateTime.parse(raw.replace(' ', 'T')) }.getOrNull()
    val istDateTime = utcDateTime
        ?.atOffset(ZoneOffset.UTC)
        ?.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    return istDateTime?.format(DateTimeFormatter.ofPattern("dd-MM-yyyy · h:mm a", Locale.ENGLISH)) ?: raw
}
