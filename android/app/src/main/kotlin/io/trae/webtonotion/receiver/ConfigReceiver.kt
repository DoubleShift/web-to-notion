package io.trae.webtonotion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.trae.webtonotion.data.prefs.SettingsStore
import kotlinx.coroutines.runBlocking

/**
 * 通过 adb 广播自动配置 Notion Token 和 Parent Page ID。
 *
 * 用法：
 *   adb shell am broadcast -a io.trae.webtonotion.SET_CONFIG \
 *     --es notion_token "secret_xxx" \
 *     --es parent_page_id "xxxxxxxx" \
 *     -n io.trae.webtonotion/.receiver.ConfigReceiver
 */
class ConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra("notion_token")?.trim().orEmpty()
        val parentPageId = intent.getStringExtra("parent_page_id")?.trim().orEmpty()

        if (token.isBlank() || parentPageId.isBlank()) {
            Toast.makeText(context, "缺少 notion_token 或 parent_page_id", Toast.LENGTH_SHORT).show()
            return
        }

        val store = SettingsStore(context)
        runBlocking {
            store.setNotionToken(token)
            store.setParentPageId(parentPageId)
        }

        Toast.makeText(context, "配置已写入", Toast.LENGTH_SHORT).show()
    }
}
