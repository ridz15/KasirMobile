package com.moncake94.pos.printer

import android.content.Context

class PrinterSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("printer_settings", Context.MODE_PRIVATE)

    fun getDefaultPrinter(): PrinterTarget? {
        val type = prefs.getString("type", null)?.let { runCatching { PrinterType.valueOf(it) }.getOrNull() } ?: return null
        val name = prefs.getString("name", null) ?: return null
        val address = prefs.getString("address", null) ?: return null
        return PrinterTarget(type, name, address)
    }

    fun saveDefaultPrinter(target: PrinterTarget) {
        prefs.edit()
            .putString("type", target.type.name)
            .putString("name", target.name)
            .putString("address", target.address)
            .apply()
    }
}
