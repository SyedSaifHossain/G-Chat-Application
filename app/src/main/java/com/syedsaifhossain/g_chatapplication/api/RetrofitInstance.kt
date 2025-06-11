package com.syedsaifhossain.g_chatapplication.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // Change the BASE_URL to your actual API URL.
    private const val BASE_URL = "http://your-webhook-server-url/"

    // Retrofit instance with base URL and Gson converter for API calls.
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL) // The base URL of your API
        .addConverterFactory(GsonConverterFactory.create()) // Add Gson converter to handle JSON
        .build()

    // API service instance
    val api: WebhookApiService = retrofit.create(WebhookApiService::class.java)
}