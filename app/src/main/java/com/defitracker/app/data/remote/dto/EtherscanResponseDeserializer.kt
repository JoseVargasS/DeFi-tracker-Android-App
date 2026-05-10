package com.defitracker.app.data.remote.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Etherscan (and compatible APIs) can return "result" as either:
 * - A JSON array of transactions (success)
 * - A string (e.g. "No transactions found", error message, or V1 deprecation message)
 * This deserializer avoids parse errors when result is a string.
 */
class EtherscanResponseDeserializer : JsonDeserializer<EtherscanResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): EtherscanResponse {
        val obj = json.asJsonObject
        val status = obj.get("status")?.asString ?: "0"
        val message = obj.get("message")?.asString ?: ""
        val resultElement = obj.get("result")

        var resultMessage: String? = null
        val result: List<EtherscanTransactionDto> = when {
            resultElement == null -> emptyList()
            resultElement.isJsonArray -> {
                resultElement.asJsonArray.mapNotNull { element ->
                    try {
                        context.deserialize<EtherscanTransactionDto>(element, EtherscanTransactionDto::class.java)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            else -> {
                resultMessage = resultElement.asString
                emptyList()
            }
        }

        return EtherscanResponse(status = status, message = message, result = result, resultMessage = resultMessage)
    }
}
