package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.CoinStatsBalanceDto
import com.defitracker.app.data.remote.dto.CoinStatsTransactionSyncRequest
import com.defitracker.app.data.remote.dto.CoinStatsTransactionSyncResponse
import com.defitracker.app.data.remote.dto.CoinStatsTransactionsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Query

interface CoinStatsApi {
    @GET("wallet/balance")
    suspend fun getWalletBalance(
        @Query("address") address: String,
        @Query("connectionId") chainId: String,
        @Header("X-API-KEY") apiKey: String
    ): List<CoinStatsBalanceDto>

    @GET("wallet/transactions")
    suspend fun getWalletTransactions(
        @Query("address") address: String,
        @Query("connectionId") chainId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Header("X-API-KEY") apiKey: String
    ): CoinStatsTransactionsResponse

    @PATCH("wallet/transactions")
    suspend fun syncWalletTransactions(
        @Header("X-API-KEY") apiKey: String,
        @Body body: CoinStatsTransactionSyncRequest
    ): CoinStatsTransactionSyncResponse

    @GET("wallet/status")
    suspend fun getWalletTransactionSyncStatus(
        @Query("address") address: String,
        @Query("connectionId") chainId: String,
        @Header("X-API-KEY") apiKey: String
    ): CoinStatsTransactionSyncResponse
}
