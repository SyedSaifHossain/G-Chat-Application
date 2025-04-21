package com.syedsaifhossain.g_chatapplication.api

import retrofit2.Response
import com.syedsaifhossain.g_chatapplication.models.Message
import retrofit2.http.Body
import retrofit2.http.POST

interface WebhookApiService {
    @POST("sendMessage")
    suspend fun sendMessage(@Body message: Message): Response<Void>
}