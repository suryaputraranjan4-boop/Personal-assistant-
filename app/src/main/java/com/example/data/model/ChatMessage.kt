package com.example.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    AIMI,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val audioBytes: ByteArray? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isSpeaking: Boolean = false,
    val isTranscribedFromVoice: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatMessage

        if (id != other.id) return false
        if (sender != other.sender) return false
        if (text != other.text) return false
        if (timestamp != other.timestamp) return false
        if (modelUsed != other.modelUsed) return false
        if (isSpeaking != other.isSpeaking) return false
        if (isTranscribedFromVoice != other.isTranscribedFromVoice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sender.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (modelUsed?.hashCode() ?: 0)
        result = 31 * result + isSpeaking.hashCode()
        result = 31 * result + isTranscribedFromVoice.hashCode()
        return result
    }
}
