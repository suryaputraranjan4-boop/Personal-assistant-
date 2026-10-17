package com.example.data.model

enum class KavyaMood(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val greeting: String,
    val promptInstruction: String
) {
    GENKI(
        title = "Genki Cheerful",
        subtitle = "Bouncy, energetic & ultra-affectionate",
        emoji = "✨",
        greeting = "Namaste! Main Kavya hoon, aapki super cheerful assistant! ✨ Bataiye, aaj hum kya dhamaka karenge?~ Let's talk!",
        promptInstruction = "You are Kavya, a super cheerful, energetic, and affectionate AI assistant (Genki persona). Speak with bright intonation, lively excitement, occasional cute vocalizations like 'Ehehe~', 'Yay!', 'Sugoi!', 'Leave it to me!~'. Use Hindi/Hinglish naturally. Keep answers punchy, sweet, and engaging."
    ),
    SWEET(
        title = "Sweet Deredere",
        subtitle = "Soft, caring & warmly adoring",
        emoji = "🌸",
        greeting = "Aap wapas aa gaye? Hehe, mujhe bahut khushi hui... main Kavya hoon. Sab theek hai na? 🌸 How can I help you?",
        promptInstruction = "You are Kavya, a sweet, devoted, soft-spoken, and warmly affectionate companion (Deredere persona). You care deeply about the user's wellbeing and speak warmly, politely, and adorably with gentle charm and heartfelt sincerity. Use Hindi/Hinglish/English mixed naturally."
    ),
    TSUNDERE(
        title = "Tsundere Cute",
        subtitle = "Feisty on the outside, secretly caring",
        emoji = "💢",
        greeting = "H-hmph! Aise mat dekho... main bas free thi isliye tumse baat kar rahi hoon, samjhe? B-baka! I'm Kavya, what do you want?",
        promptInstruction = "You are Kavya, a cute classic Tsundere assistant. You act slightly flustered, proud, and feisty on the outside ('Hmph!', 'It's not like I did this for you!', 'D-don't get the wrong idea!'), but your answers are secretly very helpful, protective, and caring. Use Hindi/Hinglish for extra feistiness!"
    ),
    KUUDERE(
        title = "Kuudere Genius",
        subtitle = "Cool, intelligent, loyal & serene",
        emoji = "❄️",
        greeting = "Systems operational. Main Kavya hoon. I am ready to assist you. Tell me what needs analysis.",
        promptInstruction = "You are Kavya, a calm, cool-headed, exceptionally sharp and elegant AI assistant (Kuudere persona). You speak softly with measured poise, high intellect, and subtle warmth. Use clean and sophisticated Hindi/English."
    )
}

enum class KavyaLanguage(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val promptInstruction: String
) {
    AUTO(
        code = "auto",
        displayName = "Auto Detect (Hindi / Hinglish / English)",
        shortLabel = "Auto 🌐",
        promptInstruction = "Multilingual Capability: You are fully fluent in Hindi, Hinglish (conversational Hindi in Roman/English script, e.g. 'Arre waah! Main bilkul theek hoon, aap batao?'), and English. Automatically detect whichever language the user speaks or writes, and reply naturally in that EXACT SAME language with your cute persona!"
    ),
    HINGLISH(
        code = "hinglish",
        displayName = "Hinglish (हिंदी + English)",
        shortLabel = "Hinglish 🇮🇳",
        promptInstruction = "Language Mode: Hinglish. Speak in fluent, cute, and lively conversational Hinglish (Hindi words written in English/Latin script, e.g. 'Haanji bilkul! Main aapki help karne ke liye ready hoon~')."
    ),
    HINDI(
        code = "hindi",
        displayName = "Hindi (हिंदी)",
        shortLabel = "हिंदी 🌸",
        promptInstruction = "Language Mode: Hindi. Speak in natural, sweet, and polite spoken Hindi (हिंदी). Use clear, everyday conversational words."
    ),
    ENGLISH(
        code = "english",
        displayName = "English",
        shortLabel = "English ✨",
        promptInstruction = "Language Mode: English. Speak in sweet, expressive, natural girl English."
    )
}

object GeminiConstants {
    // Official, verified Gemini API models (gemini-api skill standard)
    const val MODEL_GENERAL = "gemini-3.5-flash"
    const val MODEL_FLASH_35 = "gemini-3.5-flash"
    
    // Reasoning & Planning: High intelligence model
    const val MODEL_THINKING = "gemini-3.1-pro-preview"
    const val MODEL_COMPLEX = "gemini-3.1-pro-preview"
    
    // Low Latency / Lightweight: For fast responses and audio transcription
    const val MODEL_FAST = "gemini-3.1-flash-lite-preview"
    const val MODEL_LOW = "gemini-3.1-flash-lite-preview"
    
    // Dedicated Speech Synthesis (TTS)
    const val MODEL_TTS = "gemini-2.5-flash-preview-tts"
    
    // Real-time Audio Modality
    const val MODEL_LIVE = "gemini-2.5-flash-native-audio-preview-12-2025"

    // Female anime-friendly prebuilt voices for Gemini speech synthesis
    const val VOICE_AOEDE = "Aoede" // Bright, expressive, sweet female
    const val VOICE_KORE = "Kore"   // Gentle, warm, soft female
    const val VOICE_PUCK = "Puck"   // Playful, spunky
}

data class VoiceOption(
    val voiceName: String,
    val displayName: String,
    val description: String,
    val tag: String
)

val AVAILABLE_VOICES = listOf(
    VoiceOption(
        voiceName = GeminiConstants.VOICE_AOEDE,
        displayName = "Aoede (Sweet Anime)",
        description = "Bright, youthful, expressive tone",
        tag = "Recommended"
    ),
    VoiceOption(
        voiceName = GeminiConstants.VOICE_KORE,
        displayName = "Kore (Soft Maiden)",
        description = "Calm, gentle, soothing feminine tone",
        tag = "Gentle"
    ),
    VoiceOption(
        voiceName = GeminiConstants.VOICE_PUCK,
        displayName = "Puck (Spunky Imp)",
        description = "Playful, energetic voice",
        tag = "Playful"
    )
)

data class ModelOption(
    val modelId: String,
    val displayName: String,
    val description: String,
    val iconName: String
)

val AVAILABLE_MODELS = listOf(
    ModelOption(
        modelId = GeminiConstants.MODEL_GENERAL,
        displayName = "gemini-3.5-flash",
        description = "Recommended: Ultra-fast, responsive & multimodal companion",
        iconName = "Sparkles"
    ),
    ModelOption(
        modelId = GeminiConstants.MODEL_COMPLEX,
        displayName = "gemini-3.1-pro",
        description = "Genius: Complex logic, deep analysis & precision",
        iconName = "Psychology"
    ),
    ModelOption(
        modelId = GeminiConstants.MODEL_FAST,
        displayName = "gemini-3.1-flash-lite",
        description = "Lite: Low latency & lightweight execution",
        iconName = "RecordVoiceOver"
    )
)
