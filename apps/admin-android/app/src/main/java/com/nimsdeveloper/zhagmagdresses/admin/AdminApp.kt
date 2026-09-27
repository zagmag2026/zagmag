package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingRow
import com.nimsdeveloper.zhagmagdresses.admin.data.Customer
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardEntry
import com.nimsdeveloper.zhagmagdresses.admin.data.DashboardPayload
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppPageHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.statusTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted

private enum class Destination { DASHBOARD, CUSTOMERS, BOOKINGS, MORE }

@Composable
fun AdminApp(viewModel: AdminViewModel) {
    when (val auth = viewModel.authState) {
        AuthState.Checking -> SessionCheckingScreen()
        is AuthState.SessionCheckFailed -> SessionCheckFailedScreen(
            branding = viewModel.branding,
            message = auth.message,
            onRetry = viewModel::retrySessionCheck
        )
        AuthState.SignedOut -> LoginScreen(
            busy = viewModel.loginBusy,
            error = viewModel.loginError,
            environmentError = viewModel.environmentError,
            onClearError = viewModel::clearLoginError,
            onLogin = viewModel::login
        )
        is AuthState.SignedIn -> AuthenticatedApp(auth.user, viewModel)
    }
}

@Composable
private fun SessionCheckingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppCard {
            Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(AppSpacing.xs))
            Text(stringResource(R.string.session_checking), color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.md))
            LoadingState()
        }
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    error: String?,
    environmentError: String?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(AppSpacing.lg), contentAlignment = Alignment.Center) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.brand_subtitle), color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.lg))
            Text(stringResource(R.string.sign_in), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.sign_in_subtitle), color = AppTextMuted)
            Spacer(Modifier.height(AppSpacing.md))

            AppTextField(
                value = identifier,
                onValueChange = { identifier = it; onClearError() },
                label = stringResource(R.string.email_or_mobile),
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(AppSpacing.sm))
            AppTextField(
                value = password,
                onValueChange = { password = it; onClearError() },
                label = stringResource(R.string.password),
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                supportingText = null
            )
            if (!environmentError.isNullOrBlank()) {
                Spacer(Modifier.height(AppSpacing.sm))
                InlineMessage(stringResource(R.string.environment_not_configured), error = true)
            }
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(AppSpacing.sm))
                InlineMessage(error, error = true)
            }
            Spacer(Modifier.height(AppSpacing.md))
            PrimaryButton(
                text = if (busy) stringResource(R.string.please_wait) else stringResource(R.string.sign_in),
                onClick = { onLogin(identifier, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = identifier.isNotBlank() && password.isNotBlank() && environmentError.isNullOrBlank(),
                loading = busy
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(user: SessionUser, viewModel: AdminViewModel) {
    var destination by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }

    LaunchedEffect(destination) {
        when (destination) {
            Destination.CUSTOMERS -> viewModel.ensureCustomers()
            Destination.BOOKINGS -> viewModel.ensureBookings()
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${user.name} · ${user.role.name}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                }
                Spacer(Modifier.width(AppSpacing.sm))
                SecondaryButton(stringResource(R.string.logout), onClick = viewModel::logout)
            }
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(
                    selected = destination == Destination.DASHBOARD,
                    onClick = { destination = Destination.DASHBOARD },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(R.string.dashboard)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.CUSTOMERS,
                    onClick = { destination = Destination.CUSTOMERS },
                    icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                    label = { Text(stringResource(R.string.customers)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.BOOKINGS,
                    onClick = { destination = Destination.BOOKINGS },
                    icon = { Icon(Icons.Rounded.EventNote, contentDescription = null) },
                    label = { Text(stringResource(R.string.bookings)) }
                )
                NavigationBarItem(
                    selected = destination == Destination.MORE,
                    onClick = { destination = Destination.MORE },
                    icon = { Icon(Icons.Rounded.Menu, contentDescription = null) },
                    label = { Text(stringResource(R.string.more)) }
                )
            }
        }
    ) { innerPadding ->
        when (destination) {
            Destination.DASHBOARD -> DashboardScreen(
                state = viewModel.dashboardState,
                onRefresh = viewModel::loadDashboard,
                modifier = Modifier.padding(innerPadding)
            )
            Destination.CUSTOMERS -> CustomerScreen(
                state = viewModel.customerState,
                onSearch = viewModel::searchCustomers,
                onPage = viewModel::customerPage,
                onRefresh = viewModel::refreshCustomers,
                modifier = Modifier.padding(innerPadding)
            )
            Destination.BOOKINGS -> BookingScreen(
                state = viewModel.bookingState,
                onSearch = viewModel::searchBookings,
                onPage = viewModel::bookingPage,
                onRefresh = viewModel::refreshBookings,
                modifier = Modifier.padding(innerPadding)
            )
            Destination.MORE -> MoreScreen(user.role, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun DashboardScreen(
    state: DataState<DashboardPayload>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categoriesLabel = stringResource(R.string.categories)
    val itemsLabel = stringResource(R.string.items)
    val totalQuantityLabel = stringResource(R.string.total_quantity)
    val availableNowLabel = stringResource(R.string.available_now)
    val bookedPendingLabel = stringResource(R.string.booked_pending)
    val missedPickupsLabel = stringResource(R.string.missed_pickups)
    val givenOutLabel = stringResource(R.string.given_out)
    val overdueReturnsLabel = stringResource(R.string.overdue_returns)
    val todayBookingsLabel = stringResource(R.string.today_bookings)
    val todayPickupsLabel = stringResource(R.string.today_pickups)
    val todayReturnsLabel = stringResource(R.string.today_returns)
    val noRecordsLabel = stringResource(R.string.no_records)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item {
            Spacer(Modifier.height(AppSpacing.xs))
            AppPageHeader(
                eyebrow = stringResource(R.string.today),
                title = stringResource(R.string.dashboard),
                subtitle = state.data?.today,
                action = { SecondaryButton(stringResource(R.string.refresh), onRefresh, enabled = !state.loading) }
            )
        }

        if (state.loading && state.data == null) item { LoadingState() }
        if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, error = true) }

        state.data?.let { dashboard ->
            val stats = listOf(
                categoriesLabel to dashboard.summary.totalCategories,
                itemsLabel to dashboard.summary.totalItems,
                totalQuantityLabel to dashboard.summary.totalQuantity,
                availableNowLabel to dashboard.summary.availableQuantity,
                bookedPendingLabel to dashboard.summary.bookedQuantity,
                missedPickupsLabel to dashboard.summary.missedPickups,
                givenOutLabel to dashboard.summary.givenQuantity,
                overdueReturnsLabel to dashboard.summary.overdueReturns
            )
            stats.chunked(2).forEach { rowStats ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        rowStats.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
                        if (rowStats.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            dashboardSection(missedPickupsLabel, dashboard.missedPickups, context, noRecordsLabel)
            dashboardSection(todayBookingsLabel, dashboard.todayBookings, context, noRecordsLabel)
            dashboardSection(todayPickupsLabel, dashboard.todayPickups, context, noRecordsLabel)
            dashboardSection(todayReturnsLabel, dashboard.todayReturns, context, noRecordsLabel)
            dashboardSection(overdueReturnsLabel, dashboard.overdueReturns, context, noRecordsLabel)
        }

        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardSection(
    title: String,
    rows: List<DashboardEntry>,
    context: Context,
    emptyText: String
) {
    item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            StatusBadge(rows.size.toString())
        }
    }
    if (rows.isEmpty()) {
        item { AppCard { EmptyState(emptyText) } }
    } else {
        items(rows, key = { "dashboard-${title}-${it.id}" }) { row ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.customerName, style = MaterialTheme.typography.titleMedium)
                        Text(row.customerMobile, color = AppTextMuted)
                    }
                    StatusBadge(row.status.replace('_', ' '), statusTone(row.status))
                }
                Spacer(Modifier.height(AppSpacing.xs))
                Text(row.itemsSummary.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(AppSpacing.xs))
                Text("${row.bookingNo} · ${row.pickupDate} → ${row.returnDate}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                Spacer(Modifier.height(AppSpacing.sm))
                ContactActions(context, row.customerMobile)
            }
        }
    }
}

@Composable
private fun CustomerScreen(
    state: PagedState<Customer>,
    onSearch: (String) -> Unit,
    onPage: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item {
            Spacer(Modifier.height(AppSpacing.xs))
            AppPageHeader(
                eyebrow = stringResource(R.string.customer_master),
                title = stringResource(R.string.customers),
                subtitle = stringResource(R.string.customer_hint),
                action = { SecondaryButton(stringResource(R.string.refresh), onRefresh, enabled = !state.loading) }
            )
        }
        item {
            AppCard {
                AppTextField(
                    value = query,
                    onValueChange = { value ->
                        query = value
                        val normalized = value.trim()
                        when {
                            normalized.isEmpty() || normalized.length >= 2 -> onSearch(normalized)
                            state.search.isNotEmpty() -> onSearch("")
                        }
                    },
                    label = stringResource(R.string.search_customer),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    supportingText = stringResource(R.string.auto_search_hint)
                )
            }
        }
        item { Text(stringResource(R.string.records_count, state.total), style = MaterialTheme.typography.labelLarge) }
        if (state.loading && !state.loaded) item { LoadingState() }
        if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, error = true) }
        if (state.loaded && state.items.isEmpty()) item { AppCard { EmptyState(stringResource(R.string.no_records)) } }

        items(state.items, key = { it.id }) { customer ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        Text(customer.mobile, color = AppTextMuted)
                    }
                    StatusBadge(if (customer.isActive) "ACTIVE" else "INACTIVE", if (customer.isActive) com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone.SUCCESS else com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone.NEUTRAL)
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    MiniMetric(stringResource(R.string.total_bookings), customer.totalBookings, Modifier.weight(1f))
                    MiniMetric(stringResource(R.string.active_bookings), customer.activeBookings, Modifier.weight(1f))
                }
                if (!customer.lastBookingDate.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    Text("${stringResource(R.string.last_booking)}: ${customer.lastBookingDate}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                }
                Spacer(Modifier.height(AppSpacing.sm))
                ContactActions(context, customer.mobile)
            }
        }

        if (state.loaded) item {
            Pager(state.page, state.totalPages, state.loading, onPage)
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }
}

@Composable
private fun BookingScreen(
    state: PagedState<BookingRow>,
    onSearch: (String) -> Unit,
    onPage: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(state.search) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item {
            Spacer(Modifier.height(AppSpacing.xs))
            AppPageHeader(
                eyebrow = stringResource(R.string.booking_module),
                title = stringResource(R.string.bookings),
                subtitle = stringResource(R.string.booking_hint),
                action = { SecondaryButton(stringResource(R.string.refresh), onRefresh, enabled = !state.loading) }
            )
        }
        item {
            AppCard {
                Text(stringResource(R.string.booking_pickup_flow), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(AppSpacing.xs))
                Text(stringResource(R.string.booking_pickup_flow_hint), color = AppTextMuted)
            }
        }
        item {
            AppCard {
                AppTextField(
                    value = query,
                    onValueChange = { value ->
                        query = value
                        val normalized = value.trim()
                        when {
                            normalized.isEmpty() || normalized.length >= 2 -> onSearch(normalized)
                            state.search.isNotEmpty() -> onSearch("")
                        }
                    },
                    label = stringResource(R.string.search_booking),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    supportingText = stringResource(R.string.auto_search_hint)
                )
            }
        }
        item { Text(stringResource(R.string.records_count, state.total), style = MaterialTheme.typography.labelLarge) }
        if (state.loading && !state.loaded) item { LoadingState() }
        if (!state.error.isNullOrBlank()) item { InlineMessage(state.error, error = true) }
        if (state.loaded && state.items.isEmpty()) item { AppCard { EmptyState(stringResource(R.string.no_records)) } }

        items(state.items, key = { it.id }) { booking ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(booking.customerName, style = MaterialTheme.typography.titleMedium)
                        Text(booking.bookingNo, color = AppTextMuted)
                    }
                    StatusBadge(booking.status.replace('_', ' '), statusTone(booking.status))
                }
                Spacer(Modifier.height(AppSpacing.xs))
                Text(booking.itemsSummary.ifBlank { "—" })
                Spacer(Modifier.height(AppSpacing.xs))
                Text("${booking.pickupDate} → ${booking.returnDate} · Qty ${booking.bookedQty}", style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
                Spacer(Modifier.height(AppSpacing.sm))
                ContactActions(context, booking.customerMobile)
            }
        }
        if (state.loaded) item { Pager(state.page, state.totalPages, state.loading, onPage) }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }
}

@Composable
private fun MoreScreen(role: UserRole, modifier: Modifier = Modifier) {
    val baseModules = listOf(
        stringResource(R.string.items),
        stringResource(R.string.pickup),
        stringResource(R.string.returns),
        stringResource(R.string.reports)
    )
    val adminModules = if (role == UserRole.STAFF) emptyList() else listOf(
        stringResource(R.string.categories),
        stringResource(R.string.users),
        stringResource(R.string.settings),
        stringResource(R.string.audit_logs)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        item {
            Spacer(Modifier.height(AppSpacing.xs))
            AppPageHeader(
                eyebrow = stringResource(R.string.module_foundation),
                title = stringResource(R.string.more),
                subtitle = stringResource(R.string.coming_next_batch)
            )
        }
        items(baseModules + adminModules) { name ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(stringResource(R.string.coming_next_batch))
                }
            }
        }
        item { Spacer(Modifier.height(AppSpacing.lg)) }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
        Spacer(Modifier.height(AppSpacing.xxs))
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTextMuted)
        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ContactActions(context: Context, mobile: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        SecondaryButton(
            text = stringResource(R.string.call),
            onClick = { context.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$mobile"))) },
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = stringResource(R.string.whatsapp),
            onClick = {
                val number = if (mobile.length == 10) "91$mobile" else mobile
                context.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Pager(page: Int, totalPages: Int, loading: Boolean, onPage: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryButton(stringResource(R.string.previous), onClick = { onPage(page - 1) }, enabled = !loading && page > 1)
        Spacer(Modifier.width(AppSpacing.sm))
        Text(stringResource(R.string.page_of, page, totalPages), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(AppSpacing.sm))
        SecondaryButton(stringResource(R.string.next), onClick = { onPage(page + 1) }, enabled = !loading && page < totalPages)
    }
}

private fun Context.safeStart(intent: Intent) {
    runCatching { startActivity(intent) }
}
