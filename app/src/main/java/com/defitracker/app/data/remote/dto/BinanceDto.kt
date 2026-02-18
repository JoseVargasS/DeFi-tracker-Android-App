package com.defitracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BinancePriceDto(
    val symbol: String,
    val price: String
)

data class Binance24hStatsDto(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val highPrice: String,
    val lowPrice: String,
    val volume: String,
    val quoteVolume: String
)

data class BinanceExchangeInfoDto(
    val symbols: List<BinanceSymbolDto>
)

data class BinanceSymbolDto(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String
)
