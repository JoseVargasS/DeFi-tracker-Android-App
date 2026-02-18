package com.defitracker.app.data.remote

import com.defitracker.app.data.remote.dto.HTXHistoryKlineDto
import com.defitracker.app.data.remote.dto.HTXMarketDetailDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HTXApi {
    @GET("market/detail")
    suspend fun getMarketDetail(@Query("symbol") symbol: String): HTXMarketDetailDto

    @GET("market/detail/merged")
    suspend fun getMarketDetailMerged(@Query("symbol") symbol: String): HTXMarketDetailDto

    @GET("market/history/kline")
    suspend fun getHistoryKline(
        @Query("symbol") symbol: String,
        @Query("period") period: String,
        @Query("size") size: Int = 500
    ): HTXHistoryKlineDto
}
