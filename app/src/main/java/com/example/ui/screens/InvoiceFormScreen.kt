package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvoiceItem
import com.example.ui.BillingViewModel
import com.example.util.InvoicePdfGenerator
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Bind form variables from ViewModel flows
    val customerName by viewModel.customerName.collectAsState()
    val customerMobile by viewModel.customerMobile.collectAsState()
    val customerAddress by viewModel.customerAddress.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val invoiceDate by viewModel.invoiceDate.collectAsState()
    val itemsList by viewModel.itemsList.collectAsState()
    val note by viewModel.note.collectAsState()
    
    val allCategories by viewModel.allCategories.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    
    var showCatalogPicker by remember { mutableStateOf(false) }
    var selectedLineIndex by remember { mutableStateOf(-1) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("All") }
    var pickerSearchQuery by remember { mutableStateOf("") }
    
    // AI Parsing States
    val isParsing by viewModel.isParsing.collectAsState()
    val parsingError by viewModel.parsingError.collectAsState()
    
    // launcher for Android speech to text intent
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.parseVoiceTranscriptText(spokenText)
                Toast.makeText(context, "AI is processing: \"$spokenText\"", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val editingInvoice by viewModel.editingInvoice.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingInvoice != null) "Edit Digital Bill (A5)" else "Generate Digital Bill (A5)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Voice Mic Floating Trigger in TopBar
                    IconButton(
                        onClick = {
                            try {
                                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak customer details and products to bill...")
                                }
                                speechRecognizerLauncher.launch(speechIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Assistant Entry")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            
            // AI Processing Feedback Loader Block
            AnimatedVisibility(
                visible = isParsing || parsingError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (parsingError != null) MaterialTheme.colorScheme.errorContainer 
                                         else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isParsing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text("Billing Assistant Spark ✨", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Parsing spoken transcript using Gemini API...", fontSize = 11.sp)
                            }
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Parsing Issue", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(parsingError ?: "Connection error", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            IconButton(onClick = { viewModel.startNewInvoice() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            }
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                
                // SECTION 1: VOICE INTRO HINT BOX
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voice Invoicing Assistant", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "Speak normally: \"Rajesh Kumar Jamshedpur mobile 9800011122 purchased 1 pair Acme shoe at 1400 and 2 goggles at 150 each.\" Tap mic in top right to start!",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                
                // SECTION 2: CUSTOMER METADATA
                item {
                    Text("CUSTOMER DETAILS", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = invoiceNumber,
                                onValueChange = { viewModel.setInvoiceNumber(it) },
                                label = { Text("Bill No.") },
                                singleLine = true,
                                modifier = Modifier.weight(0.40f)
                            )
                            OutlinedTextField(
                                value = invoiceDate,
                                onValueChange = { viewModel.setInvoiceDate(it) },
                                label = { Text("Date") },
                                placeholder = { Text("DD-MM-YYYY") },
                                singleLine = true,
                                modifier = Modifier.weight(0.60f)
                            )
                        }
                        
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.setCustomerName(it) },
                            label = { Text("Customer Name *") },
                            placeholder = { Text("Enter full name") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        val isMobileInvalid = customerMobile.isNotBlank() && customerMobile.replace(Regex("[^0-9]"), "").length != 10
                        OutlinedTextField(
                            value = customerMobile,
                            onValueChange = { viewModel.setCustomerMobile(it) },
                            label = { Text("Customer Mobile (WhatsApp)") },
                            placeholder = { Text("Enter 10-digit number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            isError = isMobileInvalid,
                            supportingText = {
                                if (isMobileInvalid) {
                                    Text("Must be a 10-digit mobile number.", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { viewModel.setCustomerAddress(it) },
                            label = { Text("Customer Address & Pincode") },
                            placeholder = { Text("Dindli market, Jamshedpur") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // SECTION 3: QUICK CATALOG POPULATOR HOTKEYS
                item {
                    Text("QUICK CHIPS (DATABASE INVENTORY)", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (allProducts.isEmpty()) {
                        Text("Add products in Catalog Admin to show quick chips.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allProducts.take(6).size) { idx ->
                                val product = allProducts[idx]
                                SuggestionChip(
                                    onClick = {
                                        val newItem = InvoiceItem(
                                            itemName = product.name,
                                            brandName = product.brandName,
                                            quantity = 1,
                                            rate = product.price,
                                            totalPrice = product.price,
                                            warrantyNotice = product.warrantyNotice
                                        )
                                        val cl = itemsList.toMutableList()
                                        if (cl.size == 1 && cl[0].itemName.isBlank()) {
                                            cl[0] = newItem
                                        } else {
                                            cl.add(newItem)
                                        }
                                        cl.forEachIndexed { i, it ->
                                            viewModel.updateInvoiceItem(i, it)
                                        }
                                        viewModel.addInvoiceItem()
                                        viewModel.removeInvoiceItem(cl.size)
                                    },
                                    label = { Text(product.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
                
                // SECTION 4: TABLE OF SALES ITEMS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ITEMISED BILLING", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        
                        TextButton(
                            onClick = { viewModel.addInvoiceItem() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Line", fontSize = 12.sp)
                        }
                    }
                }
                
                itemsIndexed(itemsList) { index, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Line Item #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                if (itemsList.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removeInvoiceItem(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete line", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = item.itemName,
                                onValueChange = {
                                    val updated = viewModel.calculateItemTotal(item.copy(itemName = it))
                                    viewModel.updateInvoiceItem(index, updated)
                                },
                                label = { Text("Product Name") },
                                placeholder = { Text("e.g. Safety Shoe Size 8") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            selectedLineIndex = index
                                            showCatalogPicker = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = "Pick from Database",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Row of Brand selector, quantity input, rate input
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Simple text entry for custom brands or catalog selection
                                OutlinedTextField(
                                    value = item.brandName,
                                    onValueChange = {
                                        val updated = viewModel.calculateItemTotal(item.copy(brandName = it))
                                        viewModel.updateInvoiceItem(index, updated)
                                    },
                                    label = { Text("Brand / Maker") },
                                    placeholder = { Text("Bata, Acme, Image, local...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                
                                OutlinedTextField(
                                    value = if (item.rate == 0.0) "" else item.rate.toString(),
                                    onValueChange = {
                                        val parseRate = it.toDoubleOrNull() ?: 0.0
                                        val updated = viewModel.calculateItemTotal(item.copy(rate = parseRate))
                                        viewModel.updateInvoiceItem(index, updated)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    label = { Text("Rate (₹)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Qty incrementers (Touch target compliance >48dp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Qty: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    
                                    FilledTonalIconButton(
                                        onClick = {
                                            if (item.quantity > 1) {
                                                val updated = viewModel.calculateItemTotal(item.copy(quantity = item.quantity - 1))
                                                viewModel.updateInvoiceItem(index, updated)
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(
                                        text = item.quantity.toString(), 
                                        fontWeight = FontWeight.Black, 
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    FilledTonalIconButton(
                                        onClick = {
                                            val updated = viewModel.calculateItemTotal(item.copy(quantity = item.quantity + 1))
                                            viewModel.updateInvoiceItem(index, updated)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                // Print subtotal for line item
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Line Total:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Text(
                                        text = "₹ " + String.format("%,.2f", item.totalPrice), 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            
                            // Displays dynamic warranty status directly on the line item
                            if (item.itemName.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    val isBranded = item.brandName.lowercase().trim() in listOf("acme", "bata", "image", "hillson")
                                    Icon(
                                        imageVector = if (isBranded) Icons.Default.Verified else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isBranded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.warrantyNotice,
                                        fontSize = 10.sp,
                                        fontWeight = if (isBranded) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isBranded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // SECTION 5: WARRANTY SUMMARY ALERT CONTAINER
                item {
                    val computedWarrantyText = viewModel.getCalculatedWarrantyText()
                    if (computedWarrantyText.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CONSOLIDATED WARRANTY CLAUSES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    text = computedWarrantyText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                
                // SECTION 6: TERMS & CONDITIONS
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { viewModel.setNote(it) },
                        label = { Text("Footer Custom Message") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // --- HORIZONTAL BOTTOM STATS BAR & SAVE ACTION BUTTONS ---
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "GRAND TOTAL AMOUNT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text(
                        text = "₹ " + String.format("%,.2f", viewModel.getGrandTotal()),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Button(
                    onClick = {
                        viewModel.saveActiveInvoice(
                            onSuccess = { savedInvoice ->
                                Toast.makeText(context, "Bill saved successfully!", Toast.LENGTH_SHORT).show()
                                val pdfFile = InvoicePdfGenerator.generateA5InvoicePdf(context, savedInvoice)
                                sharePdfFile(context, pdfFile, savedInvoice)
                                onNavigateBack()
                            },
                            onFailure = { errorString ->
                                Toast.makeText(context, "Error: $errorString", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (editingInvoice != null) "Update & Export" else "Export & Share")
                }
            }
        }
    }

    if (showCatalogPicker) {
        AlertDialog(
            onDismissRequest = { showCatalogPicker = false },
            title = {
                Text(
                    text = "Pick Product from Database",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = { Text("Search by name or brand...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category chips selector
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "All",
                                onClick = { selectedCategoryFilter = "All" },
                                label = { Text("All") }
                            )
                        }
                        items(allCategories.size) { index ->
                            val cat = allCategories[index]
                            FilterChip(
                                selected = selectedCategoryFilter == cat.name,
                                onClick = { selectedCategoryFilter = cat.name },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    // Matching products list
                    val filteredProducts = allProducts.filter { prod ->
                        val matchesCategory = (selectedCategoryFilter == "All" || prod.categoryName == selectedCategoryFilter)
                        val matchesSearch = prod.name.contains(pickerSearchQuery, ignoreCase = true) || 
                                            prod.brandName.contains(pickerSearchQuery, ignoreCase = true)
                        matchesCategory && matchesSearch
                    }

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products found.\nAdd items in 'Catalog Admin' on Home Screen.",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(filteredProducts.size) { index ->
                                val product = filteredProducts[index]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    onClick = {
                                        if (selectedLineIndex >= 0 && selectedLineIndex < itemsList.size) {
                                            val currentLine = itemsList[selectedLineIndex]
                                            val updated = viewModel.calculateItemTotal(
                                                currentLine.copy(
                                                    itemName = product.name,
                                                    brandName = product.brandName,
                                                    rate = product.price,
                                                    warrantyNotice = product.warrantyNotice
                                                )
                                            )
                                            viewModel.updateInvoiceItem(selectedLineIndex, updated)
                                        }
                                        showCatalogPicker = false
                                        pickerSearchQuery = ""
                                    }
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
                                                fontSize = 14.sp
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = product.categoryName.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.background(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ).padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                                if (product.brandName.isNotBlank() && product.brandName != "local") {
                                                    Text(
                                                        text = "⭐ ${product.brandName}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "₹ ${product.price}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCatalogPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
