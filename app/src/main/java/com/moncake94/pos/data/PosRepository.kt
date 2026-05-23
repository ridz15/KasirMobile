package com.moncake94.pos.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class PosRepository(private val database: AppDatabase) {
    private val dao = database.dao()

    val categories = dao.observeCategories()
    val products = dao.observeProductDetails()
    val transactions = dao.observeTransactions()
    val closingReports = dao.observeClosingReports()
    val bestSellers = dao.observeBestSellers()

    fun observeTotalSalesToday(): Flow<Long> {
        val (start, end) = todayRange()
        return dao.observeTotalSalesBetween(start, end)
    }

    fun observeTransactionCountToday(): Flow<Int> {
        val (start, end) = todayRange()
        return dao.observeTransactionCountBetween(start, end)
    }

    suspend fun seedIfNeeded() {
        if (dao.countProducts() > 0) return
        val rekomendasi = dao.insertCategory(CategoryEntity(name = "Rekomendasi Favorit"))
        val burntCheesecake = dao.insertCategory(CategoryEntity(name = "Burnt Cheesecake"))
        val brownies = dao.insertCategory(CategoryEntity(name = "Brownies & Lainnya"))
        val cakeBolu = dao.insertCategory(CategoryEntity(name = "Aneka Cake / Bolu"))
        val dessert = dao.insertCategory(CategoryEntity(name = "Dessert"))

        addProduct(
            product = ProductEntity(name = "Soes Vanilla", categoryId = rekomendasi, basePrice = 10_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Soes Coklat", categoryId = rekomendasi, basePrice = 10_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Original", categoryId = burntCheesecake, basePrice = 25_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Choco Cookies", categoryId = burntCheesecake, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Brownies", categoryId = burntCheesecake, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Blueberry", categoryId = burntCheesecake, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Matcha", categoryId = burntCheesecake, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Burnt Cheesecake Strawberry", categoryId = burntCheesecake, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Brownies Insert Cheese", categoryId = brownies, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Brokies", categoryId = brownies, basePrice = 25_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Gabin Tape", categoryId = brownies, basePrice = 5_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Marmer Cake", categoryId = cakeBolu, basePrice = 0, hasVariations = true),
            variationName = "Ukuran",
            options = listOf("Satu Slice" to 8_000L, "Dua Slice" to 15_000L, "Whole" to 120_000L)
        )
        addProduct(
            product = ProductEntity(name = "Marmer Choco Cheese", categoryId = cakeBolu, basePrice = 0, hasVariations = true),
            variationName = "Ukuran",
            options = listOf("Slice" to 10_000L, "Whole" to 170_000L)
        )
        addProduct(
            product = ProductEntity(name = "Bolu Pisang Keju", categoryId = cakeBolu, basePrice = 0, hasVariations = true),
            variationName = "Ukuran",
            options = listOf("Slice" to 10_000L, "Whole" to 70_000L)
        )
        addProduct(
            product = ProductEntity(name = "Bolu Tape", categoryId = cakeBolu, basePrice = 35_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Bolu Kukus Ketan Hitam Keju Lumer", categoryId = cakeBolu, basePrice = 30_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Klappertart", categoryId = dessert, basePrice = 0, hasVariations = true),
            variationName = "Ukuran",
            options = listOf("S" to 10_000L, "L" to 20_000L)
        )
        addProduct(
            product = ProductEntity(name = "Mango Sago", categoryId = dessert, basePrice = 10_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Buko Pandan", categoryId = dessert, basePrice = 10_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Puding Chia Mango", categoryId = dessert, basePrice = 15_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Puding Chia Dragon Fruit", categoryId = dessert, basePrice = 15_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Dessert Box Choco Cheese", categoryId = dessert, basePrice = 10_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Cheesecake Strawberry Lumer", categoryId = dessert, basePrice = 25_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Brownies Lumer", categoryId = dessert, basePrice = 15_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Puding Coklat Belgian", categoryId = dessert, basePrice = 20_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
        addProduct(
            product = ProductEntity(name = "Tiramisu", categoryId = dessert, basePrice = 20_000L, hasVariations = false),
            variationName = null,
            options = emptyList()
        )
    }

    suspend fun saveCategory(id: Long?, name: String): Long {
        val trimmed = name.trim()
        if (id == null || id == 0L) return dao.insertCategory(CategoryEntity(name = trimmed))
        dao.updateCategory(CategoryEntity(id = id, name = trimmed, updatedAt = now()))
        return id
    }

    suspend fun deleteCategoryIfSafe(categoryId: Long): Boolean {
        if (dao.countProductsInCategory(categoryId) > 0) return false
        dao.deleteCategory(categoryId)
        return true
    }

    suspend fun saveProduct(
        id: Long?,
        name: String,
        categoryId: Long?,
        basePrice: Long,
        variationName: String?,
        options: List<Pair<String, Long>>
    ) {
        val hasVariations = options.isNotEmpty()
        val productId = if (id == null || id == 0L) {
            dao.insertProduct(
                ProductEntity(
                    name = name.trim(),
                    categoryId = categoryId,
                    basePrice = if (hasVariations) 0 else basePrice,
                    hasVariations = hasVariations
                )
            )
        } else {
            dao.updateProduct(
                ProductEntity(
                    id = id,
                    name = name.trim(),
                    categoryId = categoryId,
                    basePrice = if (hasVariations) 0 else basePrice,
                    hasVariations = hasVariations,
                    updatedAt = now()
                )
            )
            dao.deleteVariationsForProduct(id)
            id
        }

        if (hasVariations) {
            val variationId = dao.insertVariation(
                ProductVariationEntity(
                    productId = productId,
                    name = variationName?.trim().takeUnless { it.isNullOrBlank() } ?: "Variant"
                )
            )
            options.filter { it.first.isNotBlank() }.forEach { (option, price) ->
                dao.insertVariationOption(
                    ProductVariationOptionEntity(
                        variationId = variationId,
                        optionName = option.trim(),
                        optionPrice = price
                    )
                )
            }
        }
    }

    suspend fun deleteProduct(productId: Long) = dao.deleteProduct(productId)

    suspend fun saveTransaction(
        cartItems: List<CartItem>,
        paymentAmount: Long,
        changeAmount: Long,
        paymentMethod: PaymentMethod
    ): Long {
        val total = cartItems.sumOf { it.subtotal }
        val number = "TRX-${System.currentTimeMillis()}"
        val transactionId = dao.insertTransaction(
            TransactionEntity(
                transactionNumber = number,
                total = total,
                paymentAmount = paymentAmount,
                changeAmount = changeAmount,
                paymentMethod = paymentMethod.name,
                status = TransactionStatus.SUCCESS.name
            )
        )
        dao.insertTransactionItems(
            cartItems.map {
                TransactionItemEntity(
                    transactionId = transactionId,
                    productId = it.productId,
                    productName = it.productName,
                    selectedVariationName = it.variationName,
                    selectedVariationOption = it.optionName,
                    price = it.price,
                    quantity = it.quantity,
                    subtotal = it.subtotal
                )
            }
        )
        return transactionId
    }

    suspend fun getTransaction(id: Long): TransactionWithItems? = dao.getTransaction(id)

    suspend fun saveClosingReport(report: DailyReport): Long = dao.insertClosingReport(report.toClosingEntity())

    suspend fun getClosingReport(id: Long): ClosingReportEntity? = dao.getClosingReport(id)

    suspend fun voidTransaction(transaction: TransactionWithItems, reason: String?): Boolean {
        if (transaction.transaction.status != TransactionStatus.SUCCESS.name) return false
        return dao.voidTransaction(transaction.transaction.id, reason?.trim()?.takeIf { it.isNotBlank() }, now()) > 0
    }

    suspend fun refundTransaction(transaction: TransactionWithItems, reason: String?): Boolean {
        if (transaction.transaction.status != TransactionStatus.SUCCESS.name) return false
        return dao.refundTransaction(
            transactionId = transaction.transaction.id,
            refundAmount = transaction.transaction.total,
            reason = reason?.trim()?.takeIf { it.isNotBlank() },
            refundedAt = now()
        ) > 0
    }

    private suspend fun addProduct(
        product: ProductEntity,
        variationName: String?,
        options: List<Pair<String, Long>>
    ) {
        val productId = dao.insertProduct(product)
        if (options.isNotEmpty()) {
            val variationId = dao.insertVariation(ProductVariationEntity(productId = productId, name = variationName ?: "Variant"))
            options.forEach { (name, price) ->
                dao.insertVariationOption(ProductVariationOptionEntity(variationId = variationId, optionName = name, optionPrice = price))
            }
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}

data class CartItem(
    val key: String,
    val productId: Long,
    val productName: String,
    val variationName: String?,
    val optionName: String?,
    val price: Long,
    val quantity: Int
) {
    val subtotal: Long = price * quantity
}
