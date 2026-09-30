package com.nimsdeveloper.zhagmagdresses.admin

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object Screen11BillingPdfExporter {
    private val displayDate = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
    private val displayDay = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
    private val displayTime = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val businessZone = ZoneId.of("Asia/Kolkata")

    fun create(
        context: Context,
        branding: AppBranding,
        detail: BillingDetail
    ): File {
        val bill = detail.bill
        val isQuotation = bill.status == "DRAFT"
        if (!isQuotation) require(!bill.billNo.isNullOrBlank()) { "Finalize the Bill before creating PDF." }

        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentWidth = pageWidth - margin * 2
        val itemsPerPage = 13
        val pages = detail.items.chunked(itemsPerPage).ifEmpty { listOf(emptyList()) }
        val business = detail.business
        val shopName = business.shopName.ifBlank { branding.shopName.ifBlank { "Zhagmag Dresses" } }

        val document = PdfDocument()
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 45, 58)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bold = Paint(normal).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val small = Paint(normal).apply { textSize = 7.8f }
        val muted = Paint(small).apply { color = Color.rgb(90, 101, 114) }
        val brand = Paint(bold).apply {
            textSize = 14f
            color = if (isQuotation) Color.rgb(83, 72, 138) else Color.rgb(20, 73, 145)
        }
        val billTitle = Paint(bold).apply {
            textSize = 22f
            color = if (isQuotation) Color.rgb(62, 55, 104) else Color.rgb(26, 49, 78)
        }
        val section = Paint(bold).apply {
            textSize = 9.5f
            color = if (isQuotation) Color.rgb(83, 72, 138) else Color.rgb(20, 73, 145)
        }
        val amountStrong = Paint(bold).apply {
            textSize = 10.5f
            color = if (isQuotation) Color.rgb(62, 55, 104) else Color.rgb(26, 49, 78)
        }
        val balancePaint = Paint(bold).apply {
            textSize = 13f
            color = if (bill.balanceAmount > 0) Color.rgb(181, 48, 48) else Color.rgb(20, 110, 75)
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(208, 217, 227)
            strokeWidth = 0.8f
        }
        val cardBorder = Paint(line).apply { style = Paint.Style.STROKE }
        val softBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isQuotation) Color.rgb(244, 242, 252) else Color.rgb(239, 246, 255)
            style = Paint.Style.FILL
        }
        val softGray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val softGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 248, 240)
            style = Paint.Style.FILL
        }

        fun formattedDate(value: String): String =
            runCatching { LocalDate.parse(value).format(displayDate) }.getOrDefault(value)

        fun day(value: String): String =
            runCatching { LocalDate.parse(value).format(displayDay) }.getOrDefault("")

        fun eventTime(value: String): String {
            if (value.isBlank()) return ""
            val zoned = runCatching { Instant.parse(value).atZone(businessZone) }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(value.replace(' ', 'T').take(19))
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(businessZone)
                }.getOrNull()
                ?: return ""
            return zoned.format(displayTime)
        }

        fun truncate(value: String, paint: Paint, maxWidth: Float): String {
            val clean = value.replace("\n", " ").trim()
            if (paint.measureText(clean) <= maxWidth) return clean
            var end = clean.length
            while (end > 1 && paint.measureText(clean.substring(0, end) + "...") > maxWidth) end--
            return clean.substring(0, end.coerceAtLeast(1)).trimEnd() + "..."
        }

        fun drawCard(canvas: android.graphics.Canvas, left: Float, top: Float, right: Float, bottom: Float) {
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 7f, 7f, softGray)
            canvas.drawRoundRect(rect, 7f, 7f, cardBorder)
        }

        fun drawSectionTitle(canvas: android.graphics.Canvas, text: String, x: Float, y: Float) {
            canvas.drawText(text, x, y, section)
        }

        fun drawInfoLine(
            canvas: android.graphics.Canvas,
            label: String,
            value: String,
            x: Float,
            y: Float,
            width: Float
        ) {
            canvas.drawText(label, x, y, muted)
            val labelWidth = muted.measureText(label) + 8f
            canvas.drawText(truncate(value.ifBlank { "-" }, normal, width - labelWidth), x + labelWidth, y, normal)
        }

        pages.forEachIndexed { pageIndex, pageItems ->
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            )
            val canvas = page.canvas
            var y = 28f

            runCatching {
                context.getDrawable(R.drawable.ic_launcher)?.let { drawable ->
                    drawable.setBounds(margin.toInt(), y.toInt(), (margin + 32f).toInt(), (y + 32f).toInt())
                    drawable.draw(canvas)
                }
            }
            canvas.drawText(truncate(shopName, brand, 250f), margin + 42f, y + 15f, brand)
            canvas.drawText(if (isQuotation) "Rental Quotation" else "Rental Bill", margin + 42f, y + 29f, muted)

            val documentTitle = if (isQuotation) "QUOTATION" else "BILL"
            canvas.drawText(documentTitle, pageWidth - margin - billTitle.measureText(documentTitle), y + 15f, billTitle)
            val rightX = pageWidth - margin
            fun rightText(text: String, baseline: Float, paint: Paint = normal) {
                canvas.drawText(text, rightX - paint.measureText(text), baseline, paint)
            }
            if (isQuotation) rightText("Quotation  ${bill.bookingNo ?: "-"}", y + 32f, bold) else rightText("Bill No.  ${bill.billNo}", y + 32f, bold)
            rightText("Order  ${bill.bookingNo ?: "-"}", y + 44f, small)
            rightText("Date  ${formattedDate(bill.billDate)}", y + 56f, small)
            rightText(if (isQuotation) "QUOTATION" else "FINALIZED", y + 68f, bold)
            y += 78f
            canvas.drawLine(margin, y, pageWidth - margin, y, line)
            y += 14f

            drawSectionTitle(canvas, "Booking & Customer", margin, y)
            y += 8f
            val customerTop = y
            val customerHeight = if (bill.customerAddress.isBlank()) 50f else 62f
            drawCard(canvas, margin, customerTop, pageWidth - margin, customerTop + customerHeight)
            var cy = customerTop + 14f
            drawInfoLine(canvas, "Customer", bill.customerName, margin + 10f, cy, contentWidth - 20f)
            cy += 12f
            drawInfoLine(canvas, "Mobile", bill.customerMobile, margin + 10f, cy, contentWidth - 20f)
            if (bill.customerAddress.isNotBlank()) {
                cy += 12f
                drawInfoLine(canvas, "Address", bill.customerAddress, margin + 10f, cy, contentWidth - 20f)
            }
            cy += 12f
            drawInfoLine(canvas, "Order ID", bill.bookingNo ?: "-", margin + 10f, cy, contentWidth - 20f)
            y = customerTop + customerHeight + 14f

            drawSectionTitle(
                canvas,
                "Items (${detail.items.size})" + if (pages.size > 1) " · Page ${pageIndex + 1}/${pages.size}" else "",
                margin,
                y
            )
            y += 8f
            val itemLeft = margin
            val itemRight = pageWidth - margin
            val headerH = 22f
            canvas.drawRect(itemLeft, y, itemRight, y + headerH, softBlue)
            canvas.drawText("Item", itemLeft + 8f, y + 15f, bold)
            canvas.drawText("Qty × Rate", itemLeft + 300f, y + 15f, bold)
            canvas.drawText("Amount", itemLeft + 430f, y + 15f, bold)
            y += headerH

            pageItems.forEach { item ->
                val rowH = 29f
                canvas.drawText(truncate(item.itemName, normal, 230f), itemLeft + 8f, y + 12f, bold)
                canvas.drawText(
                    truncate("${item.itemCode} · ${item.categoryName}", muted, 230f),
                    itemLeft + 8f,
                    y + 23f,
                    muted
                )
                canvas.drawText("${item.quantity} × ₹${item.rentRate}", itemLeft + 300f, y + 18f, normal)
                canvas.drawText("₹${item.amount}", itemLeft + 430f, y + 18f, bold)
                y += rowH
                canvas.drawLine(itemLeft, y, itemRight, y, line)
            }
            y += 14f

            if (pageIndex == pages.lastIndex) {
                val gap = 12f
                val leftWidth = 210f
                val rightLeft = margin + leftWidth + gap
                val rightWidth = contentWidth - leftWidth - gap
                val blockTop = y

                drawSectionTitle(canvas, "Pickup / Return", margin, blockTop)
                drawSectionTitle(canvas, "Amount Summary", rightLeft, blockTop)
                val cardTop = blockTop + 8f
                val cardHeight = 168f
                drawCard(canvas, margin, cardTop, margin + leftWidth, cardTop + cardHeight)
                drawCard(canvas, rightLeft, cardTop, rightLeft + rightWidth, cardTop + cardHeight)

                fun lifecycleLine(label: String, value: String, baseline: Float, valuePaint: Paint = normal) {
                    canvas.drawText(label, margin + 10f, baseline, muted)
                    canvas.drawText(
                        truncate(value, valuePaint, leftWidth - 68f),
                        margin + 58f,
                        baseline,
                        valuePaint
                    )
                }

                var ly = cardTop + 17f
                canvas.drawText("Pickup", margin + 10f, ly, bold)
                ly += 15f
                lifecycleLine("Status", if (bill.pickupComplete) "Done" else "Pending", ly, bold)
                ly += 13f
                lifecycleLine("Date", formattedDate(bill.pickupDate), ly)
                ly += 13f
                lifecycleLine("Day", day(bill.pickupDate), ly)
                eventTime(bill.pickupAt).takeIf { it.isNotBlank() }?.let { time ->
                    ly += 13f
                    lifecycleLine("Time", time, ly)
                }

                ly = cardTop + 88f
                canvas.drawLine(margin + 10f, ly - 10f, margin + leftWidth - 10f, ly - 10f, line)
                canvas.drawText("Return", margin + 10f, ly, bold)
                ly += 15f
                lifecycleLine("Status", if (bill.returnComplete) "Done" else "Pending", ly, bold)
                ly += 13f
                lifecycleLine("Date", formattedDate(bill.returnDate), ly)
                ly += 13f
                lifecycleLine("Day", day(bill.returnDate), ly)
                eventTime(bill.returnAt).takeIf { it.isNotBlank() }?.let { time ->
                    ly += 13f
                    lifecycleLine("Time", time, ly)
                }

                val otherReceived = (bill.receivedAmount - bill.advanceAmount).coerceAtLeast(0)
                var ay = cardTop + 17f
                fun amountRow(label: String, value: Int, strong: Boolean = false, minus: Boolean = false) {
                    val p = if (strong) bold else normal
                    canvas.drawText(label, rightLeft + 10f, ay, p)
                    val text = (if (minus && value > 0) "−" else "") + "₹" + value
                    canvas.drawText(text, rightLeft + rightWidth - 10f - p.measureText(text), ay, p)
                    ay += 16f
                }
                amountRow("Item Total", bill.totalRent)
                amountRow("Discount", bill.discountAmount, minus = true)
                amountRow("Bill Amount", bill.netAmount, strong = true)
                amountRow("Advance Received", bill.advanceAmount)
                amountRow("Other Received", otherReceived)
                amountRow("Total Received", bill.receivedAmount, strong = true)
                canvas.drawLine(rightLeft + 10f, ay - 7f, rightLeft + rightWidth - 10f, ay - 7f, line)
                canvas.drawText("Balance Due", rightLeft + 10f, ay + 5f, amountStrong)
                val balanceText = "₹${bill.balanceAmount}"
                canvas.drawText(
                    balanceText,
                    rightLeft + rightWidth - 10f - balancePaint.measureText(balanceText),
                    ay + 5f,
                    balancePaint
                )
                y = cardTop + cardHeight + 14f

                drawSectionTitle(canvas, "Notes", margin, y)
                y += 8f
                val notesTop = y
                val notesHeight = 46f
                drawCard(canvas, margin, notesTop, pageWidth - margin, notesTop + notesHeight)
                canvas.drawText(
                    truncate(bill.notes.ifBlank { "No notes." }, normal, contentWidth - 20f),
                    margin + 10f,
                    notesTop + 18f,
                    if (bill.notes.isBlank()) muted else normal
                )
                y = notesTop + notesHeight + 14f

                canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 36f), 7f, 7f, softGreen)
                canvas.drawText("Thank You", margin + 12f, y + 16f, brand)
                canvas.drawText("Thank you for choosing $shopName.", margin + 12f, y + 29f, muted)
                val signText = "Authorized Signatory"
                canvas.drawText(signText, pageWidth - margin - 12f - bold.measureText(signText), y + 23f, bold)
            }

            val footerTop = pageHeight - 48f
            canvas.drawLine(margin, footerTop, pageWidth - margin, footerTop, line)
            val footerParts = listOf(
                business.address,
                business.contactNumber.takeIf { it.isNotBlank() }?.let { "Mobile: $it" }.orEmpty(),
                business.websiteUrl
            ).filter { it.isNotBlank() }
            val footer = footerParts.joinToString("  ·  ")
            canvas.drawText(truncate(footer, muted, contentWidth - 80f), margin, footerTop + 17f, muted)
            val pageText = "Page ${pageIndex + 1} / ${pages.size}"
            canvas.drawText(pageText, pageWidth - margin - muted.measureText(pageText), footerTop + 17f, muted)

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "report-pdfs").apply { mkdirs() }
        val safeNo = (bill.billNo ?: "Quotation_${bill.bookingNo ?: "Order"}").replace("/", "-").replace(" ", "_")
        val file = File(dir, if (isQuotation) "Zhagmag_Quotation_$safeNo.pdf" else "Zhagmag_Bill_$safeNo.pdf")
        file.outputStream().use(document::writeTo)
        document.close()
        return file
    }
    
    /**
     * Shares a Quotation PDF directly to the customer's WhatsApp chat.
     * Keep the shared launcher in Screen8PdfExporter so the existing PDF
     * sharing behavior remains centralized for Admin Android.
     */
    fun shareDirectToCustomerWhatsApp(
        context: Context,
        file: File,
        customerMobile: String,
        title: String
    ) {
        Screen8PdfExporter.shareDirectToCustomerWhatsApp(
            context = context,
            file = file,
            customerMobile = customerMobile,
            title = title
        )
    }


}
