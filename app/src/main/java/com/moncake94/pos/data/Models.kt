package com.moncake94.pos.data

import androidx.room.Embedded
import androidx.room.Relation
import com.moncake94.pos.util.formatCurrency
import com.moncake94.pos.util.formatDate
import com.moncake94.pos.util.formatDateTime

data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "id", entityColumn = "product_id", entity = ProductVariationEntity::class)
    val variations: List<VariationWithOptions>
)

data class VariationWithOptions(
    @Embedded val variation: ProductVariationEntity,
    @Relation(parentColumn = "id", entityColumn = "variation_id")
    val options: List<ProductVariationOptionEntity>
)

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "id", entityColumn = "transaction_id")
    val items: List<TransactionItemEntity>
)

data class DailySummary(
    val totalSales: Long,
    val transactionCount: Int
)

data class BestSeller(
    val productName: String,
    val quantitySold: Long
)

data class DailyReport(
    val dateMillis: Long,
    val grossSales: Long,
    val voidTotal: Long,
    val refundTotal: Long,
    val netSales: Long,
    val successCount: Int,
    val voidCount: Int,
    val refundCount: Int,
    val tunaiTotal: Long,
    val qrisTotal: Long,
    val itemSoldCount: Int,
    val bestSellers: List<BestSeller>,
    val transactions: List<TransactionWithItems>
)

fun DailyReport.toClosingEntity(): ClosingReportEntity {
    return ClosingReportEntity(
        reportDate = dateMillis,
        grossSales = grossSales,
        voidTotal = voidTotal,
        refundTotal = refundTotal,
        netSales = netSales,
        successCount = successCount,
        voidCount = voidCount,
        refundCount = refundCount,
        tunaiTotal = tunaiTotal,
        qrisTotal = qrisTotal,
        itemSoldCount = itemSoldCount,
        bestSellersText = bestSellers.joinToString("\n") { "${it.productName} (${it.quantitySold})" }
    )
}

fun DailyReport.toClosingText(): String {
    return buildString {
        appendLine("moncake94")
        appendLine("LAPORAN CLOSING")
        appendLine("Tanggal: ${formatDate(dateMillis)}")
        appendLine("Dibuat: ${formatDateTime(System.currentTimeMillis())}")
        appendLine()
        appendLine("Total Penjualan: ${formatCurrency(grossSales)}")
        appendLine("Penjualan Bersih: ${formatCurrency(netSales)}")
        appendLine("Tunai: ${formatCurrency(tunaiTotal)}")
        appendLine("QRIS: ${formatCurrency(qrisTotal)}")
        appendLine("Void: ${formatCurrency(voidTotal)}")
        appendLine("Refund: ${formatCurrency(refundTotal)}")
        appendLine()
        appendLine("Transaksi Sukses: $successCount")
        appendLine("Transaksi Void: $voidCount")
        appendLine("Transaksi Refund: $refundCount")
        appendLine("Item Terjual: $itemSoldCount")
        if (bestSellers.isNotEmpty()) {
            appendLine()
            appendLine("Produk Terlaris:")
            bestSellers.forEach { appendLine("${it.productName} (${it.quantitySold})") }
        }
    }
}

fun DailyReport.toClosingJson(): String {
    return org.json.JSONObject()
        .put("type", "moncake94_closing_report")
        .put("date", formatDate(dateMillis))
        .put("created_at", System.currentTimeMillis())
        .put("gross_sales", grossSales)
        .put("void_total", voidTotal)
        .put("refund_total", refundTotal)
        .put("net_sales", netSales)
        .put("success_count", successCount)
        .put("void_count", voidCount)
        .put("refund_count", refundCount)
        .put("tunai_total", tunaiTotal)
        .put("qris_total", qrisTotal)
        .put("item_sold_count", itemSoldCount)
        .put("best_sellers", org.json.JSONArray().also { array ->
            bestSellers.forEach { array.put(org.json.JSONObject().put("product_name", it.productName).put("quantity_sold", it.quantitySold)) }
        })
        .toString(2)
}

fun ClosingReportEntity.toClosingText(): String {
    return buildString {
        appendLine("moncake94")
        appendLine("RIWAYAT CLOSING")
        appendLine("Tanggal: ${formatDate(reportDate)}")
        appendLine("Dicetak: ${formatDateTime(printedAt)}")
        appendLine()
        appendLine("Total Penjualan: ${formatCurrency(grossSales)}")
        appendLine("Penjualan Bersih: ${formatCurrency(netSales)}")
        appendLine("Tunai: ${formatCurrency(tunaiTotal)}")
        appendLine("QRIS: ${formatCurrency(qrisTotal)}")
        appendLine("Void: ${formatCurrency(voidTotal)}")
        appendLine("Refund: ${formatCurrency(refundTotal)}")
        appendLine()
        appendLine("Transaksi Sukses: $successCount")
        appendLine("Transaksi Void: $voidCount")
        appendLine("Transaksi Refund: $refundCount")
        appendLine("Item Terjual: $itemSoldCount")
        if (bestSellersText.isNotBlank()) {
            appendLine()
            appendLine("Produk Terlaris:")
            appendLine(bestSellersText)
        }
    }
}
