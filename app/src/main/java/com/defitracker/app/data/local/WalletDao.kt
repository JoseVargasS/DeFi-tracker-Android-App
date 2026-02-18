package com.defitracker.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY addedAt DESC")
    fun getWallets(): Flow<List<WalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE address = :address LIMIT 1")
    suspend fun getWalletByAddress(address: String): WalletEntity?
}
