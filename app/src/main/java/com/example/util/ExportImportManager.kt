package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupPayload(
    val store: StoreSettings,
    val qris: QrisSettings,
    val drive: DriveSyncSettings,
    val employees: List<Employee>,
    val products: List<Product>,
    val packages: List<RentalPackage>,
    val members: List<Member>,
    val transactions: List<RentalTransaction>,
    val cashEntries: List<CashEntry>
)

object ExportImportManager {

    fun buildBackupJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", Formatters.todayIso())

        val storeObj = JSONObject().apply {
            put("namaToko", payload.store.namaToko)
            put("alamat", payload.store.alamat)
            put("noHp", payload.store.noHp)
            put("catatanStruk", payload.store.catatanStruk)
            put("adminPin", payload.store.adminPin)
            put("kasirPin", payload.store.kasirPin)
            put("printerWidthMm", payload.store.printerWidthMm)
            val payArr = JSONArray()
            payload.store.activePaymentMethods.forEach { payArr.put(it) }
            put("activePaymentMethods", payArr)
        }
        root.put("storeSettings", storeObj)

        val qrisObj = JSONObject().apply {
            put("namaMerchant", payload.qris.namaMerchant)
            put("nmid", payload.qris.nmid)
            put("rekeningInfo", payload.qris.rekeningInfo)
            put("payloadStatis", payload.qris.payloadStatis)
        }
        root.put("qrisSettings", qrisObj)

        val driveObj = JSONObject().apply {
            put("webAppUrl", payload.drive.webAppUrl)
            put("autoSync", payload.drive.autoSync)
        }
        root.put("driveSettings", driveObj)

        val empArr = JSONArray()
        payload.employees.forEach { emp ->
            empArr.put(JSONObject().apply {
                put("id", emp.id)
                put("nama", emp.nama)
                put("role", emp.role.name)
                put("pin", emp.pin)
                put("noHp", emp.noHp)
                put("aktif", emp.aktif)
            })
        }
        root.put("employees", empArr)

        val prodArr = JSONArray()
        payload.products.forEach { prod ->
            val vArr = JSONArray()
            prod.variants.forEach { v ->
                vArr.put(JSONObject().apply {
                    put("id", v.id)
                    put("productId", v.productId)
                    put("namaVarian", v.namaVarian)
                    put("sku", v.sku)
                    put("warnaNama", v.warnaNama)
                    put("warnaHex", v.warnaHex)
                    put("ukuran", v.ukuran)
                    put("stok", v.stok)
                    put("harga1H", v.harga1H)
                    put("harga2H", v.harga2H)
                    put("harga3H", v.harga3H)
                    put("harga4H", v.harga4H)
                    put("harga5H", v.harga5H)
                    put("harga6H", v.harga6H)
                    put("status", v.status.name)
                })
            }
            val tArr = JSONArray()
            prod.customTiers.forEach { t ->
                tArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("durasi", t.durasi)
                    put("satuan", t.satuan.name)
                    put("harga", t.harga)
                })
            }
            prodArr.put(JSONObject().apply {
                put("id", prod.id)
                put("nama", prod.nama)
                put("kategori", prod.kategori)
                put("deskripsi", prod.deskripsi)
                put("photoPath", prod.photoPath)
                put("isDisewa", prod.isDisewa)
                put("isDijual", prod.isDijual)
                put("satuan", prod.satuan.name)
                put("gunakanStok", prod.gunakanStok)
                put("customTiers", tArr)
                put("variants", vArr)
            })
        }
        root.put("products", prodArr)

        val pkgArr = JSONArray()
        payload.packages.forEach { pkg ->
            val pItems = JSONArray()
            pkg.items.forEach { item ->
                pItems.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("variantId", item.variantId)
                    put("variantName", item.variantName)
                    put("sku", item.sku)
                    put("qty", item.qty)
                })
            }
            pkgArr.put(JSONObject().apply {
                put("id", pkg.id)
                put("nama", pkg.nama)
                put("deskripsi", pkg.deskripsi)
                put("harga1H", pkg.harga1H)
                put("harga2H", pkg.harga2H)
                put("harga3H", pkg.harga3H)
                put("harga4H", pkg.harga4H)
                put("harga5H", pkg.harga5H)
                put("harga6H", pkg.harga6H)
                put("aktif", pkg.aktif)
                put("items", pItems)
            })
        }
        root.put("packages", pkgArr)

        val memArr = JSONArray()
        payload.members.forEach { m ->
            memArr.put(JSONObject().apply {
                put("id", m.id)
                put("nama", m.nama)
                put("noHp", m.noHp)
                put("memberSejak", m.memberSejak)
                put("totalSewa", m.totalSewa)
                put("totalBelanja", m.totalBelanja)
                put("transaksiTerakhir", m.transaksiTerakhir)
            })
        }
        root.put("members", memArr)

        val txArr = JSONArray()
        payload.transactions.forEach { tx ->
            val itemsArr = JSONArray()
            tx.items.forEach { item ->
                itemsArr.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("variantId", item.variantId)
                    put("variantName", item.variantName)
                    put("sku", item.sku)
                    put("warnaHex", item.warnaHex)
                    put("qty", item.qty)
                    put("hargaSatuan", item.hargaSatuan)
                    put("kolomHargaLabel", item.kolomHargaLabel)
                    put("subtotal", item.subtotal)
                    put("isPackage", item.isPackage)
                    put("packageSummary", item.packageSummary)
                })
            }
            val feesArr = JSONArray()
            tx.biayaTambahan.forEach { f ->
                feesArr.put(JSONObject().apply {
                    put("id", f.id)
                    put("jenis", f.jenis.name)
                    put("keterangan", f.keterangan)
                    put("nominal", f.nominal)
                })
            }
            txArr.put(JSONObject().apply {
                put("id", tx.id)
                put("nomorTransaksi", tx.nomorTransaksi)
                put("tanggalTransaksi", tx.tanggalTransaksi)
                put("mulaiSewa", tx.mulaiSewa)
                put("selesaiSewa", tx.selesaiSewa)
                put("durasiHari", tx.durasiHari)
                put("satuan", tx.satuan.name)
                put("customerName", tx.customerName)
                put("customerPhone", tx.customerPhone)
                put("memberId", tx.memberId)
                put("memberSejak", tx.memberSejak)
                put("sewaKeN", tx.sewaKeN)
                put("kasirName", tx.kasirName)
                put("items", itemsArr)
                put("biayaTambahan", feesArr)
                put("diskonAktif", tx.diskonAktif)
                put("diskonTipe", tx.diskonTipe.name)
                put("diskonNilai", tx.diskonNilai)
                put("kodePromo", tx.kodePromo)
                put("subtotalItems", tx.subtotalItems)
                put("nominalDiskon", tx.nominalDiskon)
                put("totalBiayaTambahan", tx.totalBiayaTambahan)
                put("total", tx.total)
                put("statusPembayaran", tx.statusPembayaran.name)
                put("sudahDibayar", tx.sudahDibayar)
                put("sisa", tx.sisa)
                put("metodePembayaran", tx.metodePembayaran.name)
                put("statusTransaksi", tx.statusTransaksi.name)
                put("jenisJaminan", tx.jenisJaminan.name)
                put("detailJaminan", tx.detailJaminan)
                put("catatan", tx.catatan)
                put("createdAtMillis", tx.createdAtMillis)
            })
        }
        root.put("transactions", txArr)

        val cashArr = JSONArray()
        payload.cashEntries.forEach { c ->
            cashArr.put(JSONObject().apply {
                put("id", c.id)
                put("tanggal", c.tanggal)
                put("tipe", c.tipe.name)
                put("kategori", c.kategori)
                put("deskripsi", c.deskripsi)
                put("nominal", c.nominal)
                put("metode", c.metode.name)
                put("createdAtMillis", c.createdAtMillis)
            })
        }
        root.put("cashEntries", cashArr)

        return root.toString(2)
    }

    fun parseBackupJson(rawJson: String): Result<BackupPayload> {
        return try {
            val root = JSONObject(rawJson)
            val storeObj = root.optJSONObject("storeSettings") ?: JSONObject()
            val payArr = storeObj.optJSONArray("activePaymentMethods")
            val activeMethods = if (payArr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until payArr.length()) list.add(payArr.getString(i))
                list
            } else listOf("CASH", "QRIS", "TRANSFER")

            val store = StoreSettings(
                namaToko = storeObj.optString("namaToko", "Sewa Outdoor Nusantara"),
                alamat = storeObj.optString("alamat", ""),
                noHp = storeObj.optString("noHp", ""),
                catatanStruk = storeObj.optString("catatanStruk", ""),
                adminPin = storeObj.optString("adminPin", "1234"),
                kasirPin = storeObj.optString("kasirPin", "0000"),
                printerWidthMm = storeObj.optInt("printerWidthMm", 58),
                activePaymentMethods = activeMethods
            )
            val qrisObj = root.optJSONObject("qrisSettings") ?: JSONObject()
            val qris = QrisSettings(
                namaMerchant = qrisObj.optString("namaMerchant", "SEWA OUTDOOR"),
                nmid = qrisObj.optString("nmid", ""),
                rekeningInfo = qrisObj.optString("rekeningInfo", ""),
                payloadStatis = qrisObj.optString("payloadStatis", "")
            )
            val driveObj = root.optJSONObject("driveSettings") ?: JSONObject()
            val drive = DriveSyncSettings(
                webAppUrl = driveObj.optString("webAppUrl", ""),
                autoSync = driveObj.optBoolean("autoSync", false)
            )

            val employees = mutableListOf<Employee>()
            val empArr = root.optJSONArray("employees") ?: JSONArray()
            for (i in 0 until empArr.length()) {
                val o = empArr.getJSONObject(i)
                employees.add(
                    Employee(
                        id = o.getString("id"),
                        nama = o.getString("nama"),
                        role = runCatching { UserRole.valueOf(o.getString("role")) }.getOrDefault(UserRole.KASIR),
                        pin = o.getString("pin"),
                        noHp = o.optString("noHp", ""),
                        aktif = o.optBoolean("aktif", true)
                    )
                )
            }

            val products = mutableListOf<Product>()
            val prodArr = root.optJSONArray("products") ?: JSONArray()
            for (i in 0 until prodArr.length()) {
                val o = prodArr.getJSONObject(i)
                val pid = o.getString("id")
                val vArr = o.optJSONArray("variants") ?: JSONArray()
                val variants = mutableListOf<ProductVariant>()
                for (j in 0 until vArr.length()) {
                    val vo = vArr.getJSONObject(j)
                    variants.add(
                        ProductVariant(
                            id = vo.getString("id"),
                            productId = pid,
                            namaVarian = vo.getString("namaVarian"),
                            sku = vo.getString("sku"),
                            warnaNama = vo.getString("warnaNama"),
                            warnaHex = vo.optString("warnaHex", "#571331"),
                            ukuran = vo.optString("ukuran", "Standar"),
                            stok = vo.optInt("stok", 0),
                            harga1H = vo.optLong("harga1H", 0L),
                            harga2H = vo.optLong("harga2H", 0L),
                            harga3H = vo.optLong("harga3H", 0L),
                            harga4H = vo.optLong("harga4H", 0L),
                            harga5H = vo.optLong("harga5H", 0L),
                            harga6H = vo.optLong("harga6H", 0L),
                            status = runCatching { VariantStatus.valueOf(vo.optString("status")) }.getOrDefault(VariantStatus.TERSEDIA)
                        )
                    )
                }
                val tArr = o.optJSONArray("customTiers") ?: JSONArray()
                val tiers = mutableListOf<ProductPriceTier>()
                for (j in 0 until tArr.length()) {
                    val to = tArr.getJSONObject(j)
                    tiers.add(
                        ProductPriceTier(
                            id = to.optString("id", "tier_$j"),
                            durasi = to.optInt("durasi", 2),
                            satuan = runCatching { ProductPriceUnit.valueOf(to.optString("satuan")) }.getOrDefault(ProductPriceUnit.HARI),
                            harga = to.optLong("harga", 0L)
                        )
                    )
                }
                products.add(
                    Product(
                        id = pid,
                        nama = o.getString("nama"),
                        kategori = o.optString("kategori", "Lainnya"),
                        deskripsi = o.optString("deskripsi", ""),
                        variants = variants,
                        photoPath = o.optString("photoPath", ""),
                        isDisewa = o.optBoolean("isDisewa", true),
                        isDijual = o.optBoolean("isDijual", false),
                        satuan = runCatching { ProductPriceUnit.valueOf(o.optString("satuan")) }.getOrDefault(ProductPriceUnit.HARI),
                        gunakanStok = o.optBoolean("gunakanStok", true),
                        customTiers = tiers,
                        hargaJual = o.optLong("hargaJual", 0L),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }

            val packages = mutableListOf<RentalPackage>()
            val pkgArr = root.optJSONArray("packages") ?: JSONArray()
            for (i in 0 until pkgArr.length()) {
                val o = pkgArr.getJSONObject(i)
                val pItemsArr = o.optJSONArray("items") ?: JSONArray()
                val pItems = mutableListOf<PackageItem>()
                for (j in 0 until pItemsArr.length()) {
                    val po = pItemsArr.getJSONObject(j)
                    pItems.add(
                        PackageItem(
                            productId = po.getString("productId"),
                            productName = po.getString("productName"),
                            variantId = po.getString("variantId"),
                            variantName = po.getString("variantName"),
                            sku = po.optString("sku", ""),
                            qty = po.optInt("qty", 1)
                        )
                    )
                }
                packages.add(
                    RentalPackage(
                        id = o.getString("id"),
                        nama = o.getString("nama"),
                        deskripsi = o.optString("deskripsi", ""),
                        harga1H = o.optLong("harga1H", 0L),
                        harga2H = o.optLong("harga2H", 0L),
                        harga3H = o.optLong("harga3H", 0L),
                        harga4H = o.optLong("harga4H", 0L),
                        harga5H = o.optLong("harga5H", 0L),
                        harga6H = o.optLong("harga6H", 0L),
                        items = pItems,
                        aktif = o.optBoolean("aktif", true)
                    )
                )
            }

            val members = mutableListOf<Member>()
            val memArr = root.optJSONArray("members") ?: JSONArray()
            for (i in 0 until memArr.length()) {
                val o = memArr.getJSONObject(i)
                members.add(
                    Member(
                        id = o.getString("id"),
                        nama = o.getString("nama"),
                        noHp = o.getString("noHp"),
                        memberSejak = o.optString("memberSejak", Formatters.todayIso()),
                        totalSewa = o.optInt("totalSewa", 1),
                        totalBelanja = o.optLong("totalBelanja", 0L),
                        transaksiTerakhir = o.optString("transaksiTerakhir", Formatters.todayIso())
                    )
                )
            }

            val transactions = mutableListOf<RentalTransaction>()
            val txArr = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                val items = parseTransactionItemsJson(o.optJSONArray("items")?.toString() ?: "[]")
                val fees = parseAdditionalFeesJson(o.optJSONArray("biayaTambahan")?.toString() ?: "[]")
                transactions.add(
                    RentalTransaction(
                        id = o.getString("id"),
                        nomorTransaksi = o.getString("nomorTransaksi"),
                        tanggalTransaksi = o.getString("tanggalTransaksi"),
                        mulaiSewa = o.getString("mulaiSewa"),
                        selesaiSewa = o.getString("selesaiSewa"),
                        durasiHari = o.optInt("durasiHari", 1),
                        satuan = runCatching { RentalUnit.valueOf(o.optString("satuan")) }.getOrDefault(RentalUnit.HARI),
                        customerName = o.getString("customerName"),
                        customerPhone = o.getString("customerPhone"),
                        memberId = o.optString("memberId", ""),
                        memberSejak = o.optString("memberSejak", o.getString("tanggalTransaksi")),
                        sewaKeN = o.optInt("sewaKeN", 1),
                        kasirName = o.optString("kasirName", "Admin"),
                        items = items,
                        biayaTambahan = fees,
                        diskonAktif = o.optBoolean("diskonAktif", false),
                        diskonTipe = runCatching { DiscountType.valueOf(o.optString("diskonTipe")) }.getOrDefault(DiscountType.RUPIAH),
                        diskonNilai = o.optLong("diskonNilai", 0L),
                        kodePromo = o.optString("kodePromo", ""),
                        subtotalItems = o.optLong("subtotalItems", 0L),
                        nominalDiskon = o.optLong("nominalDiskon", 0L),
                        totalBiayaTambahan = o.optLong("totalBiayaTambahan", 0L),
                        total = o.optLong("total", 0L),
                        statusPembayaran = runCatching { PaymentStatus.valueOf(o.optString("statusPembayaran")) }.getOrDefault(PaymentStatus.LUNAS),
                        sudahDibayar = o.optLong("sudahDibayar", 0L),
                        sisa = o.optLong("sisa", 0L),
                        metodePembayaran = runCatching { PaymentMethod.valueOf(o.optString("metodePembayaran")) }.getOrDefault(PaymentMethod.CASH),
                        statusTransaksi = runCatching { RentalStatus.valueOf(o.optString("statusTransaksi")) }.getOrDefault(RentalStatus.AKTIF),
                        jenisJaminan = runCatching { CollateralType.valueOf(o.optString("jenisJaminan")) }.getOrDefault(CollateralType.KTP),
                        detailJaminan = o.optString("detailJaminan", ""),
                        catatan = o.optString("catatan", ""),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            val cashEntries = mutableListOf<CashEntry>()
            val cashArr = root.optJSONArray("cashEntries") ?: JSONArray()
            for (i in 0 until cashArr.length()) {
                val o = cashArr.getJSONObject(i)
                cashEntries.add(
                    CashEntry(
                        id = o.getString("id"),
                        tanggal = o.getString("tanggal"),
                        tipe = runCatching { CashEntryType.valueOf(o.optString("tipe")) }.getOrDefault(CashEntryType.PENGELUARAN),
                        kategori = o.optString("kategori", "Operasional"),
                        deskripsi = o.optString("deskripsi", ""),
                        nominal = o.optLong("nominal", 0L),
                        metode = runCatching { PaymentMethod.valueOf(o.optString("metode")) }.getOrDefault(PaymentMethod.CASH),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            Result.success(
                BackupPayload(
                    store = store,
                    qris = qris,
                    drive = drive,
                    employees = employees,
                    products = products,
                    packages = packages,
                    members = members,
                    transactions = transactions,
                    cashEntries = cashEntries
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("File JSON tidak valid: ${e.localizedMessage}"))
        }
    }

    fun transactionItemsToJson(items: List<TransactionItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("variantId", item.variantId)
                put("variantName", item.variantName)
                put("sku", item.sku)
                put("warnaHex", item.warnaHex)
                put("qty", item.qty)
                put("hargaSatuan", item.hargaSatuan)
                put("kolomHargaLabel", item.kolomHargaLabel)
                put("subtotal", item.subtotal)
                put("isPackage", item.isPackage)
                put("packageSummary", item.packageSummary)
            })
        }
        return arr.toString()
    }

    fun parseTransactionItemsJson(json: String): List<TransactionItem> {
        val arr = JSONArray(json)
        val list = mutableListOf<TransactionItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                TransactionItem(
                    productId = o.optString("productId"),
                    productName = o.optString("productName"),
                    variantId = o.optString("variantId"),
                    variantName = o.optString("variantName"),
                    sku = o.optString("sku"),
                    warnaHex = o.optString("warnaHex", "#571331"),
                    qty = o.optInt("qty", 1),
                    hargaSatuan = o.optLong("hargaSatuan", 0L),
                    kolomHargaLabel = o.optString("kolomHargaLabel", "Tarif 1H"),
                    subtotal = o.optLong("subtotal", 0L),
                    isPackage = o.optBoolean("isPackage", false),
                    packageSummary = o.optString("packageSummary", "")
                )
            )
        }
        return list
    }

    fun additionalFeesToJson(fees: List<AdditionalFee>): String {
        val arr = JSONArray()
        fees.forEach { f ->
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("jenis", f.jenis.name)
                put("keterangan", f.keterangan)
                put("nominal", f.nominal)
            })
        }
        return arr.toString()
    }

    fun parseAdditionalFeesJson(json: String): List<AdditionalFee> {
        val arr = JSONArray(json)
        val list = mutableListOf<AdditionalFee>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                AdditionalFee(
                    id = o.optString("id"),
                    jenis = runCatching { FeeType.valueOf(o.optString("jenis")) }.getOrDefault(FeeType.LAINNYA),
                    keterangan = o.optString("keterangan"),
                    nominal = o.optLong("nominal", 0L)
                )
            )
        }
        return list
    }

    fun packageItemsToJson(items: List<PackageItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("variantId", item.variantId)
                put("variantName", item.variantName)
                put("sku", item.sku)
                put("qty", item.qty)
            })
        }
        return arr.toString()
    }

    fun parsePackageItemsJson(json: String): List<PackageItem> {
        val arr = JSONArray(json)
        val list = mutableListOf<PackageItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                PackageItem(
                    productId = o.optString("productId"),
                    productName = o.optString("productName"),
                    variantId = o.optString("variantId"),
                    variantName = o.optString("variantName"),
                    sku = o.optString("sku"),
                    qty = o.optInt("qty", 1)
                )
            )
        }
        return list
    }

    fun productTiersToJson(tiers: List<ProductPriceTier>): String {
        val arr = JSONArray()
        tiers.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("durasi", t.durasi)
                put("satuan", t.satuan.name)
                put("harga", t.harga)
            })
        }
        return arr.toString()
    }

    fun parseProductTiersJson(json: String): List<ProductPriceTier> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        val list = mutableListOf<ProductPriceTier>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ProductPriceTier(
                    id = o.optString("id", "tier_$i"),
                    durasi = o.optInt("durasi", 2),
                    satuan = runCatching { ProductPriceUnit.valueOf(o.optString("satuan")) }.getOrDefault(ProductPriceUnit.HARI),
                    harga = o.optLong("harga", 0L)
                )
            )
        }
        return list
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun buildProductsCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.appendLine("Produk,Kategori,SKU,Varian,Warna,Hex,Ukuran,Stok,Status,1H,2H,3H,4H,5H,6H")
        products.forEach { p ->
            p.variants.forEach { v ->
                sb.appendLine(
                    listOf(
                        csvEscape(p.nama),
                        csvEscape(p.kategori),
                        csvEscape(v.sku),
                        csvEscape(v.namaVarian),
                        csvEscape(v.warnaNama),
                        csvEscape(v.warnaHex),
                        csvEscape(v.ukuran),
                        v.stok.toString(),
                        csvEscape(v.status.label),
                        v.harga1H.toString(),
                        v.harga2H.toString(),
                        v.harga3H.toString(),
                        v.harga4H.toString(),
                        v.harga5H.toString(),
                        v.harga6H.toString()
                    ).joinToString(",")
                )
            }
        }
        return sb.toString()
    }

    fun buildMembersCsv(members: List<Member>): String {
        val sb = StringBuilder()
        sb.appendLine("Nama,NoHP,MemberSejak,TotalSewa,TotalBelanja,TransaksiTerakhir")
        members.forEach { m ->
            sb.appendLine(
                listOf(
                    csvEscape(m.nama),
                    csvEscape(m.noHp),
                    csvEscape(m.memberSejak),
                    m.totalSewa.toString(),
                    m.totalBelanja.toString(),
                    csvEscape(m.transaksiTerakhir)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildTransactionsCsv(transactions: List<RentalTransaction>): String {
        val sb = StringBuilder()
        sb.appendLine("NoTransaksi,Tanggal,MulaiSewa,SelesaiSewa,DurasiHari,Pelanggan,NoHP,Kasir,Item,Subtotal,Diskon,BiayaTambahan,Total,Dibayar,Sisa,Metode,StatusBayar,StatusSewa,Jaminan")
        transactions.forEach { t ->
            val itemsSummary = t.items.joinToString(" | ") { "${it.productName}(${it.sku}) x${it.qty}" }
            sb.appendLine(
                listOf(
                    csvEscape(t.nomorTransaksi),
                    csvEscape(t.tanggalTransaksi),
                    csvEscape(t.mulaiSewa),
                    csvEscape(t.selesaiSewa),
                    t.durasiHari.toString(),
                    csvEscape(t.customerName),
                    csvEscape(t.customerPhone),
                    csvEscape(t.kasirName),
                    csvEscape(itemsSummary),
                    t.subtotalItems.toString(),
                    t.nominalDiskon.toString(),
                    t.totalBiayaTambahan.toString(),
                    t.total.toString(),
                    t.sudahDibayar.toString(),
                    t.sisa.toString(),
                    csvEscape(t.metodePembayaran.label),
                    csvEscape(t.statusPembayaran.label),
                    csvEscape(t.statusTransaksi.label),
                    csvEscape("${t.jenisJaminan.label}: ${t.detailJaminan}")
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun saveTextFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String
    ): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SewaOutdoor")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Gagal membuat file di Downloads"))
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                Result.success("Tersimpan di Downloads/SewaOutdoor/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SewaOutdoor")
                dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(content, Charsets.UTF_8)
                Result.success("Tersimpan di ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareTextFile(context: Context, fileName: String, mimeType: String, content: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan $fileName"))
    }
}
