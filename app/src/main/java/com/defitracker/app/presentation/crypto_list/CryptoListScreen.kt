package com.defitracker.app.presentation.crypto_list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyList()
        else state.availableSymbols.filter {
            it.symbol.contains(query, ignoreCase = true) ||
            it.baseAsset.contains(query, ignoreCase = true) ||
            it.quoteAsset.contains(query, ignoreCase = true) ||
            it.displayName.contains(query, ignoreCase = true)
        }.take(10)
    }

    val showSymbolError = state.availableSymbols.isEmpty() && state.symbolsError.isNotEmpty()

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
                    IconButton(onClick = {
                        isSearchMode = !isSearchMode
                        if (isSearchMode.not()) {
                            searchQuery = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isSearchMode,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search symbol (e.g. ETH/BTC)...", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
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

                        AnimatedVisibility(
                            visible = searchQuery.isNotBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                color = Color(0xFF1A1D23),
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 2.dp
                            ) {
                                if (showSymbolError) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(state.symbolsError, color = Color.Gray, fontSize = 13.sp)
                                        TextButton(onClick = { viewModel.retryLoadSymbols() }) {
                                            Text("Retry", color = Color(0xFF1ECB81), fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(
                                            items = filteredSymbols,
                                            key = { pair -> pair.symbol }
                                        ) { pair ->
                                            ListItem(
                                                headlineContent = { Text(pair.displayName, color = Color.White, fontSize = 14.sp) },
                                                supportingContent = { Text(pair.symbol, color = Color.Gray, fontSize = 11.sp) },
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.onAddPair(
                                                            symbol = pair.symbol,
                                                            baseAsset = pair.baseAsset,
                                                            quoteAsset = pair.quoteAsset,
                                                            source = "Binance"
                                                        )
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
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .animateContentSize()
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
                            items(
                                items = state.pairs,
                                key = { pair -> "${pair.symbol}-${pair.source}" }
                            ) { pair ->
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
        }
    }
}

