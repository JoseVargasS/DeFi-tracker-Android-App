package com.defitracker.app.data.remote.dto

// Reserved for fallback/alternate CoinStats responses kept in the project on purpose.
@Suppress("unused")
data class CoinStatsCoinDto(
    val id: String,
    val symbol: String,
    val price: Double?
)

