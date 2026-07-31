package io.trae.webtonotion

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.trae.webtonotion.data.local.NoteType
import io.trae.webtonotion.data.prefs.SettingsStore
import io.trae.webtonotion.data.repository.NoteRepository
import io.trae.webtonotion.util.UrlExtractor
import io.trae.webtonotion.work.SaveNoteWorker
import kotlinx.coroutines.launch

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val url = UrlExtractor.extract(text)
        val repository = NoteRepository.getInstance(applicationContext)
        val settings = SettingsStore(applicationContext)

        lifecycleScope.launch {
            val token = settings.getNotionTokenSync()
            val databaseId = settings.getDatabaseIdSync()

            if (token.isEmpty() || databaseId.isEmpty()) {
                // 未配置 → 跳转到主界面设置
                val intent = Intent(this@ShareActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                Toast.makeText(this@ShareActivity, "请先配置 Notion Token", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            if (url != null) {
                // URL → webpage 类型，Phase 2 会接入 SaveUrlWorker
                val id = repository.createWebpage(url)
                Toast.makeText(this@ShareActivity, "已加入队列", Toast.LENGTH_SHORT).show()
                finish()
            } else if (text.isNotBlank()) {
                // 文本 → 便签，立即同步
                val id = repository.createNote(
                    title = text.take(50),
                    content = text
                )
                SaveNoteWorker.enqueue(this@ShareActivity, id)
                Toast.makeText(this@ShareActivity, "已保存", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@ShareActivity, "无法识别内容", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
