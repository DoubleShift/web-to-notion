package io.trae.webtonotion.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 笔记类型
object NoteType {
    const val NOTE = "note"        // 便签
    const val WEBPAGE = "webpage"  // URL 保存
}

// 笔记状态
object NoteStatus {
    const val DRAFT = "draft"          // 本地草稿，未提交
    const val PENDING = "pending"      // 已入队，等待处理
    const val PROCESSING = "processing" // 处理中
    const val SUCCESS = "success"      // 成功
    const val FAILED = "failed"        // 失败
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notionPageId: String? = null,
    val type: String = NoteType.NOTE,
    val title: String,
    val content: String = "",
    val url: String? = null,
    val tags: String = "",           // 逗号分隔
    val status: String = NoteStatus.DRAFT,
    val error: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
