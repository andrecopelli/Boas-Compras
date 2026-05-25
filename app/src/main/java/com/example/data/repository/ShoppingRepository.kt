package com.example.data.repository

import com.example.data.database.ProductSuggestion
import com.example.data.database.SavedList
import com.example.data.database.ShoppingDao
import com.example.data.database.ShoppingListItem
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(private val shoppingDao: ShoppingDao) {

    val suggestions: Flow<List<ProductSuggestion>> = shoppingDao.getAllSuggestions()
    val savedLists: Flow<List<SavedList>> = shoppingDao.getSavedLists()

    fun getItemsForList(listId: Long): Flow<List<ShoppingListItem>> {
        return shoppingDao.getItemsForList(listId)
    }

    suspend fun insertItem(item: ShoppingListItem): Long {
        return shoppingDao.insertItem(item)
    }

    suspend fun updateItem(item: ShoppingListItem) {
        shoppingDao.updateItem(item)
    }

    suspend fun deleteItem(itemId: Long) {
        shoppingDao.deleteItem(itemId)
    }

    suspend fun clearListItems(listId: Long) {
        shoppingDao.clearListItems(listId)
    }

    suspend fun insertSuggestion(name: String) {
        if (name.isNotBlank()) {
            shoppingDao.insertSuggestion(ProductSuggestion(name.trim(), System.currentTimeMillis()))
        }
    }

    suspend fun deleteSavedList(listId: Long) {
        shoppingDao.deleteSavedList(listId)
        shoppingDao.clearListItems(listId) // Clean up orphaned items
    }

    suspend fun saveCurrentList(listName: String, activeItems: List<ShoppingListItem>): Long {
        val total = activeItems.sumOf { it.totalPrice }
        val listId = shoppingDao.insertSavedList(
            SavedList(
                name = listName.trim().ifEmpty { "Lista de Compras" },
                totalAmount = total,
                createdAt = System.currentTimeMillis()
            )
        )
        // Insert copies of the items with the new listId
        activeItems.forEach { item ->
            shoppingDao.insertItem(
                item.copy(id = 0, listId = listId)
            )
        }
        // Save unique names to product suggestions as well
        activeItems.forEach { item ->
            insertSuggestion(item.name)
        }
        // Clear active items
        shoppingDao.clearListItems(0)
        return listId
    }
}
