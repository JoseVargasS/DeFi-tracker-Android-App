package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.MoralisTransactionsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Moralis API - transacciones nativas por wallet y cadena.
 * Tier gratuito incluye BSC, Polygon, Base, Optimism, Arbitrum.
 * API key gratuita: https://admin.moralis.io/register
 */
interface MoralisApi {
    @GET("api/v2.2/{address}")
    suspend fun getNativeTransactions(
        @Path("address") address: String,
        @Query("chain") chain: String,
        @Query("order") order: String = "DESC",
        @Query("limit") limit: Int = 100,
        @Header("X-API-Key") apiKey: String
    ): MoralisTransactionsResponse
}
