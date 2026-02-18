package com.defitracker.app.domain.model

data class CryptoPair(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val price: String,
    val priceChangePercent: String,
    val isPositive: Boolean,
    val source: String
)

data class PairDetail(
    val symbol: String,
    val price: String,
    val priceChange: String,
    val priceChangePercent: String,
    val highPrice: String,
    val lowPrice: String,
    val volume: String,
    val quoteVolume: String,
    val isPositive: Boolean
)
