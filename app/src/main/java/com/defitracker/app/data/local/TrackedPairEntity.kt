package com.defitracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_pairs")
data class TrackedPairEntity(
    @PrimaryKey val symbol: String, // e.g., "BTCUSDT"
    val baseAsset: String,
    val quoteAsset: String = "USDT",
    val source: String = "Binance" // "Binance" or "HTX"
)
