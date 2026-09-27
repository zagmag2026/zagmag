package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.data.WhatsAppTemplateOption
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppTextField
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppUnsavedChangesDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InfoValueRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InlineStatusMessage
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.MessageTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.PrimaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SecondaryButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft

enum class CustomerContactAction { CALL, WHATSAPP }

fun normalizedCustomerMobile(number: String): String? {
    val digits = number.filter(Char::isDigit).takeLast(10)
    return digits.takeIf { it.length == 10 }
}

fun launchCustomerCall(context: Context, number: String): Boolean {
    val digits = normalizedCustomerMobile(number) ?: return false
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    }.isSuccess
}

fun launchCustomerWhatsApp(context: Context, number: String, message: String): Boolean {
    val digits = normalizedCustomerMobile(number) ?: return false
    val uri = Uri.parse("https://wa.me/91$digits?text=${Uri.encode(message)}")
    return runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
}

/**
 * Compatibility helper retained because older Screen 4 call sites use null/non-null to decide whether
 * to route through the shared contact surface. Alternative mobile is no longer inspected: returning
 * primary keeps those call sites on the confirmation path.
 */
@Suppress("UNUSED_PARAMETER")
fun usableAlternateMobile(primary: String, alternate: String?): String? = primary

/**
 * Primary-mobile-only contact surface used by the current Admin runtime.
 * Call remains confirmation-first; WhatsApp continues to the shared template/preview flow.
 */
@Composable
fun CustomerContactChooserSheet(
    customerName: String = "",
    primary: String,
    action: CustomerContactAction,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (action == CustomerContactAction.WHATSAPP) {
        LaunchedEffect(primary) { onSelect(primary) }
    } else {
        CustomerContactConfirmationSheet(
            customerName = customerName,
            mobile = primary,
            action = CustomerContactAction.CALL,
            onDismiss = onDismiss,
            onConfirm = {
                onSelect(normalizedCustomerMobile(primary) ?: primary)
            }
        )
    }
}

/** Legacy source-compatibility overload; alternate mobile is intentionally ignored. */
@Deprecated("Current UI uses primary Mobile only.")
@Composable
@Suppress("UNUSED_PARAMETER")
fun CustomerContactChooserSheet(
    customerName: String = "",
    primary: String,
    alternate: String?,
    action: CustomerContactAction,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) = CustomerContactChooserSheet(
    customerName = customerName,
    primary = primary,
    action = action,
    onSelect = onSelect,
    onDismiss = onDismiss
)

@Composable
private fun sheetSystemBottomPadding() =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(24.dp)

@Composable
internal fun rememberReliableKeyboardDismiss(): () -> Unit =
    com.nimsdeveloper.zhagmagdresses.admin.ui.components.rememberReliableKeyboardDismiss()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerContactConfirmationSheet(
    customerName: String,
    mobile: String,
    action: CustomerContactAction,
    message: String? = null,
    busy: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val validMobile = normalizedCustomerMobile(mobile) != null
    val title = if (action == CustomerContactAction.CALL) "Confirm Call" else "WhatsApp Preview"
    val actionLabel = if (action == CustomerContactAction.CALL) "Call" else "WhatsApp"
    val actionIcon = if (action == CustomerContactAction.CALL) Icons.Rounded.Call else Icons.Rounded.Chat
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomPadding = sheetSystemBottomPadding()

    LaunchedEffect(sheetState) { sheetState.expand() }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, enabled = !busy) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            if (customerName.isNotBlank()) {
                InfoValueRow(
                    icon = Icons.Rounded.Person,
                    value = customerName,
                    contentDescription = "Name",
                    emphasized = true
                )
            }
            InfoValueRow(
                icon = if (action == CustomerContactAction.CALL) Icons.Rounded.Call else Icons.Rounded.Phone,
                value = mobile,
                contentDescription = "Mobile"
            )

            if (action == CustomerContactAction.WHATSAPP && !message.isNullOrBlank()) {
                Text("Message", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                    color = BrandSoft,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = message,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(AppSpacing.sm),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!validMobile) {
                InlineStatusMessage("Enter a valid 10-digit mobile number.", MessageTone.ERROR)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                PrimaryButton(
                    text = actionLabel,
                    onClick = onConfirm,
                    enabled = validMobile && !busy && (action != CustomerContactAction.WHATSAPP || !message.isNullOrBlank()),
                    loading = busy,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppPreparingSheet() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(sheetState) { sheetState.expand() }
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("WhatsApp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Loading applicable templates…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppTemplateSelectionSheet(
    statusGroupLabel: String,
    templates: List<WhatsAppTemplateOption>,
    onSelect: (WhatsAppTemplateOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(sheetState) { sheetState.expand() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("WhatsApp Templates", style = MaterialTheme.typography.titleLarge)
                    if (statusGroupLabel.isNotBlank()) {
                        Text(statusGroupLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                templates.forEach { template ->
                    Surface(
                        onClick = { onSelect(template) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = BrandSoft
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                template.templateName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.size(sheetSystemBottomPadding()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormSheet(
    title: String,
    initialName: String = "",
    initialMobile: String = "",
    @Suppress("UNUSED_PARAMETER") initialAlternateMobile: String = "",
    initialAddress: String = "",
    busy: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var mobile by rememberSaveable(initialMobile) { mutableStateOf(initialMobile) }
    var address by rememberSaveable(initialAddress) { mutableStateOf(initialAddress) }
    var nameTouched by rememberSaveable(initialName) { mutableStateOf(false) }
    var mobileTouched by rememberSaveable(initialMobile) { mutableStateOf(false) }
    val mobileFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val dismissKeyboard = rememberReliableKeyboardDismiss()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomPadding = sheetSystemBottomPadding()

    val primaryDigits = mobile.filter(Char::isDigit)
    val nameInvalid = name.trim().isBlank()
    val mobileInvalid = primaryDigits.length != 10
    val canSave = !nameInvalid && !mobileInvalid && !busy
    val dirty = name != initialName ||
        primaryDigits != initialMobile.filter(Char::isDigit) ||
        address != initialAddress
    var showDiscardChanges by rememberSaveable(initialName, initialMobile, initialAddress) { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!busy) {
            dismissKeyboard()
            if (dirty) showDiscardChanges = true else onDismiss()
        }
    }

    LaunchedEffect(sheetState) { sheetState.expand() }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, enabled = !busy) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                AppTextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it.take(120)
                    },
                    label = "Name *",
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { mobileFocus.requestFocus() }),
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    isError = nameTouched && nameInvalid,
                    supportingText = if (nameTouched && nameInvalid) "Name is required." else null
                )
                AppTextField(
                    value = mobile,
                    onValueChange = {
                        mobileTouched = true
                        mobile = it.filter(Char::isDigit).take(10)
                    },
                    label = "Mobile Number *",
                    modifier = Modifier.focusRequester(mobileFocus),
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { addressFocus.requestFocus() }),
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    isError = mobileTouched && mobileInvalid,
                    supportingText = when {
                        !mobileTouched -> null
                        primaryDigits.isEmpty() -> "Mobile number is required."
                        mobileInvalid -> "Enter a valid 10-digit mobile number."
                        else -> null
                    }
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it.replace('\n', ' ').take(500) },
                    label = "Address",
                    modifier = Modifier.focusRequester(addressFocus),
                    enabled = !busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) }
                )
                if (!error.isNullOrBlank()) {
                    InlineStatusMessage(error, MessageTone.ERROR)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = AppSpacing.sm,
                        bottom = bottomPadding
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = requestDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                PrimaryButton(
                    text = "Save",
                    onClick = {
                        dismissKeyboard()
                        onSave(name.trim(), primaryDigits, "", address.trim())
                    },
                    enabled = canSave,
                    loading = busy,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
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
