package com.syedsaifhossain.g_chatapplication.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://your-webhook-server-url/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: WebhookApiService = retrofit.create(WebhookApiService::class.java)
}