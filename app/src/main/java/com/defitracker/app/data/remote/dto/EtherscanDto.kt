package com.defitracker.app.data.remote.dto

data class EtherscanResponse(
    val status: String,
    val message: String,
    val result: List<EtherscanTransactionDto>
)

data class EtherscanTransactionDto(
    val hash: String,
    val from: String,
    val to: String,
    val value: String,
    val timeStamp: String,
    val tokenSymbol: String?,
    val tokenDecimal: String?,
    val tokenName: String?,
    val input: String?,
    val symbol: String?,
    val decimals: String?,
    val network: String? = null
)
