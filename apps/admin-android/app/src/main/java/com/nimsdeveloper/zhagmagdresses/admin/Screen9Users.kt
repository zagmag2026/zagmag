package com.nimsdeveloper.zhagmagdresses.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimsdeveloper.zhagmagdresses.admin.data.ApiClient
import com.nimsdeveloper.zhagmagdresses.admin.data.SecureSessionStore
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.data.StaffAccess
import com.nimsdeveloper.zhagmagdresses.admin.data.UserRole
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.DangerTextButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppBackHeader
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppDestructiveConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFeedbackHost
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFormSheetScaffold
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppFilterOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppSingleSelectFilter
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppUnsavedChangesDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactSearchField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.EmptyState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadingState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.LoadFailureState
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PopupMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineRetryMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted

private val screen9PermissionLabels = linkedMapOf(
    StaffAccess.DASHBOARD to "Dashboard",
    StaffAccess.ITEMS to "Items / Stock",
    StaffAccess.CUSTOMERS to "Customers",
    StaffAccess.BOOKINGS to "Bookings",
    StaffAccess.PICKUPS to "Pickup",
    StaffAccess.RETURNS to "Returns",
    StaffAccess.REPORTS to "Reports"
)

private val Screen9RoleSaver = Saver<UserRole, String>(
    save = { it.name },
    restore = { runCatching { UserRole.valueOf(it) }.getOrDefault(UserRole.STAFF) }
)
private val Screen9PermissionSetSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Screen9Users(
    currentUser: SessionUser,
    businessDate: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) {
        Screen9UsersRepository(ApiClient(BuildConfig.API_BASE_URL, SecureSessionStore(appContext)))
    }
    val factory = remember(repository) { Screen9UsersViewModel.Factory(repository) }
    val vm: Screen9UsersViewModel = viewModel(factory = factory)
    val state = vm.state

    var editor by remember { mutableStateOf<Screen9ManagedUser?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var passwordTarget by remember { mutableStateOf<Screen9ManagedUser?>(null) }
    var archiveTarget by remember { mutableStateOf<Screen9ManagedUser?>(null) }

    LaunchedEffect(Unit) {
        vm.clearFeedback()
        vm.ensureLoaded()
    }

    BackHandler {
        if (!state.actionBusy) onBack()
    }

    PullToRefreshBox(
        isRefreshing = state.loading && state.loaded,
        onRefresh = vm::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = AppSpacing.md,
                end = AppSpacing.md,
                top = AppSpacing.xs,
                bottom = AppSpacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            item { MainScreenDateRow(businessDate) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppBackHeader(
                        title = "Users",
                        onBack = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !state.actionBusy
                    )
                    CompactNewActionButton(
                        label = "Add User",
                        enabled = !state.actionBusy,
                        onClick = {
                            editor = null
                            showEditor = true
                            vm.clearFeedback()
                        }
                    )
                }
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

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    CompactSearchField(
                        value = state.search,
                        onValueChange = vm::search,
                        placeholder = "Search name or mobile",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Screen9FilterButton(
                            label = "Role",
                            options = listOf("ALL" to "All Roles", "OWNER" to "Owner", "STAFF" to "Staff"),
                            selected = state.roleFilter,
                            resetValue = "ALL",
                            modifier = Modifier.weight(1f),
                            onSelected = vm::setRoleFilter
                        )
                        Screen9FilterButton(
                            label = "Status",
                            options = listOf(
                                "active" to "Active",
                                "inactive" to "Inactive",
                                "archived" to "Archived",
                                "all" to "All Status"
                            ),
                            selected = state.statusFilter,
                            resetValue = "active",
                            modifier = Modifier.weight(1f),
                            onSelected = vm::setStatusFilter
                        )
                    }
                }
            }

            if (state.loaded) {
                item {
                    Text(
                        "${state.total} users",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTextMuted
                    )
                }
            }

            if (state.loading && !state.loaded) {
                item { LoadingState() }
            } else if (state.loaded && state.items.isEmpty() && state.loadError.isNullOrBlank()) {
                item { EmptyState("No users found.") }
            } else {
                items(state.items.size, key = { index -> state.items[index].id }) { index ->
                    val user = state.items[index]
                    Screen9UserCard(
                        user = user,
                        isCurrentUser = user.id == currentUser.id,
                        busy = state.actionBusy,
                        onEdit = {
                            editor = user
                            showEditor = true
                            vm.clearFeedback()
                        },
                        onResetPassword = {
                            passwordTarget = user
                            vm.clearFeedback()
                        },
                        onArchiveRestore = {
                            if (user.archivedAt == null) archiveTarget = user else vm.restore(user.id)
                        }
                    )
                }
            }

            if (state.canLoadMore) {
                item(key = "screen9-load-more") {
                    LaunchedEffect(state.page, state.items.size) { vm.loadMore() }
                    LoadingState()
                }
            } else if (state.loading && state.loaded) {
                item { LoadingState() }
            }
            if (!state.loadMoreError.isNullOrBlank()) {
                item(key = "screen9-load-more-retry-${state.page}") {
                    InlineRetryMessage(
                        message = state.loadMoreError.orEmpty(),
                        onRetry = vm::retryLoadMore,
                        retryLabel = "Retry loading more"
                    )
                }
            }
        }
    }

    AppFeedbackHost(
        successMessage = state.message,
        errorMessage = state.error
    )

    if (showEditor) {
        Screen9UserEditorSheet(
            existing = editor,
            currentUserId = currentUser.id,
            permissionOptions = state.permissionOptions.ifEmpty { StaffAccess.operationalDefaults },
            busy = state.actionBusy,
            onDismiss = { showEditor = false },
            onSave = { name, mobile, role, active, password, permissions ->
                if (editor == null) {
                    vm.create(name, mobile, role, password, permissions) {
                        showEditor = false
                    }
                } else {
                    vm.update(editor!!.id, name, mobile, role, active, permissions) {
                        showEditor = false
                    }
                }
            }
        )
    }

    passwordTarget?.let { target ->
        Screen9PasswordSheet(
            target = target,
            busy = state.actionBusy,
            onDismiss = { passwordTarget = null },
            onSave = { password ->
                vm.resetPassword(target.id, password) {
                    passwordTarget = null
                }
            }
        )
    }

    archiveTarget?.let { target ->
        AppDestructiveConfirmDialog(
            title = "Archive user?",
            message = "Archive ${target.name}? Login will be disabled and history will be preserved.",
            confirmLabel = "Archive",
            busy = state.actionBusy,
            onConfirm = {
                archiveTarget = null
                vm.archive(target.id)
            },
            onDismiss = { archiveTarget = null }
        )
    }
}

@Composable
private fun Screen9FilterButton(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    resetValue: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    AppSingleSelectFilter(
        label = label,
        selected = selected,
        options = options.map { (value, text) -> AppFilterOption(value, text) },
        resetValue = resetValue,
        onApply = onSelected,
        modifier = modifier,
        title = "Filter Users · $label"
    )
}

@Composable
private fun Screen9UserCard(
    user: Screen9ManagedUser,
    isCurrentUser: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onArchiveRestore: () -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Text(
                if (isCurrentUser) "${user.name} · You" else user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(user.mobile, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
            ResponsiveCompactPair(
                minSideBySideWidth = 240.dp,
                first = { child ->
                    StatusBadge(
                        text = if (user.role == UserRole.OWNER) "Owner" else "Staff",
                        tone = if (user.role == UserRole.OWNER) BadgeTone.INFO else BadgeTone.SUCCESS,
                        modifier = child
                    )
                },
                second = { child ->
                    StatusBadge(
                        text = when {
                            user.archivedAt != null -> "Archived"
                            user.active -> "Active"
                            else -> "Inactive"
                        },
                        tone = when {
                            user.archivedAt != null -> BadgeTone.ERROR
                            user.active -> BadgeTone.SUCCESS
                            else -> BadgeTone.WARNING
                        },
                        modifier = child
                    )
                }
            )
        }

        Spacer(Modifier.size(4.dp))
        Text(
            "Bookings ${user.bookingsCreated}  |  Pickups ${user.pickupsHandled}  |  Returns ${user.returnsHandled}",
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
            maxLines = 1
        )
        Text(
            "Last login · ${user.lastLoginAt ?: "Never"}",
            style = MaterialTheme.typography.labelSmall,
            color = AppTextMuted,
            maxLines = 1
        )
        if (user.role == UserRole.STAFF && user.staffPermissions.isNotEmpty()) {
            Text(
                user.staffPermissions.mapNotNull(screen9PermissionLabels::get).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = AppTextMuted,
                maxLines = 2
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            SecondaryButton(
                text = "Edit",
                onClick = onEdit,
                enabled = !busy && user.archivedAt == null,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Reset Password",
                onClick = onResetPassword,
                enabled = !busy && user.archivedAt == null,
                modifier = Modifier.weight(1f)
            )
        }
        if (!isCurrentUser) {
            Spacer(Modifier.size(AppSpacing.xs))
            SecondaryButton(
                text = if (user.archivedAt == null) "Archive" else "Restore",
                onClick = onArchiveRestore,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen9UserEditorSheet(
    existing: Screen9ManagedUser?,
    currentUserId: String,
    permissionOptions: List<String>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        mobile: String,
        role: UserRole,
        active: Boolean,
        password: String,
        permissions: Set<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var mobile by rememberSaveable(existing?.id) { mutableStateOf(existing?.mobile.orEmpty()) }
    var nameTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var mobileTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var role by rememberSaveable(existing?.id, stateSaver = Screen9RoleSaver) { mutableStateOf(existing?.role ?: UserRole.STAFF) }
    var active by rememberSaveable(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var password by remember(existing?.id) { mutableStateOf("") }
    var passwordTouched by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var passwordVisible by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var permissions by rememberSaveable(existing?.id, permissionOptions, stateSaver = Screen9PermissionSetSaver) {
        mutableStateOf(
            if (existing == null) permissionOptions.toSet()
            else existing.staffPermissions
        )
    }
    val isSelf = existing?.id == currentUserId
    val nameValid = name.trim().isNotBlank()
    val mobileDigits = mobile.filter(Char::isDigit).take(10)
    val mobileValid = mobileDigits.length == 10
    val passwordValid = existing != null || password.length >= 8
    val canSave = nameValid && mobileValid && passwordValid && !busy
    val initialPermissions = if (existing == null) permissionOptions.toSet() else existing.staffPermissions
    val dirty =
        name != existing?.name.orEmpty() ||
        mobileDigits != existing?.mobile.orEmpty().filter(Char::isDigit) ||
        role != (existing?.role ?: UserRole.STAFF) ||
        active != (existing?.active ?: true) ||
        password.isNotBlank() ||
        permissions != initialPermissions
    var showDiscardChanges by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!busy) {
            if (dirty) showDiscardChanges = true else onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState
    ) {
        AppFormSheetScaffold(
            title = if (existing == null) "Add User" else "Edit User",
            subtitle = if (existing == null) "Create an Owner or Staff account" else existing.name,
            busy = busy,
            onDismiss = requestDismiss,
            onSave = {
                onSave(
                    name.trim(),
                    mobileDigits,
                    role,
                    active,
                    password,
                    if (role == UserRole.OWNER) emptySet() else permissions
                )
            },
            saveEnabled = canSave,
            saveLabel = if (existing == null) "Create User" else "Save",
            expanded = true
        ) {
                AppTextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it.take(120)
                    },
                    label = "Name *",
                    placeholder = "Enter name",
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = nameTouched && !nameValid,
                    supportingText = if (nameTouched && !nameValid) "Name is required." else null
                )
                AppTextField(
                    value = mobileDigits,
                    onValueChange = {
                        mobileTouched = true
                        mobile = it.filter(Char::isDigit).take(10)
                    },
                    label = "Mobile Number *",
                    placeholder = "Enter mobile number",
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = if (existing == null) ImeAction.Next else ImeAction.Done
                    ),
                    isError = mobileTouched && !mobileValid,
                    supportingText = when {
                        !mobileTouched -> null
                        mobileDigits.isEmpty() -> "Mobile number is required."
                        !mobileValid -> "Enter a valid 10-digit mobile number."
                        else -> null
                    }
                )

                Text("Role", style = MaterialTheme.typography.labelLarge)
                Screen9RoleField(
                    role = role,
                    enabled = !isSelf,
                    onRole = { next ->
                        role = next
                        if (next == UserRole.STAFF && permissions.isEmpty()) {
                            permissions = permissionOptions.toSet()
                        }
                    }
                )

                if (existing == null) {
                    AppTextField(
                        value = password,
                        onValueChange = {
                            passwordTouched = true
                            password = it.take(128)
                        },
                        label = "Password *",
                        placeholder = "Enter password",
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        isError = passwordTouched && password.length < 8,
                        supportingText = when {
                            !passwordTouched -> null
                            password.isEmpty() -> "Password is required."
                            password.length < 8 -> "Password must contain at least 8 characters."
                            else -> null
                        }
                    )
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(if (passwordVisible) "Hide password" else "Show password")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Account", style = MaterialTheme.typography.bodyLarge)
                            if (isSelf) Text("Your own account stays active.", style = MaterialTheme.typography.labelSmall, color = AppTextMuted)
                        }
                        Switch(
                            checked = active,
                            onCheckedChange = { active = it },
                            enabled = !isSelf
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        if (role == UserRole.OWNER) "Owner Access" else "Staff Access",
                        modifier = Modifier.padding(start = AppSpacing.xs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (role == UserRole.OWNER) {
                    Text("Owner has full access.", style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        TextButton(onClick = { permissions = permissionOptions.toSet() }) { Text("Select All") }
                        TextButton(onClick = { permissions = emptySet() }) { Text("Clear All") }
                    }
                    permissionOptions.forEach { key ->
                        val label = screen9PermissionLabels[key] ?: key.lowercase().replaceFirstChar { it.titlecase() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = permissions.contains(key),
                                onCheckedChange = { checked ->
                                    permissions = if (checked) permissions + key else permissions - key
                                }
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
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
private fun Screen9RoleField(
    role: UserRole,
    enabled: Boolean,
    onRole: (UserRole) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                if (role == UserRole.OWNER) "Owner" else "Staff",
                modifier = Modifier.weight(1f).padding(start = AppSpacing.xs)
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(UserRole.OWNER, UserRole.STAFF).forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == UserRole.OWNER) "Owner" else "Staff") },
                    leadingIcon = if (option == role) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = {
                        expanded = false
                        onRole(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen9PasswordSheet(
    target: Screen9ManagedUser,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var password by remember { mutableStateOf("") }
    var passwordTouched by remember { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var showDiscardChanges by rememberSaveable(target.id) { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!busy) {
            if (password.isNotBlank()) showDiscardChanges = true else onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState
    ) {
        AppFormSheetScaffold(
            title = "Reset Password",
            subtitle = target.name,
            busy = busy,
            onDismiss = requestDismiss,
            onSave = { onSave(password) },
            saveEnabled = password.length >= 8,
            saveLabel = "Reset Password"
        ) {
            AppTextField(
                value = password,
                onValueChange = {
                    passwordTouched = true
                    password = it.take(128)
                },
                label = "New Password *",
                placeholder = "Enter new password",
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = passwordTouched && password.length < 8,
                supportingText = when {
                    !passwordTouched -> null
                    password.isEmpty() -> "Password is required."
                    password.length < 8 -> "Password must contain at least 8 characters."
                    else -> null
                }
            )
            TextButton(onClick = { visible = !visible }, enabled = !busy) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(if (visible) "Hide password" else "Show password")
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
