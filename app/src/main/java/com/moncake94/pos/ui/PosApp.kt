package com.moncake94.pos.ui

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moncake94.pos.data.CategoryEntity
import com.moncake94.pos.data.ClosingReportEntity
import com.moncake94.pos.data.PaymentMethod
import com.moncake94.pos.data.ProductWithDetails
import com.moncake94.pos.data.TransactionStatus
import com.moncake94.pos.data.TransactionWithItems
import com.moncake94.pos.ui.printer.PrinterSetupScreen
import com.moncake94.pos.util.formatCurrency
import com.moncake94.pos.util.formatDate
import com.moncake94.pos.util.formatDateTime
import com.moncake94.pos.util.formatThousandsInput
import com.moncake94.pos.util.parseCurrency
import java.time.LocalDate

private enum class Tab(val label: String) { Cashier("Kasir"), Products("Produk"), History("Riwayat"), Printer("Printer") }
private enum class AdminTab(val label: String) { Products("Produk"), Categories("Kategori") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(viewModel: PosViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(Tab.Cashier) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHost.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text("moncake94 POS", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            when (item) {
                                Tab.Cashier -> Icon(Icons.Default.ShoppingCart, null)
                                Tab.Products -> Icon(Icons.Default.Inventory2, null)
                                Tab.History -> Icon(Icons.AutoMirrored.Filled.ReceiptLong, null)
                                Tab.Printer -> Icon(Icons.Default.Print, null)
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Cashier -> CashierScreen(state, viewModel)
                Tab.Products -> ProductAdminScreen(state, viewModel)
                Tab.History -> HistoryScreen(state, viewModel)
                Tab.Printer -> PrinterSetupScreen(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CashierScreen(state: UiState, viewModel: PosViewModel) {
    var selectedProduct by remember { mutableStateOf<ProductWithDetails?>(null) }
    var cartOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredProducts by remember(state.products, state.search, state.selectedCategoryId) {
        derivedStateOf {
            state.products.filter {
                val matchesSearch = state.search.isBlank() || it.product.name.contains(state.search, ignoreCase = true)
                val matchesCategory = state.selectedCategoryId == null || it.product.categoryId == state.selectedCategoryId
                matchesSearch && matchesCategory
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::search,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Cari produk") },
                    singleLine = true
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 12.dp)) {
                    item {
                        FilterChip(selected = state.selectedCategoryId == null, onClick = { viewModel.selectCategory(null) }, label = { Text("Semua") })
                    }
                    items(state.categories, key = { category -> "cashier-category-${category.id}" }) {
                        FilterChip(selected = state.selectedCategoryId == it.id, onClick = { viewModel.selectCategory(it.id) }, label = { Text(it.name) })
                    }
                }
            }
            items(filteredProducts, key = { product -> "cashier-product-${product.product.id}" }) { product ->
                ProductButton(product) {
                    if (!product.product.isAvailable) {
                        return@ProductButton
                    } else if (product.product.hasVariations && product.variations.flatMap { it.options }.isNotEmpty()) {
                        selectedProduct = product
                    } else {
                        viewModel.addProduct(product)
                    }
                }
            }
        }

        CartSummary(
            modifier = Modifier.align(Alignment.BottomCenter),
            itemCount = state.itemCount,
            total = state.total,
            onOpen = { cartOpen = true }
        )
    }

    selectedProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text(product.product.name) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pilih variasi", fontWeight = FontWeight.SemiBold)
                    product.variations.forEach { variation ->
                        Text(variation.variation.name)
                        variation.options.forEach { option ->
                            Button(
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                onClick = {
                                    viewModel.addProduct(product, option)
                                    selectedProduct = null
                                }
                            ) {
                                Text("${option.optionName} - ${formatCurrency(option.optionPrice)}")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedProduct = null }) { Text("Tutup") } }
        )
    }

    if (cartOpen) {
        ModalBottomSheet(onDismissRequest = { cartOpen = false }, sheetState = sheetState) {
            CartSheet(state, viewModel, onClose = { cartOpen = false })
        }
    }
}

@Composable
private fun ProductButton(product: ProductWithDetails, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.product.isAvailable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    product.product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(product.category?.name ?: "Tanpa kategori", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (product.product.hasVariations) {
                    Text("Ada variasi", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                }
                if (!product.product.isAvailable) {
                    Text("Habis", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            val priceText = remember(product) {
                if (product.product.hasVariations) {
                    product.variations.flatMap { it.options }.minOfOrNull { it.optionPrice }?.let { "Mulai ${formatCurrency(it)}" } ?: "Pilih"
                } else {
                    formatCurrency(product.product.basePrice)
                }
            }
            Text(priceText, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CartSummary(modifier: Modifier, itemCount: Int, total: Long, onOpen: () -> Unit) {
    Surface(modifier.fillMaxWidth().navigationBarsPadding(), tonalElevation = 6.dp, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$itemCount item", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(formatCurrency(total), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onOpen, modifier = Modifier.height(48.dp)) {
                Text("Checkout")
            }
        }
    }
}

@Composable
private fun CartSheet(state: UiState, viewModel: PosViewModel, onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Checkout", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        if (state.cart.isEmpty()) {
            item { Text("Keranjang masih kosong.") }
        } else {
            items(state.cart, key = { item -> "cart-${item.key}" }) { item ->
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.productName, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (item.optionName != null) Text("${item.variationName}: ${item.optionName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatCurrency(item.price)} x ${item.quantity} = ${formatCurrency(item.subtotal)}")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.decrease(item.key) }) { Icon(Icons.Default.Remove, "Kurangi") }
                            Text(item.quantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.increase(item.key) }) { Icon(Icons.Default.Add, "Tambah") }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.remove(item.key) }) { Icon(Icons.Default.Delete, "Hapus") }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = formatThousandsInput(state.discountText),
                    onValueChange = viewModel::discount,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Diskon nominal") },
                    prefix = { Text("Rp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.transactionNote,
                    onValueChange = viewModel::transactionNote,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan transaksi") },
                    maxLines = 2
                )
            }
            item { TotalRows(state) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { viewModel.selectPaymentMethod(method) },
                            label = { Text(method.label) }
                        )
                    }
                }
            }
            if (state.paymentMethod == PaymentMethod.TUNAI) {
                item {
                    OutlinedTextField(
                        value = formatThousandsInput(state.paymentText),
                        onValueChange = viewModel::payment,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Uang diterima") },
                        prefix = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            } else {
                item {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Pastikan pembayaran QRIS sudah diterima sebelum menyimpan transaksi.", fontWeight = FontWeight.SemiBold)
                            Text("Dibayar: ${formatCurrency(state.total)}")
                        }
                    }
                }
            }
            if (state.paymentMethod == PaymentMethod.TUNAI && state.cart.isNotEmpty() && state.paymentAmount < state.total) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Uang diterima belum cukup", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        Text("Kurang: ${formatCurrency(state.shortageAmount)}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.checkout(printAfterSave = true); onClose() },
                    enabled = state.isPaymentEnough,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan + Print")
                }
            }
            item {
                Button(
                    onClick = { viewModel.checkout(printAfterSave = false); onClose() },
                    enabled = state.isPaymentEnough,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Simpan Transaksi")
                }
            }
        }
    }
}

@Composable
private fun TotalRows(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) { Text("Subtotal", Modifier.weight(1f)); Text(formatCurrency(state.subtotal), fontWeight = FontWeight.Bold) }
        if (state.discountAmount > 0) {
            Row(Modifier.fillMaxWidth()) { Text("Diskon", Modifier.weight(1f)); Text("-${formatCurrency(state.discountAmount)}", fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth()) { Text("Total", Modifier.weight(1f)); Text(formatCurrency(state.total), fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth()) { Text("Metode", Modifier.weight(1f)); Text(state.paymentMethod.label, fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth()) { Text("Dibayar", Modifier.weight(1f)); Text(formatCurrency(state.paymentAmount), fontWeight = FontWeight.Bold) }
        if (state.paymentMethod == PaymentMethod.TUNAI && state.paymentAmount < state.total) {
            Row(Modifier.fillMaxWidth()) {
                Text("Kurang", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                Text(formatCurrency(state.shortageAmount), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(Modifier.fillMaxWidth()) { Text("Kembalian", Modifier.weight(1f)); Text(formatCurrency(state.changeAmount), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProductAdminScreen(state: UiState, viewModel: PosViewModel) {
    var editingProduct by remember { mutableStateOf<ProductWithDetails?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var editingCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var adminTab by remember { mutableStateOf(AdminTab.Products) }
    var productToDelete by remember { mutableStateOf<ProductWithDetails?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTab.entries.forEach {
                    FilterChip(selected = adminTab == it, onClick = { adminTab = it }, label = { Text(it.label) })
                }
            }
        }
        if (adminTab == AdminTab.Products) {
            item {
                Button(onClick = { editingProduct = null; showProductForm = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Produk")
                }
            }
            items(state.products, key = { product -> "admin-product-${product.product.id}" }) { product ->
                Card(shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(product.product.name, fontWeight = FontWeight.Bold)
                            Text("${product.category?.name ?: "Tanpa kategori"} - ${if (product.product.isAvailable) "Tersedia" else "Habis"}", fontSize = 13.sp)
                            Text(
                                if (product.product.hasVariations) "Variasi: ${product.variations.flatMap { it.options }.joinToString { opt -> opt.optionName }}" else formatCurrency(product.product.basePrice),
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = { viewModel.setProductAvailability(product.product.id, !product.product.isAvailable) }) {
                            Text(if (product.product.isAvailable) "Habis" else "Tersedia")
                        }
                        IconButton(onClick = { editingProduct = product; showProductForm = true }) { Icon(Icons.Default.Edit, "Edit produk") }
                        IconButton(onClick = { productToDelete = product }) { Icon(Icons.Default.Delete, "Hapus produk") }
                    }
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (editingCategoryId == null) "Tambah Kategori" else "Edit Kategori", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(categoryName, { categoryName = it }, Modifier.weight(1f), label = { Text("Nama kategori") }, singleLine = true)
                            IconButton(onClick = {
                                viewModel.saveCategory(editingCategoryId, categoryName)
                                editingCategoryId = null
                                categoryName = ""
                            }) { Icon(Icons.Default.Add, "Simpan kategori") }
                        }
                    }
                }
            }
            items(state.categories, key = { category -> "admin-category-${category.id}" }) { category ->
                Card(shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { categoryName = category.name; editingCategoryId = category.id }) { Icon(Icons.Default.Edit, "Edit kategori") }
                        IconButton(onClick = { viewModel.deleteCategory(category.id) }) { Icon(Icons.Default.Delete, "Hapus kategori") }
                    }
                }
            }
        }
    }

    if (showProductForm) {
        ProductFormDialog(
            product = editingProduct,
            categories = state.categories,
            onDismiss = { showProductForm = false },
            onSave = { id, name, categoryId, price, isAvailable, variationName, options ->
                viewModel.saveProduct(id, name, categoryId, price, isAvailable, variationName, options)
                showProductForm = false
            }
        )
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Produk ini akan dihapus dari daftar kasir.")
                    Text(product.product.name, fontWeight = FontWeight.Bold)
                    Text("Tindakan ini tidak menghapus riwayat transaksi yang sudah tersimpan.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.product.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductFormDialog(
    product: ProductWithDetails?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long?, Long, Boolean, String?, List<Pair<String, Long>>) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.product?.name.orEmpty()) }
    var categoryId by remember(product) { mutableStateOf(product?.product?.categoryId) }
    var basePrice by remember(product) { mutableStateOf(product?.product?.basePrice?.takeIf { it > 0 }?.toString().orEmpty()) }
    var isAvailable by remember(product) { mutableStateOf(product?.product?.isAvailable ?: true) }
    var useVariation by remember(product) { mutableStateOf(product?.product?.hasVariations ?: false) }
    var variationName by remember(product) { mutableStateOf(product?.variations?.firstOrNull()?.variation?.name ?: "Variant") }
    val options = remember(product) {
        mutableStateListOf<Pair<String, String>>().apply {
            val existing = product?.variations?.flatMap { it.options }.orEmpty()
            if (existing.isEmpty()) add("" to "") else existing.forEach { add(it.optionName to it.optionPrice.toString()) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Tambah Produk" else "Edit Produk") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { categoryId = null }, label = { Text("Tanpa kategori") })
                    categories.forEach { category ->
                        FilterChip(selected = categoryId == category.id, onClick = { categoryId = category.id }, label = { Text(category.name) })
                    }
                }
                FilterChip(selected = isAvailable, onClick = { isAvailable = !isAvailable }, label = { Text(if (isAvailable) "Tersedia" else "Habis") })
                FilterChip(selected = useVariation, onClick = { useVariation = !useVariation }, label = { Text("Produk punya variasi") })
                if (useVariation) {
                    OutlinedTextField(variationName, { variationName = it }, Modifier.fillMaxWidth(), label = { Text("Nama variasi") }, singleLine = true)
                    options.forEachIndexed { index, pair ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Opsi ${index + 1}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { if (options.size > 1) options.removeAt(index) else options[index] = "" to "" }) {
                                    Icon(Icons.Default.Delete, "Hapus opsi variasi")
                                }
                            }
                            OutlinedTextField(pair.first, { options[index] = it to pair.second }, Modifier.fillMaxWidth(), label = { Text("Nama opsi") }, singleLine = true)
                            OutlinedTextField(pair.second, { options[index] = pair.first to it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Harga opsi") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                    }
                    OutlinedButton(onClick = { options.add("" to "") }, modifier = Modifier.fillMaxWidth()) { Text("Tambah Variation Option") }
                } else {
                    OutlinedTextField(basePrice, { basePrice = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Harga dasar") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cleanOptions = if (useVariation) options.map { it.first to parseCurrency(it.second) }.filter { it.first.isNotBlank() && it.second > 0 } else emptyList()
                onSave(product?.product?.id, name, categoryId, parseCurrency(basePrice), isAvailable, variationName, cleanOptions)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryScreen(state: UiState, viewModel: PosViewModel) {
    var detail by remember { mutableStateOf<TransactionWithItems?>(null) }
    var action by remember { mutableStateOf<Pair<String, TransactionWithItems>?>(null) }
    var showClosing by remember { mutableStateOf(false) }
    var savedClosingDetail by remember { mutableStateOf<ClosingReportEntity?>(null) }
    val context = LocalContext.current
    val report by remember(state.reportDate, state.transactions) {
        derivedStateOf { buildDailyReport(state.reportDate, state.transactions) }
    }
    val closingTxtExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { viewModel.exportClosingText(it, report) }
    }
    val closingJsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportClosingJson(it, report) }
    }
    val savedClosingExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val saved = savedClosingDetail
        if (uri != null && saved != null) viewModel.exportSavedClosingText(uri, saved)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (state.reportDate == LocalDate.now()) "Laporan Hari Ini" else "Laporan ${formatDate(report.dateMillis)}", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = state.reportDate == LocalDate.now(), onClick = { viewModel.selectReportDate(LocalDate.now()) }, label = { Text("Hari ini") })
                        FilterChip(selected = state.reportDate == LocalDate.now().minusDays(1), onClick = { viewModel.selectReportDate(LocalDate.now().minusDays(1)) }, label = { Text("Kemarin") })
                        FilterChip(
                            selected = state.reportDate != LocalDate.now() && state.reportDate != LocalDate.now().minusDays(1),
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        viewModel.selectReportDate(LocalDate.of(year, month + 1, day))
                                    },
                                    state.reportDate.year,
                                    state.reportDate.monthValue - 1,
                                    state.reportDate.dayOfMonth
                                ).show()
                            },
                            label = { Text("Pilih Tanggal") }
                        )
                    }
                    Text("Tanggal laporan: ${formatDate(report.dateMillis)}", fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Total Penjualan", fontSize = 13.sp)
                            Text(formatCurrency(report.grossSales), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Penjualan Bersih", fontSize = 13.sp)
                            Text(formatCurrency(report.netSales), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (report.discountTotal > 0) {
                        Text("Diskon: ${formatCurrency(report.discountTotal)}")
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Tunai (${report.tunaiCount})", fontSize = 13.sp)
                            Text(formatCurrency(report.tunaiTotal), fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("QRIS (${report.qrisCount})", fontSize = 13.sp)
                            Text(formatCurrency(report.qrisTotal), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Transaksi: ${report.successCount} sukses, ${report.voidCount} void, ${report.refundCount} refund")
                    Text("Void: ${formatCurrency(report.voidTotal)} | Refund: ${formatCurrency(report.refundTotal)}")
                    Text("Item terjual: ${report.itemSoldCount}")
                    if (report.bestSellers.isNotEmpty()) {
                        Text("Produk Terlaris: ${report.bestSellers.first().productName} (${report.bestSellers.first().quantitySold})")
                    }
                    Button(onClick = { showClosing = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(if (state.reportDate == LocalDate.now()) "Closing Hari Ini" else "Cetak Laporan")
                    }
                    OutlinedButton(
                        onClick = {
                            closingTxtExportLauncher.launch("closing-moncake94-${formatDate(report.dateMillis)}.txt")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Backup Closing TXT")
                    }
                    OutlinedButton(
                        onClick = {
                            closingJsonExportLauncher.launch("closing-moncake94-${formatDate(report.dateMillis)}.json")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Backup Closing JSON")
                    }
                }
            }
        }
        if (state.closingReports.isNotEmpty()) {
            item {
                Text("Riwayat Closing", fontWeight = FontWeight.Bold)
            }
            items(state.closingReports, key = { closing -> "closing-${closing.id}" }) { closing ->
                Card(onClick = { savedClosingDetail = closing }, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(formatDate(closing.reportDate), fontWeight = FontWeight.Bold)
                        Text("Dicetak: ${formatDateTime(closing.printedAt)}", fontSize = 13.sp)
                        Text("Penjualan Bersih: ${formatCurrency(closing.netSales)}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (report.transactions.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = MaterialTheme.colorScheme.outline)
                        Text("Belum ada transaksi pada tanggal ini.", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(report.transactions, key = { trx -> "transaction-${trx.transaction.id}" }) { trx ->
            Card(onClick = { detail = trx }, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(trx.transaction.transactionNumber, fontWeight = FontWeight.Bold)
                        Text(formatDateTime(trx.transaction.createdAt))
                        Text("${trx.transaction.paymentMethod.toPaymentLabel()} - ${trx.transaction.status.toStatusLabel()} - ${trx.items.sumOf { it.quantity }} item")
                    }
                    Text(formatCurrency(trx.transaction.total), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    detail?.let { trx ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text("Detail Transaksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trx.transaction.transactionNumber, fontWeight = FontWeight.Bold)
                    Text(formatDateTime(trx.transaction.createdAt))
                    Text("Metode Bayar: ${trx.transaction.paymentMethod.toPaymentLabel()}")
                    Text("Status: ${trx.transaction.status.toStatusLabel()}", fontWeight = FontWeight.Bold)
                    trx.items.forEach {
                        Text("${it.productName}${it.selectedVariationOption?.let { opt -> " - $opt" } ?: ""}\n${it.quantity}x ${formatCurrency(it.price)} = ${formatCurrency(it.subtotal)}")
                    }
                    Text("Subtotal: ${formatCurrency(trx.transaction.subtotal.takeIf { it > 0 } ?: trx.transaction.total)}")
                    if (trx.transaction.discountAmount > 0) Text("Diskon: ${formatCurrency(trx.transaction.discountAmount)}")
                    Text("Total: ${formatCurrency(trx.transaction.total)}", fontWeight = FontWeight.Bold)
                    Text("Dibayar: ${formatCurrency(trx.transaction.paymentAmount)}")
                    Text("Kembalian: ${formatCurrency(trx.transaction.changeAmount)}")
                    trx.transaction.note?.takeIf { it.isNotBlank() }?.let { Text("Catatan: $it") }
                    if (trx.transaction.status == TransactionStatus.VOID.name) {
                        Text("Alasan void: ${trx.transaction.voidReason ?: "-"}")
                        trx.transaction.voidedAt?.let { Text("Waktu void: ${formatDateTime(it)}") }
                    }
                    if (trx.transaction.status == TransactionStatus.REFUNDED.name) {
                        Text("Nominal refund: ${formatCurrency(trx.transaction.refundAmount)}")
                        Text("Alasan refund: ${trx.transaction.refundReason ?: "-"}")
                        trx.transaction.refundedAt?.let { Text("Waktu refund: ${formatDateTime(it)}") }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.printTransaction(trx) }) { Text("Reprint") } },
            dismissButton = {
                Row {
                    if (trx.transaction.status == TransactionStatus.SUCCESS.name) {
                        TextButton(onClick = { action = "VOID" to trx }) { Text("Void", color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { action = "REFUND" to trx }) { Text("Refund", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { detail = null }) { Text("Tutup") }
                }
            }
        )
    }
    action?.let { (type, trx) ->
        TransactionActionDialog(
            type = type,
            transaction = trx,
            onDismiss = { action = null },
            onConfirm = { reason ->
                if (type == "VOID") viewModel.voidTransaction(trx, reason) else viewModel.refundTransaction(trx, reason)
                action = null
                detail = null
            }
        )
    }
    if (showClosing) {
        ClosingDialog(
            report = report,
            onDismiss = { showClosing = false },
            onSave = {
                viewModel.saveClosingReport(report)
                showClosing = false
            },
            onPrint = {
                viewModel.printClosingReport(report)
                showClosing = false
            }
        )
    }
    savedClosingDetail?.let { closing ->
        SavedClosingDialog(
            report = closing,
            onDismiss = { savedClosingDetail = null },
            onPrint = { viewModel.printSavedClosingReport(closing) },
            onExport = { savedClosingExportLauncher.launch("closing-moncake94-${formatDate(closing.reportDate)}.txt") }
        )
    }
}

@Composable
private fun ClosingDialog(
    report: com.moncake94.pos.data.DailyReport,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview Closing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("moncake94", fontWeight = FontWeight.Bold)
                Text("Tanggal: ${formatDate(report.dateMillis)}")
                Text("Total Penjualan: ${formatCurrency(report.grossSales)}")
                if (report.discountTotal > 0) Text("Diskon: ${formatCurrency(report.discountTotal)}")
                Text("Penjualan Bersih: ${formatCurrency(report.netSales)}")
                Text("Tunai (${report.tunaiCount}): ${formatCurrency(report.tunaiTotal)}")
                Text("QRIS (${report.qrisCount}): ${formatCurrency(report.qrisTotal)}")
                Text("Transaksi Sukses: ${report.successCount}")
                Text("Void: ${report.voidCount}")
                Text("Refund: ${report.refundCount}")
                Text("Item Terjual: ${report.itemSoldCount}")
                if (report.bestSellers.isNotEmpty()) {
                    Text("Produk Terlaris:", fontWeight = FontWeight.SemiBold)
                    report.bestSellers.take(3).forEach {
                        Text("${it.productName} (${it.quantitySold})")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onPrint) { Text("Cetak Closing") } },
        dismissButton = {
            Row {
                TextButton(onClick = onSave) { Text("Simpan") }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

@Composable
private fun SavedClosingDialog(
    report: ClosingReportEntity,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Riwayat Closing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tanggal: ${formatDate(report.reportDate)}")
                Text("Dicetak: ${formatDateTime(report.printedAt)}")
                Text("Total Penjualan: ${formatCurrency(report.grossSales)}")
                if (report.discountTotal > 0) Text("Diskon: ${formatCurrency(report.discountTotal)}")
                Text("Penjualan Bersih: ${formatCurrency(report.netSales)}")
                Text("Tunai (${report.tunaiCount}): ${formatCurrency(report.tunaiTotal)}")
                Text("QRIS (${report.qrisCount}): ${formatCurrency(report.qrisTotal)}")
                Text("Transaksi Sukses: ${report.successCount}")
                Text("Void: ${report.voidCount}")
                Text("Refund: ${report.refundCount}")
                Text("Item Terjual: ${report.itemSoldCount}")
                if (report.bestSellersText.isNotBlank()) {
                    Text("Produk Terlaris:", fontWeight = FontWeight.SemiBold)
                    Text(report.bestSellersText)
                }
            }
        },
        confirmButton = { Button(onClick = onPrint) { Text("Cetak Ulang") } },
        dismissButton = {
            Row {
                TextButton(onClick = onExport) { Text("Export TXT") }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

@Composable
private fun TransactionActionDialog(
    type: String,
    transaction: TransactionWithItems,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val isVoid = type == "VOID"
    val requiredConfirmation = if (isVoid) "VOID" else "REFUND"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isVoid) "Void Transaksi" else "Refund Transaksi") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (isVoid) "Yakin ingin void transaksi ini?" else "Yakin ingin refund transaksi ini?")
                Text(transaction.transaction.transactionNumber, fontWeight = FontWeight.Bold)
                if (!isVoid) Text("Nominal refund: ${formatCurrency(transaction.transaction.total)}")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isVoid) "Alasan void" else "Alasan refund") }
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ketik $requiredConfirmation untuk lanjut") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = confirmation == requiredConfirmation,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isVoid) "Void Transaksi" else "Refund")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun String.toPaymentLabel(): String = runCatching { PaymentMethod.valueOf(this).label }.getOrDefault(this)

private fun String.toStatusLabel(): String = runCatching { TransactionStatus.valueOf(this).label }.getOrDefault(this)
