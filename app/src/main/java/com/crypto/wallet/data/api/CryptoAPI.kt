package com.crypto.wallet.data.api

import com.crypto.wallet.data.models.Coin
import retrofit2.http.GET

interface CryptoAPI {
    @GET("coins/markets?vs_currency=usd")
    suspend fun getCoins(): List<Coin>
}