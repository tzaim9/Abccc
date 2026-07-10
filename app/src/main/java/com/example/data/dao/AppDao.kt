package com.example.data.dao

import androidx.room.*
import com.example.data.model.Wallet
import com.example.data.model.TransactionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM wallet WHERE email = :email LIMIT 1")
    fun getWalletFlow(email: String): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE email = :email LIMIT 1")
    suspend fun getWallet(email: String): Wallet?

    @Query("SELECT * FROM wallet WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getWalletByUniqueId(uniqueId: String): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWallet(wallet: Wallet)

    @Query("SELECT * FROM transactions WHERE playerEmail = :email ORDER BY timestamp DESC")
    fun getTransactionsFlowForEmail(email: String): Flow<List<TransactionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionLog)

    @Query("DELETE FROM transactions")
    suspend fun clearHistory()
}
