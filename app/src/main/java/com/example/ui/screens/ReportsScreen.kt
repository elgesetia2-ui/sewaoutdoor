package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.ExportImportManager
import com.example.util.Formatters

private enum class AdminReportTab(val label: String) {
    TRANSAKSI("Transaksi"),
    PEMBAYARAN("Pembayaran"),
    PRODUK("Produk Tersewa"),
    MEMBER("Member"),
    BUKU_KAS("Buku Kas"),
    LABA_RUGI("Laba Rugi")
}

@Composable
fun ReportsScreen(
    session: SessionState,
    transactions: List<RentalTransaction>,
    members: List<Member>,
    cashEntries: List<CashEntry>,
    onExportCsv: (String, String) -> Unit
) {
    val today = remember { Formatters.todayIso() }
    var startDate by remember { mutableStateOf(Formatters.addDaysIso(today, -30)) }
    var endDate by remember { mutableStateOf(today) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AdminReportTab.TRANSAKSI) }

    val scopedTx = remember(transactions, session, startDate, endDate) {
        transactions.filter { tx ->
            val dateOk = tx.tanggalTransaksi in startDate..endDate
            val roleOk = if (session.role == UserRole.KASIR) {
                tx.kasirName.equals(session.userName, ignoreCase = true)
            } else true
            dateOk && roleOk
        }
    }

    val scopedCash = remember(cashEntries, startDate, endDate) {
        cashEntries.filter { it.tanggal in startDate..endDate }
    }

    val totalOmzet = remember(scopedTx) { scopedTx.sumOf { it.total } }
    val totalDibayar = remember(scopedTx) { scopedTx.sumOf { it.sudahDibayar } }
    val totalPiutang = remember(scopedTx) { scopedTx.sumOf { it.sisa } }

    val byMethod = remember(scopedTx) {
        PaymentMethod.entries.associateWith { pm ->
            scopedTx.filter { it.metodePembayaran == pm }.sumOf { it.sudahDibayar }
        }
    }

    val productRank = remember(scopedTx) {
        scopedTx.flatMap { it.items }
            .groupBy { "${it.productName} (${it.variantName})" }
            .map { (name, items) ->
                Triple(name, items.sumOf { it.qty }, items.sumOf { it.subtotal })
            }
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = if (session.role == UserRole.KASIR) "Laporan Penjualan" else "Laporan Lengkap",
            subtitle = if (session.role == UserRole.KASIR) "Kasir: ${session.userName}" else "Semua transaksi & keuangan",
            trailing = {
                Button(
                    onClick = {
                        val csv = ExportImportManager.buildTransactionsCsv(scopedTx)
                        val name = if (session.role == UserRole.KASIR) "Laporan_Kasir" else "Laporan_SewaOutdoor"
                        onExportCsv(name, csv)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    modifier = Modifier.testTag("report_export_csv_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV")
                }
            }
        )

        // Date Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateChipBox("Dari", startDate, { pickStart = true }, Modifier.weight(1f))
            DateChipBox("Sampai", endDate, { pickEnd = true }, Modifier.weight(1f))
        }

        if (session.role == UserRole.ADMIN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminReportTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineSubtle)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Kasir Sales Report OR Admin Tab Content
            if (session.role == UserRole.KASIR || selectedTab == AdminReportTab.TRANSAKSI) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "Total Omzet",
                            value = Formatters.formatRupiah(totalOmzet),
                            subtitle = "${scopedTx.size} transaksi",
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Produk Terjual",
                            value = "${scopedTx.sumOf { t -> t.items.sumOf { it.qty } }} Unit",
                            subtitle = "Diterima ${Formatters.formatRupiah(totalDibayar)}",
                            icon = Icons.Default.Inventory,
                            accentColor = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    ReportPaymentMethodCard(byMethod, totalPiutang)
                }
                item {
                    Text("Produk Terlaris / Terjual (${productRank.size})", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary)
                }
                items(productRank) { (name, qty, subtotal) ->
                    ReportRowCard(title = name, subtitle = "Terjual $qty unit", value = Formatters.formatRupiah(subtotal))
                }
                if (session.role == UserRole.ADMIN) {
                    item {
                        Text("Daftar Transaksi (${scopedTx.size})", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary)
                    }
                    items(scopedTx, key = { it.id }) { tx ->
                        ReportRowCard(
                            title = "${tx.nomorTransaksi} • ${tx.customerName}",
                            subtitle = "${Formatters.formatTanggalIndo(tx.tanggalTransaksi)} • ${tx.statusTransaksi.label} • ${tx.statusPembayaran.label}",
                            value = Formatters.formatRupiah(tx.total)
                        )
                    }
                }
            } else if (selectedTab == AdminReportTab.PEMBAYARAN) {
                item { ReportPaymentMethodCard(byMethod, totalPiutang) }
                items(scopedTx.filter { it.sisa > 0 }) { tx ->
                    ReportRowCard(
                        title = "${tx.customerName} (${tx.nomorTransaksi})",
                        subtitle = "Total ${Formatters.formatRupiah(tx.total)} • Bayar ${Formatters.formatRupiah(tx.sudahDibayar)}",
                        value = "Sisa ${Formatters.formatRupiah(tx.sisa)}"
                    )
                }
            } else if (selectedTab == AdminReportTab.PRODUK) {
                items(productRank) { (name, qty, subtotal) ->
                    ReportRowCard(title = name, subtitle = "Disewa $qty unit", value = Formatters.formatRupiah(subtotal))
                }
            } else if (selectedTab == AdminReportTab.MEMBER) {
                items(members.sortedByDescending { it.totalBelanja }) { m ->
                    ReportRowCard(
                        title = m.nama,
                        subtitle = "${m.noHp} • ${m.totalSewa}x sewa",
                        value = Formatters.formatRupiah(m.totalBelanja)
                    )
                }
            } else if (selectedTab == AdminReportTab.BUKU_KAS) {
                items(scopedCash) { c ->
                    ReportRowCard(
                        title = "${c.tipe.label}: ${c.kategori}",
                        subtitle = "${Formatters.formatTanggalIndo(c.tanggal)} • ${c.deskripsi}",
                        value = Formatters.formatRupiah(c.nominal)
                    )
                }
            } else if (selectedTab == AdminReportTab.LABA_RUGI) {
                val masukLain = scopedCash.filter { it.tipe == CashEntryType.PEMASUKAN_LAIN }.sumOf { it.nominal }
                val keluar = scopedCash.filter { it.tipe == CashEntryType.PENGELUARAN }.sumOf { it.nominal }
                val laba = totalDibayar + masukLain - keluar
                item {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ringkasan Laba Rugi", style = MaterialTheme.typography.titleMedium, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                            ReportRowInline("Pendapatan Sewa Diterima", Formatters.formatRupiah(totalDibayar))
                            ReportRowInline("Pemasukan Lain", Formatters.formatRupiah(masukLain))
                            ReportRowInline("Total Pengeluaran", "-${Formatters.formatRupiah(keluar)}")
                            HorizontalDivider(color = OutlineSubtle)
                            ReportRowInline("Laba / Rugi Bersih", Formatters.formatRupiah(laba), bold = true)
                        }
                    }
                }
            }
        }
    }

    if (pickStart) {
        RentalDatePickerDialog(startDate, { pickStart = false }, { startDate = it })
    }
    if (pickEnd) {
        RentalDatePickerDialog(endDate, { pickEnd = false }, { endDate = it })
    }
}

@Composable
private fun ReportPaymentMethodCard(
    byMethod: Map<PaymentMethod, Long>,
    totalPiutang: Long
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Metode Pembayaran (Diterima)", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
            PaymentMethod.entries.forEach { pm ->
                ReportRowInline(pm.label, Formatters.formatRupiah(byMethod[pm] ?: 0L))
            }
            HorizontalDivider(color = OutlineSubtle)
            ReportRowInline("Sisa / Piutang Pelanggan", Formatters.formatRupiah(totalPiutang), bold = true)
        }
    }
}

@Composable
private fun ReportRowCard(title: String, subtitle: String, value: String) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaroonPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReportRowInline(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall)
        Text(value, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaroonPrimary)
    }
}

@Composable
private fun DateChipBox(label: String, iso: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        color = SurfaceMuted,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaroonPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(Formatters.formatTanggalIndo(iso), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
