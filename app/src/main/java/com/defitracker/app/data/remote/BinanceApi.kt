package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.Binance24hStatsDto
import com.defitracker.app.data.remote.dto.BinanceExchangeInfoDto
import com.defitracker.app.data.remote.dto.BinancePriceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {
    @GET("ticker/price")
    suspend fun getPrice(@Query("symbol") symbol: String): BinancePriceDto

    @GET("ticker/24hr")
    suspend fun get24hStats(@Query("symbol") symbol: String): Binance24hStatsDto

    @GET("exchangeInfo")
    suspend fun getExchangeInfo(): BinanceExchangeInfoDto

    @GET("klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500
    ): List<List<Any>>
}
