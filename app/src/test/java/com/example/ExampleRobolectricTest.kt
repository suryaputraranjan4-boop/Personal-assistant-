package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioUtils
import com.example.data.model.AimiMood
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiConstants
import com.example.data.model.MessageSender
import com.example.service.ScreenCaptureService
import com.example.service.SystemAction
import com.example.service.SystemActionExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aimi AI", appName)
    }

    @Test
    fun `pcmToWav creates valid RIFF header`() {
        val dummyPcm = ByteArray(100) { 0 }
        val wav = AudioUtils.pcmToWav(dummyPcm, sampleRate = 24000, channels = 1)
        assertTrue(wav.size >= 44)
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])
        assertEquals('W'.code.toByte(), wav[8])
        assertEquals('A'.code.toByte(), wav[9])
        assertEquals('V'.code.toByte(), wav[10])
        assertEquals('E'.code.toByte(), wav[11])
    }

    @Test
    fun `audioUtils extracts sample rate from mimeType`() {
        val sampleRate = AudioUtils.extractSampleRateFromMime("audio/pcm;rate=16000", 24000)
        assertEquals(16000, sampleRate)

        val defaultRate = AudioUtils.extractSampleRateFromMime("audio/wav", 24000)
        assertEquals(24000, defaultRate)
    }

    @Test
    fun `verified gemini models and persona are properly defined`() {
        assertEquals("gemini-2.5-flash-preview-tts", GeminiConstants.MODEL_TTS)
        assertEquals("gemini-2.5-flash", GeminiConstants.MODEL_GENERAL)
        assertEquals("gemini-3.5-flash", GeminiConstants.MODEL_FLASH_35)
        assertEquals("gemini-3.1-pro-preview", GeminiConstants.MODEL_COMPLEX)
        assertTrue(AimiMood.GENKI.greeting.isNotEmpty())
    }

    @Test
    fun `system action executor parses YouTube search commands`() {
        val action = SystemActionExecutor.parseSystemAction("Open YouTube and search for Hindi anime songs")
        assertTrue(action is SystemAction.YouTubeSearch)
        val ytAction = action as SystemAction.YouTubeSearch
        assertTrue(ytAction.query.contains("Hindi anime songs", ignoreCase = true))
    }

    @Test
    fun `system action executor parses screen share command`() {
        val action = SystemActionExecutor.parseSystemAction("Screen share")
        assertTrue(action is SystemAction.ReadScreen)
    }

    @Test
    fun `screen capture service state is accessible`() {
        assertNotNull(ScreenCaptureService.isCapturing)
        assertEquals(false, ScreenCaptureService.isCapturing.value)
    }

    @Test
    fun `chat message model creation`() {
        val msg = ChatMessage(
            sender = MessageSender.AIMI,
            text = "Konnichiwa!",
            modelUsed = GeminiConstants.MODEL_GENERAL
        )
        assertEquals(MessageSender.AIMI, msg.sender)
        assertEquals("Konnichiwa!", msg.text)
    }

    @Test
    fun `language options support Hindi Hinglish and English`() {
        val langs = com.example.data.model.AimiLanguage.values()
        assertTrue(langs.any { it.code == "hindi" })
        assertTrue(langs.any { it.code == "hinglish" })
        assertTrue(langs.any { it.code == "english" })
        assertTrue(langs.any { it.code == "auto" })
    }
}
