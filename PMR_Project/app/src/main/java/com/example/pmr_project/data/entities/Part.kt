package com.example.pmr_project.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "parts")
data class Part(
    @PrimaryKey
    val qrCode: String,
    val name: String,
    val description: String,
    val location: String,
    val technicalSpecs: String,
    val requiredTools: String,
    val notes: String = "",
    val quantity: Int = 0,
    val reference: String = "",
    val position: String = "",
    val torqueSpecification: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable 