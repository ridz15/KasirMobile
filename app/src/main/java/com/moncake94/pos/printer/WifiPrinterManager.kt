package com.moncake94.pos.printer

import java.net.InetSocketAddress
import java.net.Socket

class WifiPrinterManager {
    fun print(bytes: ByteArray, target: PrinterTarget): PrinterResult {
        val parts = target.address.split(":")
        val host = parts.firstOrNull().orEmpty()
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100
        if (host.isBlank()) return PrinterResult.Error("Alamat printer WiFi belum diisi")

        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 5_000)
                socket.getOutputStream().use { output ->
                    output.write(bytes)
                    output.flush()
                }
            }
        }.fold(
            onSuccess = { PrinterResult.Success },
            onFailure = { PrinterResult.Error(it.message ?: "Gagal mencetak ke printer WiFi/LAN") }
        )
    }
}
