package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.SeedData
import com.example.data.db.*
import com.example.model.*
import com.example.util.AppsScriptSyncClient
import com.example.util.BackupPayload
import com.example.util.ExportImportManager
import com.example.util.Formatters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.UUID

private val Context.sessionDataStore by preferencesDataStore(name = "sewa_session")

class SewaRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val packageDao = db.packageDao()
    private val transactionDao = db.transactionDao()
    private val memberDao = db.memberDao()
    private val cashEntryDao = db.cashEntryDao()
    private val employeeDao = db.employeeDao()
    private val settingsDao = db.settingsDao()

    private object SessionKeys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val ROLE = stringPreferencesKey("role")
        val USER_NAME = stringPreferencesKey("user_name")
        val EMPLOYEE_ID = stringPreferencesKey("employee_id")
    }

    val sessionFlow: Flow<SessionState> = context.sessionDataStore.data.map { prefs ->
        val loggedIn = prefs[SessionKeys.IS_LOGGED_IN] ?: false
        val role = runCatching {
            UserRole.valueOf(prefs[SessionKeys.ROLE] ?: UserRole.KASIR.name)
        }.getOrDefault(UserRole.KASIR)
        SessionState(
            isLoggedIn = loggedIn,
            role = role,
            userName = prefs[SessionKeys.USER_NAME] ?: "",
            employeeId = prefs[SessionKeys.EMPLOYEE_ID] ?: ""
        )
    }

    val productsFlow: Flow<List<Product>> = productDao.observeProductsWithVariants().map { list ->
        list.map { it.toDomain() }
    }

    val packagesFlow: Flow<List<RentalPackage>> = packageDao.observePackages().map { list ->
        list.map { it.toDomain() }
    }

    val transactionsFlow: Flow<List<RentalTransaction>> = transactionDao.observeTransactions().map { list ->
        list.map { it.toDomain() }
    }

    val membersFlow: Flow<List<Member>> = memberDao.observeMembers().map { list ->
        list.map { it.toDomain() }
    }

    val cashEntriesFlow: Flow<List<CashEntry>> = cashEntryDao.observeCashEntries().map { list ->
        list.map { it.toDomain() }
    }

    val employeesFlow: Flow<List<Employee>> = employeeDao.observeEmployees().map { list ->
        list.map { it.toDomain() }
    }

    val settingsFlow: Flow<AppSettingsBundle> = settingsDao.observeSettings().map { entity ->
        entity?.toDomain() ?: SeedData.initialSettings()
    }

    suspend fun ensureSeeded() {
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsertSettings(SeedData.initialSettings().toEntity())
        }
        if (employeeDao.getEmployeesOnce().isEmpty()) {
            employeeDao.insertEmployees(SeedData.initialEmployees().map { it.toEntity() })
        }
        if (productDao.countProducts() == 0) {
            val seededProducts = SeedData.initialProducts()
            productDao.insertProducts(seededProducts.map { it.toEntity() })
            productDao.insertVariants(seededProducts.flatMap { p -> p.variants.map { it.toEntity() } })
            packageDao.insertPackages(SeedData.initialPackages().map { it.toEntity() })
        }
    }

    suspend fun login(role: UserRole, inputName: String, inputPin: String): Result<SessionState> {
        val cleanName = inputName.trim()
        val cleanPin = inputPin.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama wajib diisi"))
        }
        if (cleanPin.isBlank()) {
            return Result.failure(IllegalArgumentException("PIN wajib diisi"))
        }
        val settings = settingsDao.getSettingsOnce()?.toDomain() ?: SeedData.initialSettings()
        val employees = employeeDao.getEmployeesOnce().map { it.toDomain() }
        val matchingEmp = employees.firstOrNull {
            it.aktif && it.role == role && it.nama.equals(cleanName, ignoreCase = true)
        }
        val defaultRolePin = if (role == UserRole.ADMIN) settings.store.adminPin else settings.store.kasirPin

        val pinValid = when {
            matchingEmp != null -> cleanPin == matchingEmp.pin || cleanPin == defaultRolePin
            else -> cleanPin == defaultRolePin
        }

        if (!pinValid) {
            return Result.failure(IllegalArgumentException("PIN salah untuk peran ${role.label}"))
        }

        val session = SessionState(
            isLoggedIn = true,
            role = role,
            userName = matchingEmp?.nama ?: cleanName,
            employeeId = matchingEmp?.id ?: ""
        )
        context.sessionDataStore.edit { prefs ->
            prefs[SessionKeys.IS_LOGGED_IN] = true
            prefs[SessionKeys.ROLE] = session.role.name
            prefs[SessionKeys.USER_NAME] = session.userName
            prefs[SessionKeys.EMPLOYEE_ID] = session.employeeId
        }
        return Result.success(session)
    }

    suspend fun logout() {
        context.sessionDataStore.edit { prefs ->
            prefs[SessionKeys.IS_LOGGED_IN] = false
            prefs[SessionKeys.USER_NAME] = ""
            prefs[SessionKeys.EMPLOYEE_ID] = ""
        }
    }

    suspend fun createTransaction(
        mulaiSewa: String,
        selesaiSewa: String,
        durasiHari: Int,
        satuan: RentalUnit,
        customerName: String,
        customerPhoneRaw: String,
        kasirName: String,
        items: List<TransactionItem>,
        biayaTambahan: List<AdditionalFee>,
        diskonAktif: Boolean,
        diskonTipe: DiscountType,
        diskonNilai: Long,
        kodePromo: String,
        subtotalItems: Long,
        nominalDiskon: Long,
        totalBiayaTambahan: Long,
        total: Long,
        statusPembayaran: PaymentStatus,
        sudahDibayar: Long,
        sisa: Long,
        metodePembayaran: PaymentMethod,
        statusTransaksi: RentalStatus,
        jenisJaminan: CollateralType,
        detailJaminan: String,
        catatan: String
    ): Result<RentalTransaction> {
        val today = Formatters.todayIso()
        val cleanPhone = Formatters.normalizePhone(customerPhoneRaw)
        val cleanCustomer = customerName.trim()
        if (cleanCustomer.isBlank() || cleanPhone.length < 8) {
            return Result.failure(IllegalArgumentException("Nama dan No. HP pelanggan wajib diisi dengan benar"))
        }
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pilih minimal 1 produk atau paket sewa"))
        }

        // Auto-member create or update
        val existingMember = memberDao.findByPhone(cleanPhone)?.toDomain()
        val member: Member
        val sewaKeN: Int
        if (existingMember == null) {
            sewaKeN = 1
            member = Member(
                id = "mem_${UUID.randomUUID().toString().take(8)}",
                nama = cleanCustomer,
                noHp = cleanPhone,
                memberSejak = today,
                totalSewa = 1,
                totalBelanja = total,
                transaksiTerakhir = today
            )
        } else {
            sewaKeN = existingMember.totalSewa + 1
            member = existingMember.copy(
                nama = cleanCustomer,
                totalSewa = sewaKeN,
                totalBelanja = existingMember.totalBelanja + total,
                transaksiTerakhir = today
            )
        }
        memberDao.insertMember(member.toEntity())

        // Generate transaction number: TRX-YYYYMMDD-XXX
        val countToday = transactionDao.countByDate(today) + 1
        val dateCompact = today.replace("-", "")
        val nomorTx = "TRX-$dateCompact-${countToday.toString().padStart(3, '0')}"

        val tx = RentalTransaction(
            id = "tx_${UUID.randomUUID().toString().take(10)}",
            nomorTransaksi = nomorTx,
            tanggalTransaksi = today,
            mulaiSewa = mulaiSewa,
            selesaiSewa = selesaiSewa,
            durasiHari = durasiHari,
            satuan = satuan,
            customerName = cleanCustomer,
            customerPhone = cleanPhone,
            memberId = member.id,
            memberSejak = member.memberSejak,
            sewaKeN = sewaKeN,
            kasirName = kasirName,
            items = items,
            biayaTambahan = biayaTambahan,
            diskonAktif = diskonAktif,
            diskonTipe = diskonTipe,
            diskonNilai = diskonNilai,
            kodePromo = kodePromo.trim(),
            subtotalItems = subtotalItems,
            nominalDiskon = nominalDiskon,
            totalBiayaTambahan = totalBiayaTambahan,
            total = total,
            statusPembayaran = statusPembayaran,
            sudahDibayar = sudahDibayar,
            sisa = sisa,
            metodePembayaran = metodePembayaran,
            statusTransaksi = statusTransaksi,
            jenisJaminan = jenisJaminan,
            detailJaminan = detailJaminan.trim(),
            catatan = catatan.trim(),
            createdAtMillis = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx.toEntity())

        // Adjust stock per variant if Booking or Aktif
        if (statusTransaksi == RentalStatus.BOOKING || statusTransaksi == RentalStatus.AKTIF) {
            adjustStockForItems(items, sign = -1)
        }

        triggerAutoSyncIfEnabled()
        return Result.success(tx)
    }

    private suspend fun adjustStockForItems(items: List<TransactionItem>, sign: Int) {
        val packages = packageDao.getPackagesOnce().map { it.toDomain() }
        for (item in items) {
            if (item.isPackage) {
                val pkg = packages.firstOrNull { it.id == item.productId }
                pkg?.items?.forEach { pItem ->
                    productDao.adjustVariantStock(pItem.variantId, sign * pItem.qty * item.qty)
                }
            } else {
                productDao.adjustVariantStock(item.variantId, sign * item.qty)
            }
        }
    }

    suspend fun updateTransactionStatusAndPayment(
        tx: RentalTransaction,
        newStatus: RentalStatus,
        newPaymentStatus: PaymentStatus,
        newSudahDibayar: Long,
        newMethod: PaymentMethod
    ) {
        val oldActive = tx.statusTransaksi == RentalStatus.BOOKING || tx.statusTransaksi == RentalStatus.AKTIF
        val newActive = newStatus == RentalStatus.BOOKING || newStatus == RentalStatus.AKTIF
        if (oldActive && !newActive) {
            // Returned / Selesai -> restore stock
            adjustStockForItems(tx.items, sign = +1)
        } else if (!oldActive && newActive) {
            // Re-opened -> deduct stock
            adjustStockForItems(tx.items, sign = -1)
        }

        val clampedPaid = newSudahDibayar.coerceIn(0L, tx.total)
        val newSisa = (tx.total - clampedPaid).coerceAtLeast(0L)
        val resolvedPayStatus = when {
            clampedPaid <= 0L -> PaymentStatus.TERHUTANG
            newSisa == 0L -> PaymentStatus.LUNAS
            else -> newPaymentStatus
        }

        val updated = tx.copy(
            statusTransaksi = newStatus,
            statusPembayaran = resolvedPayStatus,
            sudahDibayar = clampedPaid,
            sisa = newSisa,
            metodePembayaran = newMethod
        )
        transactionDao.insertTransaction(updated.toEntity())
        triggerAutoSyncIfEnabled()
    }

    suspend fun saveProduct(product: Product) {
        productDao.insertProducts(listOf(product.toEntity()))
        productDao.deleteVariantsForProduct(product.id)
        productDao.insertVariants(product.variants.map { it.copy(productId = product.id).toEntity() })
        triggerAutoSyncIfEnabled()
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteVariantsForProduct(productId)
        productDao.deleteProductById(productId)
        triggerAutoSyncIfEnabled()
    }

    suspend fun savePackage(pkg: RentalPackage) {
        packageDao.insertPackages(listOf(pkg.toEntity()))
        triggerAutoSyncIfEnabled()
    }

    suspend fun deletePackage(pkgId: String) {
        packageDao.deleteById(pkgId)
        triggerAutoSyncIfEnabled()
    }

    suspend fun saveMember(member: Member) {
        memberDao.insertMember(member.copy(noHp = Formatters.normalizePhone(member.noHp)).toEntity())
        triggerAutoSyncIfEnabled()
    }

    suspend fun deleteMember(memberId: String) {
        memberDao.deleteById(memberId)
        triggerAutoSyncIfEnabled()
    }

    suspend fun addCashEntry(entry: CashEntry) {
        cashEntryDao.insertEntry(entry.toEntity())
        triggerAutoSyncIfEnabled()
    }

    suspend fun deleteCashEntry(id: String) {
        cashEntryDao.deleteById(id)
        triggerAutoSyncIfEnabled()
    }

    suspend fun saveEmployee(employee: Employee) {
        employeeDao.insertEmployee(employee.toEntity())
        triggerAutoSyncIfEnabled()
    }

    suspend fun deleteEmployee(id: String) {
        employeeDao.deleteById(id)
        triggerAutoSyncIfEnabled()
    }

    suspend fun saveSettings(bundle: AppSettingsBundle) {
        settingsDao.upsertSettings(bundle.toEntity())
        triggerAutoSyncIfEnabled()
    }

    suspend fun buildCurrentBackupJson(): String {
        val settings = settingsDao.getSettingsOnce()?.toDomain() ?: SeedData.initialSettings()
        val payload = BackupPayload(
            store = settings.store,
            qris = settings.qris,
            drive = settings.drive,
            employees = employeeDao.getEmployeesOnce().map { it.toDomain() },
            products = productDao.getProductsWithVariantsOnce().map { it.toDomain() },
            packages = packageDao.getPackagesOnce().map { it.toDomain() },
            members = memberDao.getMembersOnce().map { it.toDomain() },
            transactions = transactionDao.getTransactionsOnce().map { it.toDomain() },
            cashEntries = cashEntryDao.getCashEntriesOnce().map { it.toDomain() }
        )
        return ExportImportManager.buildBackupJson(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<String> {
        val parsed = ExportImportManager.parseBackupJson(rawJson)
        val payload = parsed.getOrElse { return Result.failure(it) }

        val currentSettings = settingsDao.getSettingsOnce()?.toDomain() ?: SeedData.initialSettings()
        val mergedDrive = if (payload.drive.webAppUrl.isNotBlank()) payload.drive else currentSettings.drive
        settingsDao.upsertSettings(
            AppSettingsBundle(payload.store, payload.qris, mergedDrive).toEntity()
        )

        if (payload.employees.isNotEmpty()) {
            employeeDao.clearAll()
            employeeDao.insertEmployees(payload.employees.map { it.toEntity() })
        }
        if (payload.products.isNotEmpty()) {
            productDao.clearAllVariants()
            productDao.clearAllProducts()
            productDao.insertProducts(payload.products.map { it.toEntity() })
            productDao.insertVariants(payload.products.flatMap { p -> p.variants.map { it.toEntity() } })
        }
        packageDao.clearAll()
        if (payload.packages.isNotEmpty()) {
            packageDao.insertPackages(payload.packages.map { it.toEntity() })
        }
        memberDao.clearAll()
        if (payload.members.isNotEmpty()) {
            memberDao.insertMembers(payload.members.map { it.toEntity() })
        }
        transactionDao.clearAll()
        if (payload.transactions.isNotEmpty()) {
            transactionDao.insertTransactions(payload.transactions.map { it.toEntity() })
        }
        cashEntryDao.clearAll()
        if (payload.cashEntries.isNotEmpty()) {
            cashEntryDao.insertEntries(payload.cashEntries.map { it.toEntity() })
        }

        return Result.success(
            "Berhasil import ${payload.products.size} produk, ${payload.members.size} member, ${payload.transactions.size} transaksi"
        )
    }

    suspend fun syncToGoogleDriveNow(): Result<String> {
        val current = settingsDao.getSettingsOnce()?.toDomain() ?: SeedData.initialSettings()
        val url = current.drive.webAppUrl.trim()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("URL Web App Apps Script belum diisi"))
        }
        val json = buildCurrentBackupJson()
        val result = AppsScriptSyncClient.uploadBackup(url, json)
        val updatedDrive = result.fold(
            onSuccess = {
                current.drive.copy(
                    lastSyncTime = Formatters.nowTimestampLabel(),
                    lastSyncStatus = "Berhasil tersinkronisasi"
                )
            },
            onFailure = { err ->
                current.drive.copy(
                    lastSyncTime = Formatters.nowTimestampLabel(),
                    lastSyncStatus = err.localizedMessage ?: "Gagal sinkronisasi"
                )
            }
        )
        settingsDao.upsertSettings(current.copy(drive = updatedDrive).toEntity())
        return result
    }

    private suspend fun triggerAutoSyncIfEnabled() {
        val current = settingsDao.getSettingsOnce()?.toDomain() ?: return
        if (current.drive.autoSync && current.drive.webAppUrl.trim().startsWith("https://")) {
            syncToGoogleDriveNow()
        }
    }

    // Mappers
    private fun ProductWithVariants.toDomain() = Product(
        id = product.id,
        nama = product.nama,
        kategori = product.kategori,
        deskripsi = product.deskripsi,
        variants = variants.map { it.toDomain() },
        photoPath = product.photoPath,
        isDisewa = product.isDisewa,
        isDijual = product.isDijual,
        satuan = runCatching { ProductPriceUnit.valueOf(product.satuan) }.getOrDefault(ProductPriceUnit.HARI),
        gunakanStok = product.gunakanStok,
        customTiers = runCatching { ExportImportManager.parseProductTiersJson(product.customTiersJson) }.getOrDefault(emptyList()),
        hargaJual = product.hargaJual,
        createdAtMillis = product.createdAtMillis,
        updatedAtMillis = product.updatedAtMillis
    )

    private fun ProductVariantEntity.toDomain() = ProductVariant(
        id = id,
        productId = productId,
        namaVarian = namaVarian,
        sku = sku,
        warnaNama = warnaNama,
        warnaHex = warnaHex,
        ukuran = ukuran,
        stok = stok,
        harga1H = harga1H,
        harga2H = harga2H,
        harga3H = harga3H,
        harga4H = harga4H,
        harga5H = harga5H,
        harga6H = harga6H,
        status = runCatching { VariantStatus.valueOf(status) }.getOrDefault(VariantStatus.TERSEDIA)
    )

    private fun Product.toEntity() = ProductEntity(
        id = id,
        nama = nama,
        kategori = kategori,
        deskripsi = deskripsi,
        photoPath = photoPath,
        isDisewa = isDisewa,
        isDijual = isDijual,
        satuan = satuan.name,
        gunakanStok = gunakanStok,
        customTiersJson = ExportImportManager.productTiersToJson(customTiers),
        hargaJual = hargaJual,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
    private fun ProductVariant.toEntity() = ProductVariantEntity(
        id, productId, namaVarian, sku, warnaNama, warnaHex, ukuran, stok,
        harga1H, harga2H, harga3H, harga4H, harga5H, harga6H, status.name
    )

    private fun PackageEntity.toDomain() = RentalPackage(
        id = id,
        nama = nama,
        deskripsi = deskripsi,
        harga1H = harga1H,
        harga2H = harga2H,
        harga3H = harga3H,
        harga4H = harga4H,
        harga5H = harga5H,
        harga6H = harga6H,
        items = runCatching { ExportImportManager.parsePackageItemsJson(itemsJson) }.getOrDefault(emptyList()),
        aktif = aktif
    )

    private fun RentalPackage.toEntity() = PackageEntity(
        id, nama, deskripsi, harga1H, harga2H, harga3H, harga4H, harga5H, harga6H,
        ExportImportManager.packageItemsToJson(items), aktif
    )

    private fun TransactionEntity.toDomain() = RentalTransaction(
        id = id,
        nomorTransaksi = nomorTransaksi,
        tanggalTransaksi = tanggalTransaksi,
        mulaiSewa = mulaiSewa,
        selesaiSewa = selesaiSewa,
        durasiHari = durasiHari,
        satuan = runCatching { RentalUnit.valueOf(satuan) }.getOrDefault(RentalUnit.HARI),
        customerName = customerName,
        customerPhone = customerPhone,
        memberId = memberId,
        memberSejak = memberSejak,
        sewaKeN = sewaKeN,
        kasirName = kasirName,
        items = runCatching { ExportImportManager.parseTransactionItemsJson(itemsJson) }.getOrDefault(emptyList()),
        biayaTambahan = runCatching { ExportImportManager.parseAdditionalFeesJson(biayaTambahanJson) }.getOrDefault(emptyList()),
        diskonAktif = diskonAktif,
        diskonTipe = runCatching { DiscountType.valueOf(diskonTipe) }.getOrDefault(DiscountType.RUPIAH),
        diskonNilai = diskonNilai,
        kodePromo = kodePromo,
        subtotalItems = subtotalItems,
        nominalDiskon = nominalDiskon,
        totalBiayaTambahan = totalBiayaTambahan,
        total = total,
        statusPembayaran = runCatching { PaymentStatus.valueOf(statusPembayaran) }.getOrDefault(PaymentStatus.LUNAS),
        sudahDibayar = sudahDibayar,
        sisa = sisa,
        metodePembayaran = runCatching { PaymentMethod.valueOf(metodePembayaran) }.getOrDefault(PaymentMethod.CASH),
        statusTransaksi = runCatching { RentalStatus.valueOf(statusTransaksi) }.getOrDefault(RentalStatus.AKTIF),
        jenisJaminan = runCatching { CollateralType.valueOf(jenisJaminan) }.getOrDefault(CollateralType.KTP),
        detailJaminan = detailJaminan,
        catatan = catatan,
        createdAtMillis = createdAtMillis
    )

    private fun RentalTransaction.toEntity() = TransactionEntity(
        id, nomorTransaksi, tanggalTransaksi, mulaiSewa, selesaiSewa, durasiHari, satuan.name,
        customerName, customerPhone, memberId, memberSejak, sewaKeN, kasirName,
        ExportImportManager.transactionItemsToJson(items),
        ExportImportManager.additionalFeesToJson(biayaTambahan),
        diskonAktif, diskonTipe.name, diskonNilai, kodePromo, subtotalItems, nominalDiskon,
        totalBiayaTambahan, total, statusPembayaran.name, sudahDibayar, sisa,
        metodePembayaran.name, statusTransaksi.name, jenisJaminan.name, detailJaminan, catatan, createdAtMillis
    )

    private fun MemberEntity.toDomain() = Member(id, nama, noHp, memberSejak, totalSewa, totalBelanja, transaksiTerakhir)
    private fun Member.toEntity() = MemberEntity(id, nama, noHp, memberSejak, totalSewa, totalBelanja, transaksiTerakhir)

    private fun CashEntryEntity.toDomain() = CashEntry(
        id, tanggal,
        runCatching { CashEntryType.valueOf(tipe) }.getOrDefault(CashEntryType.PENGELUARAN),
        kategori, deskripsi, nominal,
        runCatching { PaymentMethod.valueOf(metode) }.getOrDefault(PaymentMethod.CASH),
        createdAtMillis
    )

    private fun CashEntry.toEntity() = CashEntryEntity(
        id, tanggal, tipe.name, kategori, deskripsi, nominal, metode.name, createdAtMillis
    )

    private fun EmployeeEntity.toDomain() = Employee(
        id, nama,
        runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.KASIR),
        pin, noHp, aktif
    )

    private fun Employee.toEntity() = EmployeeEntity(id, nama, role.name, pin, noHp, aktif)

    private fun SettingsEntity.toDomain() = AppSettingsBundle(
        store = StoreSettings(
            namaToko = namaToko,
            alamat = alamat,
            noHp = noHp,
            catatanStruk = catatanStruk,
            adminPin = adminPin,
            kasirPin = kasirPin,
            printerWidthMm = printerWidthMm,
            activePaymentMethods = parseActivePaymentMethodsJson(activePaymentMethodsJson)
        ),
        qris = QrisSettings(namaMerchant, nmid, rekeningInfo, payloadStatis),
        drive = DriveSyncSettings(driveWebAppUrl, driveAutoSync, lastSyncTime, lastSyncStatus)
    )

    private fun AppSettingsBundle.toEntity() = SettingsEntity(
        id = 1,
        namaToko = store.namaToko,
        alamat = store.alamat,
        noHp = store.noHp,
        catatanStruk = store.catatanStruk,
        adminPin = store.adminPin,
        kasirPin = store.kasirPin,
        printerWidthMm = store.printerWidthMm,
        namaMerchant = qris.namaMerchant,
        nmid = qris.nmid,
        rekeningInfo = qris.rekeningInfo,
        payloadStatis = qris.payloadStatis,
        driveWebAppUrl = drive.webAppUrl,
        driveAutoSync = drive.autoSync,
        lastSyncTime = drive.lastSyncTime,
        lastSyncStatus = drive.lastSyncStatus,
        activePaymentMethodsJson = serializeActivePaymentMethodsJson(store.activePaymentMethods)
    )

    private fun parseActivePaymentMethodsJson(raw: String): List<String> {
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val str = arr.getString(i)
                if (str.isNotBlank()) list.add(str)
            }
            if (list.isNotEmpty()) list else listOf("CASH", "QRIS", "TRANSFER")
        }.getOrDefault(listOf("CASH", "QRIS", "TRANSFER"))
    }

    private fun serializeActivePaymentMethodsJson(methods: List<String>): String {
        val arr = JSONArray()
        methods.forEach { arr.put(it) }
        return arr.toString()
    }
}
