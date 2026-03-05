package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.EtherscanResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Explorer API V1-style: used by BscScan, Polygonscan, Basescan, etc.
 * Same request/response as Etherscan; each chain has its own base URL.
 * No chainid parameter.
 */
interface ExplorerApi {
    @GET("api")
    suspend fun getTransactions(
        @Query("module") module: String = "account",
        @Query("action") action: String,
        @Query("address") address: String,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String
    ): EtherscanResponse
}
