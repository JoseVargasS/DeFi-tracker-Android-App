package com.defitracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MoralisTransactionsResponse(
    val result: List<MoralisTxDto>? = null,
    val cursor: String? = null,
    val page: String? = null,
    val page_size: String? = null
)

data class MoralisTxDto(
    val hash: String,
    @SerializedName("from_address") val fromAddress: String,
    @SerializedName("to_address") val toAddress: String,
    val value: Any? = null, // Gson devuelve Number; lo convertimos a string en el repo
    @SerializedName("block_timestamp") val blockTimestamp: String?
)
