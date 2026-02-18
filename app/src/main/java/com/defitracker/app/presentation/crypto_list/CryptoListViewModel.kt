package com.defitracker.app.presentation.crypto_list

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.defitracker.app.domain.model.CryptoPair
import com.defitracker.app.domain.repository.CryptoRepository
import com.defitracker.app.widget.CryptoWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CryptoListViewModel @Inject constructor(
    private val repository: CryptoRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _state = mutableStateOf(CryptoListState())
    val state: State<CryptoListState> = _state

    private var getPairsJob: Job? = null
    private var refreshJob: Job? = null

    init {
        getTrackedPairs()
        startPriceUpdates()
        loadAvailableSymbols()
    }

    private fun loadAvailableSymbols() {
        viewModelScope.launch {
            val symbols = repository.getAvailableSymbols()
            _state.value = state.value.copy(availableSymbols = symbols)
        }
    }

    private fun getTrackedPairs() {
        getPairsJob?.cancel()
        getPairsJob = repository.getTrackedPairs()
            .onEach { pairs ->
                _state.value = state.value.copy(
                    pairs = pairs
                )
                refreshPrices()
            }
            .launchIn(viewModelScope)
    }

    private fun startPriceUpdates() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                refreshPrices()
                updateWidget()
                delay(2000)
            }
        }
    }

    private suspend fun refreshPrices() {
        val updatedPairs = state.value.pairs.map { pair ->
            try {
                val detail = repository.getPairDetail(pair.symbol, pair.source)
                pair.copy(
                    price = detail.price,
                    priceChangePercent = detail.priceChangePercent,
                    isPositive = detail.isPositive
                )
            } catch (e: Exception) {
                pair
            }
        }
        _state.value = state.value.copy(pairs = updatedPairs)
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                CryptoWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onAddPair(symbol: String, baseAsset: String, source: String) {
        viewModelScope.launch {
            repository.addTrackedPair(symbol, baseAsset, source)
        }
    }

    fun onRemovePair(symbol: String) {
        viewModelScope.launch {
            repository.removeTrackedPair(symbol)
        }
    }
}

data class CryptoListState(
    val pairs: List<CryptoPair> = emptyList(),
    val availableSymbols: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String = ""
)
