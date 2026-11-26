package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.UUID

@Composable
fun CashBookScreen(
    transactions: List<RentalTransaction>,
    cashEntries: List<CashEntry>,
    mutations: List<CashMutation>,
    onBack: () -> Unit,
    onAddCashEntry: (CashEntry) -> Unit,
    onDeleteCashEntry: (String) -> Unit
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf<CashEntryType?>(null) }

    val pemasukanSewa = remember(transactions) { transactions.sumOf { it.sudahDibayar } }
    val pemasukanLain = remember(cashEntries) {
        cashEntries.filter { it.tipe == CashEntryType.PEMASUKAN_LAIN }.sumOf { it.nominal }
    }
    val totalPengeluaran = remember(cashEntries) {
        cashEntries.filter { it.tipe == CashEntryType.PENGELUARAN }.sumOf { it.nominal }
    }
    val saldoKas = pemasukanSewa + pemasukanLain - totalPengeluaran
    val labaRugi = pemasukanSewa + pemasukanLain - totalPengeluaran

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = "Buku Kas",
            subtitle = "Mutasi kas & Laba Rugi sederhana",
            onBack = onBack
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                MetricStatCard(
                    title = "Saldo Kas Saat Ini",
                    value = Formatters.formatRupiah(saldoKas),
                    subtitle = "Sewa + Pemasukan Lain - Pengeluaran",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = MaroonPrimary,
                    containerColor = MaroonContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = CashEntryType.PEMASUKAN_LAIN },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pemasukan Lain")
                    }
                    Button(
                        onClick = { showAddDialog = CashEntryType.PENGELUARAN },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pengeluaran")
                    }
                }
            }

            // Simple Profit & Loss Card
            item {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Laba Rugi Sederhana", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        CashSummaryRow("Pendapatan Sewa Diterima", Formatters.formatRupiah(pemasukanSewa), SuccessGreen)
                        CashSummaryRow("Pemasukan Lain-lain", Formatters.formatRupiah(pemasukanLain), SuccessGreen)
                        CashSummaryRow("Total Pengeluaran", "-${Formatters.formatRupiah(totalPengeluaran)}", DangerRed)
                        HorizontalDivider(color = OutlineSubtle)
                        CashSummaryRow(
                            "Laba / Rugi Bersih",
                            Formatters.formatRupiah(labaRugi),
                            if (labaRugi >= 0) MaroonPrimary else DangerRed,
                            bold = true
                        )
                    }
                }
            }

            item {
                Text("Mutasi Kas (${mutations.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }

            if (mutations.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Belum ada mutasi kas",
                        description = "Pembayaran transaksi dan catatan kas manual akan tampil di sini."
                    )
                }
            } else {
                items(mutations, key = { it.id }) { mut ->
                    val isManual = !mut.id.startsWith("mut_")
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mut.isMasuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (mut.isMasuk) SuccessGreen else DangerRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mut.sumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(mut.deskripsi, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${Formatters.formatTanggalIndo(mut.tanggal)} • ${mut.metode.label}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (mut.isMasuk) "+" else "-"}${Formatters.formatRupiah(mut.nominal)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (mut.isMasuk) SuccessGreen else DangerRed,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isManual) {
                                    IconButton(onClick = { onDeleteCashEntry(mut.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showAddDialog?.let { entryType ->
        var kategori by remember { mutableStateOf(if (entryType == CashEntryType.PEMASUKAN_LAIN) "Modal / Lainnya" else "Operasional") }
        var deskripsi by remember { mutableStateOf("") }
        var nominalText by remember { mutableStateOf("") }
        var metode by remember { mutableStateOf(PaymentMethod.CASH) }

        AlertDialog(
            onDismissRequest = { showAddDialog = null },
            title = { Text("Tambah ${entryType.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = kategori, onValueChange = { kategori = it }, label = { Text("Kategori") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deskripsi, onValueChange = { deskripsi = it }, label = { Text("Keterangan*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = nominalText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) nominalText = it },
                        label = { Text("Nominal (Rp)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { pm ->
                            FilterChip(selected = metode == pm, onClick = { metode = pm }, label = { Text(pm.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nom = nominalText.toLongOrNull() ?: 0L
                        if (deskripsi.trim().isNotEmpty() && nom > 0L) {
                            onAddCashEntry(
                                CashEntry(
                                    id = "cash_${UUID.randomUUID().toString().take(8)}",
                                    tanggal = Formatters.todayIso(),
                                    tipe = entryType,
                                    kategori = kategori.trim().ifBlank { entryType.label },
                                    deskripsi = deskripsi.trim(),
                                    nominal = nom,
                                    metode = metode,
                                    createdAtMillis = System.currentTimeMillis()
                                )
                            )
                            showAddDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CashSummaryRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, color = TextPrimary)
        Text(value, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
    }
}
