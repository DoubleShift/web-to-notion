package io.trae.webtonotion.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CreatePageResponse(
    val id: String,
    val url: String
)

@Serializable
data class QueryDatabaseResponse(
    val results: List<PageResult>,
    val has_more: Boolean = false,
    val next_cursor: String? = null
)

@Serializable
data class PageResult(
    val id: String,
    val url: String,
    val properties: JsonObject
)

@Serializable
data class GetBlocksResponse(
    val results: List<JsonObject>,
    val has_more: Boolean = false
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
