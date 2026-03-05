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
import com.defitracker.app.data.remote.ExplorerApi
import com.defitracker.app.data.remote.HTXApi
import com.defitracker.app.data.remote.MoralisApi
import com.defitracker.app.data.remote.dto.EtherscanResponse
import com.defitracker.app.data.remote.dto.EtherscanResponseDeserializer
import com.defitracker.app.data.repository.CryptoRepositoryImpl
import com.defitracker.app.domain.repository.CryptoRepository
import com.google.gson.GsonBuilder
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

    private fun explorerGson() = GsonBuilder()
        .registerTypeAdapter(EtherscanResponse::class.java, EtherscanResponseDeserializer())
        .create()

    @Provides
    @Singleton
    fun provideEtherscanApi(): EtherscanApi {
        return Retrofit.Builder()
            .baseUrl(Constants.ETHERSCAN_V2_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(explorerGson()))
            .build()
            .create(EtherscanApi::class.java)
    }

    @Provides @Singleton @Named("bsc")
    fun provideBscScanApi(): ExplorerApi = Retrofit.Builder()
        .baseUrl(Constants.BSC_SCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(explorerGson()))
        .build().create(ExplorerApi::class.java)

    @Provides @Singleton @Named("polygon")
    fun providePolygonScanApi(): ExplorerApi = Retrofit.Builder()
        .baseUrl(Constants.POLYGON_SCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(explorerGson()))
        .build().create(ExplorerApi::class.java)

    @Provides @Singleton @Named("base")
    fun provideBaseScanApi(): ExplorerApi = Retrofit.Builder()
        .baseUrl(Constants.BASE_SCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(explorerGson()))
        .build().create(ExplorerApi::class.java)

    @Provides @Singleton @Named("optimism")
    fun provideOptimismScanApi(): ExplorerApi = Retrofit.Builder()
        .baseUrl(Constants.OPTIMISM_SCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(explorerGson()))
        .build().create(ExplorerApi::class.java)

    @Provides @Singleton @Named("arbitrum")
    fun provideArbiscanApi(): ExplorerApi = Retrofit.Builder()
        .baseUrl(Constants.ARBISCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(explorerGson()))
        .build().create(ExplorerApi::class.java)

    @Provides @Singleton
    fun provideMoralisApi(): MoralisApi = Retrofit.Builder()
        .baseUrl(Constants.MORALIS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(MoralisApi::class.java)

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
        etherscanApi: EtherscanApi,
        @Named("bsc") bscScanApi: ExplorerApi,
        @Named("polygon") polygonScanApi: ExplorerApi,
        @Named("base") baseScanApi: ExplorerApi,
        @Named("optimism") optimismScanApi: ExplorerApi,
        @Named("arbitrum") arbiscanApi: ExplorerApi,
        moralisApi: MoralisApi,
        trackedPairDao: TrackedPairDao,
        walletDao: WalletDao
    ): CryptoRepository {
        return CryptoRepositoryImpl(
            binanceApi, htxApi, coinStatsApi,
            etherscanApi, bscScanApi, polygonScanApi, baseScanApi, optimismScanApi, arbiscanApi,
            moralisApi,
            trackedPairDao, walletDao
        )
    }
}
