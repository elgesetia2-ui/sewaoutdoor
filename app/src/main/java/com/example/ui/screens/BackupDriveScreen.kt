package com.example.ui.screens

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
import com.example.model.*
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.ExportImportManager

private const val APPS_SCRIPT_SAMPLE = """function doGet() {
  return ContentService.createTextOutput("Sewa Outdoor Apps Script Aktif").setMimeType(ContentService.MimeType.TEXT);
}
function doPost(e) {
  var folder = DriveApp.getRootFolder();
  var fileName = "SewaOutdoor_Backup.json";
  var files = folder.getFilesByName(fileName);
  var content = e.postData.contents;
  if (files.hasNext()) { files.next().setContent(content); }
  else { folder.createFile(fileName, content, MimeType.PLAIN_TEXT); }
  return ContentService.createTextOutput("OK").setMimeType(ContentService.MimeType.TEXT);
}"""

@Composable
fun BackupDriveScreen(
    settings: AppSettingsBundle,
    products: List<Product>,
    members: List<Member>,
    transactions: List<RentalTransaction>,
    driveBusy: Boolean,
    driveTestResult: String?,
    onBack: () -> Unit,
    onExportAllJson: () -> Unit,
    onExportCsv: (String, String) -> Unit,
    onImportJson: (String) -> Unit,
    onSaveDriveSettings: (AppSettingsBundle) -> Unit,
    onTestDrive: (String) -> Unit,
    onSyncDriveNow: () -> Unit,
    onMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var webAppUrl by remember(settings.drive.webAppUrl) { mutableStateOf(settings.drive.webAppUrl) }
    var autoSync by remember(settings.drive.autoSync) { mutableStateOf(settings.drive.autoSync) }
    var showScriptHelp by remember { mutableStateOf(false) }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { raw ->
                if (raw.isNotBlank()) onImportJson(raw)
                else onMessage("File JSON kosong")
            }.onFailure {
                onMessage("Gagal membaca file: ${it.localizedMessage}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        SectionHeader(
            title = "Backup & Google Drive",
            subtitle = "Export/Import JSON, CSV & Apps Script",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Export & Import Local Files
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup & Restore Lokal", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = onExportAllJson,
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_all_json_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Semua JSON")
                    }

                    OutlinedButton(
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        border = BorderStroke(1.dp, MaroonPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_json_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON (Restore Backup)")
                    }

                    HorizontalDivider(color = OutlineSubtle)
                    Text("Export CSV", style = MaterialTheme.typography.labelLarge, color = TextPrimary)

                    OutlinedButton(
                        onClick = {
                            onExportCsv("Produk_SewaOutdoor", ExportImportManager.buildProductsCsv(products))
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Export Produk CSV (${products.size} produk)")
                    }
                    OutlinedButton(
                        onClick = {
                            onExportCsv("Member_SewaOutdoor", ExportImportManager.buildMembersCsv(members))
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Export Member CSV (${members.size} member)")
                    }
                    OutlinedButton(
                        onClick = {
                            onExportCsv("Transaksi_SewaOutdoor", ExportImportManager.buildTransactionsCsv(transactions))
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Export Transaksi CSV (${transactions.size} transaksi)")
                    }
                }
            }

            // Google Drive via Apps Script
            Surface(color = PureWhite, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSubtle)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaroonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hubungkan Google Drive (Apps Script)", style = MaterialTheme.typography.titleSmall, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = webAppUrl,
                        onValueChange = { webAppUrl = it },
                        label = { Text("URL Web App Apps Script") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("drive_webapp_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Sync Otomatis", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Kirim backup ke Drive tiap ada perubahan data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = autoSync,
                            onCheckedChange = {
                                autoSync = it
                                onSaveDriveSettings(
                                    settings.copy(
                                        drive = settings.drive.copy(
                                            webAppUrl = webAppUrl.trim(),
                                            autoSync = it
                                        )
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaroonPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSaveDriveSettings(
                                    settings.copy(drive = settings.drive.copy(webAppUrl = webAppUrl.trim(), autoSync = autoSync))
                                )
                                onTestDrive(webAppUrl)
                            },
                            enabled = !driveBusy,
                            border = BorderStroke(1.dp, MaroonPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("test_drive_button")
                        ) {
                            Text("Test Koneksi")
                        }

                        Button(
                            onClick = {
                                onSaveDriveSettings(
                                    settings.copy(drive = settings.drive.copy(webAppUrl = webAppUrl.trim(), autoSync = autoSync))
                                )
                                onSyncDriveNow()
                            },
                            enabled = !driveBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("sync_drive_now_button")
                        ) {
                            Text(if (driveBusy) "Proses..." else "Sync Sekarang")
                        }
                    }

                    if (!driveTestResult.isNullOrBlank()) {
                        Surface(
                            color = SurfaceMuted,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(driveTestResult, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.padding(10.dp))
                        }
                    }

                    Text(
                        text = "Sync terakhir: ${settings.drive.lastSyncTime} (${settings.drive.lastSyncStatus})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    TextButton(onClick = { showScriptHelp = !showScriptHelp }) {
                        Text(if (showScriptHelp) "Sembunyikan Kode Apps Script" else "Lihat Contoh Kode Apps Script")
                    }

                    if (showScriptHelp) {
                        Surface(color = SurfaceMuted, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(APPS_SCRIPT_SAMPLE, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily))
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(APPS_SCRIPT_SAMPLE))
                                    onMessage("Kode Apps Script disalin")
                                }) {
                                    Text("Salin Kode Apps Script")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
