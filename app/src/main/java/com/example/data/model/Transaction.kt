package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "ADD_FUNDS", "WITHDRAW", "WIN", "LOSS", "DRAW", "TRANSFER_SENT", "TRANSFER_RECEIVED"
    val betAmount: Double,
    val playerProfit: Double,
    val companyProfit: Double,
    val opponent: String,
    val boardData: String,
    val playerEmail: String = "guest@gmail.com"
)
