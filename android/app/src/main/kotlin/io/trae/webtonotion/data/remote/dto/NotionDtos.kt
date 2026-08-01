package io.trae.webtonotion.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
@Serializable
data class CreatePageResponse(
    val id: String,
    val url: String,
    val properties: JsonObject? = null
) {
    val title: String? get() = properties?.get("title")
        ?.jsonObject?.get("title")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("plain_text")
        ?.jsonPrimitive
        ?.content
}

@Serializable
data class GetBlocksResponse(
    val results: List<JsonObject>,
    val has_more: Boolean = false,
    val next_cursor: String? = null
)

@Serializable
data class FileUploadCreateResponse(
    val id: String,
    val status: String
)

@Serializable
data class FileUploadSendResponse(
    val id: String,
    val status: String
)

@Serializable
data class NotionError(
    val status: Int = 0,
    val code: String? = null,
    val message: String? = null
)
