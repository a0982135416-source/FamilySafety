package com.example.familysafety.environment

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 客戶端單例物件 (RetrofitClient Object)
 * 管理並提供 ApiService 實例
 */
object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.100:8080/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
