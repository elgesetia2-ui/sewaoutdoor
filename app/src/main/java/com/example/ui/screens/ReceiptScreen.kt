package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FeeType
import com.example.model.RentalTransaction
import com.example.model.StoreSettings
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.*
import kotlinx.coroutines.launch

@Composable
fun ReceiptScreen(
    tx: RentalTransaction,
    store: StoreSettings,
    onClose: () -> Unit,
    onMessage: (String) -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showOtherWaDialog by remember { mutableStateOf(false) }
    var paperWidthMm by remember { mutableIntStateOf(store.printerWidthMm) }
    var pairedDevices by remember { mutableStateOf<List<PairedPrinterDevice>>(emptyList()) }
    var btStatusMsg by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val btPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            val res = BluetoothPrinterManager.getPairedDevices(context)
            res.onSuccess {
                pairedDevices = it
                btStatusMsg = if (it.isEmpty()) "Belum ada printer Bluetooth yang dipasangkan (pair) di pengaturan HP." else null
            }.onFailure {
                btStatusMsg = it.localizedMessage
            }
            showBluetoothDialog = true
        } else {
            onMessage("Izin Bluetooth ditolak, tidak dapat mengakses printer thermal")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = "Struk Transaksi",
            subtitle = tx.nomorTransaksi,
            onBack = onClose
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual Thermal Receipt Card
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Maroon header stripe
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaroonPrimary, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = store.namaToko.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaroonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (store.alamat.isNotBlank()) {
                        Text(store.alamat, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (store.noHp.isNotBlank()) {
                        Text("HP: ${store.noHp}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineSubtle)

                    ReceiptKeyValue("No. Transaksi", tx.nomorTransaksi, mono = true)
                    ReceiptKeyValue("Tanggal", Formatters.formatTanggalIndo(tx.tanggalTransaksi))
                    ReceiptKeyValue("Kasir", tx.kasirName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Sewa", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        StatusPill(
                            text = tx.statusTransaksi.label,
                            containerColor = MaroonContainer,
                            contentColor = MaroonPrimary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineSubtle)

                    Text("Data Member & Jadwal", style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    ReceiptKeyValue("Penyewa", tx.customerName)
                    ReceiptKeyValue("No. HP", tx.customerPhone, mono = true)
                    ReceiptKeyValue("Member Sejak", Formatters.formatTanggalIndo(tx.memberSejak))
                    ReceiptKeyValue("Sewa Ke", "Sewa ke-${tx.sewaKeN}")
                    ReceiptKeyValue(
                        "Pinjam - Kembali",
                        "${Formatters.formatTanggalIndo(tx.mulaiSewa)} s/d ${Formatters.formatTanggalIndo(tx.selesaiSewa)} (${tx.durasiHari}H)"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineSubtle)

                    Text("Rincian Item", style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    tx.items.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${item.variantName}${if (item.sku.isNotBlank()) " • ${item.sku}" else ""}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = TextSecondary
                            )
                            if (item.isPackage && item.packageSummary.isNotBlank()) {
                                Text(
                                    text = "Isi Paket: ${item.packageSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.qty} x ${Formatters.formatRupiah(item.hargaSatuan)} (${item.kolomHargaLabel})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = Formatters.formatRupiah(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineSubtle)

                    ReceiptKeyValue("Subtotal Item", Formatters.formatRupiah(tx.subtotalItems))
                    if (tx.diskonAktif && tx.nominalDiskon > 0) {
                        val promo = if (tx.kodePromo.isNotBlank()) " (${tx.kodePromo})" else ""
                        ReceiptKeyValue("Diskon$promo", "-${Formatters.formatRupiah(tx.nominalDiskon)}")
                    }
                    if (tx.biayaTambahan.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        tx.biayaTambahan.forEach { fee ->
                            val label = if (fee.jenis == FeeType.GANTI_RUGI || fee.keterangan.isNotBlank()) {
                                "${fee.jenis.label} (${fee.keterangan})"
                            } else {
                                fee.jenis.label
                            }
                            ReceiptKeyValue(label, Formatters.formatRupiah(fee.nominal))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", style = MaterialTheme.typography.titleMedium, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        Text(Formatters.formatRupiah(tx.total), style = MaterialTheme.typography.titleLarge, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ReceiptKeyValue("Dibayar (${tx.metodePembayaran.label})", Formatters.formatRupiah(tx.sudahDibayar))
                    ReceiptKeyValue("Sisa", Formatters.formatRupiah(tx.sisa))
                    ReceiptKeyValue("Status Bayar", tx.statusPembayaran.label)
                    ReceiptKeyValue(
                        "Jaminan",
                        if (tx.detailJaminan.isBlank()) tx.jenisJaminan.label else "${tx.jenisJaminan.label} - ${tx.detailJaminan}"
                    )
                    if (tx.catatan.isNotBlank()) {
                        ReceiptKeyValue("Catatan", tx.catatan)
                    }

                    if (store.catatanStruk.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = OutlineSubtle)
                        Text(
                            text = store.catatanStruk,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Action Buttons (Bluetooth Print, WhatsApp, Salin, Unduh)
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Aksi Struk", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                    Button(
                        onClick = {
                            if (BluetoothPrinterManager.hasBluetoothPermission(context)) {
                                val res = BluetoothPrinterManager.getPairedDevices(context)
                                res.onSuccess {
                                    pairedDevices = it
                                    btStatusMsg = if (it.isEmpty()) "Belum ada printer Bluetooth terpasang (paired)." else null
                                }.onFailure {
                                    btStatusMsg = it.localizedMessage
                                }
                                showBluetoothDialog = true
                            } else {
                                btPermLauncher.launch(BluetoothPrinterManager.requiredPermissions())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("print_bluetooth_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print Bluetooth (ESC/POS 58/80mm)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val waNum = Formatters.toWhatsAppNumber(tx.customerPhone)
                                val waText = Uri.encode(ReceiptFormatter.buildWhatsAppText(tx, store))
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=$waText"))
                                runCatching { context.startActivity(intent) }
                                    .onFailure { onMessage("Tidak dapat membuka WhatsApp") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("send_whatsapp_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim WhatsApp")
                        }

                        OutlinedButton(
                            onClick = { showOtherWaDialog = true },
                            border = BorderStroke(1.dp, SuccessGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("send_whatsapp_other_button")
                        ) {
                            Text("Nomor Lain")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val txt = ReceiptFormatter.buildPlainText(tx, store, if (paperWidthMm >= 80) 48 else 32)
                                clipboard.setText(AnnotatedString(txt))
                                onMessage("Teks struk disalin ke clipboard")
                            },
                            border = BorderStroke(1.dp, MaroonPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_receipt_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Struk")
                        }

                        OutlinedButton(
                            onClick = {
                                val res = ReceiptFormatter.saveReceiptPng(context, tx, store)
                                onMessage(res.getOrElse { it.localizedMessage ?: "Gagal mengunduh struk" })
                            },
                            border = BorderStroke(1.dp, MaroonPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("download_receipt_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh Struk")
                        }
                    }
                }
            }
        }
    }

    // Bluetooth Printer Dialog
    if (showBluetoothDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPrinting) showBluetoothDialog = false },
            title = { Text("Print Bluetooth ESC/POS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ukuran Kertas Thermal:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paperWidthMm == 58,
                            onClick = { paperWidthMm = 58 },
                            label = { Text("58 mm") }
                        )
                        FilterChip(
                            selected = paperWidthMm == 80,
                            onClick = { paperWidthMm = 80 },
                            label = { Text("80 mm") }
                        )
                    }
                    if (!btStatusMsg.isNullOrBlank()) {
                        Surface(
                            color = WarningContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = btStatusMsg!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    if (pairedDevices.isNotEmpty()) {
                        Text("Pilih Printer Terpasang:", style = MaterialTheme.typography.labelMedium)
                        pairedDevices.forEach { dev ->
                            Surface(
                                color = SurfaceMuted,
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    if (!isPrinting) {
                                        isPrinting = true
                                        scope.launch {
                                            val bytes = ReceiptFormatter.buildEscPosBytes(tx, store, paperWidthMm)
                                            val res = BluetoothPrinterManager.printEscPos(context, dev.address, bytes)
                                            isPrinting = false
                                            onMessage(res.getOrElse { it.localizedMessage ?: "Gagal mencetak" })
                                            if (res.isSuccess) showBluetoothDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaroonPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(dev.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(dev.address, style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBluetoothDialog = false }) { Text("Tutup") }
            }
        )
    }

    // Send WhatsApp to Other Number Dialog
    if (showOtherWaDialog) {
        var otherPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOtherWaDialog = false },
            title = { Text("Kirim Struk ke Nomor Lain") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan nomor WhatsApp tujuan (contoh: 0812xxxx atau 62812xxxx):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = otherPhone,
                        onValueChange = { otherPhone = it },
                        label = { Text("Nomor WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val norm = Formatters.normalizePhone(otherPhone)
                        if (norm.length < 8) {
                            onMessage("Nomor WhatsApp tidak valid")
                        } else {
                            val waNum = Formatters.toWhatsAppNumber(norm)
                            val waText = Uri.encode(ReceiptFormatter.buildWhatsAppText(tx, store))
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=$waText"))
                            runCatching { context.startActivity(intent) }
                                .onFailure { onMessage("Tidak dapat membuka WhatsApp") }
                            showOtherWaDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtherWaDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ReceiptKeyValue(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
            else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
