package com.defitracker.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedPairDao {
    @Query("SELECT * FROM tracked_pairs")
    fun getAllTrackedPairs(): Flow<List<TrackedPairEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedPair(pair: TrackedPairEntity)

    @Query("DELETE FROM tracked_pairs WHERE symbol = :symbol")
    suspend fun deleteTrackedPairBySymbol(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM tracked_pairs WHERE symbol = :symbol)")
    suspend fun isPairTracked(symbol: String): Boolean
}
