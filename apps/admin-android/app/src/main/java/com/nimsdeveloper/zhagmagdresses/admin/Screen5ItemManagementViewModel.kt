package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class Screen5ItemManagementState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val data: Screen5Bootstrap = Screen5Bootstrap(emptyList(), emptyList(), emptyList()),
    val itemItems: List<Screen5Item> = emptyList(),
    val itemPage: Int = 1,
    val itemTotalPages: Int = 1,
    val itemTotal: Int = 0,
    val itemLoading: Boolean = false,
    val itemLoaded: Boolean = false,
    val itemSearch: String = "",
    val itemCategoryId: String = "",
    val itemLoadError: String? = null,
    val itemLoadMoreError: String? = null,
    val notice: String? = null,
    val error: String? = null,
    val loadError: String? = null
) {
    val canLoadMoreItems: Boolean
        get() = itemLoaded && !itemLoading && itemLoadError == null && itemLoadMoreError == null && itemPage < itemTotalPages
}

private data class Screen5ItemPageRequest(
    val page: Int,
    val reset: Boolean,
    val search: String,
    val categoryId: String
)

internal class Screen5ItemManagementViewModel(
    private val repository: Screen5ItemManagementRepository
) : ViewModel() {
    var state by mutableStateOf(Screen5ItemManagementState())
        private set

    private var noticeJob: Job? = null
    private var errorJob: Job? = null
    private var itemSearchJob: Job? = null
    private var pendingItemRequest: Screen5ItemPageRequest? = null
    private var seenInventoryRevision = AdminDataFreshness.inventoryRevision

    fun ensureLoaded() {
        val stale = seenInventoryRevision != AdminDataFreshness.inventoryRevision
        if ((!state.loaded || stale) && !state.loading) load(keepContent = state.loaded)
    }

    fun refresh() {
        if (!state.loading && !state.busy) {
            itemSearchJob?.cancel()
            pendingItemRequest = null
            load(keepContent = true)
        }
    }

    fun searchItems(value: String) {
        val clean = value.take(120)
        if (clean == state.itemSearch && state.itemLoaded) return
        state = state.copy(itemSearch = clean, itemLoadError = null)
        itemSearchJob?.cancel()
        itemSearchJob = viewModelScope.launch {
            delay(300)
            requestItemPage(page = 1, reset = true)
        }
    }

    fun setItemCategory(categoryId: String) {
        val clean = categoryId.trim()
        if (clean == state.itemCategoryId && state.itemLoaded) return
        itemSearchJob?.cancel()
        state = state.copy(itemCategoryId = clean, itemLoadError = null)
        requestItemPage(page = 1, reset = true)
    }

    fun openItemCategory(categoryId: String) {
        val clean = categoryId.trim()
        if (clean == state.itemCategoryId && state.itemSearch.isBlank() && state.itemLoaded) return
        itemSearchJob?.cancel()
        state = state.copy(itemSearch = "", itemCategoryId = clean, itemLoadError = null)
        requestItemPage(page = 1, reset = true)
    }

    fun loadMoreItems() {
        if (!state.canLoadMoreItems) return
        requestItemPage(page = state.itemPage + 1, reset = false)
    }

    fun retryItems() {
        if (state.loading || state.itemLoading) return
        requestItemPage(page = 1, reset = true)
    }

    fun retryLoadMoreItems() {
        if (state.itemLoading || state.itemLoadMoreError.isNullOrBlank() || state.itemPage >= state.itemTotalPages) return
        state = state.copy(itemLoadMoreError = null)
        requestItemPage(page = state.itemPage + 1, reset = false)
    }

    fun clearActionFeedback() {
        noticeJob?.cancel()
        errorJob?.cancel()
        state = state.copy(notice = null, error = null)
    }

    private fun load(keepContent: Boolean) {
        if (state.loading) return
        val search = state.itemSearch
        val categoryId = state.itemCategoryId
        state = state.copy(loading = true, itemLoading = true, loadError = null, itemLoadError = null)
        viewModelScope.launch {
            try {
                val data = repository.bootstrap()
                val page = repository.items(1, search, categoryId)
                seenInventoryRevision = AdminDataFreshness.inventoryRevision
                state = state.copy(
                    loaded = true,
                    loading = false,
                    data = data,
                    itemItems = page.items,
                    itemPage = page.page,
                    itemTotalPages = page.totalPages,
                    itemTotal = page.total,
                    itemLoading = false,
                    itemLoaded = true,
                    loadError = null,
                    itemLoadError = null
                )
            } catch (error: Throwable) {
                val message = messageFor(error)
                state = if (keepContent && state.loaded) {
                    state.copy(loading = false, itemLoading = false, loadError = message)
                } else {
                    state.copy(loaded = false, loading = false, itemLoading = false, loadError = message)
                }
                scheduleErrorClear(message)
            }
            drainPendingItemRequest()
        }
    }

    private fun requestItemPage(page: Int, reset: Boolean) {
        val request = Screen5ItemPageRequest(
            page = page.coerceAtLeast(1),
            reset = reset,
            search = state.itemSearch,
            categoryId = state.itemCategoryId
        )
        if (state.loading || state.itemLoading) {
            pendingItemRequest = request
            return
        }
        loadItemPage(request)
    }

    private fun loadItemPage(request: Screen5ItemPageRequest) {
        if (state.loading || state.itemLoading) {
            pendingItemRequest = request
            return
        }
        state = state.copy(
            itemLoading = true,
            itemLoadError = if (request.reset) null else state.itemLoadError,
            itemLoadMoreError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                repository.items(request.page, request.search, request.categoryId)
            }
            val stillCurrent =
                state.itemSearch == request.search && state.itemCategoryId == request.categoryId
            result.onSuccess { page ->
                if (stillCurrent) {
                    val merged = if (request.reset || request.page == 1) {
                        page.items
                    } else {
                        (state.itemItems + page.items).distinctBy { it.id }
                    }
                    state = state.copy(
                        itemItems = merged,
                        itemPage = page.page,
                        itemTotalPages = page.totalPages,
                        itemTotal = page.total,
                        itemLoading = false,
                        itemLoaded = true,
                        itemLoadError = null,
                        itemLoadMoreError = null
                    )
                } else {
                    state = state.copy(itemLoading = false)
                }
            }.onFailure { error ->
                val message = messageFor(error)
                state = if (!request.reset && state.itemLoaded) {
                    state.copy(itemLoading = false, itemLoadMoreError = message)
                } else {
                    state.copy(itemLoading = false, itemLoadError = message, itemLoadMoreError = null)
                }
            }
            drainPendingItemRequest()
        }
    }

    private fun drainPendingItemRequest() {
        if (state.loading || state.itemLoading) return
        val next = pendingItemRequest ?: return
        pendingItemRequest = null
        loadItemPage(next)
    }

    private fun mutate(successMessage: String, onSuccess: () -> Unit = {}, operation: suspend () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null, notice = null, loadError = null, itemLoadError = null)
        viewModelScope.launch {
            try {
                operation()
            } catch (error: Throwable) {
                val message = messageFor(error)
                state = state.copy(busy = false, error = message)
                scheduleErrorClear(message)
                return@launch
            }

            AdminDataFreshness.markInventoryMutation()
            try {
                val refreshed = repository.bootstrap()
                val page = repository.items(1, state.itemSearch, state.itemCategoryId)
                seenInventoryRevision = AdminDataFreshness.inventoryRevision
                state = state.copy(
                    loaded = true,
                    loading = false,
                    busy = false,
                    data = refreshed,
                    itemItems = page.items,
                    itemPage = page.page,
                    itemTotalPages = page.totalPages,
                    itemTotal = page.total,
                    itemLoading = false,
                    itemLoaded = true,
                    notice = successMessage,
                    error = null,
                    loadError = null,
                    itemLoadError = null
                )
            } catch (_: Throwable) {
                state = state.copy(
                    busy = false,
                    notice = successMessage,
                    error = null,
                    loadError = "Changes were saved, but latest data could not be loaded. Refresh to sync."
                )
            }
            onSuccess()
            scheduleNoticeClear(successMessage)
        }
    }

    fun saveCategory(
        existingId: String?,
        name: String,
        codePrefix: String,
        displayOrder: Int,
        active: Boolean,
        publicVisible: Boolean
    ) = mutate(if (existingId == null) "Category added." else "Category updated.") {
        repository.saveCategory(existingId, name, codePrefix, displayOrder, active, publicVisible)
    }

    fun deleteCategory(id: String) = mutate("Category deleted.") {
        repository.deleteCategory(id)
    }

    fun saveField(
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
    ) = mutate(if (existingId == null) "Custom field added." else "Custom field updated.") {
        repository.saveField(
            categoryId,
            existingId,
            name,
            type,
            required,
            defaultValue,
            options,
            publicVisible,
            active,
            displayOrder
        )
    }

    fun deleteField(id: String) = mutate("Custom field deleted.") {
        repository.deleteField(id)
    }

    fun deactivateField(field: Screen5CategoryField) = mutate("Custom field deactivated.") {
        repository.fieldDeleteAction(field.id, "DEACTIVATE")
    }

    fun removeFieldDataAndDelete(id: String) = mutate("Custom field data removed and field deleted.") {
        repository.fieldDeleteAction(id, "REMOVE_DATA_AND_DELETE", "DELETE FIELD")
    }

    fun saveItem(
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
        cloudinaryAssets: List<Screen5UploadedAsset>,
        onSuccess: () -> Unit = {}
    ) = mutate(
        if (existing == null) "Item added." else "Item updated.",
        onSuccess = onSuccess
    ) {
        repository.saveItem(
            existing,
            itemCode,
            itemName,
            categoryId,
            totalQuantity,
            rentAmount,
            active,
            publicVisible,
            fieldValues,
            clearFieldIds,
            imageUrls,
            cloudinaryAssets
        )
    }

    fun deleteItem(id: String) = mutate("Item deleted.") {
        repository.deleteItem(id)
    }

    suspend fun uploadItemImage(uri: Uri): Screen5UploadedAsset =
        repository.uploadItemImage(uri)

    suspend fun discardItemUploads(assets: List<Screen5UploadedAsset>) =
        repository.discardItemUploads(assets)

    fun replaceRelatedItems(sourceItemId: String, relatedItemIds: List<String>) =
        mutate("Related items updated.") {
            repository.replaceRelatedItems(
                sourceItemId,
                relatedItemIds,
                state.data.items.firstOrNull { it.id == sourceItemId }?.updatedAt
            )
        }

    private fun messageFor(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    private fun scheduleNoticeClear(message: String) {
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(2600)
            if (state.notice == message) state = state.copy(notice = null)
        }
    }

    private fun scheduleErrorClear(message: String) {
        errorJob?.cancel()
        errorJob = viewModelScope.launch {
            delay(5100)
            if (state.error == message) state = state.copy(error = null)
        }
    }

    class Factory(private val repository: Screen5ItemManagementRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            Screen5ItemManagementViewModel(repository) as T
    }
}
