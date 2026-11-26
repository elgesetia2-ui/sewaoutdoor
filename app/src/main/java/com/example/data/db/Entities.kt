package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val nama: String,
    val kategori: String,
    val deskripsi: String,
    val photoPath: String = "",
    val isDisewa: Boolean = true,
    val isDijual: Boolean = false,
    val satuan: String = "HARI",
    val gunakanStok: Boolean = true,
    val customTiersJson: String = "[]",
    val hargaJual: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey val id: String,
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
    val status: String
)

data class ProductWithVariants(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val variants: List<ProductVariantEntity>
)

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String,
    val nama: String,
    val deskripsi: String,
    val harga1H: Long,
    val harga2H: Long,
    val harga3H: Long,
    val harga4H: Long,
    val harga5H: Long,
    val harga6H: Long,
    val itemsJson: String,
    val aktif: Boolean
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val nomorTransaksi: String,
    val tanggalTransaksi: String,
    val mulaiSewa: String,
    val selesaiSewa: String,
    val durasiHari: Int,
    val satuan: String,
    val customerName: String,
    val customerPhone: String,
    val memberId: String,
    val memberSejak: String,
    val sewaKeN: Int,
    val kasirName: String,
    val itemsJson: String,
    val biayaTambahanJson: String,
    val diskonAktif: Boolean,
    val diskonTipe: String,
    val diskonNilai: Long,
    val kodePromo: String,
    val subtotalItems: Long,
    val nominalDiskon: Long,
    val totalBiayaTambahan: Long,
    val total: Long,
    val statusPembayaran: String,
    val sudahDibayar: Long,
    val sisa: Long,
    val metodePembayaran: String,
    val statusTransaksi: String,
    val jenisJaminan: String,
    val detailJaminan: String,
    val catatan: String,
    val createdAtMillis: Long
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val nama: String,
    val noHp: String,
    val memberSejak: String,
    val totalSewa: Int,
    val totalBelanja: Long,
    val transaksiTerakhir: String
)

@Entity(tableName = "cash_entries")
data class CashEntryEntity(
    @PrimaryKey val id: String,
    val tanggal: String,
    val tipe: String,
    val kategori: String,
    val deskripsi: String,
    val nominal: Long,
    val metode: String,
    val createdAtMillis: Long
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val nama: String,
    val role: String,
    val pin: String,
    val noHp: String,
    val aktif: Boolean
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val namaToko: String,
    val alamat: String,
    val noHp: String,
    val catatanStruk: String,
    val adminPin: String,
    val kasirPin: String,
    val printerWidthMm: Int,
    val namaMerchant: String,
    val nmid: String,
    val rekeningInfo: String,
    val payloadStatis: String,
    val driveWebAppUrl: String,
    val driveAutoSync: Boolean,
    val lastSyncTime: String,
    val lastSyncStatus: String,
    val activePaymentMethodsJson: String = "[\"CASH\",\"QRIS\",\"TRANSFER\"]"
)
