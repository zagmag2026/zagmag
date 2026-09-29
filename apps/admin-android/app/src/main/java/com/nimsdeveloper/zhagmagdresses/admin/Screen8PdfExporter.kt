package com.nimsdeveloper.zhagmagdresses.admin

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.nimsdeveloper.zhagmagdresses.admin.data.AppBranding
import com.nimsdeveloper.zhagmagdresses.admin.data.SessionUser
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object Screen8PdfExporter {
    private const val MIME = "application/pdf"

    fun purgeCache(context: Context) {
        runCatching { File(context.cacheDir, "report-pdfs").deleteRecursively() }
    }

    fun create(
        context: Context,
        branding: AppBranding,
        user: SessionUser,
        report: Screen8ReportPage
    ): File {
        require(report.rows.isNotEmpty()) { "Report has no data." }

        val landscape = report.columns.size >= 7
        val pageWidth = if (landscape) 842 else 595
        val pageHeight = if (landscape) 595 else 842
        val margin = 30f
        val footerSpace = 30f
        val availableWidth = pageWidth - margin * 2
        val tableColumns = report.columns.ifEmpty {
            report.rows.firstOrNull()?.values?.keys?.map { Screen8Column(it, it) }.orEmpty()
        }.ifEmpty {
            listOf(Screen8Column("_", "Details"))
        }

        val document = PdfDocument()
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 45, 58)
            textSize = 8.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val small = Paint(normal).apply { textSize = 7.4f }
        val muted = Paint(small).apply { color = Color.rgb(90, 101, 114) }
        val bold = Paint(normal).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val smallBold = Paint(bold).apply { textSize = 7.6f }
        val titlePaint = Paint(bold).apply {
            textSize = 15f
            color = Color.rgb(26, 49, 78)
        }
        val shopPaint = Paint(bold).apply {
            textSize = 11f
            color = Color.rgb(20, 73, 145)
        }
        val sectionPaint = Paint(bold).apply {
            textSize = 10f
            color = Color.rgb(20, 73, 145)
        }
        val kpiPaint = Paint(bold).apply {
            textSize = 12f
            color = Color.rgb(20, 73, 145)
        }
        val kpiLabelPaint = Paint(muted).apply { textSize = 7.2f }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 214, 224)
            strokeWidth = 0.8f
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 214, 224)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(231, 240, 251)
            style = Paint.Style.FILL
        }
        val brandFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(247, 250, 254)
            style = Paint.Style.FILL
        }
        val filterFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(249, 251, 253)
            style = Paint.Style.FILL
        }
        val summaryFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 249, 255)
            style = Paint.Style.FILL
        }
        val alternateRowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(250, 252, 254)
            style = Paint.Style.FILL
        }

        val semanticKeys = setOf(
            "status", "action", "availability_status", "customer_type", "outcome", "exception_type"
        )
        val displayDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
        val displayTimeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun truncate(value: String, paint: Paint, maxWidth: Float): String {
            val clean = value.replace("\n", " ").replace("\r", " ").trim()
            if (paint.measureText(clean) <= maxWidth) return clean
            if (clean.length <= 1) return clean
            var endIndex = clean.length
            while (endIndex > 1 && paint.measureText(clean.substring(0, endIndex) + "...") > maxWidth) {
                endIndex--
            }
            return clean.substring(0, endIndex.coerceAtLeast(1)).trimEnd() + "..."
        }

        fun wrapText(
            value: String,
            paint: Paint,
            maxWidth: Float,
            maxLines: Int = Int.MAX_VALUE
        ): List<String> {
            val clean = value.replace("\n", " ").replace("\r", " ").trim().ifBlank { "-" }
            if (maxWidth <= 8f) return listOf(truncate(clean, paint, maxWidth.coerceAtLeast(8f)))
            if (paint.measureText(clean) <= maxWidth) return listOf(clean)

            val lines = mutableListOf<String>()
            var remaining = clean
            while (remaining.isNotBlank() && lines.size < maxLines) {
                if (paint.measureText(remaining) <= maxWidth) {
                    lines += remaining
                    remaining = ""
                    break
                }

                var endIndex = remaining.length
                while (endIndex > 1 && paint.measureText(remaining.substring(0, endIndex)) > maxWidth) {
                    endIndex--
                }
                var splitIndex = endIndex.coerceAtLeast(1)
                val lastSpace = remaining.lastIndexOf(' ', startIndex = (splitIndex - 1).coerceAtLeast(0))
                if (lastSpace > 0 && lastSpace >= splitIndex / 2) splitIndex = lastSpace

                val line = remaining.substring(0, splitIndex).trim()
                lines += line.ifBlank { remaining.substring(0, endIndex.coerceAtLeast(1)) }
                remaining = remaining.substring(splitIndex).trimStart()
            }

            if (remaining.isNotBlank() && lines.isNotEmpty()) {
                var last = lines.last()
                while (last.length > 1 && paint.measureText(last + "...") > maxWidth) {
                    last = last.dropLast(1).trimEnd()
                }
                lines[lines.lastIndex] = last + "..."
            }
            return lines.ifEmpty { listOf("-") }
        }

        fun humanizeCode(value: String): String =
            value.replace('_', ' ')
                .lowercase(Locale.ENGLISH)
                .split(' ')
                .filter(String::isNotBlank)
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

        fun formatDateLike(value: String): String {
            val clean = value.trim()
            val match = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?.*$""")
                .matchEntire(clean) ?: return clean
            val date = runCatching {
                LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt()
                )
            }.getOrNull() ?: return clean
            val dateText = date.format(displayDateFormat)
            val hour = match.groupValues.getOrNull(4).orEmpty()
            val minute = match.groupValues.getOrNull(5).orEmpty()
            if (hour.isBlank() || minute.isBlank()) return dateText
            val time = runCatching { LocalTime.of(hour.toInt(), minute.toInt()) }.getOrNull()
                ?: return dateText
            return "$dateText ${time.format(displayTimeFormat)}"
        }

        fun formatPdfValue(column: Screen8Column, rawValue: String): String {
            val value = rawValue.trim().ifBlank { "-" }
            val key = column.key.lowercase(Locale.ENGLISH)
            return when {
                key in semanticKeys -> screen8StatusLabel(value)
                key.contains("date") || key.endsWith("_at") ||
                    Regex("""^\d{4}-\d{2}-\d{2}([T ].*)?$""").matches(value) -> formatDateLike(value)
                else -> value.replace("\n", " ").replace("\r", " ")
            }
        }

        fun appliedFilterParts(): List<String> {
            val f = report.appliedFilters
            val parts = mutableListOf<String>()
            if (f.fromDate.isNotBlank() || f.toDate.isNotBlank()) {
                parts += "Date: ${formatDateLike(f.fromDate.ifBlank { f.toDate })} to " +
                    formatDateLike(f.toDate.ifBlank { f.fromDate })
            }
            if (f.datePreset.isNotBlank() && f.datePreset != "CUSTOM") {
                parts += "Range: ${humanizeCode(f.datePreset)}"
            }
            if (f.dateBasis.isNotBlank()) parts += "Based on: ${humanizeCode(f.dateBasis)}"
            if (f.status.isNotBlank()) parts += "Status: ${screen8StatusLabel(f.status)}"
            if (f.categoryId.isNotBlank()) parts += "Category: Selected"
            if (f.itemSearch.isNotBlank()) parts += "Item: ${f.itemSearch}"
            if (f.customerSearch.isNotBlank()) parts += "Customer: ${f.customerSearch}"
            if (f.staffUserId.isNotBlank()) parts += "Staff: Selected"
            if (f.search.isNotBlank()) parts += "Search: ${f.search}"
            if (f.customFilters.isNotEmpty()) parts += "Custom filters: ${f.customFilters.size}"
            if (f.grouping != "NONE") parts += "Group: ${humanizeCode(f.grouping)}"
            parts += "Sort: ${humanizeCode(f.sort)}"
            return parts
        }

        val preferredColumnWidths = tableColumns.map { column ->
            val headerWidth = smallBold.measureText(column.label) + 16f
            val sampleWidth = report.rows.take(40).maxOfOrNull { row ->
                normal.measureText(formatPdfValue(column, row[column.key]).take(64)) + 16f
            } ?: headerWidth
            maxOf(headerWidth, sampleWidth).coerceIn(48f, if (landscape) 150f else 170f)
        }
        val preferredTotal = preferredColumnWidths.sum().takeIf { it > 0f } ?: 1f
        val columnWidths = preferredColumnWidths.map { it / preferredTotal * availableWidth }
        val columnLefts = mutableListOf<Float>().apply {
            var left = margin
            columnWidths.forEach { width ->
                add(left)
                left += width
            }
        }

        fun finishCurrentPage() {
            val current = page ?: return
            val canvas = current.canvas
            val footerY = pageHeight - 20f
            canvas.drawLine(margin, footerY, pageWidth - margin, footerY, linePaint)
            canvas.drawText(
                truncate(report.title, muted, availableWidth * 0.65f),
                margin,
                pageHeight - 8f,
                muted
            )
            val pageText = "Page $pageNumber"
            canvas.drawText(
                pageText,
                pageWidth - margin - muted.measureText(pageText),
                pageHeight - 8f,
                muted
            )
            document.finishPage(current)
            page = null
        }

        fun drawTableHeader() {
            val current = page ?: return
            val canvas = current.canvas
            val headerLines = tableColumns.mapIndexed { index, column ->
                wrapText(column.label, smallBold, columnWidths[index] - 8f, maxLines = 2)
            }
            val maxHeaderLines = headerLines.maxOfOrNull { it.size } ?: 1
            val headerHeight = maxOf(24f, 8f + maxHeaderLines * smallBold.fontSpacing)

            canvas.drawRect(margin, y, pageWidth - margin, y + headerHeight, headerFill)
            tableColumns.forEachIndexed { index, _ ->
                val left = columnLefts[index]
                val lines = headerLines[index]
                var baseline = y + 6f - smallBold.fontMetrics.ascent
                lines.forEach { line ->
                    canvas.drawText(line, left + 4f, baseline, smallBold)
                    baseline += smallBold.fontSpacing
                }
                if (index > 0) canvas.drawLine(left, y, left, y + headerHeight, linePaint)
            }
            canvas.drawLine(margin, y + headerHeight, pageWidth - margin, y + headerHeight, linePaint)
            y += headerHeight
        }

        fun drawFirstPageHeader(canvas: android.graphics.Canvas) {
            val titleLines = wrapText(report.title, titlePaint, availableWidth - 62f, maxLines = 2)
            val headerHeight = if (titleLines.size > 1) 70f else 60f
            val headerRect = RectF(margin, y, pageWidth - margin, y + headerHeight)
            canvas.drawRoundRect(headerRect, 8f, 8f, brandFill)
            canvas.drawRoundRect(headerRect, 8f, 8f, cardBorderPaint)

            runCatching {
                context.getDrawable(R.drawable.ic_launcher)?.let { drawable ->
                    val iconSize = 34
                    val top = (y + 12f).toInt()
                    drawable.setBounds(
                        (margin + 10f).toInt(),
                        top,
                        (margin + 10f).toInt() + iconSize,
                        top + iconSize
                    )
                    drawable.draw(canvas)
                }
            }

            val textLeft = margin + 54f
            canvas.drawText(
                truncate(branding.shopName, shopPaint, availableWidth - 66f),
                textLeft,
                y + 21f,
                shopPaint
            )
            var titleBaseline = y + 40f
            titleLines.forEach { line ->
                canvas.drawText(line, textLeft, titleBaseline, titlePaint)
                titleBaseline += titlePaint.fontSpacing
            }
            y += headerHeight + 10f

            val generated = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.ENGLISH))
            val generatedText = "Generated: $generated"
            val byText = "Generated by: ${user.name} (${user.role.name})"
            canvas.drawText(
                truncate(generatedText, muted, availableWidth * 0.48f),
                margin,
                y,
                muted
            )
            val byRendered = truncate(byText, muted, availableWidth * 0.48f)
            canvas.drawText(
                byRendered,
                pageWidth - margin - muted.measureText(byRendered),
                y,
                muted
            )
            y += 15f

            val filterText = appliedFilterParts().joinToString(" | ").ifBlank { "No additional filters" }
            val filterLines = wrapText(filterText, small, availableWidth - 16f)
            val filterHeight = 30f + filterLines.size * small.fontSpacing
            val filterRect = RectF(margin, y, pageWidth - margin, y + filterHeight)
            canvas.drawRoundRect(filterRect, 7f, 7f, filterFill)
            canvas.drawRoundRect(filterRect, 7f, 7f, cardBorderPaint)
            canvas.drawText("Applied Filters", margin + 8f, y + 14f, smallBold)
            var filterBaseline = y + 27f
            filterLines.forEach { line ->
                canvas.drawText(line, margin + 8f, filterBaseline, small)
                filterBaseline += small.fontSpacing
            }
            y += filterHeight + 10f

            if (report.summary.isNotEmpty()) {
                canvas.drawText("Summary", margin, y + 10f, sectionPaint)
                y += 18f
                val summaryColumns = if (landscape) 4 else 3
                val gap = 6f
                val cellWidth = (availableWidth - gap * (summaryColumns - 1)) / summaryColumns
                val cellHeight = 44f

                report.summary.chunked(summaryColumns).forEach { summaryRow ->
                    summaryRow.forEachIndexed { index, item ->
                        val left = margin + index * (cellWidth + gap)
                        val rect = RectF(left, y, left + cellWidth, y + cellHeight)
                        canvas.drawRoundRect(rect, 7f, 7f, summaryFill)
                        canvas.drawRoundRect(rect, 7f, 7f, cardBorderPaint)

                        val labelLines = wrapText(item.label, kpiLabelPaint, cellWidth - 16f, maxLines = 2)
                        var labelBaseline = y + 11f
                        labelLines.forEach { line ->
                            canvas.drawText(line, left + 8f, labelBaseline, kpiLabelPaint)
                            labelBaseline += kpiLabelPaint.fontSpacing
                        }

                        val value = item.value.ifBlank { "-" }
                        val valueX = left + cellWidth - 8f - kpiPaint.measureText(value)
                        canvas.drawText(value, valueX, y + 36f, kpiPaint)
                    }
                    y += cellHeight + gap
                }
                y += 3f
            }

            canvas.drawText("Detailed Report", margin, y + 11f, sectionPaint)
            val countText = "${report.total} ${if (report.total == 1) "record" else "records"}"
            canvas.drawText(
                countText,
                pageWidth - margin - muted.measureText(countText),
                y + 11f,
                muted
            )
            y += 20f
        }

        fun drawContinuationHeader(canvas: android.graphics.Canvas) {
            val headerRect = RectF(margin, y, pageWidth - margin, y + 28f)
            canvas.drawRoundRect(headerRect, 6f, 6f, brandFill)
            canvas.drawRoundRect(headerRect, 6f, 6f, cardBorderPaint)
            val title = "${branding.shopName} - ${report.title}"
            canvas.drawText(
                truncate(title, bold, availableWidth - 16f),
                margin + 8f,
                y + 18f,
                bold
            )
            y += 36f
        }

        fun startPage(first: Boolean) {
            finishCurrentPage()
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(info)
            val canvas = page!!.canvas
            y = margin

            if (first) {
                drawFirstPageHeader(canvas)
            } else {
                drawContinuationHeader(canvas)
            }
            drawTableHeader()
        }

        startPage(first = true)
        report.rows.forEachIndexed { rowIndex, row ->
            val cellLines = tableColumns.mapIndexed { index, column ->
                wrapText(
                    formatPdfValue(column, row[column.key]),
                    normal,
                    columnWidths[index] - 8f,
                    maxLines = 3
                )
            }
            val maxLines = cellLines.maxOfOrNull { it.size } ?: 1
            val rowHeight = maxOf(22f, 8f + maxLines * normal.fontSpacing)

            if (y + rowHeight > pageHeight - footerSpace) {
                startPage(first = false)
            }

            val canvas = page!!.canvas
            if (rowIndex % 2 == 1) {
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, alternateRowFill)
            }

            tableColumns.forEachIndexed { index, _ ->
                val left = columnLefts[index]
                var baseline = y + 5f - normal.fontMetrics.ascent
                cellLines[index].forEach { line ->
                    canvas.drawText(line, left + 4f, baseline, normal)
                    baseline += normal.fontSpacing
                }
                if (index > 0) canvas.drawLine(left, y, left, y + rowHeight, linePaint)
            }
            canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, linePaint)
            y += rowHeight
        }
        finishCurrentPage()

        val dir = File(context.cacheDir, "report-pdfs").apply { mkdirs() }
        val stamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ENGLISH))
        val safeTitle = report.title.lowercase(Locale.ENGLISH)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "report" }
        val file = File(dir, "zhagmag-$safeTitle-$stamp.pdf")
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
        return file
    }

    fun view(context: Context, file: File) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "View PDF"))
    }

    fun share(context: Context, file: File, title: String) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    /**
     * Shares the finalized bill PDF directly to the customer's primary WhatsApp chat.
     * Uses the customer's normalized Indian 10-digit mobile and WhatsApp's direct-chat JID
     * so the Android share chooser is not shown.
     */
    fun shareDirectToCustomerWhatsApp(
        context: Context,
        file: File,
        customerMobile: String,
        title: String
    ) {
        val digits = customerMobile.filter(Char::isDigit).takeLast(10)
        require(digits.length == 10) { "Customer mobile number is invalid." }

        val uri = contentUri(context, file)
        val jid = "91$digits@s.whatsapp.net"
        val intent = Intent(Intent.ACTION_SEND)
            .setType(MIME)
            .setPackage("com.whatsapp")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, title)
            .putExtra("jid", "91$digits@s.whatsapp.net")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setClipData(android.content.ClipData.newRawUri("Quotation PDF", uri))

        check(intent.resolveActivity(context.packageManager) != null) {
            "WhatsApp is not installed."
        }
        context.startActivity(intent)
    }

    fun print(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled()
                    return
                }
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build(),
                    true
                )
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    return
                }
                runCatching {
                    file.inputStream().use { input ->
                        ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                            input.copyTo(output)
                        }
                    }
                }.onSuccess {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure { error ->
                    callback.onWriteFailed(error.message ?: "Unable to print PDF.")
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    fun download(context: Context, file: File): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, MIME)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zhagmag Dresses")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create download.")
            resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                ?: error("Unable to write download.")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved to Downloads/Zhagmag Dresses/${file.name}"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Zhagmag Dresses")
                .apply { mkdirs() }
            val target = File(dir, file.name)
            file.copyTo(target, overwrite = true)
            "Saved to ${target.absolutePath}"
        }
    }

    private fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
