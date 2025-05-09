package com.syedsaifhossain.g_chatapplication.api

import com.syedsaifhossain.g_chatapplication.models.AddMoneyRequest
import com.syedsaifhossain.g_chatapplication.models.BalanceResponse
import com.syedsaifhossain.g_chatapplication.models.GenericResponse
import com.syedsaifhossain.g_chatapplication.models.PaymentIntentResponse
import com.syedsaifhossain.g_chatapplication.models.WithdrawRequest
import retrofit2.Call
import retrofit2.http.*

interface WalletApi {
    @POST("/create-payment-intent")
    suspend fun addMoney(@Body request: AddMoneyRequest): PaymentIntentResponse

    @POST("/withdraw")
    suspend fun withdraw(@Body request: WithdrawRequest): GenericResponse

    @GET("/balance/{userId}")
    suspend fun getBalance(@Path("userId") userId: String): BalanceResponse
}