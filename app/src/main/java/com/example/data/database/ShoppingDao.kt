package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    // Product suggestions (autocomplete query)
    @Query("SELECT * FROM product_suggestions ORDER BY lastUsed DESC")
    fun getAllSuggestions(): Flow<List<ProductSuggestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: ProductSuggestion)

    // Shopping List Items
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY timestamp ASC")
    fun getItemsForList(listId: Long): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItem): Long

    @Update
    suspend fun updateItem(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun clearListItems(listId: Long)

    // Saved Lists (Historical)
    @Query("SELECT * FROM saved_lists ORDER BY createdAt DESC")
    fun getSavedLists(): Flow<List<SavedList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedList(savedList: SavedList): Long

    @Query("DELETE FROM saved_lists WHERE id = :listId")
    suspend fun deleteSavedList(listId: Long)

    @Query("SELECT * FROM saved_lists WHERE id = :listId LIMIT 1")
    suspend fun getSavedListById(listId: Long): SavedList?
}
