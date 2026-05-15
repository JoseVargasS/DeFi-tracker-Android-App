package com.defitracker.app.data.repository

import android.util.Log
import com.defitracker.app.core.Constants
import com.defitracker.app.data.local.TrackedPairDao
import com.defitracker.app.data.local.TrackedPairEntity
import com.defitracker.app.data.local.WalletDao
import com.defitracker.app.data.local.WalletEntity
import com.defitracker.app.data.remote.BinanceApi
import com.defitracker.app.data.remote.CoinStatsApi
import com.defitracker.app.data.remote.dto.CoinStatsBalanceDto
import com.defitracker.app.data.remote.dto.CoinStatsTransactionDto
import com.defitracker.app.data.remote.dto.EtherscanTransactionDto
import com.defitracker.app.domain.model.CryptoPair
import com.defitracker.app.domain.model.PairDetail
import com.defitracker.app.domain.repository.CryptoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val coinStatsApi: CoinStatsApi,
    private val trackedPairDao: TrackedPairDao,
    private val walletDao: WalletDao
) : CryptoRepository {

    private fun logNonFatal(context: String, throwable: Throwable) {
        Log.w(TAG, context, throwable)
    }

    private var cachedSymbols: List<Pair<String, String>> = emptyList()

    override fun getTrackedPairs(): Flow<List<CryptoPair>> {
        return trackedPairDao.getAllTrackedPairs().map { entities ->
            entities.map {
                CryptoPair(
                    symbol = it.symbol,
                    baseAsset = it.baseAsset,
                    quoteAsset = it.quoteAsset,
                    price = "0.00",
                    priceChangePercent = "0.00",
                    isPositive = true,
                    source = it.source
                )
            }
        }
    }

    override suspend fun addTrackedPair(symbol: String, baseAsset: String, source: String) {
        trackedPairDao.insertTrackedPair(TrackedPairEntity(symbol, baseAsset, source = source))
    }

    override suspend fun removeTrackedPair(symbol: String) {
        trackedPairDao.deleteTrackedPairBySymbol(symbol)
    }

    override suspend fun getPairDetail(symbol: String, source: String): PairDetail {
        return try {
            when (source) {
                "Binance" -> getBinancePairDetail(symbol)
                else -> getBinancePairDetail(symbol)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PairDetail(symbol, "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", true)
        }
    }

    override suspend fun getKlines(symbol: String, interval: String, source: String): List<List<Any>> {
        return try {
            when (source) {
                "Binance" -> getBinanceKlines(symbol, interval)
                else -> getBinanceKlines(symbol, interval)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
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
        val results = linkedMapOf<String, List<CoinStatsBalanceDto>>()

        walletBalanceChains.forEachIndexed { index, chain ->
            try {
                if (index > 0) delay(500)

                val balances = coinStatsApi.getWalletBalance(
                    address,
                    chain.id,
                    Constants.COINSTATS_API_KEY
                )

                val filtered = balances.filter { balance ->
                    (balance.amount ?: 0.0) * (balance.price ?: 0.0) > 0.0001
                }

                if (filtered.isNotEmpty()) {
                    results[chain.name] = filtered
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    throw IllegalStateException("CoinStats API key is invalid or expired.")
                }
                if (e.code() == 429) {
                    throw IllegalStateException("CoinStats rate limit reached. Try again later.")
                }
                logNonFatal("Wallet balances request failed for ${chain.name}", e)
            } catch (e: Exception) {
                logNonFatal("Unexpected wallet balances failure for ${chain.name}", e)
            }
        }

        results
    }

    override suspend fun getWalletTransactions(address: String): List<EtherscanTransactionDto> = coroutineScope {
        val allTransactions = mutableListOf<EtherscanTransactionDto>()

        transactionChains.forEachIndexed { index, chain ->
            try {
                if (index > 0) delay(350)

                val transactions = coinStatsApi.getWalletTransactions(
                    address = address,
                    chainId = chain.id,
                    apiKey = Constants.COINSTATS_API_KEY
                ).result
                    .filterNot { it.type.equals("Fill", ignoreCase = true) }
                    .mapNotNull { it.toTransactionDto(address, chain.name) }

                allTransactions += transactions
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    throw IllegalStateException("CoinStats API key is invalid or expired.")
                }
                if (e.code() == 429) {
                    throw IllegalStateException("CoinStats rate limit reached. Try again later.")
                }
                logNonFatal("Wallet transactions request failed for ${chain.name}", e)
            } catch (e: Exception) {
                logNonFatal("Unexpected wallet transactions failure for ${chain.name}", e)
            }
        }

        allTransactions
            .distinctBy { "${it.network}-${it.hash}-${it.tokenSymbol}-${it.value}" }
            .sortedWith(
                compareByDescending<EtherscanTransactionDto> { it.timeStamp.toLongOrNull() ?: 0L }
                    .thenBy { it.network ?: "" }
            )
    }

    private fun CoinStatsTransactionDto.toTransactionDto(
        walletAddress: String,
        networkName: String
    ): EtherscanTransactionDto? {
        val firstItem = transactions.firstOrNull()?.items?.firstOrNull()
        val count = firstItem?.count ?: coinData?.count ?: return null
        val symbol = firstItem?.coin?.symbol ?: coinData?.symbol ?: return null
        val action = transactions.firstOrNull()?.action ?: type.orEmpty()
        val isSent = action.equals("Sent", ignoreCase = true) || count < 0.0
        val hashId = hash?.id ?: firstItem?.id ?: return null
        val timestamp = try {
            date?.let { java.time.Instant.parse(it).epochSecond.toString() } ?: "0"
        } catch (_: Exception) {
            "0"
        }

        return EtherscanTransactionDto(
            hash = hashId,
            from = firstItem?.fromAddress ?: if (isSent) walletAddress else "",
            to = firstItem?.toAddress ?: if (isSent) "" else walletAddress,
            value = kotlin.math.abs(count).toString(),
            timeStamp = timestamp,
            tokenSymbol = symbol.trim(),
            tokenDecimal = "0",
            tokenName = firstItem?.coin?.name ?: symbol.trim(),
            input = null,
            symbol = null,
            decimals = null,
            network = networkName
        )
    }

    private suspend fun getBinancePairDetail(symbol: String): PairDetail {
        return try {
            val stats = binanceApi.get24hStats(symbol)
            val priceDouble = stats.lastPrice?.toDoubleOrNull() ?: 0.0
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
        } catch (e: Exception) {
            val fallbackPrice = binanceApi.getPrice(symbol).price.toDoubleOrNull() ?: 0.0
            PairDetail(
                symbol = symbol,
                price = formatPrice(fallbackPrice),
                priceChange = "0.00",
                priceChangePercent = "0.00",
                highPrice = "0.00",
                lowPrice = "0.00",
                volume = "0.00",
                quoteVolume = "0.00",
                isPositive = true
            )
        }
    }

    private suspend fun getBinanceKlines(symbol: String, interval: String): List<List<Any>> {
        val allKlines = mutableListOf<List<Any>>()
        var endTime: Long? = null
        var pagesFetched = 0

        while (pagesFetched < CHART_KLINE_PAGE_COUNT) {
            val page = binanceApi.getKlines(
                symbol = symbol,
                interval = interval,
                limit = CHART_KLINE_PAGE_SIZE,
                endTime = endTime
            )
            if (page.isEmpty()) break

            allKlines.addAll(0, page)
            val oldestOpenTime = page.firstOrNull()?.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong()
                ?: break
            endTime = oldestOpenTime - 1
            pagesFetched++

            if (page.size < CHART_KLINE_PAGE_SIZE) break
        }

        return allKlines
    }

    private fun formatPrice(price: Double): String {
        return if (price < 1.0) {
            String.format(java.util.Locale.US, "%.6f", price)
        } else {
            String.format(java.util.Locale.US, "%.2f", price)
        }
    }

    private data class CoinStatsChain(val id: String, val name: String)

    private companion object {
        const val TAG = "CryptoRepository"
        const val CHART_KLINE_PAGE_SIZE = 1000
        const val CHART_KLINE_PAGE_COUNT = 3

        val walletBalanceChains = listOf(
            CoinStatsChain("ethereum", "Ether"),
            CoinStatsChain("base-wallet", "Base"),
            CoinStatsChain("binancesmartchain", "BSC"),
            CoinStatsChain("solana", "Solana"),
            CoinStatsChain("optimism", "Optimism"),
            CoinStatsChain("arbitrum", "Arbitrum"),
            CoinStatsChain("polygon", "Polygon")
        )

        val transactionChains = listOf(
            CoinStatsChain("ethereum", "Ethereum"),
            CoinStatsChain("base-wallet", "Base"),
            CoinStatsChain("binancesmartchain", "BSC"),
            CoinStatsChain("polygon", "Polygon"),
            CoinStatsChain("arbitrum", "Arbitrum"),
            CoinStatsChain("optimism", "Optimism"),
            CoinStatsChain("solana", "Solana")
        )
    }
}

