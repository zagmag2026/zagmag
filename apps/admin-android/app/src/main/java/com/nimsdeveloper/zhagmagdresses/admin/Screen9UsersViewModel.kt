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
import com.nimsdeveloper.zhagmagdresses.admin.data.StaffAccess
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class Screen9UsersState(
    val items: List<Screen9ManagedUser> = emptyList(),
    val permissionOptions: List<String> = StaffAccess.operationalDefaults,
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val search: String = "",
    val roleFilter: String = "ALL",
    val statusFilter: String = "active",
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val actionBusy: Boolean = false,
    val loadError: String? = null,
    val loadMoreError: String? = null,
    val error: String? = null,
    val message: String? = null
) {
    val canLoadMore: Boolean get() = loaded && !loading && loadError == null && loadMoreError == null && page < totalPages
}

internal class Screen9UsersViewModel(
    private val repository: Screen9UsersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var state by mutableStateOf(
        Screen9UsersState(
            page = savedStateHandle["usersPage"] ?: 1,
            search = savedStateHandle["usersSearch"] ?: "",
            roleFilter = savedStateHandle["usersRole"] ?: "ALL",
            statusFilter = savedStateHandle["usersStatus"] ?: "active"
        )
    )
        private set

    private var searchJob: Job? = null
    private var pendingReset = false
    private var seenUsersRevision = AdminDataFreshness.usersRevision

    fun ensureLoaded() {
        val stale = seenUsersRevision != AdminDataFreshness.usersRevision
        if ((!state.loaded || stale) && !state.loading) load(reset = true, restorePage = true)
    }

    fun refresh() {
        searchJob?.cancel()
        state = state.copy(loadError = null, error = null, message = null)
        requestReset()
    }

    fun search(value: String) {
        val normalized = value.take(120)
        if (normalized == state.search && state.loaded) return
        savedStateHandle["usersSearch"] = normalized
        savedStateHandle["usersPage"] = 1
        state = state.copy(search = normalized, loadError = null, error = null, message = null)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            requestReset()
        }
    }

    fun setRoleFilter(value: String) {
        val normalized = value.uppercase()
        if (normalized == state.roleFilter) return
        savedStateHandle["usersRole"] = normalized
        savedStateHandle["usersPage"] = 1
        state = state.copy(roleFilter = normalized, loadError = null, error = null, message = null)
        searchJob?.cancel()
        requestReset()
    }

    fun setStatusFilter(value: String) {
        val normalized = value.lowercase()
        if (normalized == state.statusFilter) return
        savedStateHandle["usersStatus"] = normalized
        savedStateHandle["usersPage"] = 1
        state = state.copy(statusFilter = normalized, loadError = null, error = null, message = null)
        searchJob?.cancel()
        requestReset()
    }

    private fun requestReset() {
        if (state.loading) {
            pendingReset = true
            return
        }
        load(reset = true)
    }

    fun loadMore() {
        if (!state.canLoadMore) return
        load(reset = false)
    }

    fun retryLoadMore() {
        if (state.loading || state.loadMoreError.isNullOrBlank() || state.page >= state.totalPages) return
        state = state.copy(loadMoreError = null)
        load(reset = false)
    }

    fun create(
        name: String,
        mobile: String,
        role: UserRole,
        password: String,
        staffPermissions: Set<String>,
        onSuccess: () -> Unit
    ) = mutate("User created.", onSuccess) {
        repository.create(name, mobile, role, password, staffPermissions)
    }

    fun update(
        id: String,
        name: String,
        mobile: String,
        role: UserRole,
        active: Boolean,
        staffPermissions: Set<String>,
        onSuccess: () -> Unit
    ) = mutate("User updated.", onSuccess) {
        repository.update(
            id,
            name,
            mobile,
            role,
            active,
            staffPermissions,
            state.items.firstOrNull { it.id == id }?.updatedAt
        )
    }

    fun resetPassword(id: String, password: String, onSuccess: () -> Unit) {
        if (state.actionBusy) return
        state = state.copy(actionBusy = true, loadError = null, error = null, message = null)
        viewModelScope.launch {
            runCatching { repository.resetPassword(id, password) }
                .onSuccess {
                    AdminDataFreshness.markPasswordMutation()
                    state = state.copy(actionBusy = false, message = "Password reset.")
                    onSuccess()
                }
                .onFailure { error ->
                    state = state.copy(actionBusy = false, error = userMessage(error))
                }
        }
    }

    fun archive(id: String) =
        mutate("User archived.", {}) { repository.archive(id) }

    fun restore(id: String) =
        mutate("User restored.", {}) { repository.restore(id) }

    fun clearFeedback() {
        state = state.copy(loadError = null, error = null, message = null)
    }

    private fun load(reset: Boolean, restorePage: Boolean = false) {
        if (state.loading) return
        val targetPage = if (reset) {
            if (restorePage) state.page.coerceAtLeast(1) else 1
        } else state.page + 1
        viewModelScope.launch {
            state = state.copy(
                loading = true,
                loadError = if (reset) null else state.loadError,
                loadMoreError = null
            )
            runCatching {
                repository.users(
                    page = targetPage,
                    search = state.search,
                    role = state.roleFilter,
                    status = state.statusFilter
                )
            }.onSuccess { result ->
                val merged = if (reset) result.users else {
                    (state.items + result.users).distinctBy { it.id }
                }
                seenUsersRevision = AdminDataFreshness.usersRevision
                savedStateHandle["usersPage"] = result.page
                state = state.copy(
                    items = merged,
                    permissionOptions = result.permissionOptions.ifEmpty { StaffAccess.operationalDefaults },
                    page = result.page,
                    totalPages = result.totalPages,
                    total = result.total,
                    loading = false,
                    loaded = true,
                    loadError = null,
                    loadMoreError = null
                )
            }.onFailure { error ->
                val message = userMessage(error)
                state = if (!reset && state.loaded) {
                    state.copy(loading = false, loadMoreError = message)
                } else {
                    state.copy(loading = false, loaded = state.loaded, loadError = message, loadMoreError = null)
                }
            }

            if (pendingReset && !state.loading) {
                pendingReset = false
                load(reset = true)
            }
        }
    }

    private fun mutate(
        successMessage: String,
        onSuccess: () -> Unit,
        block: suspend () -> Unit
    ) {
        if (state.actionBusy) return
        state = state.copy(actionBusy = true, loadError = null, error = null, message = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    AdminDataFreshness.markUserMutation()
                    state = state.copy(actionBusy = false, message = successMessage)
                    onSuccess()
                    load(reset = true)
                }
                .onFailure { error ->
                    state = state.copy(actionBusy = false, error = userMessage(error))
                }
        }
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid value."
        else -> "Something went wrong. Please try again."
    }

    class Factory(private val repository: Screen9UsersRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            Screen9UsersViewModel(repository, extras.createSavedStateHandle()) as T
    }
}
