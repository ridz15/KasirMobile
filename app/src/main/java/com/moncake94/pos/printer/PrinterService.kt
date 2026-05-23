package com.moncake94.pos.printer

import android.content.Context
import com.moncake94.pos.data.CartItem
import com.moncake94.pos.data.ClosingReportEntity
import com.moncake94.pos.data.DailyReport
import com.moncake94.pos.data.PaymentMethod
import com.moncake94.pos.data.TransactionWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrinterService(context: Context) {
    private val settings = PrinterSettingsRepository(context)
    private val bluetooth = BluetoothPrinterManager(context)
    private val usb = UsbPrinterManager(context)
    private val wifi = WifiPrinterManager()
    private val receiptBuilder = EscPosReceiptBuilder()

    fun getDefaultPrinter(): PrinterTarget? = settings.getDefaultPrinter()

    fun saveDefaultPrinter(target: PrinterTarget) {
        if (target.type == PrinterType.USB) usb.requestPermission(target)
        settings.saveDefaultPrinter(target)
    }

    fun listBluetoothPrinters(): PrinterResultWithPrinters = bluetooth.listPairedPrinters()

    fun listUsbPrinters(): List<PrinterTarget> = usb.listPrinters()

    suspend fun testPrint(): PrinterResult {
        return printBytes(receiptBuilder.buildTestReceipt())
    }

    suspend fun printCartReceipt(
        items: List<CartItem>,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: PaymentMethod
    ): PrinterResult {
        return printBytes(receiptBuilder.buildCartReceipt(items, paymentAmount, changeAmount, paymentMethod))
    }

    suspend fun printTransactionReceipt(transaction: TransactionWithItems): PrinterResult {
        return printBytes(receiptBuilder.buildTransactionReceipt(transaction))
    }

    suspend fun printClosingReport(report: DailyReport): PrinterResult {
        return printBytes(receiptBuilder.buildClosingReport(report))
    }

    suspend fun printSavedClosingReport(report: ClosingReportEntity): PrinterResult {
        return printBytes(receiptBuilder.buildSavedClosingReport(report))
    }

    private suspend fun printBytes(bytes: ByteArray): PrinterResult = withContext(Dispatchers.IO) {
        val target = settings.getDefaultPrinter() ?: return@withContext PrinterResult.Error("Printer belum dipilih.")
        when (target.type) {
            PrinterType.BLUETOOTH -> bluetooth.print(bytes, target)
            PrinterType.USB -> usb.print(bytes, target)
            PrinterType.WIFI -> wifi.print(bytes, target)
        }
    }
}
