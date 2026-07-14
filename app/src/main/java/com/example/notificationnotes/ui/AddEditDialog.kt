package com.example.notificationnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notificationnotes.data.NoteEntity
import com.example.notificationnotes.ui.theme.*

@Composable
fun AddEditDialog(
    editingNote: NoteEntity?,
    onSave: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember(editingNote) { mutableStateOf(editingNote?.content ?: "") }
    var isSticky by remember(editingNote) { mutableStateOf(editingNote?.isSticky ?: true) }
    var contentError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题
                Text(
                    text = if (editingNote != null) "编辑便签" else "新建便签",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NearBlack
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 文本输入
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        contentError = false
                    },
                    placeholder = {
                        Text("输入提醒内容…", color = DarkGray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MediumGray,
                        focusedContainerColor = OffWhite,
                        unfocusedContainerColor = OffWhite,
                        cursorColor = PrimaryBlue
                    ),
                    isError = contentError,
                    supportingText = if (contentError) {
                        { Text("内容不能为空") }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sticky 开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "📌 固定在通知栏",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = NearBlack
                        )
                        Text(
                            "不会在清理通知时被清除",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkGray
                        )
                    }
                    Switch(
                        checked = isSticky,
                        onCheckedChange = { isSticky = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = White,
                            uncheckedTrackColor = MediumGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 说明文字
                Surface(
                    color = OffWhite,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSticky) {
                            "固定后，关机重启也不会丢失通知内容"
                        } else {
                            "普通模式，通知可以被系统清除"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkGray,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = DarkGray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (content.isBlank()) {
                                contentError = true
                            } else {
                                onSave(content.trim(), isSticky)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (editingNote != null) "保存" else "添加",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
