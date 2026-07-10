package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val adHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background-color: transparent;
                    overflow: hidden;
                    width: 100%;
                    height: 100%;
                }
                #ad-wrapper {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    width: 100%;
                    min-height: 100px;
                }
            </style>
        </head>
        <body>
            <div id="ad-wrapper">
                <script type="text/javascript" src="https://pl27495494.effectivecpmnetwork.com/7b/75/20/7b7520b5fbcee0057a71c1aea4d1fda0.js"></script>
            </div>
        </body>
        </html>
    """.trimIndent()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProBorderLight.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = ProWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header for sponsored labeling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProPrimaryPurple.copy(alpha = 0.04f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📢 SPONSORED ADVERTISEMENT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProPrimaryPurple,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ProSecondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Partner",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProTextDark
                    )
                }
            }

            // WebView Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            // Enable all required features for advanced ad serving networks
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(true)
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            
                            // Set layout params explicitly
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            webChromeClient = android.webkit.WebChromeClient()
                            
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    // Let links load in standard browser or handle them internally
                                    return false
                                }
                            }
                            
                            // Load the HTML content with a correct BaseURL matching the script's original domain
                            // This ensures the external Javascript is successfully loaded without domain/referer restrictions
                            loadDataWithBaseURL(
                                "https://pl27495494.effectivecpmnetwork.com",
                                adHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        // Optional updates
                    }
                )
            }
        }
    }
}
