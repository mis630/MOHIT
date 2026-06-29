package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.Invoice
import com.example.ui.BillingViewModel
import com.example.util.InvoicePdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BillingViewModel,
    onNavigateToCreateInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val invoices by viewModel.allInvoices.collectAsState()
    
    // Bottom Navigation Tab state
    var activeTab by remember { mutableStateOf(0) } // 0 = Invoices, 1 = Catalog Admin
    
    // Category management states
    val categories by viewModel.allCategories.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    // Product management states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var selectedCategoryForNewProduct by remember { mutableStateOf("") }
    var newProductBrand by remember { mutableStateOf("local") }
    var newProductPrice by remember { mutableStateOf("") }
    var newProductWarranty by remember { mutableStateOf("No Warranty.") }
    
    // Quick price edit state
    var showEditPriceDialog by remember { mutableStateOf<com.example.data.Product?>(null) }
    var updatedPriceStr by remember { mutableStateOf("") }
    
    var selectedAdminCategoryFilter by remember { mutableStateOf<String?>("All") }

    var searchQuery by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }
    var showPaymentCollectDialog by remember { mutableStateOf(false) }
    
    // Filtering invoices based on input search path
    val filteredInvoices = remember(searchQuery, invoices) {
        if (searchQuery.isBlank()) {
            invoices
        } else {
            invoices.filter {
                it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                it.customerMobile.contains(searchQuery)
            }
        }
    }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Shree Ram Trader",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dindli Market, Jamshedpur - 831013",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SRT",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.relockApp() }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock App")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Security Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.startNewInvoice()
                        onNavigateToCreateInvoice()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Bill (A5)") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    SmallFloatingActionButton(
                        onClick = { showAddCategoryDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Cat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (categories.isNotEmpty()) {
                                selectedCategoryForNewProduct = categories.first().name
                            } else {
                                selectedCategoryForNewProduct = ""
                            }
                            newProductName = ""
                            newProductBrand = "local"
                            newProductPrice = ""
                            newProductWarranty = "No Warranty."
                            showAddProductDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New Product") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Invoices") },
                    label = { Text("Invoices") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Catalog Admin") },
                    label = { Text("Catalog Admin") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (activeTab == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Input Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Customer Name, Mobile, or Bill No...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats summary card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Invoiced Sales", 
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            val totalSales = invoices.sumOf { it.grandTotal }
                            Text(
                                text = String.format("₹ %,.2f", totalSales), 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Bills", 
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${invoices.size} Invoices", 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "BILLING HISTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching invoices found" else "No invoices created yet",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Click 'New Bill (A5)' to create your first digital PDF invoice.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp).padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredInvoices) { invoice ->
                            InvoiceCard(
                                invoice = invoice,
                                onClick = { selectedInvoiceForDetail = invoice }
                            )
                        }
                    }
                }
            } else {
                // Catalog Admin Screen!
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "E-COMMERCE CATALOG DATABASE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Add and manage categories, custom items, default rates/prices, and warranty details.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Horizontal scroll of categories for filtering products
                Text(
                    text = "CATEGORIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedAdminCategoryFilter == "All",
                                onClick = { selectedAdminCategoryFilter = "All" },
                                label = { Text("All Products") }
                            )
                        }
                        items(categories.size) { idx ->
                            val cat = categories[idx]
                            FilterChip(
                                selected = selectedAdminCategoryFilter == cat.name,
                                onClick = { selectedAdminCategoryFilter = cat.name },
                                label = { Text(cat.name) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(cat)
                                            if (selectedAdminCategoryFilter == cat.name) {
                                                selectedAdminCategoryFilter = "All"
                                            }
                                            Toast.makeText(context, "Deleted category: ${cat.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete category",
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Products List Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRODUCTS & PRICING (${selectedAdminCategoryFilter?.uppercase()})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val filteredProducts = products.filter { prod ->
                    selectedAdminCategoryFilter == "All" || prod.categoryName == selectedAdminCategoryFilter
                }
                
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No products found",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap '+' to add a product or category.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(filteredProducts) { product ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.categoryName.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ).padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                            if (product.brandName.isNotBlank() && product.brandName != "local") {
                                                Text(
                                                    text = "•  ⭐ ${product.brandName}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            if (product.warrantyNotice.isNotBlank()) {
                                                Text(
                                                    text = "•  🛡️ ${product.warrantyNotice}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format("₹ %,.2f", product.price),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Default Price",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                updatedPriceStr = product.price.toString()
                                                showEditPriceDialog = product
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Price",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteProduct(product)
                                                Toast.makeText(context, "Deleted product: ${product.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete product",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Admin Database Management Dialogs ---
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Safety Helmets, Soles") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName)
                            Toast.makeText(context, "Category added: $newCategoryName", Toast.LENGTH_SHORT).show()
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        } else {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Product Name") },
                        placeholder = { Text("e.g. Acme Safety Goggles") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Selector
                    Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    if (categories.isEmpty()) {
                        Text("Please add a category first!", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories.size) { index ->
                                val cat = categories[index]
                                FilterChip(
                                    selected = selectedCategoryForNewProduct == cat.name,
                                    onClick = { selectedCategoryForNewProduct = cat.name },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newProductBrand,
                        onValueChange = { newProductBrand = it },
                        label = { Text("Brand / Maker") },
                        placeholder = { Text("Acme, Bata, local, etc.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text("Default Rate / Price (₹)") },
                        placeholder = { Text("e.g. 250") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newProductWarranty,
                        onValueChange = { newProductWarranty = it },
                        label = { Text("Warranty Notice") },
                        placeholder = { Text("e.g. 3 Months Warranty. or No Warranty.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceNum = newProductPrice.toDoubleOrNull() ?: 0.0
                        if (newProductName.isBlank()) {
                            Toast.makeText(context, "Product Name cannot be empty", Toast.LENGTH_SHORT).show()
                        } else if (selectedCategoryForNewProduct.isBlank()) {
                            Toast.makeText(context, "Please select or add a category", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addProduct(
                                name = newProductName,
                                categoryName = selectedCategoryForNewProduct,
                                brandName = newProductBrand,
                                price = priceNum,
                                warrantyNotice = newProductWarranty
                            )
                            Toast.makeText(context, "Product added: $newProductName", Toast.LENGTH_SHORT).show()
                            newProductName = ""
                            newProductPrice = ""
                            showAddProductDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showEditPriceDialog?.let { product ->
        AlertDialog(
            onDismissRequest = { showEditPriceDialog = null },
            title = { Text("Quick Update Price", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Product: ${product.name}", fontSize = 13.sp)
                    OutlinedTextField(
                        value = updatedPriceStr,
                        onValueChange = { updatedPriceStr = it },
                        label = { Text("New Rate / Price (₹)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = updatedPriceStr.toDoubleOrNull()
                        if (newPrice != null) {
                            viewModel.updateProduct(product.copy(price = newPrice))
                            Toast.makeText(context, "Price updated successfully!", Toast.LENGTH_SHORT).show()
                            showEditPriceDialog = null
                        } else {
                            Toast.makeText(context, "Invalid price amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Update Price")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPriceDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // --- Settings / PIN management Dialog ---
    if (showSettingsDialog) {
        var isEnabled by remember { mutableStateOf(viewModel.securityManager.isLockEnabled()) }
        var inputNewPin by remember { mutableStateOf("") }
        var showSavePinAlert by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Security Lock Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Customize access lock settings. Since this is for your personal use, protect this device with a standard 4-digit pincode.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Enable PIN Lock on Startup", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                viewModel.toggleAppLock(checked)
                            }
                        )
                    }
                    
                    if (isEnabled) {
                        Divider()
                        Text(text = "Change Passcode PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = inputNewPin,
                            onValueChange = { if (it.length <= 4) inputNewPin = it },
                            placeholder = { Text("4-digit new PIN (e.g. 1234)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = {
                                if (inputNewPin.length == 4) {
                                    viewModel.changePin(inputNewPin)
                                    showSavePinAlert = true
                                    inputNewPin = ""
                                }
                            },
                            enabled = inputNewPin.length == 4,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update PIN")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
        
        if (showSavePinAlert) {
            AlertDialog(
                onDismissRequest = { showSavePinAlert = false },
                title = { Text("Success") },
                text = { Text("Security Passcode PIN code has been updated successfully!") },
                confirmButton = {
                    TextButton(onClick = { showSavePinAlert = false }) {
                        Text("Awesome")
                    }
                }
            )
        }
    }
    
    // --- Invoice Item Detail Dialog Sheet ---
    if (selectedInvoiceForDetail != null) {
        val invoice = selectedInvoiceForDetail!!
        var showDeleteConfirm by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { selectedInvoiceForDetail = null },
            title = {
                Column {
                    Text(" SRT-${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Date: ${invoice.dateString}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider()
                    
                    // Customer info summary
                    Text(text = "Customer Name: ${invoice.customerName.uppercase()}", fontWeight = FontWeight.SemiBold)
                    if (invoice.customerMobile.isNotBlank()) {
                        Text(text = "Customer Mobile: ${invoice.customerMobile}")
                    }
                    if (invoice.customerAddress.isNotBlank()) {
                        Text(text = "Customer Address: ${invoice.customerAddress}")
                    }
                    
                    Divider()
                    
                    Text(text = "ITEMS LISTED:", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    
                    // Linear scrollable block for the items listed
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 140.dp)
                            .fillMaxWidth()
                    ) {
                        items(invoice.items) { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${item.itemName} (${item.brandName})", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text(text = "₹" + String.format("%.2f", item.totalPrice), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Text(
                                    text = "Qty: ${item.quantity} | Rate: ₹${item.rate}", 
                                    fontSize = 11.sp, 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Warranty: ${item.warrantyNotice}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Divider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                            }
                        }
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = String.format("₹ %,.2f", invoice.grandTotal), 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (invoice.isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = invoice.paymentStatus.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.loadInvoiceForEditing(invoice)
                                onNavigateToCreateInvoice()
                                selectedInvoiceForDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Bill", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                showPaymentCollectDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receive Payment", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            // Generate file
                            val pdfFile = InvoicePdfGenerator.generateA5InvoicePdf(context, invoice)
                            sharePdfFile(context, pdfFile, invoice)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share (WhatsApp)", fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = {
                            val pdfFile = InvoicePdfGenerator.generateA5InvoicePdf(context, invoice)
                            printPdfFile(context, pdfFile)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp)
                    }
                    
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
        
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Bill?") },
                text = { Text("Are you sure you want to permanently delete SRT-${invoice.invoiceNumber} invoice? This is irreversible.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteInvoice(invoice)
                            showDeleteConfirm = false
                            selectedInvoiceForDetail = null
                        }
                    ) {
                        Text("Yes, Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showPaymentCollectDialog && selectedInvoiceForDetail != null) {
        val invoice = selectedInvoiceForDetail!!
        var upiId by remember { mutableStateOf(invoice.upiIdUsed.ifBlank { "shreeramtrader@okaxis" }) }
        var selectedPaymentMethod by remember { mutableStateOf("PhonePe") }
        val paymentMethods = listOf("PhonePe", "Paytm", "GPay", "BharatPe", "Cash")
        
        // Construct the standard Indian UPI intent string
        val upiUrl = "upi://pay?pa=${upiId.trim()}&pn=Shree%20Ram%20Trader&am=${invoice.grandTotal}&cu=INR&tn=SRT-Bill-${invoice.invoiceNumber}"
        val qrCodeUrl = "https://chart.googleapis.com/chart?chs=350x350&cht=qr&chl=${java.net.URLEncoder.encode(upiUrl, "UTF-8")}"
        
        AlertDialog(
            onDismissRequest = { showPaymentCollectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Collect Bill Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scan to pay Shree Ram Trader the exact amount of ₹${String.format("%,.2f", invoice.grandTotal)}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // UPI ID Input field (Customization is crucial for real use cases!)
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("Your Merchant UPI ID") },
                        placeholder = { Text("e.g. shreeramtrader@okaxis") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Dynamic QR Code fetched from safe open-source chart API
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "Scan UPI QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Text(
                        text = "Scan with PhonePe, Paytm, Google Pay, or BharatPe",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Divider()
                    
                    Text("Or mark manually received:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    // Custom chips of payment modes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        paymentMethods.forEach { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateInvoicePaymentStatus(
                            invoice = invoice,
                            isPaid = true,
                            method = selectedPaymentMethod,
                            upiId = upiId
                        )
                        showPaymentCollectDialog = false
                        selectedInvoiceForDetail = null // close the main details screen too so changes refresh
                        Toast.makeText(context, "Payment status updated to Paid via $selectedPaymentMethod!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Payment Received")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateInvoicePaymentStatus(
                            invoice = invoice,
                            isPaid = false,
                            method = "None",
                            upiId = upiId
                        )
                        showPaymentCollectDialog = false
                        selectedInvoiceForDetail = null
                        Toast.makeText(context, "Marked as Unpaid / Pending", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Mark Unpaid", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SRT-${invoice.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }
                
                Text(
                    text = invoice.dateString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = invoice.customerName.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (invoice.isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = invoice.paymentStatus.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                if (invoice.customerMobile.isNotBlank()) {
                    Text(
                        text = "•  📞 ${invoice.customerMobile}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number of items indicator
                val totalQty = invoice.items.sumOf { it.quantity }
                Text(
                    text = "$totalQty items sold",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = String.format("₹ %,.2f", invoice.grandTotal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Helpers for opening and sharing generated PDF documents
fun sharePdfFile(context: Context, pdfFile: java.io.File, invoice: Invoice) {
    try {
        val authority = "com.aistudio.shreerambilling.wtpxq.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SRT Invoice SRT-${invoice.invoiceNumber}")
            // WhatsApp-friendly quick description body:
            putExtra(
                Intent.EXTRA_TEXT,
                "🧾 DIGITAL INVOICE - SHREE RAM TRADER\n" +
                "📍 Jamshedpur, Jharkhand\n" +
                "Bill No: SRT-${invoice.invoiceNumber}\n" +
                "Customer: ${invoice.customerName}\n" +
                "Grand Total: ₹${invoice.grandTotal}\n" +
                "Thank you for shopping with us! Safely securing your workplace."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share A5 PDF via..."))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun printPdfFile(context: Context, pdfFile: java.io.File) {
    try {
        val authority = "com.aistudio.shreerambilling.wtpxq.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val printIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(printIntent, "Print PDF..."))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
