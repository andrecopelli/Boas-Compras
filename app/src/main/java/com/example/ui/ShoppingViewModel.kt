package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ShoppingListItem
import com.example.data.database.SavedList
import com.example.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ShoppingRepository(database.shoppingDao())
    }

    // Input States
    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun setDarkTheme(isDark: Boolean?) {
        _isDarkTheme.value = isDark
    }

    private val _productName = MutableStateFlow("")
    val productName = _productName.asStateFlow()

    private val _productQuantity = MutableStateFlow("1")
    val productQuantity = _productQuantity.asStateFlow()

    private val _productPrice = MutableStateFlow("")
    val productPrice = _productPrice.asStateFlow()

    // Observable states
    val activeItems: StateFlow<List<ShoppingListItem>> = repository.getItemsForList(0)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedLists: StateFlow<List<SavedList>> = repository.savedLists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Suggestion Autocomplete State
    val filteredSuggestions: StateFlow<List<String>> = combine(
        _productName,
        repository.suggestions
    ) { query, allSuggests ->
        if (query.isBlank() || query.length < 1) {
            emptyList()
        } else {
            allSuggests
                .map { it.name }
                .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                .take(5)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Total active sum
    val totalActiveAmount: StateFlow<Double> = activeItems
        .map { items -> items.sumOf { it.totalPrice } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Form inputs and parsing helpers
    fun onProductNameChange(name: String) {
        _productName.value = name
    }

    fun onProductQuantityChange(qty: String) {
        // Accept only valid numbers or empty (to let user delete)
        val sanitized = qty.replace(",", ".")
        if (sanitized.isEmpty() || sanitized.toDoubleOrNull() != null || sanitized == ".") {
            _productQuantity.value = qty
        }
    }

    fun onProductPriceChange(price: String) {
        val sanitized = price.replace(",", ".")
        if (sanitized.isEmpty() || sanitized.toDoubleOrNull() != null || sanitized == ".") {
            _productPrice.value = price
        }
    }

    // Insert Item to Active List
    fun addCurrentItem() {
        val name = _productName.value.trim()
        if (name.isEmpty()) return

        // Parse quantity (default to 1.0 if empty/invalid)
        val qtyString = _productQuantity.value.replace(",", ".")
        val quantity = qtyString.toDoubleOrNull() ?: 1.0

        // Parse price (default to 0.0 if empty/invalid)
        val priceString = _productPrice.value.replace(",", ".")
        val price = priceString.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val newItem = ShoppingListItem(
                listId = 0,
                name = name,
                quantity = quantity,
                unitPrice = price,
                isPurchased = false
            )
            repository.insertItem(newItem)
            // Save as auto-suggestion
            repository.insertSuggestion(name)

            // Reset inputs (keeping quantity = "1")
            _productName.value = ""
            _productQuantity.value = "1"
            _productPrice.value = ""
        }
    }

    // List Action helpers
    fun selectSuggestion(selectedName: String) {
        _productName.value = selectedName
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    fun toggleItemPurchased(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.updateItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun updateItemDetails(item: ShoppingListItem, newQty: Double, newPrice: Double) {
        viewModelScope.launch {
            repository.updateItem(item.copy(quantity = newQty, unitPrice = newPrice))
        }
    }

    fun clearActiveList() {
        viewModelScope.launch {
            repository.clearListItems(0)
        }
    }

    fun saveActiveList(listName: String) {
        viewModelScope.launch {
            val currentItems = activeItems.value
            if (currentItems.isNotEmpty()) {
                repository.saveCurrentList(listName, currentItems)
            }
        }
    }

    fun deleteSavedList(listId: Long) {
        viewModelScope.launch {
            repository.deleteSavedList(listId)
        }
    }

    fun loadSavedList(savedList: SavedList) {
        viewModelScope.launch {
            // Fetch items of the saved list
            repository.getItemsForList(savedList.id).firstOrNull()?.let { savedItems ->
                // Clear active list
                repository.clearListItems(0)
                // Copy saved items into the active list (listId = 0)
                savedItems.forEach { item ->
                    repository.insertItem(
                        item.copy(id = 0, listId = 0, isPurchased = false)
                    )
                }
            }
        }
    }

    // Formatting currency in BRL (R$)
    fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            format.format(amount)
        } catch (e: Exception) {
            val formatted = String.format(Locale.US, "%.2f", amount).replace(".", ",")
            "R$ $formatted"
        }
    }

    // Formatting decimal quantities cleanly
    fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).replace(".", ",")
        }
    }
}
