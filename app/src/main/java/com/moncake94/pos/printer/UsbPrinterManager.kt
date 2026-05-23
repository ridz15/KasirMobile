package com.moncake94.pos.printer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager

class UsbPrinterManager(private val context: Context) {
    private val manager: UsbManager
        get() = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun listPrinters(): List<PrinterTarget> {
        return manager.deviceList.values
            .filter { device ->
                (0 until device.interfaceCount).any { index ->
                    device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                }
            }
            .map { device -> PrinterTarget(PrinterType.USB, device.productName ?: "USB printer", device.deviceName) }
    }

    fun requestPermission(target: PrinterTarget) {
        if (target.type != PrinterType.USB) return
        val device = manager.deviceList.values.firstOrNull { it.deviceName == target.address } ?: return
        if (manager.hasPermission(device)) return
        val intent = PendingIntent.getBroadcast(
            context,
            100,
            Intent("com.moncake94.pos.USB_PERMISSION"),
            PendingIntent.FLAG_IMMUTABLE
        )
        manager.requestPermission(device, intent)
    }

    fun print(bytes: ByteArray, target: PrinterTarget): PrinterResult {
        val device = manager.deviceList.values.firstOrNull { it.deviceName == target.address }
            ?: return PrinterResult.Error("Printer USB tidak ditemukan")
        if (!manager.hasPermission(device)) return PrinterResult.Error("Izin USB printer belum diberikan")

        val printerInterface = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }
            ?: return PrinterResult.Error("Interface printer USB tidak ditemukan")
        val endpoint = (0 until printerInterface.endpointCount)
            .map { printerInterface.getEndpoint(it) }
            .firstOrNull { it.direction == UsbConstants.USB_DIR_OUT }
            ?: return PrinterResult.Error("Endpoint USB printer tidak ditemukan")

        return runCatching {
            val connection = manager.openDevice(device) ?: error("Gagal membuka printer USB")
            try {
                connection.claimInterface(printerInterface, true)
                val result = connection.bulkTransfer(endpoint, bytes, bytes.size, 5_000)
                if (result < 0) error("Transfer USB gagal")
            } finally {
                connection.releaseInterface(printerInterface)
                connection.close()
            }
        }.fold(
            onSuccess = { PrinterResult.Success },
            onFailure = { PrinterResult.Error(it.message ?: "Gagal mencetak ke printer USB") }
        )
    }
}
