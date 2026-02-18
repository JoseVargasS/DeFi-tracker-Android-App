package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.EtherscanResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EtherscanApi {
    @GET("api")
    suspend fun getTransactions(
        @Query("module") module: String = "account",
        @Query("action") action: String,
        @Query("address") address: String,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): EtherscanResponse
}
