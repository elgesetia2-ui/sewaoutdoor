package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import com.example.util.Formatters
import java.io.File
import java.util.UUID

private enum class ProductSortOption(val label: String) {
    DEFAULT("Urutan Default"),
    NAME_ASC("Nama A - Z"),
    NAME_DESC("Nama Z - A"),
    PRICE_LOW("Harga Terendah"),
    PRICE_HIGH("Harga Tertinggi"),
    STOCK_HIGH("Stok Terbanyak"),
    STOCK_LOW("Stok Terdikit")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsAdminScreen(
    products: List<Product>,
    onBack: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    BackHandler {
        if (isAddingNew || editingProduct != null) {
            isAddingNew = false
            editingProduct = null
        } else {
            onBack()
        }
    }

    if (isAddingNew || editingProduct != null) {
        val currentEdit = editingProduct
        ProductEditorScreen(
            initial = currentEdit,
            existingCategories = products.map { it.kategori }.distinct(),
            onBack = {
                isAddingNew = false
                editingProduct = null
            },
            onSave = { prod ->
                onSaveProduct(prod)
                isAddingNew = false
                editingProduct = null
            },
            onDelete = if (currentEdit != null) {
                {
                    onDeleteProduct(currentEdit.id)
                    editingProduct = null
                }
            } else null
        )
        return
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tersedia, 1 = Tidak Tersedia
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyDijual by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(ProductSortOption.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf(false) }
    var selectedProductForAction by remember { mutableStateOf<Product?>(null) }
    var viewingProductDetail by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val categories = remember(products) {
        products.map { it.kategori }.distinct().sorted()
    }

    val filtered = remember(products, selectedTab, search, selectedCategory, onlyDijual, sortOption) {
        val q = search.trim().lowercase()
        val base = products.filter { prod ->
            val isAvailable = prod.variants.any { it.status == VariantStatus.TERSEDIA }
            val tabOk = if (selectedTab == 0) isAvailable else !isAvailable
            val catOk = selectedCategory == null || prod.kategori == selectedCategory
            val dijualOk = !onlyDijual || prod.isDijual
            val queryOk = q.isEmpty() ||
                prod.nama.lowercase().contains(q) ||
                prod.deskripsi.lowercase().contains(q) ||
                prod.kategori.lowercase().contains(q) ||
                prod.variants.any { v -> v.sku.lowercase().contains(q) }
            tabOk && catOk && dijualOk && queryOk
        }
        when (sortOption) {
            ProductSortOption.DEFAULT -> base
            ProductSortOption.NAME_ASC -> base.sortedBy { it.nama.lowercase() }
            ProductSortOption.NAME_DESC -> base.sortedByDescending { it.nama.lowercase() }
            ProductSortOption.PRICE_LOW -> base.sortedBy { it.variants.firstOrNull()?.harga1H ?: 0L }
            ProductSortOption.PRICE_HIGH -> base.sortedByDescending { it.variants.firstOrNull()?.harga1H ?: 0L }
            ProductSortOption.STOCK_HIGH -> base.sortedByDescending { it.variants.sumOf { v -> v.stok } }
            ProductSortOption.STOCK_LOW -> base.sortedBy { it.variants.sumOf { v -> v.stok } }
        }
    }

    Scaffold(
        containerColor = PureWhite,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(modifier = Modifier.background(PureWhite)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("products_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Produk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PureWhite,
                    contentColor = MaroonPrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaroonPrimary,
                            height = 2.5.dp
                        )
                    },
                    divider = { HorizontalDivider(color = OutlineSubtle) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Tersedia",
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedTab == 0) MaroonPrimary else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Tidak Tersedia",
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedTab == 1) MaroonPrimary else TextSecondary
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (deleteMode) DangerRed else OutlineSubtle),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showMoreMenu = true }
                                .testTag("products_more_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu Lainnya",
                                    tint = if (deleteMode) DangerRed else TextPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (deleteMode) "Selesai Mode Hapus" else "Mode Hapus Produk")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = DangerRed
                                    )
                                },
                                onClick = {
                                    deleteMode = !deleteMode
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset Filter") },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                },
                                onClick = {
                                    search = ""
                                    selectedCategory = null
                                    onlyDijual = false
                                    sortOption = ProductSortOption.DEFAULT
                                    showMoreMenu = false
                                }
                            )
                        }
                    }

                    Button(
                        onClick = { isAddingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_product_button")
                    ) {
                        Text(
                            text = "Tambah",
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
                .background(PureWhite)
        ) {
            // Search + Sort + Category + Produk Dijual Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = {
                            Text(
                                text = "Cari nama atau deskripsi produk",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaroonPrimary,
                            unfocusedBorderColor = OutlineSubtle,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("products_search_input")
                    )

                    Box {
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (sortOption != ProductSortOption.DEFAULT) MaroonPrimary else OutlineSubtle
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .clickable { showSortMenu = true }
                                .testTag("products_sort_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Urutkan Produk",
                                    tint = if (sortOption != ProductSortOption.DEFAULT) MaroonPrimary else TextPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ProductSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt.label,
                                            fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == opt) MaroonPrimary else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        sortOption = opt
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Split Category Dropdown + Clear Button
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, OutlineSubtle, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showCategoryMenu = true }
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory ?: "Pilih Kategori Produk",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Pilih Kategori",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(OutlineSubtle)
                            )

                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight()
                                    .clickable { selectedCategory = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset Kategori",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Semua Kategori") },
                                onClick = {
                                    selectedCategory = null
                                    showCategoryMenu = false
                                }
                            )
                            HorizontalDivider(color = OutlineSubtle)
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat,
                                            color = if (selectedCategory == cat) MaroonPrimary else TextPrimary,
                                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Produk Dijual",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = onlyDijual,
                            onCheckedChange = { onlyDijual = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = MaroonPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = OutlineSubtle)

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = if (selectedTab == 0) "Belum ada produk tersedia" else "Tidak ada produk nonaktif",
                        description = "Tekan Tambah di bawah untuk menambah produk baru atau ubah filter pencarian."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { prod ->
                        val firstVar = prod.variants.firstOrNull()
                        val totalStok = prod.variants.sumOf { it.stok }
                        val thumbBitmap = remember(prod.photoPath) {
                            if (prod.photoPath.isBlank()) null
                            else runCatching {
                                val file = File(File(context.filesDir, "product_photos"), prod.photoPath)
                                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                            }.getOrNull()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProductForAction = prod }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SurfaceMuted,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OutlineSubtle),
                                modifier = Modifier.size(50.dp)
                            ) {
                                if (thumbBitmap != null) {
                                    Image(
                                        bitmap = thumbBitmap.asImageBitmap(),
                                        contentDescription = prod.nama,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaroonPrimary.copy(alpha = 0.65f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.nama,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatRupiahCompact(firstVar?.harga1H ?: 0L)}/${prod.satuan.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            if (deleteMode) {
                                IconButton(onClick = { onDeleteProduct(prod.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus Produk",
                                        tint = DangerRed
                                    )
                                }
                            } else if (prod.gunakanStok) {
                                Text(
                                    text = "Stok: $totalStok",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    val actionProd = selectedProductForAction
    if (actionProd != null) {
        val isCurrentlyAvailable = actionProd.variants.any { it.status == VariantStatus.TERSEDIA }
        ModalBottomSheet(
            onDismissRequest = { selectedProductForAction = null },
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp)
            ) {
                ProductActionSheetRow(
                    icon = Icons.Default.Visibility,
                    label = "Lihat Produk",
                    tint = TextPrimary,
                    onClick = {
                        viewingProductDetail = actionProd
                        selectedProductForAction = null
                    }
                )
                ProductActionSheetRow(
                    icon = Icons.Default.Edit,
                    label = "Ubah Produk",
                    tint = TextPrimary,
                    onClick = {
                        editingProduct = actionProd
                        selectedProductForAction = null
                    }
                )
                ProductActionSheetRow(
                    icon = if (isCurrentlyAvailable) Icons.Default.ToggleOff else Icons.Default.ToggleOn,
                    label = if (isCurrentlyAvailable) "Jadikan Tidak Tersedia" else "Jadikan Tersedia",
                    tint = TextPrimary,
                    onClick = {
                        val targetStatus = if (isCurrentlyAvailable) VariantStatus.NONAKTIF else VariantStatus.TERSEDIA
                        val updated = actionProd.copy(
                            variants = actionProd.variants.map { it.copy(status = targetStatus) }
                        )
                        onSaveProduct(updated)
                        selectedProductForAction = null
                    }
                )
                ProductActionSheetRow(
                    icon = Icons.Default.DeleteOutline,
                    label = "Hapus",
                    tint = Color(0xFFEA580C),
                    onClick = {
                        productToDelete = actionProd
                        selectedProductForAction = null
                    }
                )
            }
        }
    }

    val detailProd = viewingProductDetail
    if (detailProd != null) {
        val firstVar = detailProd.variants.firstOrNull()
        val isAvail = detailProd.variants.any { it.status == VariantStatus.TERSEDIA }
        val detailBitmap = remember(detailProd.photoPath) {
            if (detailProd.photoPath.isBlank()) null
            else runCatching {
                val file = File(File(context.filesDir, "product_photos"), detailProd.photoPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { viewingProductDetail = null },
            containerColor = PureWhite,
            title = {
                Text(
                    text = detailProd.nama,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (detailBitmap != null) {
                        Image(
                            bitmap = detailBitmap.asImageBitmap(),
                            contentDescription = detailProd.nama,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    DetailRow("Kategori", detailProd.kategori)
                    DetailRow(
                        "Harga Sewa",
                        "${Formatters.formatRupiahCompact(firstVar?.harga1H ?: 0L)}/${detailProd.satuan.label}"
                    )
                    DetailRow(
                        "Harga Jual",
                        if (detailProd.hargaJual > 0L) Formatters.formatRupiahCompact(detailProd.hargaJual) else "-"
                    )
                    DetailRow("Deskripsi", detailProd.deskripsi.ifBlank { "-" })
                    DetailRow(
                        "Stok",
                        if (detailProd.gunakanStok) "${detailProd.variants.sumOf { it.stok }}" else "-"
                    )
                    DetailRow("Status", if (isAvail) "Tersedia" else "Tidak Tersedia")
                    DetailRow("Waktu Dibuat", Formatters.formatTimestamp(detailProd.createdAtMillis))
                    DetailRow("Waktu Diperbarui", Formatters.formatTimestamp(detailProd.updatedAtMillis))

                    val displayTiers = remember(detailProd) {
                        if (detailProd.customTiers.isNotEmpty()) {
                            detailProd.customTiers
                        } else if (firstVar != null) {
                            listOf(
                                2 to firstVar.harga2H,
                                3 to firstVar.harga3H,
                                4 to firstVar.harga4H,
                                5 to firstVar.harga5H,
                                6 to firstVar.harga6H
                            ).filter { it.second > 0L }.map { (d, p) ->
                                ProductPriceTier(
                                    id = "v_$d",
                                    durasi = d,
                                    satuan = ProductPriceUnit.HARI,
                                    harga = p
                                )
                            }
                        } else {
                            emptyList()
                        }
                    }

                    if (displayTiers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Harga Paket",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            displayTiers.forEach { t ->
                                Text(
                                    text = "${Formatters.formatRupiahCompact(t.harga)}/${t.durasi} ${t.satuan.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewingProductDetail = null
                        editingProduct = detailProd
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("Ubah Produk")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingProductDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    val delProd = productToDelete
    if (delProd != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            containerColor = PureWhite,
            title = { Text("Hapus Produk?") },
            text = { Text("Yakin ingin menghapus \"${delProd.nama}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(delProd.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductActionSheetRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
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

@Composable
fun PackagesAdminScreen(
    packages: List<RentalPackage>,
    products: List<Product>,
    onBack: () -> Unit,
    onSavePackage: (RentalPackage) -> Unit,
    onDeletePackage: (String) -> Unit
) {
    BackHandler { onBack() }

    var editingPkg by remember { mutableStateOf<RentalPackage?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = "Paket Produk",
            subtitle = "${packages.size} paket bundling",
            onBack = onBack,
            trailing = {
                Button(
                    onClick = { addingNew = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paket")
                }
            }
        )

        if (packages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Layers,
                    title = "Belum ada paket produk",
                    description = "Buat paket bundling peralatan dengan harga khusus."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(packages, key = { it.id }) { pkg ->
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pkg.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(pkg.deskripsi, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Row {
                                    IconButton(onClick = { editingPkg = pkg }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaroonPrimary)
                                    }
                                    IconButton(onClick = { onDeletePackage(pkg.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = DangerRed)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1H: ${Formatters.formatRupiah(pkg.harga1H)} • 2H: ${Formatters.formatRupiah(pkg.harga2H)} • 3H: ${Formatters.formatRupiah(pkg.harga3H)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaroonPrimary
                            )
                            pkg.items.forEach { item ->
                                Text("• ${item.productName} (${item.variantName}) x${item.qty}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (addingNew || editingPkg != null) {
        PackageEditorDialog(
            initial = editingPkg,
            products = products,
            onDismiss = {
                addingNew = false
                editingPkg = null
            },
            onSave = { pkg ->
                onSavePackage(pkg)
                addingNew = false
                editingPkg = null
            }
        )
    }
}

@Composable
private fun PackageEditorDialog(
    initial: RentalPackage?,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (RentalPackage) -> Unit
) {
    var nama by remember { mutableStateOf(initial?.nama ?: "") }
    var deskripsi by remember { mutableStateOf(initial?.deskripsi ?: "") }
    var h1 by remember { mutableStateOf((initial?.harga1H ?: 80000L).toString()) }
    var h2 by remember { mutableStateOf((initial?.harga2H ?: 144000L).toString()) }
    var h3 by remember { mutableStateOf((initial?.harga3H ?: 200000L).toString()) }
    var h4 by remember { mutableStateOf((initial?.harga4H ?: 248000L).toString()) }
    var h5 by remember { mutableStateOf((initial?.harga5H ?: 296000L).toString()) }
    var h6 by remember { mutableStateOf((initial?.harga6H ?: 336000L).toString()) }
    val selectedItems = remember {
        mutableStateListOf<PackageItem>().apply {
            if (initial != null) addAll(initial.items)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Paket Produk" else "Edit Paket Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Paket*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deskripsi, onValueChange = { deskripsi = it }, label = { Text("Deskripsi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Harga Paket 1H-6H", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary)
                    TextButton(onClick = {
                        val auto = Formatters.calculateAutoPrices(h1.toLongOrNull() ?: 0L)
                        h2 = auto[1].toString(); h3 = auto[2].toString(); h4 = auto[3].toString(); h5 = auto[4].toString(); h6 = auto[5].toString()
                    }) { Text("Isi Otomatis") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("1H", h1, { h1 = it }, Modifier.weight(1f))
                    PriceField("2H", h2, { h2 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("3H", h3, { h3 = it }, Modifier.weight(1f))
                    PriceField("4H", h4, { h4 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceField("5H", h5, { h5 = it }, Modifier.weight(1f))
                    PriceField("6H", h6, { h6 = it }, Modifier.weight(1f))
                }

                Text("Isi Produk dalam Paket:", style = MaterialTheme.typography.labelMedium)
                products.forEach { prod ->
                    val firstVar = prod.variants.firstOrNull() ?: return@forEach
                    val existingIdx = selectedItems.indexOfFirst { it.productId == prod.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (existingIdx >= 0) selectedItems.removeAt(existingIdx)
                                else selectedItems.add(
                                    PackageItem(prod.id, prod.nama, firstVar.id, firstVar.namaVarian, firstVar.sku, 1)
                                )
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = existingIdx >= 0, onCheckedChange = null)
                        Text("${prod.nama} (${firstVar.namaVarian})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.trim().isNotEmpty()) {
                        onSave(
                            RentalPackage(
                                id = initial?.id ?: "pkg_${UUID.randomUUID().toString().take(8)}",
                                nama = nama.trim(),
                                deskripsi = deskripsi.trim(),
                                harga1H = h1.toLongOrNull() ?: 0L,
                                harga2H = h2.toLongOrNull() ?: 0L,
                                harga3H = h3.toLongOrNull() ?: 0L,
                                harga4H = h4.toLongOrNull() ?: 0L,
                                harga5H = h5.toLongOrNull() ?: 0L,
                                harga6H = h6.toLongOrNull() ?: 0L,
                                items = selectedItems.toList(),
                                aktif = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text("Simpan Paket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
