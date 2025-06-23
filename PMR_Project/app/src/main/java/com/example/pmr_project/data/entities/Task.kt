package com.example.pmr_project.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val vehicleId: String,
    var status: TaskStatus = TaskStatus.PENDING,
    val assignedTo: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PAUSED
} 