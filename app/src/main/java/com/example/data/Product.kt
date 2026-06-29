package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryName: String,
    val brandName: String = "",
    val price: Double = 0.0,
    val warrantyNotice: String = ""
)
