package com.deliveryplatform.cliente.data

import com.deliveryplatform.cliente.data.api.AddressDto
import com.deliveryplatform.cliente.data.api.AddressRequest
import com.deliveryplatform.cliente.data.api.ApiService
import com.deliveryplatform.cliente.data.api.AuthResponse
import com.deliveryplatform.cliente.data.api.AvailabilityRequest
import com.deliveryplatform.cliente.data.api.CardDto
import com.deliveryplatform.cliente.data.api.CardRequest
import com.deliveryplatform.cliente.data.api.CreateOrderRequest
import com.deliveryplatform.cliente.data.api.CustomerProfile
import com.deliveryplatform.cliente.data.api.CustomerStoreDto
import com.deliveryplatform.cliente.data.api.DishDto
import com.deliveryplatform.cliente.data.api.GoogleLoginRequest
import com.deliveryplatform.cliente.data.api.LinkStoreRequest
import com.deliveryplatform.cliente.data.api.LoginRequest
import com.deliveryplatform.cliente.data.api.OrderAvailability
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.data.api.RegisterRequest
import com.deliveryplatform.cliente.data.api.TenantSummary
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.data.api.UpdateProfileRequest

class AuthRepository(private val api: ApiService, private val session: SessionStore) {

    suspend fun login(email: String, password: String): AuthResponse =
        api.login(LoginRequest(email.trim(), password)).also(::persist)

    suspend fun register(name: String, email: String, password: String, phone: String?): AuthResponse =
        api.register(RegisterRequest(name.trim(), email.trim(), password, phone?.trim())).also(::persist)

    suspend fun google(idToken: String): AuthResponse =
        api.loginWithGoogle(GoogleLoginRequest(idToken)).also(::persist)

    fun logout() = session.clear()

    private fun persist(response: AuthResponse) {
        session.save(response.token, response.user.id, response.user.name, response.user.email)
    }
}

class StoreRepository(private val api: ApiService) {
    suspend fun theme(slug: String): TenantTheme = api.theme(slug)
    suspend fun tenants(): List<TenantSummary> = api.tenants()
    suspend fun dishes(tenantId: String, tags: String?): List<DishDto> =
        api.dishes(tenantId, availableOnly = true, tags = tags)
}
class CustomerRepository(private val api: ApiService) {
    suspend fun profile(): CustomerProfile = api.profile()
    suspend fun stores(): List<CustomerStoreDto> = api.customerStores()
    suspend fun linkStore(slug: String): CustomerStoreDto = api.linkStore(LinkStoreRequest(slug))
    suspend fun unlinkStore(slug: String) = api.unlinkStore(slug)
    suspend fun updateProfile(body: UpdateProfileRequest): CustomerProfile = api.updateProfile(body)
    suspend fun addresses(): List<AddressDto> = api.addresses()
    suspend fun createAddress(body: AddressRequest): AddressDto = api.createAddress(body)
    suspend fun updateAddress(id: String, body: AddressRequest): AddressDto = api.updateAddress(id, body)
    suspend fun deleteAddress(id: String) = api.deleteAddress(id)
    suspend fun defaultAddress(id: String): AddressDto = api.defaultAddress(id)
    suspend fun cards(): List<CardDto> = api.cards()
    suspend fun createCard(body: CardRequest): CardDto = api.createCard(body)
    suspend fun deleteCard(id: String) = api.deleteCard(id)
    suspend fun defaultCard(id: String): CardDto = api.defaultCard(id)
}

class OrderRepository(private val api: ApiService) {
    suspend fun availability(body: AvailabilityRequest): OrderAvailability = api.availability(body)
    suspend fun create(body: CreateOrderRequest): OrderDto = api.createOrder(body)
    suspend fun byCustomer(customerId: String): List<OrderDto> = api.customerOrders(customerId)
}
