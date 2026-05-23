package com.moncake94.pos.printer

data class PrinterTarget(
    val type: PrinterType,
    val name: String,
    val address: String
)

enum class PrinterType { BLUETOOTH, USB, WIFI }

sealed class PrinterResult {
    data object Success : PrinterResult()
    data class Error(val message: String) : PrinterResult()
}

object StoreInfo {
    const val name = "moncake94"
    const val address = "Jl. Danau Limboto Blok C-1 no.5, RT.13/RW.5, Pejompongan, Bend. Hilir, Kecamatan Tanah Abang, Kota Jakarta Pusat, Daerah Khusus Ibukota Jakarta 10210"
    const val instagram = "@moncake94"
    const val whatsapp = "087877530387"
}
