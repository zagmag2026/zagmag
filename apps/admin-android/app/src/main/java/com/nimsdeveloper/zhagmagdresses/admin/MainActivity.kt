package com.nimsdeveloper.zhagmagdresses.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimsdeveloper.zhagmagdresses.admin.data.AdminRepository
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.BrandingStore
import com.nimsdeveloper.zhagmagdresses.admin.data.CustomerScreen3Repository
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.ZhagmagAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionStore = SecureSessionStore(applicationContext)
        val brandingStore = BrandingStore(applicationContext)
        val apiClient = ApiClient(BuildConfig.API_BASE_URL, sessionStore)
        val repository = AdminRepository(apiClient)
        val adminFactory = AdminViewModel.Factory(
            repository = repository,
            sessionStore = sessionStore,
            brandingStore = brandingStore,
            apiConfigured = BuildConfig.API_BASE_URL.isNotBlank(),
            appContext = applicationContext
        )
        val bookingFactory = BookingLifecycleViewModel.Factory(repository)
        val bookingListFactory = BookingListViewModel4.Factory(repository)
        val customerFactory = CustomerScreen3ViewModel.Factory(CustomerScreen3Repository(apiClient))
        val reportsFactory = Screen8ReportsViewModel.Factory(Screen8ReportsRepository(apiClient))

        setContent {
            ZhagmagAdminTheme {
                val adminViewModel: AdminViewModel = viewModel(factory = adminFactory)
                val bookingViewModel: BookingLifecycleViewModel = viewModel(factory = bookingFactory)
                val bookingListViewModel: BookingListViewModel4 = viewModel(factory = bookingListFactory)
                val customerViewModel: CustomerScreen3ViewModel = viewModel(factory = customerFactory)
                val reportsViewModel: Screen8ReportsViewModel = viewModel(factory = reportsFactory)
                ZhagmagAdminRootScreen4(
                    adminViewModel = adminViewModel,
                    bookingViewModel = bookingViewModel,
                    bookingListViewModel = bookingListViewModel,
                    customerViewModel = customerViewModel,
                    reportsViewModel = reportsViewModel
                )
            }
        }
    }
}
