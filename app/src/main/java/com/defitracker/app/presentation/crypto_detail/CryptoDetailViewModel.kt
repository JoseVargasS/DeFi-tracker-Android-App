package com.defitracker.app.presentation.crypto_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defitracker.app.domain.model.PairDetail
import com.defitracker.app.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class CryptoDetailViewModel @Inject constructor(
    private val repository: CryptoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(CryptoDetailState())
    val state: State<CryptoDetailState> = _state

    private val symbol: String = checkNotNull(savedStateHandle["symbol"])
    private val source: String = checkNotNull(savedStateHandle["source"])

    private var refreshJob: Job? = null

    init {
        loadDetail()
        startUpdates()
        loadChartData("1d")
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
                delay(2000)
                try {
                    val detail = repository.getPairDetail(symbol, source)
                    _state.value = state.value.copy(detail = detail)
                } catch (e: Exception) {}
            }
        }
    }

    fun loadChartData(interval: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(selectedInterval = interval)
            try {
                val rawKlines = repository.getKlines(symbol, interval, source)
                val candles = rawKlines.map {
                    CandleData(
                        time = (it[0] as? Number)?.toLong() ?: it[0].toString().toLongOrNull() ?: 0L,
                        open = it[1].toString().toDoubleOrNull() ?: 0.0,
                        high = it[2].toString().toDoubleOrNull() ?: 0.0,
                        low = it[3].toString().toDoubleOrNull() ?: 0.0,
                        close = it[4].toString().toDoubleOrNull() ?: 0.0,
                        volume = it[5].toString().toDoubleOrNull() ?: 0.0
                    )
                }.filter { it.time > 0 }

                if (candles.isEmpty()) return@launch

                // Calculate Bollinger Bands
                val period = 20
                val multiplier = 2.0
                val bbUpper = mutableListOf<Pair<Long, Double>>()
                val bbMiddle = mutableListOf<Pair<Long, Double>>()
                val bbLower = mutableListOf<Pair<Long, Double>>()

                for (i in candles.indices) {
                    if (i >= period - 1) {
                        val slice = candles.subList(i - period + 1, i + 1)
                        val sma = slice.sumOf { it.close } / period
                        val variance = slice.sumOf { Math.pow(it.close - sma, 2.0) } / period
                        val stdDev = sqrt(variance)
                        
                        val time = candles[i].time
                        bbMiddle.add(time to sma)
                        bbUpper.add(time to (sma + multiplier * stdDev))
                        bbLower.add(time to (sma - multiplier * stdDev))
                    }
                }

                // Calculate StochRSI (14, 14, 3, 3)
                val stochK = mutableListOf<Pair<Long, Double>>()
                val stochD = mutableListOf<Pair<Long, Double>>()
                val rsiValues = calculateRSI(candles, 14)
                
                if (rsiValues.size >= 14) {
                    val stochRSI = mutableListOf<Double>()
                    for (i in rsiValues.indices) {
                        if (i >= 13) {
                            val slice = rsiValues.subList(i - 13, i + 1)
                            val low = slice.minOrNull() ?: 0.0
                            val high = slice.maxOrNull() ?: 100.0
                            val current = rsiValues[i]
                            val s = if (high - low != 0.0) (current - low) / (high - low) * 100 else 0.0
                            stochRSI.add(s)
                        } else {
                            stochRSI.add(0.0)
                        }
                    }
                    
                    // Smooth K (3)
                    val smoothK = calculateSMA(stochRSI, 3)
                    // Smooth D (3)
                    val smoothD = calculateSMA(smoothK, 3)
                    
                    for (i in smoothK.indices) {
                        val candleIndex = i + (candles.size - smoothK.size)
                        if (candleIndex >= 0) {
                            stochK.add(candleIndex.toFloat().toLong() to smoothK[i]) // Using index as "time" to align with chart
                            stochD.add(candleIndex.toFloat().toLong() to smoothD[i])
                        }
                    }
                }

                _state.value = state.value.copy(
                    candles = candles,
                    bbUpper = bbUpper,
                    bbMiddle = bbMiddle,
                    bbLower = bbLower,
                    stochK = stochK,
                    stochD = stochD
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateRSI(candles: List<CandleData>, period: Int): List<Double> {
        val rsi = mutableListOf<Double>()
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

    private fun calculateSMA(values: List<Double>, period: Int): List<Double> {
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
}

data class CryptoDetailState(
    val detail: PairDetail? = null,
    val candles: List<CandleData> = emptyList(),
    val bbUpper: List<Pair<Long, Double>> = emptyList(),
    val bbMiddle: List<Pair<Long, Double>> = emptyList(),
    val bbLower: List<Pair<Long, Double>> = emptyList(),
    val stochK: List<Pair<Long, Double>> = emptyList(),
    val stochD: List<Pair<Long, Double>> = emptyList(),
    val selectedInterval: String = "1d",
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
