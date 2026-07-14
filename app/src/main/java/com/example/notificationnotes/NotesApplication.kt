package com.example.notificationnotes

import android.app.Application
import com.example.notificationnotes.data.NoteDatabase
import com.example.notificationnotes.data.NoteRepository

class NotesApplication : Application() {

    val database by lazy { NoteDatabase.getInstance(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NotesApplication
            private set
    }
}
