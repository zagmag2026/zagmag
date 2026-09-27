package com.nimsdeveloper.zhagmagdresses.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.BadgeTone
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemDisplayRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.AppItemListDivider
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.CompactExpandCollapseAction
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.InfoValueRow
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.ResponsiveCompactPair
import com.nimsdeveloper.zhagmagdresses.admin.ui.components.StatusBadge
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppBorder
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppSpacing
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.AppTextMuted
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.BrandPrimary
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardCategories
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardCategoriesSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGiven
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardGivenSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItems
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardItemsSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardTotal
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.DashboardTotalSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusError
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusErrorSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccess
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusSuccessSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfo
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusInfoSoft
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarning
import com.nimsdeveloper.zhagmagdresses.admin.ui.theme.StatusWarningSoft
import com.nimsdeveloper.zhagmagdresses.admin.data.BookingDetailData

internal data class BookingSummaryItemUi(
    val imageUrl: String?,
    val itemName: String,
    val itemCode: String = "",
    val categoryName: String = "",
    val quantity: Int,
    val statusLine: String? = null,
    val imageUrls: List<String> = emptyList()
)

@Composable
internal fun BookingSummaryCard(
    id: String,
    customerName: String,
    customerMobile: String,
    customerAddress: String = "",
    bookingNo: String,
    displayStatus: String,
    paymentStatus: String? = null,
    bookingDate: String = "",
    pickupDate: String,
    returnDate: String,
    items: List<BookingSummaryItemUi>,
    totalItemCount: Int = items.size,
    fallbackItemsSummary: String = "",
    containerColor: Color = MaterialTheme.colorScheme.surface,
    showAllItems: Boolean = false,
    showNext: Boolean = true,
    showActions: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    val shownItems = if (showAllItems || expanded) items else items.take(2)
    val effectiveTotal = maxOf(totalItemCount, items.size)
    val hiddenCount = if (showAllItems) 0 else (effectiveTotal - 2).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    InfoValueRow(Icons.Rounded.Person, customerName, emphasized = true)
                    InfoValueRow(Icons.Rounded.Phone, customerMobile)
                    if (customerAddress.isNotBlank()) {
                        InfoValueRow(Icons.Rounded.LocationOn, customerAddress, muted = true)
                    }
                    InfoValueRow(Icons.Rounded.ReceiptLong, bookingNo, muted = true)
                }
                if (!displayStatus.equals("RESERVED", true) && !paymentStatus.isNullOrBlank()) {
                    StatusBadge(bookingPaymentLabel(paymentStatus), bookingPaymentTone(paymentStatus))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BookingMetaStatusCard(
                    title = "Pickup",
                    status = bookingPickupStatus(displayStatus),
                    date = pickupDate,
                    icon = Icons.Rounded.LocalShipping,
                    background = StatusInfoSoft,
                    foreground = StatusInfo,
                    modifier = Modifier.weight(1f)
                )
                val currentColors = bookingCurrentColors(displayStatus)
                BookingMetaStatusCard(
                    title = "Current",
                    status = lifecycleLabel4(displayStatus),
                    date = bookingDate.ifBlank { pickupDate },
                    icon = Icons.Rounded.CheckCircle,
                    background = currentColors.first,
                    foreground = currentColors.second,
                    modifier = Modifier.weight(1f)
                )
                BookingMetaStatusCard(
                    title = "Return",
                    status = bookingReturnStatus(displayStatus),
                    date = returnDate,
                    icon = Icons.Rounded.Reply,
                    background = StatusWarningSoft,
                    foreground = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            if (shownItems.isNotEmpty()) {
                shownItems.forEachIndexed { index, item ->
                    AppItemDisplayRow(
                        imageUrl = item.imageUrl,
                        imageUrls = item.imageUrls,
                        itemName = item.itemName,
                        itemCode = item.itemCode,
                        categoryName = item.categoryName,
                        quantity = item.quantity,
                        statusLine = item.statusLine
                    )
                    if (index != shownItems.lastIndex) AppItemListDivider()
                }
                if (!showAllItems) {
                    CompactExpandCollapseAction(
                        hiddenCount = hiddenCount,
                        expanded = expanded,
                        onToggle = { expanded = !expanded }
                    )
                }
            } else if (fallbackItemsSummary.isNotBlank()) {
                Text(fallbackItemsSummary, style = MaterialTheme.typography.bodyMedium)
            }

            if (showNext) bookingNextLabel(displayStatus)?.let { next ->
                CompactInfoPill4(
                    icon = Icons.Rounded.ArrowForward,
                    label = "Next",
                    value = next,
                    background = StatusWarningSoft,
                    foreground = StatusWarning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showActions) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

internal fun bookingPaymentLabel(status: String): String = when (status.uppercase()) {
    "FULL_AMOUNT_RECEIVED", "FULL_PAYMENT" -> "Full Payment"
    "PART_RECEIVED", "PART_PAYMENT" -> "Part Payment"
    "PENDING", "PENDING_PAYMENT" -> "Pending Payment"
    else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

internal fun bookingPaymentTone(status: String): BadgeTone = when (status.uppercase()) {
    "FULL_AMOUNT_RECEIVED", "FULL_PAYMENT" -> BadgeTone.SUCCESS
    "PART_RECEIVED", "PART_PAYMENT" -> BadgeTone.INFO
    else -> BadgeTone.WARNING
}

private fun bookingCurrentColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "RESERVED" -> DashboardCategoriesSoft to DashboardCategories
    "BOOKED" -> BrandSoft to BrandPrimary
    "PART_PICKUP" -> DashboardItemsSoft to DashboardItems
    "FULL_PICKUP" -> DashboardGivenSoft to DashboardGiven
    "PART_RETURN" -> DashboardTotalSoft to DashboardTotal
    "FULL_RETURN" -> StatusSuccessSoft to StatusSuccess
    "CANCELLED" -> StatusErrorSoft to StatusError
    else -> BrandSoft to BrandPrimary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingOrderPreviewSheet(
    detail: BookingDetailData,
    onDismiss: () -> Unit
) {
    val booking = detail.booking
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Text("Order Preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BookingSummaryCard(
                id = "preview-${booking.id}",
                customerName = booking.customerName,
                customerMobile = booking.customerMobile,
                customerAddress = booking.customerAddress,
                bookingNo = booking.bookingNo,
                displayStatus = booking.displayStatus,
                paymentStatus = booking.paymentStatus,
                bookingDate = booking.bookingDate,
                pickupDate = booking.pickupDate,
                returnDate = booking.returnDate,
                items = detail.items.map { item ->
                    BookingSummaryItemUi(
                        imageUrl = item.imageUrl,
                        imageUrls = item.imageUrls,
                        itemName = item.itemName,
                        itemCode = item.itemCode,
                        categoryName = item.categoryName,
                        quantity = item.bookedQty,
                        statusLine = "Booked ${item.bookedQty} · Picked ${item.givenQty} · Returned ${item.returnedQty}"
                    )
                },
                totalItemCount = detail.items.size,
                showAllItems = true,
                showNext = true,
                showActions = false
            )
        }
    }
}

@Composable
private fun BookingMetaStatusCard(
    title: String,
    status: String,
    date: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    val formatted = formatDateDay4(date)
    val parts = formatted.split(" · ", limit = 2)
    val dateText = parts.getOrNull(0).orEmpty().ifBlank { date }
    val dayText = parts.getOrNull(1).orEmpty()
    Surface(
        modifier = modifier,
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                Text(status, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                Text(dateText, fontSize = 10.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Rounded.EventNote, contentDescription = null, modifier = Modifier.size(12.dp))
                Text(dayText, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

private fun bookingPickupStatus(status: String): String = when (status.uppercase()) {
    "FULL_PICKUP", "PART_RETURN", "FULL_RETURN" -> "Done"
    else -> "Pending"
}

private fun bookingReturnStatus(status: String): String =
    if (status.uppercase() == "FULL_RETURN") "Done" else "Pending"

private fun bookingNextLabel(status: String): String? = when (status.uppercase()) {
    "RESERVED" -> "Confirmation Pending"
    "BOOKED" -> "Pickup Pending"
    "PART_PICKUP" -> "Pickup / Return Pending"
    "FULL_PICKUP" -> "Return Pending"
    "PART_RETURN" -> "Return Pending"
    else -> null
}
