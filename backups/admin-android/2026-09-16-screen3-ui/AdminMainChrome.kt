package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainTopBar(
    branding: AppBranding,
    user: SessionUser,
    onLogout: () -> Unit
) {
    var confirmLogout by remember { mutableStateOf(false) }
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
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${user.name} · ${user.role.name}",
                style = MaterialTheme.typography.labelSmall,
                color = AppTextMuted
            )
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text("Log out") } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = { confirmLogout = true }) {
                Icon(Icons.Rounded.Logout, contentDescription = "Log out")
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MainScreenDateRow(today: String) {
    if (today.isBlank()) return
    val formatted = remember(today) {
        runCatching {
            LocalDate.parse(today).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
        }.getOrDefault(today)
    }
    Text(
        text = formatted,
        style = MaterialTheme.typography.labelLarge,
        color = AppTextMuted
    )
}
