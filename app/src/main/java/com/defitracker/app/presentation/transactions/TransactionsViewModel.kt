package com.defitracker.app.presentation.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defitracker.app.data.local.WalletEntity
import com.defitracker.app.data.remote.dto.EtherscanTransactionDto
import com.defitracker.app.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsState(
    val wallets: List<WalletEntity> = emptyList(),
    val selectedAddress: String = "",
    val transactions: List<EtherscanTransactionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _state = mutableStateOf(TransactionsState())
    val state: State<TransactionsState> = _state

    init {
        getSavedWallets()
    }

    private fun getSavedWallets() {
        repository.getSavedWallets()
            .onEach { wallets ->
                _state.value = _state.value.copy(wallets = wallets)
                if (wallets.isNotEmpty() && _state.value.selectedAddress.isEmpty()) {
                    onAddressSelected(wallets.first().address)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAddressSelected(address: String) {
        _state.value = _state.value.copy(selectedAddress = address)
        fetchTransactions(address)
    }

    fun fetchTransactions(address: String) {
        if (address.isEmpty()) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, transactions = emptyList())
            try {
                val txs = repository.getWalletTransactions(address)
                _state.value = _state.value.copy(transactions = txs, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
