package com.example.pmr_project.data.dao

import androidx.room.*
import com.example.pmr_project.data.entities.Part
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query("SELECT * FROM parts")
    fun getAllParts(): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE qrCode = :qrCode")
    suspend fun getPartByQrCode(qrCode: String): Part?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: Part)

    @Update
    suspend fun updatePart(part: Part)

    @Delete
    suspend fun deletePart(part: Part)

    @Query("UPDATE parts SET quantity = quantity + :amount WHERE qrCode = :qrCode")
    suspend fun updateQuantity(qrCode: String, amount: Int)

    @Query("SELECT * FROM parts WHERE quantity < 5")
    fun getLowStockParts(): Flow<List<Part>>
} 