package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.FeeType
import com.example.model.RentalTransaction
import com.example.model.StoreSettings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset

object ReceiptFormatter {

    private fun line(width: Int, char: Char = '-'): String = char.toString().repeat(width)

    private fun twoCol(left: String, right: String, width: Int): String {
        val r = right.trim()
        val maxLeft = (width - r.length - 1).coerceAtLeast(4)
        return if (left.length <= maxLeft) {
            val spaces = (width - left.length - r.length).coerceAtLeast(1)
            left + " ".repeat(spaces) + r
        } else {
            val padRight = (width - r.length).coerceAtLeast(0)
            "$left\n${" ".repeat(padRight)}$r"
        }
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    fun buildPlainText(tx: RentalTransaction, store: StoreSettings, width: Int = 32): String {
        val sb = StringBuilder()
        sb.appendLine(center(store.namaToko.uppercase(), width))
        if (store.alamat.isNotBlank()) sb.appendLine(center(store.alamat, width))
        if (store.noHp.isNotBlank()) sb.appendLine(center("HP: ${store.noHp}", width))
        sb.appendLine(line(width, '='))
        sb.appendLine(twoCol("No:", tx.nomorTransaksi, width))
        sb.appendLine(twoCol("Tanggal:", Formatters.formatTanggalIndo(tx.tanggalTransaksi), width))
        sb.appendLine(twoCol("Kasir:", tx.kasirName, width))
        sb.appendLine(twoCol("Status:", tx.statusTransaksi.label, width))
        sb.appendLine(line(width))
        sb.appendLine("Member: ${tx.customerName}")
        sb.appendLine("HP    : ${tx.customerPhone}")
        sb.appendLine("Sejak : ${Formatters.formatTanggalIndo(tx.memberSejak)} (Sewa ke-${tx.sewaKeN})")
        sb.appendLine("Pinjam: ${Formatters.formatTanggalIndo(tx.mulaiSewa)}")
        sb.appendLine("Kembali:${Formatters.formatTanggalIndo(tx.selesaiSewa)} (${tx.durasiHari}H)")
        sb.appendLine(line(width))

        for (item in tx.items) {
            sb.appendLine(item.productName)
            val skuPart = if (item.sku.isNotBlank()) "${item.variantName} (${item.sku})" else item.variantName
            sb.appendLine("  $skuPart")
            if (item.isPackage && item.packageSummary.isNotBlank()) {
                sb.appendLine("  Isi: ${item.packageSummary}")
            }
            val leftQty = "  ${item.qty}x ${Formatters.formatRupiah(item.hargaSatuan)} (${item.kolomHargaLabel})"
            sb.appendLine(twoCol(leftQty, Formatters.formatRupiah(item.subtotal), width))
        }

        sb.appendLine(line(width))
        sb.appendLine(twoCol("Subtotal Item", Formatters.formatRupiah(tx.subtotalItems), width))
        if (tx.diskonAktif && tx.nominalDiskon > 0) {
            val promoTag = if (tx.kodePromo.isNotBlank()) " (${tx.kodePromo})" else ""
            sb.appendLine(twoCol("Diskon$promoTag", "-${Formatters.formatRupiah(tx.nominalDiskon)}", width))
        }
        if (tx.biayaTambahan.isNotEmpty()) {
            for (fee in tx.biayaTambahan) {
                val label = if (fee.jenis == FeeType.GANTI_RUGI || fee.keterangan.isNotBlank()) {
                    "${fee.jenis.label}: ${fee.keterangan}"
                } else {
                    fee.jenis.label
                }
                sb.appendLine(twoCol(label, Formatters.formatRupiah(fee.nominal), width))
            }
        }
        sb.appendLine(line(width, '='))
        sb.appendLine(twoCol("TOTAL", Formatters.formatRupiah(tx.total), width))
        sb.appendLine(twoCol("Bayar (${tx.metodePembayaran.label})", Formatters.formatRupiah(tx.sudahDibayar), width))
        sb.appendLine(twoCol("Sisa", Formatters.formatRupiah(tx.sisa), width))
        sb.appendLine(twoCol("Pembayaran", tx.statusPembayaran.label, width))
        sb.appendLine(line(width))
        val jaminanText = if (tx.detailJaminan.isBlank()) tx.jenisJaminan.label else "${tx.jenisJaminan.label} - ${tx.detailJaminan}"
        sb.appendLine("Jaminan: $jaminanText")
        if (tx.catatan.isNotBlank()) {
            sb.appendLine("Catatan: ${tx.catatan}")
        }
        sb.appendLine(line(width))
        if (store.catatanStruk.isNotBlank()) {
            sb.appendLine(store.catatanStruk)
        }
        sb.appendLine(center("Terima Kasih & Selamat Berpetualang!", width))
        return sb.toString()
    }

    fun buildWhatsAppText(tx: RentalTransaction, store: StoreSettings): String {
        val sb = StringBuilder()
        sb.appendLine("*${store.namaToko.uppercase()}*")
        if (store.alamat.isNotBlank()) sb.appendLine(store.alamat)
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("*STRUK SEWA PERALATAN OUTDOOR*")
        sb.appendLine("No. Transaksi : *${tx.nomorTransaksi}*")
        sb.appendLine("Tanggal       : ${Formatters.formatTanggalIndo(tx.tanggalTransaksi)}")
        sb.appendLine("Kasir         : ${tx.kasirName}")
        sb.appendLine("Status Sewa   : ${tx.statusTransaksi.label}")
        sb.appendLine("")
        sb.appendLine("*Data Penyewa*")
        sb.appendLine("Nama          : ${tx.customerName}")
        sb.appendLine("No. HP        : ${tx.customerPhone}")
        sb.appendLine("Member Sejak  : ${Formatters.formatTanggalIndo(tx.memberSejak)} (Sewa ke-${tx.sewaKeN})")
        sb.appendLine("Periode Sewa  : ${Formatters.formatTanggalIndo(tx.mulaiSewa)} s/d ${Formatters.formatTanggalIndo(tx.selesaiSewa)} (${tx.durasiHari} Hari)")
        sb.appendLine("")
        sb.appendLine("*Rincian Sewa*")
        for (item in tx.items) {
            val skuTxt = if (item.sku.isNotBlank()) " [${item.sku}]" else ""
            sb.appendLine("• *${item.productName}* - ${item.variantName}$skuTxt")
            if (item.isPackage && item.packageSummary.isNotBlank()) {
                sb.appendLine("  Isi: ${item.packageSummary}")
            }
            sb.appendLine("  ${item.qty} x ${Formatters.formatRupiah(item.hargaSatuan)} (${item.kolomHargaLabel}) = *${Formatters.formatRupiah(item.subtotal)}*")
        }
        sb.appendLine("")
        sb.appendLine("Subtotal Item : ${Formatters.formatRupiah(tx.subtotalItems)}")
        if (tx.diskonAktif && tx.nominalDiskon > 0) {
            val promo = if (tx.kodePromo.isNotBlank()) " (${tx.kodePromo})" else ""
            sb.appendLine("Diskon$promo : -${Formatters.formatRupiah(tx.nominalDiskon)}")
        }
        if (tx.biayaTambahan.isNotEmpty()) {
            sb.appendLine("*Biaya Tambahan:*")
            for (fee in tx.biayaTambahan) {
                val detail = if (fee.keterangan.isNotBlank()) " (${fee.keterangan})" else ""
                sb.appendLine("- ${fee.jenis.label}$detail: ${Formatters.formatRupiah(fee.nominal)}")
            }
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("*TOTAL        : ${Formatters.formatRupiah(tx.total)}*")
        sb.appendLine("Sudah Dibayar : ${Formatters.formatRupiah(tx.sudahDibayar)} (${tx.metodePembayaran.label})")
        sb.appendLine("Sisa          : ${Formatters.formatRupiah(tx.sisa)}")
        sb.appendLine("Status Bayar  : *${tx.statusPembayaran.label}*")
        val jaminanWa = if (tx.detailJaminan.isBlank()) tx.jenisJaminan.label else "${tx.jenisJaminan.label} (${tx.detailJaminan})"
        sb.appendLine("Jaminan       : $jaminanWa")
        if (tx.catatan.isNotBlank()) {
            sb.appendLine("Catatan       : ${tx.catatan}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        if (store.catatanStruk.isNotBlank()) {
            sb.appendLine(store.catatanStruk)
        }
        return sb.toString()
    }

    fun buildEscPosBytes(tx: RentalTransaction, store: StoreSettings, paperWidthMm: Int = 58): ByteArray {
        val charsPerLine = if (paperWidthMm >= 80) 48 else 32
        val out = ByteArrayOutputStream()
        val charset = Charset.forName("UTF-8")

        fun cmd(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }
        fun text(s: String) {
            out.write(s.toByteArray(charset))
        }

        // ESC @ Initialize
        cmd(0x1B, 0x40)
        // Center + Bold + Double height for store name
        cmd(0x1B, 0x61, 0x01)
        cmd(0x1B, 0x45, 0x01)
        cmd(0x1D, 0x21, 0x01)
        text("${store.namaToko.uppercase()}\n")
        cmd(0x1D, 0x21, 0x00)
        cmd(0x1B, 0x45, 0x00)
        if (store.alamat.isNotBlank()) text("${store.alamat}\n")
        if (store.noHp.isNotBlank()) text("HP: ${store.noHp}\n")
        cmd(0x1B, 0x61, 0x00) // Left align

        val bodyText = buildPlainText(tx, store, charsPerLine)
        // Skip first 3 lines already printed in bold header
        val lines = bodyText.lines()
        val startIndex = lines.indexOfFirst { it.startsWith("=") }.coerceAtLeast(0)
        for (i in startIndex until lines.size) {
            val l = lines[i]
            if (l.startsWith("TOTAL")) {
                cmd(0x1B, 0x45, 0x01)
                text("$l\n")
                cmd(0x1B, 0x45, 0x00)
            } else {
                text("$l\n")
            }
        }
        text("\n\n")
        // Cut paper GS V 66 0
        cmd(0x1D, 0x56, 0x42, 0x00)
        return out.toByteArray()
    }

    fun createReceiptPngBitmap(tx: RentalTransaction, store: StoreSettings): Bitmap {
        val text = buildPlainText(tx, store, 38)
        val lines = text.lines()
        val width = 760
        val lineHeight = 32f
        val padding = 44f
        val height = (padding * 2 + lines.size * lineHeight + 40).toInt().coerceAtLeast(400)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val topBarPaint = Paint().apply { color = AndroidColor.parseColor("#571331") }
        canvas.drawRect(0f, 0f, width.toFloat(), 14f, topBarPaint)

        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E1419")
            textSize = 23f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldPaint = Paint(monoPaint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = AndroidColor.parseColor("#571331")
        }

        var y = padding + 24f
        lines.forEachIndexed { idx, line ->
            val paint = if (idx == 0 || line.startsWith("TOTAL")) boldPaint else monoPaint
            canvas.drawText(line, padding, y, paint)
            y += lineHeight
        }
        return bmp
    }

    fun saveReceiptPng(context: Context, tx: RentalTransaction, store: StoreSettings): Result<String> {
        return try {
            val bmp = createReceiptPngBitmap(tx, store)
            val safeNo = tx.nomorTransaksi.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val fileName = "Struk_${safeNo}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SewaOutdoor")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Gagal menyimpan gambar struk"))
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Struk PNG tersimpan di Pictures/SewaOutdoor/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SewaOutdoor")
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Struk PNG tersimpan di ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
