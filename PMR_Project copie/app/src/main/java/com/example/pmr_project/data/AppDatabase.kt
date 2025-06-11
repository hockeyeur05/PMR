package com.example.pmr_project.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pmr_project.data.entities.Task
import com.example.pmr_project.data.entities.Part
import com.example.pmr_project.data.dao.TaskDao
import com.example.pmr_project.data.dao.PartDao

@Database(
    entities = [
        Task::class,
        Part::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun partDao(): PartDao

    companion object {
        const val DATABASE_NAME = "garage_assistant_db"
    }
} 