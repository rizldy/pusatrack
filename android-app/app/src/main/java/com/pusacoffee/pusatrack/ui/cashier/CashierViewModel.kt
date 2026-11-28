package com.pusacoffee.pusatrack.ui.cashier

import androidx.lifecycle.ViewModel
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CartLine(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Long get() = product.sellingPrice * quantity
}

class CashierViewModel : ViewModel() {

    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart: StateFlow<Map<Int, Int>> = _cart.asStateFlow()

    var selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH
    var cashAmountReceived: Long = 0L

    fun addItem(productId: Int) {
        val current = _cart.value.toMutableMap()
        current[productId] = (current[productId] ?: 0) + 1
        _cart.value = current
    }

    fun decrementItem(productId: Int) {
        val current = _cart.value.toMutableMap()
        val qty = (current[productId] ?: 0) - 1
        if (qty <= 0) {
            current.remove(productId)
        } else {
            current[productId] = qty
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        selectedPaymentMethod = PaymentMethod.CASH
        cashAmountReceived = 0L
    }

    fun getCartLines(): List<CartLine> {
        val productsById = ProductRepository.getAllProducts()
            .filter { it.isActive }
            .associateBy { it.productId }
        return _cart.value.mapNotNull { (productId, qty) ->
            val product = productsById[productId]
            if (product != null && qty > 0) CartLine(product, qty) else null
        }
    }

    fun getTotalItemCount(): Int = getCartLines().sumOf { it.quantity }

    fun getTotalAmount(): Long = getCartLines().sumOf { it.subtotal }
}
