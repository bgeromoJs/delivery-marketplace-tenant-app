package com.deliveryplatform.cliente.data.api

data class LoginRequest(val email: String, val password: String)

data class GoogleLoginRequest(val idToken: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: String = "CUSTOMER",
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String = "CUSTOMER",
    val tenantId: String? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
)

data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val user: UserDto,
)

data class TenantTheme(
    val tenantId: String,
    val name: String,
    val slug: String,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val primaryColor: String = "#D9480F",
    val secondaryColor: String = "#B83A0A",
    val deliveryFee: Double? = 0.0,
    val deliveryTimeMin: Int? = 40,
    val address: String? = null,
    val phone: String? = null,
    val rating: Double? = 4.8,
    val description: String? = null,
    val status: String? = null,
)

data class DishDto(
    val id: String,
    val tenantId: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
    val category: String = "Geral",
    val imageUrl: String? = null,
    val available: Boolean = true,
    val preparationTimeMin: Int? = null,
    val optionsJson: String? = null,
    val tagsJson: String? = null,
    val salesDaysJson: String? = null,
    val salesWindowsJson: String? = null,
    val effortMinutes: Int? = null,
)

data class DishOption(
    val id: String? = null,
    val name: String,
    val price: Double = 0.0,
    val required: Boolean = false,
    val maxChoices: Int = 1,
)

data class AvailabilityWindow(
    val date: String,
    val window: String,
    val start: String? = null,
    val end: String? = null,
)

data class OrderAvailability(
    val effortMinutes: Int = 0,
    val windows: List<AvailabilityWindow> = emptyList(),
)

data class AvailabilityItem(
    val dishId: String,
    val quantity: Int,
    val effortMinutes: Int? = null,
)

data class AvailabilityRequest(
    val tenantId: String,
    val items: List<AvailabilityItem>,
)

data class OrderItemDto(
    val id: String? = null,
    val dishId: String? = null,
    val dishName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double? = null,
    val optionsJson: String? = null,
    val effortMinutes: Int? = null,
)

data class OrderDto(
    val id: String,
    val orderNumber: String? = null,
    val tenantId: String,
    val customerId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val courierId: String? = null,
    val status: String,
    val origin: String = "APP",
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val deliveryAddress: String = "",
    val paymentMethod: String = "PIX",
    val notes: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val preparingAt: String? = null,
    val scheduledWindow: String? = null,
    val scheduledStart: String? = null,
    val scheduledEnd: String? = null,
    val reservedEffortMinutes: Int? = null,
)

data class CreateOrderItem(
    val dishId: String,
    val dishName: String,
    val quantity: Int,
    val unitPrice: Double,
    val optionsJson: String? = null,
    val effortMinutes: Int? = null,
)

data class CreateOrderRequest(
    val tenantId: String,
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String? = null,
    val deliveryAddress: String,
    val paymentMethod: String,
    val notes: String? = null,
    val deliveryFee: Double? = null,
    val scheduledDate: String? = null,
    val scheduledWindow: String? = null,
    val items: List<CreateOrderItem>,
)

data class CustomerProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val foodPreferences: List<String> = emptyList(),
    val googleLinked: Boolean = false,
    val createdAt: String? = null,
)

data class UpdateProfileRequest(
    val name: String,
    val phone: String? = null,
    val foodPreferences: List<String> = emptyList(),
)

data class AddressDto(
    val id: String,
    val label: String,
    val street: String,
    val number: String,
    val complement: String? = null,
    val district: String? = null,
    val city: String,
    val state: String,
    val zipCode: String? = null,
    val reference: String? = null,
    val isDefault: Boolean = false,
    val formatted: String? = null,
)

data class AddressRequest(
    val label: String,
    val street: String,
    val number: String,
    val complement: String? = null,
    val district: String? = null,
    val city: String,
    val state: String,
    val zipCode: String? = null,
    val reference: String? = null,
    val isDefault: Boolean = false,
)

data class CardDto(
    val id: String,
    val brand: String,
    val last4: String,
    val holderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean = false,
    val maskedNumber: String? = null,
)

data class CardRequest(
    val brand: String,
    val last4: String,
    val holderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean = false,
)
