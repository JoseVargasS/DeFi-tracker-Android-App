package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.CoinStatsBalanceDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CoinStatsApi {
    @GET("wallet/balance")
    suspend fun getWalletBalance(
        @Query("address") address: String,
        @Query("connectionId") chainId: String,
        @Header("X-API-KEY") apiKey: String
    ): List<CoinStatsBalanceDto>
}
