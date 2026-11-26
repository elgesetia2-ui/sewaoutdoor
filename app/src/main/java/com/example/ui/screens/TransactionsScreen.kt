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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrisPaymentDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun TransactionsScreen(
    transactions: List<RentalTransaction>,
    session: SessionState,
    qrisSettings: QrisSettings,
    onOpenCatalog: () -> Unit,
    onOpenReceipt: (RentalTransaction) -> Unit,
    onUpdateTransaction: (RentalTransaction, RentalStatus, PaymentStatus, Long, PaymentMethod) -> Unit,
    onMessage: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<RentalStatus?>(null) }
    var paymentFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var detailTx by remember { mutableStateOf<RentalTransaction?>(null) }
    var showQrisForTx by remember { mutableStateOf<RentalTransaction?>(null) }

    val today = remember { Formatters.todayIso() }

    val scopedTransactions = remember(transactions, session) {
        if (session.role == UserRole.KASIR) {
            transactions.filter { it.kasirName.equals(session.userName, ignoreCase = true) }
        } else {
            transactions
        }
    }

    val filtered = remember(scopedTransactions, search, statusFilter, paymentFilter) {
        val q = search.trim().lowercase()
        scopedTransactions.filter { tx ->
            val stOk = statusFilter == null || tx.statusTransaksi == statusFilter
            val payOk = paymentFilter == null || tx.statusPembayaran == paymentFilter
            val qOk = q.isEmpty() ||
                tx.nomorTransaksi.lowercase().contains(q) ||
                tx.customerName.lowercase().contains(q) ||
                tx.customerPhone.contains(q)
            stOk && payOk && qOk
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = if (session.role == UserRole.KASIR) "Transaksi Saya" else "Daftar Transaksi",
            subtitle = "${filtered.size} transaksi • ${session.userName}",
            trailing = {
                FilledTonalButton(
                    onClick = onOpenCatalog,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaroonContainer,
                        contentColor = MaroonPrimary
                    ),
                    modifier = Modifier.testTag("tx_open_catalog_button")
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Katalog")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari nomor transaksi, nama, atau HP...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaroonPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Semua Status") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                )
                RentalStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text(st.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                    )
                }
                PaymentStatus.entries.forEach { ps ->
                    FilterChip(
                        selected = paymentFilter == ps,
                        onClick = { paymentFilter = if (paymentFilter == ps) null else ps },
                        label = { Text(ps.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonContainer, selectedLabelColor = MaroonPrimary)
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineSubtle)

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.ReceiptLong,
                    title = "Belum ada transaksi",
                    description = "Tekan tombol [+] maroon di bawah untuk membuat transaksi sewa baru."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { tx ->
                    val isOverdue = tx.statusTransaksi != RentalStatus.SELESAI && tx.selesaiSewa < today
                    TransactionCard(
                        tx = tx,
                        isOverdue = isOverdue,
                        onClick = { detailTx = tx },
                        onOpenReceipt = { onOpenReceipt(tx) }
                    )
                }
            }
        }
    }

    detailTx?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            onDismiss = { detailTx = null },
            onOpenReceipt = {
                detailTx = null
                onOpenReceipt(tx)
            },
            onOpenQris = {
                detailTx = null
                showQrisForTx = tx
            },
            onSaveUpdate = { newStatus, newPayStatus, newPaid, newMethod ->
                onUpdateTransaction(tx, newStatus, newPayStatus, newPaid, newMethod)
                detailTx = null
            }
        )
    }

    showQrisForTx?.let { tx ->
        val amountToPay = if (tx.sisa > 0L) tx.sisa else tx.total
        QrisPaymentDialog(
            qrisSettings = qrisSettings,
            amount = amountToPay,
            referenceLabel = "${tx.nomorTransaksi} • ${tx.customerName}",
            onDismiss = { showQrisForTx = null },
            onConfirmPayment = {
                onUpdateTransaction(
                    tx,
                    tx.statusTransaksi,
                    PaymentStatus.LUNAS,
                    tx.total,
                    PaymentMethod.QRIS
                )
            },
            onMessage = onMessage
        )
    }
}

@Composable
private fun TransactionCard(
    tx: RentalTransaction,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onOpenReceipt: () -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isOverdue) DangerRed.copy(alpha = 0.5f) else OutlineSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.nomorTransaksi,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaroonPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isOverdue) {
                        StatusPill("Terlambat", DangerContainer, DangerRed)
                    }
                    val (bg, fg) = when (tx.statusTransaksi) {
                        RentalStatus.BOOKING -> InfoContainer to InfoBlue
                        RentalStatus.AKTIF -> WarningContainer to WarningAmber
                        RentalStatus.SELESAI -> SuccessContainer to SuccessGreen
                    }
                    StatusPill(tx.statusTransaksi.label, bg, fg)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${tx.customerName} (${tx.customerPhone})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "${Formatters.formatTanggalIndo(tx.mulaiSewa)} s/d ${Formatters.formatTanggalIndo(tx.selesaiSewa)} • ${tx.durasiHari} Hari • ${tx.items.sumOf { it.qty }} item",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.formatRupiah(tx.total),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaroonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    val payText = if (tx.sisa > 0) {
                        "${tx.statusPembayaran.label} • Sisa ${Formatters.formatRupiah(tx.sisa)}"
                    } else {
                        "${tx.statusPembayaran.label} (${tx.metodePembayaran.label})"
                    }
                    Text(
                        text = payText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tx.sisa > 0) DangerRed else SuccessGreen
                    )
                }

                OutlinedButton(
                    onClick = onOpenReceipt,
                    border = BorderStroke(1.dp, MaroonPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Struk", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    tx: RentalTransaction,
    onDismiss: () -> Unit,
    onOpenReceipt: () -> Unit,
    onOpenQris: () -> Unit,
    onSaveUpdate: (RentalStatus, PaymentStatus, Long, PaymentMethod) -> Unit
) {
    var status by remember { mutableStateOf(tx.statusTransaksi) }
    var payStatus by remember { mutableStateOf(tx.statusPembayaran) }
    var method by remember { mutableStateOf(tx.metodePembayaran) }
    var paidText by remember { mutableStateOf(tx.sudahDibayar.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(tx.nomorTransaksi, style = MaterialTheme.typography.titleMedium, color = MaroonPrimary)
                Text("${tx.customerName} • ${Formatters.formatRupiah(tx.total)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Status Transaksi", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RentalStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                Text("Status Pembayaran", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentStatus.entries.forEach { ps ->
                        FilterChip(
                            selected = payStatus == ps,
                            onClick = {
                                payStatus = ps
                                paidText = when (ps) {
                                    PaymentStatus.TERHUTANG -> "0"
                                    PaymentStatus.LUNAS -> tx.total.toString()
                                    PaymentStatus.SEBAGIAN -> paidText
                                }
                            },
                            label = { Text(ps.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = paidText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) paidText = it },
                    label = { Text("Sudah Dibayar (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Metode Pembayaran", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = method == pm,
                            onClick = { method = pm },
                            label = { Text(pm.label) }
                        )
                    }
                }

                if (tx.sisa > 0) {
                    OutlinedButton(
                        onClick = onOpenQris,
                        border = BorderStroke(1.dp, MaroonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayar Sisa via QRIS (${Formatters.formatRupiah(tx.sisa)})")
                    }
                }

                TextButton(
                    onClick = onOpenReceipt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka & Cetak / Share Struk")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = paidText.toLongOrNull() ?: 0L
                    onSaveUpdate(status, payStatus, paid, method)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
