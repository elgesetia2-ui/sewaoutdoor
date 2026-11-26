package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.QrisSettings
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.util.QrisGenerator

@Composable
fun QrisPaymentDialog(
    qrisSettings: QrisSettings,
    amount: Long,
    referenceLabel: String,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val dynamicPayload = remember(qrisSettings.payloadStatis, amount) {
        QrisGenerator.buildDynamicPayload(qrisSettings.payloadStatis, amount)
    }
    val qrBitmap = remember(dynamicPayload) {
        QrisGenerator.generateQrBitmap(dynamicPayload, 640)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QRIS",
                            tint = MaroonPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pembayaran QRIS",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaroonPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = qrisSettings.namaMerchant.ifBlank { "SEWA OUTDOOR" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "NMID: ${qrisSettings.nmid.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = TextSecondary
                )
                if (qrisSettings.rekeningInfo.isNotBlank()) {
                    Text(
                        text = qrisSettings.rekeningInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaroonPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Kode QR QRIS",
                        modifier = Modifier
                            .size(250.dp)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaroonContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total yang Harus Dibayar",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnMaroonContainer
                        )
                        Text(
                            text = Formatters.formatRupiah(amount),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaroonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (referenceLabel.isNotBlank()) {
                            Text(
                                text = referenceLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnMaroonContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Instruksi: Buka GoPay, OVO, DANA, ShopeePay, atau Mobile Banking lalu scan QR di atas. Pastikan nama merchant & nominal sesuai sebelum membayar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onConfirmPayment()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_qris_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Pembayaran QR")
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val res = QrisGenerator.saveQrPosterPng(context, qrisSettings, amount, referenceLabel)
                        onMessage(res.getOrElse { it.localizedMessage ?: "Gagal menyimpan QR PNG" })
                    },
                    border = BorderStroke(1.dp, MaroonPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_qris_png_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan QR PNG")
                }
            }
        }
    }
}
