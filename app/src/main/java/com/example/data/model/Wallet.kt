package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val email: String,
    val uniqueId: String,
    val name: String,
    val playerBalance: Double = 200.0,
    val companyBalance: Double = 0.0,
    val isVerified: Boolean = true
)
