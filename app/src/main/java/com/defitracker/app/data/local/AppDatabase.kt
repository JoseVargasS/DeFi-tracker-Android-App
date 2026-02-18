package com.defitracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackedPairEntity::class, WalletEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackedPairDao: TrackedPairDao
    abstract val walletDao: WalletDao
}
