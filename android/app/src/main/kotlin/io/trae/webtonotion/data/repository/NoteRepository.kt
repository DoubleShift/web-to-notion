package io.trae.webtonotion.data.repository

import android.content.Context
import io.trae.webtonotion.data.local.AppDatabase
import io.trae.webtonotion.data.local.NoteDao
import io.trae.webtonotion.data.local.NoteEntity
import io.trae.webtonotion.data.local.NoteStatus
import io.trae.webtonotion.data.local.NoteType
import io.trae.webtonotion.data.prefs.SettingsStore
import io.trae.webtonotion.data.remote.ApiClient
import io.trae.webtonotion.data.remote.NotionRequestBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NoteRepository(
    private val dao: NoteDao,
    private val settings: SettingsStore
) {
    fun observeAll(): Flow<List<NoteEntity>> = dao.observeAll()
    fun observeById(id: Long): Flow<NoteEntity?> = dao.observeById(id)

    suspend fun createNote(
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        isPinned: Boolean = false
    ): Long {
        val note = NoteEntity(
            type = NoteType.NOTE,
            title = title,
            content = content,
            tags = tags.joinToString(","),
            status = NoteStatus.PENDING,
            isPinned = isPinned
        )
        return dao.insert(note)
    }

    suspend fun createWebpage(url: String, title: String = url): Long {
        val note = NoteEntity(
            type = NoteType.WEBPAGE,
            title = title,
            url = url,
            status = NoteStatus.PENDING
        )
        return dao.insert(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        dao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: Long) {
        val note = dao.getById(id)
        if (note != null) {
            // 如果已同步到 Notion，先归档
            val token = settings.getNotionTokenSync()
            if (token.isNotEmpty() && note.notionPageId != null) {
                try {
                    ApiClient.notionApi.archivePage(
                        ApiClient.bearer(token),
                        note.notionPageId,
                        NotionRequestBuilder.buildArchiveRequest()
                    )
                } catch (_: Exception) {
                    // Notion 归档失败不阻塞本地删除
                }
            }
            dao.deleteById(id)
        }
    }

    // 同步便签到 Notion（由 Worker 调用）
    suspend fun syncNote(noteId: Long): Boolean {
        val note = dao.getById(noteId) ?: return false
        val token = settings.getNotionTokenSync()
        val parentPageId = settings.getParentPageIdSync()

        if (token.isEmpty() || parentPageId.isEmpty()) {
            dao.updateStatus(noteId, NoteStatus.FAILED, "Notion Token 或父页面 ID 未配置")
            return false
        }

        dao.updateStatus(noteId, NoteStatus.PROCESSING, null)

        return try {
            val children = NotionRequestBuilder.buildNoteChildren(note.content)
            val request = NotionRequestBuilder.buildCreatePage(
                parentPageId = parentPageId,
                title = note.title,
                type = note.type,
                url = note.url,
                tags = note.tags.split(",").filter { it.isNotBlank() },
                children = children
            )

            val response = ApiClient.notionApi.createPage(
                ApiClient.bearer(token),
                request
            )
            dao.updateNotionPageId(noteId, response.id, NoteStatus.SUCCESS)
            true
        } catch (e: Exception) {
            dao.updateStatus(noteId, NoteStatus.FAILED, e.message ?: "Unknown error")
            false
        }
    }

    // 从 Notion 拉取父页面下的子页面到本地
    suspend fun syncFromNotion(): Pair<Int, List<String>> {
        val token = settings.getNotionTokenSync()
        val parentPageId = settings.getParentPageIdSync()
        if (token.isEmpty()) return Pair(0, listOf("Notion Token 未填写"))
        if (parentPageId.isEmpty()) return Pair(0, listOf("父页面 ID 未填写"))

        val errors = mutableListOf<String>()
        var syncedCount = 0
        var cursor: String? = null

        do {
            val queryBody = NotionRequestBuilder.buildBlockChildrenQuery(100, cursor)
            val response = try {
                ApiClient.notionApi.queryBlockChildren(
                    ApiClient.bearer(token),
                    parentPageId,
                    queryBody
                )
            } catch (e: Exception) {
                errors.add("查询子页面失败: ${e.message}")
                break
            }

            for (block in response.results) {
                try {
                    val type = block["type"]?.jsonPrimitive?.content ?: continue
                    if (type != "child_page") continue

                    val blockId = block["id"]?.jsonPrimitive?.content ?: continue
                    val childPage = block["child_page"]?.jsonObject ?: continue
                    val rawTitle = childPage["title"]?.jsonPrimitive?.content ?: "无标题"

                    // 获取 page 详情获取更完整信息
                    val page = try {
                        ApiClient.notionApi.getPage(ApiClient.bearer(token), blockId)
                    } catch (e: Exception) {
                        errors.add("获取页面 $blockId 失败: ${e.message}")
                        continue
                    }

                    val title = page.title ?: rawTitle
                    val content = parsePageBlocks(token, page.id)

                    // 查找是否已存在
                    val existing = dao.getByNotionPageId(page.id)
                    if (existing != null) {
                        if (existing.title == title && existing.content == content) {
                            continue
                        }
                        dao.update(existing.copy(
                            title = title,
                            content = content,
                            status = NoteStatus.SUCCESS,
                            notionPageId = page.id,
                            updatedAt = System.currentTimeMillis()
                        ))
                    } else {
                        val note = NoteEntity(
                            notionPageId = page.id,
                            type = NoteType.NOTE,
                            title = title,
                            content = content,
                            status = NoteStatus.SUCCESS,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.insertOrReplace(note)
                    }
                    syncedCount++
                } catch (e: Exception) {
                    errors.add("处理 block 失败: ${e.message}")
                }
            }

            cursor = response.next_cursor
        } while (cursor != null && response.has_more)

        return Pair(syncedCount, errors)
    }

    /**
     * 从页面的 children blocks 中提取 Markdown 文本
     */
    private suspend fun parsePageBlocks(token: String, pageId: String): String {
        val sb = StringBuilder()
        var cursor: String? = null

        do {
            val queryBody = NotionRequestBuilder.buildBlockChildrenQuery(100, cursor)
            val resp = try {
                ApiClient.notionApi.queryBlockChildren(ApiClient.bearer(token), pageId, queryBody)
            } catch (e: Exception) {
                break
            }

            for (block in resp.results) {
                val type = block["type"]?.jsonPrimitive?.content ?: continue
                val blockContent = getBlockRichText(block, type)
                if (blockContent.isNotBlank()) {
                    val prefix = when (type) {
                        "heading_1" -> "# "
                        "heading_2" -> "## "
                        "heading_3" -> "### "
                        "callout" -> "> "
                        "bulleted_list_item", "numbered_list_item", "to_do", "toggle" -> "- "
                        else -> ""
                    }
                    val suffix = if (type in listOf("paragraph", "heading_1", "heading_2", "heading_3", "callout")) "\n\n" else "\n"
                    sb.append(prefix).append(blockContent).append(suffix)
                }
                if (type == "divider") sb.append("---\n\n")
            }
            cursor = resp.next_cursor
        } while (cursor != null && resp.has_more)

        return sb.toString().trim()
    }

    /**
     * 从 JsonObject block 中提取 rich_text 内容
     */
    private fun getBlockRichText(block: JsonObject, type: String): String {
        return try {
            val typeBlock = block[type]?.jsonObject ?: return ""
            val richTextArray = typeBlock["rich_text"]?.jsonArray ?: return ""
            val sb = StringBuilder()
            for (item in richTextArray) {
                val obj = item.jsonObject
                val plainText = obj["plain_text"]?.jsonPrimitive?.content ?: ""
                if (plainText.isNotBlank()) {
                    sb.append(plainText)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    // 测试 Notion 连接，验证父页面可访问
    suspend fun testConnection(): TestResult {
        val token = settings.getNotionTokenSync()
        val parentPageId = settings.getParentPageIdSync()
        if (token.isEmpty()) return TestResult(false, "Notion Token 未填写")
        if (parentPageId.isEmpty()) return TestResult(false, "父页面 ID 未填写")

        return try {
            ApiClient.notionApi.getPage(
                ApiClient.bearer(token),
                parentPageId
            )
            TestResult(true, null)
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string()
            TestResult(false, "HTTP ${e.code()}: ${body ?: e.message()}")
        } catch (e: java.net.UnknownHostException) {
            TestResult(false, "无法连接到 api.notion.com（网络问题）")
        } catch (e: java.net.SocketTimeoutException) {
            TestResult(false, "连接超时")
        } catch (e: Exception) {
            TestResult(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // 重新同步失败的笔记
    suspend fun retrySync(noteId: Long) {
        dao.updateStatus(noteId, NoteStatus.PENDING, null)
        // Worker 需要重新入队，由调用方处理
    }

    data class TestResult(val success: Boolean, val error: String?)

    suspend fun getPendingNotes(): List<NoteEntity> = dao.getPendingNotes()

    companion object {
        @Volatile
        private var INSTANCE: NoteRepository? = null

        fun getInstance(context: Context): NoteRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteRepository(
                    dao = AppDatabase.getInstance(context).noteDao(),
                    settings = SettingsStore(context)
                ).also { INSTANCE = it }
            }
        }
    }
}
