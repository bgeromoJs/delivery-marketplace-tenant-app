package com.deliveryplatform.cliente.data.api

import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginRequest): AuthResponse

    @GET("tenants/{slug}/theme")
    suspend fun theme(@Path("slug") slug: String): TenantTheme

    @GET("catalog/tenants/{tenantId}/dishes")
    suspend fun dishes(
        @Path("tenantId") tenantId: String,
        @Query("availableOnly") availableOnly: Boolean = true,
        @Query("tags") tags: String? = null,
    ): List<DishDto>

    @POST("orders/availability")
    suspend fun availability(@Body body: AvailabilityRequest): OrderAvailability

    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): OrderDto

    @GET("orders/customer/{customerId}")
    suspend fun customerOrders(@Path("customerId") customerId: String): List<OrderDto>

    @GET("customers/me")
    suspend fun profile(): CustomerProfile

    @PUT("customers/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): CustomerProfile

    @GET("customers/me/addresses")
    suspend fun addresses(): List<AddressDto>

    @POST("customers/me/addresses")
    suspend fun createAddress(@Body body: AddressRequest): AddressDto

    @PUT("customers/me/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: String, @Body body: AddressRequest): AddressDto

    @DELETE("customers/me/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: String)

    @PATCH("customers/me/addresses/{id}/default")
    suspend fun defaultAddress(@Path("id") id: String): AddressDto

    @GET("customers/me/cards")
    suspend fun cards(): List<CardDto>

    @POST("customers/me/cards")
    suspend fun createCard(@Body body: CardRequest): CardDto

    @DELETE("customers/me/cards/{id}")
    suspend fun deleteCard(@Path("id") id: String)

    @PATCH("customers/me/cards/{id}/default")
    suspend fun defaultCard(@Path("id") id: String): CardDto
}
