package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val invoiceDao: InvoiceDao) {
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    fun getInvoiceById(id: Int): Flow<Invoice?> {
        return invoiceDao.getInvoiceById(id)
    }

    suspend fun insertInvoice(invoice: Invoice): Long {
        return invoiceDao.insertInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice)
    }

    suspend fun deleteInvoiceById(id: Int) {
        invoiceDao.deleteInvoiceById(id)
    }
}
