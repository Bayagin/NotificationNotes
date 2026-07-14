package com.example.notificationnotes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.notificationnotes.data.NoteEntity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * JSON 导入导出工具
 */
object JsonExportUtil {

    private const val MIME_TYPE_JSON = "application/json"

    data class ExportData(
        val version: Int = 1,
        val exportTime: Long = System.currentTimeMillis(),
        val notes: List<NoteExportItem>
    )

    data class NoteExportItem(
        val content: String,
        val isSticky: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    /**
     * 将便签列表转换为导出 JSON 字符串
     */
    fun toJsonString(notes: List<NoteEntity>): String {
        val exportItems = notes.map { note ->
            NoteExportItem(
                content = note.content,
                isSticky = note.isSticky,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt
            )
        }
        val exportData = ExportData(notes = exportItems)
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(exportData)
    }

    /**
     * 分享导出（通过系统分享菜单）
     */
    suspend fun shareNotes(context: Context, notes: List<NoteEntity>) {
        val jsonString = toJsonString(notes)
        val tempFile = File(context.cacheDir, "notification_notes_backup.json")
        tempFile.writeText(jsonString)
        shareFile(context, tempFile)
    }

    /**
     * 从 JSON 字符串导入便签
     */
    fun parseImportJson(jsonString: String): List<NoteEntity>? {
        return try {
            val gson = GsonBuilder().create()
            val type = object : TypeToken<ExportData>() {}.type
            val exportData: ExportData = gson.fromJson(jsonString, type)
            exportData.notes.map { item ->
                NoteEntity(
                    content = item.content,
                    isSticky = item.isSticky,
                    createdAt = item.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun shareFile(context: Context, file: File) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_JSON
                putExtra(Intent.EXTRA_STREAM, getShareFileUri(context, file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "导出便签数据"))
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getShareFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
    }

    fun formatTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
