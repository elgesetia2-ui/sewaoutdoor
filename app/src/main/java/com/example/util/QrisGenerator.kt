package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.QrisSettings
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object QrisGenerator {

    /**
     * Injects transaction amount (EMVCo tag 54) into a static QRIS payload and recalculates CRC16-CCITT (tag 63).
     * Falls back cleanly if the static payload does not follow EMVCo format.
     */
    fun buildDynamicPayload(staticPayload: String, amount: Long): String {
        val raw = staticPayload.trim()
        if (raw.isEmpty()) return "QRIS|AMOUNT=$amount"
        if (amount <= 0L) return raw

        return try {
            val crcMarkerIndex = raw.lastIndexOf("6304")
            val bodyWithoutCrc = if (crcMarkerIndex >= 0 && crcMarkerIndex == raw.length - 8) {
                raw.substring(0, crcMarkerIndex)
            } else {
                raw
            }

            // Parse EMVCo TLV tags
            val tags = mutableListOf<Pair<String, String>>()
            var idx = 0
            while (idx + 4 <= bodyWithoutCrc.length) {
                val tag = bodyWithoutCrc.substring(idx, idx + 2)
                val len = bodyWithoutCrc.substring(idx + 2, idx + 4).toIntOrNull() ?: break
                val start = idx + 4
                val end = start + len
                if (end > bodyWithoutCrc.length) break
                val value = bodyWithoutCrc.substring(start, end)
                tags.add(tag to value)
                idx = end
            }

            if (idx != bodyWithoutCrc.length || tags.isEmpty()) {
                return raw
            }

            val updated = StringBuilder()
            var hasTag54 = false
            val amountStr = amount.toString()

            for ((tag, value) in tags) {
                when (tag) {
                    "01" -> {
                        // Point of Initiation: 11 static -> 12 dynamic
                        val newVal = if (value == "11") "12" else value
                        updated.append("01").append(newVal.length.toString().padStart(2, '0')).append(newVal)
                    }
                    "54" -> {
                        hasTag54 = true
                        updated.append("54").append(amountStr.length.toString().padStart(2, '0')).append(amountStr)
                    }
                    "58" -> {
                        if (!hasTag54) {
                            hasTag54 = true
                            updated.append("54").append(amountStr.length.toString().padStart(2, '0')).append(amountStr)
                        }
                        updated.append(tag).append(value.length.toString().padStart(2, '0')).append(value)
                    }
                    else -> {
                        updated.append(tag).append(value.length.toString().padStart(2, '0')).append(value)
                    }
                }
            }

            if (!hasTag54) {
                updated.append("54").append(amountStr.length.toString().padStart(2, '0')).append(amountStr)
            }

            updated.append("6304")
            val crc = crc16Ccitt(updated.toString())
            updated.append(crc)
            updated.toString()
        } catch (_: Exception) {
            raw
        }
    }

    private fun crc16Ccitt(input: String): String {
        var crc = 0xFFFF
        for (ch in input) {
            crc = crc xor (ch.code shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }

    fun generateQrBitmap(content: String, sizePx: Int = 640): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = writer.encode(content.ifBlank { "QRIS" }, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bmp
    }

    fun createQrPosterBitmap(qris: QrisSettings, amount: Long, refLabel: String): Bitmap {
        val width = 900
        val height = 1220
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val maroonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#571331")
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FCE8F0")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E1419")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#66565E")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#571331")
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Top Maroon Header
        canvas.drawRect(0f, 0f, width.toFloat(), 190f, maroonPaint)
        canvas.drawText("PEMBAYARAN QRIS", width / 2f, 82f, titlePaint)
        canvas.drawText(qris.namaMerchant.ifBlank { "Sewa Outdoor" }, width / 2f, 134f, subHeaderPaint)

        // NMID
        canvas.drawText("NMID: ${qris.nmid.ifBlank { "-" }}", width / 2f, 245f, bodyPaint)

        // QR Code Frame
        val payload = buildDynamicPayload(qris.payloadStatis, amount)
        val qrBmp = generateQrBitmap(payload, 580)
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E5D8DE")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val left = (width - 620) / 2f
        val top = 280f
        canvas.drawRoundRect(RectF(left, top, left + 620f, top + 620f), 28f, 28f, framePaint)
        canvas.drawBitmap(qrBmp, left + 20f, top + 20f, null)

        // Total & Reference
        if (amount > 0L) {
            canvas.drawText("Total Bayar:", width / 2f, 965f, bodyPaint)
            canvas.drawText(Formatters.formatRupiah(amount), width / 2f, 1025f, amountPaint)
        } else {
            canvas.drawText("QRIS Statis Merchant", width / 2f, 990f, bodyBoldPaint)
        }

        if (refLabel.isNotBlank()) {
            canvas.drawText(refLabel, width / 2f, 1085f, bodyPaint)
        }
        canvas.drawText("Scan dengan GoPay, OVO, DANA, ShopeePay, atau Mobile Banking", width / 2f, 1155f, bodyPaint)
        return bmp
    }

    fun saveQrPosterPng(context: Context, qris: QrisSettings, amount: Long, refLabel: String): Result<String> {
        return try {
            val bmp = createQrPosterBitmap(qris, amount, refLabel)
            val fileName = "QRIS_${Formatters.fileTimestamp()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SewaOutdoor")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Gagal membuat file gambar di Galeri"))
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Tersimpan di Pictures/SewaOutdoor/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SewaOutdoor")
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Tersimpan di ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
