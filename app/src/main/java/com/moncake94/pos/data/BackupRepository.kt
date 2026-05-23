package com.moncake94.pos.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val dao = database.dao()

    suspend fun backupTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject()
                .put("version", 1)
                .put("app", "moncake94 POS")
                .put("created_at", now())
                .put("categories", dao.getAllCategories().toJsonArray { it.toJson() })
                .put("products", dao.getAllProducts().toJsonArray { it.toJson() })
                .put("product_variations", dao.getAllVariations().toJsonArray { it.toJson() })
                .put("product_variation_options", dao.getAllVariationOptions().toJsonArray { it.toJson() })
                .put("transactions", dao.getAllTransactions().toJsonArray { it.toJson() })
                .put("transaction_items", dao.getAllTransactionItems().toJsonArray { it.toJson() })

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Tidak bisa membuka file backup")
        }.isSuccess
    }

    suspend fun restoreFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("File backup tidak bisa dibaca")
            val json = JSONObject(text)
            if (json.optString("app") != "moncake94 POS") error("File backup tidak valid")

            val categories = json.getJSONArray("categories").mapObjects { it.toCategory() }
            val products = json.getJSONArray("products").mapObjects { it.toProduct() }
            val variations = json.getJSONArray("product_variations").mapObjects { it.toVariation() }
            val options = json.getJSONArray("product_variation_options").mapObjects { it.toVariationOption() }
            val transactions = json.getJSONArray("transactions").mapObjects { it.toTransaction() }
            val items = json.getJSONArray("transaction_items").mapObjects { it.toTransactionItem() }

            database.withTransaction {
                dao.clearTransactionItems()
                dao.clearTransactions()
                dao.clearVariationOptions()
                dao.clearVariations()
                dao.clearProducts()
                dao.clearCategories()
                dao.insertCategories(categories)
                dao.insertProducts(products)
                dao.insertVariations(variations)
                dao.insertVariationOptions(options)
                dao.insertTransactions(transactions)
                dao.replaceTransactionItems(items)
            }
        }.isSuccess
    }

    suspend fun exportText(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("Tidak bisa membuka file")
        }.isSuccess
    }

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        forEach { array.put(mapper(it)) }
        return array
    }

    private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
        return (0 until length()).map { mapper(getJSONObject(it)) }
    }

    private fun CategoryEntity.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun ProductEntity.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("category_id", categoryId)
        .put("base_price", basePrice)
        .put("has_variations", hasVariations)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun ProductVariationEntity.toJson() = JSONObject()
        .put("id", id)
        .put("product_id", productId)
        .put("name", name)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun ProductVariationOptionEntity.toJson() = JSONObject()
        .put("id", id)
        .put("variation_id", variationId)
        .put("option_name", optionName)
        .put("option_price", optionPrice)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun TransactionEntity.toJson() = JSONObject()
        .put("id", id)
        .put("transaction_number", transactionNumber)
        .put("total", total)
        .put("payment_amount", paymentAmount)
        .put("change_amount", changeAmount)
        .put("payment_method", paymentMethod)
        .put("status", status)
        .put("void_reason", voidReason)
        .put("voided_at", voidedAt)
        .put("refunded_at", refundedAt)
        .put("refund_amount", refundAmount)
        .put("refund_reason", refundReason)
        .put("created_at", createdAt)

    private fun TransactionItemEntity.toJson() = JSONObject()
        .put("id", id)
        .put("transaction_id", transactionId)
        .put("product_id", productId)
        .put("product_name", productName)
        .put("selected_variation_name", selectedVariationName)
        .put("selected_variation_option", selectedVariationOption)
        .put("price", price)
        .put("quantity", quantity)
        .put("subtotal", subtotal)

    private fun JSONObject.toCategory() = CategoryEntity(
        id = getLong("id"),
        name = getString("name"),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at")
    )

    private fun JSONObject.toProduct() = ProductEntity(
        id = getLong("id"),
        name = getString("name"),
        categoryId = optNullableLong("category_id"),
        basePrice = getLong("base_price"),
        hasVariations = getBoolean("has_variations"),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at")
    )

    private fun JSONObject.toVariation() = ProductVariationEntity(
        id = getLong("id"),
        productId = getLong("product_id"),
        name = getString("name"),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at")
    )

    private fun JSONObject.toVariationOption() = ProductVariationOptionEntity(
        id = getLong("id"),
        variationId = getLong("variation_id"),
        optionName = getString("option_name"),
        optionPrice = getLong("option_price"),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at")
    )

    private fun JSONObject.toTransaction() = TransactionEntity(
        id = getLong("id"),
        transactionNumber = getString("transaction_number"),
        total = getLong("total"),
        paymentAmount = getLong("payment_amount"),
        changeAmount = getLong("change_amount"),
        paymentMethod = optString("payment_method", PaymentMethod.TUNAI.name),
        status = optString("status", TransactionStatus.SUCCESS.name),
        voidReason = optNullableString("void_reason"),
        voidedAt = optNullableLong("voided_at"),
        refundedAt = optNullableLong("refunded_at"),
        refundAmount = optLong("refund_amount", 0),
        refundReason = optNullableString("refund_reason"),
        createdAt = getLong("created_at")
    )

    private fun JSONObject.toTransactionItem() = TransactionItemEntity(
        id = getLong("id"),
        transactionId = getLong("transaction_id"),
        productId = getLong("product_id"),
        productName = getString("product_name"),
        selectedVariationName = optNullableString("selected_variation_name"),
        selectedVariationOption = optNullableString("selected_variation_option"),
        price = getLong("price"),
        quantity = getInt("quantity"),
        subtotal = getLong("subtotal")
    )

    private fun JSONObject.optNullableString(name: String): String? {
        return if (isNull(name)) null else optString(name)
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        return if (isNull(name)) null else optLong(name)
    }
}
