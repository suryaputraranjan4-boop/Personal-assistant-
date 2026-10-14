package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AudioRecorder {

    private val tag = "AudioRecorder"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val pcmOutputStream = ByteArrayOutputStream()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope): Boolean {
        if (_isRecording.value) return true

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(tag, "Invalid audio record buffer size")
            return false
        }

        val bufferSize = minBufferSize * 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord could not initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            pcmOutputStream.reset()
            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)

                while (isActive && _isRecording.value) {
                    if (pcmOutputStream.size() > 320000) {
                        break // Cap to 10 seconds to keep memory & network fast
                    }
                    val readShorts = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readShorts > 0) {
                        // Calculate amplitude for UI
                        val rms = AudioUtils.calculateRmsAmplitude(shortBuffer, readShorts)
                        _amplitude.value = rms

                        // Convert shorts to little-endian bytes
                        var byteIndex = 0
                        for (i in 0 until readShorts) {
                            val sample = shortBuffer[i].toInt()
                            byteBuffer[byteIndex++] = (sample and 0xFF).toByte()
                            byteBuffer[byteIndex++] = ((sample shr 8) and 0xFF).toByte()
                        }
                        synchronized(pcmOutputStream) {
                            pcmOutputStream.write(byteBuffer, 0, byteIndex)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(tag, "startRecording failed", e)
            stopRecording()
            return false
        }
    }

    fun stopRecording(): ByteArray {
        _isRecording.value = false
        _amplitude.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audioRecord", e)
        }
        audioRecord = null

        val rawPcm = synchronized(pcmOutputStream) {
            pcmOutputStream.toByteArray()
        }
        pcmOutputStream.reset()

        // Package PCM into proper 16kHz WAV format
        return AudioUtils.pcmToWav(rawPcm, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
    }

    fun cancelRecording() {
        _isRecording.value = false
        _amplitude.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error cancelling audioRecord", e)
        }
        audioRecord = null
        synchronized(pcmOutputStream) {
            pcmOutputStream.reset()
        }
    }
}
