package com.defitracker.app.data.remote.dto

data class HTXMarketDetailDto(
    val status: String,
    val tick: HTXTickDto?
)

data class HTXTickDto(
    val id: Long,
    val open: Double,
    val close: Double,
    val low: Double,
    val high: Double,
    val amount: Double,
    val vol: Double
)

data class HTXHistoryKlineDto(
    val status: String,
    val data: List<HTXTickDto>?
)
