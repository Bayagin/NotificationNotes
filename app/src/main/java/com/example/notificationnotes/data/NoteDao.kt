package com.example.notificationnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** 获取所有便签（包括隐藏的），按创建时间倒序 */
    @Query("SELECT * FROM notes ORDER BY hiddenFromNotification ASC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** 获取所有可见便签（未从通知栏删除的） */
    @Query("SELECT * FROM notes WHERE hiddenFromNotification = 0 ORDER BY createdAt DESC")
    suspend fun getVisibleNotes(): List<NoteEntity>

    /** 获取所有隐藏便签（从通知栏删除但保留数据） */
    @Query("SELECT * FROM notes WHERE hiddenFromNotification = 1 ORDER BY createdAt DESC")
    fun getHiddenNotes(): Flow<List<NoteEntity>>

    /** 获取所有 sticky 便签（用于开机恢复） */
    @Query("SELECT * FROM notes WHERE isSticky = 1 AND hiddenFromNotification = 0 ORDER BY createdAt DESC")
    suspend fun getStickyNotes(): List<NoteEntity>

    /** 根据 ID 获取便签 */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    /** 插入便签，返回生成的 ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    /** 更新便签 */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /** 按 ID 更新 hiddenFromNotification 状态 */
    @Query("UPDATE notes SET hiddenFromNotification = :hidden WHERE id = :id")
    suspend fun updateHiddenStatus(id: Long, hidden: Boolean)

    /** 删除便签（永久） */
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    /** 按 ID 删除（永久） */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    /** 永久删除所有隐藏的便签 */
    @Query("DELETE FROM notes WHERE hiddenFromNotification = 1")
    suspend fun deleteAllHiddenNotes()

    /** 获取所有便签（非 Flow，用于导出） */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    /** 获取下一个可用的 notificationId */
    @Query("SELECT COALESCE(MAX(notificationId), 100) + 1 FROM notes")
    suspend fun getNextNotificationId(): Int
}
