package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters

data class AdminShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val tag: String
)

@Composable
fun DashboardScreen(
    storeName: String,
    userName: String,
    transactions: List<RentalTransaction>,
    cashEntries: List<CashEntry>,
    products: List<Product>,
    members: List<Member>,
    onNavigate: (String) -> Unit,
    onOpenTransaction: (RentalTransaction) -> Unit
) {
    val today = remember { Formatters.todayIso() }
    val monthPrefix = remember(today) { today.take(7) }

    val omzetHari = remember(transactions, today) {
        transactions.filter { it.tanggalTransaksi == today }.sumOf { it.total }
    }
    val omzetBulan = remember(transactions, monthPrefix) {
        transactions.filter { it.tanggalTransaksi.startsWith(monthPrefix) }.sumOf { it.total }
    }
    val unitTersewa = remember(transactions) {
        transactions
            .filter { it.statusTransaksi == RentalStatus.AKTIF || it.statusTransaksi == RentalStatus.BOOKING }
            .sumOf { tx -> tx.items.sumOf { it.qty } }
    }
    val terlambatList = remember(transactions, today) {
        transactions.filter {
            it.statusTransaksi != RentalStatus.SELESAI && it.selesaiSewa < today
        }
    }
    val saldoKas = remember(transactions, cashEntries) {
        val masukSewa = transactions.sumOf { it.sudahDibayar }
        val masukLain = cashEntries.filter { it.tipe == CashEntryType.PEMASUKAN_LAIN }.sumOf { it.nominal }
        val keluar = cashEntries.filter { it.tipe == CashEntryType.PENGELUARAN }.sumOf { it.nominal }
        masukSewa + masukLain - keluar
    }

    val shortcuts = remember(products.size, members.size) {
        listOf(
            AdminShortcut("Katalog", "Cari & swatch warna", Icons.Default.GridView, "catalog", "menu_catalog"),
            AdminShortcut("Produk & Varian", "${products.size} alat", Icons.Default.Inventory2, "products_admin", "menu_products"),
            AdminShortcut("Paket Produk", "Bundling hemat", Icons.Default.Layers, "packages_admin", "menu_packages"),
            AdminShortcut("Member", "${members.size} penyewa", Icons.Default.Groups, "members", "menu_members"),
            AdminShortcut("Buku Kas", "Mutasi & Laba Rugi", Icons.Default.AccountBalanceWallet, "cashbook", "menu_cashbook"),
            AdminShortcut("Karyawan", "Admin & Kasir", Icons.Default.Badge, "employees", "menu_employees"),
            AdminShortcut("Pengaturan QRIS", "Merchant & NMID", Icons.Default.QrCode2, "qris_settings", "menu_qris"),
            AdminShortcut("Pengaturan Toko", "Profil & Printer", Icons.Default.Storefront, "store_settings", "menu_store"),
            AdminShortcut("Peran & PIN", "Akses & PIN", Icons.Default.Security, "roles_pin", "menu_roles_pin"),
            AdminShortcut("Backup & Drive", "JSON/CSV & Sync", Icons.Default.CloudSync, "backup_drive", "menu_backup")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Compact Maroon Top Header Bar
        Surface(
            color = MaroonPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = storeName.ifBlank { "Sewa Outdoor" },
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Halo, $userName • ${Formatters.formatTanggalIndo(today)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaroonContainer
                    )
                }
                FilledTonalButton(
                    onClick = { onNavigate("catalog") },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PureWhite.copy(alpha = 0.16f),
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.testTag("dashboard_open_catalog_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Katalog")
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2x3 Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Omzet Hari Ini",
                    value = Formatters.formatRupiah(omzetHari),
                    subtitle = "${transactions.count { it.tanggalTransaksi == today }} trx hari ini",
                    icon = Icons.Default.Today,
                    accentColor = MaroonPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Omzet Bulan Ini",
                    value = Formatters.formatRupiah(omzetBulan),
                    subtitle = "${transactions.count { it.tanggalTransaksi.startsWith(monthPrefix) }} trx bulan ini",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Sedang Tersewa",
                    value = "$unitTersewa Unit",
                    subtitle = "${transactions.count { it.statusTransaksi == RentalStatus.AKTIF }} aktif",
                    icon = Icons.Default.Terrain,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("transactions") }
                )
                MetricStatCard(
                    title = "Total Transaksi",
                    value = "${transactions.size}",
                    subtitle = "${transactions.count { it.sisa > 0 }} belum lunas",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("transactions") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Terlambat Kembali",
                    value = "${terlambatList.size}",
                    subtitle = if (terlambatList.isEmpty()) "Semua tepat waktu" else "Perlu ditindaklanjuti",
                    icon = Icons.Default.WarningAmber,
                    accentColor = if (terlambatList.isEmpty()) SuccessGreen else DangerRed,
                    containerColor = if (terlambatList.isEmpty()) PureWhite else DangerContainer.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("transactions") }
                )
                MetricStatCard(
                    title = "Saldo Kas",
                    value = Formatters.formatRupiah(saldoKas),
                    subtitle = "Lihat Buku Kas",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = MaroonPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("cashbook") }
                )
            }

            // Overdue Rentals Alert List
            if (terlambatList.isNotEmpty()) {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Terlambat Kembali (${terlambatList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = DangerRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        terlambatList.take(3).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenTransaction(tx) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${tx.nomorTransaksi} • Batas ${Formatters.formatTanggalIndo(tx.selesaiSewa)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                StatusPill("Terlambat", DangerContainer, DangerRed)
                            }
                        }
                    }
                }
            }

            // Admin Quick Menus Grid
            Text(
                text = "Menu Manajemen Admin",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shortcuts.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { item ->
                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OutlineSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onNavigate(item.route) }
                                    .testTag(item.tag)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaroonContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = MaroonPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
