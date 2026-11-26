package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.model.Product
import com.example.model.ProductPriceUnit
import com.example.model.ProductVariant
import com.example.model.RentalPackage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

object Formatters {
    private val indoLocale = Locale("id", "ID")
    private val rupiahFormat = NumberFormat.getNumberInstance(indoLocale)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", indoLocale)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indoLocale)
    private val fullStampFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    fun formatTimestamp(millis: Long): String {
        return if (millis <= 0L) "-" else dateTimeFormat.format(Date(millis))
    }

    fun formatRupiah(amount: Long): String {
        val prefix = if (amount < 0) "-Rp " else "Rp "
        val absVal = kotlin.math.abs(amount)
        return "$prefix${rupiahFormat.format(absVal)}"
    }

    fun formatRupiahCompact(amount: Long): String {
        val prefix = if (amount < 0) "-Rp" else "Rp"
        val absVal = kotlin.math.abs(amount)
        return "$prefix${rupiahFormat.format(absVal)}"
    }

    fun formatNumber(amount: Long): String = rupiahFormat.format(amount)

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.startsWith("0062") -> "0" + digits.drop(4)
            digits.startsWith("62") -> "0" + digits.drop(2)
            digits.startsWith("8") -> "0$digits"
            else -> digits
        }
    }

    fun toWhatsAppNumber(phone: String): String {
        val normalized = normalizePhone(phone)
        return if (normalized.startsWith("0")) {
            "62" + normalized.drop(1)
        } else {
            normalized
        }
    }

    fun todayIso(): String = isoFormat.format(Date())

    fun nowTimestampLabel(): String = SimpleDateFormat("dd MMM yyyy HH:mm", indoLocale).format(Date())

    fun fileTimestamp(): String = fullStampFormat.format(Date())

    fun millisToIso(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return isoFormat.format(cal.time)
    }

    fun isoToMillis(iso: String): Long {
        return try {
            isoFormat.parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatTanggalIndo(isoDate: String): String {
        return try {
            val d = isoFormat.parse(isoDate)
            if (d != null) displayDateFormat.format(d) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun addDaysIso(startIso: String, daysToAdd: Int): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoToMillis(startIso)
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return isoFormat.format(cal.time)
    }

    fun daysBetweenInclusive(startIso: String, endIso: String): Int {
        val startMs = isoToMillis(startIso)
        val endMs = isoToMillis(endIso)
        val diffDays = TimeUnit.MILLISECONDS.toDays(endMs - startMs).toInt()
        return (diffDays + 1).coerceAtLeast(1)
    }

    fun calculateAutoPrices(harga1H: Long): List<Long> {
        val h1 = harga1H.coerceAtLeast(0L)
        return listOf(
            h1,
            (h1 * 1.8).roundToLong(),
            (h1 * 2.5).roundToLong(),
            (h1 * 3.1).roundToLong(),
            (h1 * 3.7).roundToLong(),
            (h1 * 4.2).roundToLong()
        )
    }

    fun calculateVariantPriceForDuration(variant: ProductVariant, durasiHari: Int): Pair<Long, String> {
        val d = durasiHari.coerceAtLeast(1)
        return when (d) {
            1 -> variant.harga1H to "Tarif 1H"
            2 -> variant.harga2H to "Tarif 2H"
            3 -> variant.harga3H to "Tarif 3H"
            4 -> variant.harga4H to "Tarif 4H"
            5 -> variant.harga5H to "Tarif 5H"
            6 -> variant.harga6H to "Tarif 6H"
            else -> {
                val extraDays = d - 6
                val price = variant.harga6H + extraDays * variant.harga1H
                price to "6H + ${extraDays}x1H"
            }
        }
    }

    fun calculateProductVariantPrice(product: Product, variant: ProductVariant, durasiHari: Int): Pair<Long, String> {
        val d = durasiHari.coerceAtLeast(1)
        val exactTier = product.customTiers.firstOrNull { it.satuan == ProductPriceUnit.HARI && it.durasi == d }
        if (exactTier != null && exactTier.harga > 0L) {
            return exactTier.harga to "Paket ${d}H"
        }
        return calculateVariantPriceForDuration(variant, d)
    }

    fun calculatePackagePriceForDuration(pkg: RentalPackage, durasiHari: Int): Pair<Long, String> {
        val d = durasiHari.coerceAtLeast(1)
        return when (d) {
            1 -> pkg.harga1H to "Paket 1H"
            2 -> pkg.harga2H to "Paket 2H"
            3 -> pkg.harga3H to "Paket 3H"
            4 -> pkg.harga4H to "Paket 4H"
            5 -> pkg.harga5H to "Paket 5H"
            6 -> pkg.harga6H to "Paket 6H"
            else -> {
                val extraDays = d - 6
                val price = pkg.harga6H + extraDays * pkg.harga1H
                price to "Paket 6H + ${extraDays}x1H"
            }
        }
    }

    fun parseColorHex(hex: String, fallback: Color = Color(0xFF571331)): Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            val parsed = when (clean.length) {
                6 -> ("FF$clean").toLong(16)
                8 -> clean.toLong(16)
                else -> return fallback
            }
            Color(parsed)
        } catch (_: Exception) {
            fallback
        }
    }
}
