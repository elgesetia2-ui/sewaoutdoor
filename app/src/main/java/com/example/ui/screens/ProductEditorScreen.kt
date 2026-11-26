package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ColorSwatchCircle
import com.example.ui.theme.*
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val defaultCategories = listOf(
    "Tenda",
    "Tas & Carrier",
    "Sleeping Bag",
    "Matras",
    "Masak",
    "Penerangan",
    "Furniture",
    "Aksesoris",
    "Lainnya"
)

private val editorPresetColors = listOf(
    "Maroon" to "#571331",
    "Olive" to "#556B2F",
    "Hitam" to "#1F2937",
    "Biru Navy" to "#1E3A8A",
    "Merah" to "#B91C1C",
    "Oranye" to "#EA580C",
    "Kuning" to "#D97706",
    "Hijau Sage" to "#4D7C0F",
    "Abu Slate" to "#475569",
    "Coklat" to "#78350F"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorScreen(
    initial: Product?,
    existingCategories: List<String>,
    onBack: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val productId = remember { initial?.id ?: "prod_${UUID.randomUUID().toString().take(8)}" }

    var photoPath by remember { mutableStateOf(initial?.photoPath ?: "") }
    var nama by remember { mutableStateOf(initial?.nama ?: "") }

    val allCategories = remember(existingCategories) {
        (defaultCategories + existingCategories).distinct().sorted()
    }
    var kategori by remember {
        mutableStateOf(initial?.kategori?.ifBlank { "Sleeping Bag" } ?: "Sleeping Bag")
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showCustomCategoryInput by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    var isDisewa by remember { mutableStateOf(initial?.isDisewa ?: true) }
    var isDijual by remember { mutableStateOf(initial?.isDijual ?: false) }

    val primaryVariant = remember(initial) { initial?.variants?.firstOrNull() }
    var hargaSewaText by remember { mutableStateOf((primaryVariant?.harga1H ?: 50000L).toString()) }
    var hargaJualText by remember { mutableStateOf(initial?.hargaJual?.let { if (it > 0) it.toString() else "" } ?: "") }
    var satuanPenyewaan by remember { mutableStateOf(initial?.satuan ?: ProductPriceUnit.HARI) }
    var deskripsi by remember { mutableStateOf(initial?.deskripsi ?: "") }
    var gunakanStok by remember { mutableStateOf(initial?.gunakanStok ?: true) }
    var stokText by remember { mutableStateOf((primaryVariant?.stok ?: 1).toString()) }
    var produkTersedia by remember {
        mutableStateOf((primaryVariant?.status ?: VariantStatus.TERSEDIA) == VariantStatus.TERSEDIA)
    }

    val packageTiers = remember {
        mutableStateListOf<ProductPriceTier>().apply {
            val savedTiers = initial?.customTiers.orEmpty()
            if (savedTiers.isNotEmpty()) {
                addAll(savedTiers)
            } else if (primaryVariant != null) {
                val basePrices = listOf(
                    2 to primaryVariant.harga2H,
                    3 to primaryVariant.harga3H,
                    4 to primaryVariant.harga4H,
                    5 to primaryVariant.harga5H,
                    6 to primaryVariant.harga6H
                )
                basePrices.forEach { (durasi, price) ->
                    if (price > 0L) {
                        add(
                            ProductPriceTier(
                                id = "tier_${UUID.randomUUID().toString().take(8)}",
                                durasi = durasi,
                                satuan = ProductPriceUnit.HARI,
                                harga = price
                            )
                        )
                    }
                }
            }
        }
    }

    val variants = remember {
        mutableStateListOf<ProductVariant>().apply {
            if (initial != null && initial.variants.isNotEmpty()) addAll(initial.variants)
            else {
                val p = Formatters.calculateAutoPrices(50000L)
                add(
                    ProductVariant(
                        id = "var_${UUID.randomUUID().toString().take(8)}",
                        productId = productId,
                        namaVarian = "Standar",
                        sku = "OUT-STD",
                        warnaNama = "Maroon",
                        warnaHex = "#571331",
                        ukuran = "Standar",
                        stok = 1,
                        harga1H = p[0], harga2H = p[1], harga3H = p[2],
                        harga4H = p[3], harga5H = p[4], harga6H = p[5],
                        status = VariantStatus.TERSEDIA
                    )
                )
            }
        }
    }

    var showVariantSection by remember { mutableStateOf(false) }
    var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
    var addingVariant by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val dir = File(context.filesDir, "product_photos").apply { mkdirs() }
                val dest = File(dir, "prod_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                photoPath = dest.name
            }
        }
    }

    val photoBitmap = remember(photoPath) {
        if (photoPath.isBlank()) null
        else runCatching {
            val file = File(File(context.filesDir, "product_photos"), photoPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }

    Scaffold(
        containerColor = PureWhite,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(color = PureWhite, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("product_editor_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = if (initial == null) "Tambah Produk" else "Ubah Produk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    if (initial != null && onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Produk",
                                tint = DangerRed
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanName = nama.trim()
                            if (cleanName.isEmpty()) {
                                errorMsg = "Nama produk wajib diisi"
                                return@Button
                            }
                            val basePrice = hargaSewaText.toLongOrNull() ?: 0L
                            val stockVal = if (gunakanStok) (stokText.toIntOrNull() ?: 0) else 999
                            val targetStatus = if (produkTersedia) VariantStatus.TERSEDIA else VariantStatus.NONAKTIF
                            val autoPrices = Formatters.calculateAutoPrices(basePrice)

                            fun tierPriceOrAuto(day: Int, autoFallback: Long): Long {
                                val match = packageTiers.firstOrNull {
                                    it.satuan == ProductPriceUnit.HARI && it.durasi == day && it.harga > 0L
                                }
                                return match?.harga ?: autoFallback
                            }

                            val updatedVariants = variants.mapIndexed { idx, v ->
                                if (idx == 0) {
                                    v.copy(
                                        stok = stockVal,
                                        status = targetStatus,
                                        harga1H = basePrice,
                                        harga2H = tierPriceOrAuto(2, autoPrices[1]),
                                        harga3H = tierPriceOrAuto(3, autoPrices[2]),
                                        harga4H = tierPriceOrAuto(4, autoPrices[3]),
                                        harga5H = tierPriceOrAuto(5, autoPrices[4]),
                                        harga6H = tierPriceOrAuto(6, autoPrices[5])
                                    )
                                } else {
                                    v.copy(
                                        status = if (produkTersedia) v.status else VariantStatus.NONAKTIF
                                    )
                                }
                            }

                            val finalCat = if (showCustomCategoryInput && customCategoryText.isNotBlank()) {
                                customCategoryText.trim()
                            } else {
                                kategori.trim().ifBlank { "Lainnya" }
                            }

                            onSave(
                                Product(
                                    id = productId,
                                    nama = cleanName,
                                    kategori = finalCat,
                                    deskripsi = deskripsi.trim(),
                                    variants = updatedVariants,
                                    photoPath = photoPath,
                                    isDisewa = isDisewa,
                                    isDijual = isDijual,
                                    satuan = satuanPenyewaan,
                                    gunakanStok = gunakanStok,
                                    customTiers = packageTiers.filter { it.durasi > 0 && it.harga > 0L },
                                    hargaJual = hargaJualText.toLongOrNull() ?: 0L,
                                    createdAtMillis = initial?.createdAtMillis ?: System.currentTimeMillis(),
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_product_button")
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. UPLOAD FOTO PRODUK (TOP CENTER)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .testTag("upload_product_photo_box")
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = if (photoBitmap != null) PureWhite else SurfaceMuted,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap.asImageBitmap(),
                                contentDescription = "Foto Produk",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Upload Foto Produk",
                                    tint = MaroonPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Upload Foto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Bottom-right edit badge matching screenshot
                    Surface(
                        color = PureWhite,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ubah Foto",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                if (photoPath.isNotBlank()) {
                    TextButton(
                        onClick = { photoPath = "" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Hapus Foto", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                    }
                }
            }

            if (errorMsg != null) {
                Surface(
                    color = DangerContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg!!,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 2. NAMA PRODUK
            LabeledField(label = "Nama Produk") {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it; errorMsg = null },
                    placeholder = { Text("Contoh: Sleeping Bag Polar") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
            }

            // 3. KATEGORI PRODUK (Dropdown)
            LabeledField(label = "Kategori Produk") {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = if (showCustomCategoryInput) "Kategori Baru..." else kategori,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    kategori = cat
                                    showCustomCategoryInput = false
                                    categoryExpanded = false
                                }
                            )
                        }
                        HorizontalDivider(color = OutlineSubtle)
                        DropdownMenuItem(
                            text = { Text("+ Kategori Baru", color = MaroonPrimary, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showCustomCategoryInput = true
                                categoryExpanded = false
                            }
                        )
                    }
                }
                if (showCustomCategoryInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        label = { Text("Nama Kategori Baru") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. JENIS PRODUK
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Jenis Produk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDisewa = !isDisewa }
                    ) {
                        Checkbox(
                            checked = isDisewa,
                            onCheckedChange = { isDisewa = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaroonPrimary)
                        )
                        Text("Disewa", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDijual = !isDijual }
                    ) {
                        Checkbox(
                            checked = isDijual,
                            onCheckedChange = { isDijual = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaroonPrimary)
                        )
                        Text("Dijual", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            // 5. HARGA SEWA & SATUAN PENYEWAAN (Conditional on isDisewa)
            AnimatedVisibility(visible = isDisewa) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledField(label = "Harga Sewa") {
                        OutlinedTextField(
                            value = hargaSewaText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) hargaSewaText = it },
                            prefix = { Text("Rp", color = TextPrimary, fontWeight = FontWeight.Medium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_base_price_input")
                        )
                    }

                    // 6. SATUAN PENYEWAAN
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Satuan Penyewaan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductPriceUnit.entries.forEach { unit ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { satuanPenyewaan = unit }
                                        .padding(end = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = satuanPenyewaan == unit,
                                        onClick = { satuanPenyewaan = unit },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaroonPrimary)
                                    )
                                    Text(
                                        text = unit.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6.5. HARGA JUAL (Conditional on isDijual)
            AnimatedVisibility(visible = isDijual) {
                LabeledField(label = "Harga Jual") {
                    OutlinedTextField(
                        value = hargaJualText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) hargaJualText = it },
                        prefix = { Text("Rp", color = TextPrimary, fontWeight = FontWeight.Medium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_sell_price_input")
                    )
                }
            }

            // 7. DESKRIPSI (OPSIONAL)
            LabeledField(label = "Deskripsi (Opsional)") {
                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 8. GUNAKAN STOK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gunakan Stok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = gunakanStok,
                    onCheckedChange = { gunakanStok = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = MaroonPrimary
                    )
                )
            }

            // 9. STOK
            if (gunakanStok) {
                LabeledField(label = "Stok") {
                    OutlinedTextField(
                        value = stokText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) stokText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 10. PRODUK TERSEDIA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Produk Tersedia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = produkTersedia,
                    onCheckedChange = { produkTersedia = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = MaroonPrimary
                    )
                )
            }

            AnimatedVisibility(visible = isDisewa) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider(color = OutlineSubtle)

                    // 11. HARGA PAKET
                    Text(
                        text = "Harga Paket",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    packageTiers.forEachIndexed { idx, tier ->
                        PackageTierRowItem(
                            tier = tier,
                            onUpdate = { updated -> packageTiers[idx] = updated },
                            onDelete = { packageTiers.removeAt(idx) }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val nextDurasi = (packageTiers.maxOfOrNull { it.durasi } ?: 1) + 1
                            val base = hargaSewaText.toLongOrNull() ?: 0L
                            packageTiers.add(
                                ProductPriceTier(
                                    id = "tier_${UUID.randomUUID().toString().take(8)}",
                                    durasi = nextDurasi,
                                    satuan = satuanPenyewaan,
                                    harga = base * nextDurasi
                                )
                            )
                        },
                        border = BorderStroke(1.dp, OutlineSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PureWhite,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_package_tier_button")
                    ) {
                        Text(
                            text = "Tambah Harga Paket",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Optional Variant & Color Management (Collapsed by default so UI matches screenshot)
            Surface(
                color = SurfaceMuted.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVariantSection = !showVariantSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Varian & Warna (${variants.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaroonPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Atur SKU, warna swatch, dan multi-varian",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = if (showVariantSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaroonPrimary
                        )
                    }

                    AnimatedVisibility(visible = showVariantSection) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { addingVariant = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaroonContainer,
                                    contentColor = MaroonPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Varian Warna")
                            }

                            variants.forEachIndexed { idx, v ->
                                Surface(
                                    color = PureWhite,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, OutlineSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ColorSwatchCircle(hex = v.warnaHex, selected = false, size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${v.namaVarian} • ${v.ukuran}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${v.sku} • Stok: ${v.stok}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        IconButton(onClick = { editingVariantIndex = idx }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Varian", tint = MaroonPrimary)
                                        }
                                        if (variants.size > 1) {
                                            IconButton(onClick = { variants.removeAt(idx) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Varian", tint = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (addingVariant || editingVariantIndex != null) {
        val cur = editingVariantIndex?.let { variants[it] }
        ProductVariantEditorDialog(
            productId = productId,
            initial = cur,
            onDismiss = {
                addingVariant = false
                editingVariantIndex = null
            },
            onSave = { v ->
                val idx = editingVariantIndex
                if (idx != null) variants[idx] = v else variants.add(v)
                addingVariant = false
                editingVariantIndex = null
            }
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Produk?") },
            text = { Text("Produk \"${initial?.nama ?: ""}\" akan dihapus dari daftar.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageTierRowItem(
    tier: ProductPriceTier,
    onUpdate: (ProductPriceTier) -> Unit,
    onDelete: () -> Unit
) {
    var durasiText by remember(tier.id) { mutableStateOf(if (tier.durasi > 0) tier.durasi.toString() else "") }
    var hargaText by remember(tier.id) { mutableStateOf(if (tier.harga > 0L) tier.harga.toString() else "") }
    var unitExpanded by remember { mutableStateOf(false) }

    val durasiNum = durasiText.toIntOrNull() ?: 0
    val hargaNum = hargaText.toLongOrNull() ?: 0L
    val hargaDasar = if (durasiNum > 0) hargaNum / durasiNum else 0L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LabeledField(label = "Durasi Sewa") {
                    OutlinedTextField(
                        value = durasiText,
                        onValueChange = { txt ->
                            if (txt.all { it.isDigit() }) {
                                durasiText = txt
                                onUpdate(tier.copy(durasi = txt.toIntOrNull() ?: 0))
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                LabeledField(label = "Satuan Penyewaan") {
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded }
                    ) {
                        OutlinedTextField(
                            value = tier.satuan.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            ProductPriceUnit.entries.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.label) },
                                    onClick = {
                                        onUpdate(tier.copy(satuan = u))
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Harga Sewa",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hargaText,
                    onValueChange = { txt ->
                        if (txt.all { it.isDigit() }) {
                            hargaText = txt
                            onUpdate(tier.copy(harga = txt.toLongOrNull() ?: 0L))
                        }
                    },
                    prefix = { Text("Rp", color = TextPrimary, fontWeight = FontWeight.Medium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Harga Paket",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        Surface(
            color = Color(0xFFE0F7FA),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Harga Dasar Paket ${durasiNum.coerceAtLeast(1)} ${tier.satuan.label} : ${Formatters.formatRupiahCompact(hargaDasar)}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color(0xFF00838F),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ProductVariantEditorDialog(
    productId: String,
    initial: ProductVariant?,
    onDismiss: () -> Unit,
    onSave: (ProductVariant) -> Unit
) {
    var namaVarian by remember { mutableStateOf(initial?.namaVarian ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var warnaNama by remember { mutableStateOf(initial?.warnaNama ?: "Maroon") }
    var warnaHex by remember { mutableStateOf(initial?.warnaHex ?: "#571331") }
    var ukuran by remember { mutableStateOf(initial?.ukuran ?: "Standar") }
    var stokText by remember { mutableStateOf((initial?.stok ?: 5).toString()) }
    var status by remember { mutableStateOf(initial?.status ?: VariantStatus.TERSEDIA) }

    var h1 by remember { mutableStateOf((initial?.harga1H ?: 50000L).toString()) }
    var h2 by remember { mutableStateOf((initial?.harga2H ?: 90000L).toString()) }
    var h3 by remember { mutableStateOf((initial?.harga3H ?: 125000L).toString()) }
    var h4 by remember { mutableStateOf((initial?.harga4H ?: 155000L).toString()) }
    var h5 by remember { mutableStateOf((initial?.harga5H ?: 185000L).toString()) }
    var h6 by remember { mutableStateOf((initial?.harga6H ?: 210000L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Varian" else "Edit Varian") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = namaVarian, onValueChange = { namaVarian = it }, label = { Text("Nama Varian*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sku, onValueChange = { sku = it.uppercase() }, label = { Text("SKU (KODE-WARNA)*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = warnaNama, onValueChange = { warnaNama = it }, label = { Text("Warna") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = warnaHex, onValueChange = { warnaHex = it }, label = { Text("Hex") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    editorPresetColors.forEach { (cName, cHex) ->
                        ColorSwatchCircle(
                            hex = cHex,
                            selected = warnaHex.equals(cHex, ignoreCase = true),
                            size = 24.dp,
                            onClick = {
                                warnaNama = cName
                                warnaHex = cHex
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ukuran, onValueChange = { ukuran = it }, label = { Text("Ukuran") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = stokText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) stokText = it },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Text("Status Varian:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VariantStatus.entries.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                    }
                }

                HorizontalDivider(color = OutlineSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Harga 6 Kolom (1H-6H)", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary)
                    FilledTonalButton(
                        onClick = {
                            val base = h1.toLongOrNull() ?: 0L
                            val auto = Formatters.calculateAutoPrices(base)
                            h2 = auto[1].toString()
                            h3 = auto[2].toString()
                            h4 = auto[3].toString()
                            h5 = auto[4].toString()
                            h6 = auto[5].toString()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaroonContainer, contentColor = MaroonPrimary)
                    ) {
                        Text("Isi Otomatis")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("1 Hari", h1, { h1 = it }, Modifier.weight(1f))
                    PriceField("2 Hari", h2, { h2 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("3 Hari", h3, { h3 = it }, Modifier.weight(1f))
                    PriceField("4 Hari", h4, { h4 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("5 Hari", h5, { h5 = it }, Modifier.weight(1f))
                    PriceField("6 Hari", h6, { h6 = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (namaVarian.trim().isNotEmpty() && sku.trim().isNotEmpty()) {
                        onSave(
                            ProductVariant(
                                id = initial?.id ?: "var_${UUID.randomUUID().toString().take(8)}",
                                productId = productId,
                                namaVarian = namaVarian.trim(),
                                sku = sku.trim().uppercase(),
                                warnaNama = warnaNama.trim().ifBlank { "Warna" },
                                warnaHex = warnaHex.trim().ifBlank { "#571331" },
                                ukuran = ukuran.trim().ifBlank { "Standar" },
                                stok = stokText.toIntOrNull() ?: 0,
                                harga1H = h1.toLongOrNull() ?: 0L,
                                harga2H = h2.toLongOrNull() ?: 0L,
                                harga3H = h3.toLongOrNull() ?: 0L,
                                harga4H = h4.toLongOrNull() ?: 0L,
                                harga5H = h5.toLongOrNull() ?: 0L,
                                harga6H = h6.toLongOrNull() ?: 0L,
                                status = status
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun PriceField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { ch -> ch.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
