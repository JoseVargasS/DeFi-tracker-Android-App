package com.defitracker.app.data.remote.dto

data class CoinStatsResponse(
    val result: List<CoinStatsCoinDto>?
)

data class CoinStatsCoinDto(
    val id: String,
    val symbol: String,
    val price: Double?
)

data class CoinStatsChartDto(
    val result: List<List<Double>>? // Assuming chart data is a list of [timestamp, price]
)
