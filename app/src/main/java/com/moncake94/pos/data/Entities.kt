package com.moncake94.pos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = now()
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("category_id")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "base_price") val basePrice: Long,
    @ColumnInfo(name = "has_variations") val hasVariations: Boolean,
    @ColumnInfo(name = "is_available", defaultValue = "1") val isAvailable: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = now()
)

@Entity(
    tableName = "product_variations",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("product_id")]
)
data class ProductVariationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = now()
)

@Entity(
    tableName = "product_variation_options",
    foreignKeys = [
        ForeignKey(
            entity = ProductVariationEntity::class,
            parentColumns = ["id"],
            childColumns = ["variation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("variation_id")]
)
data class ProductVariationOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "variation_id") val variationId: Long,
    @ColumnInfo(name = "option_name") val optionName: String,
    @ColumnInfo(name = "option_price") val optionPrice: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = now(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = now()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_number") val transactionNumber: String,
    @ColumnInfo(defaultValue = "0") val subtotal: Long = 0,
    @ColumnInfo(name = "discount_amount", defaultValue = "0") val discountAmount: Long = 0,
    val total: Long,
    @ColumnInfo(name = "payment_amount") val paymentAmount: Long,
    @ColumnInfo(name = "change_amount") val changeAmount: Long,
    @ColumnInfo(name = "payment_method", defaultValue = "'TUNAI'") val paymentMethod: String = PaymentMethod.TUNAI.name,
    @ColumnInfo(defaultValue = "'SUCCESS'") val status: String = TransactionStatus.SUCCESS.name,
    @ColumnInfo(name = "void_reason") val voidReason: String? = null,
    @ColumnInfo(name = "voided_at") val voidedAt: Long? = null,
    @ColumnInfo(name = "refunded_at") val refundedAt: Long? = null,
    @ColumnInfo(name = "refund_amount", defaultValue = "0") val refundAmount: Long = 0,
    @ColumnInfo(name = "refund_reason") val refundReason: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = now()
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transaction_id"), Index("product_id")]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "selected_variation_name") val selectedVariationName: String?,
    @ColumnInfo(name = "selected_variation_option") val selectedVariationOption: String?,
    val price: Long,
    val quantity: Int,
    val subtotal: Long
)

@Entity(tableName = "closing_reports", indices = [Index("report_date")])
data class ClosingReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "report_date") val reportDate: Long,
    @ColumnInfo(name = "printed_at") val printedAt: Long = now(),
    @ColumnInfo(name = "gross_sales") val grossSales: Long,
    @ColumnInfo(name = "discount_total", defaultValue = "0") val discountTotal: Long = 0,
    @ColumnInfo(name = "void_total") val voidTotal: Long,
    @ColumnInfo(name = "refund_total") val refundTotal: Long,
    @ColumnInfo(name = "net_sales") val netSales: Long,
    @ColumnInfo(name = "success_count") val successCount: Int,
    @ColumnInfo(name = "void_count") val voidCount: Int,
    @ColumnInfo(name = "refund_count") val refundCount: Int,
    @ColumnInfo(name = "tunai_total") val tunaiTotal: Long,
    @ColumnInfo(name = "qris_total") val qrisTotal: Long,
    @ColumnInfo(name = "tunai_count", defaultValue = "0") val tunaiCount: Int = 0,
    @ColumnInfo(name = "qris_count", defaultValue = "0") val qrisCount: Int = 0,
    @ColumnInfo(name = "item_sold_count") val itemSoldCount: Int,
    @ColumnInfo(name = "best_sellers_text") val bestSellersText: String
)

fun now(): Long = System.currentTimeMillis()

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    QRIS("QRIS")
}

enum class TransactionStatus(val label: String) {
    SUCCESS("Sukses"),
    VOID("Void"),
    REFUNDED("Refund")
}
