package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

class AudioPlayer(private val context: Context) {

    private val tag = "AudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var progressJob: Job? = null
    private var currentTempFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackAmplitude = MutableStateFlow(0f)
    val playbackAmplitude: StateFlow<Float> = _playbackAmplitude.asStateFlow()

    private val _currentPlayingMessageId = MutableStateFlow<String?>(null)
    val currentPlayingMessageId: StateFlow<String?> = _currentPlayingMessageId.asStateFlow()

    fun playAudio(
        audioBytes: ByteArray,
        messageId: String,
        coroutineScope: CoroutineScope,
        onCompletion: () -> Unit = {}
    ) {
        stop()

        if (audioBytes.isEmpty()) {
            onCompletion()
            return
        }

        // Strategy 1: Use Android MediaPlayer via temp WAV file for 100% reliable system playback
        val mediaPlayerSuccess = tryPlayWithMediaPlayer(audioBytes, messageId, coroutineScope, onCompletion)
        if (mediaPlayerSuccess) {
            return
        }

        // Strategy 2: Fallback to direct AudioTrack playback
        val isWav = audioBytes.size > 44 &&
            audioBytes[0] == 'R'.code.toByte() &&
            audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() &&
            audioBytes[3] == 'F'.code.toByte()

        val pcmPayload = if (isWav) audioBytes.copyOfRange(44, audioBytes.size) else audioBytes
        tryPlayWithAudioTrack(pcmPayload, messageId, coroutineScope, onCompletion)
    }

    private fun tryPlayWithAudioTrack(
        pcmData: ByteArray,
        messageId: String,
        coroutineScope: CoroutineScope,
        onCompletion: () -> Unit
    ): Boolean {
        return try {
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = track.write(pcmData, 0, pcmData.size)
            if (written <= 0) {
                track.release()
                return false
            }

            track.play()
            audioTrack = track
            _isPlaying.value = true
            _currentPlayingMessageId.value = messageId

            // Duration in ms: samples / sampleRate * 1000
            val durationMs = (pcmData.size / 2.0 / sampleRate * 1000).toLong()

            progressJob = coroutineScope.launch(Dispatchers.Default) {
                var tick = 0f
                val startTime = System.currentTimeMillis()
                while (isActive && _isPlaying.value && (System.currentTimeMillis() - startTime) < durationMs + 100) {
                    tick += 0.38f
                    val wave = (sin(tick.toDouble()) * 0.45 + sin(tick * 2.1) * 0.3 + 0.55).toFloat()
                    _playbackAmplitude.value = wave.coerceIn(0.2f, 0.95f)
                    delay(45)
                }
                _playbackAmplitude.value = 0f
                _isPlaying.value = false
                _currentPlayingMessageId.value = null
                stop()
                onCompletion()
            }
            true
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack playback failed, trying MediaPlayer: ${e.message}")
            false
        }
    }

    private fun tryPlayWithMediaPlayer(
        audioBytes: ByteArray,
        messageId: String,
        coroutineScope: CoroutineScope,
        onCompletion: () -> Unit
    ): Boolean {
        return try {
            val tempFile = File.createTempFile("kavya_speech_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }
            currentTempFile = tempFile

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackAmplitude.value = 0f
                    _currentPlayingMessageId.value = null
                    progressJob?.cancel()
                    progressJob = null
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    onCompletion()
                    true
                }
                start()
            }

            _isPlaying.value = true
            _currentPlayingMessageId.value = messageId

            progressJob = coroutineScope.launch(Dispatchers.Default) {
                var tick = 0f
                while (isActive && _isPlaying.value) {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        tick += 0.35f
                        val wave = (sin(tick.toDouble()) * 0.4 + sin(tick * 2.3) * 0.3 + 0.5).toFloat()
                        _playbackAmplitude.value = wave.coerceIn(0.15f, 0.95f)
                    } else {
                        _playbackAmplitude.value = 0f
                    }
                    delay(50)
                }
                _playbackAmplitude.value = 0f
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "MediaPlayer playback error", e)
            stop()
            false
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        _isPlaying.value = false
        _playbackAmplitude.value = 0f
        _currentPlayingMessageId.value = null

        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing audioTrack: ${e.message}")
        }
        audioTrack = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error resetting mediaPlayer: ${e.message}")
        }
        mediaPlayer = null

        try {
            currentTempFile?.delete()
            currentTempFile = null
        } catch (e: Exception) {
            Log.w(tag, "Error deleting temp audio file: ${e.message}")
        }
    }

    fun release() {
        stop()
    }
}
