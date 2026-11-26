package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.ColorSwatchCircle
import com.example.ui.components.QrisPaymentDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream

private data class CartEntry(
    val isPackage: Boolean,
    val productId: String,
    val productName: String,
    val variantId: String,
    val variantName: String,
    val sku: String,
    val warnaHex: String,
    val qty: Int,
    val packageSummary: String = ""
)

@Composable
fun AddTransactionScreen(
    products: List<Product>,
    packages: List<RentalPackage>,
    members: List<Member>,
    qrisSettings: QrisSettings,
    activePaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.CASH, PaymentMethod.QRIS, PaymentMethod.TRANSFER),
    preselectedItem: Pair<Product, ProductVariant>?,
    onConsumePreselected: () -> Unit,
    onBack: () -> Unit,
    onSaved: (RentalTransaction) -> Unit,
    onMessage: (String) -> Unit,
    onSaveTransaction: (
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
    ) -> Unit
) {
    BackHandler { onBack() }

    val today = remember { Formatters.todayIso() }
    var mulaiSewa by remember { mutableStateOf(today) }
    var selesaiSewa by remember { mutableStateOf(today) }
    var satuan by remember { mutableStateOf(RentalUnit.HARI) }
    var activePresetDays by remember { mutableIntStateOf(1) }

    val durasiHari = remember(mulaiSewa, selesaiSewa) {
        Formatters.daysBetweenInclusive(mulaiSewa, selesaiSewa)
    }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var showMemberSuggestions by remember { mutableStateOf(false) }

    val cartEntries = remember { mutableStateListOf<CartEntry>() }
    val additionalFees = remember { mutableStateListOf<AdditionalFee>() }

    var diskonAktif by remember { mutableStateOf(false) }
    var diskonTipe by remember { mutableStateOf(DiscountType.RUPIAH) }
    var diskonNilaiText by remember { mutableStateOf("") }
    var kodePromo by remember { mutableStateOf("") }

    var statusPembayaran by remember { mutableStateOf(PaymentStatus.LUNAS) }
    var sudahDibayarText by remember { mutableStateOf("0") }
    var metodePembayaran by remember(activePaymentMethods) {
        mutableStateOf(activePaymentMethods.firstOrNull() ?: PaymentMethod.CASH)
    }
    var statusTransaksi by remember { mutableStateOf(RentalStatus.AKTIF) }

    var jenisJaminan by remember { mutableStateOf(CollateralType.KTP) }
    var catatan by remember { mutableStateOf("") }

    val context = LocalContext.current
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showPackagePicker by remember { mutableStateOf(false) }
    var showAddFeeDialog by remember { mutableStateOf(false) }
    var showQrisModal by remember { mutableStateOf(false) }
    var showPaymentInfoSheet by remember { mutableStateOf(false) }
    var jaminanImageUri by remember { mutableStateOf<Uri?>(null) }
    var jaminanSavedPath by remember { mutableStateOf("") }

    val jaminanPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            jaminanImageUri = uri
            runCatching {
                val dir = File(context.filesDir, "jaminan").apply { mkdirs() }
                val dest = File(dir, "jaminan_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                jaminanSavedPath = dest.name
            }
        }
    }

    val jaminanPreviewBitmap = remember(jaminanImageUri) {
        jaminanImageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    // Pre-fill item if navigated from Katalog
    LaunchedEffect(preselectedItem) {
        if (preselectedItem != null) {
            val (p, v) = preselectedItem
            val existingIdx = cartEntries.indexOfFirst { !it.isPackage && it.variantId == v.id }
            if (existingIdx >= 0) {
                val cur = cartEntries[existingIdx]
                cartEntries[existingIdx] = cur.copy(qty = cur.qty + 1)
            } else {
                cartEntries.add(
                    CartEntry(
                        isPackage = false,
                        productId = p.id,
                        productName = p.nama,
                        variantId = v.id,
                        variantName = v.namaVarian,
                        sku = v.sku,
                        warnaHex = v.warnaHex,
                        qty = 1
                    )
                )
            }
            onConsumePreselected()
        }
    }

    // Recalculate TransactionItems dynamically whenever durasiHari or cartEntries change
    val calculatedItems: List<TransactionItem> = remember(cartEntries.toList(), products, packages, durasiHari) {
        cartEntries.map { entry ->
            if (entry.isPackage) {
                val pkg = packages.firstOrNull { it.id == entry.productId }
                val (unitPrice, label) = if (pkg != null) {
                    Formatters.calculatePackagePriceForDuration(pkg, durasiHari)
                } else 0L to "Paket"
                TransactionItem(
                    productId = entry.productId,
                    productName = entry.productName,
                    variantId = entry.variantId,
                    variantName = entry.variantName,
                    sku = entry.sku,
                    warnaHex = entry.warnaHex,
                    qty = entry.qty,
                    hargaSatuan = unitPrice,
                    kolomHargaLabel = label,
                    subtotal = unitPrice * entry.qty,
                    isPackage = true,
                    packageSummary = entry.packageSummary
                )
            } else {
                val product = products.firstOrNull { it.id == entry.productId }
                val variant = (product?.variants ?: products.flatMap { it.variants }).firstOrNull { it.id == entry.variantId }
                val (unitPrice, label) = if (product != null && variant != null) {
                    Formatters.calculateProductVariantPrice(product, variant, durasiHari)
                } else if (variant != null) {
                    Formatters.calculateVariantPriceForDuration(variant, durasiHari)
                } else 0L to "Tarif 1H"
                TransactionItem(
                    productId = entry.productId,
                    productName = entry.productName,
                    variantId = entry.variantId,
                    variantName = entry.variantName,
                    sku = entry.sku,
                    warnaHex = entry.warnaHex,
                    qty = entry.qty,
                    hargaSatuan = unitPrice,
                    kolomHargaLabel = label,
                    subtotal = unitPrice * entry.qty
                )
            }
        }
    }

    val subtotalItems = remember(calculatedItems) { calculatedItems.sumOf { it.subtotal } }
    val diskonInputVal = diskonNilaiText.toLongOrNull() ?: 0L
    val nominalDiskon = remember(diskonAktif, diskonTipe, diskonInputVal, subtotalItems) {
        if (!diskonAktif) 0L
        else when (diskonTipe) {
            DiscountType.RUPIAH -> diskonInputVal.coerceIn(0L, subtotalItems)
            DiscountType.PERSEN -> ((subtotalItems * diskonInputVal.coerceIn(0L, 100L)) / 100L)
        }
    }
    val totalBiayaTambahan = remember(additionalFees.toList()) { additionalFees.sumOf { it.nominal } }
    val grandTotal = remember(subtotalItems, nominalDiskon, totalBiayaTambahan) {
        (subtotalItems - nominalDiskon + totalBiayaTambahan).coerceAtLeast(0L)
    }

    // Keep Sudah Dibayar synced for Lunas / Terhutang
    LaunchedEffect(statusPembayaran, grandTotal) {
        when (statusPembayaran) {
            PaymentStatus.LUNAS -> sudahDibayarText = grandTotal.toString()
            PaymentStatus.TERHUTANG -> sudahDibayarText = "0"
            PaymentStatus.SEBAGIAN -> Unit
        }
    }

    val sudahDibayar = (sudahDibayarText.toLongOrNull() ?: 0L).coerceIn(0L, grandTotal)
    val sisa = (grandTotal - sudahDibayar).coerceAtLeast(0L)

    val matchingMembers = remember(customerName, customerPhone, members) {
        val qName = customerName.trim().lowercase()
        val qPhone = customerPhone.filter { it.isDigit() }
        if (qName.isEmpty() && qPhone.isEmpty()) emptyList()
        else members.filter {
            (qName.isNotEmpty() && it.nama.lowercase().contains(qName)) ||
                (qPhone.isNotEmpty() && it.noHp.contains(qPhone))
        }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = "Tambah Transaksi",
            subtitle = "Durasi $durasiHari Hari • ${calculatedItems.sumOf { it.qty }} item",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. PERIODE SEWA (Date only, NO time)
            FormSectionCard(title = "Periode Sewa (Tanggal Saja)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateSelectBox(
                        label = "Mulai Sewa",
                        isoDate = mulaiSewa,
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f).testTag("mulai_sewa_button")
                    )
                    DateSelectBox(
                        label = "Selesai Sewa",
                        isoDate = selesaiSewa,
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f).testTag("selesai_sewa_button")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Satuan:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RentalUnit.entries.forEach { u ->
                            FilterChip(
                                selected = satuan == u,
                                onClick = {
                                    satuan = u
                                    val days = u.daysPerUnit
                                    selesaiSewa = Formatters.addDaysIso(mulaiSewa, days - 1)
                                    activePresetDays = days
                                },
                                label = { Text(u.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                            )
                        }
                    }
                }

                // Quick 1-7 Hari Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..7).forEach { d ->
                        FilterChip(
                            selected = durasiHari == d,
                            onClick = {
                                satuan = RentalUnit.HARI
                                activePresetDays = d
                                selesaiSewa = Formatters.addDaysIso(mulaiSewa, d - 1)
                            },
                            label = { Text("$d Hari") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonContainer, selectedLabelColor = MaroonPrimary)
                        )
                    }
                }

                Surface(
                    color = MaroonContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Durasi: $durasiHari Hari",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaroonPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 2. PELANGGAN + AUTO-MEMBER + AUTOCOMPLETE + NORMALISASI HP
            FormSectionCard(title = "Pelanggan (Auto-Member)") {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        showMemberSuggestions = true
                    },
                    label = { Text("Nama Pelanggan*") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaroonPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                        showMemberSuggestions = true
                    },
                    label = { Text("No. HP / WhatsApp* (62/+62 otomatis jadi 08)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaroonPrimary) },
                    trailingIcon = {
                        val norm = Formatters.normalizePhone(customerPhone)
                        if (norm.isNotEmpty() && norm != customerPhone) {
                            TextButton(onClick = { customerPhone = norm }) {
                                Text("Jadi $norm", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
                )

                if (showMemberSuggestions && matchingMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Member Lama Ditemukan:", style = MaterialTheme.typography.labelSmall, color = MaroonPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    matchingMembers.forEach { m ->
                        Surface(
                            color = SurfaceMuted,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    customerName = m.nama
                                    customerPhone = m.noHp
                                    showMemberSuggestions = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(m.nama, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${m.noHp} • Sewa ke-${m.totalSewa + 1}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                StatusPill("Pilih", MaroonContainer, MaroonPrimary)
                            }
                        }
                    }
                }
            }

            // 3. PRODUK & PAKET PRODUK
            FormSectionCard(title = "Item & Paket Sewa") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showProductPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("pick_product_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih Produk")
                    }
                    OutlinedButton(
                        onClick = { showPackagePicker = true },
                        border = BorderStroke(1.dp, MaroonPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("pick_package_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paket Produk")
                    }
                }

                if (calculatedItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    calculatedItems.forEachIndexed { idx, item ->
                        val entry = cartEntries[idx]
                        Surface(
                            color = AppBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!item.isPackage) {
                                    ColorSwatchCircle(hex = item.warnaHex, selected = false, size = 18.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${item.variantName}${if (item.sku.isNotBlank()) " • ${item.sku}" else ""}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("${Formatters.formatRupiah(item.hargaSatuan)} (${item.kolomHargaLabel})", style = MaterialTheme.typography.labelSmall, color = MaroonPrimary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (entry.qty > 1) cartEntries[idx] = entry.copy(qty = entry.qty - 1)
                                            else cartEntries.removeAt(idx)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (entry.qty > 1) Icons.Default.RemoveCircleOutline else Icons.Default.DeleteOutline,
                                            contentDescription = "Kurangi",
                                            tint = DangerRed
                                        )
                                    }
                                    Text("${entry.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(
                                        onClick = { cartEntries[idx] = entry.copy(qty = entry.qty + 1) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = MaroonPrimary)
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Item", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(Formatters.formatRupiah(subtotalItems), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. DISKON
            FormSectionCard(title = "Diskon & Kode Promo") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifkan Diskon", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = diskonAktif,
                        onCheckedChange = { diskonAktif = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary)
                    )
                }
                if (diskonAktif) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiscountType.entries.forEach { dt ->
                            FilterChip(
                                selected = diskonTipe == dt,
                                onClick = { diskonTipe = dt },
                                label = { Text(dt.label) }
                            )
                        }
                        OutlinedTextField(
                            value = diskonNilaiText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) diskonNilaiText = it },
                            label = { Text(if (diskonTipe == DiscountType.RUPIAH) "Potongan (Rp)" else "Persen (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kodePromo,
                        onValueChange = { kodePromo = it },
                        label = { Text("Kode Promo (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nominalDiskon > 0) {
                        Text(
                            text = "Potongan Diskon: -${Formatters.formatRupiah(nominalDiskon)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 5. BIAYA TAMBAHAN (Ganti Rugi wajib keterangan kerusakan)
            FormSectionCard(title = "Biaya Tambahan") {
                OutlinedButton(
                    onClick = { showAddFeeDialog = true },
                    border = BorderStroke(1.dp, MaroonPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("add_fee_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Biaya (Ganti Rugi / Denda / Lainnya)")
                }

                additionalFees.forEachIndexed { idx, fee ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fee.jenis.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (fee.keterangan.isNotBlank()) {
                                Text(fee.keterangan, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Formatters.formatRupiah(fee.nominal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                            IconButton(onClick = { additionalFees.removeAt(idx) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 6. PEMBAYARAN & STATUS TRANSAKSI
            FormSectionCard(title = "Pembayaran & Status Transaksi") {
                Text("Status Pembayaran:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentStatus.entries.forEach { ps ->
                        FilterChip(
                            selected = statusPembayaran == ps,
                            onClick = { statusPembayaran = ps },
                            label = { Text(ps.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sudahDibayarText,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() }) {
                                sudahDibayarText = it
                                statusPembayaran = PaymentStatus.SEBAGIAN
                            }
                        },
                        label = { Text("Sudah Dibayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("sudah_dibayar_input")
                    )
                    Surface(
                        color = if (sisa > 0) DangerContainer else SuccessContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Sisa Tagihan", style = MaterialTheme.typography.labelSmall, color = if (sisa > 0) DangerRed else SuccessGreen)
                            Text(Formatters.formatRupiah(sisa), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (sisa > 0) DangerRed else SuccessGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Status Transaksi:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RentalStatus.entries.forEach { rs ->
                        FilterChip(
                            selected = statusTransaksi == rs,
                            onClick = { statusTransaksi = rs },
                            label = { Text(rs.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonPrimary, selectedLabelColor = PureWhite)
                        )
                    }
                }
            }

            // 7. GAMBAR JAMINAN & CATATAN (SESUAI GAMBAR REFERENSI)
            FormSectionCard(title = "Jaminan & Catatan") {
                Text("Jenis Jaminan:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CollateralType.entries.forEach { ct ->
                        FilterChip(
                            selected = jenisJaminan == ct,
                            onClick = { jenisJaminan = ct },
                            label = { Text(ct.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaroonContainer, selectedLabelColor = MaroonPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Gambar Jaminan",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (jaminanPreviewBitmap != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = jaminanPreviewBitmap.asImageBitmap(),
                            contentDescription = "Foto Jaminan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    jaminanPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp).testTag("change_jaminan_file_button")
                            ) {
                                Text("Ganti File")
                            }
                            OutlinedButton(
                                onClick = {
                                    jaminanImageUri = null
                                    jaminanSavedPath = ""
                                },
                                border = BorderStroke(1.dp, DangerRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("Hapus")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageNotSupported,
                            contentDescription = "Belum ada file jaminan",
                            tint = TextSecondary.copy(alpha = 0.55f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum ada file yang ditambahkan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                jaminanPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("add_jaminan_file_button")
                        ) {
                            Text(
                                text = "Tambah File",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Catatan (Opsional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    placeholder = { Text("Tambahkan catatan...") },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("catatan_input")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // STICKY FOOTER: TOTAL + TOMBOL BAYAR MAROON
        Surface(
            color = PureWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Tagihan", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = Formatters.formatRupiah(grandTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaroonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val normPhone = Formatters.normalizePhone(customerPhone)
                        when {
                            customerName.trim().isEmpty() -> onMessage("Nama pelanggan wajib diisi")
                            normPhone.length < 8 -> onMessage("No. HP pelanggan wajib diisi (min 8 digit)")
                            calculatedItems.isEmpty() -> onMessage("Pilih minimal 1 produk atau paket sewa")
                            else -> {
                                showPaymentInfoSheet = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .widthIn(min = 140.dp)
                        .testTag("pay_transaction_button")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bayar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showPaymentInfoSheet) {
        PaymentInfoBottomSheet(
            subtotal = subtotalItems,
            totalBiayaTambahan = totalBiayaTambahan,
            nominalDiskon = nominalDiskon,
            grandTotal = grandTotal,
            statusPembayaran = statusPembayaran,
            sudahDibayar = sudahDibayar,
            sisa = sisa,
            selectedMethod = metodePembayaran,
            activeMethods = activePaymentMethods,
            onSelectMethod = { pm ->
                metodePembayaran = pm
                if (pm == PaymentMethod.QRIS && grandTotal > 0L) {
                    showQrisModal = true
                }
            },
            onShowQris = { showQrisModal = true },
            onDismiss = { showPaymentInfoSheet = false },
            onConfirmSave = {
                showPaymentInfoSheet = false
                val normPhone = Formatters.normalizePhone(customerPhone)
                val finalDetailJaminan = if (jaminanSavedPath.isNotBlank()) "Foto: $jaminanSavedPath" else ""
                onSaveTransaction(
                    mulaiSewa,
                    selesaiSewa,
                    durasiHari,
                    satuan,
                    customerName,
                    normPhone,
                    calculatedItems,
                    additionalFees.toList(),
                    diskonAktif,
                    diskonTipe,
                    diskonInputVal,
                    kodePromo,
                    subtotalItems,
                    nominalDiskon,
                    totalBiayaTambahan,
                    grandTotal,
                    statusPembayaran,
                    sudahDibayar,
                    sisa,
                    metodePembayaran,
                    statusTransaksi,
                    jenisJaminan,
                    finalDetailJaminan,
                    catatan.trim(),
                    onSaved
                )
            }
        )
    }

    if (showStartDatePicker) {
        RentalDatePickerDialog(
            initialIsoDate = mulaiSewa,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { newStart ->
                mulaiSewa = newStart
                if (selesaiSewa < newStart) {
                    selesaiSewa = newStart
                }
            }
        )
    }

    if (showEndDatePicker) {
        RentalDatePickerDialog(
            initialIsoDate = selesaiSewa,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { newEnd ->
                selesaiSewa = if (newEnd < mulaiSewa) mulaiSewa else newEnd
            }
        )
    }

    if (showProductPicker) {
        ProductPickerDialog(
            products = products,
            durasiHari = durasiHari,
            onDismiss = { showProductPicker = false },
            onAddVariant = { prod, variant, qty ->
                val idx = cartEntries.indexOfFirst { !it.isPackage && it.variantId == variant.id }
                if (idx >= 0) {
                    val cur = cartEntries[idx]
                    cartEntries[idx] = cur.copy(qty = cur.qty + qty)
                } else {
                    cartEntries.add(
                        CartEntry(
                            isPackage = false,
                            productId = prod.id,
                            productName = prod.nama,
                            variantId = variant.id,
                            variantName = variant.namaVarian,
                            sku = variant.sku,
                            warnaHex = variant.warnaHex,
                            qty = qty
                        )
                    )
                }
            }
        )
    }

    if (showPackagePicker) {
        PackagePickerDialog(
            packages = packages,
            durasiHari = durasiHari,
            onDismiss = { showPackagePicker = false },
            onAddPackage = { pkg ->
                val summary = pkg.items.joinToString(", ") { "${it.productName} x${it.qty}" }
                val idx = cartEntries.indexOfFirst { it.isPackage && it.productId == pkg.id }
                if (idx >= 0) {
                    val cur = cartEntries[idx]
                    cartEntries[idx] = cur.copy(qty = cur.qty + 1)
                } else {
                    cartEntries.add(
                        CartEntry(
                            isPackage = true,
                            productId = pkg.id,
                            productName = pkg.nama,
                            variantId = pkg.id,
                            variantName = "Paket Bundling",
                            sku = "PKT",
                            warnaHex = "#571331",
                            qty = 1,
                            packageSummary = summary
                        )
                    )
                }
            }
        )
    }

    if (showAddFeeDialog) {
        AddFeeDialog(
            onDismiss = { showAddFeeDialog = false },
            onAddFee = { fee -> additionalFees.add(fee) }
        )
    }

    if (showQrisModal) {
        val qrisAmount = if (sudahDibayar > 0L) sudahDibayar else grandTotal
        QrisPaymentDialog(
            qrisSettings = qrisSettings,
            amount = qrisAmount,
            referenceLabel = customerName.ifBlank { "Transaksi Baru" },
            onDismiss = { showQrisModal = false },
            onConfirmPayment = {
                statusPembayaran = PaymentStatus.LUNAS
                sudahDibayarText = grandTotal.toString()
                metodePembayaran = PaymentMethod.QRIS
                onMessage("Pembayaran QRIS dikonfirmasi Lunas")
            },
            onMessage = onMessage
        )
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaroonPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DateSelectBox(
    label: String,
    isoDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceMuted,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, contentDescription = label, tint = MaroonPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(Formatters.formatTanggalIndo(isoDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
