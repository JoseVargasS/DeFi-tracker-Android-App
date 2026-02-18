package com.defitracker.app.di

import android.app.Application
import androidx.room.Room
import com.defitracker.app.core.Constants
import com.defitracker.app.data.local.AppDatabase
import com.defitracker.app.data.local.TrackedPairDao
import com.defitracker.app.data.local.WalletDao
import com.defitracker.app.data.local.WalletEntity
import com.defitracker.app.data.remote.BinanceApi
import com.defitracker.app.data.remote.CoinStatsApi
import com.defitracker.app.data.remote.EtherscanApi
import com.defitracker.app.data.remote.HTXApi
import com.defitracker.app.data.repository.CryptoRepositoryImpl
import com.defitracker.app.domain.repository.CryptoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBinanceApi(): BinanceApi {
        return Retrofit.Builder()
            .baseUrl(Constants.BINANCE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHTXApi(): HTXApi {
        return Retrofit.Builder()
            .baseUrl(Constants.HTX_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HTXApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCoinStatsApi(): CoinStatsApi {
        return Retrofit.Builder()
            .baseUrl(Constants.COINSTATS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinStatsApi::class.java)
    }

    @Provides
    @Singleton
    @Named("ethereum")
    fun provideEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.ETHEREUM_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    @Named("bsc")
    fun provideBSCEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.BSC_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    @Named("base")
    fun provideBaseEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    @Named("optimism")
    fun provideOptimismEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.OPTIMISM_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    @Named("polygon")
    fun providePolygonEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.POLYGON_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    @Named("arbitrum")
    fun provideArbitrumEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.ARBITRUM_ETHERSCAN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackedPairDao(db: AppDatabase) = db.trackedPairDao

    @Provides
    @Singleton
    fun provideWalletDao(db: AppDatabase) = db.walletDao

    @Provides
    @Singleton
    fun provideCryptoRepository(
        binanceApi: BinanceApi,
        htxApi: HTXApi,
        coinStatsApi: CoinStatsApi,
        @Named("ethereum") ethereumApi: EtherscanApi,
        @Named("bsc") bscApi: EtherscanApi,
        @Named("base") baseApi: EtherscanApi,
        @Named("optimism") optimismApi: EtherscanApi,
        @Named("polygon") polygonApi: EtherscanApi,
        @Named("arbitrum") arbitrumApi: EtherscanApi,
        trackedPairDao: TrackedPairDao,
        walletDao: WalletDao
    ): CryptoRepository {
        return CryptoRepositoryImpl(
            binanceApi, htxApi, coinStatsApi,
            ethereumApi, bscApi, baseApi, optimismApi, polygonApi, arbitrumApi,
            trackedPairDao, walletDao
        )
    }
}
