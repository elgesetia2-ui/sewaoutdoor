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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.components.ColorSwatchCircle
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDatePickerDialog(
    initialIsoDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = Formatters.isoToMillis(initialIsoDate)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val ms = state.selectedDateMillis
                    if (ms != null) {
                        onDateSelected(Formatters.millisToIso(ms))
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text("Pilih Tanggal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun ProductPickerDialog(
    products: List<Product>,
    durasiHari: Int,
    onDismiss: () -> Unit,
    onAddVariant: (Product, ProductVariant, Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var pickedProduct by remember { mutableStateOf<Product?>(null) }
    var pickedVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var qty by remember { mutableIntStateOf(1) }

    val filtered = remember(products, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) products
        else products.filter { p ->
            p.nama.lowercase().contains(q) ||
                p.kategori.lowercase().contains(q) ||
                p.variants.any { it.sku.lowercase().contains(q) || it.warnaNama.lowercase().contains(q) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(14.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaroonPrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pilih Produk & Varian", style = MaterialTheme.typography.titleMedium, color = PureWhite, fontWeight = FontWeight.Bold)
                        Text("Harga otomatis untuk durasi $durasiHari Hari", style = MaterialTheme.typography.bodySmall, color = MaroonContainer)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = PureWhite)
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Cari nama alat, SKU, atau warna...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaroonPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        val isExpanded = pickedProduct?.id == prod.id
                        Surface(
                            color = if (isExpanded) MaroonContainer.copy(alpha = 0.35f) else AppBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isExpanded) MaroonPrimary else OutlineSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    pickedProduct = prod
                                    pickedVariant = prod.variants.firstOrNull { it.status == VariantStatus.TERSEDIA && it.stok > 0 }
                                        ?: prod.variants.firstOrNull()
                                    qty = 1
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${prod.kategori} • ${prod.variants.size} varian", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    val firstPrice = prod.variants.firstOrNull()?.let {
                                        Formatters.calculateProductVariantPrice(prod, it, durasiHari).first
                                    } ?: 0L
                                    Text(Formatters.formatRupiah(firstPrice), style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Wajib Pilih Varian & Warna:", style = MaterialTheme.typography.labelMedium, color = MaroonPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    prod.variants.forEach { v ->
                                        val selected = pickedVariant?.id == v.id
                                        val (unitPrice, priceLabel) = Formatters.calculateProductVariantPrice(prod, v, durasiHari)
                                        val available = v.status == VariantStatus.TERSEDIA && v.stok > 0
                                        Surface(
                                            color = if (selected) PureWhite else SurfaceMuted,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaroonPrimary else OutlineSubtle),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(enabled = available) {
                                                    pickedVariant = v
                                                    qty = 1
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ColorSwatchCircle(hex = v.warnaHex, selected = selected, size = 20.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${v.namaVarian} (${v.ukuran})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text("${v.sku} • Stok: ${v.stok}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = if (available) TextSecondary else DangerRed)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(Formatters.formatRupiah(unitPrice), style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                                                    Text(priceLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                }
                                            }
                                        }
                                    }

                                    pickedVariant?.let { v ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { if (qty > 1) qty-- },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurangi", tint = MaroonPrimary)
                                                }
                                                Text("$qty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                                IconButton(
                                                    onClick = { if (qty < v.stok.coerceAtLeast(1)) qty++ },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = MaroonPrimary)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    onAddVariant(prod, v, qty)
                                                    onDismiss()
                                                },
                                                enabled = v.status == VariantStatus.TERSEDIA && v.stok > 0,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                                            ) {
                                                Text("Tambahkan")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackagePickerDialog(
    packages: List<RentalPackage>,
    durasiHari: Int,
    onDismiss: () -> Unit,
    onAddPackage: (RentalPackage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Paket Produk ($durasiHari Hari)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val active = packages.filter { it.aktif }
                if (active.isEmpty()) {
                    Text("Belum ada paket produk aktif.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    active.forEach { pkg ->
                        val (price, label) = Formatters.calculatePackagePriceForDuration(pkg, durasiHari)
                        Surface(
                            color = SurfaceMuted,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            onClick = {
                                onAddPackage(pkg)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pkg.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(Formatters.formatRupiah(price), style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                                }
                                Text(pkg.deskripsi, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                StatusPill(label, MaroonContainer, MaroonPrimary, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun AddFeeDialog(
    onDismiss: () -> Unit,
    onAddFee: (AdditionalFee) -> Unit
) {
    var selectedType by remember { mutableStateOf(FeeType.GANTI_RUGI) }
    var keterangan by remember { mutableStateOf("") }
    var nominalText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Biaya Tambahan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Jenis Biaya:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeeType.entries.forEach { ft ->
                        FilterChip(
                            selected = selectedType == ft,
                            onClick = { selectedType = ft; errorMsg = null },
                            label = { Text(ft.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it; errorMsg = null },
                    label = {
                        Text(
                            if (selectedType == FeeType.GANTI_RUGI) "Keterangan Kerusakan (WAJIB)*"
                            else "Keterangan (Opsional)"
                        )
                    },
                    placeholder = {
                        Text(if (selectedType == FeeType.GANTI_RUGI) "Misal: Tenda robek / Frame patah" else "Detail biaya")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) { nominalText = it; errorMsg = null } },
                    label = { Text("Nominal (Rp)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nom = nominalText.toLongOrNull() ?: 0L
                    if (selectedType.requiresDetail && keterangan.trim().isEmpty()) {
                        errorMsg = "Untuk Ganti Rugi, keterangan kerusakan wajib diisi (misal: Tenda robek)"
                    } else if (nom <= 0L) {
                        errorMsg = "Nominal harus lebih dari Rp 0"
                    } else {
                        onAddFee(
                            AdditionalFee(
                                id = "fee_${UUID.randomUUID().toString().take(6)}",
                                jenis = selectedType,
                                keterangan = keterangan.trim(),
                                nominal = nom
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text("Tambah Biaya")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentInfoBottomSheet(
    subtotal: Long,
    totalBiayaTambahan: Long,
    nominalDiskon: Long,
    grandTotal: Long,
    statusPembayaran: PaymentStatus,
    sudahDibayar: Long,
    sisa: Long,
    selectedMethod: PaymentMethod,
    activeMethods: List<PaymentMethod> = listOf(PaymentMethod.CASH, PaymentMethod.QRIS, PaymentMethod.TRANSFER),
    onSelectMethod: (PaymentMethod) -> Unit,
    onShowQris: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val effectiveActiveMethods = remember(activeMethods) {
        if (activeMethods.isNotEmpty()) activeMethods
        else listOf(PaymentMethod.CASH, PaymentMethod.QRIS, PaymentMethod.TRANSFER)
    }

    LaunchedEffect(effectiveActiveMethods, selectedMethod) {
        if (selectedMethod !in effectiveActiveMethods && effectiveActiveMethods.isNotEmpty()) {
            onSelectMethod(effectiveActiveMethods.first())
        }
    }

    var selectedBukuKas by remember { mutableStateOf<String?>(null) }
    var bukuKasExpanded by remember { mutableStateOf(false) }
    val bukuKasList = listOf("Kas Utama", "Kas Operasional", "Bank BCA", "Bank Mandiri")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Info Pembayaran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                effectiveActiveMethods.forEach { method ->
                    val isSelected = selectedMethod == method
                    Surface(
                        color = if (isSelected) MaroonContainer.copy(alpha = 0.45f) else PureWhite,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaroonPrimary else OutlineSubtle
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectMethod(method) }
                            .testTag("payment_method_${method.name.lowercase()}")
                    ) {
                        Text(
                            text = method.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) MaroonPrimary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (selectedMethod == PaymentMethod.QRIS) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onShowQris,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaroonContainer,
                        contentColor = MaroonPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_qris_from_sheet_button")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tampilkan Kode QRIS (${Formatters.formatRupiah(sudahDibayar.takeIf { it > 0 } ?: grandTotal)})")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Buku Kas (Opsional)",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = bukuKasExpanded,
                        onExpandedChange = { bukuKasExpanded = !bukuKasExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBukuKas ?: "Pilih Buku Kas",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bukuKasExpanded)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = bukuKasExpanded,
                            onDismissRequest = { bukuKasExpanded = false }
                        ) {
                            bukuKasList.forEach { bk ->
                                DropdownMenuItem(
                                    text = { Text(bk) },
                                    onClick = {
                                        selectedBukuKas = bk
                                        bukuKasExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    color = PureWhite,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedBukuKas = null }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Buku Kas",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OutlineSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            PaymentSummaryLine("Subtotal", Formatters.formatRupiah(subtotal))
            if (totalBiayaTambahan > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                PaymentSummaryLine("Biaya Tambahan", Formatters.formatRupiah(totalBiayaTambahan))
            }
            Spacer(modifier = Modifier.height(6.dp))
            PaymentSummaryLine("Diskon", "-${Formatters.formatRupiah(nominalDiskon)}")

            if (statusPembayaran != PaymentStatus.LUNAS) {
                Spacer(modifier = Modifier.height(6.dp))
                PaymentSummaryLine("Sudah Dibayar", Formatters.formatRupiah(sudahDibayar))
                Spacer(modifier = Modifier.height(6.dp))
                PaymentSummaryLine("Sisa Tagihan", Formatters.formatRupiah(sisa))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = OutlineSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatRupiah(grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onConfirmSave,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 160.dp)
                        .testTag("confirm_save_transaction_button")
                ) {
                    Text("Simpan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
