package com.defitracker.app.presentation.crypto_detail

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defitracker.app.domain.model.PairDetail
import com.defitracker.app.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class CryptoDetailViewModel @Inject constructor(
    private val repository: CryptoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun logNonFatal(context: String, throwable: Throwable) {
        Log.w(TAG, context, throwable)
    }

    private val _state = mutableStateOf(CryptoDetailState())
    val state: State<CryptoDetailState> = _state

    private val symbol: String = checkNotNull(savedStateHandle["symbol"])
    private val source: String = checkNotNull(savedStateHandle["source"])

    private var refreshJob: Job? = null
    private var chartJob: Job? = null

    init {
        _state.value = state.value.copy(symbol = symbol)
        loadDetail()
        startUpdates()
        loadChartData(DEFAULT_CHART_INTERVAL)
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            try {
                val detail = repository.getPairDetail(symbol, source)
                _state.value = state.value.copy(detail = detail, isLoading = false)
            } catch (e: Exception) {
                _state.value = state.value.copy(error = e.message ?: "Error", isLoading = false)
            }
        }
    }

    private fun startUpdates() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(DETAIL_REFRESH_MS)
                try {
                    val detail = repository.getPairDetail(symbol, source)
                    val currentPrice = detail.price.toDoubleOrNull() ?: 0.0
                    
                    if (currentPrice > 0.0) {
                        val updatedCandles = _state.value.candles.toMutableList()
                        if (updatedCandles.isNotEmpty()) {
                            val lastCandle = updatedCandles.last()
                            val newLastCandle = lastCandle.copy(
                                close = currentPrice,
                                high = currentPrice.coerceAtLeast(lastCandle.high),
                                low = currentPrice.coerceAtMost(lastCandle.low)
                            )
                            updatedCandles[updatedCandles.size - 1] = newLastCandle
                        }

                        val chartData = withContext(Dispatchers.Default) {
                            updatedCandles.toChartComputation()
                        }
                        
                        _state.value = _state.value.copy(
                            detail = detail,
                            candles = chartData.candles,
                            bbUpper = chartData.bbUpper,
                            bbMiddle = chartData.bbMiddle,
                            bbLower = chartData.bbLower,
                            stochK = chartData.stochK,
                            stochD = chartData.stochD
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }
        }
    }

    fun loadChartData(interval: String) {
        val normalizedInterval = interval.trim()
        if (normalizedInterval == state.value.selectedInterval && state.value.candles.isNotEmpty()) return

        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _state.value = state.value.copy(selectedInterval = normalizedInterval, isLoading = true, error = "")
            try {
                val rawKlines = repository.getKlines(symbol, normalizedInterval.toBinanceInterval(), source)
                val chartData = withContext(Dispatchers.Default) {
                    val candles = rawKlines.map {
                        CandleData(
                            time = (it[0] as? Number)?.toLong() ?: it[0].toString().toLongOrNull() ?: 0L,
                            open = it[1].toString().toDoubleOrNull() ?: 0.0,
                            high = it[2].toString().toDoubleOrNull() ?: 0.0,
                            low = it[3].toString().toDoubleOrNull() ?: 0.0,
                            close = it[4].toString().toDoubleOrNull() ?: 0.0,
                            volume = it.getOrNull(5)?.toString()?.toDoubleOrNull() ?: 0.0
                        )
                    }.filter { it.time > 0 }.aggregateForInterval(normalizedInterval)

                    if (candles.isEmpty()) {
                        return@withContext ChartComputation()
                    }

                    candles.toChartComputation()
                }

                _state.value = state.value.copy(
                    candles = chartData.candles,
                    bbUpper = chartData.bbUpper,
                    bbMiddle = chartData.bbMiddle,
                    bbLower = chartData.bbLower,
                    stochK = chartData.stochK,
                    stochD = chartData.stochD,
                    isLoading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logNonFatal("Chart data load failed for $symbol/$normalizedInterval", e)
                _state.value = state.value.copy(isLoading = false, error = e.message ?: "Error")
            }
        }
    }

    private fun List<CandleData>.toChartComputation(): ChartComputation {
        if (isEmpty()) return ChartComputation()

        val period = 20
        val multiplier = 2.0
        val bbUpper = mutableListOf<Pair<Long, Double>>()
        val bbMiddle = mutableListOf<Pair<Long, Double>>()
        val bbLower = mutableListOf<Pair<Long, Double>>()

        for (i in indices) {
            if (i >= period - 1) {
                val slice = subList(i - period + 1, i + 1)
                val sma = slice.sumOf { it.close } / period
                val variance = slice.sumOf { (it.close - sma).pow(2.0) } / period
                val stdDev = sqrt(variance)

                bbMiddle.add(i.toLong() to sma)
                bbUpper.add(i.toLong() to (sma + multiplier * stdDev))
                bbLower.add(i.toLong() to (sma - multiplier * stdDev))
            }
        }

        val stochK = mutableListOf<Pair<Long, Double>>()
        val stochD = mutableListOf<Pair<Long, Double>>()
        val rsiValues = calculateRSI(this)

        if (rsiValues.size >= STOCH_RSI_PERIOD) {
            val stochRSI = mutableListOf<Double>()
            for (i in rsiValues.indices) {
                if (i >= STOCH_RSI_PERIOD - 1) {
                    val slice = rsiValues.subList(i - STOCH_RSI_PERIOD + 1, i + 1)
                    val low = slice.minOrNull() ?: 0.0
                    val high = slice.maxOrNull() ?: 100.0
                    val current = rsiValues[i]
                    val s = if (high - low != 0.0) (current - low) / (high - low) * 100 else 0.0
                    stochRSI.add(s)
                } else {
                    stochRSI.add(0.0)
                }
            }

            val smoothK = calculateSMA(stochRSI)
            val smoothD = calculateSMA(smoothK)

            for (i in smoothK.indices) {
                val candleIndex = i + (size - smoothK.size)
                if (candleIndex >= 0) {
                    stochK.add(candleIndex.toLong() to smoothK[i])
                    stochD.add(candleIndex.toLong() to smoothD[i])
                }
            }
        }

        return ChartComputation(
            candles = this,
            bbUpper = bbUpper,
            bbMiddle = bbMiddle,
            bbLower = bbLower,
            stochK = stochK,
            stochD = stochD
        )
    }

    private fun calculateRSI(candles: List<CandleData>): List<Double> {
        val rsi = mutableListOf<Double>()
        val period = STOCH_RSI_PERIOD
        if (candles.size < period) return emptyList()
        
        var avgGain = 0.0
        var avgLoss = 0.0
        
        for (i in 1..period) {
            val diff = candles[i].close - candles[i-1].close
            if (diff >= 0) avgGain += diff else avgLoss -= diff
        }
        avgGain /= period
        avgLoss /= period
        
        rsi.add(if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss)))
        
        for (i in period + 1 until candles.size) {
            val diff = candles[i].close - candles[i-1].close
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            
            rsi.add(if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss)))
        }
        
        // Pad beginning with zeros to match candle indices
        val padding = List(period) { 0.0 }
        return padding + rsi
    }

    private fun calculateSMA(values: List<Double>): List<Double> {
        val period = STOCH_SMOOTH_PERIOD
        val sma = mutableListOf<Double>()
        for (i in values.indices) {
            if (i >= period - 1) {
                val slice = values.subList(i - period + 1, i + 1)
                sma.add(slice.average())
            } else {
                sma.add(values[i])
            }
        }
        return sma
    }

    private fun String.toBinanceInterval(): String {
        return when (this) {
            "5d" -> "1d"
            "2w" -> "1w"
            "1mo" -> "1M"
            else -> this
        }
    }

    private fun List<CandleData>.aggregateForInterval(interval: String): List<CandleData> {
        val chunkSize = when (interval) {
            "5d" -> 5
            "2w" -> 2
            else -> return this
        }

        return chunked(chunkSize).mapNotNull { chunk ->
            val first = chunk.firstOrNull() ?: return@mapNotNull null
            val last = chunk.last()
            CandleData(
                time = first.time,
                open = first.open,
                high = chunk.maxOf { it.high },
                low = chunk.minOf { it.low },
                close = last.close,
                volume = chunk.sumOf { it.volume }
            )
        }
    }

    private companion object {
        const val TAG = "CryptoDetailVM"
        const val DETAIL_REFRESH_MS = 5_000L
        const val DEFAULT_CHART_INTERVAL = "12h"
        const val STOCH_RSI_PERIOD = 14
        const val STOCH_SMOOTH_PERIOD = 3
    }
}

data class CryptoDetailState(
    val symbol: String = "",
    val detail: PairDetail? = null,
    val candles: List<CandleData> = emptyList(),
    val bbUpper: List<Pair<Long, Double>> = emptyList(),
    val bbMiddle: List<Pair<Long, Double>> = emptyList(),
    val bbLower: List<Pair<Long, Double>> = emptyList(),
    val stochK: List<Pair<Long, Double>> = emptyList(),
    val stochD: List<Pair<Long, Double>> = emptyList(),
    val selectedInterval: String = "12h",
    val isLoading: Boolean = false,
    val error: String = ""
)

data class CandleData(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)

private data class ChartComputation(
    val candles: List<CandleData> = emptyList(),
    val bbUpper: List<Pair<Long, Double>> = emptyList(),
    val bbMiddle: List<Pair<Long, Double>> = emptyList(),
    val bbLower: List<Pair<Long, Double>> = emptyList(),
    val stochK: List<Pair<Long, Double>> = emptyList(),
    val stochD: List<Pair<Long, Double>> = emptyList()
)
