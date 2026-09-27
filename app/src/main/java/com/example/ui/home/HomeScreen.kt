package com.example.ui.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.outlined.Star
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
import com.example.ui.ads.RewardedAdManager
import com.example.ui.history.HistoryDialog
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

    val isAdLoaded by RewardedAdManager.isAdLoaded.collectAsState()
    val isAdLoading by RewardedAdManager.isLoading.collectAsState()
    val totalRewards by RewardedAdManager.totalRewardsEarned.collectAsState()
    val isVipUnlocked by RewardedAdManager.isVipUnlocked.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showRemoveAdsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var rewardEarnedMessage by remember { mutableStateOf<String?>(null) }

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
            // "WhiteBoard" Title (matches screenshot 1)
            Text(
                text = "WhiteBoard",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(56.dp))

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
                        .clickable { showRateDialog = true },
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
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "WhiteBoard App")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out WhiteBoard - the ultimate digital canvas with smart drawing, shapes, math formulas, and science diagrams!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share WhiteBoard"))
                        },
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

            // Row: Remove Ads and Watch Ad
            Row(
                modifier = Modifier.width(260.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Remove Ads" Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
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
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Remove Ads",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // "Watch Ad" Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isVipUnlocked) Color(0xFF3E2723) else Color(0xFF2D231B))
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        .clickable { showRewardedAdDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Watch Rewarded Ad",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isVipUnlocked) "VIP Active ★" else "Watch Ad 🎁",
                            color = Color(0xFFFFE0B2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (totalRewards > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "★ Rewarded Points: $totalRewards",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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

    // Rate App Dialog
    if (showRateDialog) {
        var userRating by remember { mutableStateOf(5) }
        Dialog(onDismissRequest = { showRateDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rate WhiteBoard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "How would you rate your experience with WhiteBoard?",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            IconButton(onClick = { userRating = i }) {
                                Icon(
                                    imageVector = if (i <= userRating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Star $i",
                                    tint = if (i <= userRating) Color(0xFFFFB300) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Thank you for giving $userRating stars!", Toast.LENGTH_SHORT).show()
                            showRateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Rating", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showRemoveAdsDialog = false
                            showRewardedAdDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Rewarded Ad for VIP Tools")
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

                    Text("WhiteBoard v1.0", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Features include freehand drawing, smart shape snapping, formulas, tables, layers, Bohr atomic models, and PDF export.", color = Color.Gray, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))

                    Text("Storage", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Boards saved: ${boards.size}", color = Color.Gray, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))

                    // Rewarded Test Ad Integration Section
                    Text("Google AdMob Rewarded Test Ad", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Ad Unit: ${RewardedAdManager.TEST_REWARDED_AD_UNIT_ID}", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAdLoaded) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isAdLoaded) "Ad Ready to play ✅" else if (isAdLoading) "Loading test ad..." else "Ready to request ad",
                            color = if (isAdLoaded) Color(0xFF81C784) else Color(0xFFFFB74D),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val activity = RewardedAdManager.findActivity(context)
                            if (activity != null) {
                                RewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardItem ->
                                        rewardEarnedMessage = "🎉 Test Ad Completed! Earned ${rewardItem.amount} ${rewardItem.type} points!"
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Play Rewarded Ad", fontWeight = FontWeight.Bold)
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

    // Rewarded Ad Prompt Dialog
    if (showRewardedAdDialog) {
        Dialog(onDismissRequest = { showRewardedAdDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3E2723)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Watch Rewarded Ad",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Watch a test video ad to earn rewards and unlock VIP WhiteBoard tools:\n\n• Golden & Neon Drawing Pens\n• Blueprint & Grid Canvas Textures\n• Science Diagrams & Bonus Stickers\n• High-Definition PDF Export",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Test Ad Unit: ca-app-pub-3940256099942544/5224354917", color = Color.Gray, fontSize = 10.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isAdLoaded) "Status: Ready to Display ✅" else "Status: Ready (will load ad)",
                                color = if (isAdLoaded) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val activity = RewardedAdManager.findActivity(context)
                            if (activity != null) {
                                RewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardItem ->
                                        showRewardedAdDialog = false
                                        rewardEarnedMessage = "🎉 Awesome! You earned ${rewardItem.amount} ${rewardItem.type}! VIP features are now active!"
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Activity context not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Video Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { showRewardedAdDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Maybe Later", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Celebration Reward Earned Dialog
    if (rewardEarnedMessage != null) {
        Dialog(onDismissRequest = { rewardEarnedMessage = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242B1E)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF81C784)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Reward Earned!",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = rewardEarnedMessage ?: "",
                        color = Color(0xFFE8F5E9),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { rewardEarnedMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
