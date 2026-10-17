package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.KavyaAvatarView
import com.example.ui.components.KavyaScreenShareDialog
import com.example.ui.components.KavyaSettingsSheet
import com.example.ui.viewmodel.KavyaViewModel
import com.example.ui.viewmodel.AssistantState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavyaMainScreen(
    viewModel: KavyaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val recordingAmp by viewModel.recordingAmplitude.collectAsState()
    val playbackAmp by viewModel.playbackAmplitude.collectAsState()
    val assistantName by viewModel.assistantName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showScreenShareDialog by viewModel.showScreenShareDialog.collectAsState()
    val avatarScale by viewModel.avatarScale.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
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

    // Dynamic Avatar Animations
    val activeAmplitude = if (isRecording) recordingAmp else if (isPlaying) playbackAmp else 0.02f
    
    val dynamicScale by animateFloatAsState(
        targetValue = avatarScale + (activeAmplitude * 0.5f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "avatar_dynamic_scale"
    )

    val shakeOffset by animateFloatAsState(
        targetValue = if (activeAmplitude > 0.1f) (activeAmplitude * 12f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "avatar_shake"
    )

    val rotationAngle by animateFloatAsState(
        targetValue = if (activeAmplitude > 0.1f) (activeAmplitude * 8f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "avatar_rotation"
    )

    // Background Layer (Glassmorphism + Blur)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C29), // Deep Space Blue
                        Color(0xFF302B63), // Purple Night
                        Color(0xFF24243E)  // Deep Navy
                    )
                )
            )
    ) {
        // Decorative Blurry Background Elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF6C5CE7).copy(alpha = 0.2f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(250.dp)
                .offset(x = 50.dp, y = 50.dp)
                .blur(70.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
        )

        // Main UI Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar - Minimalist Glass View
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Menu (Three Bar) - Access to Sound, Refresh, Resize
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("open_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Center: App Logo / Name (Optional, but user said logo big, so maybe just title here)
                    Text(
                        text = assistantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )

                    // Right: Screen Share (Outside as requested)
                    IconButton(
                        onClick = {
                            viewModel.refreshScreenShare()
                            localShowScreenShare = true
                        },
                        modifier = Modifier.testTag("open_screen_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                            contentDescription = "Screen Share",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Large Reactive Avatar with Shaking/Rotation/Scaling
            Box(
                modifier = Modifier
                    .size(320.dp) // Bigger as requested
                    .offset(y = shakeOffset.dp)
                    .scale(dynamicScale)
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
                    .clickable { 
                        if (isPlaying) viewModel.stopSpeaking() else if (!isRecording) viewModel.startListening()
                    }
                    .testTag("main_avatar_container"),
                contentAlignment = Alignment.Center
            ) {
                // Outer Glow Ring
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                width = (2.dp + (10.dp * activeAmplitude)),
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF6C5CE7), Color(0xFF00E5FF), Color(0xFF6C5CE7))
                                )
                            ),
                            shape = CircleShape
                        )
                        .blur(if (activeAmplitude > 0.2f) (10.dp * activeAmplitude) else 0.dp)
                )

                KavyaAvatarView(
                    assistantState = assistantState,
                    amplitude = activeAmplitude,
                    size = 220.dp,
                    modifier = Modifier.scale(1f) // Animation applied to parent Box
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Latest Response Display (Only one reply)
            val latestReply = messages.lastOrNull { it.sender == com.example.data.model.MessageSender.KAVYA }
            AnimatedVisibility(
                visible = latestReply != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
            ) {
                latestReply?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = msg.text,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Typing / Mic Control
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .imePadding(),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic Button
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "pulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                            )
                        }

                        IconButton(
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
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFF00E5FF) else Color(0xFF6C5CE7))
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                if (isRecording) "Listening..." else "Talk to $assistantName...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendTextMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendTextMessage(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF6C5CE7)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Error SnackBar Layer
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    // Dialogs & Sheets
    if (localShowScreenShare || showScreenShareDialog) {
        KavyaScreenShareDialog(
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
                viewModel.closeScreenShareDialog()
            }
        )
    }

    if (showSettings) {
        val skillsList by viewModel.skillsList.collectAsState()
        val githubSearchResults by viewModel.githubSearchResults.collectAsState()
        val isSearchingGithub by viewModel.isSearchingGithub.collectAsState()

        KavyaSettingsSheet(
            assistantName = assistantName,
            onNameChanged = { viewModel.setAssistantName(it) },
            selectedLanguage = viewModel.selectedLanguage.collectAsState().value,
            onLanguageSelected = { viewModel.setLanguage(it) },
            selectedModel = viewModel.selectedModel.collectAsState().value,
            onModelSelected = { viewModel.setSelectedModel(it) },
            selectedVoice = viewModel.selectedVoice.collectAsState().value,
            onVoiceSelected = { viewModel.setSelectedVoice(it) },
            currentMood = viewModel.currentMood.collectAsState().value,
            onMoodSelected = { viewModel.setMood(it) },
            skillsList = skillsList,
            onLearnNewSkill = { viewModel.learnNewSkillFromPrompt(it) },
            onDeleteSkill = { viewModel.deleteSkill(it) },
            githubSearchResults = githubSearchResults,
            isSearchingGithub = isSearchingGithub,
            onSearchGithub = { viewModel.searchGitHubRepositories(it) },
            onImportGithubSkill = { viewModel.importSkillFromGitHubRepo(it) },
            onClearChat = { viewModel.clearChat() },
            isMuted = viewModel.isMuted.collectAsState().value,
            onToggleMute = { viewModel.toggleMute() },
            isHandsConnected = viewModel.isHandsConnected.collectAsState().value,
            onOpenAccessibility = { viewModel.openAccessibilitySettings() },
            avatarScale = avatarScale,
            onScaleChanged = { viewModel.setAvatarScale(it) },
            isThinkingMode = viewModel.isThinkingMode.collectAsState().value,
            onThinkingModeChanged = { viewModel.setThinkingMode(it) },
            thinkingLevel = viewModel.thinkingLevel.collectAsState().value,
            onThinkingLevelChanged = { viewModel.setThinkingLevel(it) },
            onDismiss = { showSettings = false }
        )
    }
}
