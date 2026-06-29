package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    // Categories Queries
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    // Products Queries
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE categoryName = :categoryName ORDER BY name ASC")
    fun getProductsByCategory(categoryName: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE categoryName = :categoryName")
    suspend fun deleteProductsByCategory(categoryName: String)
}
