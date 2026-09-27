package com.nimsdeveloper.zhagmagdresses.admin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusErrorSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft

@Composable
fun AppPageHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)
        }
        if (action != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                action()
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) { content() }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        supportingText = supportingText?.let { text -> { Text(text) } },
        isError = isError,
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = AppSpacing.xs),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else if (icon != null) {
            icon()
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(text)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(text)
    }
}

enum class BadgeTone { NEUTRAL, SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusBadge(text: String, tone: BadgeTone = BadgeTone.NEUTRAL) {
    val pair = when (tone) {
        BadgeTone.SUCCESS -> StatusSuccessSoft to StatusSuccess
        BadgeTone.WARNING -> StatusWarningSoft to StatusWarning
        BadgeTone.ERROR -> StatusErrorSoft to StatusError
        BadgeTone.INFO -> StatusInfoSoft to StatusInfo
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = pair.first, contentColor = pair.second, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun InlineMessage(message: String?, error: Boolean = false) {
    if (message.isNullOrBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (error) StatusErrorSoft else StatusSuccessSoft,
        contentColor = if (error) StatusError else StatusSuccess,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(message, modifier = Modifier.padding(AppSpacing.sm), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xl),
        style = MaterialTheme.typography.bodyMedium,
        color = AppTextMuted
    )
}

@Composable
fun LoadingState() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xl),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

fun statusTone(status: String): BadgeTone = when (status.uppercase()) {
    "COMPLETED", "RETURNED", "ACTIVE" -> BadgeTone.SUCCESS
    "CANCELLED", "OVERDUE" -> BadgeTone.ERROR
    "CONFIRMED", "BOOKED", "PARTIAL_PICKUP", "PARTIAL_RETURN" -> BadgeTone.WARNING
    else -> BadgeTone.INFO
}
