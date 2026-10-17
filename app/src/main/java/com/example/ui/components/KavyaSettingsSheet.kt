package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DynamicSkill
import com.example.data.model.AVAILABLE_MODELS
import com.example.data.model.AVAILABLE_VOICES
import com.example.data.model.KavyaLanguage
import com.example.data.model.KavyaMood
import com.example.data.service.GitHubRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavyaSettingsSheet(
    assistantName: String,
    onNameChanged: (String) -> Unit,
    selectedLanguage: KavyaLanguage,
    onLanguageSelected: (KavyaLanguage) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    selectedVoice: String,
    onVoiceSelected: (String) -> Unit,
    currentMood: KavyaMood,
    onMoodSelected: (KavyaMood) -> Unit,
    skillsList: List<DynamicSkill>,
    onLearnNewSkill: (String) -> Unit,
    onDeleteSkill: (DynamicSkill) -> Unit,
    githubSearchResults: List<GitHubRepo>,
    isSearchingGithub: Boolean,
    onSearchGithub: (String) -> Unit,
    onImportGithubSkill: (String) -> Unit,
    onClearChat: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isHandsConnected: Boolean,
    onOpenAccessibility: () -> Unit,
    avatarScale: Float,
    onScaleChanged: (Float) -> Unit,
    isThinkingMode: Boolean,
    onThinkingModeChanged: (Boolean) -> Unit,
    thinkingLevel: String,
    onThinkingLevelChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Skills & GitHub, 1: Voice & Language, 2: Thinking & Models, 3: System & Identity
    var githubQuery by remember { mutableStateOf("") }
    var newSkillPrompt by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("kavya_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C5CE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kavya AI Studio & Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Self-Learning Engine & GitHub Integration",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                val tabs = listOf("⚡ Skills & GitHub", "🗣️ Voice", "🧠 Thinking", "⚙️ Identity")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index },
                        color = if (isSelected) Color(0xFF6C5CE7) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Skills & GitHub Hub
                    Text(
                        text = "Teach Kavya New Capabilities",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C5CE7)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSkillPrompt,
                            onValueChange = { newSkillPrompt = it },
                            placeholder = { Text("e.g. Learn to summarize web pages") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSkillPrompt.isNotBlank()) {
                                    onLearnNewSkill(newSkillPrompt)
                                    newSkillPrompt = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Learn")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "GitHub Integration & Repository Search",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = githubQuery,
                            onValueChange = { githubQuery = it },
                            placeholder = { Text("Search GitHub repos (e.g. android-ai)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (githubQuery.isNotBlank()) {
                                    onSearchGithub(githubQuery)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSearchingGithub) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (githubSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "GitHub Repositories Found:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        githubSearchResults.forEach { repo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = repo.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = repo.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2)
                                        Text(text = "⭐ ${repo.starsCount} | ${repo.language}", fontSize = 10.sp, color = Color(0xFF6C5CE7))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { onImportGithubSkill(repo.fullName) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Import Skill", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Learned Skills Registry (${skillsList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    skillsList.forEach { skill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF6C5CE7).copy(alpha = 0.10f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (skill.category == "GitHub") Icons.Default.Code else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF6C5CE7),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = skill.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = skill.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Triggers: ${skill.triggerPhrases}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { onDeleteSkill(skill) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Voice & Language Controls
                    Text(
                        text = "Spoken Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    KavyaLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onLanguageSelected(lang) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { onLanguageSelected(lang) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = lang.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Cute Anime Voice (Gemini TTS)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD81B60)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AVAILABLE_VOICES.forEach { voice ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onVoiceSelected(voice.voiceName) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedVoice == voice.voiceName) Color(0xFFFF4081).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedVoice == voice.voiceName, onClick = { onVoiceSelected(voice.voiceName) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = voice.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(text = voice.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Thinking & Models
                    Text(
                        text = "Gemini Reasoning & Thinking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C5CE7)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF6C5CE7).copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Deep Thinking Mode", fontWeight = FontWeight.Bold)
                                    Text(text = "Uses gemini-3.1-pro for complex logic, multi-step planning, and coding.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = isThinkingMode, onCheckedChange = onThinkingModeChanged)
                            }

                            if (isThinkingMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "Thinking Intensity", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("low", "medium", "high").forEach { level ->
                                        val selected = thinkingLevel == level
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { onThinkingLevelChanged(level) },
                                            color = if (selected) Color(0xFF6C5CE7) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = level.replaceFirstChar { it.uppercase() },
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                textAlign = TextAlign.Center,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Primary Intelligence Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    AVAILABLE_MODELS.forEach { model ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onModelSelected(model.modelId) },
                            colors = CardDefaults.cardColors(containerColor = if (selectedModel == model.modelId) Color(0xFF6C5CE7).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedModel == model.modelId, onClick = { onModelSelected(model.modelId) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = model.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(text = model.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: System & Identity
                    Text(text = "Assistant Identity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = assistantName,
                        onValueChange = onNameChanged,
                        label = { Text("Assistant Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Avatar Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Slider(
                        value = avatarScale,
                        onValueChange = onScaleChanged,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenAccessibility() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PanTool, contentDescription = null, tint = if (isHandsConnected) Color(0xFF00E676) else MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Kavya Hands (Device Gesture Control)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = if (isHandsConnected) "Accessibility Service Connected" else "Tap to open Accessibility settings", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isHandsConnected, onCheckedChange = { onOpenAccessibility() })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    FilledTonalButton(
                        onClick = {
                            onClearChat()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Memory & Conversation")
                    }
                }
            }
        }
    }
}
