package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.SystemAction
import com.example.ui.components.AimiAvatarView
import com.example.ui.components.AimiScreenShareDialog
import com.example.ui.components.AimiSettingsSheet
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ChatTranscriptView
import com.example.ui.viewmodel.AimiViewModel
import com.example.ui.viewmodel.AssistantState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AimiMainScreen(
    viewModel: AimiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val recordingAmp by viewModel.recordingAmplitude.collectAsState()
    val playbackAmp by viewModel.playbackAmplitude.collectAsState()
    val currentMood by viewModel.currentMood.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isHandsConnected by viewModel.isHandsConnected.collectAsState()
    val handsActionStatus by viewModel.handsActionStatus.collectAsState()
    val showScreenShareDialog by viewModel.showScreenShareDialog.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showHandsPanel by remember { mutableStateOf(false) }
    var localShowScreenShare by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onScreenSharePermissionGranted(result.resultCode, result.data!!)
        } else {
            viewModel.onScreenSharePermissionDenied()
        }
    }

    // Audio recording permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Mic button pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6C5CE7), Color(0xFFFF4081))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "愛",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aimi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "愛美",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD81B60),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${selectedLanguage.shortLabel} • ${currentMood.title.split(" ").first()} • $selectedVoice",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    // Dedicated Screen Share Option
                    IconButton(
                        onClick = {
                            viewModel.refreshScreenShare()
                            localShowScreenShare = true
                        },
                        modifier = Modifier.testTag("open_screen_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                            contentDescription = "Screen Share Option",
                            tint = Color(0xFF00B4D8)
                        )
                    }

                    // Aimi Hands & System Harness Trigger
                    IconButton(
                        onClick = { showHandsPanel = true },
                        modifier = Modifier.testTag("open_hands_panel_btn")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = "Aimi Hands & System Harness",
                                tint = if (isHandsConnected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isHandsConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Quick Language Switcher
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00B4D8).copy(alpha = 0.15f),
                        modifier = Modifier
                            .clickable {
                                val allLangs = com.example.data.model.AimiLanguage.values()
                                val nextIndex = (allLangs.indexOf(selectedLanguage) + 1) % allLangs.size
                                viewModel.setLanguage(allLangs[nextIndex])
                            }
                            .testTag("quick_language_toggle_btn")
                    ) {
                        Text(
                            text = selectedLanguage.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0096C7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Mute / Unmute
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.testTag("mute_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Aimi" else "Mute Aimi",
                            tint = if (isMuted) Color(0xFFFF5252) else Color(0xFF6C5CE7)
                        )
                    }

                    // Reset conversation
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("reset_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Bottom control dock with glowing anime mic and text input
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Quick suggestion chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestionPrompts = listOf(
                            "Screen share 🖥️",
                            "Open YouTube and search songs 🎬",
                            "Scroll down 📜",
                            "Long press and scroll up 🚀",
                            "Screen par kya hai? 📱",
                            "Kaise ho Aimi? 🌸",
                            "Tap on Settings ⚙️",
                            "Scroll up 📜",
                            "Daba ke upar drag karo ✨",
                            "Go Home 🏠",
                            "Recent apps 📱",
                            "Tell me a cute anime joke! 🎀"
                        )
                        suggestionPrompts.forEach { prompt ->
                            AssistChip(
                                onClick = {
                                    if (prompt.startsWith("Screen share")) {
                                        viewModel.refreshScreenShare()
                                        localShowScreenShare = true
                                    } else {
                                        viewModel.sendTextMessage(prompt)
                                    }
                                },
                                label = { Text(prompt, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (prompt.startsWith("Screen share")) Color(0xFF00B4D8).copy(alpha = 0.2f)
                                    else if (prompt.contains("YouTube")) Color(0xFFFF0000).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("suggestion_chip_$prompt")
                            )
                        }
                    }

                    // Bottom Bar Controls: Mic Button & Text Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Big Anime Mic Button with Pulsing Effect
                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                                )
                            }

                            FloatingActionButton(
                                onClick = {
                                    if (isRecording) {
                                        viewModel.stopListeningAndProcess()
                                    } else {
                                        if (hasMicPermission) {
                                            viewModel.startListening()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                containerColor = if (isRecording) Color(0xFF00E5FF) else Color(0xFF6C5CE7),
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("voice_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop listening" else "Start talking to Aimi",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Text Field Input
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    if (isRecording) "Listening to you..." else "Type or tap mic to speak...",
                                    fontSize = 14.sp
                                )
                            },
                            trailingIcon = {
                                if (textInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val messageToSend = textInput.trim()
                                            textInput = ""
                                            keyboardController?.hide()
                                            viewModel.sendTextMessage(messageToSend)
                                        },
                                        modifier = Modifier.testTag("send_text_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send message",
                                            tint = Color(0xFF6C5CE7)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        val messageToSend = textInput.trim()
                                        textInput = ""
                                        keyboardController?.hide()
                                        viewModel.sendTextMessage(messageToSend)
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Anime Character Stage
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("avatar_stage_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeAmplitude = if (isRecording) recordingAmp else if (isPlaying) playbackAmp else 0.05f

                    // Animated Avatar View
                    AimiAvatarView(
                        assistantState = assistantState,
                        amplitude = activeAmplitude,
                        size = 150.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio Waveform Equalizer Bar
                    AudioWaveformBar(
                        amplitude = activeAmplitude,
                        barCount = 14,
                        height = 22.dp,
                        barWidth = 4.dp,
                        activeColorStart = if (isRecording) Color(0xFF00E5FF) else Color(0xFF6C5CE7),
                        activeColorEnd = if (isRecording) Color(0xFF76FF03) else Color(0xFFFF4081)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status pill banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (assistantState) {
                            AssistantState.LISTENING -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                            AssistantState.TRANSCRIBING -> Color(0xFF76FF03).copy(alpha = 0.15f)
                            AssistantState.THINKING -> Color(0xFFFFD600).copy(alpha = 0.15f)
                            AssistantState.SPEAKING -> Color(0xFFFF4081).copy(alpha = 0.15f)
                            AssistantState.HANDS_ACTION -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                            AssistantState.ERROR -> Color(0xFFFF5252).copy(alpha = 0.15f)
                            AssistantState.IDLE -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.testTag("status_pill_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            val statusDotColor = when (assistantState) {
                                AssistantState.LISTENING -> Color(0xFF00E5FF)
                                AssistantState.TRANSCRIBING -> Color(0xFF76FF03)
                                AssistantState.THINKING -> Color(0xFFFFD600)
                                AssistantState.SPEAKING -> Color(0xFFFF4081)
                                AssistantState.HANDS_ACTION -> Color(0xFF00E5FF)
                                AssistantState.ERROR -> Color(0xFFFF5252)
                                AssistantState.IDLE -> Color(0xFF6C5CE7)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Quick stop button when talking
                            if (isPlaying || isRecording) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (isRecording) viewModel.cancelListening()
                                        if (isPlaying) viewModel.stopSpeaking()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color(0xFFFF4081),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Direct Stage Shortcuts: Screen Share, YouTube & Touch Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00B4D8).copy(alpha = 0.15f),
                            modifier = Modifier
                                .clickable {
                                    viewModel.refreshScreenShare()
                                    localShowScreenShare = true
                                }
                                .testTag("stage_screen_share_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                                    contentDescription = null,
                                    tint = Color(0xFF0096C7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Screen Share 🖥️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0096C7)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF0000).copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable {
                                    viewModel.sendTextMessage("Open YouTube")
                                }
                                .testTag("stage_youtube_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "YouTube 🎬",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isHandsConnected) Color(0xFF00E676).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable { showHandsPanel = true }
                                .testTag("hands_harness_quick_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PanTool,
                                    contentDescription = null,
                                    tint = if (isHandsConnected) Color(0xFF00C853) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHandsConnected) "Hands ON ✨" else "Touch 👆",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isHandsConnected) Color(0xFF00C853) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Chat Transcript List
            ChatTranscriptView(
                messages = messages,
                onReplayAudio = { message -> viewModel.replayMessage(message) },
                onStopAudio = { viewModel.stopSpeaking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (localShowScreenShare || showScreenShareDialog || showHandsPanel) {
        AimiScreenShareDialog(
            viewModel = viewModel,
            onRequestNativeScreenShare = {
                try {
                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                } catch (e: Exception) {
                    viewModel.sendTextMessage("Could not launch screen capture dialog: ${e.message}")
                }
            },
            onDismiss = {
                localShowScreenShare = false
                showHandsPanel = false
                viewModel.closeScreenShareDialog()
            }
        )
    }

    if (showSettings) {
        AimiSettingsSheet(
            assistantName = assistantName,
            onNameChanged = { viewModel.setAssistantName(it) },
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { viewModel.setLanguage(it) },
            selectedModel = selectedModel,
            onModelSelected = { viewModel.setSelectedModel(it) },
            selectedVoice = selectedVoice,
            onVoiceSelected = { viewModel.setSelectedVoice(it) },
            currentMood = currentMood,
            onMoodSelected = { viewModel.setMood(it) },
            onClearChat = { viewModel.clearChat() },
            onDismiss = { showSettings = false }
        )
    }
}
