package com.suvojeetsengupta.suvform.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.suvojeetsengupta.suvform.data.remote.CalculationDto
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResponseExport {

    private val tsFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileTsFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

    // Editorial "paper" palette (matches the app + public form).
    private const val PAPER = 0xFFF4F1EA.toInt()
    private const val CARD = 0xFFFFFFFF.toInt()
    private const val INK = 0xFF0F0F10.toInt()
    private const val MUTED = 0xFF6E6B62.toInt()
    private const val LINE = 0xFFDDD6C7.toInt()
    private const val ACCENT = 0xFFE94221.toInt()

    private fun slug(s: String): String =
        s.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "form" }.take(40)

    // ---------------------------------------------------------------------
    // CSV (opens cleanly in Excel / Sheets)
    // ---------------------------------------------------------------------

    /**
     * Build CSV bytes: a UTF-8 BOM so Excel renders Unicode correctly, CRLF line
     * endings, calculated columns appended after the answer columns, and
     * formula-injection neutralization on every cell.
     */
    fun toCsv(
        fields: List<FieldDto>,
        calculations: List<CalculationDto>,
        responses: List<ResponseItemDto>,
    ): ByteArray {
        val sb = StringBuilder()

        val header = buildList {
            add("Submitted at")
            addAll(fields.map { it.label })
            addAll(calculations.map { it.label })
        }
        sb.append(header.joinToString(",") { csvEscape(it) }).append("\r\n")

        for (r in responses) {
            val row = buildList {
                add(tsFormat.format(Date(r.submittedAt)))
                addAll(fields.map { flattenAnswer(r.answers[it.id]) })
                addAll(calculations.map { c -> r.calculated[c.id]?.let { formatCalc(it, c.format) } ?: "" })
            }
            sb.append(row.joinToString(",") { csvEscape(it) }).append("\r\n")
        }

        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        return bom + sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun writeCsv(
        context: Context,
        formTitle: String,
        fields: List<FieldDto>,
        calculations: List<CalculationDto>,
        responses: List<ResponseItemDto>,
    ): File {
        val file = File(context.cacheDir, "SuvForm_${slug(formTitle)}_${fileTsFormat.format(Date())}.csv")
        file.writeBytes(toCsv(fields, calculations, responses))
        return file
    }

    // ---------------------------------------------------------------------
    // PDF (editorial theme, robust line-based pagination)
    // ---------------------------------------------------------------------

    private const val PAGE_W = 595 // A4 portrait @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val FOOTER_RESERVE = 34f

    fun writePdf(
        context: Context,
        formTitle: String,
        fields: List<FieldDto>,
        calculations: List<CalculationDto>,
        responses: List<ResponseItemDto>,
    ): File {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            isAntiAlias = true; color = INK; textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val metaPaint = Paint().apply { isAntiAlias = true; color = MUTED; textSize = 10f }
        val respLabelPaint = Paint().apply {
            isAntiAlias = true; color = ACCENT; textSize = 9f; letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val sectionPaint = Paint().apply {
            isAntiAlias = true; color = MUTED; textSize = 8.5f; letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val fieldLabelPaint = Paint().apply { isAntiAlias = true; color = MUTED; textSize = 9f; typeface = Typeface.DEFAULT_BOLD }
        val valuePaint = Paint().apply { isAntiAlias = true; color = INK; textSize = 12f }
        val calcLabelPaint = Paint().apply { isAntiAlias = true; color = INK; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val calcValPaint = Paint().apply { isAntiAlias = true; color = ACCENT; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
        val hairlinePaint = Paint().apply { color = LINE; strokeWidth = 1f }
        val accentRulePaint = Paint().apply { color = ACCENT; strokeWidth = 3f }
        val footerPaint = Paint().apply { isAntiAlias = true; color = MUTED; textSize = 8f }
        val bgPaint = Paint().apply { color = PAPER }

        val contentW = PAGE_W - MARGIN * 2

        // Mutable page state, driven by local helpers (captures vars).
        var pageNum = 1
        var page = doc.startPage(pageInfo(pageNum))
        var canvas = page.canvas
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), bgPaint)
        var y = MARGIN

        fun footerAndFinish() {
            canvas.drawText("$formTitle  ·  Page $pageNum", MARGIN, PAGE_H - 20f, footerPaint)
            doc.finishPage(page)
        }
        fun newPage() {
            footerAndFinish()
            pageNum++
            page = doc.startPage(pageInfo(pageNum))
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), bgPaint)
            y = MARGIN
        }
        fun ensure(needed: Float) {
            if (y + needed > PAGE_H - MARGIN - FOOTER_RESERVE) newPage()
        }

        // ---- Header (first page) ----
        canvas.drawText(formTitle, MARGIN, y + 18f, titlePaint)
        y += 26f
        canvas.drawLine(MARGIN, y, MARGIN + 56f, y, accentRulePaint)
        y += 16f
        canvas.drawText(
            "${responses.size} response${if (responses.size == 1) "" else "s"}  ·  exported ${tsFormat.format(Date())}",
            MARGIN, y, metaPaint,
        )
        y += 22f

        if (responses.isEmpty()) {
            canvas.drawText("No responses yet.", MARGIN, y + 20f, valuePaint)
            footerAndFinish()
            return writeDoc(context, doc, formTitle)
        }

        for ((idx, r) in responses.withIndex()) {
            // Response header
            ensure(28f)
            canvas.drawText("RESPONSE ${idx + 1}", MARGIN, y, respLabelPaint)
            canvas.drawText(tsFormat.format(Date(r.submittedAt)), MARGIN + 90f, y, metaPaint)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, hairlinePaint)
            y += 16f

            // Answers
            for (f in fields) {
                ensure(14f)
                canvas.drawText(f.label.uppercase(Locale.getDefault()), MARGIN, y, fieldLabelPaint)
                y += 14f
                val raw = flattenAnswer(r.answers[f.id]).ifBlank { "—" }
                for (line in wrapText(raw, valuePaint, contentW)) {
                    ensure(15f)
                    canvas.drawText(line, MARGIN, y, valuePaint)
                    y += 15f
                }
                y += 8f
            }

            // Calculations
            val calcRows = calculations.mapNotNull { c -> r.calculated[c.id]?.let { c to it } }
            if (calcRows.isNotEmpty()) {
                ensure(18f)
                canvas.drawText("CALCULATIONS", MARGIN, y, sectionPaint)
                y += 16f
                for ((c, v) in calcRows) {
                    ensure(16f)
                    canvas.drawText(c.label, MARGIN, y, calcLabelPaint)
                    val valText = formatCalc(v, c.format)
                    canvas.drawText(valText, PAGE_W - MARGIN - calcValPaint.measureText(valText), y, calcValPaint)
                    y += 16f
                }
            }

            // Separator between responses
            y += 10f
            if (idx < responses.lastIndex) {
                ensure(16f)
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, hairlinePaint)
                y += 18f
            }
        }

        footerAndFinish()
        return writeDoc(context, doc, formTitle)
    }

    private fun pageInfo(num: Int) =
        PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, num).create()

    private fun writeDoc(context: Context, doc: PdfDocument, formTitle: String): File {
        val file = File(context.cacheDir, "SuvForm_${slug(formTitle)}_${fileTsFormat.format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ---------------------------------------------------------------------
    // Sharing
    // ---------------------------------------------------------------------

    /** Share the file via ACTION_SEND with the given MIME type. */
    fun share(context: Context, file: File, mime: String, subject: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun csvEscape(value: String): String {
        var v = value
        // Neutralize CSV/Excel formula injection (cells starting with = + - @ tab CR).
        if (v.isNotEmpty() && v.first() in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            v = "'" + v
        }
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    private fun flattenAnswer(v: JsonElement?): String = when (v) {
        null, is JsonNull -> ""
        is JsonPrimitive -> v.content
        is JsonArray -> v.jsonArray.joinToString("; ") {
            runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString())
        }
        else -> v.toString()
    }

    private fun formatCalc(value: Double, format: String?): String = when (format) {
        "currency" -> String.format(Locale.US, "%,.2f", value)
        "percent" -> String.format(Locale.US, "%.1f%%", value)
        else ->
            if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString()
            else String.format(Locale.US, "%.2f", value)
    }

    /**
     * Word-wrap to [maxWidth], honoring explicit newlines and hard-splitting any
     * single token that is itself wider than the line. Never truncates.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            if (paragraph.isEmpty()) { lines.add(""); continue }
            var current = StringBuilder()
            for (word in paragraph.split(' ')) {
                var w = word
                // Hard-split tokens longer than the line width.
                while (paint.measureText(w) > maxWidth && w.length > 1) {
                    var cut = 1
                    while (cut < w.length && paint.measureText(w.substring(0, cut + 1)) <= maxWidth) cut++
                    if (current.isNotEmpty()) { lines.add(current.toString()); current = StringBuilder() }
                    lines.add(w.substring(0, cut))
                    w = w.substring(cut)
                }
                val candidate = if (current.isEmpty()) w else "$current $w"
                if (paint.measureText(candidate) <= maxWidth) {
                    current.clear(); current.append(candidate)
                } else {
                    if (current.isNotEmpty()) lines.add(current.toString())
                    current = StringBuilder(w)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
        }
        return lines.ifEmpty { listOf("") }
    }
}
