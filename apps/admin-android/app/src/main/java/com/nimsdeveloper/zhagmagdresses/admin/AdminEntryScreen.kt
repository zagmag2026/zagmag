package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppCard
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing

@Composable
fun ZhagmagAdminRoot(
    adminViewModel: AdminViewModel,
    bookingViewModel: BookingLifecycleViewModel
) {
    when (adminViewModel.authState) {
        AuthState.Checking -> EntryCheckingScreen(adminViewModel.branding)
        is AuthState.SessionCheckFailed -> SessionCheckFailedScreen(
            branding = adminViewModel.branding,
            message = (adminViewModel.authState as AuthState.SessionCheckFailed).message,
            onRetry = adminViewModel::retrySessionCheck
        )
        AuthState.SignedOut -> AdminLoginScreen(
            branding = adminViewModel.branding,
            busy = adminViewModel.loginBusy,
            error = adminViewModel.loginError,
            environmentError = adminViewModel.environmentError,
            onClearError = adminViewModel::clearLoginError,
            onLogin = adminViewModel::login
        )
        is AuthState.SignedIn -> AdminAppCompact(adminViewModel, bookingViewModel)
    }
}

@Composable
private fun EntryCheckingScreen(branding: AppBranding) {
    EntrySurface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            DynamicBrandLogo(branding)
            Text(
                text = branding.shopName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
internal fun SessionCheckFailedScreen(
    branding: AppBranding,
    message: String,
    onRetry: () -> Unit
) {
    EntrySurface {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                DynamicBrandLogo(branding)
                Text("Connection unavailable", style = MaterialTheme.typography.titleLarge)
                InlineMessage(message, error = true)
                PrimaryButton(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLoginScreen(
    branding: AppBranding,
    busy: Boolean,
    error: String?,
    environmentError: String?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var mobile by rememberSaveable { mutableStateOf("") }
    // Passwords and visibility state are intentionally not saveable. They must not
    // survive process recreation or a fresh Login screen after logout.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val mobileValid = mobile.length == 10
    val mobileError = mobile.isNotEmpty() && !mobileValid
    val passwordValid = password.length >= 8
    val passwordError = password.isNotEmpty() && !passwordValid
    val canSubmit = mobileValid && passwordValid && environmentError.isNullOrBlank() && !busy
    val submit = remember(mobile, password, canSubmit) {
        {
            if (canSubmit) onLogin(mobile, password)
        }
    }

    EntrySurface {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DynamicBrandLogo(branding)
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    text = branding.shopName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(AppSpacing.sm))
                Text("Sign in", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(AppSpacing.md))

                AppTextField(
                    value = mobile,
                    onValueChange = { raw ->
                        mobile = raw.filter(Char::isDigit).take(10)
                        onClearError()
                    },
                    label = "Mobile number",
                    placeholder = "10 digit mobile number",
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = if (mobileError) "Enter a 10-digit mobile number." else null,
                    isError = mobileError
                )
                Spacer(Modifier.height(AppSpacing.xs))

                AppTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onClearError()
                    },
                    label = "Password",
                    enabled = !busy,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    isError = passwordError,
                    supportingText = if (passwordError) "Password must contain at least 8 characters." else null,
                    trailingIcon = {
                        val tooltipText = if (passwordVisible) "Hide password" else "Show password"
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(tooltipText) } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = tooltipText
                                )
                            }
                        }
                    }
                )

                if (!environmentError.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    InlineMessage("Service unavailable.", error = true)
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    InlineMessage(error, error = true)
                }

                Spacer(Modifier.height(AppSpacing.md))
                PrimaryButton(
                    text = if (busy) "Please wait…" else "Sign in",
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit,
                    loading = busy,
                    icon = {
                        Icon(Icons.Rounded.Login, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun EntrySurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
            content()
        }
    }
}

@Composable
private fun DynamicBrandLogo(branding: AppBranding) {
    val fallback = painterResource(R.drawable.ic_launcher)
    AsyncImage(
        model = branding.logoUrl,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Fit
    )
}
