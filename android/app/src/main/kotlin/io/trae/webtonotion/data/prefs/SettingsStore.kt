package io.trae.webtonotion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// AI 格式化模式
object AiMode {
    const val RAW = "raw"            // 不走 AI
    const val CLEAN = "clean"        // 整理格式（默认）
    const val SUMMARIZE = "summarize" // 生成摘要 + 整理
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val NOTION_TOKEN = stringPreferencesKey("notion_token")
        private val PARENT_PAGE_ID = stringPreferencesKey("parent_page_id")
        private val GROQ_KEY = stringPreferencesKey("groq_key")
        private val GROQ_ENABLED = booleanPreferencesKey("groq_enabled")
        private val AI_MODE = stringPreferencesKey("ai_mode")
    }

    val notionToken: Flow<String> = context.dataStore.data.map { it[NOTION_TOKEN] ?: "" }
    val parentPageId: Flow<String> = context.dataStore.data.map { it[PARENT_PAGE_ID] ?: "" }
    val groqKey: Flow<String> = context.dataStore.data.map { it[GROQ_KEY] ?: "" }
    val groqEnabled: Flow<Boolean> = context.dataStore.data.map { it[GROQ_ENABLED] ?: false }
    val aiMode: Flow<String> = context.dataStore.data.map { it[AI_MODE] ?: AiMode.CLEAN }

    suspend fun setNotionToken(value: String) {
        context.dataStore.edit { it[NOTION_TOKEN] = value }
    }

    suspend fun setParentPageId(value: String) {
        context.dataStore.edit { it[PARENT_PAGE_ID] = value }
    }

    suspend fun setGroqKey(value: String) {
        context.dataStore.edit { it[GROQ_KEY] = value }
    }

    suspend fun setGroqEnabled(value: Boolean) {
        context.dataStore.edit { it[GROQ_ENABLED] = value }
    }

    suspend fun setAiMode(value: String) {
        context.dataStore.edit { it[AI_MODE] = value }
    }

    // 同步读取（用于 Worker 等非 Compose 场景）
    suspend fun getNotionTokenSync(): String =
        context.dataStore.data.first()[NOTION_TOKEN] ?: ""

    suspend fun getParentPageIdSync(): String =
        context.dataStore.data.first()[PARENT_PAGE_ID] ?: ""

    suspend fun getGroqKeySync(): String =
        context.dataStore.data.first()[GROQ_KEY] ?: ""

    suspend fun getGroqEnabledSync(): Boolean =
        context.dataStore.data.first()[GROQ_ENABLED] ?: false

    suspend fun getAiModeSync(): String =
        context.dataStore.data.first()[AI_MODE] ?: AiMode.CLEAN
}
