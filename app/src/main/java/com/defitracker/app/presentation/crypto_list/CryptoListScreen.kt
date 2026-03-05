package com.defitracker.app.presentation.crypto_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import com.defitracker.app.presentation.crypto_list.components.CryptoPairItem

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CryptoListScreen(
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: CryptoListViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredSymbols = remember(searchQuery, state.availableSymbols) {
        if (searchQuery.isEmpty()) emptyList()
        else state.availableSymbols.filter { 
            it.first.contains(searchQuery, ignoreCase = true) || 
            it.second.contains(searchQuery, ignoreCase = true) 
        }.take(10)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LaunchedEffect(isSearchMode) {
            if (isSearchMode) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Pairs",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { isSearchMode = !isSearchMode }) {
                        Icon(
                            imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }

                if (isSearchMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search symbol (e.g. BTC)...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF1ECB81),
                            focusedBorderColor = Color(0xFF1ECB81),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (state.pairs.isEmpty() && !isSearchMode) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp), 
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No pairs tracked yet", color = Color.Gray)
                            Text("Use search to add your first pair", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.pairs) { pair ->
                                CryptoPairItem(
                                    pair = pair,
                                    onClick = { onNavigateToDetail(pair.symbol, pair.source) },
                                    onDelete = { viewModel.onRemovePair(pair.symbol) }
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                            }
                        }
                    }
                }
            }

                // --- MODIFIED: Search Results Dropdown ---
                if (isSearchMode && searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 135.dp) // Positioned below the search field
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1D23))
                            .zIndex(10f)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredSymbols) { (symbol, base) ->
                                ListItem(
                                    headlineContent = { Text(symbol, color = Color.White, fontSize = 14.sp) },
                                    supportingContent = { Text(base, color = Color.Gray, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.onAddPair(symbol, base, "Binance")
                                            isSearchMode = false
                                            searchQuery = ""
                                        }
                                        .background(Color.Transparent),
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                Divider(color = Color.Gray.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPairDialog(
    availableSymbols: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("Binance") }
    
    val filteredSymbols = remember(searchQuery, availableSymbols) {
        if (searchQuery.length < 1) emptyList()
        else availableSymbols.filter { 
            it.first.contains(searchQuery, ignoreCase = true) || 
            it.second.contains(searchQuery, ignoreCase = true) 
        }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Crypto Pair") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Symbol (e.g. BTC)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                
                if (filteredSymbols.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            filteredSymbols.forEach { (symbol, base) ->
                                ListItem(
                                    headlineContent = { Text(symbol) },
                                    supportingContent = { Text(base) },
                                    modifier = Modifier.clickable {
                                        onConfirm(symbol, base, source)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Source:")
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = source == "Binance", onClick = { source = "Binance" })
                    Text("Binance")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = source == "HTX", onClick = { source = "HTX" })
                    Text("HTX")
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
