package com.moncake94.pos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM products ORDER BY name")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM product_variations ORDER BY id")
    suspend fun getAllVariations(): List<ProductVariationEntity>

    @Query("SELECT * FROM product_variation_options ORDER BY id")
    suspend fun getAllVariationOptions(): List<ProductVariationOptionEntity>

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transaction_items ORDER BY id")
    suspend fun getAllTransactionItems(): List<TransactionItemEntity>

    @Transaction
    @Query("SELECT * FROM products ORDER BY name")
    fun observeProductDetails(): Flow<List<ProductWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun observeTransactions(): Flow<List<TransactionWithItems>>

    @Query("SELECT * FROM closing_reports ORDER BY printed_at DESC")
    fun observeClosingReports(): Flow<List<ClosingReportEntity>>

    @Query("SELECT * FROM closing_reports ORDER BY id")
    suspend fun getAllClosingReports(): List<ClosingReportEntity>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionWithItems?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM products WHERE category_id = :categoryId")
    suspend fun countProductsInCategory(categoryId: Long): Int

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Insert
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: Long)

    @Query("UPDATE products SET is_available = :isAvailable, updated_at = :updatedAt WHERE id = :productId")
    suspend fun updateProductAvailability(productId: Long, isAvailable: Boolean, updatedAt: Long): Int

    @Query("DELETE FROM product_variations WHERE product_id = :productId")
    suspend fun deleteVariationsForProduct(productId: Long)

    @Insert
    suspend fun insertVariation(variation: ProductVariationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariations(variations: List<ProductVariationEntity>)

    @Insert
    suspend fun insertVariationOption(option: ProductVariationOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariationOptions(options: List<ProductVariationOptionEntity>)

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceTransactionItems(items: List<TransactionItemEntity>)

    @Insert
    suspend fun insertClosingReport(report: ClosingReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingReports(reports: List<ClosingReportEntity>)

    @Query("SELECT * FROM closing_reports WHERE id = :id")
    suspend fun getClosingReport(id: Long): ClosingReportEntity?

    @Query("DELETE FROM transaction_items")
    suspend fun clearTransactionItems()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM product_variation_options")
    suspend fun clearVariationOptions()

    @Query("DELETE FROM product_variations")
    suspend fun clearVariations()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM closing_reports")
    suspend fun clearClosingReports()

    @Query(
        "UPDATE transactions SET status = 'VOID', void_reason = :reason, voided_at = :voidedAt " +
            "WHERE id = :transactionId AND status = 'SUCCESS'"
    )
    suspend fun voidTransaction(transactionId: Long, reason: String?, voidedAt: Long): Int

    @Query(
        "UPDATE transactions SET status = 'REFUNDED', refund_reason = :reason, refunded_at = :refundedAt, refund_amount = :refundAmount " +
            "WHERE id = :transactionId AND status = 'SUCCESS'"
    )
    suspend fun refundTransaction(transactionId: Long, refundAmount: Long, reason: String?, refundedAt: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM transactions WHERE created_at BETWEEN :start AND :end")
    fun observeTotalSalesBetween(start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :start AND :end")
    fun observeTransactionCountBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :start AND :end")
    suspend fun countTransactionsBetween(start: Long, end: Long): Int

    @Query(
        "SELECT product_name AS productName, SUM(quantity) AS quantitySold " +
            "FROM transaction_items GROUP BY product_name ORDER BY quantitySold DESC LIMIT 5"
    )
    fun observeBestSellers(): Flow<List<BestSeller>>
}
