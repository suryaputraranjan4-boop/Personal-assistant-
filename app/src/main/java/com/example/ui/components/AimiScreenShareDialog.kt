package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SystemAction
import com.example.ui.viewmodel.AimiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AimiScreenShareDialog(
    viewModel: AimiViewModel,
    onRequestNativeScreenShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHandsConnected by viewModel.isHandsConnected.collectAsState()
    val isScreenShareActive by viewModel.isScreenShareActive.collectAsState()
    val screenHierarchyText by viewModel.screenHierarchyText.collectAsState()
    val actionStatus by viewModel.handsActionStatus.collectAsState()

    var ytSearchQuery by remember { mutableStateOf("") }
    var targetElementName by remember { mutableStateOf("") }
    var touchTestCoords by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var touchTestMessage by remember { mutableStateOf("Tap anywhere inside this box to test screen touch") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("aimi_screen_share_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00B4D8), Color(0xFF6C5CE7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                            contentDescription = "Screen Share Option",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aimi Screen Share & Control",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Native Android screen share, visual sense & automation",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_screen_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Screen Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 1: NATIVE ANDROID SCREEN-SHARING CONSENT FLOW (MediaProjection)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isScreenShareActive) Color(0xFF00E676).copy(alpha = 0.12f)
                    else Color(0xFF00B4D8).copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isScreenShareActive) Icons.AutoMirrored.Filled.ScreenShare else Icons.AutoMirrored.Filled.StopScreenShare,
                                contentDescription = null,
                                tint = if (isScreenShareActive) Color(0xFF00C853) else Color(0xFF0096C7),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isScreenShareActive) "Native Screen Sharing: LIVE" else "Android Screen Sharing Consent",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isScreenShareActive) "Aimi is capturing and visually analyzing live display frames."
                                    else "Tap below to open Android's native screen share permission dialog.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isScreenShareActive) {
                        Button(
                            onClick = {
                                onRequestNativeScreenShare()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0096C7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_native_screen_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Screen Share Permission 🖥️", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.analyzeActiveScreen("Look at my screen and describe what is visible in detail.")
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("analyze_screen_vision_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze with Vision 📸", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.stopScreenShare() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("stop_screen_share_btn")
                            ) {
                                Text("Stop Share", fontSize = 12.sp, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: ACCESSIBILITY SERVICE & TOUCH STATUS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHandsConnected) Color(0xFF00E676).copy(alpha = 0.10f)
                    else Color(0xFFFFB300).copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isHandsConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isHandsConnected) Color(0xFF00C853) else Color(0xFFFF8F00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHandsConnected) "Screen Touch & Accessibility: Active" else "Screen Control: Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isHandsConnected) actionStatus else "Enable 'Aimi AI' in Accessibility for simulated screen touch & scan.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (!isHandsConnected) {
                        Button(
                            onClick = { viewModel.openAccessibilitySettings() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF8F00)
                            ),
                            modifier = Modifier.testTag("enable_accessibility_screen_share_btn")
                        ) {
                            Text("Turn On", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: YOUTUBE AUTOMATION & SEARCH
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF0000).copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YouTube Automation & Search",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Opens YouTube and searches automatically",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ytSearchQuery,
                            onValueChange = { ytSearchQuery = it },
                            placeholder = { Text("e.g. Hindi songs, cute anime, tutorials", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("yt_search_query_input")
                        )

                        Button(
                            onClick = {
                                if (ytSearchQuery.isNotBlank()) {
                                    val q = ytSearchQuery.trim()
                                    ytSearchQuery = ""
                                    viewModel.executeHandsGesture(SystemAction.YouTubeSearch(q))
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000)
                            ),
                            modifier = Modifier.testTag("launch_yt_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 4: SCREEN SENSE HIERARCHY & INSPECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF6C5CE7).copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                                contentDescription = null,
                                tint = Color(0xFF6C5CE7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen Sense Hierarchy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { viewModel.refreshScreenShare() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6C5CE7)
                            ),
                            modifier = Modifier.testTag("scan_screen_hierarchy_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Screen", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 160.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        val displayText = if (!screenHierarchyText.isNullOrBlank()) {
                            screenHierarchyText!!
                        } else {
                            "No scan yet. Tap 'Scan Screen' to inspect on-screen hierarchy!"
                        }
                        Text(
                            text = displayText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.analyzeActiveScreen("Screen share: Screen par kya hai? Read and explain the active screen.")
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_aimi_about_screen_btn")
                    ) {
                        Text("Ask Aimi to Explain This Screen 🌸", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 5: PRECISE TOUCH TARGET SIMULATION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Touch Element by Name",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aimi scans screen nodes to find and tap the exact matching button or label",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetElementName,
                            onValueChange = { targetElementName = it },
                            placeholder = { Text("e.g. Subscribe, Like, Next, Install", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("target_element_input")
                        )

                        Button(
                            onClick = {
                                if (targetElementName.isNotBlank()) {
                                    val t = targetElementName.trim()
                                    targetElementName = ""
                                    viewModel.executeHandsGesture(SystemAction.ClickTarget(t))
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            modifier = Modifier.testTag("tap_target_element_btn")
                        ) {
                            Text("Tap", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 6: INTERACTIVE TOUCH TEST PAD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF6C5CE7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Touch Screen Test Pad",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Verify touch responsiveness and coordinate accuracy directly on device",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6C5CE7).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        touchTestCoords = Pair(offset.x, offset.y)
                                        touchTestMessage = "Touch OK! Tap at X=${offset.x.toInt()}, Y=${offset.y.toInt()}"
                                    },
                                    onDoubleTap = { offset ->
                                        touchTestCoords = Pair(offset.x, offset.y)
                                        touchTestMessage = "Double Tap OK! X=${offset.x.toInt()}, Y=${offset.y.toInt()}"
                                    },
                                    onLongPress = { offset ->
                                        touchTestCoords = Pair(offset.x, offset.y)
                                        touchTestMessage = "Long Press OK! Held at X=${offset.x.toInt()}, Y=${offset.y.toInt()}"
                                    }
                                )
                            }
                            .testTag("interactive_touch_test_pad"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = touchTestMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6C5CE7)
                            )
                            if (touchTestCoords != null) {
                                Text(
                                    text = "Screen Coordinates: (${touchTestCoords!!.first.toInt()} px, ${touchTestCoords!!.second.toInt()} px)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 7: GLOBAL NAVIGATION & SYSTEM ACTIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.Home)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_home_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Home", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.Back)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_back_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⬅️ Back", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.ScrollDown)
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_scroll_down_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scroll ⬇️", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.Recents)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_recents_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📱 Recents", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.Notifications)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_notifications_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔔 Alerts", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.executeHandsGesture(SystemAction.Screenshot)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).testTag("screen_share_screenshot_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📸 Shot", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
