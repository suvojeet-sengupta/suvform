package com.suvojeetsengupta.suvform.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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

    private fun slug(s: String): String = s.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)

    /** Build CSV bytes from the response list, with field order from the schema. */
    fun toCsv(fields: List<FieldDto>, responses: List<ResponseItemDto>): ByteArray {
        val sb = StringBuilder()
        sb.append("Submitted at")
        for (f in fields) { sb.append(','); sb.append(csvEscape(f.label)) }
        sb.append('\n')
        for (r in responses) {
            sb.append(csvEscape(tsFormat.format(Date(r.submittedAt))))
            for (f in fields) {
                sb.append(',')
                sb.append(csvEscape(flattenAnswer(r.answers[f.id])))
            }
            sb.append('\n')
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** Build a multi-page PDF document. Returns the file written into cache. */
    fun writePdf(context: Context, formTitle: String, fields: List<FieldDto>, responses: List<ResponseItemDto>): File {
        val doc = PdfDocument()
        val pageWidth = 595  // A4 portrait @ 72dpi
        val pageHeight = 842
        val margin = 36
        val titlePaint = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val subPaint = Paint().apply { textSize = 11f; color = 0xFF6B7280.toInt() }
        val labelPaint = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF374151.toInt() }
        val valuePaint = Paint().apply { textSize = 12f; color = 0xFF111827.toInt() }
        val dividerPaint = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = 1f }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        // Header
        canvas.drawText(formTitle, margin.toFloat(), (margin + 16).toFloat(), titlePaint)
        canvas.drawText(
            "${responses.size} response${if (responses.size == 1) "" else "s"} • exported ${tsFormat.format(Date())}",
            margin.toFloat(), (margin + 34).toFloat(), subPaint,
        )
        var y = margin + 60f

        for ((idx, r) in responses.withIndex()) {
            // Estimated height of this response block
            val blockHeight = 36f + fields.size * 26f
            if (y + blockHeight > pageHeight - margin) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin.toFloat()
            }
            // Response header line
            canvas.drawText(
                "Response ${idx + 1} — ${tsFormat.format(Date(r.submittedAt))}",
                margin.toFloat(), y, labelPaint,
            )
            y += 14f
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, dividerPaint)
            y += 10f
            for (f in fields) {
                canvas.drawText(f.label + ":", margin.toFloat(), y, labelPaint)
                val raw = flattenAnswer(r.answers[f.id]).ifBlank { "—" }
                // Wrap long values
                val maxW = (pageWidth - margin * 2 - 110).toFloat()
                val lines = wrapText(raw, valuePaint, maxW)
                for ((li, line) in lines.withIndex()) {
                    canvas.drawText(line, (margin + 110).toFloat(), y + (li * 14f), valuePaint)
                }
                y += 14f * maxOf(1, lines.size) + 4f
                if (y > pageHeight - margin) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin.toFloat()
                }
            }
            y += 14f
        }

        doc.finishPage(page)

        val file = File(context.cacheDir, "SuvForm_${slug(formTitle)}_${fileTsFormat.format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun writeCsv(context: Context, formTitle: String, fields: List<FieldDto>, responses: List<ResponseItemDto>): File {
        val file = File(context.cacheDir, "SuvForm_${slug(formTitle)}_${fileTsFormat.format(Date())}.csv")
        file.writeBytes(toCsv(fields, responses))
        return file
    }

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

    // ---- helpers ----

    private fun csvEscape(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun flattenAnswer(v: JsonElement?): String = when (v) {
        null -> ""
        is JsonPrimitive -> runCatching { v.content }.getOrDefault(v.toString())
        is JsonArray -> v.jsonArray.joinToString("; ") {
            runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString())
        }
        else -> v.toString()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth) {
                current.clear(); current.append(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return if (lines.isEmpty()) listOf(text) else lines.take(6) // cap lines for safety
    }
}
