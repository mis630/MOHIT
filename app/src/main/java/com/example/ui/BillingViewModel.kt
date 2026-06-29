package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = InvoiceRepository(db.invoiceDao())
    private val catalogRepository = CatalogRepository(db.catalogDao())
    val securityManager = SecurityManager(application)

    // --- State Streams ---
    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = catalogRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allProducts: StateFlow<List<Product>> = catalogRepository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            try {
                // Wait for a brief moment or collect the first emission
                val categories = catalogRepository.allCategories.first()
                if (categories.isEmpty()) {
                    val defaultCats = listOf("Shoes", "Safety Boots", "Soles", "Hardware")
                    defaultCats.forEach { name ->
                        catalogRepository.insertCategory(Category(name = name))
                    }
                    catalogRepository.insertProduct(Product(name = "Bata Premium Shoe", categoryName = "Shoes", brandName = "Bata", price = 1200.0, warrantyNotice = "3 Months Warranty."))
                    catalogRepository.insertProduct(Product(name = "Acme Heavy Boot", categoryName = "Safety Boots", brandName = "Acme", price = 1500.0, warrantyNotice = "6 Months Warranty."))
                    catalogRepository.insertProduct(Product(name = "PVC Sole Regular", categoryName = "Soles", brandName = "local", price = 150.0, warrantyNotice = "No Warranty."))
                    catalogRepository.insertProduct(Product(name = "Leather Gloves High Quality", categoryName = "Hardware", brandName = "local", price = 250.0, warrantyNotice = "No Warranty."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _isAppUnlocked = MutableStateFlow(!securityManager.isLockEnabled())
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // --- Active Invoice Creation / Editing Form State ---
    private val _editingInvoice = MutableStateFlow<Invoice?>(null)
    val editingInvoice = _editingInvoice.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName = _customerName.asStateFlow()

    private val _customerMobile = MutableStateFlow("")
    val customerMobile = _customerMobile.asStateFlow()

    private val _customerAddress = MutableStateFlow("")
    val customerAddress = _customerAddress.asStateFlow()

    private val _invoiceNumber = MutableStateFlow("")
    val invoiceNumber = _invoiceNumber.asStateFlow()

    private val _invoiceDate = MutableStateFlow("")
    val invoiceDate = _invoiceDate.asStateFlow()

    private val _itemsList = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val itemsList = _itemsList.asStateFlow()

    private val _note = MutableStateFlow("Thank you for shopping with us! Safely securing your workplace.")
    val note = _note.asStateFlow()

    // --- AI Speech Parsing State ---
    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _parsingError = MutableStateFlow<String?>(null)
    val parsingError = _parsingError.asStateFlow()

    // --- Pincode Lock Methods ---
    fun submitPin(pin: String): Boolean {
        return if (securityManager.verifyPin(pin)) {
            _isAppUnlocked.value = true
            _pinError.value = null
            true
        } else {
            _pinError.value = "Incorrect PIN code. Please try again."
            false
        }
    }

    fun relockApp() {
        if (securityManager.isLockEnabled()) {
            _isAppUnlocked.value = false
        }
    }

    fun changePin(newPin: String) {
        if (newPin.length == 4) {
            securityManager.setPin(newPin)
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        securityManager.setLockEnabled(enabled)
        if (!enabled) {
            _isAppUnlocked.value = true
        }
    }

    // --- Form Management Methods ---
    fun startNewInvoice() {
        _editingInvoice.value = null
        _customerName.value = ""
        _customerMobile.value = ""
        _customerAddress.value = ""
        
        // Auto-generate invoice number (SRT-1000 + size + 1)
        val nextNum = 1000 + allInvoices.value.size + 1
        _invoiceNumber.value = nextNum.toString()
        
        // Set date to current
        val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        _invoiceDate.value = sdfDate.format(Date())

        _itemsList.value = listOf(InvoiceItem("", "local", 1, 0.0, 0.0, ""))
        _note.value = "Thank you for shopping with us! Safely securing your workplace."
        _parsingError.value = null
    }

    fun loadInvoiceForEditing(invoice: Invoice) {
        _editingInvoice.value = invoice
        _customerName.value = invoice.customerName
        _customerMobile.value = invoice.customerMobile
        _customerAddress.value = invoice.customerAddress
        _invoiceNumber.value = invoice.invoiceNumber
        _invoiceDate.value = invoice.dateString
        _itemsList.value = invoice.items
        _note.value = invoice.note
        _parsingError.value = null
    }

    fun setCustomerName(name: String) { _customerName.value = name }
    fun setCustomerMobile(mobile: String) { _customerMobile.value = mobile }
    fun setCustomerAddress(address: String) { _customerAddress.value = address }
    fun setInvoiceNumber(num: String) { _invoiceNumber.value = num }
    fun setInvoiceDate(date: String) { _invoiceDate.value = date }
    fun setNote(newNote: String) { _note.value = newNote }

    fun addInvoiceItem() {
        val current = _itemsList.value.toMutableList()
        current.add(InvoiceItem("", "local", 1, 0.0, 0.0, ""))
        _itemsList.value = current
    }

    fun removeInvoiceItem(index: Int) {
        val current = _itemsList.value.toMutableList()
        if (current.size > index) {
            current.removeAt(index)
            _itemsList.value = current
        }
    }

    fun updateInvoiceItem(index: Int, updated: InvoiceItem) {
        val current = _itemsList.value.toMutableList()
        if (current.size > index) {
            current[index] = updated
            _itemsList.value = current
        }
    }

    // --- Helper to calculate total price & apply warranty rules ---
    fun calculateItemTotal(item: InvoiceItem): InvoiceItem {
        val rawTotal = item.quantity * item.rate
        val warranty = evaluateWarrantyForBrand(item.brandName, item.itemName)
        return item.copy(totalPrice = rawTotal, warrantyNotice = warranty)
    }

    private fun evaluateWarrantyForBrand(brand: String, itemName: String): String {
        val normBrand = brand.lowercase().trim()
        val normItem = itemName.lowercase().trim()
        
        return if (normBrand in listOf("acme", "bata", "image", "hillson") && normItem.contains("shoe")) {
            "3 Months Warranty against manufacturing defects."
        } else {
            "No Warranty."
        }
    }

    // Calculate Grand Total of active items
    fun getGrandTotal(): Double {
        return _itemsList.value.sumOf { it.totalPrice }
    }

    // Consolidate Warranty Notice Text
    fun getCalculatedWarrantyText(): String {
        val brands = _itemsList.value.map { it.brandName.lowercase().trim() }.distinct()
        val hasBrandedShoes = brands.any { it in listOf("acme", "bata", "image", "hillson") }
        val hasLocalOrOthers = brands.any { it == "local" || it.isBlank() } || _itemsList.value.any { !it.itemName.lowercase().contains("shoe") }

        val sb = StringBuilder()
        if (hasBrandedShoes) {
            sb.append("✓ 3-Month Warranty covers Acme, Bata, Image, Hillson safety shoes.\n")
        }
        if (hasLocalOrOthers) {
            sb.append("⚠️ Local shoes & non-shoe items (goggles, gloves, socks) carry NO Warranty.")
        }
        return sb.toString().trim()
    }

    // --- Speech Voice AI parsing ---
    fun parseVoiceTranscriptText(transcriptText: String) {
        if (transcriptText.isBlank()) return
        
        viewModelScope.launch {
            _isParsing.value = true
            _parsingError.value = null
            
            try {
                val response = GeminiClient.parseVoiceTranscript(transcriptText)
                if (response != null) {
                    if (!response.customerName.isNullOrBlank()) _customerName.value = response.customerName
                    if (!response.customerAddress.isNullOrBlank()) _customerAddress.value = response.customerAddress
                    if (!response.customerMobile.isNullOrBlank()) _customerMobile.value = response.customerMobile
                    
                    if (!response.items.isNullOrEmpty()) {
                        val parsedItems = response.items.map { parsed ->
                            val brandName = if (parsed.brandName.isBlank()) "local" else parsed.brandName
                            val rawItem = InvoiceItem(
                                itemName = parsed.itemName,
                                brandName = brandName,
                                quantity = if (parsed.quantity <= 0) 1 else parsed.quantity,
                                rate = parsed.rate,
                                totalPrice = 0.0
                            )
                            calculateItemTotal(rawItem)
                        }
                        _itemsList.value = parsedItems
                    }
                } else {
                    _parsingError.value = "Failed to parse spoken text. Make sure Gemini API Key is configured."
                }
            } catch (e: Exception) {
                _parsingError.value = "Error parsing spoken text: ${e.message}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    // --- Save invoice transaction ---
    fun saveActiveInvoice(onSuccess: (Invoice) -> Unit, onFailure: (String) -> Unit) {
        val cName = _customerName.value
        val items = _itemsList.value
        val mobile = _customerMobile.value.trim()
        
        if (cName.isBlank()) {
            onFailure("Customer Name cannot be empty.")
            return
        }
        if (items.isEmpty() || items.all { it.itemName.isBlank() }) {
            onFailure("Invoice must contain at least one valid item.")
            return
        }
        if (mobile.isNotBlank()) {
            val digitsOnly = mobile.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length != 10) {
                onFailure("Please enter a valid 10-digit mobile number.")
                return
            }
        }

        viewModelScope.launch {
            try {
                val cleanItems = items.filter { it.itemName.isNotBlank() }.map { calculateItemTotal(it) }
                val currentEdit = _editingInvoice.value
                val newInvoice = Invoice(
                    id = currentEdit?.id ?: 0,
                    invoiceNumber = _invoiceNumber.value,
                    customerName = cName.trim(),
                    customerMobile = mobile,
                    customerAddress = _customerAddress.value.trim(),
                    dateString = _invoiceDate.value,
                    items = cleanItems,
                    grandTotal = cleanItems.sumOf { it.totalPrice },
                    warrantyText = getCalculatedWarrantyText(),
                    note = _note.value,
                    createdAt = currentEdit?.createdAt ?: System.currentTimeMillis(),
                    isPaid = currentEdit?.isPaid ?: false,
                    paymentStatus = currentEdit?.paymentStatus ?: "Pending",
                    paymentMethod = currentEdit?.paymentMethod ?: "None",
                    upiIdUsed = currentEdit?.upiIdUsed ?: "shreeramtrader@okaxis"
                )
                val idLong = repository.insertInvoice(newInvoice)
                onSuccess(newInvoice.copy(id = idLong.toInt()))
            } catch (e: Exception) {
                onFailure("Database failed: ${e.message}")
            }
        }
    }

    fun updateInvoicePaymentStatus(invoice: Invoice, isPaid: Boolean, method: String, upiId: String = "shreeramtrader@okaxis") {
        viewModelScope.launch {
            try {
                val updated = invoice.copy(
                    isPaid = isPaid,
                    paymentStatus = if (isPaid) "Paid via $method" else "Pending",
                    paymentMethod = method,
                    upiIdUsed = upiId
                )
                repository.insertInvoice(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }

    // --- Category Management ---
    fun addCategory(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                catalogRepository.insertCategory(Category(name = name.trim()))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            catalogRepository.deleteCategory(category)
        }
    }

    // --- Product Management ---
    fun addProduct(name: String, categoryName: String, brandName: String, price: Double, warrantyNotice: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && categoryName.isNotBlank()) {
                catalogRepository.insertProduct(
                    Product(
                        name = name.trim(),
                        categoryName = categoryName,
                        brandName = brandName.trim(),
                        price = price,
                        warrantyNotice = warrantyNotice.trim()
                    )
                )
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            catalogRepository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            catalogRepository.deleteProduct(product)
        }
    }
}
