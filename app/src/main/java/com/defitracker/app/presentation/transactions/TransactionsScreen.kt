package com.defitracker.app.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.defitracker.app.data.remote.dto.EtherscanTransactionDto
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state = viewModel.state.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16171A))
            .padding(16.dp)
    ) {
        Text(
            text = "Transaction History",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Wallet Selector
        if (state.wallets.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Gray))
                ) {
                    Text(text = if (state.selectedAddress.isEmpty()) "Select Wallet" else state.selectedAddress.take(10) + "..." + state.selectedAddress.takeLast(8))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF23262F))
                ) {
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.address, color = Color.White) },
                            onClick = {
                                viewModel.onAddressSelected(wallet.address)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0ECB81))
            }
        } else if (state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.transactions) { tx ->
                    TransactionItem(tx, state.selectedAddress)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: EtherscanTransactionDto, userAddress: String) {
    val isSent = tx.from.lowercase() == userAddress.lowercase()
    val type = if (isSent) "Sent" else "Received"
    val color = if (isSent) Color(0xFFF6465D) else Color(0xFF0ECB81)
    val icon = if (isSent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    val date = remember(tx.timeStamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(tx.timeStamp.toLong() * 1000))
    }

    // BigDecimal para wei; si viene en notación científica (ej. 5.0E18) usar Double como respaldo
    val amount = remember(tx.value, tx.tokenDecimal) {
        val dec = tx.tokenDecimal?.toIntOrNull() ?: 18
        try {
            val valueWei = try {
                BigDecimal(tx.value)
            } catch (_: Exception) {
                BigDecimal.valueOf(tx.value.toDoubleOrNull() ?: 0.0)
            }
            valueWei.divide(BigDecimal.TEN.pow(dec), 18, java.math.RoundingMode.DOWN).toDouble()
        } catch (_: Exception) {
            0.0
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF23262F), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = type, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (tx.network != null) {
                    Text(
                        text = " • ${tx.network}", 
                        color = Color(0xFF0ECB81).copy(alpha = 0.7f), 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Text(text = date, color = Color.Gray, fontSize = 12.sp)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isSent) "-" else "+"} ${String.format("%.4f", amount)} ${tx.tokenSymbol ?: "ETH"}",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Hash: ${tx.hash.take(6)}...${tx.hash.takeLast(4)}",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
