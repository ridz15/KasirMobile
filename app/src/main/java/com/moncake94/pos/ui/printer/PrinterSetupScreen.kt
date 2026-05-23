package com.moncake94.pos.ui.printer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moncake94.pos.printer.PrinterTarget
import com.moncake94.pos.ui.PosViewModel
import com.moncake94.pos.ui.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrinterSetupScreen(state: UiState, viewModel: PosViewModel) {
    var wifi by remember { mutableStateOf("") }
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val printers: List<PrinterTarget> = state.bluetoothPrinters + state.usbPrinters
    val status = if (state.selectedPrinter == null) "Not selected" else "Ready"
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.backupTo(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Setup Printer", fontWeight = FontWeight.Bold)
                    Text(
                        "Default: ${state.selectedPrinter?.name ?: "Belum dipilih"}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Status: $status",
                        color = if (state.selectedPrinter == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(onClick = viewModel::refreshPrinters, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Cari Printer Bluetooth/USB")
                    }
                }
            }
        }
        items(printers) { printer ->
            Card(onClick = { viewModel.savePrinter(printer) }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(printer.name, fontWeight = FontWeight.Bold)
                    Text("${printer.type} - ${printer.address}")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Printer WiFi/LAN", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = wifi,
                        onValueChange = { wifi = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("IP:port, contoh 192.168.1.80:9100") },
                        singleLine = true
                    )
                    Button(onClick = { viewModel.saveWifiPrinter(wifi) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Simpan Printer WiFi")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup & Restore", fontWeight = FontWeight.Bold)
                    Text("Simpan atau pulihkan data lokal aplikasi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(Date())
                            backupLauncher.launch("moncake94-backup-$date.json")
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Backup Data")
                    }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Restore Data")
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::testPrint, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Print, null)
                Spacer(Modifier.width(8.dp))
                Text("Test Print 58mm")
            }
        }
    }
    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore Data") },
            text = { Text("Restore backup akan mengganti data lokal saat ini. Lanjutkan?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreFrom(uri)
                    pendingRestore = null
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Batal") } }
        )
    }
}
