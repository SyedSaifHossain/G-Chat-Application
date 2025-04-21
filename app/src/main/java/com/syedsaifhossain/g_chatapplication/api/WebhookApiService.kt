package com.syedsaifhossain.g_chatapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.syedsaifhossain.g_chatapplication.models.Message

interface WebhookApiService {
    @POST("sendMessage") // Make sure the URL path here matches with your endpoint
    suspend fun sendMessage(@Body message: Message): Response<Void>
}