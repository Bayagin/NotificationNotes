package com.example.notificationnotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 便签数据实体
 *
 * @param id 唯一标识
 * @param content 便签文本内容
 * @param isSticky 是否固定在通知栏（不可被清除）
 * @param hiddenFromNotification 是否从通知栏隐藏（通知栏删除但保留数据）
 * @param hasReminder 是否有定时提醒
 * @param reminderTimeMs 提醒时间（毫秒时间戳），null 表示不提醒
 * @param repeatMode 重复模式：NONE / DAILY / WEEKLY / MONTHLY / CUSTOM
 * @param repeatIntervalHours 自定义重复间隔（小时），仅 CUSTOM 模式有效
 * @param repeatCount 重复次数，-1 表示无限
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @param notificationId 关联的通知 ID
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isSticky: Boolean = true,
    val hiddenFromNotification: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderTimeMs: Long? = null,
    val repeatMode: String = "NONE",
    val repeatIntervalHours: Int = 0,
    val repeatCount: Int = -1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notificationId: Int = 0
) {
    companion object {
        const val REPEAT_NONE = "NONE"
        const val REPEAT_DAILY = "DAILY"
        const val REPEAT_WEEKLY = "WEEKLY"
        const val REPEAT_MONTHLY = "MONTHLY"
        const val REPEAT_CUSTOM = "CUSTOM"
    }
}
