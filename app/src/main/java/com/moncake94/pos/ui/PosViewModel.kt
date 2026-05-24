package com.moncake94.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.moncake94.pos.data.BackupRepository
import com.moncake94.pos.data.CartItem
import com.moncake94.pos.data.CategoryEntity
import com.moncake94.pos.data.ClosingReportEntity
import com.moncake94.pos.data.DailyReport
import com.moncake94.pos.data.PaymentMethod
import com.moncake94.pos.data.PosRepository
import com.moncake94.pos.data.ProductVariationOptionEntity
import com.moncake94.pos.data.ProductWithDetails
import com.moncake94.pos.data.TransactionStatus
import com.moncake94.pos.data.TransactionWithItems
import com.moncake94.pos.data.toClosingJson
import com.moncake94.pos.data.toClosingText
import com.moncake94.pos.printer.PrinterResult
import com.moncake94.pos.printer.PrinterService
import com.moncake94.pos.printer.PrinterTarget
import com.moncake94.pos.printer.PrinterType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class UiState(
    val products: List<ProductWithDetails> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionWithItems> = emptyList(),
    val closingReports: List<ClosingReportEntity> = emptyList(),
    val bestSellers: List<com.moncake94.pos.data.BestSeller> = emptyList(),
    val search: String = "",
    val selectedCategoryId: Long? = null,
    val cart: List<CartItem> = emptyList(),
    val paymentText: String = "",
    val discountText: String = "",
    val transactionNote: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val reportDate: LocalDate = LocalDate.now(),
    val totalSalesToday: Long = 0,
    val transactionCountToday: Int = 0,
    val selectedPrinter: PrinterTarget? = null,
    val bluetoothPrinters: List<PrinterTarget> = emptyList(),
    val usbPrinters: List<PrinterTarget> = emptyList()
) {
    val subtotal: Long = cart.sumOf { it.subtotal }
    val discountAmount: Long = discountText.filter { it.isDigit() }.toLongOrNull()?.coerceAtMost(subtotal) ?: 0
    val total: Long = (subtotal - discountAmount).coerceAtLeast(0)
    val itemCount: Int = cart.sumOf { it.quantity }
    val cashInputAmount: Long = paymentText.filter { it.isDigit() }.toLongOrNull() ?: 0
    val paymentAmount: Long = if (paymentMethod == PaymentMethod.QRIS) total else cashInputAmount
    val changeAmount: Long = if (paymentMethod == PaymentMethod.QRIS) 0 else (paymentAmount - total).coerceAtLeast(0)
    val shortageAmount: Long = (total - paymentAmount).coerceAtLeast(0)
    val isPaymentEnough: Boolean = cart.isNotEmpty() && (paymentMethod == PaymentMethod.QRIS || paymentAmount >= total)
}

class PosViewModel(
    private val repository: PosRepository,
    private val printerService: PrinterService,
    private val backupRepository: BackupRepository
) : ViewModel() {
    private val localState = MutableStateFlow(UiState(selectedPrinter = printerService.getDefaultPrinter()))
    private val events = MutableSharedFlow<String>()
    val messages = events.asSharedFlow()

    val state: StateFlow<UiState> = combine(localState, repository.products) { local, products ->
        local.copy(products = products)
    }.let { flow ->
        combine(flow, repository.categories) { current, categories -> current.copy(categories = categories) }
    }.let { flow ->
        combine(flow, repository.transactions) { current, transactions -> current.copy(transactions = transactions) }
    }.let { flow ->
        combine(flow, repository.closingReports) { current, closingReports -> current.copy(closingReports = closingReports) }
    }.let { flow ->
        combine(flow, repository.observeTotalSalesToday()) { current, total -> current.copy(totalSalesToday = total) }
    }.let { flow ->
        combine(flow, repository.observeTransactionCountToday()) { current, count -> current.copy(transactionCountToday = count) }
    }.let { flow ->
        combine(flow, repository.bestSellers) { current, bestSellers -> current.copy(bestSellers = bestSellers) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            refreshPrinters()
        }
    }

    fun search(value: String) = update { it.copy(search = value) }
    fun selectCategory(id: Long?) = update { it.copy(selectedCategoryId = id) }
    fun payment(value: String) = update { it.copy(paymentText = value.filter { c -> c.isDigit() }) }
    fun discount(value: String) = update { it.copy(discountText = value.filter { c -> c.isDigit() }) }
    fun transactionNote(value: String) = update { it.copy(transactionNote = value) }
    fun selectPaymentMethod(method: PaymentMethod) = update { it.copy(paymentMethod = method) }
    fun selectReportDate(date: LocalDate) = update { it.copy(reportDate = date) }

    fun addProduct(product: ProductWithDetails, option: ProductVariationOptionEntity? = null) {
        val price = option?.optionPrice ?: product.product.basePrice
        val variation = product.variations.firstOrNull()
        val key = "${product.product.id}:${option?.id ?: 0}"
        update { current ->
            val existing = current.cart.firstOrNull { it.key == key }
            val cart = if (existing == null) {
                current.cart + CartItem(
                    key = key,
                    productId = product.product.id,
                    productName = product.product.name,
                    variationName = option?.let { variation?.variation?.name },
                    optionName = option?.optionName,
                    price = price,
                    quantity = 1
                )
            } else {
                current.cart.map { if (it.key == key) it.copy(quantity = it.quantity + 1) else it }
            }
            current.copy(cart = cart)
        }
    }

    fun increase(key: String) = update { it.copy(cart = it.cart.map { item -> if (item.key == key) item.copy(quantity = item.quantity + 1) else item }) }

    fun decrease(key: String) = update {
        it.copy(cart = it.cart.mapNotNull { item ->
            when {
                item.key != key -> item
                item.quantity > 1 -> item.copy(quantity = item.quantity - 1)
                else -> null
            }
        })
    }

    fun remove(key: String) = update { it.copy(cart = it.cart.filterNot { item -> item.key == key }) }
    fun clearCart() = update { it.copy(cart = emptyList(), paymentText = "", discountText = "", transactionNote = "", paymentMethod = PaymentMethod.TUNAI) }

    fun checkout(printAfterSave: Boolean) {
        val snapshot = state.value
        if (snapshot.cart.isEmpty()) return message("Keranjang masih kosong")
        if (!snapshot.isPaymentEnough) return message("Uang diterima belum cukup")
        viewModelScope.launch {
            val transactionId = repository.saveTransaction(
                cartItems = snapshot.cart,
                paymentAmount = snapshot.paymentAmount,
                changeAmount = snapshot.changeAmount,
                paymentMethod = snapshot.paymentMethod,
                discountAmount = snapshot.discountAmount,
                note = snapshot.transactionNote
            )
            val saved = repository.getTransaction(transactionId)
            update { it.copy(cart = emptyList(), paymentText = "", discountText = "", transactionNote = "", paymentMethod = PaymentMethod.TUNAI) }
            message("Transaksi tersimpan")
            if (printAfterSave && saved != null) printTransaction(saved)
        }
    }

    fun saveCategory(id: Long?, name: String) {
        if (name.isBlank()) return message("Nama kategori wajib diisi")
        viewModelScope.launch {
            repository.saveCategory(id, name)
            message("Kategori tersimpan")
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            if (repository.deleteCategoryIfSafe(categoryId)) message("Kategori dihapus") else message("Kategori masih dipakai produk")
        }
    }

    fun saveProduct(id: Long?, name: String, categoryId: Long?, basePrice: Long, isAvailable: Boolean, variationName: String?, options: List<Pair<String, Long>>) {
        if (name.isBlank()) return message("Nama produk wajib diisi")
        if (options.isEmpty() && basePrice <= 0) return message("Harga produk wajib diisi")
        viewModelScope.launch {
            repository.saveProduct(id, name, categoryId, basePrice, isAvailable, variationName, options)
            message("Produk tersimpan")
        }
    }

    fun setProductAvailability(productId: Long, isAvailable: Boolean) {
        viewModelScope.launch {
            if (repository.setProductAvailability(productId, isAvailable)) {
                message(if (isAvailable) "Produk tersedia" else "Produk ditandai habis")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            message("Produk dihapus")
        }
    }

    fun voidTransaction(transaction: TransactionWithItems, reason: String?) {
        viewModelScope.launch {
            if (repository.voidTransaction(transaction, reason)) message("Transaksi berhasil di-void") else message("Transaksi tidak bisa di-void")
        }
    }

    fun refundTransaction(transaction: TransactionWithItems, reason: String?) {
        viewModelScope.launch {
            if (repository.refundTransaction(transaction, reason)) message("Transaksi berhasil di-refund") else message("Transaksi tidak bisa di-refund")
        }
    }

    fun refreshPrinters() {
        val bluetoothResult = printerService.listBluetoothPrinters()
        update {
            it.copy(
                selectedPrinter = printerService.getDefaultPrinter(),
                bluetoothPrinters = bluetoothResult.printers,
                usbPrinters = printerService.listUsbPrinters()
            )
        }
        bluetoothResult.errorMessage?.let { message(it) }
    }

    fun savePrinter(target: PrinterTarget) {
        printerService.saveDefaultPrinter(target)
        update { it.copy(selectedPrinter = target) }
        message("Printer disimpan")
    }

    fun saveWifiPrinter(host: String) {
        if (host.isBlank()) return message("Alamat printer WiFi wajib diisi")
        savePrinter(PrinterTarget(PrinterType.WIFI, "WiFi $host", host.trim()))
    }

    fun testPrint() {
        viewModelScope.launch {
            handlePrintResult(printerService.testPrint(), "Test print dikirim")
        }
    }

    fun printTransaction(transaction: TransactionWithItems) {
        viewModelScope.launch {
            handlePrintResult(printerService.printTransactionReceipt(transaction), "Struk dikirim")
        }
    }

    fun printClosingReport(report: DailyReport) {
        viewModelScope.launch {
            when (val result = printerService.printClosingReport(report)) {
                PrinterResult.Success -> {
                    repository.saveClosingReport(report)
                    message("Closing berhasil dicetak dan disimpan.")
                }
                is PrinterResult.Error -> {
                    if (result.message.contains("Printer belum dipilih", ignoreCase = true)) {
                        message("Printer belum dipilih.")
                    } else {
                        message("Gagal mencetak closing.")
                    }
                }
            }
        }
    }

    fun saveClosingReport(report: DailyReport) {
        viewModelScope.launch {
            repository.saveClosingReport(report)
            message("Closing berhasil disimpan.")
        }
    }

    fun printSavedClosingReport(report: ClosingReportEntity) {
        viewModelScope.launch {
            when (val result = printerService.printSavedClosingReport(report)) {
                PrinterResult.Success -> message("Closing berhasil dicetak.")
                is PrinterResult.Error -> message(if (result.message.contains("Printer belum dipilih", true)) "Printer belum dipilih." else "Gagal mencetak closing.")
            }
        }
    }

    fun exportClosingText(uri: Uri, report: DailyReport) {
        viewModelScope.launch {
            if (backupRepository.exportText(uri, report.toClosingText())) message("Backup closing berhasil disimpan.") else message("Backup closing gagal.")
        }
    }

    fun exportClosingJson(uri: Uri, report: DailyReport) {
        viewModelScope.launch {
            if (backupRepository.exportText(uri, report.toClosingJson())) message("Backup closing berhasil disimpan.") else message("Backup closing gagal.")
        }
    }

    fun exportSavedClosingText(uri: Uri, report: ClosingReportEntity) {
        viewModelScope.launch {
            if (backupRepository.exportText(uri, report.toClosingText())) message("Backup closing berhasil disimpan.") else message("Backup closing gagal.")
        }
    }

    fun backupTo(uri: Uri) {
        viewModelScope.launch {
            if (backupRepository.backupTo(uri)) message("Backup data berhasil disimpan.") else message("Backup data gagal.")
        }
    }

    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            if (backupRepository.restoreFrom(uri)) message("Restore data berhasil.") else message("Restore data gagal. File backup tidak valid.")
        }
    }

    private fun update(block: (UiState) -> UiState) {
        localState.value = block(localState.value)
    }

    private fun message(text: String) {
        viewModelScope.launch { events.emit(text) }
    }

    private suspend fun handlePrintResult(result: PrinterResult, successMessage: String) {
        when (result) {
            PrinterResult.Success -> message(successMessage)
            is PrinterResult.Error -> message(result.message)
        }
    }
}

internal fun buildDailyReport(date: LocalDate, transactions: List<TransactionWithItems>): DailyReport {
    val zone = ZoneId.systemDefault()
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val createdOnDate = transactions.filter { it.transaction.createdAt in start..end }
    val voidedOnDate = transactions.filter {
        it.transaction.status == TransactionStatus.VOID.name &&
            ((it.transaction.voidedAt ?: it.transaction.createdAt) in start..end)
    }
    val refundedOnDate = transactions.filter {
        it.transaction.status == TransactionStatus.REFUNDED.name &&
            ((it.transaction.refundedAt ?: it.transaction.createdAt) in start..end)
    }
    val dailyTransactions = (createdOnDate + voidedOnDate + refundedOnDate)
        .distinctBy { it.transaction.id }
        .sortedByDescending { it.transaction.createdAt }
    val success = createdOnDate.filter { it.transaction.status == TransactionStatus.SUCCESS.name }
    val bestSellers = success
        .flatMap { it.items }
        .groupBy { it.productName }
        .map { (name, items) -> com.moncake94.pos.data.BestSeller(name, items.sumOf { it.quantity }.toLong()) }
        .sortedByDescending { it.quantitySold }
        .take(5)

    val grossSales = createdOnDate.sumOf { it.transaction.subtotal.takeIf { subtotal -> subtotal > 0 } ?: it.transaction.total }
    val discountTotal = createdOnDate.sumOf { it.transaction.discountAmount }
    val voidTotal = voidedOnDate.sumOf { it.transaction.total }
    val refundTotal = refundedOnDate.sumOf { it.transaction.refundAmount.takeIf { amount -> amount > 0 } ?: it.transaction.total }

    return DailyReport(
        dateMillis = start,
        grossSales = grossSales,
        discountTotal = discountTotal,
        voidTotal = voidTotal,
        refundTotal = refundTotal,
        netSales = grossSales - discountTotal - voidTotal - refundTotal,
        successCount = success.size,
        voidCount = voidedOnDate.size,
        refundCount = refundedOnDate.size,
        tunaiTotal = success.filter { it.transaction.paymentMethod == PaymentMethod.TUNAI.name }.sumOf { it.transaction.total },
        qrisTotal = success.filter { it.transaction.paymentMethod == PaymentMethod.QRIS.name }.sumOf { it.transaction.total },
        tunaiCount = success.count { it.transaction.paymentMethod == PaymentMethod.TUNAI.name },
        qrisCount = success.count { it.transaction.paymentMethod == PaymentMethod.QRIS.name },
        itemSoldCount = success.flatMap { it.items }.sumOf { it.quantity },
        bestSellers = bestSellers,
        transactions = dailyTransactions
    )
}
