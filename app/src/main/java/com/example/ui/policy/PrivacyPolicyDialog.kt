package com.example.ui.policy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

object PrivacyPolicyConfig {
    /**
     * Hosted URL for Google Play Console submission and online viewing.
     * When hosting on GitHub Pages, this points directly to your repository's privacy_policy.html.
     */
    const val ONLINE_POLICY_URL = "https://saiyedkhan206.github.io/whiteboard-app/privacy_policy.html"
    const val DEVELOPER_EMAIL = "saiyedkhan206@gmail.com"
    const val APP_PACKAGE_NAME = "com.aistudio.whiteboard.wbdraw"
    const val EFFECTIVE_DATE = "September 27, 2026"
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
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
                                .background(Color(0xFF1565C0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Privacy Policy",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WhiteBoard • v1.0",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
                Spacer(Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(end = 4.dp)
                ) {
                    // Compliance Badge
                    Surface(
                        color = Color(0xFF0D253F),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Google Play Policy & COPPA Compliant",
                                color = Color(0xFFBBDEFB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Key Privacy Highlights",
                                color = Color(0xFFFFB74D),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "• No Account Required: We never ask for your name, email, or phone number.\n" +
                                        "• 100% Local Storage: All boards, drawings, and formulas stay strictly on your device.\n" +
                                        "• AdMob Integration: Uses standard Google AdMob SDK for non-intrusive and rewarded ads.\n" +
                                        "• Privacy-Safe Photo Picker: Zero broad storage permissions needed.",
                                color = Color(0xFFDDDDDD),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    PolicySection(
                        title = "1. Information We Collect",
                        content = "WhiteBoard does not collect personally identifiable information directly. " +
                                "Through Google Mobile Ads (AdMob) and Google Play Services, the app may automatically process:\n" +
                                "• Device identifiers (Google Advertising ID / GAID)\n" +
                                "• Coarse IP-derived geographic location for localized ad delivery\n" +
                                "• Performance logs, app diagnostics, and crash reports"
                    )

                    PolicySection(
                        title = "2. Local Storage of Canvas Creations",
                        content = "Every canvas stroke, diagram, formula, sticky note, and shape you create is stored locally " +
                                "in an encrypted/sandboxed SQLite database on your device. We do not operate cloud servers " +
                                "that upload or store your whiteboard content. When you export to PNG or PDF, the file is created locally."
                    )

                    PolicySection(
                        title = "3. Permissions & Justification",
                        content = "• INTERNET: Required solely to communicate with Google AdMob servers for ad loading and rewarded perks.\n" +
                                "• Photo Picker: Uses Android's zero-permission system photo picker. The app never accesses your full gallery or personal files."
                    )

                    PolicySection(
                        title = "4. Third-Party Services & Google AdMob",
                        content = "We use Google Mobile Ads to support the free development of WhiteBoard. " +
                                "Google may use device identifiers to provide relevant ads and prevent fraud. " +
                                "Read Google's Privacy Policy at:\nhttps://policies.google.com/privacy"
                    )

                    PolicySection(
                        title = "5. Rewarded Ads & Your Choices",
                        content = "Watching rewarded ads is 100% voluntary and unlocks VIP perks (neon pens, custom board textures, HD PDF exports).\n" +
                                "You can reset or delete your advertising identifier at any time:\n" +
                                "Android Settings ➔ Google ➔ Ads ➔ Delete advertising ID."
                    )

                    PolicySection(
                        title = "6. Children's Privacy",
                        content = "WhiteBoard complies with COPPA and Google Play Families Policies. We do not knowingly collect personal data from children under 13."
                    )

                    PolicySection(
                        title = "7. Data Deletion & Retention",
                        content = "You can delete individual boards in the History menu. To delete all local data, clear storage in Android Settings ➔ Apps ➔ WhiteBoard ➔ Clear Storage."
                    )

                    PolicySection(
                        title = "8. Contact Us",
                        content = "Developer: Saiyed Afak Ahmed (JavaGoat)\n" +
                                "Email: ${PrivacyPolicyConfig.DEVELOPER_EMAIL}\n" +
                                "Package: ${PrivacyPolicyConfig.APP_PACKAGE_NAME}\n" +
                                "Effective Date: ${PrivacyPolicyConfig.EFFECTIVE_DATE}"
                    )

                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2C2C2C))
                Spacer(Modifier.height(12.dp))

                // Bottom Buttons (Open Online Policy + Got It)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open Online Policy Button
                    OutlinedButton(
                        onClick = {
                            openOnlinePolicy(context)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Online HTML", fontSize = 13.sp)
                    }

                    // Agree & Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Agree & Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = content,
            color = Color(0xFFCCCCCC),
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
    }
}

/**
 * Opens the external online privacy policy URL in the user's web browser,
 * with graceful fallback to share or toast notification if no browser is installed.
 */
fun openOnlinePolicy(context: Context) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyPolicyConfig.ONLINE_POLICY_URL)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        // Fallback: Share or display notification
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "WhiteBoard Privacy Policy")
                putExtra(Intent.EXTRA_TEXT, "WhiteBoard Privacy Policy:\n${PrivacyPolicyConfig.ONLINE_POLICY_URL}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Open Privacy Policy"))
        } catch (ex: Exception) {
            Toast.makeText(context, "Privacy Policy URL: ${PrivacyPolicyConfig.ONLINE_POLICY_URL}", Toast.LENGTH_LONG).show()
        }
    }
}
