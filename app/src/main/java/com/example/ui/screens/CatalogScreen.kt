package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.example.model.Product
import com.example.model.ProductVariant
import com.example.model.VariantStatus
import com.example.ui.components.ColorSwatchCircle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun CatalogScreen(
    products: List<Product>,
    onBack: (() -> Unit)? = null,
    onRentVariant: (Product, ProductVariant) -> Unit
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedColor by remember { mutableStateOf("Semua") }
    val selectedVariantByProduct = remember { mutableStateMapOf<String, String>() }

    val categories = remember(products) {
        listOf("Semua") + products.map { it.kategori }.distinct().sorted()
    }
    val colors = remember(products) {
        val allVars = products.flatMap { it.variants }
        listOf("Semua" to "") + allVars.map { it.warnaNama to it.warnaHex }.distinctBy { it.first }.sortedBy { it.first }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedColor) {
        val q = searchQuery.trim().lowercase()
        products.filter { prod ->
            val catOk = selectedCategory == "Semua" || prod.kategori == selectedCategory
            val colorOk = selectedColor == "Semua" || prod.variants.any { it.warnaNama == selectedColor }
            val queryOk = q.isEmpty() ||
                prod.nama.lowercase().contains(q) ||
                prod.kategori.lowercase().contains(q) ||
                prod.variants.any {
                    it.sku.lowercase().contains(q) ||
                        it.namaVarian.lowercase().contains(q) ||
                        it.warnaNama.lowercase().contains(q)
                }
            catOk && colorOk && queryOk
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // NO HERO / NO BANNER — directly header + search + category & color filters + product grid
        SectionHeader(
            title = "Katalog Peralatan",
            subtitle = "${filteredProducts.size} produk tersedia",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama alat, SKU, atau warna...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaroonPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonPrimary,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }

            // Color Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { (colorName, hex) ->
                    FilterChip(
                        selected = selectedColor == colorName,
                        onClick = { selectedColor = colorName },
                        leadingIcon = if (hex.isNotBlank()) {
                            {
                                ColorSwatchCircle(hex = hex, selected = false, size = 14.dp)
                            }
                        } else null,
                        label = { Text(colorName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonContainer,
                            selectedLabelColor = MaroonPrimary
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = OutlineSubtle)

        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "Produk tidak ditemukan",
                    description = "Ubah kata kunci pencarian atau reset filter kategori & warna."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val defaultVariant = remember(product, selectedColor) {
                        if (selectedColor != "Semua") {
                            product.variants.firstOrNull { it.warnaNama == selectedColor }
                                ?: product.variants.firstOrNull()
                        } else {
                            product.variants.firstOrNull()
                        }
                    }
                    val activeVariantId = selectedVariantByProduct[product.id] ?: defaultVariant?.id
                    val activeVariant = product.variants.firstOrNull { it.id == activeVariantId } ?: defaultVariant

                    if (activeVariant != null) {
                        CatalogProductCard(
                            product = product,
                            activeVariant = activeVariant,
                            onSelectVariant = { v -> selectedVariantByProduct[product.id] = v.id },
                            onRentClick = { onRentVariant(product, activeVariant) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogProductCard(
    product: Product,
    activeVariant: ProductVariant,
    onSelectVariant: (ProductVariant) -> Unit,
    onRentClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbBitmap = remember(product.photoPath) {
        if (product.photoPath.isBlank()) null
        else runCatching {
            val file = File(File(context.filesDir, "product_photos"), product.photoPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap.asImageBitmap(),
                            contentDescription = product.nama,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            text = product.kategori,
                            containerColor = MaroonContainer,
                            contentColor = MaroonPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = product.nama,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatRupiah(activeVariant.harga1H),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaroonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Per Hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Variant Details + Clickable Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${activeVariant.namaVarian} • ${activeVariant.ukuran}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "SKU: ${activeVariant.sku} • Stok: ${activeVariant.stok}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = if (activeVariant.stok > 0) TextSecondary else DangerRed
                    )
                }

                val statusColors = when (activeVariant.status) {
                    VariantStatus.TERSEDIA -> if (activeVariant.stok > 0) SuccessContainer to SuccessGreen else DangerContainer to DangerRed
                    VariantStatus.PERAWATAN -> WarningContainer to WarningAmber
                    VariantStatus.NONAKTIF -> SurfaceMuted to TextSecondary
                }
                StatusPill(
                    text = if (activeVariant.stok <= 0 && activeVariant.status == VariantStatus.TERSEDIA) "Habis" else activeVariant.status.label,
                    containerColor = statusColors.first,
                    contentColor = statusColors.second
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Warna:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                product.variants.forEach { variant ->
                    ColorSwatchCircle(
                        hex = variant.warnaHex,
                        selected = variant.id == activeVariant.id,
                        size = 22.dp,
                        onClick = { onSelectVariant(variant) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6-Column Price Table (1H - 6H)
            Surface(
                color = SurfaceMuted.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val cols = listOf(
                    "1H" to activeVariant.harga1H,
                    "2H" to activeVariant.harga2H,
                    "3H" to activeVariant.harga3H,
                    "4H" to activeVariant.harga4H,
                    "5H" to activeVariant.harga5H,
                    "6H" to activeVariant.harga6H
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    cols.forEach { (label, price) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaroonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${price / 1000}rb",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRentClick,
                enabled = activeVariant.status == VariantStatus.TERSEDIA && activeVariant.stok > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("catalog_rent_${product.id}")
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sewa Varian Ini", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
