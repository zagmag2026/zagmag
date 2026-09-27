package com.nimsdeveloper.zhagmagdresses.admin

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
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal data class Screen8ReportsState(
    val bootstrap: Screen8Bootstrap? = null,
    val config: Screen8ReportConfig = Screen8ReportConfig(),
    val generatedConfig: Screen8ReportConfig? = null,
    val title: String = "",
    val columns: List<Screen8Column> = emptyList(),
    val rows: List<Screen8ReportRow> = emptyList(),
    val summary: List<Screen8SummaryItem> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val total: Int = 0,
    val loadingBootstrap: Boolean = false,
    val loadingReport: Boolean = false,
    val loadingMore: Boolean = false,
    val exportLoading: Boolean = false,
    val exportData: Screen8ReportPage? = null,
    val exportKey: String? = null,
    val presetBusy: Boolean = false,
    val loadError: String? = null,
    val loadMoreError: String? = null,
    val error: String? = null,
    val message: String? = null
) {
    val generated: Boolean get() = generatedConfig != null
    val canLoadMore: Boolean get() = generated && !loadingMore && !loadingReport && loadError == null && loadMoreError == null && page < totalPages
    val hasRows: Boolean get() = rows.isNotEmpty()
}

internal class Screen8ReportsViewModel(
    private val repository: Screen8ReportsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val restoredConfig: Screen8ReportConfig =
        savedStateHandle.get<String>("reportsConfig")
            ?.let { runCatching { JSONObject(it).toScreen8ReportConfig() }.getOrNull() }
            ?: Screen8ReportConfig()
    var state by mutableStateOf(Screen8ReportsState(config = restoredConfig))
        private set

    private val zone = ZoneId.of("Asia/Kolkata")
    private var reportRequestSerial = 0L
    private var exportRequestSerial = 0L
    private var seenMetadataRevision = AdminDataFreshness.reportsMetadataRevision
    private var seenDataRevision = AdminDataFreshness.reportsDataRevision

    fun ensureLoaded() {
        val metadataStale = seenMetadataRevision != AdminDataFreshness.reportsMetadataRevision
        val dataStale = seenDataRevision != AdminDataFreshness.reportsDataRevision
        if (dataStale) {
            invalidateGeneratedForFreshness()
            seenDataRevision = AdminDataFreshness.reportsDataRevision
        }
        if (state.bootstrap != null && !metadataStale) {
            if (state.config.type == "OPERATIONS_OVERVIEW" && !state.generated && !state.loadingReport) generate()
            return
        }
        loadBootstrap(autoGenerateOverview = state.bootstrap == null)
    }

    fun refreshBootstrap() {
        loadBootstrap(autoGenerateOverview = false)
    }

    private fun loadBootstrap(autoGenerateOverview: Boolean) {
        if (state.loadingBootstrap) return
        val initialLoad = state.bootstrap == null
        viewModelScope.launch {
            state = state.copy(loadingBootstrap = true, loadError = null)
            runCatching { repository.bootstrap() }
                .onSuccess { bootstrap ->
                    seenMetadataRevision = AdminDataFreshness.reportsMetadataRevision
                    val config = if (initialLoad) {
                        normalizeForType(
                            applyDatePreset(
                                state.config.copy(datePreset = state.config.datePreset.ifBlank { "TODAY" }),
                                currentBusinessDate()
                            )
                        )
                    } else {
                        normalizeForType(resolvePresetDates(state.config))
                    }
                    state = state.copy(
                        bootstrap = bootstrap,
                        config = config,
                        loadingBootstrap = false,
                        loadError = null,
                        loadMoreError = null
                    )
                    if (autoGenerateOverview && state.config.type == "OPERATIONS_OVERVIEW" && !state.generated) {
                        generate()
                    }
                }
                .onFailure { error ->
                    state = state.copy(loadingBootstrap = false, loadError = userMessage(error))
                }
        }
    }

    fun setSection(section: String, isOwner: Boolean) {
        setType(screen8DefaultTypeForSection(section, isOwner))
    }

    fun setType(value: String) {
        val type = value.uppercase()
        val dateBasis = screen8DateBasisOptions(type).firstOrNull()?.value.orEmpty()
        val grouping = screen8GroupingOptions(type).firstOrNull()?.value ?: "NONE"
        val next = state.config.copy(
            type = type,
            dateBasis = dateBasis,
            grouping = grouping,
            dashboardFilter = "",
            status = if (screen8SupportsStatus(type)) state.config.status else "",
            categoryId = if (screen8SupportsCategory(type)) state.config.categoryId else "",
            itemSearch = if (screen8SupportsItem(type)) state.config.itemSearch else "",
            customerSearch = if (screen8SupportsCustomer(type)) state.config.customerSearch else "",
            staffUserId = if (screen8SupportsStaff(type)) state.config.staffUserId else "",
            customFilters = if (screen8SupportsCustomFields(type)) state.config.customFilters else emptyMap()
        )
        replaceConfig(normalizeForType(next))
    }

    fun openFromDashboard(
        type: String,
        datePreset: String = "ALL_TIME",
        dashboardFilter: String = "",
        status: String = ""
    ) {
        val normalizedType = type.uppercase()
        val dateBasis = screen8DateBasisOptions(normalizedType).firstOrNull()?.value.orEmpty()
        val grouping = screen8GroupingOptions(normalizedType).firstOrNull()?.value ?: "NONE"
        var config = Screen8ReportConfig(
            type = normalizedType,
            dateBasis = dateBasis,
            datePreset = datePreset.uppercase(),
            status = status.uppercase(),
            grouping = grouping,
            sort = "NEWEST",
            dashboardFilter = dashboardFilter.uppercase()
        )
        config = when (config.datePreset) {
            "TODAY", "YESTERDAY", "THIS_WEEK", "THIS_MONTH" ->
                applyDatePreset(config, currentBusinessDate())
            else -> config
        }
        replaceConfig(normalizeForType(config))
        generate()
    }

    fun clearDashboardDrilldown() {
        if (state.config.dashboardFilter.isNotBlank()) {
            replaceConfig(Screen8ReportConfig())
        }
    }

    fun setDateBasis(value: String) = updateConfig(state.config.copy(dateBasis = value.uppercase()))

    fun setDatePreset(value: String) {
        updateConfig(applyDatePreset(state.config.copy(datePreset = value.uppercase()), currentBusinessDate()))
    }

    fun setFromDate(value: String) = updateConfig(
        state.config.copy(fromDate = value, datePreset = "CUSTOM")
    )

    fun setToDate(value: String) = updateConfig(
        state.config.copy(toDate = value, datePreset = "CUSTOM")
    )

    fun setCategory(value: String) = updateConfig(
        state.config.copy(categoryId = value, customFilters = emptyMap())
    )

    fun setCustomFilter(fieldId: String, value: String) {
        val next = state.config.customFilters.toMutableMap()
        val trimmed = value.trim().take(160)
        if (trimmed.isBlank()) next.remove(fieldId) else next[fieldId] = trimmed
        updateConfig(state.config.copy(customFilters = next))
    }

    fun setItemSearch(value: String) = updateConfig(state.config.copy(itemSearch = value.take(160)))
    fun setCustomerSearch(value: String) = updateConfig(state.config.copy(customerSearch = value.take(160)))
    fun setStatus(value: String) = updateConfig(state.config.copy(status = value.uppercase()))
    fun setStaff(value: String) = updateConfig(state.config.copy(staffUserId = value))
    fun setGrouping(value: String) = updateConfig(state.config.copy(grouping = value.uppercase()))
    fun setSort(value: String) = updateConfig(state.config.copy(sort = value.uppercase()))
    fun setSearch(value: String) = updateConfig(state.config.copy(search = value.take(160)))

    fun resetCurrentFilters() {
        val type = state.config.type
        val today = currentBusinessDate()
        val reset = Screen8ReportConfig(
            type = type,
            dateBasis = screen8DateBasisOptions(type).firstOrNull()?.value.orEmpty(),
            datePreset = "THIS_MONTH",
            grouping = screen8GroupingOptions(type).firstOrNull()?.value ?: "NONE",
            sort = "NEWEST"
        )
        replaceConfig(
            normalizeForType(
                if (screen8SupportsDates(type)) applyDatePreset(reset, today) else reset
            )
        )
    }

    fun applyPreset(preset: Screen8Preset) {
        val normalized = normalizeForType(resolvePresetDates(preset.config))
        replaceConfig(normalized)
    }

    fun generate() {
        if (state.loadingReport || state.loadingMore) return
        val config = normalizeForType(resolvePresetDates(state.config))
        val validation = validate(config)
        if (validation != null) {
            state = state.copy(error = validation, message = null)
            return
        }
        val requestId = ++reportRequestSerial
        val dataRevision = AdminDataFreshness.reportsDataRevision
        state = state.copy(
            loadingReport = true,
            loadingMore = false,
            loadError = null,
            error = null,
            message = null,
            title = "",
            columns = emptyList(),
            rows = emptyList(),
            summary = emptyList(),
            page = 0,
            totalPages = 1,
            total = 0,
            generatedConfig = config,
            config = config,
            exportData = null,
            exportKey = null
        )
        viewModelScope.launch {
            runCatching { repository.report(config, page = 1, pageSize = 10) }
                .onSuccess { page ->
                    if (
                        requestId != reportRequestSerial ||
                        state.generatedConfig != config ||
                        state.config != config ||
                        dataRevision != AdminDataFreshness.reportsDataRevision
                    ) return@onSuccess
                    seenDataRevision = dataRevision
                    state = state.copy(
                        loadingReport = false,
                        title = page.title,
                        columns = page.columns,
                        rows = page.rows,
                        summary = page.summary,
                        page = page.page,
                        totalPages = page.totalPages,
                        total = page.total,
                        generatedConfig = config,
                        config = config,
                        loadError = null,
                        error = null
                    )
                }
                .onFailure { error ->
                    if (requestId == reportRequestSerial && state.generatedConfig == config) {
                        state = state.copy(
                            loadingReport = false,
                            generatedConfig = null,
                            error = userMessage(error)
                        )
                    }
                }
        }
    }

    fun refreshGenerated() {
        val existing = state.generatedConfig ?: return
        if (state.loadingReport || state.loadingMore) return
        val config = normalizeForType(resolvePresetDates(existing))
        val requestId = ++reportRequestSerial
        val dataRevision = AdminDataFreshness.reportsDataRevision
        state = state.copy(
            loadingReport = true,
            loadError = null,
            error = null,
            generatedConfig = config,
            config = if (state.config.datePreset == existing.datePreset) config else state.config,
            exportData = null,
            exportKey = null
        )
        viewModelScope.launch {
            runCatching { repository.report(config, page = 1, pageSize = 10) }
                .onSuccess { page ->
                    if (
                        requestId != reportRequestSerial ||
                        state.generatedConfig != config ||
                        dataRevision != AdminDataFreshness.reportsDataRevision
                    ) return@onSuccess
                    seenDataRevision = dataRevision
                    state = state.copy(
                        loadingReport = false,
                        title = page.title,
                        columns = page.columns,
                        rows = page.rows,
                        summary = page.summary,
                        page = page.page,
                        totalPages = page.totalPages,
                        total = page.total,
                        loadError = null,
                        error = null
                    )
                }
                .onFailure { error ->
                    if (requestId == reportRequestSerial && state.generatedConfig == config) {
                        state = state.copy(loadingReport = false, loadError = userMessage(error))
                    }
                }
        }
    }

    fun loadMore() {
        val config = state.generatedConfig ?: return
        if (!state.canLoadMore) return
        val nextPage = state.page + 1
        val requestId = ++reportRequestSerial
        val dataRevision = AdminDataFreshness.reportsDataRevision
        state = state.copy(loadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            runCatching { repository.report(config, page = nextPage, pageSize = 10) }
                .onSuccess { page ->
                    if (
                        requestId != reportRequestSerial ||
                        state.generatedConfig != config ||
                        dataRevision != AdminDataFreshness.reportsDataRevision
                    ) return@onSuccess
                    seenDataRevision = dataRevision
                    state = state.copy(
                        loadingMore = false,
                        rows = (state.rows + page.rows).distinctBy { it.values },
                        page = page.page,
                        totalPages = page.totalPages,
                        total = page.total,
                        loadError = null
                    )
                }
                .onFailure { error ->
                    if (requestId == reportRequestSerial && state.generatedConfig == config) {
                        state = state.copy(loadingMore = false, loadMoreError = userMessage(error))
                    }
                }
        }
    }

    fun retryLoadMore() {
        if (state.loadingMore || state.loadMoreError.isNullOrBlank() || state.page >= state.totalPages) return
        state = state.copy(loadMoreError = null)
        loadMore()
    }

    fun requestExport() {
        val config = state.generatedConfig ?: return
        if (!state.hasRows || state.exportLoading) return
        val dataRevision = AdminDataFreshness.reportsDataRevision
        val key = config.cacheKey() + "|dataRevision=" + dataRevision
        if (state.exportKey == key && state.exportData != null) return
        val requestId = ++exportRequestSerial
        state = state.copy(exportLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.report(config, page = 1, pageSize = 10, exportAll = true) }
                .onSuccess { export ->
                    if (
                        requestId != exportRequestSerial ||
                        state.generatedConfig != config ||
                        dataRevision != AdminDataFreshness.reportsDataRevision
                    ) return@onSuccess
                    state = state.copy(
                        exportLoading = false,
                        exportData = export,
                        exportKey = key,
                        error = null
                    )
                }
                .onFailure { error ->
                    if (requestId == exportRequestSerial && state.generatedConfig == config) {
                        state = state.copy(exportLoading = false, error = userMessage(error))
                    }
                }
        }
    }

    fun savePreset(
        existingId: String?,
        name: String,
        visibility: String
    ) {
        if (state.presetBusy) return
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            state = state.copy(error = "Preset name is required.", message = null)
            return
        }
        state = state.copy(presetBusy = true, error = null, message = null)
        viewModelScope.launch {
            val mutation = runCatching {
                if (existingId.isNullOrBlank()) {
                    repository.createPreset(trimmed, visibility, normalizeForType(state.config))
                } else {
                    repository.updatePreset(
                        existingId,
                        trimmed,
                        visibility,
                        normalizeForType(state.config),
                        state.bootstrap?.presets?.firstOrNull { it.id == existingId }?.updatedAt
                    )
                }
            }
            if (mutation.isFailure) {
                state = state.copy(presetBusy = false, error = userMessage(mutation.exceptionOrNull()!!))
                return@launch
            }

            AdminDataFreshness.markReportsMetadataMutation()
            val successMessage = if (existingId.isNullOrBlank()) "Preset saved." else "Preset updated."
            runCatching { repository.bootstrap() }
                .onSuccess { bootstrap ->
                    seenMetadataRevision = AdminDataFreshness.reportsMetadataRevision
                    state = state.copy(
                        presetBusy = false,
                        bootstrap = bootstrap,
                        message = successMessage,
                        loadError = null,
                        error = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        presetBusy = false,
                        message = successMessage,
                        loadError = "Preset was saved, but latest report metadata could not be loaded. Refresh to sync.",
                        error = null
                    )
                }
        }
    }

    fun deletePreset(id: String) {
        if (state.presetBusy) return
        state = state.copy(presetBusy = true, error = null, message = null)
        viewModelScope.launch {
            val mutation = runCatching { repository.deletePreset(id) }
            if (mutation.isFailure) {
                state = state.copy(presetBusy = false, error = userMessage(mutation.exceptionOrNull()!!))
                return@launch
            }

            AdminDataFreshness.markReportsMetadataMutation()
            runCatching { repository.bootstrap() }
                .onSuccess { bootstrap ->
                    seenMetadataRevision = AdminDataFreshness.reportsMetadataRevision
                    state = state.copy(
                        presetBusy = false,
                        bootstrap = bootstrap,
                        message = "Preset deleted.",
                        loadError = null,
                        error = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        presetBusy = false,
                        message = "Preset deleted.",
                        loadError = "Preset was deleted, but latest report metadata could not be loaded. Refresh to sync.",
                        error = null
                    )
                }
        }
    }

    fun clearFeedback() {
        state = state.copy(loadError = null, error = null, message = null)
    }

    private fun replaceConfig(config: Screen8ReportConfig) {
        savedStateHandle["reportsConfig"] = config.toJson().toString()
        reportRequestSerial += 1
        exportRequestSerial += 1
        state = state.copy(
            config = config,
            generatedConfig = null,
            title = "",
            columns = emptyList(),
            rows = emptyList(),
            summary = emptyList(),
            page = 0,
            totalPages = 1,
            total = 0,
            loadingReport = false,
            loadingMore = false,
            exportLoading = false,
            exportData = null,
            exportKey = null,
            loadError = null,
            error = null,
            message = null
        )
    }

    private fun updateConfig(config: Screen8ReportConfig) {
        replaceConfig(normalizeForType(config))
    }

    private fun resolvePresetDates(config: Screen8ReportConfig): Screen8ReportConfig {
        val preset = config.datePreset.uppercase()
        val today = currentBusinessDate()
        return when (preset) {
            "TODAY", "YESTERDAY", "THIS_WEEK", "THIS_MONTH" ->
                applyDatePreset(config.copy(datePreset = preset), today)
            else -> config
        }
    }

    private fun currentBusinessDate(): LocalDate = LocalDate.now(zone)

    private fun invalidateGeneratedForFreshness() {
        reportRequestSerial += 1
        exportRequestSerial += 1
        state = state.copy(
            generatedConfig = null,
            title = "",
            columns = emptyList(),
            rows = emptyList(),
            summary = emptyList(),
            page = 0,
            totalPages = 1,
            total = 0,
            loadingReport = false,
            loadingMore = false,
            exportLoading = false,
            exportData = null,
            exportKey = null,
            loadError = null,
            error = null
        )
    }

    private fun normalizeForType(config: Screen8ReportConfig): Screen8ReportConfig {
        val type = config.type.uppercase()
        val dateOptions = screen8DateBasisOptions(type)
        val groupOptions = screen8GroupingOptions(type)
        return config.copy(
            type = type,
            dateBasis = dateOptions.firstOrNull { it.value == config.dateBasis }?.value
                ?: dateOptions.firstOrNull()?.value.orEmpty(),
            grouping = groupOptions.firstOrNull { it.value == config.grouping }?.value ?: "NONE",
            status = if (screen8SupportsStatus(type)) config.status else "",
            categoryId = if (screen8SupportsCategory(type)) config.categoryId else "",
            itemSearch = if (screen8SupportsItem(type)) config.itemSearch else "",
            customerSearch = if (screen8SupportsCustomer(type)) config.customerSearch else "",
            staffUserId = if (screen8SupportsStaff(type)) config.staffUserId else "",
            customFilters = if (screen8SupportsCustomFields(type) && config.categoryId.isNotBlank()) {
                config.customFilters
            } else {
                emptyMap()
            }
        )
    }

    private fun validate(config: Screen8ReportConfig): String? {
        val from = config.fromDate.takeIf(String::isNotBlank)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val to = config.toDate.takeIf(String::isNotBlank)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (config.datePreset == "CUSTOM" && screen8SupportsDates(config.type)) {
            if (from == null || to == null) return "Select From Date and To Date."
        }
        if (from != null && to != null && from.isAfter(to)) return "From Date cannot be after To Date."
        return null
    }

    private fun applyDatePreset(config: Screen8ReportConfig, today: LocalDate): Screen8ReportConfig {
        return when (config.datePreset) {
            "TODAY" -> config.copy(fromDate = today.toString(), toDate = today.toString())
            "YESTERDAY" -> {
                val day = today.minusDays(1)
                config.copy(fromDate = day.toString(), toDate = day.toString())
            }
            "THIS_WEEK" -> config.copy(
                fromDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString(),
                toDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toString()
            )
            "THIS_MONTH" -> config.copy(
                fromDate = today.withDayOfMonth(1).toString(),
                toDate = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
            )
            else -> config
        }
    }

    private fun Screen8ReportConfig.cacheKey(): String = (
        listOf(
            type, dateBasis, datePreset, fromDate, toDate, categoryId, itemSearch,
            customerSearch, status, staffUserId, grouping, sort, search, dashboardFilter
        ) + customFilters.toSortedMap().map { (key, value) -> "$key=$value" }
    ).joinToString("|")

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    class Factory(
        private val repository: Screen8ReportsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            Screen8ReportsViewModel(repository, extras.createSavedStateHandle()) as T
    }
}
