package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimsdeveloper.zhagmagdresses.admin.data.AdminRepository
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiException
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiSessionEvents
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingRow
import com.nimsdeveloper.zhagmagdresses.admin.data.BrandingStore
import com.nimsdeveloper.zhagmagdresses.admin.data.Customer
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardPayload
import com.nimsdeveloper.zhagmagdresses.admin.data.PageResult
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.StaffAccess
import com.nimsdeveloper.zhagmagdresses.admin.data.hasAccess
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class SessionCheckFailed(val message: String) : AuthState
    data class SignedIn(val user: SessionUser) : AuthState
}

data class DataState<T>(
    val data: T? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false
)

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val search: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false
)

class AdminViewModel(
    private val repository: AdminRepository,
    private val sessionStore: SecureSessionStore,
    private val brandingStore: BrandingStore,
    private val apiConfigured: Boolean,
    private val appContext: Context
) : ViewModel() {

    var authState: AuthState by mutableStateOf(AuthState.Checking)
        private set
    var branding: AppBranding by mutableStateOf(brandingStore.read())
        private set
    var loginBusy by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    var environmentError by mutableStateOf<String?>(null)
        private set

    var dashboardState by mutableStateOf(DataState<DashboardPayload>())
        private set
    var dashboardRevision by mutableStateOf(0)
        private set
    var customerState by mutableStateOf(PagedState<Customer>())
        private set
    var bookingState by mutableStateOf(PagedState<BookingRow>())
        private set

    private var pendingCustomerSearch: String? = null
    private var pendingBookingSearch: String? = null
    private var seenDashboardFreshnessRevision = AdminDataFreshness.dashboardRevision

    init {
        ApiSessionEvents.registerSessionExpiredHandler {
            viewModelScope.launch {
                if (authState is AuthState.SignedIn) expireSessionLocally()
            }
        }
        if (apiConfigured) checkSession() else {
            environmentError = "Service is not configured."
            authState = AuthState.SignedOut
        }
    }

    fun clearLoginError() {
        loginError = null
    }

    fun login(mobile: String, password: String) {
        if (loginBusy) return
        val normalizedMobile = mobile.filter(Char::isDigit)
        if (normalizedMobile.length != 10) {
            loginError = "Enter a valid 10-digit mobile number."
            return
        }
        if (password.length < 8) {
            loginError = "Password must contain at least 8 characters."
            return
        }
        if (!apiConfigured) {
            loginError = "Unable to sign in."
            return
        }

        viewModelScope.launch {
            loginBusy = true
            loginError = null
            runCatching { repository.loginWithBranding(normalizedMobile, password) }
                .onSuccess { session ->
                    applyBranding(session.branding)
                    pendingCustomerSearch = null
                    pendingBookingSearch = null
                    authState = AuthState.SignedIn(session.user)
                    AdminDataFreshness.markSessionBoundary()
                    dashboardState = DataState()
                    dashboardRevision = 0
                    customerState = PagedState()
                    bookingState = PagedState()
                    loadDashboard()
                }
                .onFailure { loginError = loginUserMessage(it) }
            loginBusy = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
            } finally {
                sessionStore.clear()
                Screen8PdfExporter.purgeCache(appContext)
                pendingCustomerSearch = null
                pendingBookingSearch = null
                authState = AuthState.SignedOut
                dashboardState = DataState()
                dashboardRevision = 0
                customerState = PagedState()
                bookingState = PagedState()
                loginError = null
            }
        }
    }

    fun ensureDashboard() {
        if ((!dashboardState.loaded || seenDashboardFreshnessRevision != AdminDataFreshness.dashboardRevision) && !dashboardState.loading) {
            loadDashboard()
        }
    }

    fun loadDashboard() {
        if (dashboardState.loading) return
        dashboardState = dashboardState.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.dashboard() }
                .onSuccess {
                    dashboardState = DataState(data = it, loaded = true)
                    seenDashboardFreshnessRevision = AdminDataFreshness.dashboardRevision
                    dashboardRevision += 1
                }
                .onFailure { handleFailure(it) { message -> dashboardState = dashboardState.copy(loading = false, error = message) } }
        }
    }

    fun ensureCustomers() {
        if (!customerState.loaded && !customerState.loading) loadCustomers(1, customerState.search)
    }

    fun searchCustomers(search: String) {
        val normalized = search.trim()
        if (customerState.loading) {
            pendingCustomerSearch = normalized
            return
        }
        if (customerState.loaded && customerState.search == normalized) return
        loadCustomers(1, normalized)
    }

    fun customerPage(page: Int) = loadCustomers(page, customerState.search)

    fun refreshCustomers() = loadCustomers(customerState.page, customerState.search)

    fun ensureBookings() {
        if (!bookingState.loaded && !bookingState.loading) loadBookings(1, bookingState.search)
    }

    fun searchBookings(search: String) {
        val normalized = search.trim()
        if (bookingState.loading) {
            pendingBookingSearch = normalized
            return
        }
        if (bookingState.loaded && bookingState.search == normalized) return
        loadBookings(1, normalized)
    }

    fun bookingPage(page: Int) = loadBookings(page, bookingState.search)

    fun refreshBookings() = loadBookings(bookingState.page, bookingState.search)

    fun retrySessionCheck() = checkSession()

    private fun checkSession() {
        viewModelScope.launch {
            authState = AuthState.Checking
            runCatching { repository.currentSession() }
                .onSuccess { session ->
                    applyBranding(session.branding)
                    pendingCustomerSearch = null
                    pendingBookingSearch = null
                    authState = AuthState.SignedIn(session.user)
                    AdminDataFreshness.markSessionBoundary()
                    if (session.user.hasAccess(StaffAccess.DASHBOARD)) loadDashboard()
                }
                .onFailure { error ->
                    if (error is ApiException && error.statusCode == 401) {
                        sessionStore.clear()
                        authState = AuthState.SignedOut
                        refreshPublicBranding()
                    } else {
                        authState = AuthState.SessionCheckFailed(
                            when (error) {
                                is ApiException -> error.message
                                else -> "Unable to verify your session. Check your connection and try again."
                            }
                        )
                    }
                }
        }
    }

    fun applySettingsBranding(shopName: String, logoUrl: String) {
        applyBranding(
            AppBranding(
                shopName = shopName.trim().ifBlank { AppBranding.DEFAULT_SHOP_NAME },
                logoUrl = logoUrl.trim().takeIf { it.isNotBlank() }
            )
        )
    }

    fun refreshPublicBranding() {
        if (!apiConfigured) return
        viewModelScope.launch {
            runCatching { repository.publicBranding() }
                .onSuccess(::applyBranding)
        }
    }

    private fun applyBranding(value: AppBranding) {
        branding = value
        brandingStore.save(value)
    }

    private fun loadCustomers(page: Int, search: String) {
        if (customerState.loading) return
        val normalized = search.trim()
        viewModelScope.launch {
            customerState = customerState.copy(loading = true, error = null, search = normalized)
            runCatching { repository.customers(page, normalized) }
                .onSuccess { result -> customerState = result.toPagedState(normalized) }
                .onFailure { handleFailure(it) { message -> customerState = customerState.copy(loading = false, error = message) } }

            val next = pendingCustomerSearch
            pendingCustomerSearch = null
            if (next != null && next != customerState.search && authState is AuthState.SignedIn) {
                loadCustomers(1, next)
            }
        }
    }

    private fun loadBookings(page: Int, search: String) {
        if (bookingState.loading) return
        val normalized = search.trim()
        viewModelScope.launch {
            bookingState = bookingState.copy(loading = true, error = null, search = normalized)
            runCatching { repository.bookings(page, normalized) }
                .onSuccess { result -> bookingState = result.toPagedState(normalized) }
                .onFailure { handleFailure(it) { message -> bookingState = bookingState.copy(loading = false, error = message) } }

            val next = pendingBookingSearch
            pendingBookingSearch = null
            if (next != null && next != bookingState.search && authState is AuthState.SignedIn) {
                loadBookings(1, next)
            }
        }
    }

    private fun expireSessionLocally() {
        sessionStore.clear()
        Screen8PdfExporter.purgeCache(appContext)
        pendingCustomerSearch = null
        pendingBookingSearch = null
        authState = AuthState.SignedOut
        dashboardState = DataState()
        dashboardRevision = 0
        customerState = PagedState()
        bookingState = PagedState()
        loginError = null
    }

    private fun handleFailure(error: Throwable, setError: (String) -> Unit) {
        if (error is ApiException && error.statusCode == 401) {
            expireSessionLocally()
            return
        }
        setError(userMessage(error))
    }

    override fun onCleared() {
        ApiSessionEvents.clearSessionExpiredHandler()
        super.onCleared()
    }

    private fun loginUserMessage(error: Throwable): String = when (error) {
        is ApiException -> when {
            error.statusCode == 0 -> "Unable to connect. Check your internet connection and try again."
            error.statusCode == 401 || error.statusCode == 403 -> "Invalid mobile number or password."
            error.statusCode == 408 -> "Request timed out. Try again."
            error.statusCode == 429 -> "Too many attempts. Try again later."
            error.statusCode >= 500 -> "Server is unavailable. Try again."
            else -> "Unable to sign in. Try again."
        }
        else -> "Unable to connect. Check your internet connection and try again."
    }

    private fun userMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: "Invalid configuration."
        else -> "Something went wrong. Please try again."
    }

    private fun <T> PageResult<T>.toPagedState(search: String) = PagedState(
        items = items,
        page = page,
        totalPages = totalPages,
        total = total,
        search = search,
        loaded = true
    )

    class Factory(
        private val repository: AdminRepository,
        private val sessionStore: SecureSessionStore,
        private val brandingStore: BrandingStore,
        private val apiConfigured: Boolean,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AdminViewModel(repository, sessionStore, brandingStore, apiConfigured, appContext) as T
    }
}
