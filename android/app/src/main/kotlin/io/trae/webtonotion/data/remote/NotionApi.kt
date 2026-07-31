package io.trae.webtonotion.data.remote

import io.trae.webtonotion.data.remote.dto.CreatePageResponse
import io.trae.webtonotion.data.remote.dto.FileUploadCreateResponse
import io.trae.webtonotion.data.remote.dto.FileUploadSendResponse
import io.trae.webtonotion.data.remote.dto.GetBlocksResponse
import io.trae.webtonotion.data.remote.dto.QueryDatabaseResponse
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface NotionApi {

    @Headers("Notion-Version: 2022-06-28")
    @POST("v1/pages")
    suspend fun createPage(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): CreatePageResponse

    @Headers("Notion-Version: 2022-06-28")
    @POST("v1/databases/{database_id}/query")
    suspend fun queryDatabase(
        @Header("Authorization") auth: String,
        @Path("database_id") databaseId: String,
        @Body body: JsonObject
    ): QueryDatabaseResponse

    @Headers("Notion-Version: 2022-06-28")
    @GET("v1/blocks/{block_id}/children")
    suspend fun getBlockChildren(
        @Header("Authorization") auth: String,
        @Path("block_id") blockId: String
    ): GetBlocksResponse

    @Headers("Notion-Version: 2022-06-28")
    @PATCH("v1/pages/{page_id}")
    suspend fun updatePage(
        @Header("Authorization") auth: String,
        @Path("page_id") pageId: String,
        @Body body: JsonObject
    ): CreatePageResponse

    @Headers("Notion-Version: 2022-06-28")
    @PATCH("v1/blocks/{block_id}/children")
    suspend fun appendBlockChildren(
        @Header("Authorization") auth: String,
        @Path("block_id") blockId: String,
        @Body body: JsonObject
    ): GetBlocksResponse

    @Headers("Notion-Version: 2022-06-28")
    @PATCH("v1/pages/{page_id}")
    suspend fun archivePage(
        @Header("Authorization") auth: String,
        @Path("page_id") pageId: String,
        @Body body: JsonObject
    ): CreatePageResponse

    @Headers("Notion-Version: 2022-06-28")
    @POST("v1/file_uploads")
    suspend fun createFileUpload(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): FileUploadCreateResponse

    @Headers("Notion-Version: 2022-06-28")
    @Multipart
    @POST("v1/file_uploads/{file_upload_id}/send")
    suspend fun sendFileUpload(
        @Header("Authorization") auth: String,
        @Path("file_upload_id") fileUploadId: String,
        @Part file: MultipartBody.Part
    ): FileUploadSendResponse
}
