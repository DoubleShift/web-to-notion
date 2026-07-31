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

    // 构建创建页面的请求体
    fun buildCreatePage(
        databaseId: String,
        title: String,
        type: String,
        url: String? = null,
        tags: List<String> = emptyList(),
        children: JsonArray = buildJsonArray {}
    ): JsonObject = buildJsonObject {
        putJsonObject("parent") { put("database_id", databaseId) }
        putJsonObject("properties") {
            // Title (title 类型)
            putJsonObject("Title") {
                put("title", buildTextRichText(title))
            }
            // Type (select)
            putJsonObject("Type") {
                putJsonObject("select") { put("name", type) }
            }
            // URL (webpage 类型填)
            if (url != null) {
                putJsonObject("URL") { put("url", url) }
            }
            // Tags (multi_select)
            if (tags.isNotEmpty()) {
                putJsonObject("Tags") {
                    put("multi_select", buildJsonArray {
                        tags.forEach { tag ->
                            add(buildJsonObject { put("name", tag) })
                        }
                    })
                }
            }
            // Created (date)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            putJsonObject("Created") {
                putJsonObject("date") {
                    put("start", today)
                }
            }
        }
        put("children", children)
    }

    // 创建数据库的请求体
    fun buildCreateDatabase(parentPageId: String): JsonObject = buildJsonObject {
        putJsonObject("parent") { put("page_id", parentPageId) }
        putJsonArray("title") {
            add(buildJsonObject {
                put("type", "text")
                putJsonObject("text") { put("content", "Web to Notion") }
            })
        }
        putJsonObject("properties") {
            putJsonObject("Title") { put("title", buildJsonObject {}) }
            putJsonObject("Type") {
                putJsonObject("select") {
                    putJsonArray("options") {
                        add(buildJsonObject {
                            put("name", "note")
                            put("color", "yellow")
                        })
                        add(buildJsonObject {
                            put("name", "webpage")
                            put("color", "blue")
                        })
                    }
                }
            }
            putJsonObject("URL") { put("url", buildJsonObject {}) }
            putJsonObject("Tags") { put("multi_select", buildJsonObject {}) }
            putJsonObject("Created") { put("date", buildJsonObject {}) }
        }
    }

    // 验证数据库属性是否包含必需的字段
    fun validateDatabaseSchema(properties: JsonObject): List<String> {
        val required = mapOf(
            "Title" to "title",
            "Type" to "select",
            "URL" to "url",
            "Tags" to "multi_select",
            "Created" to "date"
        )
        val missing = mutableListOf<String>()
        required.forEach { (name, expectedType) ->
            val prop = properties[name]
            if (prop == null) {
                missing.add("缺少属性: $name")
            } else {
                val actualType = (prop as? JsonObject)?.get("type")?.toString()?.trim('"')
                if (actualType != expectedType) {
                    missing.add("属性 $name 类型错误: 需要 $expectedType，实际 $actualType")
                }
            }
        }
        return missing
    }

    // 构建查询数据库的请求体
    fun buildQueryRequest(
        pageSize: Int = 100,
        startCursor: String? = null
    ): JsonObject = buildJsonObject {
        put("page_size", pageSize)
        if (startCursor != null) {
            put("start_cursor", startCursor)
        }
        put("sorts", buildJsonArray {
            add(buildJsonObject {
                put("property", "Created")
                put("direction", "descending")
            })
        })
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
