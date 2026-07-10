package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.TransactionLog
import com.example.data.model.Wallet
import com.example.data.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val sharedPrefs = application.getSharedPreferences("bettac_prefs", android.content.Context.MODE_PRIVATE)

    // Gmail Login and Identity States
    private val _isGmailLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("gmail_logged_in", false))
    val isGmailLoggedIn: StateFlow<Boolean> = _isGmailLoggedIn.asStateFlow()

    private val _currentEmail = MutableStateFlow(sharedPrefs.getString("gmail_email", "guest@gmail.com") ?: "guest@gmail.com")
    val currentEmail: StateFlow<String> = _currentEmail.asStateFlow()

    private val _currentName = MutableStateFlow(sharedPrefs.getString("gmail_name", "Guest Player") ?: "Guest Player")
    val currentName: StateFlow<String> = _currentName.asStateFlow()

    private val _currentUniqueId = MutableStateFlow(sharedPrefs.getString("gmail_unique_id", "BETTAC-GUEST-0000") ?: "BETTAC-GUEST-0000")
    val currentUniqueId: StateFlow<String> = _currentUniqueId.asStateFlow()

    // Referral Progress States (Refer & Earn 5000 Coins / Queens by sending to 5 persons)
    private val _referralProgress = MutableStateFlow(sharedPrefs.getInt("referral_progress", 0))
    val referralProgress: StateFlow<Int> = _referralProgress.asStateFlow()

    private val _isReferralRewardClaimed = MutableStateFlow(sharedPrefs.getBoolean("referral_reward_claimed", false))
    val isReferralRewardClaimed: StateFlow<Boolean> = _isReferralRewardClaimed.asStateFlow()

    // Scoped DB States
    private val _wallet = MutableStateFlow<Wallet?>(null)
    val wallet: StateFlow<Wallet?> = _wallet.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionLog>>(emptyList())
    val transactions: StateFlow<List<TransactionLog>> = _transactions.asStateFlow()

    // Compatibility variables
    private val _isUserLoggedIn = MutableStateFlow(true)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _loggedInPhoneNumber = MutableStateFlow("+91 99999 99999")
    val loggedInPhoneNumber: StateFlow<String> = _loggedInPhoneNumber.asStateFlow()

    // Transfer status
    private val _transferStatus = MutableStateFlow<String?>(null)
    val transferStatus: StateFlow<String?> = _transferStatus.asStateFlow()

    private var walletCollectionJob: kotlinx.coroutines.Job? = null
    private var transactionsCollectionJob: kotlinx.coroutines.Job? = null

    // UI Interactive States
    private val _board = MutableStateFlow(List(9) { "" })
    val board: StateFlow<List<String>> = _board.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _activeBet = MutableStateFlow(5.0) // Defaults to ₹5
    val activeBet: StateFlow<Double> = _activeBet.asStateFlow()

    private val _opponent = MutableStateFlow("Bot") // "Bot" or "Local Player"
    val opponent: StateFlow<String> = _opponent.asStateFlow()

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    private val _winner = MutableStateFlow<String?>(null) // "X", "O", "Draw", null
    val winner: StateFlow<String?> = _winner.asStateFlow()

    private val _winningLine = MutableStateFlow<List<Int>?>(null)
    val winningLine: StateFlow<List<Int>?> = _winningLine.asStateFlow()

    private val _botCheatTriggered = MutableStateFlow(false)
    val botCheatTriggered: StateFlow<Boolean> = _botCheatTriggered.asStateFlow()

    private val _botStatusMessage = MutableStateFlow<String>("Select a bet to start!")
    val botStatusMessage: StateFlow<String> = _botStatusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBotThinking = MutableStateFlow(false)
    val isBotThinking: StateFlow<Boolean> = _isBotThinking.asStateFlow()

    private val _lastRollOverWins = MutableStateFlow<List<String>>(emptyList())
    val lastRollOverWins: StateFlow<List<String>> = _lastRollOverWins.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    fun toggleBlockUser(name: String) {
        val currentSet = _blockedUsers.value
        _blockedUsers.value = if (currentSet.contains(name)) currentSet - name else currentSet + name
    }

    private val _randomOpponentName = MutableStateFlow("Preeti_Pro")
    val randomOpponentName: StateFlow<String> = _randomOpponentName.asStateFlow()

    private var _currentGameStartedByPlayer: Boolean = true

    private val indianOpponentNames = listOf(
        "Karan_99", "Preeti_Pro", "Star_Rider", "Vikram_X", "Rohan_Gamer",
        "Ananya_07", "Rahul_Battle", "Divya_Star", "Amit_King", "Siddharth_Y",
        "Priya_Win", "Sunny_Gamer", "Arjun_Strike", "Neha_Clash", "Vijay_X"
    )

    // Allowed bets
    val betOptions = listOf(0.0, 2.0, 5.0, 7.0, 10.0, 20.0, 25.0, 30.0, 40.0, 50.0)

    val winningPositions = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
        listOf(0, 4, 8), listOf(2, 4, 6)                  // Diagonals
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.appDao)

        // Seed some virtual verified gamers so there are pre-existing gamer IDs to test with!
        viewModelScope.launch {
            repository.getOrCreateWallet("preeti@gmail.com", "Preeti_Pro", "BETTAC-PREETI-8412")
            repository.getOrCreateWallet("karan@gmail.com", "Karan_99", "BETTAC-KARAN-3921")
            repository.getOrCreateWallet("rohan@gmail.com", "Rohan_Gamer", "BETTAC-ROHAN-5721")
        }

        val savedEmail = sharedPrefs.getString("gmail_email", "guest@gmail.com") ?: "guest@gmail.com"
        val savedName = sharedPrefs.getString("gmail_name", "Guest Player") ?: "Guest Player"
        
        selectActiveEmail(savedEmail, savedName)
    }

    fun selectActiveEmail(email: String, name: String? = null) {
        val resolvedName = name ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
        _currentEmail.value = email
        _currentName.value = resolvedName

        sharedPrefs.edit()
            .putString("gmail_email", email)
            .putString("gmail_name", resolvedName)
            .apply()

        walletCollectionJob?.cancel()
        transactionsCollectionJob?.cancel()

        walletCollectionJob = viewModelScope.launch {
            val resolvedUniqueId = if (email == "guest@gmail.com") {
                "BETTAC-GUEST-0000"
            } else {
                sharedPrefs.getString("unique_id_$email", null) ?: run {
                    val uid = repository.generateUniqueIdForEmail(email)
                    sharedPrefs.edit().putString("unique_id_$email", uid).apply()
                    uid
                }
            }
            _currentUniqueId.value = resolvedUniqueId

            // Sync with Room DB so it exists
            val currentWallet = repository.getOrCreateWallet(email, resolvedName, resolvedUniqueId)
            sharedPrefs.edit().putFloat("last_wallet_balance", currentWallet.playerBalance.toFloat()).apply()

            repository.getWalletFlow(email).collect { w ->
                _wallet.value = w
                if (w != null) {
                    sharedPrefs.edit().putFloat("last_wallet_balance", w.playerBalance.toFloat()).apply()
                }
            }
        }

        transactionsCollectionJob = viewModelScope.launch {
            repository.getTransactionsFlowForEmail(email).collect { logs ->
                _transactions.value = logs
            }
        }
    }

    fun performGmailLogin(email: String, name: String) {
        viewModelScope.launch {
            sharedPrefs.edit()
                .putBoolean("gmail_logged_in", true)
                .putString("gmail_email", email)
                .putString("gmail_name", name)
                .apply()
            _isGmailLoggedIn.value = true
            selectActiveEmail(email, name)
            _botStatusMessage.value = "🟢 Verified & Logged in as $name ($email)!"
        }
    }

    fun performGmailLogout() {
        viewModelScope.launch {
            sharedPrefs.edit()
                .putBoolean("gmail_logged_in", false)
                .putString("gmail_email", "guest@gmail.com")
                .putString("gmail_name", "Guest Player")
                .apply()
            _isGmailLoggedIn.value = false
            selectActiveEmail("guest@gmail.com", "Guest Player")
            _botStatusMessage.value = "🔴 Logged out. Now in Guest Mode."
        }
    }

    fun transferCoins(recipientUniqueId: String, amount: Double) {
        viewModelScope.launch {
            val result = repository.transferCoins(_currentEmail.value, recipientUniqueId, amount)
            _transferStatus.value = result
            if (result == "SUCCESS") {
                _botStatusMessage.value = "💸 Sent ${amount.toInt()} Coins successfully to $recipientUniqueId!"
            } else if (result == "INSUFFICIENT_FUNDS") {
                _errorMessage.value = "⚠️ Insufficient Balance for Transfer!"
            } else if (result == "CANNOT_SEND_TO_SELF") {
                _errorMessage.value = "⚠️ You cannot transfer coins to yourself!"
            } else {
                _errorMessage.value = "⚠️ Transfer failed. Invalid recipient!"
            }
        }
    }

    fun clearTransferStatus() {
        _transferStatus.value = null
    }

    fun incrementReferralProgress() {
        val current = _referralProgress.value
        if (current < 5) {
            val updated = current + 1
            _referralProgress.value = updated
            sharedPrefs.edit().putInt("referral_progress", updated).apply()
            
            _botStatusMessage.value = "📤 Sent Referral to WhatsApp Friend ($updated/5)!"
            
            if (updated == 5 && !_isReferralRewardClaimed.value) {
                claimReferralReward()
            }
        }
    }
    
    fun claimReferralReward() {
        if (_referralProgress.value >= 5 && !_isReferralRewardClaimed.value) {
            viewModelScope.launch {
                repository.addFunds(_currentEmail.value, 5000.0)
                _isReferralRewardClaimed.value = true
                sharedPrefs.edit().putBoolean("referral_reward_claimed", true).apply()
                _botStatusMessage.value = "👑 Referral Complete! Earned 5,000 simulated Coins!"
            }
        }
    }

    fun resetReferralProgress() {
        _referralProgress.value = 0
        _isReferralRewardClaimed.value = false
        sharedPrefs.edit()
            .putInt("referral_progress", 0)
            .putBoolean("referral_reward_claimed", false)
            .apply()
    }

    fun getLastKnownBalance(): Double {
        return sharedPrefs.getFloat("last_wallet_balance", 200.0f).toDouble()
    }

    fun selectBet(amount: Double) {
        if (!_isGameActive.value) {
            _activeBet.value = amount
            updateBotStatusMessageForBet(amount)
        }
    }

    private fun updateBotStatusMessageForBet(amount: Double) {
        _botStatusMessage.value = if (amount == 0.0) {
            "🌐 Matchmaker: 'Practice match ready! Opponent connected.'"
        } else {
            val opponentName = _randomOpponentName.value
            "⚔️ Stakes up! Bet ${amount.toInt()} Coins to win ${(amount * 1.6).toInt()} Coins! Player '$opponentName' is online and waiting! 🌐"
        }
    }

    fun selectOpponent(type: String) {
        _opponent.value = type
        if (_isGameActive.value && type == "Bot" && !_isPlayerTurn.value && _winner.value == null && !_isBotThinking.value) {
            triggerBotMove()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun startNewGame() {
        viewModelScope.launch {
            val email = _currentEmail.value
            val currentWallet = repository.getOrCreateWallet(email)
            val bet = if (_opponent.value == "Local Player") 0.0 else _activeBet.value

            if (_opponent.value != "Local Player" && currentWallet.playerBalance < bet) {
                _errorMessage.value = "⚠️ Insufficient Balance! Please add simulated funds."
                return@launch
            }

            _errorMessage.value = null
            _botCheatTriggered.value = false
            
            val prevWinnerSymbol = _winner.value

            val availableOpponents = indianOpponentNames.filter { !_blockedUsers.value.contains(it) }
            _randomOpponentName.value = if (availableOpponents.isNotEmpty()) availableOpponents.random() else "SmartBot_Pro"
            val opponentName = _randomOpponentName.value

            _winningLine.value = null
            _winner.value = null
            _board.value = List(9) { "" }

            val playerStarts = when (prevWinnerSymbol) {
                "X" -> true
                "O" -> false
                "Draw" -> !_currentGameStartedByPlayer
                else -> Random.nextBoolean()
            }
            _currentGameStartedByPlayer = playerStarts

            _isPlayerTurn.value = playerStarts

            val starterMessage = when {
                prevWinnerSymbol == "X" -> if (_opponent.value == "Bot") "🗣️ You start first because you won the previous round!" else "👥 Player X starts first because they won the previous round!"
                prevWinnerSymbol == "O" -> if (_opponent.value == "Bot") "🤖 $opponentName starts first because they won the previous round!" else "👥 Player O starts first because they won the previous round!"
                prevWinnerSymbol == "Draw" -> if (_opponent.value == "Bot") {
                    if (playerStarts) "🎮 You start first because $opponentName started the previous drawn round!"
                    else "🤖 $opponentName starts first because you started the previous drawn round!"
                } else {
                    if (playerStarts) "🎮 Player X starts first because Player O started the previous drawn round!"
                    else "👥 Player O starts first because Player X started the previous drawn round!"
                }
                playerStarts -> if (_opponent.value == "Bot") "🎲 You won the toss and start first!" else "🎲 Player X won the toss and starts first!"
                else -> if (_opponent.value == "Bot") "🎲 $opponentName won the toss and starts first!" else "🎲 Player O won the toss and starts first!"
            }

            if (bet > 0.0) {
                repository.deductWager(email, bet)
                _botStatusMessage.value = "⚔️ Stakes Active! ${bet.toInt()} Coins Bet placed! Winning Payout: ${(bet * 1.6).toInt()} Coins\n$starterMessage"
            } else {
                _botStatusMessage.value = if (_opponent.value == "Local Player") {
                    "👥 Local Pass & Play match started.\n$starterMessage"
                } else {
                    "Practice session started.\n$starterMessage"
                }
            }

            _isGameActive.value = true
            
            if (!playerStarts && _opponent.value == "Bot") {
                triggerBotMove()
            }
        }
    }

    fun playMove(index: Int) {
        if (!_isGameActive.value || _winner.value != null || _isBotThinking.value) return
        if (_board.value[index].isNotEmpty()) return

        val list = _board.value.toMutableList()
        val currentPlayerSymbol = if (_opponent.value == "Local Player") {
            if (_isPlayerTurn.value) "X" else "O"
        } else {
            "X"
        }

        list[index] = currentPlayerSymbol
        _board.value = list

        if (checkGameStatus()) {
            return
        }

        if (_opponent.value == "Local Player") {
            _isPlayerTurn.value = !_isPlayerTurn.value
        } else {
            _isPlayerTurn.value = false
            triggerBotMove()
        }
    }

    fun addSimulatedFunds(amount: Double) {
        viewModelScope.launch {
            repository.addFunds(_currentEmail.value, amount)
            _botStatusMessage.value = "🪙 Added simulated ${amount.toInt()} Coins to wallet!"
        }
    }

    fun withdrawSimulatedFunds(amount: Double, upiId: String) {
        viewModelScope.launch {
            repository.addFunds(_currentEmail.value, -amount)
            _botStatusMessage.value = "🟢 Successfully withdrew ${"%.1f".format(amount)} Coins to UPI ID: $upiId!"
        }
    }

    fun resetStatsAndHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _botStatusMessage.value = "Wallet and logs reset to original state."
            _board.value = List(9) { "" }
            _winner.value = null
            _winningLine.value = null
            _isGameActive.value = false
            _activeBet.value = 5.0
        }
    }

    fun returnToLobby() {
        _board.value = List(9) { "" }
        _winner.value = null
        _winningLine.value = null
        _isGameActive.value = false
        _activeBet.value = 5.0
        _botStatusMessage.value = "Select a bet to start!"
    }

    fun getBotDifficultyProgress(): BotDifficultyProgression {
        val list = transactions.value
        val botMatches = list.filter { it.opponent.contains("Bot") }
        val botWins = botMatches.count { it.type == "WIN" }
        val botLosses = botMatches.count { it.type == "LOSS" }

        val lastMatch = botMatches.lastOrNull()
        val isLastMatchLoss = lastMatch?.type == "LOSS"
        val isHighStakes = _activeBet.value >= 20.0
        val isHoneymoon = botWins < 3

        val stateName: String
        val statusMessage: String

        val chokeProbability = when {
            isLastMatchLoss -> {
                stateName = "🛡️ Loss Recovery Boost"
                statusMessage = "🛡️ Opponent under severe pressure! Making beginner-level mistakes to let you bounce back."
                0.80
            }
            isHighStakes -> {
                stateName = "😰 High-Stakes Pressure"
                statusMessage = "😰 High stakes detected! Opponent is feeling nervous and playing defensively."
                0.55
            }
            isHoneymoon -> {
                stateName = "🌟 Honeymoon Welcome"
                statusMessage = "🌟 Starter Arena Active! Enjoy smooth matches as you build up your win streak."
                0.65
            }
            else -> {
                stateName = "⚖️ Dynamic Balanced Play"
                statusMessage = "⚡ Opponent matched to your skill. Play strategically to secure the win!"
                0.30
            }
        }

        return BotDifficultyProgression(
            stateName = stateName,
            cheatProbability = chokeProbability,
            statusMessage = statusMessage,
            botWinsCount = botWins,
            botLossesCount = botLosses
        )
    }

    private fun triggerBotMove() {
        viewModelScope.launch {
            _isBotThinking.value = true
            
            val delayDuration = if (_activeBet.value > 0.0) 800L else 500L
            delay(delayDuration)

            val currentBoard = _board.value.toMutableList()
            var chosenIndex = -1
            val bet = _activeBet.value

            val prog = getBotDifficultyProgress()
            val shouldChoke = Random.nextDouble() < prog.cheatProbability

            if (shouldChoke) {
                val emptySlots = currentBoard.indices.filter { currentBoard[it] == "" }
                if (emptySlots.isNotEmpty()) {
                    chosenIndex = emptySlots.random()
                }
                _botStatusMessage.value = "🎯 Opponent is under pressure... They missed a critical cell!"
            } else {
                val winningMove = findWinningMove(currentBoard, "O")
                if (winningMove != -1) {
                    chosenIndex = winningMove
                } else {
                    val blockMove = findWinningMove(currentBoard, "X")
                    if (blockMove != -1 && Random.nextDouble() < 0.70) {
                        chosenIndex = blockMove
                    } else {
                        val emptySlots = currentBoard.indices.filter { currentBoard[it] == "" }
                        if (emptySlots.isNotEmpty()) {
                            if (Random.nextDouble() < 0.70) {
                                chosenIndex = emptySlots.random()
                            } else {
                                chosenIndex = getBestMoveMinimax(currentBoard)
                            }
                        }
                    }
                }
            }

            if (chosenIndex != -1 && chosenIndex < currentBoard.size) {
                currentBoard[chosenIndex] = "O"
                _board.value = currentBoard
            }

            _isBotThinking.value = false
            _isPlayerTurn.value = true

            checkGameStatus()
        }
    }

    private fun findWinningMove(board: List<String>, symbol: String): Int {
        for (line in winningPositions) {
            val cell1 = board[line[0]]
            val cell2 = board[line[1]]
            val cell3 = board[line[2]]

            if (cell1 == symbol && cell2 == symbol && cell3 == "") return line[2]
            if (cell1 == symbol && cell3 == symbol && cell2 == "") return line[1]
            if (cell2 == symbol && cell3 == symbol && cell1 == "") return line[0]
        }
        return -1
    }

    private fun getBestMoveMinimax(board: List<String>): Int {
        var bestVal = -1000
        var bestMove = -1
        
        val emptySlots = board.indices.filter { board[it] == "" }
        if (emptySlots.size == 9) return emptySlots.random()
        if (emptySlots.isEmpty()) return -1

        for (i in board.indices) {
            if (board[i] == "") {
                val tempBoard = board.toMutableList()
                tempBoard[i] = "O"
                val moveVal = minimax(tempBoard, 0, false)
                if (moveVal > bestVal) {
                    bestVal = moveVal
                    bestMove = i
                }
            }
        }
        return if (bestMove != -1) bestMove else emptySlots.random()
    }

    private fun minimax(board: List<String>, depth: Int, isMax: Boolean): Int {
        val score = evaluateBoard(board)
        if (score == 10) return score - depth
        if (score == -10) return score + depth
        if (!board.contains("")) return 0

        if (isMax) {
            var best = -1000
            for (i in board.indices) {
                if (board[i] == "") {
                    val temp = board.toMutableList()
                    temp[i] = "O"
                    best = maxOf(best, minimax(temp, depth + 1, false))
                }
            }
            return best
        } else {
            var best = 1000
            for (i in board.indices) {
                if (board[i] == "") {
                    val temp = board.toMutableList()
                    temp[i] = "X"
                    best = minOf(best, minimax(temp, depth + 1, true))
                }
            }
            return best
        }
    }

    private fun evaluateBoard(board: List<String>): Int {
        for (line in winningPositions) {
            if (board[line[0]] == "O" && board[line[1]] == "O" && board[line[2]] == "O") return 10
            if (board[line[0]] == "X" && board[line[1]] == "X" && board[line[2]] == "X") return -10
        }
        return 0
    }

    private fun checkGameStatus(): Boolean {
        for (line in winningPositions) {
            val cell1 = _board.value[line[0]]
            val cell2 = _board.value[line[1]]
            val cell3 = _board.value[line[2]]

            if (cell1.isNotEmpty() && cell1 == cell2 && cell1 == cell3) {
                _winner.value = cell1
                _winningLine.value = line
                handleGameEnd(cell1)
                return true
            }
        }

        if (!_board.value.contains("")) {
            _winner.value = "Draw"
            handleGameEnd("Draw")
            return true
        }

        return false
    }

    private fun handleGameEnd(symbolResult: String) {
        _isGameActive.value = false
        val bet = if (_opponent.value == "Local Player") 0.0 else _activeBet.value
        val opponentName = _randomOpponentName.value
        val opponentType = if (_opponent.value == "Bot") {
            if (bet > 0.0) "$opponentName (God Mode)" else "$opponentName (Normal)"
        } else {
            "Local Player"
        }

        val boardDataString = _board.value.joinToString(",") { if (it.isEmpty()) "_" else it }

        viewModelScope.launch {
            val email = _currentEmail.value
            if (symbolResult == "Draw") {
                SoundEffects.playClick()
                _botStatusMessage.value = if (_opponent.value == "Local Player") {
                    "🤝 Draw! Local Pass & Play match finished in a draw."
                } else if (bet > 0.0) {
                    "🤝 Draw! Bet of ${bet.toInt()} Coins refunded back to wallet."
                } else {
                    "Game finished in a Draw!"
                }
                repository.recordMatch(email, bet, "DRAW", opponentType, boardDataString)
            } else if (symbolResult == "X") {
                SoundEffects.playWin()
                _botStatusMessage.value = if (_opponent.value == "Local Player") {
                    "🎉 Player X won the Local Pass & Play match!"
                } else if (bet > 0.0) {
                    "🎉 YOU WON! Received ${(bet * 1.6).toInt()} Coins Payout (${(bet * 0.4).toInt()} Coins Platform Fee withheld)."
                } else {
                    "🎉 FREE ARENA VICTORY! Earned 0.5 Coins ad prize. Wallet updated!"
                }
                repository.recordMatch(email, bet, "PLAYER", opponentType, boardDataString)
            } else {
                SoundEffects.playClick()
                _botStatusMessage.value = if (_opponent.value == "Local Player") {
                    "🎉 Player O won the Local Pass & Play match!"
                } else if (_opponent.value == "Bot") {
                    if (bet > 0.0) {
                        "❌ Defeat! $opponentName wins ${(bet * 2.0).toInt()} Coins! House always wins."
                    } else {
                        "🤖 $opponentName: 'GG, well played! Try again under stakes!'"
                    }
                } else {
                    "Congratulations! Player O won!"
                }
                repository.recordMatch(email, bet, "OPPONENT", opponentType, boardDataString)
            }
        }
    }
}

data class BotDifficultyProgression(
    val stateName: String,
    val cheatProbability: Double,
    val statusMessage: String,
    val botWinsCount: Int,
    val botLossesCount: Int
)
