package com.defitracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val address: String,
    val name: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
