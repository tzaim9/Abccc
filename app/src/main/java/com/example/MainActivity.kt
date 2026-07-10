package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionLog
import com.example.ui.GameViewModel
import com.example.ui.SoundEffects
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = BetTacBgStart
                    ) {
                        BetTacDashboard(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun BetTacDashboard(viewModel: GameViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val board by viewModel.board.collectAsStateWithLifecycle()
    val isPlayerTurn by viewModel.isPlayerTurn.collectAsStateWithLifecycle()
    val activeBet by viewModel.activeBet.collectAsStateWithLifecycle()
    val opponent by viewModel.opponent.collectAsStateWithLifecycle()
    val isGameActive by viewModel.isGameActive.collectAsStateWithLifecycle()
    val winner by viewModel.winner.collectAsStateWithLifecycle()
    val winningLine by viewModel.winningLine.collectAsStateWithLifecycle()
    val botStatusMessage by viewModel.botStatusMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isBotThinking by viewModel.isBotThinking.collectAsStateWithLifecycle()
    val randomOpponentName by viewModel.randomOpponentName.collectAsStateWithLifecycle()

    val isGmailLoggedIn by viewModel.isGmailLoggedIn.collectAsStateWithLifecycle()
    val currentEmail by viewModel.currentEmail.collectAsStateWithLifecycle()
    val currentName by viewModel.currentName.collectAsStateWithLifecycle()
    val currentUniqueId by viewModel.currentUniqueId.collectAsStateWithLifecycle()
    val transferStatus by viewModel.transferStatus.collectAsStateWithLifecycle()

    // Tab state (0 = Lobby/Play, 1 = Ledger, 2 = Leaderboard, 3 = Settings)
    var currentTab by remember { mutableStateOf(0) }

    // Dialog state management
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    // Transfer status listener
    LaunchedEffect(transferStatus) {
        transferStatus?.let { status ->
            val message = when (status) {
                "SUCCESS" -> "💸 Coins transferred successfully!"
                "INSUFFICIENT_FUNDS" -> "⚠️ Insufficient Balance for transfer!"
                "CANNOT_SEND_TO_SELF" -> "⚠️ You cannot send coins to yourself!"
                else -> "⚠️ Transfer failed!"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearTransferStatus()
        }
    }

    // Error handler
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    // Play victory sound on game win
    LaunchedEffect(winner) {
        if (winner == "X") {
            SoundEffects.playWin()
        }
    }

    // Main layout container with full-bleed vertical gradient matching PDF tokens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BetTacBgStart, BetTacBgEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Screen Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand Side
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepAccentPurple.copy(alpha = 0.2f))
                            .border(1.dp, NeonElectricBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonElectricBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "BetTac X ⚔️",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Logged In • Online Duel Mode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonElectricBlue
                        )
                    }
                }

                // Right Utility Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            SoundEffects.playClick()
                            showRulesDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show Rules",
                            tint = NeonElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Main Active Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // If game is active or complete, overlay game board regardless of tab (or anchor in Lobby)
                if (isGameActive || winner != null) {
                    ActiveBattleArena(
                        board = board,
                        isPlayerTurn = isPlayerTurn,
                        activeBet = activeBet,
                        opponent = opponent,
                        winner = winner,
                        winningLine = winningLine,
                        isBotThinking = isBotThinking,
                        randomOpponentName = randomOpponentName,
                        viewModel = viewModel
                    )
                } else {
                    when (currentTab) {
                        0 -> LobbyPlayTab(
                            walletBalance = wallet?.playerBalance ?: viewModel.getLastKnownBalance(),
                            activeBet = activeBet,
                            opponent = opponent,
                            viewModel = viewModel,
                            onDepositClick = { showDepositDialog = true },
                            onWithdrawClick = { showWithdrawDialog = true }
                        )
                        1 -> MatchLedgerTab(
                            transactions = transactions,
                            viewModel = viewModel
                        )
                        2 -> LeaderboardTab()
                        3 -> SettingsTab(
                            viewModel = viewModel,
                            onLoginClick = { showGoogleLoginDialog = true },
                            onDepositClick = { showDepositDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom high-fidelity bottom nav bar
            BetTacBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    }

    // --- Core Dialogs ---

    // Rules Dialog
    if (showRulesDialog) {
        Dialog(onDismissRequest = { showRulesDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BetTacCardBg),
                border = BorderStroke(1.dp, BetTacCardOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How to Play BetTac ⚔️",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Choose a Simulated Stake (Free, 5, 10, 20, or 50 Coins).\n\n" +
                                "2. Select VS Smart Bot or VS Friend locally (Pass & Play).\n\n" +
                                "3. Get 3 of your symbols in a row (horizontal, vertical, diagonal) to win!\n\n" +
                                "4. Win payout is 1.6x of the stake size! Coins are credited instantly to your simulated balance.\n\n" +
                                "5. Practice makes perfect. Try to block the bot to secure your win!",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            SoundEffects.playClick()
                            showRulesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepAccentPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Got It!", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Add Funds Dialog
    if (showDepositDialog) {
        var depositAmtText by remember { mutableStateOf("50") }
        Dialog(onDismissRequest = { showDepositDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BetTacCardBg),
                border = BorderStroke(1.dp, BetTacCardOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add Simulated Coins 🪙",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    TextField(
                        value = depositAmtText,
                        onValueChange = { depositAmtText = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount (Coins)", color = ProTextLabel) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF201646),
                            unfocusedContainerColor = Color(0xFF130A30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("10", "50", "100", "200").forEach { quickAmt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF261C52))
                                    .clickable {
                                        SoundEffects.playClick()
                                        depositAmtText = quickAmt
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$quickAmt C",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDepositDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4C3A96)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                val amt = depositAmtText.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    // Trigger user requested redirect on deposit click
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.tzaim.blog/2025/09/t-zaim-introduction.html")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening link...", Toast.LENGTH_SHORT).show()
                                    }
                                    viewModel.addSimulatedFunds(amt)
                                    showDepositDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepAccentPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Deposit", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dual-Tab Wallet Actions Hub (P2P Gamer Transfer / UPI Withdraw)
    if (showWithdrawDialog) {
        var activeDialogTab by remember { mutableStateOf(0) } // 0 = Send to Gamer, 1 = UPI Withdraw
        var transferAmtText by remember { mutableStateOf("20") }
        var recipientIdText by remember { mutableStateOf("") }
        
        var withdrawAmtText by remember { mutableStateOf("50") }
        var upiIdText by remember { mutableStateOf("altabali@paytm") }

        Dialog(onDismissRequest = { showWithdrawDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BetTacCardBg),
                border = BorderStroke(1.2.dp, BetTacCardOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wallet Actions Hub 🪙",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF100B29))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeDialogTab == 0) DeepAccentPurple else Color.Transparent)
                                .clickable {
                                    SoundEffects.playClick()
                                    activeDialogTab = 0
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Send to Gamer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeDialogTab == 1) NeonCyberPink else Color.Transparent)
                                .clickable {
                                    SoundEffects.playClick()
                                    activeDialogTab = 1
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("UPI Withdraw", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeDialogTab == 0) {
                        // SEND TO GAMER
                        Text(
                            text = "Transfer coins instantly to another verified Gamer.",
                            fontSize = 11.sp,
                            color = ProTextLabel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        TextField(
                            value = recipientIdText,
                            onValueChange = { recipientIdText = it },
                            placeholder = { Text("Gamer Unique ID (e.g. BETTAC-PREETI-8412)", color = ProTextLabel.copy(alpha = 0.5f), fontSize = 11.sp) },
                            label = { Text("Recipient Gamer ID", color = ProTextLabel) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF201646),
                                unfocusedContainerColor = Color(0xFF130A30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick suggestion list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF100B29), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Text("Tap to select a virtual Online Friend:", fontSize = 9.sp, color = NeonElectricBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "Preeti" to "BETTAC-PREETI-8412",
                                "Karan" to "BETTAC-KARAN-3921",
                                "Rohan" to "BETTAC-ROHAN-5721"
                            ).forEach { (friendName, uid) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            SoundEffects.playClick()
                                            recipientIdText = uid
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "👤 $friendName", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(text = uid, fontSize = 9.sp, color = CoinGold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextField(
                            value = transferAmtText,
                            onValueChange = { transferAmtText = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount (Coins)", color = ProTextLabel) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF201646),
                                unfocusedContainerColor = Color(0xFF130A30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showWithdrawDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF4C3A96)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    val amt = transferAmtText.toDoubleOrNull() ?: 0.0
                                    val currentBalance = wallet?.playerBalance ?: viewModel.getLastKnownBalance()
                                    if (recipientIdText.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter recipient Gamer ID", Toast.LENGTH_SHORT).show()
                                    } else if (amt > currentBalance) {
                                        Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                                    } else if (amt <= 0.0) {
                                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.transferCoins(recipientIdText, amt)
                                        showWithdrawDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepAccentPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Transfer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }

                    } else {
                        // UPI WITHDRAWAL
                        Text(
                            text = "Withdraw simulated coins to your UPI account.",
                            fontSize = 11.sp,
                            color = ProTextLabel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        TextField(
                            value = withdrawAmtText,
                            onValueChange = { withdrawAmtText = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount (Coins)", color = ProTextLabel) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF201646),
                                unfocusedContainerColor = Color(0xFF130A30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TextField(
                            value = upiIdText,
                            onValueChange = { upiIdText = it },
                            label = { Text("UPI ID", color = ProTextLabel) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF201646),
                                unfocusedContainerColor = Color(0xFF130A30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showWithdrawDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF4C3A96)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    val amt = withdrawAmtText.toDoubleOrNull() ?: 0.0
                                    val currentBalance = wallet?.playerBalance ?: viewModel.getLastKnownBalance()
                                    if (amt > currentBalance) {
                                        Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                                    } else if (amt > 0.0 && upiIdText.isNotEmpty()) {
                                        viewModel.withdrawSimulatedFunds(amt, upiIdText)
                                        showWithdrawDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyberPink),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Withdraw", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Sign-In Verification Dialog
    if (showGoogleLoginDialog) {
        var inputEmail by remember { mutableStateOf("altabali2597@gmail.com") }
        var inputName by remember { mutableStateOf("Altab Ali") }
        var isVerifying by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isVerifying) showGoogleLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("o", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("o", color = Color(0xFFFBBC05), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("g", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("l", color = Color(0xFF34A853), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("e", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sign in to BetTac X",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                    Text(
                        text = "Use your Google / Gmail Account",
                        fontSize = 12.sp,
                        color = Color(0xFF5F6368),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isVerifying) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 20.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Connecting secure authentication service...", fontSize = 11.sp, color = Color(0xFF5F6368))
                        }
                    } else {
                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            label = { Text("Gmail Address") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = Color(0xFF4285F4),
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Gamer Display Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = Color(0xFF4285F4),
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Or choose one-tap verified accounts:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5F6368),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        listOf(
                            "altabali2597@gmail.com" to "Altab Ali",
                            "gamingwarrior@gmail.com" to "Cyber Giga"
                        ).forEach { (em, nm) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundEffects.playClick()
                                        inputEmail = em
                                        inputName = nm
                                    }
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F3F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(nm.take(1), color = Color(0xFF5F6368), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(nm, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202124))
                                    Text(em, fontSize = 9.sp, color = Color(0xFF5F6368))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGoogleLoginDialog = false },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A73E8))
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    if (!inputEmail.contains("@") || inputEmail.length < 5) {
                                        Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isVerifying = true
                                        coroutineScope.launch {
                                            delay(1200)
                                            viewModel.performGmailLogin(inputEmail.trim(), inputName.trim())
                                            isVerifying = false
                                            showGoogleLoginDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                            ) {
                                Text("Verify & Login", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 0: LOBBY / PLAY SCREEN ---
@Composable
fun LobbyPlayTab(
    walletBalance: Double,
    activeBet: Double,
    opponent: String,
    viewModel: GameViewModel,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    val context = LocalContext.current
    val isGmailLoggedIn by viewModel.isGmailLoggedIn.collectAsStateWithLifecycle()
    val currentEmail by viewModel.currentEmail.collectAsStateWithLifecycle()
    val currentName by viewModel.currentName.collectAsStateWithLifecycle()
    val currentUniqueId by viewModel.currentUniqueId.collectAsStateWithLifecycle()
    val referralProgress by viewModel.referralProgress.collectAsStateWithLifecycle()
    val isReferralRewardClaimed by viewModel.isReferralRewardClaimed.collectAsStateWithLifecycle()
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    // Standard Google Login dialog specifically for this local action trigger if needed:
    if (showGoogleLoginDialog) {
        var inputEmail by remember { mutableStateOf("altabali2597@gmail.com") }
        var inputName by remember { mutableStateOf("Altab Ali") }
        var isVerifying by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isVerifying) showGoogleLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("o", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("o", color = Color(0xFFFBBC05), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("g", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("l", color = Color(0xFF34A853), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("e", color = Color(0xFFEA4335), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sign in to BetTac X",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                    Text(
                        text = "Use your Google / Gmail Account",
                        fontSize = 12.sp,
                        color = Color(0xFF5F6368),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isVerifying) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 20.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Connecting secure authentication service...", fontSize = 11.sp, color = Color(0xFF5F6368))
                        }
                    } else {
                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            label = { Text("Gmail Address") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = Color(0xFF4285F4),
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Gamer Display Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = Color(0xFF4285F4),
                                focusedBorderColor = Color(0xFF4285F4)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Or choose one-tap verified accounts:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5F6368),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        listOf(
                            "altabali2597@gmail.com" to "Altab Ali",
                            "gamingwarrior@gmail.com" to "Cyber Giga"
                        ).forEach { (em, nm) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundEffects.playClick()
                                        inputEmail = em
                                        inputName = nm
                                    }
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F3F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(nm.take(1), color = Color(0xFF5F6368), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(nm, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202124))
                                    Text(em, fontSize = 9.sp, color = Color(0xFF5F6368))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGoogleLoginDialog = false },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A73E8))
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    if (!inputEmail.contains("@") || inputEmail.length < 5) {
                                        Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isVerifying = true
                                        viewModel.performGmailLogin(inputEmail.trim(), inputName.trim())
                                        isVerifying = false
                                        showGoogleLoginDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                            ) {
                                Text("Verify & Login", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Verified Gamer Identity Profile Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isGmailLoggedIn) Color(0xFF120E2E) else Color(0xFF16103A)),
                border = BorderStroke(1.dp, if (isGmailLoggedIn) NeonElectricBlue else Color(0xFF2C225C))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (isGmailLoggedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(DeepAccentPurple, Color(0xFF231454)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF34A853).copy(alpha = 0.2f))
                                                .border(0.5.dp, Color(0xFF34A853), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "VERIFIED",
                                                color = Color(0xFF34A853),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentEmail,
                                        fontSize = 10.sp,
                                        color = ProTextLabel
                                    )
                                }
                            }

                            Text(
                                text = "Disconnect",
                                color = NeonCyberPink,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        SoundEffects.playClick()
                                        viewModel.performGmailLogout()
                                    }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A133A))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "YOUR UNIQUE GAMER ID",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonElectricBlue
                                )
                                Text(
                                    text = currentUniqueId,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    SoundEffects.playClick()
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Gamer ID", currentUniqueId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Gamer ID Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy Gamer ID",
                                    tint = NeonElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color(0xFFEA4335), fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect Google Account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Get a Unique Gamer ID & unlock P2P Transfers!",
                                    fontSize = 9.sp,
                                    color = ProTextLabel
                                )
                            }
                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    showGoogleLoginDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepAccentPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Login", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        // Stats Meter Row underneath Header (Page 3 visual layout matching)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatIndicator(title = "Avatar", value = "36")
                    StatDivider()
                    StatIndicator(title = "Stats", value = "25")
                    StatDivider()
                    StatIndicator(title = "Wins", value = "187")
                    StatDivider()
                    StatIndicator(title = "Statics", value = "3")
                }
            }
        }

        // Frosted Wallet Hub Card with Gold Coin and progress bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF231454)),
                border = BorderStroke(1.2.dp, Color(0xFF422180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wallet Balance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProTextLabel,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🪙 ${"%.1f".format(walletBalance)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COINS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonElectricBlue
                                )
                            }
                        }

                        // Big gold coin visual decoration
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CoinGold, Color(0xFFB59300)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Master Level Progress Bar from Mockup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Master Level",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🌟 Streak: x1.6",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CoinGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { 0.7f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonElectricBlue,
                        trackColor = Color(0xFF130A30)
                    )

                    Text(
                        text = "Progress to: Streak Multiplier (70%)",
                        fontSize = 10.sp,
                        color = ProTextLabel,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                onDepositClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("add_funds_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Deposit",
                                tint = Color(0xFF231454),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Funds",
                                color = Color(0xFF231454),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                SoundEffects.playClick()
                                onWithdrawClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("withdraw_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, NeonElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Withdraw",
                                tint = NeonElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Withdraw",
                                color = NeonElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- REFER & EARN 5000 COINS WHATSAPP BANNER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("refer_earn_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F42)),
                border = BorderStroke(1.2.dp, NeonCyberPink)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "👑 REFER & EARN 5000 COINS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyberPink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Earn 5,000 Queens instantly by sharing!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                .padding(6.dp)
                        ) {
                            Text("👑", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WhatsApp Sends Progress: $referralProgress/5",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGold
                        )
                        if (isReferralRewardClaimed) {
                            Text(
                                text = "CLAIMED 🎉",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF34A853)
                            )
                        } else if (referralProgress >= 5) {
                            Button(
                                onClick = {
                                    SoundEffects.playWin()
                                    viewModel.claimReferralReward()
                                    Toast.makeText(context, "🎉 5,000 Queens added to your balance!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyberPink),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("CLAIM 5000", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Text(
                                text = "Send 5 to unlock",
                                fontSize = 10.sp,
                                color = ProTextLabel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { referralProgress / 5.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF25D366),
                        trackColor = Color(0xFF100B29)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                SoundEffects.playClick()
                                viewModel.resetReferralProgress()
                                Toast.makeText(context, "Referral progress reset!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color(0xFF4C3A96)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                if (referralProgress < 5) {
                                    viewModel.incrementReferralProgress()
                                    // Launch WhatsApp intent
                                    try {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Hey! Play BetTac X Tic-Tac-Toe with me and earn simulated coins! Use my referral code: $currentUniqueId. Download here: https://ai.studio/build")
                                            setPackage("com.whatsapp")
                                        }
                                        context.startActivity(sendIntent)
                                    } catch (e: Exception) {
                                        // Fallback standard chooser
                                        val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Hey! Play BetTac X Tic-Tac-Toe with me and earn simulated coins! Use my referral code: $currentUniqueId")
                                        }, "Share referral link")
                                        context.startActivity(chooserIntent)
                                    }
                                } else {
                                    Toast.makeText(context, "Already referred 5 persons! Claim your reward.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("whatsapp_share_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("💬 Send via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Play Settings: Opponent and Battle Stake selectors
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "Matchmaking Setup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Select Opponent
                    Text(
                        text = "Opponent Type:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProTextLabel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBot = opponent == "Bot"
                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                viewModel.selectOpponent("Bot")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBot) DeepAccentPurple else Color(0xFF231454)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "🤖 VS Bot",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                viewModel.selectOpponent("Local Player")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isBot) DeepAccentPurple else Color(0xFF231454)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "👥 Local Pass",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Select Stake Amount chips
                    Text(
                        text = "Wager Stake Amount:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProTextLabel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val activeBets = listOf(0.0, 5.0, 10.0, 20.0, 50.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        activeBets.forEach { amt ->
                            val isSelected = activeBet == amt
                            val chipBg = if (isSelected) NeonElectricBlue else Color(0xFF231454)
                            val chipText = if (isSelected) Color(0xFF0B081F) else Color.White

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .clickable {
                                        SoundEffects.playClick()
                                        viewModel.selectBet(amt)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (amt == 0.0) "Free" else "${amt.toInt()} C",
                                    color = chipText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LARGE START MATCH BUTTON
                    Button(
                        onClick = {
                            SoundEffects.playClick()
                            viewModel.startNewGame()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_match_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyberPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Duel",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START POWER DUEL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Online Friends Section with Gamified statuses
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friends Online",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Clear All",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyberPink,
                        modifier = Modifier.clickable {
                            SoundEffects.playClick()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnlineFriendRow(name = "Abmir Ram", status = "Just won a bet match!")
                    OnlineFriendRow(name = "Mavahanmah", status = "Searching for an opponent...")
                    OnlineFriendRow(name = "Elavis Koan", status = "In an active match.")
                }
            }
        }
    }
}

// --- ACTIVE GAME ARENA OVERLAY ---
@Composable
fun ActiveBattleArena(
    board: List<String>,
    isPlayerTurn: Boolean,
    activeBet: Double,
    opponent: String,
    winner: String?,
    winningLine: List<Int>?,
    isBotThinking: Boolean,
    randomOpponentName: String,
    viewModel: GameViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
        border = BorderStroke(1.2.dp, NeonElectricBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Battle Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (opponent == "Bot") "🤖 Smart Bot Duel" else "👥 Friend Duel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (opponent == "Bot") "VS $randomOpponentName" else "Local Pass & Play",
                        fontSize = 11.sp,
                        color = ProTextLabel
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyberPink)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Wager: ${activeBet.toInt()} Coins",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Real-time Adversary Pressure Signals & Turn Statuses (Page 3 layout specs)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val statusText = when {
                    winner != null -> {
                        if (winner == "X") "🏆 VICTORY IS YOURS!" else if (winner == "O") "💀 ELIMINATED! OPONENT WINS" else "🤝 DRAWSOME MATCH!"
                    }
                    isBotThinking -> "🤖 Opponent calculating killer move..."
                    isPlayerTurn -> "⚡ YOUR TURN (X) - Play fast!"
                    else -> "⏳ Opponent's Turn (O)"
                }

                val statusColor = when {
                    winner == "X" -> NeonElectricBlue
                    winner == "O" -> NeonCyberPink
                    isBotThinking -> CoinGold
                    else -> Color.White
                }

                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    textAlign = TextAlign.Center
                )

                // Subtitle / Strategy tip indicator
                val strategyText = if (isBotThinking) "Bot is evaluating center control blocks." else "Get 3 symbols in any row to instantly claim your coins!"
                Text(
                    text = strategyText,
                    fontSize = 10.sp,
                    color = ProTextLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // High Fidelity 3x3 Grid with frosted purple tiles
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0A29))
                    .border(1.5.dp, Color(0xFF2D235C), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val cellIndex = row * 3 + col
                                val symbol = board[cellIndex]
                                val isWinningCell = winningLine?.contains(cellIndex) == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, Color(0xFF261C52))
                                        .background(
                                            if (isWinningCell) NeonElectricBlue.copy(alpha = 0.2f) else Color.Transparent
                                        )
                                        .clickable(enabled = symbol.isEmpty() && !isBotThinking && winner == null) {
                                            SoundEffects.playClick()
                                            viewModel.playMove(cellIndex)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (symbol.isNotEmpty()) {
                                        Text(
                                            text = symbol,
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (symbol == "X") NeonElectricBlue else NeonCyberPink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Post-Game Battle controls & Reset actions
            if (winner != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            SoundEffects.playClick()
                            viewModel.returnToLobby()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, NeonElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RETURN TO LOBBY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            SoundEffects.playClick()
                            viewModel.startNewGame()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyberPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("BATTLE AGAIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Cancel Active Match Button
                OutlinedButton(
                    onClick = {
                        SoundEffects.playClick()
                        viewModel.returnToLobby()
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyberPink),
                    border = BorderStroke(1.dp, NeonCyberPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("FORFEIT & LOBBY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- TAB 1: MATCH LEDGER (STATS & PERFORMANCE ANALYSIS) ---
@Composable
fun MatchLedgerTab(
    transactions: List<TransactionLog>,
    viewModel: GameViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Switch bar at top matching mockup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var selectedToggle by remember { mutableStateOf(0) }
                    listOf("Home Screen", "Withdraw").forEachIndexed { idx, label ->
                        val isSelected = selectedToggle == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DeepAccentPurple else Color.Transparent)
                                .clickable {
                                    SoundEffects.playClick()
                                    selectedToggle = idx
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Performance Analysis with Custom charts from mockups
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "Performance Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Chart A: Wins this Week (Custom Bar rendering)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wins this Week",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProTextLabel
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simple responsive custom columns to represent wins/losses visually
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ChartBar(heightFactor = 0.8f, color = NeonElectricBlue, label = "Wins")
                                ChartBar(heightFactor = 0.4f, color = NeonCyberPink, label = "Loss")
                            }
                        }

                        // Chart B: Streak Consistency
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Streak Consistency",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProTextLabel
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ChartBar(heightFactor = 0.2f, color = DeepAccentPurple, label = "1")
                                ChartBar(heightFactor = 0.5f, color = DeepAccentPurple, label = "2")
                                ChartBar(heightFactor = 0.7f, color = DeepAccentPurple, label = "3")
                                ChartBar(heightFactor = 0.9f, color = NeonElectricBlue, label = "4")
                                ChartBar(heightFactor = 0.4f, color = DeepAccentPurple, label = "5")
                            }
                        }
                    }
                }
            }
        }

        // History logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH HISTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (transactions.isNotEmpty()) {
                    Text(
                        text = "Clear History",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyberPink,
                        modifier = Modifier.clickable {
                            SoundEffects.playClick()
                            viewModel.resetStatsAndHistory()
                        }
                    )
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recent games played.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProTextLabel
                        )
                    }
                }
            }
        } else {
            items(transactions.reversed()) { log ->
                LedgerItemRow(log)
            }
        }
    }
}

// --- TAB 2: LEADERBOARD & COMMUNITY FEED ---
@Composable
fun LeaderboardTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top 3 Podium Displays
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Power Rankings Podium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Rank 2: Silver
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🥈", fontSize = 20.sp)
                            Text(text = "Naooteri", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "19 Wins", fontSize = 9.sp, color = ProTextLabel)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(45.dp, 50.dp)
                                    .background(Color(0xFF2D235C), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                        }

                        // Rank 1: Gold
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "👑", fontSize = 24.sp)
                            Text(text = "BetTac X (You)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeonElectricBlue)
                            Text(text = "36 Wins", fontSize = 9.sp, color = CoinGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(50.dp, 75.dp)
                                    .background(NeonElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .border(1.2.dp, NeonElectricBlue, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "1", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                        }

                        // Rank 3: Bronze
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🥉", fontSize = 20.sp)
                            Text(text = "Stam. K", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "16 Wins", fontSize = 9.sp, color = ProTextLabel)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(45.dp, 40.dp)
                                    .background(Color(0xFF1F183E), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                        }
                    }
                }
            }
        }

        // Goal Setting Circular Progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 0.1f },
                            modifier = Modifier.fillMaxSize(),
                            color = NeonElectricBlue,
                            strokeWidth = 4.dp,
                            trackColor = Color(0xFF130A30)
                        )
                        Text(text = "10%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Goal-Setting Tracker",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Track progress towards our community achievements and unlock active multipliers.",
                            fontSize = 10.sp,
                            color = ProTextLabel
                        )
                    }
                }
            }
        }

        // Community Feed Heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMUNITY FEED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "See All",
                    fontSize = 10.sp,
                    color = NeonElectricBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal scrolling community cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommunityPostCard(
                    title = "Major Wins to Wit",
                    body = "Aaditya just won 190.0 Coins in a high stakes bot game!",
                    tag = "17 Coins Won"
                )

                CommunityPostCard(
                    title = "Strategy Shared",
                    body = "Never play your first symbol on corners if opponent controls center blocks.",
                    tag = "Hot Tip"
                )

                CommunityPostCard(
                    title = "Community Challenge",
                    body = "Unlock x1.8 Streak multiplier by securing 5 bot wins in a row.",
                    tag = "Live Event"
                )
            }
        }

        // Community Vibe Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VibeButton(emoji = "😊", label = "Friendly Match")
                VibeButton(emoji = "🧠", label = "Skill-Building")
                VibeButton(emoji = "👥", label = "Local Vibe")
            }
        }
    }
}

// --- TAB 3: SETTINGS & UPGRADES SHOP ---
@Composable
fun SettingsTab(
    viewModel: GameViewModel,
    onLoginClick: () -> Unit,
    onDepositClick: () -> Unit
) {
    val context = LocalContext.current
    val isGmailLoggedIn by viewModel.isGmailLoggedIn.collectAsStateWithLifecycle()
    val currentEmail by viewModel.currentEmail.collectAsStateWithLifecycle()
    val currentName by viewModel.currentName.collectAsStateWithLifecycle()
    val currentUniqueId by viewModel.currentUniqueId.collectAsStateWithLifecycle()
    val referralProgress by viewModel.referralProgress.collectAsStateWithLifecycle()
    val isReferralRewardClaimed by viewModel.isReferralRewardClaimed.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dynamic Profile Account Status Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Verified Gamer Identity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (isGmailLoggedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = currentName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF34A853).copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("VERIFIED", color = Color(0xFF34A853), fontSize = 7.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text(text = currentEmail, fontSize = 10.sp, color = ProTextLabel)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Unique ID: $currentUniqueId", 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = CoinGold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    SoundEffects.playClick()
                                    viewModel.performGmailLogout()
                                    Toast.makeText(context, "Logged out of Gmail", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, NeonCyberPink),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Logout", color = NeonCyberPink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Playing as Guest Player", fontSize = 11.sp, color = ProTextLabel)
                                Text(text = "Unique ID: BETTAC-GUEST-0000", fontSize = 9.sp, color = CoinGold, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    onLoginClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Connect Gmail", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- REFER & EARN 5000 COINS WHATSAPP BANNER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("refer_earn_settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F42)),
                border = BorderStroke(1.2.dp, NeonCyberPink)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "👑 REFER & EARN 5000 COINS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyberPink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Earn 5,000 Queens instantly by sharing!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                .padding(6.dp)
                        ) {
                            Text("👑", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WhatsApp Sends Progress: $referralProgress/5",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoinGold
                        )
                        if (isReferralRewardClaimed) {
                            Text(
                                text = "CLAIMED 🎉",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF34A853)
                            )
                        } else if (referralProgress >= 5) {
                            Button(
                                onClick = {
                                    SoundEffects.playWin()
                                    viewModel.claimReferralReward()
                                    Toast.makeText(context, "🎉 5,000 Queens added to your balance!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyberPink),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("CLAIM 5000", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Text(
                                text = "Send 5 to unlock",
                                fontSize = 10.sp,
                                color = ProTextLabel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { referralProgress / 5.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF25D366),
                        trackColor = Color(0xFF100B29)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                SoundEffects.playClick()
                                viewModel.resetReferralProgress()
                                Toast.makeText(context, "Referral progress reset!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color(0xFF4C3A96)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                SoundEffects.playClick()
                                if (referralProgress < 5) {
                                    viewModel.incrementReferralProgress()
                                    // Launch WhatsApp intent
                                    try {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Hey! Play BetTac X Tic-Tac-Toe with me and earn simulated coins! Use my referral code: $currentUniqueId. Download here: https://ai.studio/build")
                                            setPackage("com.whatsapp")
                                        }
                                        context.startActivity(sendIntent)
                                    } catch (e: Exception) {
                                        // Fallback standard chooser
                                        val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Hey! Play BetTac X Tic-Tac-Toe with me and earn simulated coins! Use my referral code: $currentUniqueId")
                                        }, "Share referral link")
                                        context.startActivity(chooserIntent)
                                    }
                                } else {
                                    Toast.makeText(context, "Already referred 5 persons! Claim your reward.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("whatsapp_share_settings_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("💬 Send via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // App Theme Selector Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Theme Vibe",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF231454))
                            .padding(2.dp)
                    ) {
                        var selectedTheme by remember { mutableStateOf(2) } // Neon as default
                        listOf("Light", "Dark", "Neon").forEachIndexed { idx, label ->
                            val isSelected = selectedTheme == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) DeepAccentPurple else Color.Transparent)
                                    .clickable {
                                        SoundEffects.playClick()
                                        selectedTheme = idx
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Coin Packs Section from mockup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Buy Coin Pack Shop",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Coin Pack Item 1
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF231454), RoundedCornerShape(12.dp))
                                .border(1.dp, NeonElectricBlue, RoundedCornerShape(12.dp))
                                .clickable {
                                    SoundEffects.playClick()
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.tzaim.blog/2025/09/t-zaim-introduction.html")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening link...", Toast.LENGTH_SHORT).show()
                                    }
                                    viewModel.addSimulatedFunds(19.5)
                                    Toast.makeText(context, "Sponsor Page Opened! 19.5 Coins added.", Toast.LENGTH_LONG).show()
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonElectricBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "Popular", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "🪙 19.5 Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Value Price: $1.99", fontSize = 10.sp, color = ProTextLabel)
                        }

                        // Coin Pack Item 2
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF231454), RoundedCornerShape(12.dp))
                                .border(1.dp, NeonCyberPink, RoundedCornerShape(12.dp))
                                .clickable {
                                    SoundEffects.playClick()
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.tzaim.blog/2025/09/t-zaim-introduction.html")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening link...", Toast.LENGTH_SHORT).show()
                                    }
                                    viewModel.addSimulatedFunds(100.0)
                                    Toast.makeText(context, "Sponsor Page Opened! 100.0 Coins added.", Toast.LENGTH_LONG).show()
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonCyberPink)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "Best Value", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "🪙 100 Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Special Price: $8.99", fontSize = 10.sp, color = ProTextLabel)
                        }
                    }
                }
            }
        }

        // BetTac Pro Upgrades
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
                border = BorderStroke(1.dp, Color(0xFF2C225C))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BetTac Pro Upgrades",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonElectricBlue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    UpgradeListItem(title = "Custom Visual Themes", description = "Unlock dark cyber neon overlays.", price = "$5.00")
                    HorizontalDivider(color = Color(0xFF2C225C), thickness = 0.5.dp)
                    UpgradeListItem(title = "Psychology Insights", description = "Real-time tracker of bot weaknesses.", price = "$3.50")
                    HorizontalDivider(color = Color(0xFF2C225C), thickness = 0.5.dp)
                    UpgradeListItem(title = "Premium Avatars Bundle", description = "36 distinct futuristic profile icons.", price = "Free")
                }
            }
        }

        // Psychology Corner Info Card (Page 3 specs matching)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF23103D)),
                border = BorderStroke(1.dp, NeonCyberPink.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧠", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Psychological Adversary Tips",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyberPink
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loss aversion kicks in when wager limits are breached. To sustain winning streaks, always keep 5x your active bet value inside the reserve vault wallet.",
                        fontSize = 10.sp,
                        color = ProTextLabel,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLE SUB-COMPONENTS ---

@Composable
fun OnlineFriendRow(name: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16103A), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FF66))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = status, color = ProTextLabel, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF231454))
                .clickable { SoundEffects.playClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "Challenge", color = NeonElectricBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatIndicator(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ProTextLabel)
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier = Modifier
            .size(1.dp, 16.dp)
            .background(Color(0xFF2C225C))
    )
}

@Composable
fun ChartBar(heightFactor: Float, color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight(heightFactor)
                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 8.sp, color = ProTextLabel, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CommunityPostCard(title: String, body: String, tag: String) {
    Card(
        modifier = Modifier
            .size(160.dp, 110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
        border = BorderStroke(1.dp, Color(0xFF2C225C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    fontSize = 9.sp,
                    color = ProTextLabel,
                    lineHeight = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF231454))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = tag, color = NeonElectricBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VibeButton(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .background(Color(0xFF16103A), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF2C225C), RoundedCornerShape(20.dp))
            .clickable { SoundEffects.playClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UpgradeListItem(title: String, description: String, price: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { SoundEffects.playClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = description, fontSize = 9.sp, color = ProTextLabel)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF231454))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = price, color = NeonElectricBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BetTacBottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF100B29))
            .border(BorderStroke(1.dp, Color(0xFF261C52)), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple(0, Icons.Default.Home, "Lobby"),
            Triple(1, Icons.Default.List, "Ledger"),
            Triple(2, Icons.Default.Star, "Rankings"),
            Triple(3, Icons.Default.Settings, "Settings")
        )

        tabs.forEach { (index, icon, label) ->
            val isSelected = currentTab == index
            val color = if (isSelected) NeonElectricBlue else Color(0xFF6C5E9F)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        SoundEffects.playClick()
                        onTabSelected(index)
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LedgerItemRow(log: TransactionLog) {
    val dateText = remember(log.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val isAddition = log.type == "ADD_FUNDS"
    val isWithdrawal = log.type == "WITHDRAW"
    val isWin = log.type == "WIN"
    val isLoss = log.type == "LOSS"

    val title = when (log.type) {
        "ADD_FUNDS" -> "Simulated Coins Added"
        "WITHDRAW" -> "Simulated Coins Withdrawn"
        "WIN" -> "Match Won vs ${log.opponent}"
        "LOSS" -> "Match Lost vs ${log.opponent}"
        else -> "Match Draw vs ${log.opponent}"
    }

    val profitColor = when {
        isAddition || isWin -> NeonElectricBlue
        isLoss || isWithdrawal -> NeonCyberPink
        else -> ProTextLabel
    }

    val amountText = when {
        isAddition -> "+${"%.0f".format(log.playerProfit)} C"
        isWithdrawal -> "-${"%.0f".format(-log.playerProfit)} C"
        isWin -> "+${"%.0f".format(log.playerProfit)} C"
        isLoss -> "-${"%.0f".format(log.betAmount)} C"
        else -> "0.0 C"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16103A)),
        border = BorderStroke(1.dp, Color(0xFF2C225C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAddition || isWin) NeonElectricBlue.copy(alpha = 0.1f)
                            else NeonCyberPink.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAddition) Icons.Default.Star
                        else if (isWin) Icons.Default.Star
                        else if (isLoss) Icons.Default.Close
                        else if (isWithdrawal) Icons.Default.Delete
                        else Icons.Default.Info,
                        contentDescription = log.type,
                        tint = if (isAddition || isWin) NeonElectricBlue else NeonCyberPink,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateText,
                        fontSize = 10.sp,
                        color = ProTextLabel
                    )
                }
            }

            Text(
                text = amountText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = profitColor
            )
        }
    }
}
