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
    private val printableColumns = columns - 2
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

    fun buildTestReceipt(): ByteArray {
        val lines = mutableListOf<String>()
        appendHeader(lines, "TEST-${System.currentTimeMillis()}", System.currentTimeMillis())
        appendItem(lines, "Marmer Cake", "Whole", 1, 120_000, 120_000)
        appendItem(lines, "Bolu Tape", null, 1, 120_000, 120_000)
        appendTotals(lines, 240_000, 0, 240_000, 250_000, 10_000, PaymentMethod.TUNAI.label, TransactionStatus.SUCCESS.label, null)
        return toEscPos(lines)
    }

    fun buildCartReceipt(
        items: List<CartItem>,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
        discountAmount: Long = 0,
        note: String? = null,
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
        val subtotal = items.sumOf { it.subtotal }
        appendTotals(lines, subtotal, discountAmount, (subtotal - discountAmount).coerceAtLeast(0), paymentAmount, changeAmount, paymentMethod.label, TransactionStatus.SUCCESS.label, note)
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
            transaction.transaction.subtotal.takeIf { it > 0 } ?: transaction.transaction.total,
            transaction.transaction.discountAmount,
            transaction.transaction.total,
            transaction.transaction.paymentAmount,
            transaction.transaction.changeAmount,
            transaction.transaction.paymentMethod.toPaymentLabel(),
            status,
            transaction.transaction.note
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
        if (report.discountTotal > 0) lines += twoColumns("Diskon", formatCurrency(report.discountTotal))
        lines += twoColumns("Penjualan Bersih", formatCurrency(report.netSales))
        lines += twoColumns("Tunai (${report.tunaiCount})", formatCurrency(report.tunaiTotal))
        lines += twoColumns("QRIS (${report.qrisCount})", formatCurrency(report.qrisTotal))
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
        if (report.discountTotal > 0) lines += twoColumns("Diskon", formatCurrency(report.discountTotal))
        lines += twoColumns("Penjualan Bersih", formatCurrency(report.netSales))
        lines += twoColumns("Tunai (${report.tunaiCount})", formatCurrency(report.tunaiTotal))
        lines += twoColumns("QRIS (${report.qrisCount})", formatCurrency(report.qrisTotal))
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
        lines += wrap(StoreInfo.address).map { center(it) }
        lines += center("IG: ${StoreInfo.instagram}")
        lines += center("WA: ${StoreInfo.whatsapp}")
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
        lines += wrap(name)
        variation?.let { value ->
            lines += wrap(value, columns - 2).map { "  $it" }
        }
        lines += twoColumns("Qty $quantity @ ${formatCurrency(price)}", formatCurrency(subtotal))
    }

    private fun appendTotals(
        lines: MutableList<String>,
        subtotal: Long,
        discountAmount: Long,
        total: Long,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: String,
        status: String,
        note: String?
    ) {
        lines += line()
        lines += twoColumns("Metode", paymentMethod)
        lines += twoColumns("Subtotal", formatCurrency(subtotal))
        if (discountAmount > 0) lines += twoColumns("Diskon", "-${formatCurrency(discountAmount)}")
        lines += line()
        lines += twoColumns("TOTAL", formatCurrency(total))
        lines += line()
        lines += twoColumns("Dibayar", formatCurrency(paymentAmount))
        lines += twoColumns("Kembalian", formatCurrency(changeAmount))
        lines += twoColumns("Status", status)
        note?.takeIf { it.isNotBlank() }?.let {
            lines += line()
            lines += wrap("Catatan: $it")
        }
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
        out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
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

    private fun wrap(text: String, maxColumns: Int = columns): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            var remaining = word
            while (remaining.isNotEmpty()) {
                val candidate = if (current.isBlank()) remaining else "$current $remaining"
                if (candidate.length <= maxColumns) {
                    current = candidate
                    remaining = ""
                } else if (current.isNotBlank()) {
                    lines += current
                    current = ""
                } else {
                    lines += remaining.take(maxColumns)
                    remaining = remaining.drop(maxColumns)
                }
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun twoColumns(left: String, right: String): String {
        val safeRight = right.take(printableColumns)
        val available = printableColumns - safeRight.length
        val safeLeft = left.take((available - 1).coerceAtLeast(1))
        return safeLeft.padEnd(available.coerceAtLeast(safeLeft.length + 1)) + safeRight
    }

    private fun center(text: String): String {
        val safe = text.take(printableColumns)
        return " ".repeat(((printableColumns - safe.length) / 2).coerceAtLeast(0)) + safe
    }

    private fun line(): String = "-".repeat(columns)

    private fun String.toPaymentLabel(): String {
        return runCatching { PaymentMethod.valueOf(this).label }.getOrDefault(this)
    }

    private fun String.toStatusLabel(): String {
        return runCatching { TransactionStatus.valueOf(this).label }.getOrDefault(this)
    }
}
