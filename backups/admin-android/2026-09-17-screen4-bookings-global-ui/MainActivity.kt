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
            apiConfigured = BuildConfig.API_BASE_URL.isNotBlank()
        )
        val bookingFactory = BookingLifecycleViewModel.Factory(repository)
        val customerFactory = CustomerScreen3ViewModel.Factory(CustomerScreen3Repository(apiClient))

        setContent {
            ZhagmagAdminTheme {
                val adminViewModel: AdminViewModel = viewModel(factory = adminFactory)
                val bookingViewModel: BookingLifecycleViewModel = viewModel(factory = bookingFactory)
                val customerViewModel: CustomerScreen3ViewModel = viewModel(factory = customerFactory)
                ZhagmagAdminRootScreen3(adminViewModel, bookingViewModel, customerViewModel)
            }
        }
    }
}
