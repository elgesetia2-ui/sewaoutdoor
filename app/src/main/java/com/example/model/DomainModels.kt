package com.example.model

enum class UserRole(val label: String) {
    ADMIN("Admin"),
    KASIR("Kasir")
}

data class SessionState(
    val isLoggedIn: Boolean = false,
    val role: UserRole = UserRole.KASIR,
    val userName: String = "",
    val employeeId: String = ""
)

enum class VariantStatus(val label: String) {
    TERSEDIA("Tersedia"),
    PERAWATAN("Perawatan"),
    NONAKTIF("Nonaktif")
}

data class ProductVariant(
    val id: String,
    val productId: String,
    val namaVarian: String,
    val sku: String,
    val warnaNama: String,
    val warnaHex: String,
    val ukuran: String,
    val stok: Int,
    val harga1H: Long,
    val harga2H: Long,
    val harga3H: Long,
    val harga4H: Long,
    val harga5H: Long,
    val harga6H: Long,
    val status: VariantStatus = VariantStatus.TERSEDIA
)

enum class ProductPriceUnit(val label: String) {
    MENIT("Menit"),
    JAM("Jam"),
    HARI("Hari"),
    PEKAN("Pekan"),
    BULAN("Bulan")
}

data class ProductPriceTier(
    val id: String,
    val durasi: Int,
    val satuan: ProductPriceUnit,
    val harga: Long
)

data class Product(
    val id: String,
    val nama: String,
    val kategori: String,
    val deskripsi: String,
    val variants: List<ProductVariant>,
    val photoPath: String = "",
    val isDisewa: Boolean = true,
    val isDijual: Boolean = false,
    val satuan: ProductPriceUnit = ProductPriceUnit.HARI,
    val gunakanStok: Boolean = true,
    val customTiers: List<ProductPriceTier> = emptyList(),
    val hargaJual: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class PackageItem(
    val productId: String,
    val productName: String,
    val variantId: String,
    val variantName: String,
    val sku: String,
    val qty: Int
)

data class RentalPackage(
    val id: String,
    val nama: String,
    val deskripsi: String,
    val harga1H: Long,
    val harga2H: Long,
    val harga3H: Long,
    val harga4H: Long,
    val harga5H: Long,
    val harga6H: Long,
    val items: List<PackageItem>,
    val aktif: Boolean = true
)

data class TransactionItem(
    val productId: String,
    val productName: String,
    val variantId: String,
    val variantName: String,
    val sku: String,
    val warnaHex: String,
    val qty: Int,
    val hargaSatuan: Long,
    val kolomHargaLabel: String,
    val subtotal: Long,
    val isPackage: Boolean = false,
    val packageSummary: String = ""
)

enum class FeeType(val label: String, val requiresDetail: Boolean = false) {
    GANTI_RUGI("Ganti Rugi", requiresDetail = true),
    DENDA("Denda"),
    LAINNYA("Lainnya")
}

data class AdditionalFee(
    val id: String,
    val jenis: FeeType,
    val keterangan: String,
    val nominal: Long
)

enum class PaymentStatus(val label: String) {
    TERHUTANG("Terhutang"),
    SEBAGIAN("Sebagian"),
    LUNAS("Lunas")
}

enum class PaymentMethod(val label: String) {
    CASH("Tunai"),
    GOPAY("GoPay"),
    DANA("Dana"),
    SHOPEEPAY("ShopeePay"),
    QRIS("QRIS"),
    LINKAJA("LinkAja"),
    OVO("OVO"),
    TRANSFER("Transfer"),
    KARTU("Kartu")
}

enum class RentalStatus(val label: String) {
    BOOKING("Booking"),
    AKTIF("Aktif"),
    SELESAI("Selesai")
}

enum class CollateralType(val label: String) {
    KTP("KTP"),
    SIM("SIM"),
    STNK("STNK"),
    MOTOR("Motor"),
    UANG("Uang")
}

enum class DiscountType(val label: String) {
    RUPIAH("Rp"),
    PERSEN("%")
}

enum class RentalUnit(val label: String, val daysPerUnit: Int) {
    HARI("Hari", 1),
    PEKAN("Pekan", 7),
    BULAN("Bulan", 30)
}

data class RentalTransaction(
    val id: String,
    val nomorTransaksi: String,
    val tanggalTransaksi: String,
    val mulaiSewa: String,
    val selesaiSewa: String,
    val durasiHari: Int,
    val satuan: RentalUnit,
    val customerName: String,
    val customerPhone: String,
    val memberId: String,
    val memberSejak: String,
    val sewaKeN: Int,
    val kasirName: String,
    val items: List<TransactionItem>,
    val biayaTambahan: List<AdditionalFee>,
    val diskonAktif: Boolean,
    val diskonTipe: DiscountType,
    val diskonNilai: Long,
    val kodePromo: String,
    val subtotalItems: Long,
    val nominalDiskon: Long,
    val totalBiayaTambahan: Long,
    val total: Long,
    val statusPembayaran: PaymentStatus,
    val sudahDibayar: Long,
    val sisa: Long,
    val metodePembayaran: PaymentMethod,
    val statusTransaksi: RentalStatus,
    val jenisJaminan: CollateralType,
    val detailJaminan: String,
    val catatan: String,
    val createdAtMillis: Long
)

data class Member(
    val id: String,
    val nama: String,
    val noHp: String,
    val memberSejak: String,
    val totalSewa: Int,
    val totalBelanja: Long,
    val transaksiTerakhir: String
)

enum class CashEntryType(val label: String) {
    PEMASUKAN_LAIN("Pemasukan Lain"),
    PENGELUARAN("Pengeluaran")
}

data class CashEntry(
    val id: String,
    val tanggal: String,
    val tipe: CashEntryType,
    val kategori: String,
    val deskripsi: String,
    val nominal: Long,
    val metode: PaymentMethod,
    val createdAtMillis: Long
)

data class CashMutation(
    val id: String,
    val tanggal: String,
    val sumber: String,
    val deskripsi: String,
    val isMasuk: Boolean,
    val nominal: Long,
    val metode: PaymentMethod,
    val timestampMillis: Long
)

data class Employee(
    val id: String,
    val nama: String,
    val role: UserRole,
    val pin: String,
    val noHp: String,
    val aktif: Boolean = true
)

data class StoreSettings(
    val namaToko: String = "Sewa Outdoor Nusantara",
    val alamat: String = "Jl. Pendaki Raya No. 45, Bandung",
    val noHp: String = "081234567890",
    val catatanStruk: String = "Harap periksa kelengkapan & kondisi alat sebelum meninggalkan toko. Terima kasih!",
    val adminPin: String = "1234",
    val kasirPin: String = "0000",
    val printerWidthMm: Int = 58,
    val activePaymentMethods: List<String> = listOf("CASH", "QRIS", "TRANSFER")
) {
    fun activePaymentMethodEnums(): List<PaymentMethod> {
        val list = activePaymentMethods.mapNotNull { key ->
            runCatching { PaymentMethod.valueOf(key) }.getOrNull()
        }
        return if (list.isNotEmpty()) list else listOf(PaymentMethod.CASH, PaymentMethod.QRIS, PaymentMethod.TRANSFER)
    }
}

data class QrisSettings(
    val namaMerchant: String = "SEWA OUTDOOR NUSANTARA",
    val nmid: String = "ID1024009876543",
    val rekeningInfo: String = "BCA 1234567890 a.n. Sewa Outdoor",
    val payloadStatis: String = "00020101021126590014ID.CO.QRIS.WWW01189360001400001234560215ID10240098765430303UMI51440014ID.CO.QRIS.WWW0215ID10240098765430303UMI5204594153033605802ID5922SEWA OUTDOOR NUSANTARA6007BANDUNG6105401156304A1B2"
)

data class DriveSyncSettings(
    val webAppUrl: String = "",
    val autoSync: Boolean = false,
    val lastSyncTime: String = "-",
    val lastSyncStatus: String = "Belum terhubung"
)

data class AppSettingsBundle(
    val store: StoreSettings = StoreSettings(),
    val qris: QrisSettings = QrisSettings(),
    val drive: DriveSyncSettings = DriveSyncSettings()
)
