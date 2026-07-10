package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.Wallet
import com.example.data.model.TransactionLog
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class GameRepository(private val appDao: AppDao) {

    fun getWalletFlow(email: String): Flow<Wallet?> = appDao.getWalletFlow(email)
    fun getTransactionsFlowForEmail(email: String): Flow<List<TransactionLog>> = appDao.getTransactionsFlowForEmail(email)

    fun generateUniqueIdForEmail(email: String): String {
        val cleanPrefix = email.substringBefore("@").replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
        val croppedPrefix = if (cleanPrefix.length > 6) cleanPrefix.substring(0, 6) else cleanPrefix
        val suffix = Random.nextInt(1000, 9999)
        return "BETTAC-$croppedPrefix-$suffix"
    }

    suspend fun getOrCreateWallet(
        email: String,
        name: String? = null,
        uniqueId: String? = null
    ): Wallet {
        val wallet = appDao.getWallet(email)
        if (wallet == null) {
            val resolvedName = name ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val resolvedUniqueId = uniqueId ?: generateUniqueIdForEmail(email)
            val newWallet = Wallet(
                email = email,
                uniqueId = resolvedUniqueId,
                name = resolvedName,
                playerBalance = 200.0,
                companyBalance = 0.0
            )
            appDao.updateWallet(newWallet)
            return newWallet
        }
        return wallet
    }

    suspend fun addFunds(email: String, amount: Double) {
        val wallet = getOrCreateWallet(email)
        val updated = wallet.copy(playerBalance = wallet.playerBalance + amount)
        appDao.updateWallet(updated)
        
        // Log transaction
        appDao.insertTransaction(
            TransactionLog(
                type = if (amount >= 0.0) "ADD_FUNDS" else "WITHDRAW",
                betAmount = 0.0,
                playerProfit = amount,
                companyProfit = 0.0,
                opponent = "SYSTEM",
                boardData = "",
                playerEmail = email
            )
        )
    }

    suspend fun transferCoins(senderEmail: String, recipientUniqueId: String, amount: Double): String {
        val trimmedId = recipientUniqueId.trim()
        val senderWallet = getOrCreateWallet(senderEmail)
        
        if (senderWallet.playerBalance < amount) {
            return "INSUFFICIENT_FUNDS"
        }
        
        // Find recipient or dynamic register
        var recipientWallet = appDao.getWalletByUniqueId(trimmedId)
        if (recipientWallet == null) {
            // Dynamic registration so the transfer succeeds cleanly
            val parts = trimmedId.split("-")
            val rawName = if (parts.size >= 2) parts[1] else "Gamer"
            val cleanName = rawName.lowercase().replaceFirstChar { it.uppercase() }
            val resolvedEmail = "${rawName.lowercase()}@gmail.com"
            recipientWallet = Wallet(
                email = resolvedEmail,
                uniqueId = trimmedId,
                name = cleanName,
                playerBalance = 0.0,
                companyBalance = 0.0
            )
            appDao.updateWallet(recipientWallet)
        }

        if (recipientWallet.email == senderEmail) {
            return "CANNOT_SEND_TO_SELF"
        }

        // Execute dynamic balances swap
        val updatedSender = senderWallet.copy(playerBalance = senderWallet.playerBalance - amount)
        val updatedRecipient = recipientWallet.copy(playerBalance = recipientWallet.playerBalance + amount)

        appDao.updateWallet(updatedSender)
        appDao.updateWallet(updatedRecipient)

        // Log transaction for sender
        appDao.insertTransaction(
            TransactionLog(
                type = "TRANSFER_SENT",
                betAmount = 0.0,
                playerProfit = -amount,
                companyProfit = 0.0,
                opponent = "Sent to: ${recipientWallet.name} (${recipientWallet.uniqueId})",
                boardData = "",
                playerEmail = senderEmail
            )
        )

        // Log transaction for recipient
        appDao.insertTransaction(
            TransactionLog(
                type = "TRANSFER_RECEIVED",
                betAmount = 0.0,
                playerProfit = amount,
                companyProfit = 0.0,
                opponent = "Received from: ${senderWallet.name} (${senderWallet.uniqueId})",
                boardData = "",
                playerEmail = recipientWallet.email
            )
        )

        return "SUCCESS"
    }

    suspend fun recordMatch(
        email: String,
        betAmount: Double,
        winner: String, // "PLAYER", "OPPONENT", "DRAW"
        opponentType: String, // "Bot", "Local Player"
        boardDataString: String
    ) {
        val wallet = getOrCreateWallet(email)
        
        var playerProfit = 0.0
        var companyProfit = 0.0
        val commissionRate = 0.20 // 20% commission (₹2 out of every ₹10 total pot)

        if (opponentType == "Local Player") {
            playerProfit = 0.0
            companyProfit = 0.0
        } else {
            when (winner) {
                "PLAYER" -> {
                    if (betAmount == 0.0) {
                        playerProfit = 0.50
                        companyProfit = 0.0
                    } else {
                        val payout = betAmount * 2.0 * (1.0 - commissionRate)
                        playerProfit = payout
                        companyProfit = betAmount * 2.0 * commissionRate
                    }
                }
                "OPPONENT" -> {
                    playerProfit = 0.0
                    if (opponentType.startsWith("Bot")) {
                        companyProfit = betAmount * 2.0
                    } else {
                        companyProfit = betAmount * 2.0 * commissionRate
                    }
                }
                "DRAW" -> {
                    playerProfit = betAmount
                    companyProfit = 0.0
                }
            }
        }

        val updatedWallet = wallet.copy(
            playerBalance = wallet.playerBalance + playerProfit,
            companyBalance = wallet.companyBalance + companyProfit
        )
        appDao.updateWallet(updatedWallet)

        val logType = when(winner) {
            "PLAYER" -> "WIN"
            "OPPONENT" -> "LOSS"
            else -> "DRAW"
        }

        appDao.insertTransaction(
            TransactionLog(
                type = logType,
                betAmount = betAmount,
                playerProfit = if (winner == "PLAYER") playerProfit - betAmount else if (winner == "DRAW") 0.0 else -betAmount,
                companyProfit = companyProfit,
                opponent = opponentType,
                boardData = boardDataString,
                playerEmail = email
            )
        )
    }

    suspend fun deductWager(email: String, betAmount: Double) {
        val wallet = getOrCreateWallet(email)
        val updated = wallet.copy(playerBalance = wallet.playerBalance - betAmount)
        appDao.updateWallet(updated)
    }

    suspend fun clearHistory() {
        appDao.clearHistory()
    }
}
