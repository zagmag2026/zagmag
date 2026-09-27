package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppConfirmDialog
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGiven
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGivenSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainTopBar(
    branding: AppBranding,
    user: SessionUser,
    onLogout: () -> Unit
) {
    var confirmLogout by remember { mutableStateOf(false) }
    AdminMainTopBarContent(branding, user) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text("Log out") } },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = "Log out")
            }
        }
    }

    if (confirmLogout) {
        AppConfirmDialog(
            title = "Log out?",
            message = "Log out of Zhagmag Dresses Admin?",
            confirmLabel = "Log out",
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
fun AdminMainTopBar(
    branding: AppBranding,
    user: SessionUser,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {
    AdminMainTopBarContent(branding, user) {
        SoftActionButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Refresh",
            onClick = onRefresh,
            enabled = !refreshing,
            tone = ActionTone.INFO
        )
    }
}

@Composable
private fun AdminMainTopBarContent(
    branding: AppBranding,
    user: SessionUser,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            model = branding.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            placeholder = painterResource(R.drawable.ic_launcher),
            error = painterResource(R.drawable.ic_launcher),
            fallback = painterResource(R.drawable.ic_launcher),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = branding.shopName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${user.name} · ${user.role.name}",
                style = MaterialTheme.typography.labelMedium,
                color = AppTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        action()
    }
}

@Composable
fun MainScreenDateRow(today: String) {
    val zone = remember { ZoneId.of("Asia/Kolkata") }
    var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(zone, now) {
        val elapsedInMinute = now.second * 1_000L + now.nano / 1_000_000L
        delay((60_000L - elapsedInMinute).coerceAtLeast(1_000L))
        now = ZonedDateTime.now(zone)
    }
    val currentBusinessDate = now.toLocalDate()
    val businessDate = remember(today, currentBusinessDate) {
        val supplied = runCatching { LocalDate.parse(today) }.getOrNull()
        if (supplied == null || supplied.isBefore(currentBusinessDate)) currentBusinessDate else supplied
    }
    val dateText = remember(businessDate) {
        businessDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH))
    }
    val dayText = remember(businessDate) {
        businessDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    }
    val timeText = now.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)).lowercase(Locale.ENGLISH)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateMetaCell(
                icon = Icons.Rounded.DateRange,
                text = dateText,
                soft = StatusInfoSoft,
                foreground = StatusInfo,
                modifier = Modifier.weight(1f)
            )
            DateMetaDivider()
            DateMetaCell(
                icon = Icons.Rounded.Event,
                text = dayText,
                soft = DashboardGivenSoft,
                foreground = DashboardGiven,
                modifier = Modifier.weight(1f)
            )
            DateMetaDivider()
            DateMetaCell(
                icon = Icons.Rounded.Schedule,
                text = timeText,
                soft = StatusSuccessSoft,
                foreground = StatusSuccess,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateMetaCell(
    icon: ImageVector,
    text: String,
    soft: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(color = soft, contentColor = foreground, shape = MaterialTheme.shapes.small) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun DateMetaDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(24.dp)
            .background(AppBorder)
    )
}
