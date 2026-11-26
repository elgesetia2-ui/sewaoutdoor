package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Member
import com.example.model.RentalTransaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun MembersScreen(
    members: List<Member>,
    transactions: List<RentalTransaction>,
    onBack: () -> Unit,
    onOpenReceipt: (RentalTransaction) -> Unit
) {
    BackHandler { onBack() }

    var search by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<Member?>(null) }

    val filtered = remember(members, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) members
        else members.filter { it.nama.lowercase().contains(q) || it.noHp.contains(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        SectionHeader(
            title = "Data Member",
            subtitle = "${members.size} member aktif (Auto-Member tanpa poin)",
            onBack = onBack
        )

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Cari nama atau nomor HP member...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaroonPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Groups,
                    title = "Belum ada data member",
                    description = "Member dibuat otomatis setiap kali transaksi sewa disimpan."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { m ->
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedMember = m }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaroonContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = m.nama.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaroonPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(m.noHp, style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), color = TextSecondary)
                                }
                                Text(
                                    text = "Sejak ${Formatters.formatTanggalIndo(m.memberSejak)} • Terakhir ${Formatters.formatTanggalIndo(m.transaksiTerakhir)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPill("${m.totalSewa}x Sewa", MaroonContainer, MaroonPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatRupiah(m.totalBelanja),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaroonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMember?.let { m ->
        val memberTxs = remember(transactions, m) {
            transactions.filter { it.customerPhone == m.noHp || it.memberId == m.id }
        }
        AlertDialog(
            onDismissRequest = { selectedMember = null },
            title = {
                Column {
                    Text(m.nama, style = MaterialTheme.typography.titleMedium)
                    Text("${m.noHp} • Sejak ${Formatters.formatTanggalIndo(m.memberSejak)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Sewa: ${m.totalSewa}x • Total Belanja: ${Formatters.formatRupiah(m.totalBelanja)}", style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                    HorizontalDivider(color = OutlineSubtle)
                    if (memberTxs.isEmpty()) {
                        Text("Belum ada riwayat transaksi.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        memberTxs.take(5).forEach { tx ->
                            Surface(
                                color = SurfaceMuted,
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    selectedMember = null
                                    onOpenReceipt(tx)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(tx.nomorTransaksi, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(Formatters.formatTanggalIndo(tx.tanggalTransaksi), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Text(Formatters.formatRupiah(tx.total), style = MaterialTheme.typography.labelLarge, color = MaroonPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMember = null }) { Text("Tutup") }
            }
        )
    }
}
