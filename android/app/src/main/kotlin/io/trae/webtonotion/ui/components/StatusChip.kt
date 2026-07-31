package io.trae.webtonotion.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.trae.webtonotion.data.local.NoteStatus

// 华为设计规范 StatusChip：8dp 圆角，Caption_L 文本，状态色映射
@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        NoteStatus.DRAFT -> "草稿" to MaterialTheme.colorScheme.outline
        NoteStatus.PENDING -> "待处理" to MaterialTheme.colorScheme.outline
        NoteStatus.PROCESSING -> "处理中" to MaterialTheme.colorScheme.primary
        NoteStatus.SUCCESS -> "已保存" to MaterialTheme.colorScheme.tertiary
        NoteStatus.FAILED -> "失败" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// TypeChip：便签/网页 类型标识
@Composable
fun TypeChip(type: String, modifier: Modifier = Modifier) {
    val text = when (type) {
        "note" -> "便签"
        "webpage" -> "网页"
        else -> type
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
