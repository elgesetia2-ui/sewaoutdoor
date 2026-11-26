package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SeedData
import com.example.data.repository.SewaRepository
import com.example.model.*
import com.example.util.AppsScriptSyncClient
import com.example.util.ExportImportManager
import com.example.util.Formatters
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SewaRepository(application)

    private val _isSeeded = MutableStateFlow(false)
    val isSeeded: StateFlow<Boolean> = _isSeeded.asStateFlow()

    val session: StateFlow<SessionState> = repository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionState())

    val products: StateFlow<List<Product>> = repository.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val packages: StateFlow<List<RentalPackage>> = repository.packagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<RentalTransaction>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<Member>> = repository.membersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashEntries: StateFlow<List<CashEntry>> = repository.cashEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<Employee>> = repository.employeesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsBundle> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeedData.initialSettings())

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Quick pre-selected item when user taps "Sewa" from Katalog
    private val _preselectedCartItem = MutableStateFlow<Pair<Product, ProductVariant>?>(null)
    val preselectedCartItem: StateFlow<Pair<Product, ProductVariant>?> = _preselectedCartItem.asStateFlow()

    // Active receipt to view
    private val _activeReceiptTxId = MutableStateFlow<String?>(null)
    val activeReceiptTx: StateFlow<RentalTransaction?> = combine(_activeReceiptTxId, transactions) { id, list ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _driveActionBusy = MutableStateFlow(false)
    val driveActionBusy: StateFlow<Boolean> = _driveActionBusy.asStateFlow()

    private val _driveTestResult = MutableStateFlow<String?>(null)
    val driveTestResult: StateFlow<String?> = _driveTestResult.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isSeeded.value = true
        }
    }

    fun emitMessage(msg: String) {
        _toastMessage.tryEmit(msg)
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun login(role: UserRole, name: String, pin: String) {
        viewModelScope.launch {
            val res = repository.login(role, name, pin)
            res.onSuccess {
                _loginError.value = null
            }.onFailure { err ->
                _loginError.value = err.localizedMessage ?: "Gagal masuk"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _activeReceiptTxId.value = null
        }
    }

    fun setPreselectedProduct(product: Product, variant: ProductVariant) {
        _preselectedCartItem.value = product to variant
    }

    fun consumePreselectedProduct(): Pair<Product, ProductVariant>? {
        val current = _preselectedCartItem.value
        _preselectedCartItem.value = null
        return current
    }

    fun openReceipt(txId: String?) {
        _activeReceiptTxId.value = txId
    }

    fun saveTransaction(
        mulaiSewa: String,
        selesaiSewa: String,
        durasiHari: Int,
        satuan: RentalUnit,
        customerName: String,
        customerPhoneRaw: String,
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
        catatan: String,
        onSaved: (RentalTransaction) -> Unit
    ) {
        viewModelScope.launch {
            val kasir = session.value.userName.ifBlank { session.value.role.label }
            val result = repository.createTransaction(
                mulaiSewa = mulaiSewa,
                selesaiSewa = selesaiSewa,
                durasiHari = durasiHari,
                satuan = satuan,
                customerName = customerName,
                customerPhoneRaw = customerPhoneRaw,
                kasirName = kasir,
                items = items,
                biayaTambahan = biayaTambahan,
                diskonAktif = diskonAktif,
                diskonTipe = diskonTipe,
                diskonNilai = diskonNilai,
                kodePromo = kodePromo,
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
                detailJaminan = detailJaminan,
                catatan = catatan
            )
            result.onSuccess { tx ->
                _toastMessage.tryEmit("Transaksi ${tx.nomorTransaksi} berhasil disimpan")
                onSaved(tx)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Gagal menyimpan transaksi")
            }
        }
    }

    fun updateTransactionStatus(
        tx: RentalTransaction,
        newStatus: RentalStatus,
        newPaymentStatus: PaymentStatus,
        newSudahDibayar: Long,
        newMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            repository.updateTransactionStatusAndPayment(tx, newStatus, newPaymentStatus, newSudahDibayar, newMethod)
            _toastMessage.tryEmit("Status transaksi ${tx.nomorTransaksi} diperbarui")
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _toastMessage.tryEmit("Produk ${product.nama} disimpan")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _toastMessage.tryEmit("Produk dihapus")
        }
    }

    fun savePackage(pkg: RentalPackage) {
        viewModelScope.launch {
            repository.savePackage(pkg)
            _toastMessage.tryEmit("Paket ${pkg.nama} disimpan")
        }
    }

    fun deletePackage(pkgId: String) {
        viewModelScope.launch {
            repository.deletePackage(pkgId)
            _toastMessage.tryEmit("Paket dihapus")
        }
    }

    fun saveMember(member: Member) {
        viewModelScope.launch {
            repository.saveMember(member)
            _toastMessage.tryEmit("Data member diperbarui")
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            _toastMessage.tryEmit("Member dihapus")
        }
    }

    fun addCashEntry(entry: CashEntry) {
        viewModelScope.launch {
            repository.addCashEntry(entry)
            _toastMessage.tryEmit("${entry.tipe.label} dicatat ke Buku Kas")
        }
    }

    fun deleteCashEntry(id: String) {
        viewModelScope.launch {
            repository.deleteCashEntry(id)
            _toastMessage.tryEmit("Catatan kas dihapus")
        }
    }

    fun saveEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.saveEmployee(employee)
            _toastMessage.tryEmit("Data karyawan ${employee.nama} disimpan")
        }
    }

    fun deleteEmployee(id: String) {
        viewModelScope.launch {
            repository.deleteEmployee(id)
            _toastMessage.tryEmit("Karyawan dihapus")
        }
    }

    fun saveSettings(bundle: AppSettingsBundle) {
        viewModelScope.launch {
            repository.saveSettings(bundle)
            _toastMessage.tryEmit("Pengaturan disimpan")
        }
    }

    fun exportAllJsonToDownloads(context: Context) {
        viewModelScope.launch {
            val json = repository.buildCurrentBackupJson()
            val fileName = "Backup_SewaOutdoor_${Formatters.fileTimestamp()}.json"
            val res = ExportImportManager.saveTextFileToDownloads(context, fileName, "application/json", json)
            _toastMessage.tryEmit(res.getOrElse { it.localizedMessage ?: "Gagal export JSON" })
        }
    }

    fun exportCsvToDownloads(context: Context, kind: String, csvContent: String) {
        viewModelScope.launch {
            val fileName = "${kind}_${Formatters.fileTimestamp()}.csv"
            val res = ExportImportManager.saveTextFileToDownloads(context, fileName, "text/csv", csvContent)
            _toastMessage.tryEmit(res.getOrElse { it.localizedMessage ?: "Gagal export CSV" })
        }
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val res = repository.importBackupJson(rawJson)
            _toastMessage.tryEmit(res.getOrElse { it.localizedMessage ?: "Gagal import JSON" })
        }
    }

    fun testDriveConnection(url: String) {
        viewModelScope.launch {
            _driveActionBusy.value = true
            _driveTestResult.value = null
            val res = AppsScriptSyncClient.testConnection(url)
            _driveTestResult.value = res.getOrElse { it.localizedMessage ?: "Koneksi gagal" }
            _driveActionBusy.value = false
        }
    }

    fun syncDriveNow() {
        viewModelScope.launch {
            _driveActionBusy.value = true
            val res = repository.syncToGoogleDriveNow()
            val msg = res.getOrElse { it.localizedMessage ?: "Gagal sync Google Drive" }
            _driveTestResult.value = msg
            _toastMessage.tryEmit(msg)
            _driveActionBusy.value = false
        }
    }

    fun buildAllMutations(txList: List<RentalTransaction>, entries: List<CashEntry>): List<CashMutation> {
        val list = mutableListOf<CashMutation>()
        txList.filter { it.sudahDibayar > 0L }.forEach { tx ->
            list.add(
                CashMutation(
                    id = "mut_${tx.id}",
                    tanggal = tx.tanggalTransaksi,
                    sumber = "Sewa ${tx.nomorTransaksi}",
                    deskripsi = "${tx.customerName} (${tx.statusPembayaran.label})",
                    isMasuk = true,
                    nominal = tx.sudahDibayar,
                    metode = tx.metodePembayaran,
                    timestampMillis = tx.createdAtMillis
                )
            )
        }
        entries.forEach { e ->
            list.add(
                CashMutation(
                    id = e.id,
                    tanggal = e.tanggal,
                    sumber = e.kategori,
                    deskripsi = e.deskripsi,
                    isMasuk = e.tipe == CashEntryType.PEMASUKAN_LAIN,
                    nominal = e.nominal,
                    metode = e.metode,
                    timestampMillis = e.createdAtMillis
                )
            )
        }
        return list.sortedByDescending { it.timestampMillis }
    }
}
