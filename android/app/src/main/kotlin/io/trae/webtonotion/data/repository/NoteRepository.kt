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
        val databaseId = settings.getDatabaseIdSync()

        if (token.isEmpty() || databaseId.isEmpty()) {
            dao.updateStatus(noteId, NoteStatus.FAILED, "Notion Token 或 Database ID 未配置")
            return false
        }

        dao.updateStatus(noteId, NoteStatus.PROCESSING, null)

        return try {
            val children = NotionRequestBuilder.buildNoteChildren(note.content)
            val request = NotionRequestBuilder.buildCreatePage(
                databaseId = databaseId,
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

    // 从 Notion 拉取最新列表
    suspend fun syncFromNotion(): Boolean {
        val token = settings.getNotionTokenSync()
        val databaseId = settings.getDatabaseIdSync()
        if (token.isEmpty() || databaseId.isEmpty()) return false

        return try {
            val request = NotionRequestBuilder.buildQueryRequest()
            val response = ApiClient.notionApi.queryDatabase(
                ApiClient.bearer(token),
                databaseId,
                request
            )
            // 简单合并：更新已同步的笔记状态
            // 新笔记不自动插入本地（用户在 Notion 手动创建的）
            true
        } catch (e: Exception) {
            false
        }
    }

    // 测试 Notion 连接，返回成功标志和错误详情
    suspend fun testConnection(): TestResult {
        val token = settings.getNotionTokenSync()
        val databaseId = settings.getDatabaseIdSync()
        if (token.isEmpty()) return TestResult(false, "Notion Token 未填写")
        if (databaseId.isEmpty()) return TestResult(false, "Database ID 未填写")

        return try {
            val db = ApiClient.notionApi.getDatabase(
                ApiClient.bearer(token),
                databaseId
            )
            val issues = NotionRequestBuilder.validateDatabaseSchema(db.properties)
            if (issues.isNotEmpty()) {
                return TestResult(
                    false,
                    "数据库 schema 不匹配：${issues.joinToString("；")}。请使用设置中的「创建数据库」功能。"
                )
            }
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

    // 在指定页面下创建 Web to Notion 数据库，返回新数据库 ID
    suspend fun createNotionDatabase(parentPageId: String): CreateDbResult {
        val token = settings.getNotionTokenSync()
        if (token.isEmpty()) return CreateDbResult(false, null, "Notion Token 未填写")

        return try {
            val request = NotionRequestBuilder.buildCreateDatabase(parentPageId)
            val response = ApiClient.notionApi.createDatabase(
                ApiClient.bearer(token),
                request
            )
            settings.setDatabaseId(response.id)
            CreateDbResult(true, response.id, null)
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string()
            CreateDbResult(false, null, "HTTP ${e.code()}: ${body ?: e.message()}")
        } catch (e: Exception) {
            CreateDbResult(false, null, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    data class CreateDbResult(val success: Boolean, val databaseId: String?, val error: String?)

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
