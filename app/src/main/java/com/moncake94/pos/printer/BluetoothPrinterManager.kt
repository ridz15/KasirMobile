package com.moncake94.pos.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    fun listPairedPrinters(): PrinterResultWithPrinters {
        if (!hasConnectPermission()) return PrinterResultWithPrinters(emptyList(), "Izin Bluetooth belum diberikan")
        val bluetooth = adapter ?: return PrinterResultWithPrinters(emptyList(), "Bluetooth tidak tersedia di perangkat ini")
        if (!bluetooth.isEnabled) return PrinterResultWithPrinters(emptyList(), "Bluetooth belum aktif")
        val printers = bluetooth.bondedDevices.map {
            PrinterTarget(PrinterType.BLUETOOTH, it.name ?: "Bluetooth printer", it.address)
        }
        return PrinterResultWithPrinters(printers, null)
    }

    fun print(bytes: ByteArray, target: PrinterTarget): PrinterResult {
        if (!hasConnectPermission()) return PrinterResult.Error("Izin Bluetooth belum diberikan")
        val bluetooth = adapter ?: return PrinterResult.Error("Bluetooth tidak tersedia di perangkat ini")
        if (!bluetooth.isEnabled) return PrinterResult.Error("Bluetooth belum aktif")
        val device = runCatching { bluetooth.getRemoteDevice(target.address) }.getOrNull()
            ?: return PrinterResult.Error("Printer Bluetooth tidak ditemukan")
        val paired = bluetooth.bondedDevices.any { it.address == target.address }
        if (!paired) return PrinterResult.Error("Printer belum dipairing di pengaturan Bluetooth Android")

        return runCatching {
            bluetooth.cancelDiscovery()
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            device.createRfcommSocketToServiceRecord(uuid).use { socket ->
                socket.connect()
                socket.outputStream.use { output ->
                    output.write(bytes)
                    output.flush()
                }
            }
        }.fold(
            onSuccess = { PrinterResult.Success },
            onFailure = { PrinterResult.Error(it.message ?: "Gagal mencetak ke printer Bluetooth") }
        )
    }
}

data class PrinterResultWithPrinters(
    val printers: List<PrinterTarget>,
    val errorMessage: String?
)
