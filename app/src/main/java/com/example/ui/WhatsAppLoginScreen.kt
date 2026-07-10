package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WhatsAppLoginScreen(
    viewModel: GameViewModel,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    
    var isOtpSent by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    
    var generatedOtp by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf(30) }
    var showNotification by remember { mutableStateOf(false) }
    var loginSuccessComplete by remember { mutableStateOf(false) }

    // Countdown Timer for OTP Resend
    LaunchedEffect(isOtpSent) {
        if (isOtpSent) {
            timerSeconds = 30
            while (timerSeconds > 0) {
                delay(1000L)
                timerSeconds--
            }
        }
    }

    // Auto-dismiss WhatsApp notification after 8 seconds
    LaunchedEffect(showNotification) {
        if (showNotification) {
            delay(8000L)
            showNotification = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ProBackground,
                        ProSurfaceContainer
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Success Animation ---
            if (loginSuccessComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = ProGreenWin,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verification Successful! 🎉",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ProDeepPurple,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Welcome to BetTac. Redirecting...",
                        fontSize = 13.sp,
                        color = ProTextLabel,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // --- Brand Header ---
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ProPrimaryPurple.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚔️",
                        fontSize = 32.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "BetTac Lobby Login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = ProDeepPurple,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Enter your mobile number to receive a secure login OTP instantly via WhatsApp.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ProTextLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!isOtpSent) {
                    // --- Phone Number Input View ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ProWhite),
                        border = BorderStroke(1.dp, ProBorderLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WhatsApp Number Verification",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProTextDark,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Country code prefix
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ProSecondaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 15.dp)
                                ) {
                                    Text(
                                        text = "+91 🇮🇳",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = ProTextDark
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Number TextField
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 10) {
                                            phoneNumber = input
                                        }
                                    },
                                    placeholder = { Text("10-digit Phone Number") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = ProPrimaryPurple
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProPrimaryPurple,
                                        unfocusedBorderColor = ProBorderMedium,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_phone_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    if (phoneNumber.length != 10) {
                                        Toast.makeText(context, "Please enter a valid 10-digit mobile number!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isSendingOtp = true
                                        delay(1500L) // Simulate API delivery duration
                                        generatedOtp = (1000 + (Math.random() * 9000).toInt()).toString()
                                        isSendingOtp = false
                                        isOtpSent = true
                                        showNotification = true
                                        Toast.makeText(context, "OTP Sent successfully via WhatsApp!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("send_otp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProPrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSendingOtp
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = ProWhite,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "SEND OTP VIA WHATSAPP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // --- OTP Entry View ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ProWhite),
                        border = BorderStroke(1.dp, ProBorderLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter Verification Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProTextDark
                                )

                                Text(
                                    text = "Edit Number",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProPrimaryPurple,
                                    modifier = Modifier.clickable {
                                        SoundEffects.playClick()
                                        isOtpSent = false
                                        otpCode = ""
                                        showNotification = false
                                    }
                                )
                            }

                            Text(
                                text = "We sent a 4-digit code to +91 $phoneNumber via WhatsApp. Enter it below to authorize login.",
                                fontSize = 11.sp,
                                color = ProTextLabel,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 4) {
                                        otpCode = input
                                    }
                                },
                                placeholder = { Text("4-digit OTP") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "OTP",
                                        tint = ProPrimaryPurple
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProPrimaryPurple,
                                        unfocusedBorderColor = ProBorderMedium,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (timerSeconds > 0) {
                                    Text(
                                        text = "Resend OTP in ${timerSeconds}s",
                                        fontSize = 12.sp,
                                        color = ProTextLabel
                                    )
                                } else {
                                    Text(
                                        text = "Resend OTP via WhatsApp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProPrimaryPurple,
                                        modifier = Modifier.clickable {
                                            SoundEffects.playClick()
                                            coroutineScope.launch {
                                                timerSeconds = 30
                                                generatedOtp = (1000 + (Math.random() * 9000).toInt()).toString()
                                                showNotification = true
                                                Toast.makeText(context, "New verification code triggered!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    SoundEffects.playClick()
                                    if (otpCode.length != 4) {
                                        Toast.makeText(context, "OTP must be 4 digits!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Let them log in if code matches or developers use 1234
                                    if (otpCode == generatedOtp || otpCode == "1234") {
                                        coroutineScope.launch {
                                            isVerifyingOtp = true
                                            delay(1000L) // Beautiful verification delay
                                            SoundEffects.playWin()
                                            loginSuccessComplete = true
                                            delay(1500L) // Celebratory victory pause
                                            onLoginSuccess(phoneNumber)
                                        }
                                    } else {
                                        Toast.makeText(context, "Invalid verification code! Use the code shown in the green WhatsApp popup above.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("verify_otp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProPrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                enabled = otpCode.length == 4 && !isVerifyingOtp
                            ) {
                                if (isVerifyingOtp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = ProWhite,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "VERIFY & CONTINUE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Custom Simulated WhatsApp Message Push Notification (Top Slide-down) ---
        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF128C7E)), // WhatsApp Green
                border = BorderStroke(1.5.dp, Color(0xFF075E54))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small WhatsApp icon emblem
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WhatsApp • BetTac Security",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Now",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your BetTac login verification code is: $generatedOtp. Do not share this OTP with anyone.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
