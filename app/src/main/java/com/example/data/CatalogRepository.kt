package com.example.data

import kotlinx.coroutines.flow.Flow

class CatalogRepository(private val catalogDao: CatalogDao) {
    val allCategories: Flow<List<Category>> = catalogDao.getAllCategories()
    val allProducts: Flow<List<Product>> = catalogDao.getAllProducts()

    fun getProductsByCategory(categoryName: String): Flow<List<Product>> {
        return catalogDao.getProductsByCategory(categoryName)
    }

    suspend fun insertCategory(category: Category): Long {
        return catalogDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        catalogDao.deleteCategory(category)
        catalogDao.deleteProductsByCategory(category.name)
    }

    suspend fun insertProduct(product: Product): Long {
        return catalogDao.insertProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        catalogDao.deleteProduct(product)
    }
}
