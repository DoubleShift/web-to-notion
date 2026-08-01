package io.trae.webtonotion.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// 构建 Notion API 请求体
object NotionRequestBuilder {

    private const val MAX_TEXT_LENGTH = 2000

    // rich_text 元素，单个 content 最多 2000 字符，超长自动分割
    fun buildTextRichText(content: String): JsonArray {
        if (content.isEmpty()) {
            return buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    putJsonObject("text") { put("content", "") }
                })
            }
        }
        return buildJsonArray {
            var remaining = content
            while (remaining.isNotEmpty()) {
                val chunk = remaining.take(MAX_TEXT_LENGTH)
                remaining = remaining.drop(MAX_TEXT_LENGTH)
                add(buildJsonObject {
                    put("type", "text")
                    putJsonObject("text") { put("content", chunk) }
                })
            }
        }
    }

    fun buildParagraph(content: String): JsonObject = buildJsonObject {
        put("type", "paragraph")
        putJsonObject("paragraph") {
            put("rich_text", buildTextRichText(content))
        }
    }

    fun buildHeading(level: Int, content: String): JsonObject = buildJsonObject {
        val key = "heading_$level"
        put("type", key)
        putJsonObject(key) {
            put("rich_text", buildTextRichText(content))
        }
    }

    fun buildDivider(): JsonObject = buildJsonObject {
        put("type", "divider")
        putJsonObject("divider") {}
    }

    fun buildCallout(content: String): JsonObject = buildJsonObject {
        put("type", "callout")
        putJsonObject("callout") {
            put("rich_text", buildTextRichText(content))
        }
    }

    fun buildImageBlock(fileUploadId: String): JsonObject = buildJsonObject {
        put("type", "image")
        putJsonObject("image") {
            put("type", "file_upload")
            putJsonObject("file_upload") { put("id", fileUploadId) }
        }
    }

    fun buildExternalImageBlock(imageUrl: String): JsonObject = buildJsonObject {
        put("type", "image")
        putJsonObject("image") {
            put("type", "external")
            putJsonObject("external") { put("url", imageUrl) }
        }
    }

    // 构建创建页面的请求体（挂在父页面下）
    fun buildCreatePage(
        parentPageId: String,
        title: String,
        type: String,
        url: String? = null,
        tags: List<String> = emptyList(),
        children: JsonArray = buildJsonArray {}
    ): JsonObject = buildJsonObject {
        putJsonObject("parent") { put("page_id", parentPageId) }
        putJsonObject("properties") {
            // Page 的标题属性 key 必须是 "title"
            put("title", buildTextRichText(title))
        }
        // 把标签、URL、类型作为页面开头的 callout / paragraph blocks
        val metaBlocks = buildJsonArray {
            if (type.isNotBlank() || tags.isNotEmpty()) {
                val meta = buildString {
                    append("类型: $type")
                    if (tags.isNotEmpty()) {
                        append("  |  标签: ${tags.joinToString(", ")}")
                    }
                }
                add(buildCallout(meta))
            }
            if (url != null) {
                add(buildParagraph("URL: $url"))
            }
            if (type.isNotBlank() || tags.isNotEmpty() || url != null) {
                add(buildDivider())
            }
        }
        put("children", buildJsonArray {
            metaBlocks.forEach { add(it) }
            children.forEach { add(it) }
        })
    }

    // 构建分页查询 block children 的参数（用于拉取父页面下的子页面）
    fun buildBlockChildrenQuery(
        pageSize: Int = 100,
        startCursor: String? = null
    ): JsonObject = buildJsonObject {
        put("page_size", pageSize)
        if (startCursor != null) {
            put("start_cursor", startCursor)
        }
    }

    // 构建归档请求体
    fun buildArchiveRequest(): JsonObject = buildJsonObject {
        put("archived", true)
    }

    // 构建文件上传创建请求 (single_part)
    fun buildFileUploadSinglePart(filename: String, contentType: String): JsonObject = buildJsonObject {
        put("mode", "single_part")
        put("filename", filename)
        put("content_type", contentType)
    }

    // 从内容文本构建便签的 children blocks（按段落分割）
    fun buildNoteChildren(content: String): JsonArray {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return buildJsonArray {
            if (lines.isEmpty()) {
                add(buildParagraph(""))
            } else {
                lines.forEach { add(buildParagraph(it)) }
            }
        }
    }
}
