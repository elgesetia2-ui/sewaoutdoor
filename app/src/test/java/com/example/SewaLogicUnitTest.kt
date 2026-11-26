package com.example

import com.example.data.SeedData
import com.example.model.ProductVariant
import com.example.model.VariantStatus
import com.example.util.ExportImportManager
import com.example.util.BackupPayload
import com.example.util.Formatters
import com.example.util.QrisGenerator
import org.junit.Assert.*
import org.junit.Test

class SewaLogicUnitTest {

    @Test
    fun testPhoneNormalization() {
        assertEquals("081234567890", Formatters.normalizePhone("+62 812-3456-7890"))
        assertEquals("081234567890", Formatters.normalizePhone("6281234567890"))
        assertEquals("081234567890", Formatters.normalizePhone("081234567890"))
        assertEquals("6281234567890", Formatters.toWhatsAppNumber("081234567890"))
    }

    @Test
    fun testDurationInclusive() {
        assertEquals(1, Formatters.daysBetweenInclusive("2026-11-26", "2026-11-26"))
        assertEquals(3, Formatters.daysBetweenInclusive("2026-11-26", "2026-11-28"))
        assertEquals("2026-11-28", Formatters.addDaysIso("2026-11-26", 2))
    }

    @Test
    fun testSixColumnAutoPricingAndExtraDays() {
        val auto = Formatters.calculateAutoPrices(50000L)
        assertEquals(listOf(50000L, 90000L, 125000L, 155000L, 185000L, 210000L), auto)

        val variant = ProductVariant(
            id = "v1", productId = "p1", namaVarian = "Test", sku = "TST-MRN",
            warnaNama = "Maroon", warnaHex = "#571331", ukuran = "Standar", stok = 5,
            harga1H = auto[0], harga2H = auto[1], harga3H = auto[2],
            harga4H = auto[3], harga5H = auto[4], harga6H = auto[5],
            status = VariantStatus.TERSEDIA
        )
        assertEquals(125000L to "Tarif 3H", Formatters.calculateVariantPriceForDuration(variant, 3))
        assertEquals(210000L to "Tarif 6H", Formatters.calculateVariantPriceForDuration(variant, 6))
        // 8 hari = 6H + (8-6)*1H = 210000 + 2*50000 = 310000
        assertEquals(310000L to "6H + 2x1H", Formatters.calculateVariantPriceForDuration(variant, 8))
    }

    @Test
    fun testQrisDynamicPayload() {
        val settings = SeedData.initialSettings()
        val dynamic = QrisGenerator.buildDynamicPayload(settings.qris.payloadStatis, 150000L)
        assertTrue(dynamic.contains("5406150000"))
        assertTrue(dynamic.contains("010212"))
        assertTrue(dynamic.dropLast(4).endsWith("6304"))
    }

    @Test
    fun testSeedProductsCountAndReceiptFormatting() {
        val products = SeedData.initialProducts()
        assertEquals(12, products.size)
        assertEquals(2, SeedData.initialPackages().size)

        val settings = SeedData.initialSettings()
        val firstProd = products.first()
        val firstVar = firstProd.variants.first()
        val tx = com.example.model.RentalTransaction(
            id = "tx1",
            nomorTransaksi = "TRX-20261126-001",
            tanggalTransaksi = "2026-11-26",
            mulaiSewa = "2026-11-26",
            selesaiSewa = "2026-11-28",
            durasiHari = 3,
            satuan = com.example.model.RentalUnit.HARI,
            customerName = "Andi",
            customerPhone = "081234567890",
            memberId = "m1",
            memberSejak = "2026-11-26",
            sewaKeN = 1,
            kasirName = "Rina (Kasir)",
            items = listOf(
                com.example.model.TransactionItem(
                    productId = firstProd.id,
                    productName = firstProd.nama,
                    variantId = firstVar.id,
                    variantName = firstVar.namaVarian,
                    sku = firstVar.sku,
                    warnaHex = firstVar.warnaHex,
                    qty = 1,
                    hargaSatuan = firstVar.harga3H,
                    kolomHargaLabel = "Tarif 3H",
                    subtotal = firstVar.harga3H
                )
            ),
            biayaTambahan = listOf(
                com.example.model.AdditionalFee("f1", com.example.model.FeeType.GANTI_RUGI, "Tenda robek", 25000L)
            ),
            diskonAktif = false,
            diskonTipe = com.example.model.DiscountType.RUPIAH,
            diskonNilai = 0L,
            kodePromo = "",
            subtotalItems = firstVar.harga3H,
            nominalDiskon = 0L,
            totalBiayaTambahan = 25000L,
            total = firstVar.harga3H + 25000L,
            statusPembayaran = com.example.model.PaymentStatus.LUNAS,
            sudahDibayar = firstVar.harga3H + 25000L,
            sisa = 0L,
            metodePembayaran = com.example.model.PaymentMethod.QRIS,
            statusTransaksi = com.example.model.RentalStatus.AKTIF,
            jenisJaminan = com.example.model.CollateralType.KTP,
            detailJaminan = "3273010101010001",
            catatan = "",
            createdAtMillis = 1L
        )
        val receipt = com.example.util.ReceiptFormatter.buildPlainText(tx, settings.store, 32)
        assertTrue(receipt.contains("TRX-20261126-001"))
        assertTrue(receipt.contains("Ganti Rugi: Tenda robek"))
        assertTrue(receipt.contains("Sewa ke-1"))
    }
}
