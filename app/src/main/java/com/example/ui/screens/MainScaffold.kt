package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.components.AccessDeniedView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

private data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isCenterAdd: Boolean = false
)

@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val session by viewModel.session.collectAsState()
    val products by viewModel.products.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val members by viewModel.members.collectAsState()
    val cashEntries by viewModel.cashEntries.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val preselectedItem by viewModel.preselectedCartItem.collectAsState()
    val activeReceiptTx by viewModel.activeReceiptTx.collectAsState()
    val driveBusy by viewModel.driveActionBusy.collectAsState()
    val driveTestResult by viewModel.driveTestResult.collectAsState()

    val isAdmin = session.role == UserRole.ADMIN
    val homeRoute = if (isAdmin) "dashboard" else "transactions"
    var currentRoute by remember(session.role) { mutableStateOf(homeRoute) }

    // Admin-only routes guard
    val adminOnlyRoutes = remember {
        setOf(
            "dashboard", "products_admin", "packages_admin", "members",
            "cashbook", "employees", "qris_settings", "store_settings",
            "roles_pin", "backup_drive", "printer_settings", "invoice_settings",
            "payment_methods_settings", "operating_hours_settings", "bonus_settings"
        )
    }

    val navItems = remember(isAdmin) {
        if (isAdmin) {
            listOf(
                NavTabItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                NavTabItem("transactions", "Transaksi", Icons.Default.ReceiptLong),
                NavTabItem("add_transaction", "Tambah", Icons.Default.Add, isCenterAdd = true),
                NavTabItem("reports", "Laporan", Icons.Default.BarChart),
                NavTabItem("account", "Akun", Icons.Default.Person)
            )
        } else {
            listOf(
                NavTabItem("transactions", "Transaksi", Icons.Default.ReceiptLong),
                NavTabItem("add_transaction", "Tambah", Icons.Default.Add, isCenterAdd = true),
                NavTabItem("reports", "Laporan", Icons.Default.BarChart),
                NavTabItem("account", "Akun", Icons.Default.Person)
            )
        }
    }

    val showBottomBar = activeReceiptTx == null && currentRoute != "add_transaction"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showBottomBar && !isWideScreen) {
                    MaroonPosBottomBar(
                        items = navItems,
                        currentRoute = currentRoute,
                        onSelect = { route -> currentRoute = route }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomBar && isWideScreen) {
                    NavigationRail(
                        containerColor = PureWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = { currentRoute = item.route },
                                icon = {
                                    if (item.isCenterAdd) {
                                        Surface(color = MaroonPrimary, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(item.icon, contentDescription = item.label, tint = PureWhite)
                                            }
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (activeReceiptTx != null) {
                        ReceiptScreen(
                            tx = activeReceiptTx!!,
                            store = settings.store,
                            onClose = { viewModel.openReceipt(null) },
                            onMessage = { viewModel.emitMessage(it) }
                        )
                    } else if (!isAdmin && currentRoute in adminOnlyRoutes) {
                        AccessDeniedView(
                            onBackToAllowed = { currentRoute = "transactions" }
                        )
                    } else {
                        when (currentRoute) {
                            "dashboard" -> DashboardScreen(
                                storeName = settings.store.namaToko,
                                userName = session.userName,
                                transactions = transactions,
                                cashEntries = cashEntries,
                                products = products,
                                members = members,
                                onNavigate = { currentRoute = it },
                                onOpenTransaction = { tx -> viewModel.openReceipt(tx.id) }
                            )
                            "transactions" -> TransactionsScreen(
                                transactions = transactions,
                                session = session,
                                qrisSettings = settings.qris,
                                onOpenCatalog = { currentRoute = "catalog" },
                                onOpenReceipt = { tx -> viewModel.openReceipt(tx.id) },
                                onUpdateTransaction = { tx, st, paySt, paid, method ->
                                    viewModel.updateTransactionStatus(tx, st, paySt, paid, method)
                                },
                                onMessage = { viewModel.emitMessage(it) }
                            )
                            "catalog" -> CatalogScreen(
                                products = products,
                                onBack = { currentRoute = homeRoute },
                                onRentVariant = { prod, variant ->
                                    viewModel.setPreselectedProduct(prod, variant)
                                    currentRoute = "add_transaction"
                                }
                            )
                            "add_transaction" -> AddTransactionScreen(
                                products = products,
                                packages = packages,
                                members = members,
                                qrisSettings = settings.qris,
                                activePaymentMethods = settings.store.activePaymentMethodEnums(),
                                preselectedItem = preselectedItem,
                                onConsumePreselected = { viewModel.consumePreselectedProduct() },
                                onBack = { currentRoute = homeRoute },
                                onSaved = { savedTx ->
                                    currentRoute = "transactions"
                                    viewModel.openReceipt(savedTx.id)
                                },
                                onMessage = { viewModel.emitMessage(it) },
                                onSaveTransaction = { mulai, selesai, durasi, satuan, nama, hp, items, fees, dAktif, dTipe, dNilai, promo, sub, nomD, totFee, tot, stBayar, paid, sisa, method, stTx, jaminan, detJam, cat, cb ->
                                    viewModel.saveTransaction(
                                        mulai, selesai, durasi, satuan, nama, hp, items, fees,
                                        dAktif, dTipe, dNilai, promo, sub, nomD, totFee, tot,
                                        stBayar, paid, sisa, method, stTx, jaminan, detJam, cat, cb
                                    )
                                }
                            )
                            "reports" -> ReportsScreen(
                                session = session,
                                transactions = transactions,
                                members = members,
                                cashEntries = cashEntries,
                                onExportCsv = { kind, csv ->
                                    viewModel.exportCsvToDownloads(
                                        viewModel.getApplication(),
                                        kind,
                                        csv
                                    )
                                }
                            )
                            "account" -> AccountScreen(
                                session = session,
                                settings = settings,
                                onNavigate = { currentRoute = it },
                                onLogout = { viewModel.logout() },
                                onSaveSettings = { viewModel.saveSettings(it) }
                            )
                            "products_admin" -> ProductsAdminScreen(
                                products = products,
                                onBack = { currentRoute = "dashboard" },
                                onSaveProduct = { viewModel.saveProduct(it) },
                                onDeleteProduct = { viewModel.deleteProduct(it) }
                            )
                            "packages_admin" -> PackagesAdminScreen(
                                packages = packages,
                                products = products,
                                onBack = { currentRoute = "dashboard" },
                                onSavePackage = { viewModel.savePackage(it) },
                                onDeletePackage = { viewModel.deletePackage(it) }
                            )
                            "members" -> MembersScreen(
                                members = members,
                                transactions = transactions,
                                onBack = { currentRoute = "dashboard" },
                                onOpenReceipt = { tx -> viewModel.openReceipt(tx.id) }
                            )
                            "cashbook" -> CashBookScreen(
                                transactions = transactions,
                                cashEntries = cashEntries,
                                mutations = viewModel.buildAllMutations(transactions, cashEntries),
                                onBack = { currentRoute = "dashboard" },
                                onAddCashEntry = { viewModel.addCashEntry(it) },
                                onDeleteCashEntry = { viewModel.deleteCashEntry(it) }
                            )
                            "employees" -> EmployeesScreen(
                                employees = employees,
                                onBack = { currentRoute = "account" },
                                onSaveEmployee = { viewModel.saveEmployee(it) },
                                onDeleteEmployee = { viewModel.deleteEmployee(it) }
                            )
                            "store_settings" -> StoreSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "qris_settings" -> QrisSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "roles_pin" -> RolesPinScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "backup_drive" -> BackupDriveScreen(
                                settings = settings,
                                products = products,
                                members = members,
                                transactions = transactions,
                                driveBusy = driveBusy,
                                driveTestResult = driveTestResult,
                                onBack = { currentRoute = "account" },
                                onExportAllJson = { viewModel.exportAllJsonToDownloads(viewModel.getApplication()) },
                                onExportCsv = { kind, csv -> viewModel.exportCsvToDownloads(viewModel.getApplication(), kind, csv) },
                                onImportJson = { viewModel.importBackupJson(it) },
                                onSaveDriveSettings = { viewModel.saveSettings(it) },
                                onTestDrive = { viewModel.testDriveConnection(it) },
                                onSyncDriveNow = { viewModel.syncDriveNow() },
                                onMessage = { viewModel.emitMessage(it) }
                            )
                            "printer_settings" -> PrinterSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "invoice_settings" -> InvoiceSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "payment_methods_settings" -> PaymentMethodsSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "operating_hours_settings" -> OperatingHoursSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                            "bonus_settings" -> BonusSettingsScreen(
                                settings = settings,
                                onBack = { currentRoute = "account" },
                                onSave = { viewModel.saveSettings(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaroonPosBottomBar(
    items: List<NavTabItem>,
    currentRoute: String,
    onSelect: (String) -> Unit
) {
    Surface(
        color = PureWhite,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = OutlineSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(68.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    if (item.isCenterAdd) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelect(item.route) }
                                .testTag("nav_add_transaction"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaroonPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(50.dp)
                                    .shadow(4.dp, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Tambah Transaksi",
                                        tint = PureWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val selected = currentRoute == item.route
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(item.route) }
                                .testTag("nav_${item.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = if (selected) MaroonContainer else PureWhite,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaroonPrimary else TextSecondary,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaroonPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
