package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class InvoiceItem(
    val itemName: String = "",
    val brandName: String = "", // Acme, Bata, Image, Hillson, local, other
    val quantity: Int = 1,
    val rate: Double = 0.0,
    val totalPrice: Double = 0.0,
    val warrantyNotice: String = ""
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,
    val dateString: String,
    val items: List<InvoiceItem>,
    val grandTotal: Double,
    val warrantyText: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val paymentStatus: String = "Pending",
    val paymentMethod: String = "None",
    val upiIdUsed: String = "shreeramtrader@okaxis"
)

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
    private val adapter = moshi.adapter<List<InvoiceItem>>(listType)

    @TypeConverter
    fun fromString(value: String?): List<InvoiceItem>? {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<InvoiceItem>?): String {
        if (list == null) return "[]"
        return adapter.toJson(list)
    }
}
