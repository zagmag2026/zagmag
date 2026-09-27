package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ActionTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactNewActionButton
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveSoftActionGrid
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.SoftActionSpec

@Composable
internal fun CompactNewBookingButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) = CompactNewActionButton(
    label = "New Booking",
    enabled = enabled,
    onClick = onClick
)

@Composable
internal fun RowScope.BookingCardActions(
    editable: Boolean,
    busy: Boolean = false,
    bookingActionsVisible: Boolean = true,
    contactActionsVisible: Boolean = true,
    billVisible: Boolean = false,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onBill: () -> Unit = {},
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val actions = buildList {
        if (bookingActionsVisible) {
            add(SoftActionSpec(Icons.Rounded.EventNote, "View", onView, !busy, ActionTone.INFO))
            if (editable) {
                add(SoftActionSpec(Icons.Rounded.Edit, "Edit", onEdit, !busy, ActionTone.PURPLE))
            }
            if (billVisible) {
                add(SoftActionSpec(Icons.Rounded.ReceiptLong, "Bill", onBill, !busy, ActionTone.BRAND))
            }
        }
        if (contactActionsVisible) {
            add(SoftActionSpec(Icons.Rounded.Call, "Call", onCall, !busy, ActionTone.SUCCESS))
            add(SoftActionSpec(Icons.Rounded.Chat, "WhatsApp", onWhatsApp, !busy, ActionTone.TEAL))
        }
    }
    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        ResponsiveSoftActionGrid(
            actions = actions,
            minCellWidth = 48.dp,
            maxColumns = 5
        )
    }
}
