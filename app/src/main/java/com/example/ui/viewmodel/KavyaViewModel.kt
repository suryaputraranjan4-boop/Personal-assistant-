package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.data.db.DynamicSkill
import com.example.data.model.KavyaLanguage
import com.example.data.model.KavyaMood
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiConstants
import com.example.data.model.MessageSender
import com.example.data.service.GeminiApiService
import com.example.data.service.GitHubRepo
import com.example.data.service.GitHubService
import com.example.data.service.SkillManager
import com.example.service.KavyaAccessibilityService
import com.example.service.ScreenCaptureService
import com.example.service.SystemAction
import com.example.service.SystemActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class AssistantState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    HANDS_ACTION,
    ERROR
}

class KavyaViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "KavyaViewModel"
    private val apiService = GeminiApiService()
    private val audioRecorder = AudioRecorder()
    private val audioPlayer = AudioPlayer(application.applicationContext)
    private val skillManager = SkillManager(application.applicationContext)
    private val gitHubService = GitHubService()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentMood = MutableStateFlow(KavyaMood.GENKI)
    val currentMood: StateFlow<KavyaMood> = _currentMood.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(KavyaLanguage.AUTO)
    val selectedLanguage: StateFlow<KavyaLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedModel = MutableStateFlow(GeminiConstants.MODEL_GENERAL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedVoice = MutableStateFlow(GeminiConstants.VOICE_AOEDE)
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to talk with you! ✨")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _assistantName = MutableStateFlow("Kavya")
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _avatarScale = MutableStateFlow(1.0f)
    val avatarScale: StateFlow<Float> = _avatarScale.asStateFlow()

    val recordingAmplitude: StateFlow<Float> = audioRecorder.amplitude
    val playbackAmplitude: StateFlow<Float> = audioPlayer.playbackAmplitude
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val currentPlayingMessageId: StateFlow<String?> = audioPlayer.currentPlayingMessageId

    // Native MediaProjection Screen Capture State
    val isScreenShareActive: StateFlow<Boolean> = ScreenCaptureService.isCapturing
    val latestScreenshotBitmap: StateFlow<Bitmap?> = ScreenCaptureService.latestScreenshotBitmap

    // Kavya Hands Accessibility Harness State
    val isHandsConnected: StateFlow<Boolean> = KavyaAccessibilityService.isServiceActive
    val handsActionStatus: StateFlow<String> = KavyaAccessibilityService.lastActionStatus
    val lastTappedCoordinates: StateFlow<Pair<Float, Float>?> = KavyaAccessibilityService.lastTappedCoordinates

    private val _screenHierarchyText = MutableStateFlow<String?>(null)
    val screenHierarchyText: StateFlow<String?> = _screenHierarchyText.asStateFlow()

    private val _showScreenShareDialog = MutableStateFlow(false)
    val showScreenShareDialog: StateFlow<Boolean> = _showScreenShareDialog.asStateFlow()

    private val _isThinkingMode = MutableStateFlow(false)
    val isThinkingMode: StateFlow<Boolean> = _isThinkingMode.asStateFlow()

    private val _thinkingLevel = MutableStateFlow("low")
    val thinkingLevel: StateFlow<String> = _thinkingLevel.asStateFlow()

    // Dynamic Skills & GitHub Integration States
    val skillsList: StateFlow<List<DynamicSkill>> = skillManager.allSkillsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _githubSearchResults = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val githubSearchResults: StateFlow<List<GitHubRepo>> = _githubSearchResults.asStateFlow()

    private val _isSearchingGithub = MutableStateFlow(false)
    val isSearchingGithub: StateFlow<Boolean> = _isSearchingGithub.asStateFlow()

    private val _isLearningSkill = MutableStateFlow(false)
    val isLearningSkill: StateFlow<Boolean> = _isLearningSkill.asStateFlow()

    init {
        // Observe audio player playback completion to reset state
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                if (!playing && _assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                    _statusText.value = "Ready to talk with you! ✨"
                }
            }
        }

        // Seed default dynamic skills into database
        viewModelScope.launch {
            skillManager.seedDefaultSkillsIfNeeded()
        }

        // Welcome greeting from Kavya
        val welcomeGreeting = _currentMood.value.greeting.replace("Kavya", _assistantName.value)
        val welcomeMsg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = welcomeGreeting,
            modelUsed = GeminiConstants.MODEL_GENERAL
        )
        _messages.value = listOf(welcomeMsg)

        // Automatic voice welcome speech with natural AI voice
        viewModelScope.launch {
            synthesizeAndPlayVoice(welcomeMsg.id, welcomeGreeting)
        }
    }

    fun searchGitHubRepositories(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearchingGithub.value = true
            val result = gitHubService.searchRepositories(query)
            if (result.isSuccess) {
                _githubSearchResults.value = result.getOrThrow()
            } else {
                _errorMessage.value = "GitHub Search: ${result.exceptionOrNull()?.message}"
            }
            _isSearchingGithub.value = false
        }
    }

    fun importSkillFromGitHubRepo(repoFullName: String) {
        viewModelScope.launch {
            _isLearningSkill.value = true
            _statusText.value = "Kavya compiling GitHub skill ($repoFullName)..."
            val result = skillManager.importSkillFromGitHubRepo(repoFullName)
            if (result.isSuccess) {
                val report = result.getOrThrow()
                val confirmText = "I have inspected $repoFullName and compiled a new GitHub skill!\n\n${report.summaryReport} ✨"
                val msg = ChatMessage(
                    sender = MessageSender.KAVYA,
                    text = confirmText,
                    modelUsed = "GitHub Dynamic Skill Engine"
                )
                _messages.value = _messages.value + msg
                if (!_isMuted.value) {
                    synthesizeAndPlayVoice(msg.id, "I have inspected $repoFullName and learned the skill ${report.skill.name}! ✨")
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to import skill from GitHub."
                _errorMessage.value = err
            }
            _isLearningSkill.value = false
            _statusText.value = "Ready to talk with you! ✨"
        }
    }

    fun learnNewSkillFromPrompt(instruction: String) {
        if (instruction.isBlank()) return
        viewModelScope.launch {
            _isLearningSkill.value = true
            _statusText.value = "Kavya learning new capability..."
            val result = skillManager.learnSkillFromInstruction(instruction)
            if (result.isSuccess) {
                val report = result.getOrThrow()
                val confirmText = "Yay! I have learned a new capability:\n\n${report.summaryReport} ✨"
                val msg = ChatMessage(
                    sender = MessageSender.KAVYA,
                    text = confirmText,
                    modelUsed = "Dynamic Skill Engine"
                )
                _messages.value = _messages.value + msg
                if (!_isMuted.value) {
                    synthesizeAndPlayVoice(msg.id, "I have learned a new skill: ${report.skill.name}! ✨")
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to learn skill."
                _errorMessage.value = err
            }
            _isLearningSkill.value = false
            _statusText.value = "Ready to talk with you! ✨"
        }
    }

    fun deleteSkill(skill: DynamicSkill) {
        viewModelScope.launch {
            skillManager.deleteSkill(skill)
        }
    }

    fun openScreenShareDialog() {
        refreshScreenShare()
        _showScreenShareDialog.value = true
    }

    fun closeScreenShareDialog() {
        _showScreenShareDialog.value = false
    }

    fun openAccessibilitySettings() {
        SystemActionExecutor.openAccessibilitySettings(getApplication())
    }

    fun refreshScreenShare() {
        executeHandsGesture(SystemAction.ReadScreen)
    }

    fun setAssistantName(name: String) {
        if (name.isNotBlank()) {
            _assistantName.value = name.trim()
        }
    }

    fun setAvatarScale(scale: Float) {
        _avatarScale.value = scale
    }

    fun onScreenSharePermissionGranted(resultCode: Int, data: Intent) {
        ScreenCaptureService.start(getApplication(), resultCode, data)
        val text = when (_selectedLanguage.value) {
            KavyaLanguage.HINDI -> "स्क्रीन शेयरिंग सक्रिय हो गई है! अब मैं आपकी स्क्रीन लाइव देख सकती हूँ। 🌸"
            KavyaLanguage.HINGLISH -> "Screen sharing on ho gayi hai! Ab main aapki screen live dekh sakti hoon~ ✨"
            else -> "Android Screen Sharing is now active! I can see and inspect your screen live! 🖥️✨"
        }
        val msg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = text,
            modelUsed = "Android MediaProjection"
        )
        _messages.value = _messages.value + msg
        if (!_isMuted.value) {
            viewModelScope.launch {
                synthesizeAndPlayVoice(msg.id, text)
            }
        }
    }

    fun onScreenSharePermissionDenied() {
        val text = when (_selectedLanguage.value) {
            KavyaLanguage.HINDI -> "स्क्रीन शेयरिंग अनुमति रद्द कर दी गई। 🌸"
            KavyaLanguage.HINGLISH -> "Screen share cancel ho gaya. Aap jab chahein Screen Share button se allow kar sakte hain~"
            else -> "Screen sharing permission was cancelled or not granted."
        }
        val msg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = text,
            modelUsed = "System"
        )
        _messages.value = _messages.value + msg
    }

    fun stopScreenShare() {
        ScreenCaptureService.stop(getApplication())
        val text = when (_selectedLanguage.value) {
            KavyaLanguage.HINDI -> "स्क्रीन शेयरिंग बंद कर दी गई है। 🌸"
            KavyaLanguage.HINGLISH -> "Screen sharing stop kar di gayi hai~"
            else -> "Screen sharing has been stopped."
        }
        val msg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = text,
            modelUsed = "System"
        )
        _messages.value = _messages.value + msg
    }

    fun analyzeActiveScreen(userQuestion: String = "Explain what is on my screen") {
        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _statusText.value = "${_assistantName.value} is looking..."

            val screenshotBytes = ScreenCaptureService.getLatestScreenshotBytes()
            if (screenshotBytes != null) {
                val visionResult = withTimeoutOrNull(20000) {
                    apiService.analyzeScreen(
                        imageBytes = screenshotBytes,
                        userQuestion = userQuestion,
                        mood = _currentMood.value,
                        language = _selectedLanguage.value,
                        assistantName = _assistantName.value
                    )
                }

                if (visionResult != null && visionResult.isSuccess) {
                    val analysis = visionResult.getOrThrow()
                    val msg = ChatMessage(
                        sender = MessageSender.KAVYA,
                        text = "👁️ Screen Vision Analysis:\n\n$analysis",
                        modelUsed = "Gemini Vision"
                    )
                    _messages.value = _messages.value + msg

                    if (!_isMuted.value) {
                        synthesizeAndPlayVoice(msg.id, analysis)
                    } else {
                        _assistantState.value = AssistantState.IDLE
                        _statusText.value = "Ready to talk with you! ✨"
                    }
                    return@launch
                }
            }

            // Fallback: Use Accessibility hierarchy if bitmap is not yet captured
            refreshScreenShare()
            val hierarchy = _screenHierarchyText.value
            val prompt = if (!hierarchy.isNullOrBlank()) {
                "Active Screen Hierarchy:\n$hierarchy\n\nQuestion: $userQuestion"
            } else {
                userQuestion
            }
            processAssistantReply(prompt, null)
        }
    }

    fun startListening(): Boolean {
        audioPlayer.stop()
        val success = audioRecorder.startRecording(viewModelScope)
        if (success) {
            _assistantState.value = AssistantState.LISTENING
            _statusText.value = "Listening in ${_selectedLanguage.value.shortLabel}..."
            _errorMessage.value = null
        } else {
            _assistantState.value = AssistantState.ERROR
            _errorMessage.value = "Could not access microphone. Please check permissions."
        }
        return success
    }

    fun stopListeningAndProcess() {
        if (!isRecording.value) return

        val wavBytes = audioRecorder.stopRecording()
        if (wavBytes.size < 1000) {
            _assistantState.value = AssistantState.IDLE
            _statusText.value = "Ready to talk with you! ✨"
            return
        }

        viewModelScope.launch {
            try {
                _assistantState.value = AssistantState.TRANSCRIBING
                _statusText.value = "Hearing your voice (${_selectedLanguage.value.shortLabel})..."

                val transcribeResult = withTimeoutOrNull(14000) {
                    apiService.transcribeAudio(wavBytes)
                }

                if (transcribeResult != null && transcribeResult.isSuccess) {
                    val userText = transcribeResult.getOrThrow()
                    val userMsg = ChatMessage(
                        sender = MessageSender.USER,
                        text = userText,
                        isTranscribedFromVoice = true
                    )
                    _messages.value = _messages.value + userMsg

                    processAssistantReply(userText, null)
                } else {
                    val err = transcribeResult?.exceptionOrNull()?.message ?: "Voice recognition timeout"
                    _assistantState.value = AssistantState.ERROR
                    _errorMessage.value = "Voice recognition: $err. Please speak again or type."
                    _statusText.value = "Ready to talk with you! ✨"
                }
            } catch (e: Exception) {
                Log.e(tag, "Voice processing failed", e)
                _assistantState.value = AssistantState.ERROR
                _errorMessage.value = "Could not transcribe audio: ${e.message}"
                _statusText.value = "Ready to talk with you! ✨"
            }
        }
    }

    fun cancelListening() {
        audioRecorder.cancelRecording()
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "Ready to talk with you! ✨"
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        audioPlayer.stop()

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = text.trim()
        )
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            processAssistantReply(text.trim(), null)
        }
    }

    private suspend fun processAssistantReply(userText: String, audioBytes: ByteArray?) {
        // Step 1: Check if prompt asks to learn a skill directly
        val lowerText = userText.lowercase()
        if (lowerText.startsWith("learn skill:") || lowerText.startsWith("learn to ") || lowerText.startsWith("acquire skill:")) {
            val instruction = userText.replace(Regex("^(learn skill:|learn to|acquire skill:)", RegexOption.IGNORE_CASE), "").trim()
            learnNewSkillFromPrompt(instruction)
            return
        }

        // Step 2: Check if this prompt is an immediate device action (YouTube search, touch, scroll, etc.)
        val systemAction = SystemActionExecutor.parseSystemAction(userText)
        if (systemAction != null) {
            _assistantState.value = AssistantState.HANDS_ACTION
            _statusText.value = "Kavya executing action..."

            val actionResult = SystemActionExecutor.execute(systemAction, _selectedLanguage.value, getApplication())
            if (actionResult.screenContentSummary != null) {
                _screenHierarchyText.value = actionResult.screenContentSummary
            }

            val replyText = if (actionResult.screenContentSummary != null) {
                "${actionResult.spokenConfirmation}\n\n${actionResult.screenContentSummary}"
            } else {
                actionResult.spokenConfirmation
            }

            val kavyaMsg = ChatMessage(
                sender = MessageSender.KAVYA,
                text = replyText,
                modelUsed = "Kavya System Harness"
            )
            _messages.value = _messages.value + kavyaMsg

            if (!_isMuted.value) {
                synthesizeAndPlayVoice(kavyaMsg.id, actionResult.spokenConfirmation)
            } else {
                _assistantState.value = AssistantState.IDLE
                _statusText.value = "Ready to talk with you! ✨"
            }
            return
        }

        // Step 3: Check if user prompt matches a custom learned skill or GitHub skill
        val matchedSkill = skillManager.findMatchingSkill(userText)
        if (matchedSkill != null && matchedSkill.executionType != "SYSTEM_ACTION") {
            _assistantState.value = AssistantState.THINKING
            _statusText.value = "Kavya executing skill '${matchedSkill.name}'..."
            val execResult = skillManager.executeSkill(matchedSkill, userText, _assistantName.value, _selectedLanguage.value)
            if (execResult.isSuccess) {
                val reply = execResult.getOrThrow()
                val kavyaMsg = ChatMessage(
                    sender = MessageSender.KAVYA,
                    text = reply,
                    modelUsed = "Dynamic Skill Engine: ${matchedSkill.name}"
                )
                _messages.value = _messages.value + kavyaMsg
                if (!_isMuted.value) {
                    synthesizeAndPlayVoice(kavyaMsg.id, reply)
                } else {
                    _assistantState.value = AssistantState.IDLE
                    _statusText.value = "Ready to talk with you! ✨"
                }
                return
            }
        }

        // Step 4: Intelligent Thinking & Dynamic Skills Conversational Response
        _assistantState.value = AssistantState.THINKING
        
        val effectiveModel = if (_isThinkingMode.value) GeminiConstants.MODEL_THINKING else _selectedModel.value
        _statusText.value = if (_isThinkingMode.value) "Kavya is reasoning deeply..." else "Kavya is thinking..."

        val skillsAddendum = skillManager.buildSkillsSystemPromptAddendum()

        // FAST PATH: One-step voice generation if thinking mode is off
        if (!_isThinkingMode.value && !_isMuted.value) {
            val voiceResult = withTimeoutOrNull(25000) {
                apiService.generateVoiceReply(
                    userPrompt = userText,
                    audioBytes = audioBytes,
                    history = _messages.value,
                    mood = _currentMood.value,
                    language = _selectedLanguage.value,
                    assistantName = _assistantName.value,
                    voiceName = _selectedVoice.value,
                    skillsAddendum = skillsAddendum
                )
            }

            if (voiceResult != null && voiceResult.isSuccess) {
                val (reply, audio) = voiceResult.getOrThrow()
                val kavyaMsg = ChatMessage(
                    sender = MessageSender.KAVYA,
                    text = reply,
                    modelUsed = GeminiConstants.MODEL_LIVE,
                    audioBytes = audio
                )
                _messages.value = _messages.value + kavyaMsg
                playKavyaAudio(kavyaMsg.id, audio)
                return
            }
            Log.w(tag, "One-step voice reply failed or timed out, falling back to two-step.")
        }

        val replyResult = withTimeoutOrNull(35000) {
            apiService.generateReply(
                userPrompt = userText,
                audioBytes = audioBytes,
                history = _messages.value,
                mood = _currentMood.value,
                language = _selectedLanguage.value,
                modelId = effectiveModel,
                assistantName = _assistantName.value,
                thinkingLevel = _thinkingLevel.value,
                skillsAddendum = skillsAddendum
            )
        }

        if (replyResult == null || replyResult.isFailure) {
            val err = replyResult?.exceptionOrNull()?.message ?: "Request timed out"
            _assistantState.value = AssistantState.ERROR
            _errorMessage.value = "AI Service Error: $err"
            _statusText.value = "Ready to talk with you! ✨"
            val errorNotice = "I encountered an error connecting to Gemini ($err). Please check your internet connection or API settings."
            val kavyaMsg = ChatMessage(
                sender = MessageSender.KAVYA,
                text = errorNotice,
                modelUsed = "System"
            )
            _messages.value = _messages.value + kavyaMsg
            return
        }

        val rawReply = replyResult.getOrThrow()

        // Check if Gemini or user prompt requested a hands action (e.g. YouTube search, app opening, gestures)
        val detectedAction = SystemActionExecutor.parseSystemAction(rawReply)
            ?: SystemActionExecutor.parseSystemAction(userText)
        if (detectedAction != null) {
            val executedResult = SystemActionExecutor.execute(detectedAction, _selectedLanguage.value, getApplication())
            if (executedResult.screenContentSummary != null) {
                _screenHierarchyText.value = executedResult.screenContentSummary
            }
        }

        val cleanReply = rawReply
            .replace(Regex("\\[ACTION:[^\\]]+\\]", RegexOption.IGNORE_CASE), "")
            .trim()

        val kavyaMsg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = cleanReply,
            modelUsed = _selectedModel.value
        )
        _messages.value = _messages.value + kavyaMsg

        // Step 4: Automatic Voice Response with natural AI voice
        if (!_isMuted.value) {
            synthesizeAndPlayVoice(kavyaMsg.id, cleanReply)
        } else {
            _assistantState.value = AssistantState.IDLE
            _statusText.value = "Ready to talk with you! ✨"
        }
    }

    /**
     * Synthesizes and plays natural AI voice using the official Gemini TTS model.
     * Completely free of robotic Android TTS to ensure high-quality natural female audio.
     */
    private suspend fun synthesizeAndPlayVoice(messageId: String, textToSpeak: String) {
        if (_isMuted.value) {
            _assistantState.value = AssistantState.IDLE
            _statusText.value = "Ready to talk with you! ✨"
            return
        }

        _assistantState.value = AssistantState.SPEAKING
        _statusText.value = "Kavya is speaking ♫"

        val ttsResult = withTimeoutOrNull(15000) {
            apiService.synthesizeSpeech(textToSpeak, _selectedVoice.value)
        }

        if (ttsResult != null && ttsResult.isSuccess) {
            val speechBytes = ttsResult.getOrThrow()
            _messages.value = _messages.value.map {
                if (it.id == messageId) it.copy(audioBytes = speechBytes) else it
            }
            playKavyaAudio(messageId, speechBytes)
            return
        }

        val err = ttsResult?.exceptionOrNull()?.message ?: "Voice synthesis timeout or unknown error"
        Log.w(tag, "Gemini cloud TTS failed: $err")
        _errorMessage.value = "Voice Error: $err"
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "Ready to talk with you! ✨"
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(isSpeaking = false) else it
        }
    }

    fun playKavyaAudio(messageId: String, audioBytes: ByteArray) {
        _assistantState.value = AssistantState.SPEAKING
        _statusText.value = "Kavya is speaking ♫"
        _messages.value = _messages.value.map {
            it.copy(isSpeaking = it.id == messageId)
        }
        audioPlayer.playAudio(
            audioBytes = audioBytes,
            messageId = messageId,
            coroutineScope = viewModelScope,
            onCompletion = {
                _assistantState.value = AssistantState.IDLE
                _statusText.value = "Ready to talk with you! ✨"
                _messages.value = _messages.value.map {
                    if (it.id == messageId) it.copy(isSpeaking = false) else it
                }
            }
        )
    }

    fun stopSpeaking() {
        audioPlayer.stop()
        if (_assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.IDLE
            _statusText.value = "Ready to talk with you! ✨"
        }
        _messages.value = _messages.value.map {
            it.copy(isSpeaking = false)
        }
    }

    fun executeHandsGesture(action: SystemAction) {
        viewModelScope.launch {
            _assistantState.value = AssistantState.HANDS_ACTION
            val result = SystemActionExecutor.execute(action, _selectedLanguage.value, getApplication())
            if (result.screenContentSummary != null) {
                _screenHierarchyText.value = result.screenContentSummary
            }

            val replyText = if (result.screenContentSummary != null) {
                "${result.spokenConfirmation}\n\n${result.screenContentSummary}"
            } else {
                result.spokenConfirmation
            }

            val systemMsg = ChatMessage(
                sender = MessageSender.KAVYA,
                text = replyText,
                modelUsed = "Kavya System Harness"
            )
            _messages.value = _messages.value + systemMsg

            if (!_isMuted.value) {
                synthesizeAndPlayVoice(systemMsg.id, result.spokenConfirmation)
            } else {
                _assistantState.value = AssistantState.IDLE
                _statusText.value = "Ready to talk with you! ✨"
            }
        }
    }

    fun setMood(mood: KavyaMood) {
        _currentMood.value = mood
        val greeting = mood.greeting
        val moodMsg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = "Mood changed to ${mood.title}! $greeting",
            modelUsed = _selectedModel.value
        )
        _messages.value = _messages.value + moodMsg

        if (!_isMuted.value) {
            viewModelScope.launch {
                synthesizeAndPlayVoice(moodMsg.id, greeting)
            }
        }
    }

    fun setLanguage(language: KavyaLanguage) {
        _selectedLanguage.value = language
        val confirmMsg = when (language) {
            KavyaLanguage.AUTO -> "Language set to Auto-Detect! Aap Hindi, English, ya Hinglish mein baat kar sakte hain~ ✨"
            KavyaLanguage.HINGLISH -> "Bilkul! Ab hum Hinglish mein baat karenge~ Bataiye kya haal chaal? 🌸"
            KavyaLanguage.HINDI -> "नमस्ते! अब मैं आपसे हिंदी में बात करूँगी~ आप कैसे हैं? 🌸"
            KavyaLanguage.ENGLISH -> "Understood! I'll speak with you in English now~ What's on your mind? ✨"
        }
        val msg = ChatMessage(
            sender = MessageSender.KAVYA,
            text = confirmMsg,
            modelUsed = _selectedModel.value
        )
        _messages.value = _messages.value + msg

        if (!_isMuted.value) {
            viewModelScope.launch {
                synthesizeAndPlayVoice(msg.id, confirmMsg)
            }
        }
    }

    fun setSelectedModel(modelId: String) {
        _selectedModel.value = modelId
    }

    fun setSelectedVoice(voiceName: String) {
        _selectedVoice.value = voiceName
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        if (newMuted) {
            audioPlayer.stop()
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun setThinkingMode(enabled: Boolean) {
        _isThinkingMode.value = enabled
    }

    fun setThinkingLevel(level: String) {
        _thinkingLevel.value = level
    }

    fun clearChat() {
        audioPlayer.stop()
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.KAVYA,
                text = _currentMood.value.greeting,
                modelUsed = _selectedModel.value
            )
        )
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "Ready to talk with you! ✨"
    }

    fun replayMessage(message: ChatMessage) {
        if (message.audioBytes != null && message.audioBytes.isNotEmpty()) {
            playKavyaAudio(message.id, message.audioBytes)
        } else {
            viewModelScope.launch {
                synthesizeAndPlayVoice(message.id, message.text)
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        audioRecorder.cancelRecording()
    }
}
