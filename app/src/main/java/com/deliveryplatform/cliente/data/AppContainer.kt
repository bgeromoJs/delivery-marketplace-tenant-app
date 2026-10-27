package com.deliveryplatform.cliente.data

import android.content.Context
import com.deliveryplatform.cliente.BuildConfig
import com.deliveryplatform.cliente.data.api.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val session = SessionStore(context)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val plainClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val plainApi: ApiService = retrofit(plainClient).create(ApiService::class.java)

    private val authClient = plainClient.newBuilder()
        .addInterceptor(Interceptor { chain ->
            val token = session.accessToken
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            }
            chain.proceed(request)
        })
        .build()

    val api: ApiService = retrofit(authClient).create(ApiService::class.java)

    val auth = AuthRepository(plainApi, session)
    val store = StoreRepository(api)
    val customer = CustomerRepository(api)
    val orders = OrderRepository(api)

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
}
