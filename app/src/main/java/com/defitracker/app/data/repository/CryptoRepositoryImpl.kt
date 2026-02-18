package com.defitracker.app.data.repository

import com.defitracker.app.data.local.TrackedPairDao
import com.defitracker.app.data.local.TrackedPairEntity
import com.defitracker.app.data.local.WalletDao
import com.defitracker.app.data.local.WalletEntity
import com.defitracker.app.data.remote.BinanceApi
import com.defitracker.app.data.remote.CoinStatsApi
import com.defitracker.app.data.remote.EtherscanApi
import com.defitracker.app.data.remote.HTXApi
import com.defitracker.app.data.remote.dto.CoinStatsBalanceDto
import com.defitracker.app.data.remote.dto.EtherscanTransactionDto
import com.defitracker.app.domain.model.CryptoPair
import com.defitracker.app.domain.model.PairDetail
import com.defitracker.app.domain.repository.CryptoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val htxApi: HTXApi,
    private val coinStatsApi: CoinStatsApi,
    @Named("ethereum") private val ethereumApi: EtherscanApi,
    @Named("bsc") private val bscApi: EtherscanApi,
    @Named("base") private val baseApi: EtherscanApi,
    @Named("optimism") private val optimismApi: EtherscanApi,
    @Named("polygon") private val polygonApi: EtherscanApi,
    @Named("arbitrum") private val arbitrumApi: EtherscanApi,
    private val trackedPairDao: TrackedPairDao,
    private val walletDao: WalletDao
) : CryptoRepository {

    private var cachedSymbols: List<Pair<String, String>> = emptyList()

    private val chainIdToApi = mapOf(
        "1" to ethereumApi,
        "56" to bscApi,
        "8453" to baseApi,
        "10" to optimismApi,
        "137" to polygonApi,
        "42161" to arbitrumApi
    )

    override fun getTrackedPairs(): Flow<List<CryptoPair>> {
        return trackedPairDao.getAllTrackedPairs().map { entities ->
            entities.map {
                CryptoPair(
                    symbol = it.symbol,
                    baseAsset = it.baseAsset,
                    quoteAsset = it.quoteAsset,
                    price = "...",
                    priceChangePercent = "0.00",
                    isPositive = true,
                    source = it.source
                )
            }
        }
    }

    override suspend fun refreshTrackedPairs() {
    }

    override suspend fun addTrackedPair(symbol: String, baseAsset: String, source: String) {
        trackedPairDao.insertTrackedPair(TrackedPairEntity(symbol, baseAsset, source = source))
    }

    override suspend fun removeTrackedPair(symbol: String) {
        trackedPairDao.deleteTrackedPairBySymbol(symbol)
    }

    override suspend fun getPairDetail(symbol: String, source: String): PairDetail {
        return try {
            if (source == "HTX") {
                val res = htxApi.getMarketDetailMerged(symbol.lowercase())
                val tick = res.tick
                PairDetail(
                    symbol = symbol,
                    price = tick?.close?.let { formatPrice(it) } ?: "0.00",
                    priceChange = (tick?.let { it.close - it.open } ?: 0.0).toString(),
                    priceChangePercent = (tick?.let { (it.close - it.open) / it.open * 100 } ?: 0.0).format(2),
                    highPrice = tick?.high?.toString() ?: "0.00",
                    lowPrice = tick?.low?.toString() ?: "0.00",
                    volume = tick?.amount?.toString() ?: "0.00",
                    quoteVolume = tick?.vol?.toString() ?: "0.00",
                    isPositive = (tick?.let { it.close >= it.open } ?: true)
                )
            } else {
                val stats = binanceApi.get24hStats(symbol)
                val priceDto = binanceApi.getPrice(symbol)
                val priceDouble = priceDto.price.toDoubleOrNull() ?: 0.0
                PairDetail(
                    symbol = symbol,
                    price = formatPrice(priceDouble),
                    priceChange = stats.priceChange,
                    priceChangePercent = stats.priceChangePercent,
                    highPrice = stats.highPrice,
                    lowPrice = stats.lowPrice,
                    volume = stats.volume,
                    quoteVolume = stats.quoteVolume,
                    isPositive = (stats.priceChangePercent.toDoubleOrNull() ?: 0.0) >= 0
                )
            }
        } catch (e: Exception) {
            PairDetail(symbol, "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", true)
        }
    }

    override suspend fun getKlines(symbol: String, interval: String, source: String): List<List<Any>> {
        return try {
            if (source == "HTX") {
                val htxInterval = when(interval) {
                    "1d" -> "1day"
                    "4h" -> "4hour"
                    "1h" -> "60min"
                    "15m" -> "15min"
                    else -> "1day"
                }
                val res = htxApi.getHistoryKline(symbol.lowercase(), htxInterval)
                res.data?.map { listOf(it.id * 1000, it.open, it.high, it.low, it.close) } ?: emptyList()
            } else {
                binanceApi.getKlines(symbol, interval)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAvailableSymbols(): List<Pair<String, String>> {
        if (cachedSymbols.isNotEmpty()) return cachedSymbols
        return try {
            val info = binanceApi.getExchangeInfo()
            cachedSymbols = info.symbols
                .filter { it.quoteAsset == "USDT" }
                .map { Pair(it.symbol, it.baseAsset) }
            cachedSymbols
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Wallets implementation
    override fun getSavedWallets(): Flow<List<WalletEntity>> = walletDao.getWallets()

    override suspend fun saveWallet(address: String, name: String) {
        walletDao.insertWallet(WalletEntity(address, name))
    }

    override suspend fun deleteWallet(address: String) {
        walletDao.getWalletByAddress(address)?.let {
            walletDao.deleteWallet(it)
        }
    }

    override suspend fun getWalletBalances(address: String): Map<String, List<CoinStatsBalanceDto>> = coroutineScope {
        val chains = listOf(
            "ethereum" to "Ether",
            "base-wallet" to "Base",
            "binancesmartchain" to "BSC",
            "solana" to "Solana",
            "optimism" to "Optimism",
            "arbitrum" to "Arbitrum",
            "polygon" to "Polygon"
        )
        
        val results = mutableMapOf<String, List<CoinStatsBalanceDto>>()
        
        chains.forEachIndexed { index, (chainId, chainName) ->
            try {
                if (index > 0) kotlinx.coroutines.delay(500)
                
                val balances = coinStatsApi.getWalletBalance(
                    address, 
                    chainId, 
                    com.defitracker.app.core.Constants.COINSTATS_API_KEY
                )
                
                if (balances.isNotEmpty()) {
                    val filtered = balances.filter { balance -> 
                        (balance.amount ?: 0.0) * (balance.price ?: 0.0) > 0.0001 
                    }
                    if (filtered.isNotEmpty()) {
                        results[chainName] = filtered
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results
    }

    override suspend fun getWalletTransactions(address: String): List<EtherscanTransactionDto> = coroutineScope {
        val chains = listOf(
            "1" to "Ethereum",
            "56" to "BSC",
            "8453" to "Base",
            "10" to "Optimism",
            "137" to "Polygon",
            "42161" to "Arbitrum"
        )
        
        val allTransactions = mutableListOf<EtherscanTransactionDto>()
        
        chains.forEach { (chainId, networkName) ->
            try {
                val api = chainIdToApi[chainId] ?: return@forEach
                
                // Fetch ERC20 transactions
                val tokentx = api.getTransactions(
                    action = "tokentx", 
                    address = address, 
                    apiKey = com.defitracker.app.core.Constants.ETHERSCAN_API_KEY
                )
                if (tokentx.status == "1") {
                    allTransactions.addAll(tokentx.result.map { it.copy(network = networkName) })
                }
                
                // Fetch Native ETH/BNB/etc transactions
                val txlist = api.getTransactions(
                    action = "txlist", 
                    address = address, 
                    apiKey = com.defitracker.app.core.Constants.ETHERSCAN_API_KEY
                )
                if (txlist.status == "1") {
                    allTransactions.addAll(txlist.result.map { 
                        it.copy(
                            tokenSymbol = when(networkName) {
                                "BSC" -> "BNB"
                                "Polygon" -> "MATIC"
                                else -> "ETH"
                            }, 
                            tokenDecimal = "18",
                            network = networkName
                        )
                    })
                }
                
                // Small delay to prevent hitting rate limits
                kotlinx.coroutines.delay(200)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        allTransactions.sortedByDescending { it.timeStamp.toLongOrNull() ?: 0L }
    }

    private fun formatPrice(price: Double): String {
        return if (price < 1.0) {
            "%.6f".format(price)
        } else {
            "%.2f".format(price)
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}

