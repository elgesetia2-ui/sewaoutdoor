package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.QrisGenerator
import java.util.UUID

@Composable
fun AccountScreen(
    session: SessionState,
    settings: AppSettingsBundle,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onSaveSettings: (AppSettingsBundle) -> Unit = {}
) {
    var showPaymentMethodsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(
            title = "Akun & Pengaturan",
            subtitle = settings.store.namaToko
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaroonPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (session.role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.userName.ifBlank { session.role.label }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(
                            text = "Peran: ${session.role.label}",
                            containerColor = MaroonContainer,
                            contentColor = MaroonPrimary
                        )
                    }
                }
            }

            // Switch Role / Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("switch_role_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ganti Peran / Keluar")
            }

            if (session.role == UserRole.ADMIN) {
                Text("Pengaturan Admin", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))

                AccountMenuTile("Karyawan", "Kelola Admin & Kasir", Icons.Default.Badge) { onNavigate("employees") }
                AccountMenuTile("Pengaturan Toko", "Nama toko, alamat, dan nomor HP", Icons.Default.Storefront) { onNavigate("store_settings") }
                AccountMenuTile("Pengaturan QRIS", "Nama merchant, NMID, payload statis", Icons.Default.QrCode2) { onNavigate("qris_settings") }
                AccountMenuTile("Peran & PIN", "Ubah PIN default Admin & Kasir", Icons.Default.Security) { onNavigate("roles_pin") }
                AccountMenuTile("Backup & Google Drive", "Export/Import JSON, CSV, Apps Script", Icons.Default.CloudSync) { onNavigate("backup_drive") }
                AccountMenuTile("Setting Printer", "Pilih lebar 58mm/80mm & tes printer", Icons.Default.Print) { onNavigate("printer_settings") }
                AccountMenuTile("Atur Struk / Invoice", "Format struk, catatan, & info kasir", Icons.Default.Receipt) { onNavigate("invoice_settings") }
                AccountMenuTile("Metode Pembayaran Aktif", "Kelola metode bayar (Tunai, QRIS, Transfer)", Icons.Default.Payment) { showPaymentMethodsSheet = true }
                AccountMenuTile("Jam Operasional", "Atur jam buka & hari operasional toko", Icons.Default.AccessTime) { onNavigate("operating_hours_settings") }
                AccountMenuTile("Pengaturan Bonus Pelanggan", "Poin loyalitas & bonus member", Icons.Default.CardGiftcard) { onNavigate("bonus_settings") }
            } else {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hak Akses Kasir", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        Text("• Tambah Transaksi & Cetak/WhatsApp Struk", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("• Melihat Daftar Transaksi pribadi & Katalog", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("• Laporan Penjualan pribadi & Export CSV", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onNavigate("catalog") },
                            border = BorderStroke(1.dp, MaroonPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Katalog Peralatan")
                        }
                    }
                }
            }
        }
    }

    if (showPaymentMethodsSheet) {
        ActivePaymentMethodsBottomSheet(
            settings = settings,
            onDismiss = { showPaymentMethodsSheet = false },
            onSave = { updated ->
                onSaveSettings(updated)
                showPaymentMethodsSheet = false
            }
        )
    }
}

@Composable
private fun AccountMenuTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaroonContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = MaroonPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun EmployeesScreen(
    employees: List<Employee>,
    onBack: () -> Unit,
    onSaveEmployee: (Employee) -> Unit,
    onDeleteEmployee: (String) -> Unit
) {
    BackHandler { onBack() }

    var editing by remember { mutableStateOf<Employee?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(
            title = "Karyawan",
            subtitle = "${employees.size} akun terdaftar",
            onBack = onBack,
            trailing = {
                Button(
                    onClick = { adding = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(employees, key = { it.id }) { emp ->
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(emp.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                StatusPill(emp.role.label, MaroonContainer, MaroonPrimary)
                            }
                            Text("PIN: ${emp.pin} • HP: ${emp.noHp.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        IconButton(onClick = { editing = emp }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaroonPrimary)
                        }
                        if (employees.size > 1) {
                            IconButton(onClick = { onDeleteEmployee(emp.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        var nama by remember { mutableStateOf(editing?.nama ?: "") }
        var role by remember { mutableStateOf(editing?.role ?: UserRole.KASIR) }
        var pin by remember { mutableStateOf(editing?.pin ?: "0000") }
        var noHp by remember { mutableStateOf(editing?.noHp ?: "") }

        AlertDialog(
            onDismissRequest = { adding = false; editing = null },
            title = { Text(if (editing == null) "Tambah Karyawan" else "Edit Karyawan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                        }
                    }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) pin = it },
                        label = { Text("PIN*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = noHp, onValueChange = { noHp = it }, label = { Text("No HP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nama.trim().isNotEmpty() && pin.isNotEmpty()) {
                            onSaveEmployee(
                                Employee(
                                    id = editing?.id ?: "emp_${UUID.randomUUID().toString().take(8)}",
                                    nama = nama.trim(),
                                    role = role,
                                    pin = pin,
                                    noHp = noHp.trim(),
                                    aktif = true
                                )
                            )
                            adding = false; editing = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { adding = false; editing = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun StoreSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }

    var namaToko by remember { mutableStateOf(settings.store.namaToko) }
    var alamat by remember { mutableStateOf(settings.store.alamat) }
    var noHp by remember { mutableStateOf(settings.store.noHp) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Pengaturan Toko", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Informasi Usaha & Toko", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    OutlinedTextField(
                        value = namaToko,
                        onValueChange = { namaToko = it },
                        label = { Text("Nama Toko") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = alamat,
                        onValueChange = { alamat = it },
                        label = { Text("Alamat Toko") },
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noHp,
                        onValueChange = { noHp = it },
                        label = { Text("No. HP / WhatsApp Toko") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            onSave(
                                settings.copy(
                                    store = settings.store.copy(
                                        namaToko = namaToko.trim(),
                                        alamat = alamat.trim(),
                                        noHp = noHp.trim()
                                    )
                                )
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Pengaturan Toko") }
                }
            }
        }
    }
}

@Composable
fun QrisSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }

    var merchant by remember { mutableStateOf(settings.qris.namaMerchant) }
    var nmid by remember { mutableStateOf(settings.qris.nmid) }
    var rekening by remember { mutableStateOf(settings.qris.rekeningInfo) }
    var payload by remember { mutableStateOf(settings.qris.payloadStatis) }

    val previewBmp = remember(payload) {
        QrisGenerator.generateQrBitmap(payload.ifBlank { "QRIS" }, 380)
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Pengaturan QRIS", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Preview QRIS",
                        modifier = Modifier.size(160.dp)
                    )
                    OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Nama Merchant QRIS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nmid, onValueChange = { nmid = it }, label = { Text("NMID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rekening, onValueChange = { rekening = it }, label = { Text("Rekening / E-Wallet") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = payload, onValueChange = { payload = it }, label = { Text("Payload QRIS Statis (EMVCo String)") }, minLines = 3, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onSave(
                                settings.copy(
                                    qris = QrisSettings(
                                        namaMerchant = merchant.trim(),
                                        nmid = nmid.trim(),
                                        rekeningInfo = rekening.trim(),
                                        payloadStatis = payload.trim()
                                    )
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Pengaturan QRIS") }
                }
            }
        }
    }
}

@Composable
fun RolesPinScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }

    var adminPin by remember { mutableStateOf(settings.store.adminPin) }
    var kasirPin by remember { mutableStateOf(settings.store.kasirPin) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Peran & PIN", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PIN Master Peran", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) adminPin = it },
                        label = { Text("PIN Default Admin (Full Fitur)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kasirPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) kasirPin = it },
                        label = { Text("PIN Default Kasir") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (adminPin.isNotEmpty() && kasirPin.isNotEmpty()) {
                                onSave(
                                    settings.copy(
                                        store = settings.store.copy(adminPin = adminPin, kasirPin = kasirPin)
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan PIN Peran") }
                }
            }

            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ringkasan Hak Akses", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                    Text("• Admin: Akses penuh ke Dashboard, Transaksi, Produk, Paket, Member, Buku Kas, Laporan Lengkap, Karyawan, Toko, QRIS, Peran & PIN, Backup/Drive.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("• Kasir: Hanya Tambah Transaksi, Daftar Transaksi milik sendiri, Katalog, dan Laporan Penjualan kasir tersebut.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun PrinterSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }
    var printerWidth by remember { mutableIntStateOf(settings.store.printerWidthMm) }
    var printerName by remember { mutableStateOf("Bluetooth ESC/POS Printer (Default)") }
    var autoPrint by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Setting Printer Thermal", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih Lebar Kertas Printer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = printerWidth == 58, onClick = { printerWidth = 58 }, label = { Text("58 mm (Mini)") })
                        FilterChip(selected = printerWidth == 80, onClick = { printerWidth = 80 }, label = { Text("80 mm (Standar)") })
                    }

                    OutlinedTextField(
                        value = printerName,
                        onValueChange = { printerName = it },
                        label = { Text("Nama/Koneksi Perangkat Printer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cetak Otomatis Setelah Transaksi", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoPrint, onCheckedChange = { autoPrint = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary))
                    }

                    Button(
                        onClick = {
                            onSave(
                                settings.copy(
                                    store = settings.store.copy(printerWidthMm = printerWidth)
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Pengaturan Printer") }
                }
            }
        }
    }
}

@Composable
fun InvoiceSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }
    var catatanStruk by remember { mutableStateOf(settings.store.catatanStruk) }
    var showCashierName by remember { mutableStateOf(true) }
    var showStoreAddress by remember { mutableStateOf(true) }
    var invoicePrefix by remember { mutableStateOf("INV-RENT") }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Atur Struk / Invoice", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Format & Tampilan Struk", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    OutlinedTextField(
                        value = invoicePrefix,
                        onValueChange = { invoicePrefix = it },
                        label = { Text("Prefix Nomor Invoice / Struk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catatanStruk,
                        onValueChange = { catatanStruk = it },
                        label = { Text("Catatan / Pesan Footer Struk") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan Nama Kasir di Struk", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showCashierName, onCheckedChange = { showCashierName = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan Alamat & No. HP Toko", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showStoreAddress, onCheckedChange = { showStoreAddress = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary))
                    }

                    Button(
                        onClick = {
                            onSave(
                                settings.copy(
                                    store = settings.store.copy(catatanStruk = catatanStruk)
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Pengaturan Struk") }
                }
            }
        }
    }
}

val allSupportedPaymentMethods = listOf(
    PaymentMethod.CASH to "Tunai",
    PaymentMethod.GOPAY to "GoPay",
    PaymentMethod.DANA to "Dana",
    PaymentMethod.SHOPEEPAY to "ShopeePay",
    PaymentMethod.QRIS to "QRIS",
    PaymentMethod.LINKAJA to "LinkAja",
    PaymentMethod.OVO to "OVO",
    PaymentMethod.TRANSFER to "Transfer",
    PaymentMethod.KARTU to "Kartu"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePaymentMethodsBottomSheet(
    settings: AppSettingsBundle,
    onDismiss: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMethods by remember(settings.store.activePaymentMethods) {
        mutableStateOf(settings.store.activePaymentMethods.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Metode Pembayaran Aktif",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = OutlineSubtle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                allSupportedPaymentMethods.forEach { (method, label) ->
                    val isChecked = selectedMethods.contains(method.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMethods = if (isChecked) {
                                    if (selectedMethods.size > 1) selectedMethods - method.name else selectedMethods
                                } else {
                                    selectedMethods + method.name
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedMethods = if (checked) {
                                    selectedMethods + method.name
                                } else {
                                    if (selectedMethods.size > 1) selectedMethods - method.name else selectedMethods
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaroonPrimary,
                                checkmarkColor = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                    HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Button(
                    onClick = {
                        val orderedList = allSupportedPaymentMethods
                            .map { it.first.name }
                            .filter { selectedMethods.contains(it) }
                            .ifEmpty { listOf("CASH") }

                        onSave(
                            settings.copy(
                                store = settings.store.copy(
                                    activePaymentMethods = orderedList
                                )
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_active_payment_methods_button")
                ) {
                    Text(
                        text = "Simpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodsSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }
    ActivePaymentMethodsBottomSheet(
        settings = settings,
        onDismiss = onBack,
        onSave = { updated ->
            onSave(updated)
            onBack()
        }
    )
}

@Composable
fun OperatingHoursSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }
    var jamBuka by remember { mutableStateOf("08:00") }
    var jamTutup by remember { mutableStateOf("21:00") }
    var hariOperasional by remember { mutableStateOf("Senin - Minggu") }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Jam Operasional Toko", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pengaturan Jadwal Toko", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    OutlinedTextField(
                        value = hariOperasional,
                        onValueChange = { hariOperasional = it },
                        label = { Text("Hari Operasional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = jamBuka,
                            onValueChange = { jamBuka = it },
                            label = { Text("Jam Buka") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = jamTutup,
                            onValueChange = { jamTutup = it },
                            label = { Text("Jam Tutup") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Jam Operasional") }
                }
            }
        }
    }
}

@Composable
fun BonusSettingsScreen(
    settings: AppSettingsBundle,
    onBack: () -> Unit,
    onSave: (AppSettingsBundle) -> Unit
) {
    BackHandler { onBack() }
    var poinPerRupiah by remember { mutableStateOf("10000") }
    var poinDidapat by remember { mutableStateOf("10") }
    var enableBonus by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(title = "Pengaturan Bonus Pelanggan", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Program Loyalitas & Poin Member", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Aktifkan Bonus Poin Pelanggan", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = enableBonus, onCheckedChange = { enableBonus = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary))
                    }
                    OutlinedTextField(
                        value = poinPerRupiah,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) poinPerRupiah = it },
                        label = { Text("Kelipatan Belanja untuk Poin (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = poinDidapat,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) poinDidapat = it },
                        label = { Text("Jumlah Poin Didapat per Kelipatan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Simpan Pengaturan Bonus") }
                }
            }
        }
    }
}
