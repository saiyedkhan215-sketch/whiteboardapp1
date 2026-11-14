package com.example.ui.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.BoardEntity
import com.example.data.repository.BoardRepository
import com.example.ui.ads.BannerAdView
import com.example.ui.history.HistoryDialog
import com.example.ui.policy.PrivacyPolicyDialog
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: BoardRepository,
    onNewBoard: () -> Unit,
    onOpenBoard: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val boards by repository.allBoards.collectAsState(initial = emptyList())

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRemoveAdsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Top Settings Icon Button
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(28.dp)
            )
        }

        // Center Content (Title + Buttons)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "WhiteBoard" Title
            Text(
                text = "WhiteBoard",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Developer: Saiyed Afak Ahmed (JavaGoat)",
                color = Color(0xFF64B5F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )

            Spacer(Modifier.height(48.dp))

            // "+ New Board" Button (matches screenshot 1)
            Button(
                onClick = onNewBoard,
                modifier = Modifier
                    .width(260.dp)
                    .height(62.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2F80ED),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "New Board",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // "🕒 History" Button (matches screenshot 1)
            Button(
                onClick = { showHistoryDialog = true },
                modifier = Modifier
                    .width(260.dp)
                    .height(62.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF383838),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "History",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Row: Rate and Share buttons (matches screenshot 1)
            Row(
                modifier = Modifier.width(260.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rate Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF282828))
                        .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(22.dp))
                        .clickable { openPlayStoreForRating(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rate",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Rate",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Share Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF282828))
                        .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(22.dp))
                        .clickable { shareApp(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // "Remove Ads" Pill
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF282828))
                    .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(22.dp))
                    .clickable { showRemoveAdsDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = "Remove Ads",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Remove Ads",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Privacy Policy Link
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPrivacyPolicyDialog = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Privacy Policy",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }

            Spacer(Modifier.height(16.dp))
            BannerAdView(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }

    // History Dialog
    if (showHistoryDialog) {
        HistoryDialog(
            boards = boards,
            onSelectBoard = { boardId ->
                onOpenBoard(boardId)
                showHistoryDialog = false
            },
            onDeleteBoard = { boardId ->
                coroutineScope.launch {
                    repository.deleteBoard(boardId)
                    Toast.makeText(context, "Board deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Remove Ads Dialog
    if (showRemoveAdsDialog) {
        Dialog(onDismissRequest = { showRemoveAdsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Ad-Free WhiteBoard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You are currently running the ad-free Pro version of WhiteBoard with unlimited canvases and export features enabled!",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { showRemoveAdsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it")
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showSettingsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("WhiteBoard v1.3", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Features include freehand drawing, smart shape snapping, formulas, tables, layers, Bohr atomic models, and PDF export.", color = Color.Gray, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))

                    Text("Storage", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Boards saved: ${boards.size}", color = Color.Gray, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))

                    Text("Legal & Compliance", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            showPrivacyPolicyDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy Policy")
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // In-App Privacy Policy Dialog (Google Play Compliance)
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openPlayStoreForRating(context: Context) {
    val packageName = context.packageName
    val activity = context.findActivity()
    val launchContext = activity ?: context

    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        if (activity == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    try {
        launchContext.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            if (activity == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            launchContext.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open Google Play Store", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open Google Play Store", Toast.LENGTH_SHORT).show()
    }
}

private fun shareApp(context: Context) {
    val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val shareText = "Check out WhiteBoard - the ultimate digital canvas with smart drawing, shapes, math formulas, and science diagrams!\n$playStoreUrl"
    val activity = context.findActivity()
    val launchContext = activity ?: context

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "WhiteBoard App")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooser = Intent.createChooser(shareIntent, "Share WhiteBoard").apply {
        if (activity == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    try {
        launchContext.startActivity(chooser)
    } catch (_: Exception) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("WhiteBoard Link", shareText))
            Toast.makeText(context, "Share link copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "No sharing app found on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
