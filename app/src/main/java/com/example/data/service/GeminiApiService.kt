package com.example.data.service

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.audio.AudioUtils
import com.example.data.model.KavyaLanguage
import com.example.data.model.KavyaMood
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiConstants
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiService {

    private val tag = "GeminiApiService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Fast, responsive OkHttp client with retry support
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Transcribes microphone audio using official multimodal Gemini models.
     */
    suspend fun transcribeAudio(audioBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets."))
        }

        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val transcribeCandidates = listOf(
            GeminiConstants.MODEL_GENERAL,
            GeminiConstants.MODEL_FLASH_35,
            GeminiConstants.MODEL_FAST
        ).distinct()

        var lastError: Exception? = null
        for (candidate in transcribeCandidates) {
            val result = executeTranscriptionCall(candidate, base64Audio, apiKey)
            if (result.isSuccess) {
                return@withContext result
            }
            lastError = result.exceptionOrNull() as? Exception
            Log.w(tag, "Transcribe with $candidate failed: ${result.exceptionOrNull()?.message}, trying next...")
        }

        Result.failure(lastError ?: Exception("Could not transcribe audio after multiple attempts."))
    }

    private fun executeTranscriptionCall(model: String, base64Audio: String, apiKey: String): Result<String> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    "Transcribe the spoken audio verbatim in its original language (Hindi, Hinglish, or English). Output ONLY the transcription text, no preamble or quotes."
                                )
                            })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "audio/wav")
                                    put("data", base64Audio)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 80)
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")?.trim()

            if (text.isNullOrBlank()) {
                return Result.failure(Exception("Transcription was empty."))
            }

            return Result.success(text)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Builds properly structured, strictly alternating conversation turns
     * starting with a user turn as required by the Gemini API.
     */
    private fun buildCleanContents(
        history: List<ChatMessage>,
        currentPrompt: String,
        audioBytes: ByteArray?
    ): JSONArray {
        val contents = JSONArray()
        val nonSystem = history.filter { it.sender != MessageSender.SYSTEM }

        // Determine effective message list
        val allMessages = if (nonSystem.isNotEmpty() && nonSystem.last().text == currentPrompt && nonSystem.last().sender == MessageSender.USER) {
            nonSystem.takeLast(6)
        } else {
            (nonSystem + ChatMessage(sender = MessageSender.USER, text = currentPrompt)).takeLast(6)
        }

        // Gemini API requires starting with a 'user' turn
        val validList = allMessages.dropWhile { it.sender != MessageSender.USER }.ifEmpty {
            listOf(ChatMessage(sender = MessageSender.USER, text = currentPrompt.ifBlank { "Hello Kavya!" }))
        }

        // Build alternating turns
        var lastRole: String? = null
        for (i in validList.indices) {
            val msg = validList[i]
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            if (role == lastRole) {
                continue // skip consecutive duplicate roles
            }
            lastRole = role

            val parts = JSONArray()
            parts.put(JSONObject().apply { put("text", msg.text) })

            // Attach inline audio if this is the final user message and audio is present
            if (i == validList.lastIndex && msg.sender == MessageSender.USER && audioBytes != null && audioBytes.isNotEmpty()) {
                val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                parts.put(JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "audio/wav")
                        put("data", base64Audio)
                    }
                    put("inlineData", inlineData)
                })
            }

            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", parts)
            })
        }

        return contents
    }

    /**
     * Generates a conversational reply from Kavya using verified official Gemini thinking models
     * without mock generators or fake fallbacks.
     */
    suspend fun generateReply(
        userPrompt: String,
        audioBytes: ByteArray? = null,
        history: List<ChatMessage>,
        mood: KavyaMood,
        language: KavyaLanguage = KavyaLanguage.AUTO,
        modelId: String,
        assistantName: String,
        thinkingLevel: String = "low",
        skillsAddendum: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        // Official Gemini models cascade prioritized by responsiveness
        val modelCascade = listOf(
            modelId,
            GeminiConstants.MODEL_THINKING,
            GeminiConstants.MODEL_GENERAL,
            GeminiConstants.MODEL_FAST
        ).distinct()

        val systemInstruction = "Your name is $assistantName. Use this name when referring to yourself.\n" +
            "${mood.promptInstruction.replace("Kavya", assistantName)}\n" +
            "${language.promptInstruction.replace("Kavya", assistantName)}\n" +
            "$skillsAddendum\n" +
            "Real-Time Spoken Conversation Guidelines:\n" +
            "- Keep replies short, vibrant, natural, and expressive (1 to 2 short sentences max). Never output long paragraphs.\n" +
            "- Do NOT output markdown asterisks or meta narration like *smiles* so spoken audio is clean.\n" +
            "- Fully support Hindi, Hinglish, and English naturally.\n" +
            "- Express enthusiasm and cute anime companion warmth!\n" +
            "- Device Hands & System Control Awareness:\n" +
            "  When the user asks you to interact with the device or perform actions, enthusiastically accept in your cute anime voice, and ALWAYS append the matching action tag at the end:\n" +
            "  * Open YouTube and search: [ACTION: YOUTUBE_SEARCH: <query>]\n" +
            "  * Open YouTube: [ACTION: OPEN_YOUTUBE]\n" +
            "  * Open specific app (Settings, Camera, Chrome, etc.): [ACTION: OPEN_APP: <app_name>]\n" +
            "  * Search the web: [ACTION: WEB_SEARCH: <query>]\n" +
            "  * Share screen or inspect what is on screen: [ACTION: SCREEN_SHARE]\n" +
            "  * Scroll down: [ACTION: SCROLL_DOWN]\n" +
            "  * Scroll up: [ACTION: SCROLL_UP]\n" +
            "  * Long press: [ACTION: LONG_PRESS]\n" +
            "  * Long press and scroll up / drag up: [ACTION: LONG_PRESS_AND_SCROLL_UP]\n" +
            "  * Tap/touch element: [ACTION: TAP: <element_name>]\n" +
            "  * Home, Back, Recents, Notifications: [ACTION: HOME], [ACTION: BACK], [ACTION: RECENTS], [ACTION: NOTIFICATIONS]."

        val contentsArray = buildCleanContents(history, userPrompt, audioBytes)

        var lastError: Exception? = null
        for (candidateModel in modelCascade) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$candidateModel:generateContent?key=$apiKey"

                val jsonBody = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.75)
                        put("topP", 0.95)
                        put("maxOutputTokens", 120)
                        put("thinkingConfig", JSONObject().apply {
                            put("thinkingLevel", thinkingLevel)
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val replyText = parts?.optJSONObject(0)?.optString("text")?.trim()

                    if (!replyText.isNullOrBlank()) {
                        return@withContext Result.success(replyText)
                    }
                } else {
                    lastError = Exception("HTTP ${response.code}: $responseBody")
                    Log.w(tag, "Model $candidateModel returned HTTP ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(tag, "Error with $candidateModel: ${e.message}")
            }
        }

        Result.failure(lastError ?: Exception("Inference failed across available Gemini models."))
    }

    /**
     * Generates both text and natural voice audio in a single call using Gemini's native audio modality.
     * This is the fastest path for real-time voice conversations.
     */
    suspend fun generateVoiceReply(
        userPrompt: String,
        audioBytes: ByteArray? = null,
        history: List<ChatMessage>,
        mood: KavyaMood,
        language: KavyaLanguage = KavyaLanguage.AUTO,
        modelId: String = GeminiConstants.MODEL_LIVE,
        assistantName: String,
        voiceName: String = GeminiConstants.VOICE_AOEDE,
        skillsAddendum: String = ""
    ): Result<Pair<String, ByteArray>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val systemInstruction = "Your name is $assistantName. ${mood.promptInstruction.replace("Kavya", assistantName)}\n" +
            "${language.promptInstruction.replace("Kavya", assistantName)}\n" +
            "$skillsAddendum\n" +
            "Reply naturally and concisely (1-2 sentences). Return both text and audio. Do NOT use markdown narration."

        val contentsArray = buildCleanContents(history, userPrompt, audioBytes)

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
                })
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.75)
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply { put("voiceName", voiceName) })
                        })
                    })
                })
            }

            val request = Request.Builder().url(url).post(jsonBody.toString().toRequestBody(jsonMediaType)).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val parts = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                
                var textContent = ""
                var audioData: ByteArray? = null
                
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i)
                        if (part.has("text")) {
                            textContent = part.optString("text").trim()
                        }
                        if (part.has("inlineData")) {
                            val inlineData = part.optJSONObject("inlineData")
                            val mimeType = inlineData.optString("mimeType", "audio/wav")
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotBlank()) {
                                val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                audioData = AudioUtils.pcmToWav(rawBytes, AudioUtils.extractSampleRateFromMime(mimeType, 24000))
                            }
                        }
                    }
                }

                if (textContent.isNotBlank() && audioData != null) {
                    return@withContext Result.success(Pair(textContent, audioData))
                }
            }
            Result.failure(Exception("Voice-enabled inference failed: ${response.code} $responseBody"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Multimodal visual screen analysis using live screen frame capture.
     * Takes JPEG image bytes and asks Gemini Vision to analyze what is currently visible on screen.
     */
    suspend fun analyzeScreen(
        imageBytes: ByteArray,
        userQuestion: String,
        mood: KavyaMood,
        language: KavyaLanguage,
        assistantName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val candidateModels = listOf(GeminiConstants.MODEL_GENERAL, GeminiConstants.MODEL_FLASH_35)

        val promptText = "Your name is $assistantName.\n" +
            "${mood.promptInstruction.replace("Kavya", assistantName)}\n" +
            "${language.promptInstruction.replace("Kavya", assistantName)}\n" +
            "The user is sharing their live screen. Look at this screen capture carefully and answer: $userQuestion\n" +
            "Keep your reply natural, concise (2-3 sentences), expressive, and in the user's selected language."

        var lastError: Exception? = null
        for (model in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val jsonBody = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val turn = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", promptText) })
                                put(JSONObject().apply {
                                    val inlineData = JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Image)
                                    }
                                    put("inlineData", inlineData)
                                })
                            }
                            put("role", "user")
                            put("parts", parts)
                        }
                        put(turn)
                    }
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("maxOutputTokens", 160)
                        put("temperature", 0.4)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val replyText = parts?.optJSONObject(0)?.optString("text")?.trim()

                    if (!replyText.isNullOrBlank()) {
                        return@withContext Result.success(replyText)
                    }
                } else {
                    lastError = Exception("Vision HTTP ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Screen vision analysis failed."))
    }

    /**
     * Synthesizes natural female speech using the official Gemini TTS models.
     */
    suspend fun synthesizeSpeech(
        text: String,
        voiceName: String = GeminiConstants.VOICE_AOEDE
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is missing."))
        }

        // Clean text for cleaner synthesis
        val cleanedText = text
            .replace(Regex("\\[ACTION:[^\\]]+\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\*.*?\\*"), "")
            .replace(Regex("[#_`~]"), "")
            .trim()

        if (cleanedText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Speech text is empty."))
        }

        // Use dedicated TTS model priority for high-quality voice synthesis
        val models = listOf(GeminiConstants.MODEL_TTS, GeminiConstants.MODEL_LOW, GeminiConstants.MODEL_GENERAL)
        var lastError: Exception? = null

        for (model in models) {
            val result = executeTtsCall(model, cleanedText, voiceName, apiKey)
            if (result.isSuccess) {
                return@withContext result
            }
            lastError = result.exceptionOrNull() as? Exception
            Log.w(tag, "TTS with $model failed: ${lastError?.message}")
        }

        Result.failure(lastError ?: Exception("Speech synthesis failed on all models."))
    }

    private fun executeTtsCall(
        model: String,
        text: String,
        voiceName: String,
        apiKey: String
    ): Result<ByteArray> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Say this naturally: $text") })
                        })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val parts = jsonResponse.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")

            if (parts == null) return Result.failure(Exception("No content returned from Gemini."))

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i)
                val inlineData = part?.optJSONObject("inlineData")
                if (inlineData != null) {
                    val audioBase64 = inlineData.optString("data")
                    val mimeType = inlineData.optString("mimeType", "audio/wav")
                    
                    if (audioBase64.isNotBlank()) {
                        val rawBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        // If it's already a WAV/MP3 with header, pcmToWav returns as is.
                        // Otherwise it wraps raw PCM in a WAV header.
                        val wavBytes = AudioUtils.pcmToWav(
                            rawBytes, 
                            sampleRate = AudioUtils.extractSampleRateFromMime(mimeType, 24000)
                        )
                        return Result.success(wavBytes)
                    }
                }
            }

            return Result.failure(Exception("No audio data found in response parts."))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
