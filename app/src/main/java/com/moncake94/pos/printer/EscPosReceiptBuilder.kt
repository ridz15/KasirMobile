package com.moncake94.pos.printer

import com.moncake94.pos.data.CartItem
import com.moncake94.pos.data.DailyReport
import com.moncake94.pos.data.ClosingReportEntity
import com.moncake94.pos.data.PaymentMethod
import com.moncake94.pos.data.TransactionItemEntity
import com.moncake94.pos.data.TransactionStatus
import com.moncake94.pos.data.TransactionWithItems
import com.moncake94.pos.util.formatCurrency
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EscPosReceiptBuilder(
    private val columns: Int = 32,
    private val charset: Charset = Charset.forName("CP437")
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

    fun buildTestReceipt(): ByteArray {
        val lines = mutableListOf<String>()
        appendHeader(lines, "TEST-${System.currentTimeMillis()}", System.currentTimeMillis())
        appendItem(lines, "Marmer Cake", "Whole", 1, 120_000, 120_000)
        appendItem(lines, "Bolu Tape", null, 1, 120_000, 120_000)
        appendTotals(lines, 240_000, 250_000, 10_000, PaymentMethod.TUNAI.label, TransactionStatus.SUCCESS.label)
        return toEscPos(lines)
    }

    fun buildCartReceipt(
        items: List<CartItem>,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
        transactionNumber: String = "DRAFT-${System.currentTimeMillis()}",
        createdAt: Long = System.currentTimeMillis()
    ): ByteArray {
        val lines = mutableListOf<String>()
        appendHeader(lines, transactionNumber, createdAt)
        items.forEach { item ->
            appendItem(
                lines,
                item.productName,
                item.displayVariation(),
                item.quantity,
                item.price,
                item.subtotal
            )
        }
        appendTotals(lines, items.sumOf { it.subtotal }, paymentAmount, changeAmount, paymentMethod.label, TransactionStatus.SUCCESS.label)
        return toEscPos(lines)
    }

    fun buildTransactionReceipt(transaction: TransactionWithItems): ByteArray {
        val lines = mutableListOf<String>()
        appendHeader(lines, transaction.transaction.transactionNumber, transaction.transaction.createdAt)
        val status = transaction.transaction.status.toStatusLabel()
        if (transaction.transaction.status == TransactionStatus.VOID.name) lines += center("*** VOID ***")
        if (transaction.transaction.status == TransactionStatus.REFUNDED.name) lines += center("*** REFUND ***")
        transaction.items.forEach { item ->
            appendItem(
                lines,
                item.productName,
                item.displayVariation(),
                item.quantity,
                item.price,
                item.subtotal
            )
        }
        appendTotals(
            lines,
            transaction.transaction.total,
            transaction.transaction.paymentAmount,
            transaction.transaction.changeAmount,
            transaction.transaction.paymentMethod.toPaymentLabel(),
            status
        )
        if (transaction.transaction.status == TransactionStatus.VOID.name) {
            transaction.transaction.voidReason?.let { lines += wrap("Alasan void: $it") }
        }
        if (transaction.transaction.status == TransactionStatus.REFUNDED.name) {
            lines += twoColumns("Refund", formatCurrency(transaction.transaction.refundAmount))
            transaction.transaction.refundReason?.let { lines += wrap("Alasan refund: $it") }
        }
        return toEscPos(lines)
    }

    fun buildClosingReport(report: DailyReport): ByteArray {
        val lines = mutableListOf<String>()
        lines += center(StoreInfo.name)
        lines += center("LAPORAN CLOSING")
        lines += "Tanggal: ${dateFormat.format(Date(report.dateMillis)).substringBefore(" ")}"
        lines += "Jam: ${dateFormat.format(Date()).substringAfter(" ")}"
        lines += line()
        lines += twoColumns("Total Penjualan", formatCurrency(report.grossSales))
        lines += twoColumns("Penjualan Bersih", formatCurrency(report.netSales))
        lines += twoColumns("Tunai", formatCurrency(report.tunaiTotal))
        lines += twoColumns("QRIS", formatCurrency(report.qrisTotal))
        lines += line()
        lines += twoColumns("Transaksi Sukses", report.successCount.toString())
        lines += twoColumns("Void", report.voidCount.toString())
        lines += twoColumns("Refund", report.refundCount.toString())
        lines += twoColumns("Item Terjual", report.itemSoldCount.toString())
        if (report.bestSellers.isNotEmpty()) {
            lines += line()
            lines += "Produk Terlaris:"
            report.bestSellers.take(3).forEach {
                lines += wrap("${it.productName} (${it.quantitySold})")
            }
        }
        lines += line()
        lines += center("Terima kasih")
        return toEscPos(lines)
    }

    fun buildSavedClosingReport(report: ClosingReportEntity): ByteArray {
        val lines = mutableListOf<String>()
        lines += center(StoreInfo.name)
        lines += center("RIWAYAT CLOSING")
        lines += "Tanggal: ${dateFormat.format(Date(report.reportDate)).substringBefore(" ")}"
        lines += "Jam: ${dateFormat.format(Date(report.printedAt)).substringAfter(" ")}"
        lines += line()
        lines += twoColumns("Total Penjualan", formatCurrency(report.grossSales))
        lines += twoColumns("Penjualan Bersih", formatCurrency(report.netSales))
        lines += twoColumns("Tunai", formatCurrency(report.tunaiTotal))
        lines += twoColumns("QRIS", formatCurrency(report.qrisTotal))
        lines += line()
        lines += twoColumns("Transaksi Sukses", report.successCount.toString())
        lines += twoColumns("Void", report.voidCount.toString())
        lines += twoColumns("Refund", report.refundCount.toString())
        lines += twoColumns("Item Terjual", report.itemSoldCount.toString())
        if (report.bestSellersText.isNotBlank()) {
            lines += line()
            lines += "Produk Terlaris:"
            report.bestSellersText.lines().take(3).forEach { lines += wrap(it) }
        }
        lines += line()
        lines += center("Terima kasih")
        return toEscPos(lines)
    }

    private fun appendHeader(lines: MutableList<String>, transactionNumber: String, createdAt: Long) {
        lines += center(StoreInfo.name)
        lines += wrap(StoreInfo.address)
        lines += "IG: ${StoreInfo.instagram}"
        lines += "WA: ${StoreInfo.whatsapp}"
        lines += line()
        lines += dateFormat.format(Date(createdAt))
        lines += transactionNumber.take(columns)
        lines += line()
    }

    private fun appendItem(
        lines: MutableList<String>,
        name: String,
        variation: String?,
        quantity: Int,
        price: Long,
        subtotal: Long
    ) {
        lines += wrap(if (variation == null) name else "$name - $variation")
        lines += twoColumns("${quantity}x ${formatCurrency(price)}", formatCurrency(subtotal))
    }

    private fun appendTotals(
        lines: MutableList<String>,
        total: Long,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: String,
        status: String
    ) {
        lines += line()
        lines += twoColumns("Metode", paymentMethod)
        lines += twoColumns("Total", formatCurrency(total))
        lines += twoColumns("Dibayar", formatCurrency(paymentAmount))
        lines += twoColumns("Kembalian", formatCurrency(changeAmount))
        lines += twoColumns("Status", status)
        lines += line()
        lines += center("Terima kasih")
    }

    private fun toEscPos(lines: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40))
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        lines.forEach { line ->
            out.write(line.take(columns).toByteArray(charset))
            out.write(byteArrayOf(0x0A))
        }
        out.write(byteArrayOf(0x0A))
        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return out.toByteArray()
    }

    private fun CartItem.displayVariation(): String? {
        val option = optionName ?: return null
        val name = variationName
        return if (name.isNullOrBlank() || name.equals("Variant", ignoreCase = true)) option else "$name: $option"
    }

    private fun TransactionItemEntity.displayVariation(): String? {
        val option = selectedVariationOption ?: return null
        val name = selectedVariationName
        return if (name.isNullOrBlank() || name.equals("Variant", ignoreCase = true)) option else "$name: $option"
    }

    private fun wrap(text: String): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (candidate.length <= columns) {
                current = candidate
            } else {
                if (current.isNotBlank()) lines += current
                current = word.chunked(columns).first()
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun twoColumns(left: String, right: String): String {
        val available = columns - right.length
        val safeLeft = left.take((available - 1).coerceAtLeast(1))
        return safeLeft.padEnd(available.coerceAtLeast(safeLeft.length + 1)) + right
    }

    private fun center(text: String): String {
        val safe = text.take(columns)
        return " ".repeat(((columns - safe.length) / 2).coerceAtLeast(0)) + safe
    }

    private fun line(): String = "-".repeat(columns)

    private fun String.toPaymentLabel(): String {
        return runCatching { PaymentMethod.valueOf(this).label }.getOrDefault(this)
    }

    private fun String.toStatusLabel(): String {
        return runCatching { TransactionStatus.valueOf(this).label }.getOrDefault(this)
    }
}
