package com.example.notificationnotes.data

import kotlinx.coroutines.flow.Flow

/**
 * 便签数据仓库
 * 统一管理数据库操作，作为 ViewModel 和 DAO 之间的桥梁
 */
class NoteRepository(private val dao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()

    fun getHiddenNotes(): Flow<List<NoteEntity>> = dao.getHiddenNotes()

    suspend fun getAllNotesList(): List<NoteEntity> = dao.getAllNotesList()

    suspend fun getVisibleNotes(): List<NoteEntity> = dao.getVisibleNotes()

    suspend fun getStickyNotes(): List<NoteEntity> = dao.getStickyNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)

    suspend fun updateHiddenStatus(id: Long, hidden: Boolean) = dao.updateHiddenStatus(id, hidden)

    suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = dao.deleteNoteById(id)

    suspend fun deleteAllHiddenNotes() = dao.deleteAllHiddenNotes()

    suspend fun getNextNotificationId(): Int = dao.getNextNotificationId()
}
