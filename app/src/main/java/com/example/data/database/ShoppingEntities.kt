package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_suggestions")
data class ProductSuggestion(
    @PrimaryKey val name: String,
    val lastUsed: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_lists")
data class SavedList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0
)

@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long, // 0 for active/current items, historical list id for saved lists
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val isPurchased: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalPrice: Double get() = quantity * unitPrice
}
